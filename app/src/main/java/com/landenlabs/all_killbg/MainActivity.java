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
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.ColorInt;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.landenlabs.all_killbg.AppPackageManager.PkgInfo;
import com.landenlabs.all_killbg.AppProcessManager.ProcInfo;

import java.util.ArrayList;
import java.util.Collections;

@SuppressWarnings("Convert2Lambda")
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private ActivityResultLauncher<Intent> accessibilitySettingsLauncher;
    private ActivityResultLauncher<Intent> appDetailsLauncher;

    // ---------------------------------------------------------------------------------------------
    private static int lastListIndex = 0;
    private static int lastTopOff = 0;
    private static int focusID = 0;

    private ActivityManager myActivityManager;
    private AppProcessManager appProcessManager;
    private AppPackageManager appPackageManager;

    private final ArrayList<String> killList = new ArrayList<>();

    private enum DisplayType {Packages, Processes}

    private DisplayType displayType = DisplayType.Packages;
    private RecyclerView.Adapter<?> arrayAdapter;
    private RecyclerView dataList;
    private TextView rightStatusTv;
    private RadioButton showPkgBtn;
    private RadioButton showProcBtn;
    private ImageView sortBtn;
    private ImageView settingsBtn;
    private View stopAppsBadge;

    private SortMode sortMode = SortMode.AppName;


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

        appDetailsLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> updateList()
        );

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        rightStatusTv = findViewById(R.id.rightStatus);
        dataList = findViewById(R.id.dataList);

        showPkgBtn = findViewById(R.id.show_pkg);
        showProcBtn = findViewById(R.id.show_proc);
        sortBtn = findViewById(R.id.sort_by);
        settingsBtn = findViewById(R.id.settings_icon);
        stopAppsBadge = findViewById(R.id.stop_apps_badge);

        myActivityManager = (ActivityManager) getSystemService(Activity.ACTIVITY_SERVICE);
        appPackageManager = new AppPackageManager(this);
        appProcessManager = new AppProcessManager(this);

        findViewById(R.id.show_pkg).setOnClickListener(this);
        findViewById(R.id.show_proc).setOnClickListener(this);
        findViewById(R.id.stop_apps).setOnClickListener(this);
        findViewById(R.id.stop_services).setOnClickListener(this);
        findViewById(R.id.EditBlackList).setOnClickListener(this);
        findViewById(R.id.settings_icon).setOnClickListener(this);
        sortBtn.setOnClickListener(this);

        sortBtn.setImageResource(sortMode.iconRes);

        appPackageManager.loadPackageIcons(0);  // Should this be async
        loadKillList(this);
        setupRecyclerView();
        updateBottomBarVisibility();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (id == R.id.show_pkg) {
            displayType = DisplayType.Packages;
            updateList();
        } else if (id == R.id.show_proc) {
            displayType = DisplayType.Processes;
            updateList();
        } else if (id == R.id.stop_apps) {
            if (isAccessibilityServiceEnabled()) {
                appProcessManager.stopProcesses();
                updateList();
            } else {
                showAccessibilityDialog();
            }
        } else if (id == R.id.stop_services) {
            final Intent intentKillService = new Intent(MainActivity.this, StopService.class);
            startService(intentKillService);
        } else if (id == R.id.EditBlackList) {
            final Intent intentKillList = new Intent(MainActivity.this, BlackListActivity.class);
            startActivity(intentKillList);
        } else if (id == R.id.settings_icon) {
            new SettingDialog().show(MainActivity.this);
        } else if (id == R.id.sort_by) {
            sortMode = sortMode.next();
            sortBtn.setImageResource(sortMode.iconRes);
            updateList();
        }
    }

    private void updateBottomBarVisibility() {
        boolean isEnabled = isAccessibilityServiceEnabled();
        if (stopAppsBadge != null) {
            stopAppsBadge.setVisibility(isEnabled ? View.GONE : View.VISIBLE);
        }
        if (settingsBtn != null) {
            if (isEnabled) {
                settingsBtn.clearColorFilter();
            } else {
                settingsBtn.setColorFilter(0xFFFF0000);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateBottomBarVisibility();
        if (appProcessManager.isJobRunning) {
            appProcessManager.stopContinue();
        }
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

        // Log.w(APP_TAG, "Accessibility service not enabled");
        return false;
    }

    private void showAccessibilityDialog() {
        new AlertDialog.Builder(this).setTitle("Permission Required")
                .setMessage("""
                        To automate stopping apps, you must enable the Accessibility Service for 'all KillBg'.
                        
                        1. Click 'Go to Settings'
                        2. Find 'all KillBg' in the list
                        3. Turn the switch ON""")
                .setPositiveButton("Go to Settings", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                    accessibilitySettingsLauncher.launch(intent);
                }).setNegativeButton("Cancel", (dialog, which) -> updateBottomBarVisibility()).show();
    }

    @Override
    protected void onPause() {
        super.onPause();
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
    public void onDestroy() {
        // Log.d(APP_TAG, "onDestroy called. Is finishing: " + isFinishing());
        super.onDestroy();
        saveKillList();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // Log.d(APP_TAG, "onSaveInstanceState, jobRunning: " + appProcessManager.isJobRunning);
        super.onSaveInstanceState(outState);
        appProcessManager.saveToBundle(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle inState) {
        // Log.d(APP_TAG, "onRestoreInstanceState " + appProcessManager);
        super.onRestoreInstanceState(inState);
        appProcessManager.loadFromBundle(inState);
    }

// ----- Private

    // Save list state, position and focus
    private void saveState() {
        // Save list scroll state
        LinearLayoutManager layoutManager = (LinearLayoutManager) dataList.getLayoutManager();
        if (layoutManager != null) {
            lastListIndex = layoutManager.findFirstVisibleItemPosition();
            View v = layoutManager.findViewByPosition(lastListIndex);
            lastTopOff = (v == null) ? 0 : v.getTop();
        }

        // Save focus and list selected state.
        View focusedChild = getCurrentFocus();
        if (focusedChild != null) {
            focusID = focusedChild.getId();
            // RecyclerView doesn't have getSelectedItemPosition() like ListView
        }
    }

    // Restore list state, position and focus
    private void restoreState() {
        LinearLayoutManager layoutManager = (LinearLayoutManager) dataList.getLayoutManager();
        if (layoutManager != null) {
            layoutManager.scrollToPositionWithOffset(lastListIndex, lastTopOff);
        }

        View focusedChild = findViewById(focusID);
        if (focusedChild != null) {
            focusedChild.requestFocus();
        }
    }

    private void setupRecyclerView() {
        dataList.setLayoutManager(new LinearLayoutManager(this));
    }

    private void onListItemClick(DataItem dataItem) {
        switch (displayType) {
            case Packages -> openAppDetailSettings(dataItem);
            case Processes -> processKillDialog(dataItem);
        }
    }

    private boolean onListItemLongClick(DataItem dataItem) {
        processExtraDialog(dataItem);
        return true;
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

    private String str(String s) {
        return s == null ? "" : s;
    }

    private void highlightText(TextView textView, String fullText, String subText) {
        if (subText == null || subText.isEmpty()) {
            textView.setText(fullText);
            return;
        }
        SpannableString spannable = new SpannableString(fullText);
        int start = fullText.indexOf(subText);
        if (start != -1) {
            int highlightColor = getResources().getColor(R.color.sort_highlight);
            spannable.setSpan(new ForegroundColorSpan(highlightColor), start, start + subText.length(), 0);
        }
        textView.setText(spannable);
    }

    private void updateList() {
        dataList.setAdapter(null);

        switch (displayType) {
            case Packages -> appPackageManager.loadList((mgr, status, msg) -> {
                if (status == AppPackageManager.PkgAction.STATUS_OK) {
                    ArrayList<PkgInfo> list = new ArrayList<>(mgr.getList());
                    Collections.sort(list, (PkgInfo a, PkgInfo b) -> switch (sortMode) {
                        case AppName -> str(a.name).compareToIgnoreCase(str(b.name));
                        case Date -> {
                            long timeA = (a.packInfo != null) ? a.packInfo.lastUpdateTime : 0;
                            long timeB = (b.packInfo != null) ? b.packInfo.lastUpdateTime : 0;
                            yield Long.compare(timeB, timeA); // Descending (newest first)
                        }
                        case Id -> {
                            String idA = (a.packInfo != null) ? a.packInfo.packageName : "";
                            String idB = (b.packInfo != null) ? b.packInfo.packageName : "";
                            yield idA.compareToIgnoreCase(idB);
                        }
                    });

                    AppAdapter<PkgInfo> adapter = new AppAdapter<>(
                            R.layout.list_row_pkg, R.id.list_pkg_text, R.id.list_pkg_image,
                            (holder, item, mode) -> {
                                String fullText = item.toString();
                                String highlight = switch (mode) {
                                    case AppName -> item.label;
                                    case Date -> {
                                        java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("MM/dd/yyyy  HH:mm");
                                        yield (item.packInfo != null && item.packInfo.firstInstallTime != item.packInfo.lastUpdateTime)
                                                ? "Install Last " + fmt.format(item.packInfo.lastUpdateTime)
                                                : "Installed " + (item.packInfo != null ? fmt.format(item.packInfo.firstInstallTime) : "");
                                    }
                                    case Id -> (item.packInfo != null) ? item.packInfo.packageName : null;
                                };
                                highlightText(holder.textView, fullText, highlight);

                                Drawable icon = appPackageManager.getPackageIcon(item.pkgName);
                                holder.imageView.setImageDrawable(icon);
                            });
                    adapter.setSortMode(sortMode);
                    adapter.setItems(list);
                    adapter.setOnItemClickListener(this::onListItemClick);
                    adapter.setOnItemLongClickListener(this::onListItemLongClick);
                    arrayAdapter = adapter;
                    dataList.setAdapter(arrayAdapter);
                    makeToast("Refresh Completed");
                    upDateMemInfo();
                } else {
                    makeToast("Failed to load packages " + msg);
                }
            });
            case Processes -> appProcessManager.loadList((mgr, status, msg) -> {
                if (status == AppProcessManager.ProcAction.STATUS_OK) {
                    ArrayList<ProcInfo> list = new ArrayList<>(mgr.getList());
                    Collections.sort(list, (ProcInfo a, ProcInfo b) -> switch (sortMode) {
                        case AppName -> str(a.name).compareToIgnoreCase(str(b.name));
                        case Date -> Long.compare(b.startTime, a.startTime); // Descending
                        case Id -> str(a.state).compareToIgnoreCase(str(b.state));
                    });

                    AppAdapter<ProcInfo> adapter = new AppAdapter<>(
                            R.layout.list_row_proc, R.id.list_proc_text, R.id.list_proc_image,
                            (holder, item, mode) -> {
                                String fullText = item.toString();
                                String highlight = switch (mode) {
                                    case AppName -> item.name;
                                    case Date -> "StartTm: " + new java.text.SimpleDateFormat("EEE  hh:mm a z").format(item.startTime);
                                    case Id -> item.state;
                                };
                                highlightText(holder.textView, fullText, highlight);

                                Drawable icon = appPackageManager.getPackageIcon(item.pkgName);
                                holder.imageView.setImageDrawable(icon);
                            });
                    adapter.setSortMode(sortMode);
                    adapter.setItems(list);
                    adapter.setOnItemClickListener(this::onListItemClick);
                    adapter.setOnItemLongClickListener(this::onListItemLongClick);
                    arrayAdapter = adapter;
                    dataList.setAdapter(arrayAdapter);
                    makeToast("Refresh Completed");
                    upDateMemInfo();
                } else {
                    makeToast("Failed to load process " + msg);
                }
            });
        }
    }

    private void openAppDetailSettings(DataItem dataItem) {
        // Manual open from list - do NOT enable automation
        StopProcByAccessibilityService.setRunning(false);

        Intent intent = new Intent("android.settings.APPLICATION_DETAILS_SETTINGS", Uri.fromParts("package", dataItem.pkgName, null));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        appDetailsLauncher.launch(intent);
        makeToast(dataItem.pkgName);
    }


    private void upDateMemInfo() {
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        myActivityManager.getMemoryInfo(memoryInfo);
        long memSize = memoryInfo.availMem;
        String freeMemSize = Formatter.formatFileSize(getBaseContext(), memSize);
        rightStatusTv.setText("Free Memory: " + freeMemSize);

        if (arrayAdapter != null) {
            int count = arrayAdapter.getItemCount();
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

        if (dataItem instanceof ProcInfo procInfo) {
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
}
