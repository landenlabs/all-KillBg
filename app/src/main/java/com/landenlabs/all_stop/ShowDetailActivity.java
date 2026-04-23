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

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Show Details about a process.
 */
public class ShowDetailActivity extends AppCompatActivity {

    public static final String EXTRA_PROCESS_PID = "EXTRA_PROCESS_PID";
    public static final String EXTRA_PROCESS_UID = "EXTRA_PROCESS_UID";
    public static final String EXTRA_PROCESS_NAME = "EXTRA_PROCESS_NAME";
    public static final String EXTRA_PROCESS_IMPORTANCE = "EXTRA_PROCESS_IMPORTANCE";
    public static final String EXTRA_PROCESS_IMPORTANCE_REASON_CODE = "EXTRA_PROCESS_IMPORTANCE_REASON_CODE";
    public static final String EXTRA_PROCESS_IMPORTANCE_REASON_PID = "EXTRA_PROCESS_IMPORTANCE_REASON_PID";
    public static final String EXTRA_PROCESS_LRU = "EXTRA_PROCESS_LRU";
    public static final String EXTRA_PKGNAMELIST = "EXTRA_PKGNAMELIST";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        Intent intent = getIntent();
        if (intent == null) return;

        String processId = intent.getStringExtra(EXTRA_PROCESS_PID);
        String processUid = intent.getStringExtra(EXTRA_PROCESS_UID);
        String processName = intent.getStringExtra(EXTRA_PROCESS_NAME);
        String processImportance = intent.getStringExtra(EXTRA_PROCESS_IMPORTANCE);
        String processImportanceReasonCode = intent.getStringExtra(EXTRA_PROCESS_IMPORTANCE_REASON_CODE);
        String processImportanceReasonPid = intent.getStringExtra(EXTRA_PROCESS_IMPORTANCE_REASON_PID);
        String processLru = intent.getStringExtra(EXTRA_PROCESS_LRU);
        String[] pkgNameList = intent.getStringArrayExtra(EXTRA_PKGNAMELIST);

        String describe = "";
        try {
            if (processImportance != null) {
                describe = AppProcessManager.ProcInfo.getImportance(Integer.parseInt(processImportance));
            }
        } catch (NumberFormatException ignore) {}

        setText(findViewById(R.id.detailPid), processId);
        setText(findViewById(R.id.detailUid), processUid);
        setText(findViewById(R.id.detailName), processName);
        setText(findViewById(R.id.detailImportance), processImportance + "\n" + describe);
        setText(findViewById(R.id.detailImportanceReasonCode), processImportanceReasonCode);
        setText(findViewById(R.id.detailImportanceReasonPid), processImportanceReasonPid);
        setText(findViewById(R.id.detailLru), processLru);

        if (pkgNameList != null) {
            StringBuilder packageList = new StringBuilder();
            for (String item : pkgNameList) {
                packageList.append(item).append("\n");
            }
            setText(findViewById(R.id.detailPkgNameList), packageList.toString());
        }
    }

    private void setText(TextView textView, @Nullable String msg) {
        if (textView != null && msg != null) {
            textView.setText(msg);
        }
    }
}
