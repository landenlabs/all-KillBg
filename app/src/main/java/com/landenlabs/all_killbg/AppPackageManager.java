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
 * @see https://landenLabs.com/
 */

package com.landenlabs.all_killbg;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Build;
import androidx.annotation.NonNull;

import java.io.File;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;


/**
 * Populate list of packages with specific attributes.
 */
@SuppressWarnings("BooleanMethodIsAlwaysInverted")
class AppPackageManager {

    private final PackageManager      packageManager;
    private final String              ourPkgName;
    private final ArrayList<PkgInfo>  dataList = new ArrayList<>();
    private final Set<String>         pkgList = new HashSet<>();
    private final Map<String, PkgData> pkgInventory = new HashMap<>();

    public interface PkgAction extends AppAction {
        void done(AppPackageManager mgr, int status, String msg);
    }

    AppPackageManager(Activity activity) {
        packageManager = ((Context) activity).getPackageManager();
        ourPkgName = ((Context) activity).getPackageName();
    }


    synchronized ArrayList<PkgInfo> getList() {
        return dataList;
    }

    synchronized void loadList(PkgAction action) {

        dataList.clear();
        pkgList.clear();
        pkgList.add(ourPkgName);

        Intent launchIntent = new Intent("android.intent.action.MAIN", null);
        launchIntent.addCategory("android.intent.category.LAUNCHER");
        List<ResolveInfo> infoList = packageManager.queryIntentActivities(launchIntent, 0);

        for (ResolveInfo info : infoList) {
            ApplicationInfo appInfo = info.activityInfo.applicationInfo;
            boolean isDup = pkgList.contains(appInfo.packageName);

            if (!isStopped(appInfo) && !isSystem(appInfo) && !isDup) {
                PkgInfo pkgInfo = new PkgInfo();
                pkgInfo.label = packageManager.getApplicationLabel(appInfo).toString();
                pkgInfo.pkgName = appInfo.packageName;
                pkgInfo.processName = appInfo.processName;

                File file = new File(appInfo.sourceDir);
                pkgInfo.pkgSize = file.length();
                pkgInfo.targetSdkVersion = appInfo.targetSdkVersion;

                // todo get network usage using appInfo.uid;
                // appInfo.storageUuid;

                dataList.add(pkgInfo);
                pkgList.add(appInfo.packageName);
            }
        }

        if (!packageManager.hasSystemFeature("android.hardware.touchscreen")
                && Build.VERSION.SDK_INT >= 21) {

            Intent leanbackIntent = new Intent("android.intent.action.MAIN", null);
            leanbackIntent.addCategory("android.intent.category.LEANBACK_LAUNCHER");

            for (ResolveInfo info : packageManager.queryIntentActivities(leanbackIntent, 0)) {
                ApplicationInfo appInfo = info.activityInfo.applicationInfo;
                boolean isDup = pkgList.contains(appInfo.packageName);

                if (!isStopped(appInfo) && !isSystem(appInfo) && !isDup) {
                    PkgInfo pkgInfo = new PkgInfo();
                    pkgInfo.label = packageManager.getApplicationLabel(appInfo).toString();
                    pkgInfo.pkgName = appInfo.packageName;
                    pkgInfo.processName = appInfo.processName;

                    File file = new File(appInfo.sourceDir);
                    pkgInfo.pkgSize = file.length();
                    pkgInfo.targetSdkVersion = appInfo.targetSdkVersion;

                    // todo get network usage using appInfo.uid;
                    // appInfo.storageUuid;

                    dataList.add(pkgInfo);
                    pkgList.add(appInfo.packageName);
                }
            }
        }

        int flags = 0; // PackageManager.MATCH_ALL;
        List<PackageInfo> packList = packageManager.getInstalledPackages(flags);
        if (packList != null) {
            for (int idx = 0; idx < packList.size(); idx++) {
                PackageInfo packInfo = packList.get(idx);
                if (pkgList.contains(packInfo.packageName)) {
                    PkgInfo pkgInfo = findPkgInfo(packInfo.packageName);
                    if (pkgInfo != null)  {
                        pkgInfo.packInfo = packInfo;
                    }
                }
            }
        }

        action.done(this, PkgAction.STATUS_OK, "");
    }

    private PkgInfo findPkgInfo(String packageName) {
        for (PkgInfo pkgInfo : dataList) {
            if (packageName.equals(pkgInfo.pkgName)) {
                return pkgInfo;
            }
        }
        return null;
    }

    private static boolean isStopped(ApplicationInfo appInfo) {
        return (appInfo.flags & ApplicationInfo.FLAG_STOPPED /* 0x200000 */) != 0;
    }

    private static boolean isSystem(ApplicationInfo appInfo) {
        // return (appInfo.flags & ApplicationInfo.FLAG_SYSTEM /* 0x1 */) != 0;
        return false;
    }

    // =============================================================================================

    static class PkgInfo extends DataItem {
        String label;
        String processName;
        int targetSdkVersion;

        long pkgSize;
        PackageInfo packInfo;

        private static final SimpleDateFormat s_timeFormat = new SimpleDateFormat("MM/dd/yyyy  HH:mm");

        @NonNull
        public String toString() {

            // TODO - generate tabular columns, sortable, etc.
            
            String msg;
            if (pkgName.equals(processName)) {
                msg = label + "\n" + pkgName;
            } else {
                msg = label + "\n" + pkgName + "\n" + processName;
            }

            if (packInfo != null) {
                if (packInfo.firstInstallTime != packInfo.lastUpdateTime) {
                    msg += "\nInstall First " + s_timeFormat.format(packInfo.firstInstallTime);
                    msg += "\nInstall Last " + s_timeFormat.format(packInfo.lastUpdateTime);
                } else {
                    msg += "\nInstalled " + s_timeFormat.format(packInfo.firstInstallTime);
                }

                msg += "\nTarget SDK " + targetSdkVersion;
                msg += "\nVersion " + packInfo.versionName;
            }

            if (pkgSize != 0) {
                msg += "\nPackage Size " + NumberFormat.getNumberInstance(Locale.getDefault()).format(pkgSize);
            }
            return msg;
        }
    }


    // =============================================================================================

    static class PkgData {
        Drawable icon;
        String appName;
        ApplicationInfo appInfo;
    }

    Drawable getPackageIcon(String packageName) {
        if (pkgInventory == null) {
            loadPackageIcons(0);
        }

        Drawable icon = null;
        if (pkgInventory.containsKey(packageName)){
             icon = Objects.requireNonNull(pkgInventory.get(packageName)).icon;
        }
        return icon;
    }

    @SuppressWarnings({"UnusedReturnValue", "SameParameterValue"})
    int loadPackageIcons(int flags) {
        int addCnt = 0;
        List<PackageInfo> packList = packageManager.getInstalledPackages(flags);
        if (packList != null) {
            for (int idx = 0; idx < packList.size(); idx++) {
                PackageInfo packInfo = packList.get(idx);
                // boolean isSys = ((packInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0);

                if (!pkgInventory.containsKey(packInfo.packageName)) {
                    PkgData pkgData = new PkgData();
                    pkgData.icon = packInfo.applicationInfo.loadIcon(packageManager);
                    pkgData.appName =
                            packInfo.applicationInfo.loadLabel(packageManager).toString();
                    pkgData.appInfo = packInfo.applicationInfo;
                    pkgInventory.put(packInfo.packageName, pkgData);
                    addCnt++;
                }
            }
        }
        return addCnt;
    }
}
