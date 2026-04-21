package com.landenlabs.all_killbg;

import static android.view.accessibility.AccessibilityEvent.eventTypeToString;
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
    public void onAccessibilityEvent(AccessibilityEvent event) {
        Log.d(APP_TAG, "StopProc -  Event " + eventTypeToString(event.getEventType()));
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

    private boolean findAndClick(AccessibilityNodeInfo nodeInfo, String resId, String text) {
        if (nodeInfo == null) return false;

        // Try by ID first
        List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByViewId(resId);
        for (AccessibilityNodeInfo node : list) {
            if (node.isClickable() && node.isEnabled()) {
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                return true;
            }
        }

        // Fallback to text
        list = nodeInfo.findAccessibilityNodeInfosByText(text);
        for (AccessibilityNodeInfo node : list) {
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
