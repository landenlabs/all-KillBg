/*
 * Copyright (c) 2026 Dennis Lang (LanDen Labs)
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the
 * following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * @author Dennis Lang
 * @see https://LanDenLabs.com/
 */

package com.landenlabs.all_stop;

import static android.content.Context.ACTIVITY_SERVICE;
import static com.landenlabs.all_stop.AppConstants.APP_TAG;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 */
@SuppressWarnings("SizeReplaceableByIsEmpty")
class AppProcessManager {

    private final Context context;
    private final ActivityManager activityManager;
    private final ArrayList<ProcInfo> dataList = new ArrayList<>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private int stopProcIdx = -1;
    public boolean isJobRunning = false;

    public void saveToBundle(@NonNull Bundle outState) {
        outState.putBoolean("isJobRunning", isJobRunning);
    }

    public void loadFromBundle(@Nullable Bundle inState) {
        if (inState != null) {
            isJobRunning = inState.getBoolean("isJobRunning");
        }
        if (stopProcIdx == -1 && dataList.isEmpty()) {
             loadList(null);
             stopProcIdx = 0;
        }
    }

    public interface ProcAction extends AppAction {
        void done(@NonNull AppProcessManager mgr, int status, @NonNull String msg);
    }

    AppProcessManager(@NonNull Activity activity) {
        context = activity;
        activityManager = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
    }

    void stopProcesses() {
        StopProcByAccessibilityService.setRunning(true);
        if (dataList.size() > 0) {
            stopProcIdx = 0;
            stopContinue();
        }
    }

    void stopContinue() {
        if (stopProcIdx < dataList.size()) {
            ProcInfo procInfo  = dataList.get(stopProcIdx);
            String pkgName = procInfo.pkgName != null ? procInfo.pkgName : procInfo.name;
            Log.d(APP_TAG, "Stopping: " + pkgName);

            // 1. Try standard background kill (mostly ineffective on Android 14+)
            activityManager.killBackgroundProcesses(pkgName);

            // 2. Trigger Accessibility Automation by opening App Info page
            if (!pkgName.equals(context.getPackageName())) {
                isJobRunning = true;
                StopProcByAccessibilityService.setRunning(true);
                openAppDetailSettings(pkgName);
                // Note: We break here because the Accessibility Service will handle the clicks
                // and then return to this app. MainActivity.onResume will then be called
                // to proceed to the next item in the list if automation is still active.
                stopProcIdx++;
            }
        } else {
            Log.d(APP_TAG, "Stopping done " + stopProcIdx + " of " + dataList.size());
            isJobRunning = false;
            stopProcIdx = -1;
            StopProcByAccessibilityService.setRunning(false);
        }
    }

    private void openAppDetailSettings(@NonNull String packageName) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + packageName));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    @NonNull
    synchronized ArrayList<ProcInfo> getList() {
        return dataList;
    }

    synchronized void loadList(@Nullable ProcAction action) {
        executorService.execute(() -> {
            synchronized (dataList) {
                dataList.clear();

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
                    // Fallback: If no usage stats (maybe permission missing), show all installed apps
                    loadInstalledApps();
                } else {
                    // Legacy fallback
                    List<ActivityManager.RunningAppProcessInfo> processes = activityManager.getRunningAppProcesses();
                    if (processes != null) {
                        for (ActivityManager.RunningAppProcessInfo proc : processes) {
                            ProcInfo procInfo = new ProcInfo();
                            procInfo.name = proc.processName;
                            procInfo.pkgName = (proc.pkgList != null && proc.pkgList.length > 0) ? proc.pkgList[0] : proc.processName;
                            procInfo.importance = proc.importance;
                            procInfo.state = ProcInfo.getImportance(proc.importance);
                            dataList.add(procInfo);
                        }
                    }
                }
            }
            if (action != null)
                action.done(this, dataList.isEmpty() ? ProcAction.STATUS_ERROR : ProcAction.STATUS_OK, "");
        });
    }

    private void loadInstalledApps() {
        List<PackageInfo> pkgs = context.getPackageManager().getInstalledPackages(0);
        for (PackageInfo pkg : pkgs) {
            // Filter out system apps if desired, or show all
            if ((pkg.applicationInfo.flags & android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0) {
                ProcInfo procInfo = new ProcInfo();
                procInfo.name = pkg.packageName;
                procInfo.pkgName = pkg.packageName;
                procInfo.state = "Installed";
                dataList.add(procInfo);
            }
        }
    }

    // =============================================================================================

    public static class ProcInfo extends DataItem{
        int pid;
        int ppid;
        int importance;
        long startTime;
        int policy;
        String state;
        long procSize;
        // public PackageInfo pkginfo;

        @NonNull
        @SuppressWarnings("deprecation")
        static String getImportance(int importanceNum) {
            return switch (importanceNum) {
                case ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND -> "Foreground";
                case ActivityManager.RunningAppProcessInfo.IMPORTANCE_TOP_SLEEPING -> "Top Sleep";
                case ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE -> "Visible";
                case ActivityManager.RunningAppProcessInfo.IMPORTANCE_PERCEPTIBLE -> "Perceptible";
                case ActivityManager.RunningAppProcessInfo.IMPORTANCE_SERVICE -> "Service";
                case ActivityManager.RunningAppProcessInfo.IMPORTANCE_CACHED -> "Cache";
                case ActivityManager.RunningAppProcessInfo.IMPORTANCE_EMPTY -> "Empty";
                case ActivityManager.RunningAppProcessInfo.IMPORTANCE_GONE -> "Gone";
                default -> "";
            };
        }

        @NonNull
        public String toString() {
            final SimpleDateFormat dateFmt = new SimpleDateFormat("EEE  hh:mm a z", Locale.US);
            String importanceStr = getImportance(importance);

            return name
                    + "\nPid: " + pid
                    + "\nState: " + state
                    + "\nStartTm: " + dateFmt.format(startTime)
                    + String.format(Locale.US, "\nMemory: %.2f KB", procSize / 1024.0)
                    + "\n" + importanceStr;
        }
    }
}
