/*
 * Copyright (c) 2020 Dennis Lang (LanDen Labs) landenlabs@gmail.com
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

package com.landenlabs.all_killbg;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.Debug;
import androidx.annotation.NonNull;

import com.jaredrummler.android.processes.AndroidProcesses;
import com.jaredrummler.android.processes.models.AndroidAppProcess;
import com.jaredrummler.android.processes.models.Stat;
import com.jaredrummler.android.processes.models.Statm;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import static android.content.Context.ACTIVITY_SERVICE;

/**
 */
class AppProcessManager {

    private final Context context;
    private final ActivityManager activityManager;
    private final ArrayList<ProcInfo> dataList = new ArrayList<>();


    public interface ProcAction extends AppAction {
        void done(AppProcessManager mgr, int status, String msg);
    }

    AppProcessManager(Activity activity) {
        context = activity;
        activityManager = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
    }

    void killAllBackgroundProcesses() {
        for (ProcInfo procInfo : dataList) {
            String processName = procInfo.name;
            activityManager.killBackgroundProcesses(processName);
        }
    }

    synchronized ArrayList<ProcInfo> getList() {
        return dataList;
    }

    synchronized void loadList(ProcAction action) {

        dataList.clear();

        //noinspection ConstantConditions
        if (false) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {

                UsageStatsManager usm =
                        (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
                long time = System.currentTimeMillis();
                List<UsageStats> appList =
                        usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY,
                                time - 1000 * 1000, time);
                if (appList != null && appList.size() > 0) {
                    SortedMap<Long, UsageStats> mySortedMap = new TreeMap<>();
                    for (UsageStats usageStats : appList) {
                        mySortedMap.put(usageStats.getLastTimeUsed(), usageStats);

                        ProcInfo procInfo = new ProcInfo();
                        procInfo.name = usageStats.getPackageName();
                        procInfo.startTime = usageStats.getFirstTimeStamp();

                        dataList.add(procInfo);
                    }
                }
            } else {
                ActivityManager am =
                        (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
                List<ActivityManager.RunningAppProcessInfo> tasks = am.getRunningAppProcesses();
            }
        }

        //noinspection ConstantConditions
        if (true) {
            if (Build.VERSION.SDK_INT >= 21) {
                List<AndroidAppProcess> processes = AndroidProcesses.getRunningAppProcesses();
                for (AndroidAppProcess process : processes) {
                    // Get some information about the process
                    ProcInfo procInfo = new ProcInfo();
                    procInfo.name = process.name;

                    try {
                        Stat stat = process.stat();
                        procInfo.pid = stat.getPid();
                        procInfo.ppid = stat.ppid();
                        procInfo.startTime = stat.stime();
                        procInfo.policy = stat.policy();
                        procInfo.state = String.valueOf(stat.state());

                        Statm statm = process.statm();
                        procInfo.procSize = statm.getSize();
                        // procInfo.residentSetSize = statm.getResidentSetSize();

                        PackageInfo pkginfo = process.getPackageInfo(context, 0);
                        procInfo.pkgName = pkginfo.packageName;

                        dataList.add(procInfo);
                    } catch (Exception ex) {
                        action.done(this, ProcAction.STATUS_ERROR, ex.getMessage());
                        return;
                    }
                }
            } else {
                List<ActivityManager.RunningAppProcessInfo> mRunningPros =
                        activityManager.getRunningAppProcesses();
                if (mRunningPros != null) {
                    // procCntTv.setText(String.valueOf(mRunningPros.size()));

                    for (ActivityManager.RunningAppProcessInfo proc : mRunningPros) {
                        // TODO - filter list
                        ProcInfo procInfo = new ProcInfo();
                        procInfo.pid = proc.pid;
                        procInfo.ppid = -1;
                        procInfo.name = proc.processName;
                        procInfo.importance = proc.importance;
                        procInfo.state = ProcInfo.getImportance(proc.importance);

                        int[] myMempid = new int[]{procInfo.pid};
                        Debug.MemoryInfo[] memoryInfo =
                                activityManager.getProcessMemoryInfo(myMempid);
                        procInfo.procSize = memoryInfo[0].getTotalPss();

                        procInfo.pkgName = (proc.pkgList != null) ? proc.pkgList[0] : proc.processName;
                        dataList.add(procInfo);
                    }
                } else {
                    action.done(this, ProcAction.STATUS_ERROR, "Unabe to get process list");
                    return;
                }
            }
        }

        action.done(this, dataList.isEmpty() ? ProcAction.STATUS_ERROR : ProcAction.STATUS_OK, "");
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
        static String getImportance(int importanceNum) {
            String importance = "";
            switch (importanceNum) {
                case ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND:   // 100
                    importance = "Foreground";
                    break;
                case ActivityManager.RunningAppProcessInfo.IMPORTANCE_TOP_SLEEPING: // 150
                    importance = "Top Sleep";
                    break;
                case ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE:      // 200
                    importance = "Visible";
                    break;
                case ActivityManager.RunningAppProcessInfo.IMPORTANCE_PERCEPTIBLE:  // 230
                    importance = "Perceptible";
                    break;
                case ActivityManager.RunningAppProcessInfo.IMPORTANCE_SERVICE:      // 300
                    importance = "Service";
                    break;
                case ActivityManager.RunningAppProcessInfo.IMPORTANCE_CACHED:       // 400
                    importance = "Cache";
                    break;
                case ActivityManager.RunningAppProcessInfo.IMPORTANCE_EMPTY:        // 500
                    importance = "Empty";
                    break;
                case ActivityManager.RunningAppProcessInfo.IMPORTANCE_GONE:         // 1000
                    importance = "Gone";
                    break;
            }
            return importance;
        }

        @NonNull
        public String toString() {

            final SimpleDateFormat dateFmt = new SimpleDateFormat("EEE  hh:mm a z");

            //int[] myMempid = new int[]{pid};
            //Debug.MemoryInfo[] memoryInfo = myActivityManager.getProcessMemoryInfo(myMempid);
            //double memSizeKB = memoryInfo[0].dalvikPrivateDirty / 1024.0;

            String importanceStr = getImportance(importance);

            return name
                    + "\nPid: " + pid
                    + "\nState: " + state
                    + "\nStartTm: " + dateFmt.format(startTime)
                    + String.format("\nMemory: %.2f KB", procSize / 1024.0)
                    + "\n" + importanceStr;
        }
    }
}
