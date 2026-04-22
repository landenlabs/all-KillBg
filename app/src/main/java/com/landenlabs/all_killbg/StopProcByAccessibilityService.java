package com.landenlabs.all_killbg;

import static com.landenlabs.all_killbg.AppConstants.APP_TAG;

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

    private String prevEventFingerprint = "";
    private static boolean sIsRunning = false;
    public static int stopCnt = 0;

    public static void setRunning(boolean running) {
        sIsRunning = running;
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        AccessibilityServiceInfo info = getServiceInfo();
        info.flags |= AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;
        setServiceInfo(info);
        Log.d(APP_TAG, "Accessibility Service Connected - View ID Reporting Enabled");
    }

    private void doneWithEvent(AccessibilityEvent event, String msg) {
        // Log.i(APP_TAG, msg + " [Back Action]");
        sIsRunning = false;
        performGlobalAction(GLOBAL_ACTION_BACK);
    }

    private static void sleep(long milli) {
        try {
            Thread.sleep(milli);
        } catch (InterruptedException ignore) {  }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Log.d(APP_TAG, "StopProc - " + sIsRunning  + " Event " + eventTypeToString(event.getEventType()) + " pkg=" + event.getPackageName());
        if (!sIsRunning || event == null)
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
            rootNode.recycle();
            return;
        }

        // Update the cache and perform action
        prevEventFingerprint = currentFingerprint;

        try {
            // 1. Verify we are in Settings
            CharSequence packageName = rootNode.getPackageName();
            if (packageName == null || !packageName.toString().contains("settings")) {
                return; // Finally block will recycle rootNode
            }

            // 2. Handle "Force stop" and Confirmation Dialog
            AccessibilityNodeInfo node = findClickable(rootNode, null, "Force stop");
            if ( node != null ) {
                if (node.isEnabled()) {
                    // Log.w(APP_TAG, "StopProc - Force stop clicked");
                    node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    sleep(1000);    // Wait for click to popup confirmation, else will get double click.
                } else
                    doneWithEvent(event,  "StopProc - not running");
                return;  // if force stop clicked, wait for next event to re-enter this method.
            }
            if (findAndClick(rootNode, "android:id/button1", "OK")) {
                stopCnt++;
                sleep(1000);
                doneWithEvent(event,"StopProc - stopped, cnt=" + stopCnt);
            }
        } finally {
            // The ONLY place you need to recycle.
            // This clears the root and all children fetched from it.
            rootNode.recycle();
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

    private AccessibilityNodeInfo findClickable(AccessibilityNodeInfo nodeInfo, @Nullable String resId, @NonNull String text) {
        if (nodeInfo == null)
            return null;

        List<AccessibilityNodeInfo> list;
        list = nodeInfo.findAccessibilityNodeInfosByText(text);
        for (AccessibilityNodeInfo node : list) {
            if (node.isClickable() ) {
                Log.i(APP_TAG, "StopProc - found '" + text + "'");
                return node;
            }
        }

        // Try by ID last
        if (resId != null) {
            list = nodeInfo.findAccessibilityNodeInfosByViewId(resId);
            for (AccessibilityNodeInfo node : list) {
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
