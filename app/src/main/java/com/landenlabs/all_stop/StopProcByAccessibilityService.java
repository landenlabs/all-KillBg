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

import static com.landenlabs.all_stop.AppConstants.APP_TAG;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

/**
 * Accessibility service to automate "Force Stop" in app settings.
 */
public class StopProcByAccessibilityService extends AccessibilityService {

    public enum ServiceMode { STOP, SCAN }
    private static ServiceMode sMode = ServiceMode.STOP;
    private static final List<String> sRunningPackages = new java.util.ArrayList<>();
    private static String sCurrentInspectedPackage = null;

    private String prevEventFingerprint = "";
    private static boolean sIsRunning = false;
    public static int stopCnt = 0;

    public static void setRunning(boolean running, ServiceMode mode) {
        sIsRunning = running;
        sMode = mode;
        if (running && mode == ServiceMode.SCAN) {
            sRunningPackages.clear();
        }
    }

    public static void setCurrentInspectedPackage(String pkg) {
        sCurrentInspectedPackage = pkg;
    }

    public static List<String> getRunningPackages() {
        return new java.util.ArrayList<>(sRunningPackages);
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        AccessibilityServiceInfo info = getServiceInfo();
        info.flags |= AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;
        setServiceInfo(info);
        Log.d(APP_TAG, "Accessibility Service Connected - View ID Reporting Enabled");
    }

    private void doneWithEvent(@NonNull AccessibilityEvent event, @NonNull String msg) {
        Log.i(APP_TAG, msg + " [Back Action]");
        sIsRunning = false;
        sleep(1000);
        performGlobalAction(GLOBAL_ACTION_BACK);
        sleep(1000);
    }

    private static void sleep(long milli) {
        try {
            Thread.sleep(milli);
        } catch (InterruptedException ignore) {  }
    }

    @Override
    public void onAccessibilityEvent(@NonNull AccessibilityEvent event) {
        // Log.d(APP_TAG, "StopProc - " + sIsRunning  + " Event " + eventTypeToString(event.getEventType()) + " pkg=" + event.getPackageName());
        if (!sIsRunning)
            return;

        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null)
            return;

        // Build a unique key (Ignoring Time)
        String currentFingerprint = event.getPackageName() + ":" +
                event.getEventType() + ":" +
                rootNode.getViewIdResourceName() + ":" +
                rootNode.getText();

        // Compare with the last one
        if (currentFingerprint.equals(prevEventFingerprint)) {
            // It's a duplicate or a "chattering" event - discard it
            // Log.d(APP_TAG, "StopProc - ignore dup event");
            return;
        }

        // Update the cache and perform action
        prevEventFingerprint = currentFingerprint;

        // 1. Verify we are in Settings
        CharSequence packageName = rootNode.getPackageName();
        if (packageName == null || !packageName.toString().contains("settings")) {
            return;
        }

        // 2. Handle "Force stop" and Confirmation Dialog
        AccessibilityNodeInfo node = findClickable(rootNode, null, "Force stop");
        if ( node != null ) {
            if (node.isEnabled()) {
                if (sMode == ServiceMode.SCAN) {
                    if (sCurrentInspectedPackage != null && !sRunningPackages.contains(sCurrentInspectedPackage)) {
                        sRunningPackages.add(sCurrentInspectedPackage);
                    }
                    doneWithEvent(event, "StopProc - scanning, found running: " + sCurrentInspectedPackage);
                } else {
                    // Log.w(APP_TAG, "StopProc - Force stop clicked");
                    node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    sleep(1000);    // Wait for click to popup confirmation, else will get double click.
                }
            } else
                doneWithEvent(event,  "StopProc - not running");
            return;  // if force stop clicked, wait for next event to re-enter this method.
        }
        if (sMode == ServiceMode.STOP && findAndClick(rootNode, "android:id/button1", "OK")) {
            stopCnt++;
            doneWithEvent(event,"StopProc - stopped, cnt=" + stopCnt);
        }
    }

    private boolean findAndClick(AccessibilityNodeInfo nodeInfo, @Nullable String resId, @NonNull String text) {
        AccessibilityNodeInfo node = findClickable(nodeInfo, resId, text);
        if (node != null && node.isEnabled()) {
            Log.w(APP_TAG, "StopProc - click '" + text + "'");
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            return true;
        }

        return false;
    }

    @Nullable
    private AccessibilityNodeInfo findClickable(@Nullable AccessibilityNodeInfo nodeInfo, @Nullable String resId, @NonNull String text) {
        if (nodeInfo == null)
            return null;

        List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByText(text);
        for (AccessibilityNodeInfo node : list) {
            if (node.isClickable() ) {
                Log.i(APP_TAG, "StopProc - found '" + text + "'");
                return node;
            }
        }

        // Try by ID last
        if (resId != null) {
            List<AccessibilityNodeInfo> idList = nodeInfo.findAccessibilityNodeInfosByViewId(resId);
            for (AccessibilityNodeInfo node : idList) {
                if (node.isClickable() ) {
                    Log.i(APP_TAG, "StopProc - found " + resId);
                    return node;
                }
            }
        }

        return null;
    }

    @Override
    public void onInterrupt() {
        sIsRunning = false;
    }

    /*
    private void debugDumpNodes(AccessibilityNodeInfo node, int depth) {
        if (node == null)
            return;

        String id = node.getViewIdResourceName();
        CharSequence text = node.getText();
        CharSequence contentDesc = node.getContentDescription();
        String className = node.getClassName().toString();

        if (node.isClickable() && hasText(id, text)) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < depth; i++)
                sb.append("  "); // Indentation
            id = ifNullEmpty(id);
            text = ifNullEmpty(text);
            sb.append(String.format("[%s] ID: %s | Text: %s | Desc: %s | Clickable: %b | Enabled: %b", className, id, text, contentDesc, node.isClickable(), node.isEnabled()));
            Log.d(APP_TAG, sb.toString());
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            debugDumpNodes(node.getChild(i), depth + 1);
        }
    }

    private static boolean hasText(CharSequence ... texts) {
        for (CharSequence text : texts) {
            if (text != null && text.length() > 0)
                return true;
        }
        return false;
    }
    private static String ifNullEmpty(CharSequence text) {
        return (text == null) ? "" : text.toString();
    }
     */
}
