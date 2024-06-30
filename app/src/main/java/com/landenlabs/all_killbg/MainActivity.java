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
 * @see https://landenLabs.com/
 */

package com.landenlabs.all_killbg;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.format.Formatter;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


import com.landenlabs.all_killbg.AppPackageManager.PkgInfo;
import com.landenlabs.all_killbg.AppProcessManager.ProcInfo;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("Convert2Lambda")
public class MainActivity extends Activity {

    private static final int EXIT = 0x113;
    private static final int TASK = 0x117;
    private static final int INTENT_APP_DETAILS = 1;

    // ---------------------------------------------------------------------------------------------
    private static int lastListIndex = 0;
    private static int lastTopOff = 0;
    private static int focusID = 0;
    private static int selectedPos = 0;

    private ActivityManager myActivityManager;
    private AppProcessManager appProcessManager;
    private AppPackageManager appPackageManager;

    private final ArrayList<String> killList = new ArrayList<>();

    private enum DisplayType {Packages, Processes}
    private DisplayType displayType = DisplayType.Packages;
    private ListAdapter arrayAdapter;
    private ListView dataList;
    private TextView leftStatusTv;
    private TextView rightStatusTv;


    // =============================================================================================
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Menu not working.
        //   setHasOptionsMenu(true);

        rightStatusTv = findViewById(R.id.rightStatus);
        leftStatusTv = findViewById(R.id.leftStatus);
        dataList = findViewById(R.id.dataList);
        myActivityManager = (ActivityManager) getSystemService(Activity.ACTIVITY_SERVICE);
        appPackageManager = new AppPackageManager(this);
        appProcessManager = new AppProcessManager(this);

        Ui.viewById(this, R.id.show_pkg).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                displayType = DisplayType.Packages;
                updateList();
            }
        });

        Ui.viewById(this, R.id.show_proc).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                displayType = DisplayType.Processes;
                updateList();
            }
        });

        Ui.viewById(this, R.id.kill_all).setOnClickListener(new OnClickListener() {
            public void onClick(View source) {
                appProcessManager.killAllBackgroundProcesses();
                updateList();
            }
        });

        Ui.viewById(this, R.id.killService).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                final Intent intentKillService = new Intent(MainActivity.this, KillService.class);
                startService(intentKillService);
            }
        });

        Ui.viewById(this, R.id.killListMgr).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                final Intent intentKillList = new Intent(MainActivity.this, KillListActivity.class);
                startActivity(intentKillList);
            }
        });


        // TODO - enable strict rules

        appPackageManager.loadPackageIcons(0);  // Should this be async
        loadKillList(this);
        setupListView();
    }

    // ---------------------------------------------------------------------------------------------

    @Override
    protected void onStart() {
        super.onStart();
        updateList();
        restoreState();
    }

    // ---------------------------------------------------------------------------------------------

    @Override
    protected void onStop() {
        saveState();
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        saveKillList();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle inState) {
        super.onRestoreInstanceState(inState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //  menu.add(0, TASK, 0, "Task");
        // TODO - add "about" menu
        menu.add(0, EXIT, 0, "Exit");
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem mi) {
        switch (mi.getItemId()) {
            case TASK:
                break;
            case EXIT:
                finish();
                break;
        }
        return true;
    }

    /*
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        Log.d("DDD", "dispatchKey=" + event.toString());
        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d("DDD", "onKeyDown=" + event.toString());
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Log.d("DDD", "onTouchEvent=" + event.toString());
        return super.onTouchEvent(event);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        Log.d("DDD", "dispatchTouchEvent=" + ev.toString());
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent ev) {
        Log.d("DDD", "dispatchGenericMotionEvent=" + ev.toString());
        return super.dispatchGenericMotionEvent(ev);
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        Log.d("DDD", "onGenericMotionEvent=" + event.toString());
        return super.onGenericMotionEvent(event);
    }
     */

// ----- Private

    // Save list state, position and focus
    private void saveState() {
        // Save list scroll state
        lastListIndex = dataList.getFirstVisiblePosition();
        View v = dataList.getChildAt(lastListIndex);
        lastTopOff = (v == null) ? 0 : v.getTop();

        // Save focus and list selected state.
        View focusedChild = getCurrentFocus();
        if (focusedChild != null) {
            focusID = focusedChild.getId();
            selectedPos = 0;
            if (focusedChild instanceof ListView) {
                selectedPos = ((ListView) focusedChild).getSelectedItemPosition();
            }
        }
    }

    // Restore list state, position and focus
    private void restoreState() {
        dataList.smoothScrollToPositionFromTop(lastListIndex, lastTopOff);
        // dataList.setSelectionFromTop(lastListIndex, lastTopOff);

        View focusedChild = findViewById(focusID);
        if (focusedChild != null) {
            focusedChild.requestFocus();
            if (focusedChild instanceof ListView) {
                ((ListView) focusedChild).setSelection(selectedPos);
            }
        }
    }

    private void setupListView() {

        dataList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> adapterView, View arg1, int position, long arg3) {
                final DataItem dataItem = (DataItem) adapterView.getItemAtPosition(position);
                switch (displayType) {
                    case Packages:
                        openAppDetailSettings(dataItem);
                        break;
                    case Processes:
                        processKillDialog(dataItem);
                        break;
                }
            }
        });

        dataList.setOnItemLongClickListener(new ListView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, final View view,
                    int position, long arg3) {
                DataItem dataItem = (DataItem) adapterView.getItemAtPosition(position);
                switch (displayType) {
                    case Packages:
                        processExtraDialog(dataItem);
                        break;
                    case Processes:
                        processExtraDialog(dataItem);
                        break;
                }
                return true;
            }
        });
    }

    // ---------------------------------------------------------------------------------------------

    private void processExtraDialog(final DataItem dataItem) {
        final String processName = dataItem.name;
        android.content.DialogInterface.OnClickListener listener1 =
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            myActivityManager.killBackgroundProcesses(processName);
                            updateList();
                            makeToast("Killed Background for " + processName);
                        } else if (which == 1) {
                            showDetails(dataItem);
                        } else if (which == 2) {
                            killList.add(processName);
                            saveKillList();
                            makeToast(" New kill list size " + killList.size());
                        }
                    }
                };

        new AlertDialog.Builder(MainActivity.this)
                .setTitle(processName)
                .setItems(R.array.operation, listener1)
                .setPositiveButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                }).show();
    }

    private void processKillDialog(DataItem dataItem) {
        final String processName = dataItem.name;
        new AlertDialog.Builder(MainActivity.this)
                .setMessage("Kill Background\n" + processName)
                .setPositiveButton("Kill", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        myActivityManager.killBackgroundProcesses(processName);
                        updateList();
                        makeToast("Killed Bg " + processName);
                    }
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        }).create().show();
    }

    private void updateList() {
        dataList.setAdapter(null);

        switch (displayType) {
            case Packages:
                appPackageManager.loadList(new AppPackageManager.PkgAction() {
                    @Override
                    public void done(AppPackageManager mgr, int status, String msg) {
                        if (status == STATUS_OK) {
                            arrayAdapter =
                                    new PkgListAdapter(MainActivity.this, R.layout.list_row_pkg,
                                            R.id.list_pkg_text, mgr.getList());
                            dataList.setAdapter(arrayAdapter);
                            makeToast("Refresh Completed");
                            upDateMemInfo();
                        } else {
                            makeToast("Failed to load packages " + msg);
                        }
                    }
                });
                break;

            case Processes:
                appProcessManager.loadList(new AppProcessManager.ProcAction() {
                    @Override
                    public void done(AppProcessManager mgr, int status, String msg) {
                        if (status == STATUS_OK) {
                            arrayAdapter =
                                    new ProcListAdapter(MainActivity.this, R.layout.list_row_proc,
                                            R.id.list_proc_text, mgr.getList());
                            dataList.setAdapter(arrayAdapter);
                            makeToast("Refresh Completed");
                            upDateMemInfo();
                        } else {
                            makeToast("Failed to load process " + msg);
                        }
                    }
                });
                break;
        }
    }

    private void openAppDetailSettings(DataItem dataItem) {
        Intent intent = new Intent("android.settings.APPLICATION_DETAILS_SETTINGS", Uri
                .fromParts("package", dataItem.pkgName, null));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivityForResult(intent, INTENT_APP_DETAILS);
        makeToast(dataItem.pkgName);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // updateList();
    }

    private void upDateMemInfo() {
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        myActivityManager.getMemoryInfo(memoryInfo);
        long memSize = memoryInfo.availMem;
        String freeMemSize = Formatter.formatFileSize(getBaseContext(), memSize);
        rightStatusTv.setText("Free Memory: " + freeMemSize);

        String leftStatus = "";
        if (arrayAdapter != null) {
            leftStatus = "#Items: " + arrayAdapter.getCount();
        }
        leftStatusTv.setText(leftStatus);
    }

    private void makeToast(String str) {
        Toast toast = Toast.makeText(MainActivity.this, str, Toast.LENGTH_SHORT);
        View toastView = toast.getView();

        ImageView image = new ImageView(MainActivity.this);
        image.setImageResource(R.drawable.killbg);
        LinearLayout ll = new LinearLayout(MainActivity.this);

        // int dim = Math.round(getResources().getDimension(R.dimen.toastIconDim));
        int dim = 256;       // TODO - fix so resources work.

        ll.addView(image, new LinearLayout.LayoutParams(dim, dim));
    //    ll.addView(toastView);
        toast.setView(ll);
        toast.show();
    }

    //  ProcInfo processInfo = adapterProcList.get(position);
    private void showDetails(DataItem dataItem) {
        Intent intentKillService = new Intent(this, ShowDetailActivity.class);
        intentKillService.putExtra("EXTRA_PROCESS_NAME", dataItem.name);

        if (dataItem instanceof ProcInfo) {
            ProcInfo procInfo = (ProcInfo) dataItem;
            intentKillService.putExtra("EXTRA_PROCESS_PID", procInfo.pid + "");
            intentKillService.putExtra("EXTRA_PROCESS_IMPORTANCE", procInfo.importance + "");
        }
        // intentKillService.putExtra("EXTRA_PROCESS_UID", processInfo.uid + "");
        /*
        intentKillService.putExtra("EXTRA_PROCESS_IMPORTANCE_REASON_CODE",
                processInfo.importanceReasonCode + "");
        intentKillService.putExtra("EXTRA_PROCESS_IMPORTANCE_REASON_PID",
                processInfo.importanceReasonPid + "");
        intentKillService.putExtra("EXTRA_PROCESS_LRU", processInfo.lru + "");
        intentKillService.putExtra("EXTRA_PKGNAMELIST", processInfo.pkgList);
        */
        startActivity(intentKillService);
    }

    private void saveKillList() {
        SharedPreferences preferences = getSharedPreferences("main", Context.MODE_PRIVATE);
        SharedPreferences.Editor mEdit = preferences.edit();
        mEdit.putInt("Status_size", killList.size());
        for (int i = 0; i < killList.size(); i++) {
            mEdit.remove("Status_" + i);
            mEdit.putString("Status_" + i, killList.get(i));
        }
        mEdit.apply();
    }

    private void loadKillList(Context context) {
        SharedPreferences preference = getSharedPreferences("main", Context.MODE_PRIVATE);
        killList.clear();
        int size = preference.getInt("Status_size", 0);
        for (int i = 0; i < size; i++) {
            killList.add(preference.getString("Status_" + i, null));
        }
    }

    /*
    private void StopProcess(String processname){
        Process sh = null;
        DataOutputStream os = null;
        try{
            sh = Runtime.getRuntime().exec("su");
            os = new DataOutputStream(sh.getOutputStream());
            final String Command = "am force-stop "+processname + "\n";
            os.writeBytes(Command);
            os.flush();
        }catch (IOException e){
            e.printStackTrace();
        }
        try{
            sh.waitFor();
        }catch (InterruptedException e){
            e.printStackTrace();
        }
    }
    */



    // =============================================================================================
    // Custom List adapter to populate available Raster layers.
    @SuppressWarnings("SameParameterValue")
    private class ProcListAdapter extends ArrayAdapter<ProcInfo> {
        final int mTextViewResId;

        ProcListAdapter(Context context, int resource, int textViewResId,
                        List<ProcInfo> listLayers) {
            super(context, resource, textViewResId, listLayers);
            mTextViewResId = textViewResId;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View rowView = super.getView(position, convertView, parent);
            ProcInfo procInfo = getItem(position);

            TextView textView = Ui.viewById(rowView, R.id.list_proc_text);
            textView.setText(procInfo.toString());

            Drawable icon = appPackageManager.getPackageIcon(procInfo.pkgName);
            if (icon != null) {
                Ui.<ImageView>viewById(rowView, R.id.list_proc_image).setImageDrawable(icon);
            }

            rowView.setBackgroundColor((position & 1) == 1 ? Color.WHITE : 0xffddffdd);
            rowView.setBackgroundResource(R.drawable.list_color_state);
            return rowView;
        }
    }


    // =============================================================================================
    // Custom List adapter to show package list.
    private class PkgListAdapter extends ArrayAdapter<PkgInfo> {
        final int mTextViewResId;

        PkgListAdapter(Context context, int resource, int textViewResId,
                       List<PkgInfo> listLayers) {
            super(context, resource, textViewResId, listLayers);
            mTextViewResId = textViewResId;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View rowView = super.getView(position, convertView, parent);
            PkgInfo pkgInfo = getItem(position);

            TextView textView = Ui.viewById(rowView, R.id.list_pkg_text);
            textView.setText(pkgInfo.toString());
            textView.setBackgroundColor((position & 1) == 1 ? Color.WHITE : 0xffddffdd);
            textView.setBackgroundResource(R.drawable.list_color_state);

            Drawable icon = appPackageManager.getPackageIcon(pkgInfo.pkgName);
            Ui.<ImageView>viewById(rowView, R.id.list_pkg_image) .setImageDrawable(icon);
            return rowView;
        }
    }

}