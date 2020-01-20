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
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

/**
 * Show Details about a process.
 */
public class ShowDetailActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        Intent intent = getIntent();

        String processId = intent.getStringExtra("EXTRA_PROCESS_PID");
        String processUid = intent.getStringExtra("EXTRA_PROCESS_UID");
        String processName = intent.getStringExtra("EXTRA_PROCESS_NAME");
        String processImportance = intent.getStringExtra("EXTRA_PROCESS_IMPORTANCE");
        String processImportanceReasonCode =
                intent.getStringExtra("EXTRA_PROCESS_IMPORTANCE_REASON_CODE");
        String processImportanceReasonPid =
                intent.getStringExtra("EXTRA_PROCESS_IMPORTANCE_REASON_PID");
        String processLru = intent.getStringExtra("EXTRA_PROCESS_LRU");
        String[] pkgNameList = intent.getStringArrayExtra("EXTRA_PKGNAMELIST");

        String describe = "";

        try {
            switch (Integer.parseInt(processImportance)) {
                case 400:
                    describe = "IMPORTANCE_BACKGROUND";
                    break;
                case 500:
                    describe = "IMPORTANCE_EMPTY";
                    break;
                case 100:
                    describe = "IMPORTANCE_FOREGROUND";
                    break;
                case 130:
                    describe = "IMPORTANCE_PERCEPTIBLE";
                    break;
                case 300:
                    describe = "IMPORTANCE_SERVICE";
                    break;
                case 200:
                    describe = "IMPORTANCE_VISIBLE";
                    break;
                default:
                    describe = "";
                    break;
            }
        } catch (NumberFormatException ex) {
            describe = "";
        }

        setText(Ui.viewById(this, R.id.detailPid), processId);
        setText(Ui.viewById(this, R.id.detailUid), processUid);
        setText(Ui.viewById(this, R.id.detailName), processName);
        setText(Ui.viewById(this, R.id.detailImportance), processImportance + "\n" + describe);
        setText(Ui.viewById(this, R.id.detailImportanceReasonCode), processImportanceReasonCode);
        setText(Ui.viewById(this, R.id.detailImportanceReasonPid), processImportanceReasonPid);
        setText(Ui.viewById(this, R.id.detailLru), processLru);

        StringBuilder packageList = new StringBuilder();
        if (pkgNameList != null) {
            for (String item : pkgNameList)
                packageList.append(item).append("\n");
        }
        setText(Ui.<TextView>viewById(this, R.id.detailPkgNameList), packageList.toString());
    }

    private static void setText(TextView textView, String msg) {
        if (textView != null && msg != null) {
            textView.setText(msg);
        }
    }
}
