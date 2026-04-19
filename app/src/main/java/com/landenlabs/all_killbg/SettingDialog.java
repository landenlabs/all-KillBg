package com.landenlabs.all_killbg;

import static androidx.core.app.ActivityCompat.recreate;

import static com.landenlabs.all_killbg.AppConstants.APP_TAG;
import static com.landenlabs.all_killbg.AppConstants.PREF_THEME;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.ImageDecoder;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;

public class SettingDialog {

    public  void show(Activity activity) {
        View dialogView = activity.getLayoutInflater().inflate(R.layout.settings_dialog, null);
        final AlertDialog dialog = new AlertDialog.Builder(activity)
                .setView(dialogView)
                .create();

        ImageView aboutImage = dialogView.findViewById(R.id.about_anim_image);
        TextView versionTv = dialogView.findViewById(R.id.about_version);
        TextView buildTv = dialogView.findViewById(R.id.about_compile_date);
        RadioGroup themeGroup = dialogView.findViewById(R.id.theme_radio_group);

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        int savedTheme = prefs.getInt(PREF_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

        if (savedTheme == AppCompatDelegate.MODE_NIGHT_NO) {
            themeGroup.check(R.id.theme_light);
        } else if (savedTheme == AppCompatDelegate.MODE_NIGHT_YES) {
            themeGroup.check(R.id.theme_dark);
        } else {
            themeGroup.check(R.id.theme_auto);
        }

        themeGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                int mode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
                if (checkedId == R.id.theme_light)
                    mode = AppCompatDelegate.MODE_NIGHT_NO;
                else if (checkedId == R.id.theme_dark)
                    mode = AppCompatDelegate.MODE_NIGHT_YES;

                prefs.edit().putInt(PREF_THEME, mode).apply();
                setAppTheme(mode, activity);
                dialog.dismiss();
            }
        });

        // Set version and build info
        try {
            String versionName = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0).versionName;
            versionTv.setText("Version: " + versionName);
        } catch (Exception e) {
            versionTv.setText("Version: Unknown");
        }
        buildTv.setText("Built: " + new java.util.Date(BuildConfig.BuildTimeMilli).toString());

        // Handle animation
        if (Build.VERSION.SDK_INT >= 28) {
            try {
                Drawable decoded = ImageDecoder.decodeDrawable(
                        ImageDecoder.createSource(activity.getResources(), R.raw.landen_labs_anim));
                aboutImage.setImageDrawable(decoded);
                if (decoded instanceof Animatable) {
                    ((Animatable) decoded).start();
                }
            } catch (Exception e) {
                aboutImage.setImageResource(R.drawable.landen_labs_img);
            }
        } else {
            aboutImage.setImageResource(R.drawable.landen_labs_img);
        }

        dialogView.findViewById(R.id.about_close_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    public static void restoreAppTheme(@NonNull Activity activity) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        int savedTheme = prefs.getInt(PREF_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        if (savedTheme != AppCompatDelegate.getDefaultNightMode()) {
            AppCompatDelegate.setDefaultNightMode(savedTheme);
        }
    }

    public static void setAppTheme(int themeMode, @Nullable Activity activity) {
        Log.d(APP_TAG, "setAppTheme (auto=-1, 1=lite, 2=night) " + themeMode);
        AppCompatDelegate.setDefaultNightMode(themeMode);

        // Recreate the activity to apply the theme immediately
        if (activity != null)
            recreate(activity);   // done automatically by setDefaultNightMode()
    }
}
