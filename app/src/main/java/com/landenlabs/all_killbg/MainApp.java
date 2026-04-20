package com.landenlabs.all_killbg;

import static com.landenlabs.all_killbg.AppConstants.APP_TAG;
import static com.landenlabs.all_killbg.SettingDialog.restoreAppTheme;

import android.app.Application;
import android.util.Log;

import androidx.appcompat.app.AppCompatDelegate;

public class MainApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // This ensures the theme is set globally before any
        // Activity (like MainActivity) tries to inflate its resources.
        // This prevents the "stale resource" bug you saw in onCreate.

        int mode =  AppCompatDelegate.getDefaultNightMode();
        Log.d(APP_TAG, "AppTheme (auto=-1, 1=lite, 2=night) Before:" + mode);
        restoreAppTheme(this);
        mode = AppCompatDelegate.getDefaultNightMode();

        Log.d(APP_TAG, "AppTheme (auto=-1, 1=lite, 2=night) After" + mode);
    }


}