package com.landenlabs.all_killbg;

/*
 * Copyright (C) 2020 Dennis Lang (landenlabs@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
                    dataList.add(pkgInfo);
                    pkgList.add(appInfo.packageName);
                }
            }
        }

        action.done(this, PkgAction.STATUS_OK, "");
    }

    private static boolean isStopped(ApplicationInfo appInfo) {
        return (appInfo.flags & ApplicationInfo.FLAG_STOPPED /* 0x200000 */) != 0;
    }

    private static boolean isSystem(ApplicationInfo appInfo) {
        return (appInfo.flags & ApplicationInfo.FLAG_SYSTEM /* 0x1 */) != 0;
    }

    // =============================================================================================

    static class PkgInfo extends DataItem {
        String label;
        String processName;

        @NonNull
        public String toString() {
            return label + "\n" + pkgName + "\n" + processName;
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
