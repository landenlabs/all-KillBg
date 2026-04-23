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

import static androidx.core.app.ActivityCompat.recreate;
import static com.landenlabs.all_stop.AppConstants.APP_TAG;
import static com.landenlabs.all_stop.AppConstants.PREF_THEME;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.ImageDecoder;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

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

        themeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            int mode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
            if (checkedId == R.id.theme_light) {
                mode = AppCompatDelegate.MODE_NIGHT_NO;
            } else if (checkedId == R.id.theme_dark) {
                mode = AppCompatDelegate.MODE_NIGHT_YES;
            }

            prefs.edit().putInt(PREF_THEME, mode).apply();
            setAppTheme(mode, activity);
            dialog.dismiss();
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

        dialogView.findViewById(R.id.settings_accessibility_link).setOnClickListener(v -> {
            Intent intent = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
            activity.startActivity(intent);
        });

        dialogView.findViewById(R.id.about_close_btn).setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    public static void restoreAppTheme(@NonNull Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int savedTheme = prefs.getInt(PREF_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        Log.d(APP_TAG, "restoreAppTheme (auto=-1, 1=lite, 2=night) " + savedTheme);
        if (savedTheme != AppCompatDelegate.getDefaultNightMode()) {
            setAppTheme(savedTheme, null);
        }
    }

    /**
     * Activity must by an extension of AppCompatActivity.
     */
    public static void setAppTheme(int themeMode, @Nullable Activity activity) {
        Log.d(APP_TAG, "setAppTheme (auto=-1, 1=lite, 2=night) " + themeMode);
        AppCompatDelegate.setDefaultNightMode(themeMode);


        // Recreate the activity to apply the theme immediately
        if (activity != null)
            recreate(activity);   // done automatically by setDefaultNightMode()
    }
}
