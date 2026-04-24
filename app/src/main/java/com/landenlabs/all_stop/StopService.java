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

import android.app.ActivityManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class StopService extends Service {
    private final List<String> safeList = new ArrayList<>();
    private ActivityManager activityManager;

    private static final String TAG = "StopService";

    @Nullable
    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        loadSafeList();
        
        Log.i(TAG, "Service Created");
    }

    @Override
    public int onStartCommand(Intent intent, int flag, int startid) {
        Log.i(TAG, "Service Started");
        
        final Handler handler = new Handler(Looper.getMainLooper());
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                handler.post(() -> {
                    Log.d(TAG, "Periodic Stop check running");
                    for (String pkg : safeList) {
                        AppUtils.stopProcess(StopService.this, activityManager, pkg);
                    }
                });
            }
        }, 0, TimeUnit.MINUTES.toMillis(30));
        
        return START_STICKY;
    }

    private void loadSafeList() {
        SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(this);
        Set<String> set = preference.getStringSet("stop_list", null);
        safeList.clear();
        if (set != null) {
            safeList.addAll(set);
        }
    }
}
