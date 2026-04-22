package com.landenlabs.all_killbg;

import static com.landenlabs.all_killbg.SettingDialog.restoreAppTheme;

import android.app.Application;

public class MainApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // This ensures the theme is set globally before any
        // Activity (like MainActivity) tries to inflate its resources.
        // This prevents the "stale resource" bug you saw in onCreate.
        restoreAppTheme(this);
    }
}