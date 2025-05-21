/*
 * Copyright (c) 2020 Dennis Lang (LanDen Labs) landenlabs@gmail.com
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

package com.landenlabs.all_killbg;

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
