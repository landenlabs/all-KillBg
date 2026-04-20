package com.landenlabs.all_killbg;

import static com.landenlabs.all_killbg.AppConstants.APP_TAG;

import android.accessibilityservice.AccessibilityService;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

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


    // @Override
    public void onAccessibilityEvent2(AccessibilityEvent event) {
        if (!sIsRunning || !Locale.getDefault().getLanguage().equals("en")) {
            return;
        }

        AccessibilityNodeInfo nodeInfo = event.getSource();
        if (nodeInfo == null) {
            nodeInfo = getRootInActiveWindow();
        }
        if (nodeInfo == null) return;


        // 1. Handle the Confirmation Dialog (The "OK" Button)
        // We check this first because the dialog is a "top-most" window event.
        boolean clickedOk = findAndClick(nodeInfo, "android:id/button1", "OK") ||
                findAndClick(nodeInfo, "android:id/button1", "Force stop");

        // Log.d(APP_TAG, "StopProc Event clickedOk: " + clickedOk + " " +  event.getPackageName());


        // 2. Handle the App Info Page (The "Force stop" Button)
        // If we didn't just click OK, look for the main button.
        if (!clickedOk) {
            // Find the "Force stop" button
            List<AccessibilityNodeInfo> buttons = nodeInfo.findAccessibilityNodeInfosByViewId("com.android.settings:id/force_stop_button");

            if (buttons != null && !buttons.isEmpty()) {
                AccessibilityNodeInfo stopButton = buttons.get(0);

                if (stopButton.isEnabled()) {
                    // Button is active -> App is running -> Click it
                    stopButton.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                } else {
                    // IMPORTANT: If the button is DISABLED, it means the app is already stopped.
                    // This is our cue to exit this screen so the next app can be processed.
                    Log.d(APP_TAG, "App is stopped (Button disabled). Going back.");
                    performGlobalAction(GLOBAL_ACTION_BACK);

                    // Optional: Short sleep or flag reset to prevent rapid-fire back actions
                    // sIsRunning = false;
                }
            }
        }

        Log.d(APP_TAG, "StopProc Event done");
        performGlobalAction(GLOBAL_ACTION_BACK);
        sIsRunning = false;
    }

    // @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        Log.d(APP_TAG, "StopProc Event " +  event);
        if (!sIsRunning) {
            return;
        }

        // Only automate if English locale
        if (!Locale.getDefault().getLanguage().equals("en")) {
            return;
        }

        // Prioritize the event source node
        AccessibilityNodeInfo nodeInfo = event.getSource();
        if (nodeInfo == null) {
            // Fallback to active window if source is null
            nodeInfo = getRootInActiveWindow();
        }
        
        if (nodeInfo == null) {
            return;
        }

        // 1. Try to find and click the initial "Force stop" button
        // We use the specific settings ID and common text variations.
        boolean clickedForceStop = findAndClick(nodeInfo, "com.android.settings:id/force_stop_button", "Force stop");
        if (!clickedForceStop) {
             // Try a broader search if the specific ID wasn't found in this node sub-tree
             AccessibilityNodeInfo root = getRootInActiveWindow();
             if (root != null && root != nodeInfo) {
                 clickedForceStop = findAndClick(root, "com.android.settings:id/force_stop_button", "Force stop");
                 root.recycle();
             }
        }

        // 2. Try to find and click "OK" or confirmation "Force stop" in the dialog
        // This is usually in a separate dialog window, so we check both the current node and root.
        boolean clickedOk = findAndClick(nodeInfo, "android:id/button1", "OK") || 
                           findAndClick(nodeInfo, "android:id/button1", "Force stop") ||
                           findAndClick(nodeInfo, "android:id/button1", "FORCE STOP");
        
        if (!clickedOk) {
            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (root != null && root != nodeInfo) {
                clickedOk = findAndClick(root, "android:id/button1", "OK") || 
                            findAndClick(root, "android:id/button1", "Force stop") ||
                            findAndClick(root, "android:id/button1", "FORCE STOP");
                root.recycle();
            }
        }

        // if (clickedOk) {
            sIsRunning = false; // Automation complete
            performGlobalAction(GLOBAL_ACTION_BACK);
        // }
        
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

    private boolean findAndClick(AccessibilityNodeInfo nodeInfo, String resId, String text) {
        if (nodeInfo == null) return false;

        // Try by ID first
        List<AccessibilityNodeInfo> idMatches = nodeInfo.findAccessibilityNodeInfosByViewId(resId);
        for (AccessibilityNodeInfo node : idMatches) {
            if (node.isClickable() && node.isEnabled()) {
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                return true;
            }
        }

        // Fallback to text search
        List<AccessibilityNodeInfo> textMatches = nodeInfo.findAccessibilityNodeInfosByText(text);
        for (AccessibilityNodeInfo node : textMatches) {
            // Ensure we are clicking the actual button (some descriptions might contain the text)
            if (node.isClickable() && node.isEnabled()) {
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                return true;
            }
        }
        return false;
    }

    @Override
    public void onInterrupt() {
        sIsRunning = false;
    }
}
