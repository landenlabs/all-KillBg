package com.landenlabs.all_killbg;

/*
 * Copyright (C) 2020 Dennis Lang (landenlabs@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class KillService extends Service {
    private ArrayList<String> blackName;
    private ActivityManager myActivityManagerService;

    private static final int SRV_KILL_MSG = 0x1233;


    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        myActivityManagerService = (ActivityManager) getSystemService(Activity.ACTIVITY_SERVICE);
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        int size = pref.getInt("Status_size", 0);

        blackName = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            blackName.add(pref.getString("Status_" + i, null));
        }
        Toast.makeText(this, "Service is Started", Toast.LENGTH_LONG).show();
    }

    @Override
    public int onStartCommand(Intent intent, int flag, int startid) {
        Toast.makeText(this, "Service is Running", Toast.LENGTH_LONG).show();
        final Handler myHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == SRV_KILL_MSG) {
                    for (int i = 0; i < blackName.size(); i++) {
                        myActivityManagerService.killBackgroundProcesses(blackName.get(i));
                    }
                }
            }
        };
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                myHandler.sendEmptyMessage(SRV_KILL_MSG);
            }
        }, 0, TimeUnit.MINUTES.toMillis(30));
        return START_STICKY;

    }

}
