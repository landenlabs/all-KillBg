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
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

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
    public boolean isScanRunning = false;

    public void saveToBundle(@NonNull Bundle outState) {
        outState.putBoolean("isJobRunning", isJobRunning);
        outState.putBoolean("isScanRunning", isScanRunning);
        outState.putInt("stopProcIdx", stopProcIdx);
        Log.i(APP_TAG, "saveToBundle, job="+ isJobRunning + " scan=" + isScanRunning + " idx=" + stopProcIdx);
    }

    public void loadFromBundle(@Nullable Bundle inState) {
        if (inState != null) {
            isJobRunning = inState.getBoolean("isJobRunning");
            isScanRunning = inState.getBoolean("isScanRunning");
            stopProcIdx = inState.getInt("stopProcIdx", -1);
        }
        
        if ((isJobRunning || isScanRunning) && dataList.isEmpty()) {
             loadList(null);
             if (stopProcIdx == -1) stopProcIdx = 0;
        }
        Log.i(APP_TAG, "loadFromBundle, job="+ isJobRunning + " scan=" + isScanRunning + " idx=" + stopProcIdx);
    }

    public interface ProcAction extends AppAction {
        void done(@NonNull AppProcessManager mgr, int status, @NonNull String msg);
    }

    AppProcessManager(@NonNull Activity activity) {
        context = activity;
        activityManager = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
    }

    void stopProcesses() {
        StopProcByAccessibilityService.setRunning(dataList.size() > 0, StopProcByAccessibilityService.ServiceMode.STOP);
        if (dataList.size() > 0) {
            stopProcIdx = 0;
            stopContinue();
        }
    }

    void scanProcesses() {
        StopProcByAccessibilityService.setRunning(dataList.size() > 0, StopProcByAccessibilityService.ServiceMode.SCAN);
        if (dataList.size() > 0) {
            stopProcIdx = 0;
            isScanRunning = true;
            scanContinue();
        }
    }

    void stopContinue() {
        if (stopProcIdx < dataList.size()) {
            ProcInfo procInfo  = dataList.get(stopProcIdx);
            String pkgName = procInfo.pkgName != null ? procInfo.pkgName : procInfo.name;
            Log.d(APP_TAG, String.format(Locale.US, "%s %s %d of %d", "Stopping:", pkgName, stopProcIdx, dataList.size()));

            // Use helper to stop/open settings
            if (!pkgName.equals(context.getPackageName())) {
                isJobRunning = true;
                StopProcByAccessibilityService.setCurrentInspectedPackage(pkgName);
                StopProcByAccessibilityService.setRunning(true, StopProcByAccessibilityService.ServiceMode.STOP);
                AppUtils.stopProcess(context, activityManager, pkgName);

                // Note: We break here because the Accessibility Service will handle the clicks
                // and then return to this app. MainActivity.onResume will then be called
                // to proceed to the next item in the list if automation is still active.
                stopProcIdx++;
            }
        } else {
            Log.d(APP_TAG, "Stopping done " + stopProcIdx + " of " + dataList.size());
            isJobRunning = false;
            stopProcIdx = -1;
            StopProcByAccessibilityService.setRunning(false, StopProcByAccessibilityService.ServiceMode.STOP);
        }
    }

    void scanContinue() {
        if (stopProcIdx < dataList.size()) {
            ProcInfo procInfo = dataList.get(stopProcIdx);
            String pkgName = procInfo.pkgName != null ? procInfo.pkgName : procInfo.name;
            Log.d(APP_TAG, String.format(Locale.US, "%s %s %d of %d", "Scanning:", pkgName, stopProcIdx, dataList.size()));

            if (!pkgName.equals(context.getPackageName())) {
                isScanRunning = true;
                StopProcByAccessibilityService.setCurrentInspectedPackage(pkgName);
                StopProcByAccessibilityService.setRunning(true, StopProcByAccessibilityService.ServiceMode.SCAN);
                AppUtils.stopProcess(context, activityManager, pkgName);
                stopProcIdx++;
            } else {
                stopProcIdx++;
                scanContinue();
            }
        } else {
            Log.d(APP_TAG, "Scanning done " + stopProcIdx + " of " + dataList.size());
            isScanRunning = false;
            stopProcIdx = -1;
            StopProcByAccessibilityService.setRunning(false, StopProcByAccessibilityService.ServiceMode.SCAN);
            showScanResults();
        }
    }

    private void showScanResults() {
        List<String> running = StopProcByAccessibilityService.getRunningPackages();
        if (running.isEmpty()) {
            AppUtils.showStatus(findViewById(android.R.id.content), "No running apps found during scan.");
            return;
        }

        // Try to resolve package names to labels for better UI
        String[] displayNames = new String[running.size()];
        for (int i = 0; i < running.size(); i++) {
            String pkg = running.get(i);
            try {
                displayNames[i] = context.getPackageManager().getApplicationLabel(
                        context.getPackageManager().getApplicationInfo(pkg, 0)).toString();
            } catch (Exception e) {
                displayNames[i] = pkg;
            }
        }

        final boolean[] checked = new boolean[running.size()];
        java.util.Arrays.fill(checked, true);

        new AlertDialog.Builder(context)
                .setTitle("Scan Results - Running Apps")
                .setMultiChoiceItems(displayNames, checked, (dialog, which, isChecked) -> checked[which] = isChecked)
                .setPositiveButton("Add to Stop List", (dialog, which) -> {
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                    Set<String> stopList = new HashSet<>(prefs.getStringSet("stop_regex_list", new HashSet<>()));
                    int addedCount = 0;
                    for (int i = 0; i < running.size(); i++) {
                        if (checked[i]) {
                            // Add as exact match regex
                            if (stopList.add("^" + Pattern.quote(running.get(i)) + "$")) {
                                addedCount++;
                            }
                        }
                    }
                    prefs.edit().putStringSet("stop_regex_list", stopList).apply();
                    AppUtils.showStatus(findViewById(android.R.id.content), "Added " + addedCount + " items to Stop List");
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private View findViewById(int id) {
        return ((Activity)context).findViewById(id);
    }

    @NonNull
    synchronized ArrayList<ProcInfo> getList() {
        return dataList;
    }

    synchronized void loadListAsync(@Nullable ProcAction action) {
        executorService.execute(() -> { loadList(action); });
    }

    void loadList(@Nullable ProcAction action) {
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
