package com.landenlabs.all_killbg;

import static android.view.accessibility.AccessibilityEvent.eventTypeToString;
import static com.landenlabs.all_killbg.AppConstants.APP_TAG;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * Accessibility service to automate "Force Stop" in app settings.
 */
public class StopProcByAccessibilityService extends AccessibilityService {

    private static boolean sIsRunning = false;

    public static void setRunning(boolean running) {
        sIsRunning = running;
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        AccessibilityServiceInfo info = getServiceInfo();

        // Add the flag to the existing flags
        info.flags |= AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;

        setServiceInfo(info);
        Log.d(APP_TAG, "Accessibility Service Connected - View ID Reporting Enabled");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Log.i(APP_TAG, "StopProc -  isRunning=" + sIsRunning);

        if (!sIsRunning || event == null) return;

        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) return;

        try {
            // 1. Verify we are in Settings
            CharSequence packageName = rootNode.getPackageName();
            if (packageName == null || !packageName.toString().contains("settings")) {
                return; // Finally block will recycle rootNode
            }

            Log.d(APP_TAG, "StopProc -  Event " + eventTypeToString(event.getEventType()));

            // 2. Handle Confirmation Dialog
            // Note: findAndClick should NOT recycle nodes internally to be safe here
            AccessibilityNodeInfo node = findClickable(rootNode, null, "Force stop");
            if ( node != null && ! node.isEnabled()) {
                Log.i(APP_TAG, "StopProc - not running");
                performGlobalAction(GLOBAL_ACTION_BACK);
                return;
            }
            boolean clickedConfirmation = findAndClick(rootNode, "android:id/button1", "OK") ||
                    findAndClick(rootNode, null, "Force stop");
            //         findAndClick(rootNode, "android:id/button1", "Force stop");

            if (clickedConfirmation) {
                Log.i(APP_TAG, "StopProc - Confirmed kill.");
                performGlobalAction(GLOBAL_ACTION_BACK);
                return;
            }

            Log.d(APP_TAG, "--- START NODE DUMP ---");
            debugDumpNodes(rootNode, 0);
            Log.d(APP_TAG, "--- END NODE DUMP ---");

            // 3. Handle Main Page
            List<AccessibilityNodeInfo> stopButtons = rootNode.findAccessibilityNodeInfosByViewId(
                    "com.android.settings:id/force_stop_button");

            Log.d(APP_TAG, "StopProc - Nodes found: " + (stopButtons != null ? stopButtons.size() : "null"));

            if (stopButtons == null || stopButtons.isEmpty()) {
                stopButtons = rootNode.findAccessibilityNodeInfosByText("Force stop");
            }

            if (stopButtons != null && !stopButtons.isEmpty()) {
                AccessibilityNodeInfo stopButton = stopButtons.get(0);
                if (stopButton.isEnabled()) {
                    stopButton.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    Log.i(APP_TAG, "StopProc - Clicked Force Stop");
                } else {
                    Log.i(APP_TAG, "StopProc - Button disabled (App Stopped).");
                    performGlobalAction(GLOBAL_ACTION_BACK);
                    sIsRunning = false;
                }
                // We don't need to manually recycle stopButton if we recycle rootNode
            } else {
                // If we've been on this page for a while and find nothing, go back
                Log.d(APP_TAG, "StopProc - No button found.");
                performGlobalAction(GLOBAL_ACTION_BACK);
            }

        } finally {
            // The ONLY place you need to recycle.
            // This clears the root and all children fetched from it.
            rootNode.recycle();
        }
    }

    /*
    // @Override
    public void onAccessibilityEvent2(AccessibilityEvent event) {
        Log.i(APP_TAG, "StopProc -  isRunning=" + sIsRunning);
        Log.d(APP_TAG, "StopProc -  Event " + eventTypeToString(event.getEventType()));

        if (!sIsRunning || !Locale.getDefault().getLanguage().equals("en")) {
            return;
        }

        AccessibilityNodeInfo nodeInfo = event.getSource();
        if (nodeInfo == null) {
            nodeInfo = getRootInActiveWindow();
        }
        if (nodeInfo == null) return;

        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode != null) {
            Log.d(APP_TAG, "--- START NODE DUMP ---");
            debugDumpNodes(rootNode, 0);
            Log.d(APP_TAG, "--- END NODE DUMP ---");

            // ... rest of your logic ...
            rootNode.recycle();
        }

        // 1. Handle the Confirmation Dialog (The "OK" Button)
        // We check this first because the dialog is a "top-most" window event.
        boolean clickedOk = findAndClick(nodeInfo, "android:id/button1", "OK") ||
                findAndClick(nodeInfo, "android:id/button1", "Force stop");

        // Log.d(APP_TAG, "StopProc Event clickedOk: " + clickedOk + " " +  event.getPackageName());


        // 2. Handle the App Info Page (The "Force stop" Button)
        // If we didn't just click OK, look for the main button.
        if (!clickedOk) {
            // Find the "Force stop" button
            List<AccessibilityNodeInfo> buttons = nodeInfo.findAccessibilityNodeInfosByViewId(
                    "com.android.settings:id/force_stop_button");

            if (buttons != null && !buttons.isEmpty()) {
                AccessibilityNodeInfo stopButton = buttons.get(0);

                if (stopButton.isEnabled()) {
                    // Button is active -> App is running -> Click it
                    stopButton.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                } else {
                    // IMPORTANT: If the button is DISABLED, it means the app is already stopped.
                    // This is our cue to exit this screen so the next app can be processed.
                    Log.d(APP_TAG, "StopProc -  App is stopped (Button disabled). Going back.");
                    performGlobalAction(GLOBAL_ACTION_BACK);

                    // Optional: Short sleep or flag reset to prevent rapid-fire back actions
                    // sIsRunning = false;
                }
            }
        }

        Log.d(APP_TAG, "StopProc -  Event done");
        performGlobalAction(GLOBAL_ACTION_BACK);
        sIsRunning = false;
    }

    // @Override
    public void onAccessibilityEvent3(AccessibilityEvent event) {
        Log.i(APP_TAG, "StopProc -  isRunning=" + sIsRunning);
        // Log.d(APP_TAG, "StopProc -  Event " + eventTypeToString(event.getEventType()));
        if (!sIsRunning) {
            return;
        }

        // Only automate if English locale
        if (!Locale.getDefault().getLanguage().equals("en")) {
            Log.e(APP_TAG, "StopProc -  Not in English locale");
            return;
        }

        // Prioritize the event source node
        AccessibilityNodeInfo nodeInfo = event.getSource();
        if (nodeInfo == null) {
            Log.e(APP_TAG, "StopProc -  missing nodeInfo");
            return;
        }

        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode != null) {
            Log.d(APP_TAG, "--- START NODE DUMP ---");
            debugDumpNodes(rootNode, 0);
            Log.d(APP_TAG, "--- END NODE DUMP ---");

            // ... rest of your logic ...
            rootNode.recycle();
        }


        // 1. Try to find and click "Force stop"
        if (findAndClick(nodeInfo, "com.android.settings:id/force_stop_button", "Force stop")) {
            // After clicking "Force stop", look for the "OK" button in the confirmation dialog.
            // Dialog events might come in a separate event, but sometimes they are part of the same window
            Log.i(APP_TAG, "StopProc -  force stop pressed " + nodeInfo.getPackageName());
        }

        // 2. Try to find and click "OK" or "Force stop" confirmation.
        // Confirmation buttons often have ID "android:id/button1" or text "OK" / "Force stop"
        if (findAndClick(nodeInfo, "android:id/button1", "OK") || 
            findAndClick(nodeInfo, "android:id/button1", "Force stop")) {
            Log.i(APP_TAG, "StopProc -  ok pressed " + nodeInfo.getPackageName());
        }

        sIsRunning = false; // Automation done for this app
        performGlobalAction(GLOBAL_ACTION_BACK);
        Log.d(APP_TAG, "StopProc -  done ");
        // nodeInfo.recycle();

        // Note: Do not recycle nodeInfo if it's the event source; the system handles that.
        // But we should recycle it if it came from getRootInActiveWindow.
        // For simplicity and safety in onAccessibilityEvent, we rely on system cleanup for the source.
    }

    private boolean findAndClick2(AccessibilityNodeInfo nodeInfo, String resId, String text) {
        if (nodeInfo == null) return false;

        List<AccessibilityNodeInfo> nodes = nodeInfo.findAccessibilityNodeInfosByViewId(resId);
        if (nodes.isEmpty()) {
            nodes = nodeInfo.findAccessibilityNodeInfosByText(text);
        }

        for (AccessibilityNodeInfo node : nodes) {
            if (node.isEnabled() && node.isClickable()) {
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                // Recycle all nodes in the list
                for (AccessibilityNodeInfo n : nodes) n.recycle();
                Log.d(APP_TAG, "StopProc clicked " + text);
                return true;
            }
        }

        for (AccessibilityNodeInfo n : nodes) n.recycle();
        return false;
    }
    */

    private boolean findAndClick(AccessibilityNodeInfo nodeInfo, @Nullable String resId, @NonNull String text) {
        AccessibilityNodeInfo node = findClickable(nodeInfo, resId, text);
        if (node != null && node.isEnabled()) {
            Log.i(APP_TAG, "StopProc - click " + text);
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
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
}
