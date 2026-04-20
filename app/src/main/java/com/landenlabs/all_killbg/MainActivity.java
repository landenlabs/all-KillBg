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

package com.landenlabs.all_killbg;

import static com.landenlabs.all_killbg.AppConstants.APP_TAG;

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
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.ColorInt;
import androidx.appcompat.app.AppCompatActivity;

import com.landenlabs.all_killbg.AppPackageManager.PkgInfo;
import com.landenlabs.all_killbg.AppProcessManager.ProcInfo;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("Convert2Lambda")
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int INTENT_APP_DETAILS = 1;

    private ActivityResultLauncher<Intent> accessibilitySettingsLauncher;

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
    private TextView rightStatusTv;
    private RadioButton showPkgBtn;
    private RadioButton showProcBtn;


    // =============================================================================================
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(APP_TAG, "onCreate called. savedInstanceState is null: " + (savedInstanceState == null));
        // restoreAppTheme(this);
        getDelegate();

        @ColorInt int textColorPrimary = getResources().getColor(R.color.text_primary);
        Log.i(APP_TAG, "textColorPrimary: " + (textColorPrimary & 0xffffff));
        if (textColorPrimary != 0) {
            Log.e(APP_TAG, "textColorPrimary failed to change to 0");
        }


        accessibilitySettingsLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> updateList()
        );

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        rightStatusTv = findViewById(R.id.rightStatus);
        dataList = findViewById(R.id.dataList);

        showPkgBtn = findViewById(R.id.show_pkg);
        showProcBtn = findViewById(R.id.show_proc);

        myActivityManager = (ActivityManager) getSystemService(Activity.ACTIVITY_SERVICE);
        appPackageManager = new AppPackageManager(this);
        appProcessManager = new AppProcessManager(this);

        findViewById(R.id.show_pkg).setOnClickListener(this);
        findViewById(R.id.show_proc).setOnClickListener(this);
        findViewById(R.id.stop_apps).setOnClickListener(this);
        findViewById(R.id.stop_services).setOnClickListener(this);
        findViewById(R.id.EditBlackList).setOnClickListener(this);
        findViewById(R.id.settings_icon).setOnClickListener(this);

        appPackageManager.loadPackageIcons(0);  // Should this be async
        loadKillList(this);
        setupListView();
        updateBottomBarVisibility();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (id == R.id.show_pkg) {
            displayType = DisplayType.Packages;
            updateList();
        } else  if (id == R.id.show_proc) {
            displayType = DisplayType.Processes;
            updateList();
        } else if (id == R.id.stop_apps) {
            if (isAccessibilityServiceEnabled()) {
                StopProcByAccessibilityService.setRunning(true);
                appProcessManager.stopProcesses();
                updateList();
            } else {
                showAccessibilityDialog();
            }
        } else if (id == R.id.stop_services) {
            final Intent intentKillService = new Intent(MainActivity.this, StopService.class);
            startService(intentKillService);
        } else  if (id == R.id.EditBlackList) {
            final Intent intentKillList = new Intent(MainActivity.this, BlackListActivity.class);
            startActivity(intentKillList);
        } else  if (id == R.id.settings_icon) {
            new SettingDialog().show(MainActivity.this);
        }
    }

    private void updateBottomBarVisibility() {
    }

    private boolean isAccessibilityServiceEnabled() {
        String service = getPackageName() + "/" + StopProcByAccessibilityService.class.getCanonicalName();
        try {
            int accessibilityEnabled = Settings.Secure.getInt(getContentResolver(), Settings.Secure.ACCESSIBILITY_ENABLED);
            if (accessibilityEnabled == 1) {
                String settingValue = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
                if (settingValue != null) {
                    TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(':');
                    splitter.setString(settingValue);
                    while (splitter.hasNext()) {
                        String accessibilityService = splitter.next();
                        if (accessibilityService.equalsIgnoreCase(service)) {
                            return true;
                        }
                    }
                }
            }
        } catch (Settings.SettingNotFoundException e) {
            // Ignore
        }

        Log.w(APP_TAG, "Accessibility service not enabled");
        return false;
    }

    private void showAccessibilityDialog() {
        new AlertDialog.Builder(this).setTitle("Permission Required")
                .setMessage("To automate stopping apps, you must enable the Accessibility Service for 'all KillBg'.\n\n"
                        + "1. Click 'Go to Settings'\n"
                        + "2. Find 'all KillBg' in the list\n"
                        + "3. Turn the switch ON")
                .setPositiveButton("Go to Settings", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                accessibilitySettingsLauncher.launch(intent);
            }
        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                updateBottomBarVisibility();
            }
        }).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mIsAutomating) {
            // The last app was processed (or at least we attempted it),
            // increment the index and move to the next.
            mCurrentProcessingIndex++;

            // Add a slight delay (300-500ms) to allow the OS to stabilize
            // after the 'Back' animation before jumping into the next Settings page.
            new Handler(Looper.getMainLooper()).postDelayed(this::processNextApp, 500);
        }
    }

    /*
    private int mCurrentProcessingIndex = 0;
    private boolean mIsAutomating = false;

    // Call this to start the whole process
    void startKillAll() {
        mCurrentProcessingIndex = 0;
        mIsAutomating = true;
        StopProcByAccessibilityService.setRunning(true);
        processNextApp();
    }

    void processNextApp() {
        if (!mIsAutomating || mCurrentProcessingIndex >= dataList.size()) {
            mIsAutomating = false;
            StopProcByAccessibilityService.setRunning(false);
            Log.d(APP_TAG, "All processes handled.");
            return;
        }

        ProcInfo procInfo = dataList.get(mCurrentProcessingIndex);
        String pkgName = procInfo.pkgName != null ? procInfo.pkgName : procInfo.name;

        // Skip our own app
        if (pkgName.equals(context.getPackageName())) {
            mCurrentProcessingIndex++;
            processNextApp();
            return;
        }

        Log.d(APP_TAG, "Processing: " + pkgName);

        // 1. Optional: standard background kill
        activityManager.killBackgroundProcesses(pkgName);

        // 2. Open settings (This will trigger the Accessibility Service)
        openAppDetailSettings(pkgName);

        // We do NOT increment the index here yet.
        // We increment it only when we successfully return.
    }
    */
    
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
        // Log.d(APP_TAG, "onDestroy called. Is finishing: " + isFinishing());
        // findViewById(R.id.show_pkg).setOnClickListener(null);
        // findViewById(R.id.show_proc).setOnClickListener(null);
        // findViewById(R.id.stop_apps).setOnClickListener(null);
        // findViewById(R.id.stop_services).setOnClickListener(null);
        // findViewById(R.id.EditBlackList).setOnClickListener(null);
        // findViewById(R.id.settings_icon).setOnClickListener(null);

        super.onDestroy();
        saveKillList();
    }

    /*
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle inState) {
        super.onRestoreInstanceState(inState);
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
            public boolean onItemLongClick(AdapterView<?> adapterView, final View view, int position, long arg3) {
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
        android.content.DialogInterface.OnClickListener listener1 = new DialogInterface.OnClickListener() {
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

        new AlertDialog.Builder(MainActivity.this).setTitle(processName).setItems(R.array.operation, listener1).setPositiveButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        }).show();
    }

    private void processKillDialog(DataItem dataItem) {
        final String processName = dataItem.name;
        new AlertDialog.Builder(MainActivity.this).setMessage("Kill Process\n" + processName).setPositiveButton("Kill", new DialogInterface.OnClickListener() {
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
                            arrayAdapter = new PkgListAdapter(MainActivity.this, R.layout.list_row_pkg, R.id.list_pkg_text, mgr.getList());
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
                            arrayAdapter = new ProcListAdapter(MainActivity.this, R.layout.list_row_proc, R.id.list_proc_text, mgr.getList());
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
        // Manual open from list - do NOT enable automation
        StopProcByAccessibilityService.setRunning(false);

        Intent intent = new Intent("android.settings.APPLICATION_DETAILS_SETTINGS", Uri.fromParts("package", dataItem.pkgName, null));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivityForResult(intent, INTENT_APP_DETAILS);
        makeToast(dataItem.pkgName);
    }

    /*
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // updateList();
    }
     */

    private void upDateMemInfo() {
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        myActivityManager.getMemoryInfo(memoryInfo);
        long memSize = memoryInfo.availMem;
        String freeMemSize = Formatter.formatFileSize(getBaseContext(), memSize);
        rightStatusTv.setText("Free Memory: " + freeMemSize);

        if (arrayAdapter != null) {
            int count = arrayAdapter.getCount();
            if (displayType == DisplayType.Packages) {
                showPkgBtn.setText("Show Apps\n(" + count + ")");
                showProcBtn.setText("Show\nProcess");
            } else {
                showPkgBtn.setText("Show\nApps");
                showProcBtn.setText("Show Proc\n(" + count + ")");
            }
        }
    }

    @SuppressWarnings("deprecation")
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

    // =============================================================================================
    // Custom List adapter to populate available Raster layers.
    @SuppressWarnings("SameParameterValue")
    private class ProcListAdapter extends ArrayAdapter<ProcInfo> {
        final int mTextViewResId;

        ProcListAdapter(Context context, int resource, int textViewResId, List<ProcInfo> listLayers) {
            super(context, resource, textViewResId, listLayers);
            mTextViewResId = textViewResId;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View rowView = super.getView(position, convertView, parent);
            ProcInfo procInfo = getItem(position);

            TextView textView = rowView.findViewById(R.id.list_proc_text);
            textView.setText(procInfo.toString());

            Drawable icon = appPackageManager.getPackageIcon(procInfo.pkgName);
            if (icon != null) {
                rowView.<ImageView>findViewById(R.id.list_proc_image).setImageDrawable(icon);
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

        PkgListAdapter(Context context, int resource, int textViewResId, List<PkgInfo> listLayers) {
            super(context, resource, textViewResId, listLayers);
            mTextViewResId = textViewResId;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View rowView = super.getView(position, convertView, parent);
            PkgInfo pkgInfo = getItem(position);

            TextView textView = rowView.findViewById(R.id.list_pkg_text);
            textView.setText(pkgInfo.toString());
            textView.setBackgroundColor((position & 1) == 1 ? Color.WHITE : 0xffddffdd);
            textView.setBackgroundResource(R.drawable.list_color_state);

            Drawable icon = appPackageManager.getPackageIcon(pkgInfo.pkgName);
            rowView.<ImageView>findViewById(R.id.list_pkg_image).setImageDrawable(icon);
            return rowView;
        }
    }
}