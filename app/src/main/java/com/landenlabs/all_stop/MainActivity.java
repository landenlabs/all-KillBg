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

import static com.landenlabs.all_stop.AppConstants.APP_TAG;

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
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.landenlabs.all_stop.AppPackageManager.PkgInfo;
import com.landenlabs.all_stop.AppProcessManager.ProcInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

@SuppressWarnings("Convert2Lambda")
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private ActivityResultLauncher<Intent> accessibilitySettingsLauncher;
    private ActivityResultLauncher<Intent> appDetailsLauncher;
    private FirebaseAnalytics mFirebaseAnalytics;

    // ---------------------------------------------------------------------------------------------
    private int lastListIndex = 0;
    private int lastTopOff = 0;
    private int focusID = 0;

    private ActivityManager myActivityManager;
    private AppProcessManager appProcessManager;
    private AppPackageManager appPackageManager;

    private final ArrayList<String> stopList = new ArrayList<>();

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
    private View findRunningBtn;

    private SortMode sortMode = SortMode.AppName;


    // =============================================================================================
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(APP_TAG, "onCreate called. savedInstanceState is null: " + (savedInstanceState == null));
        // restoreAppTheme(this);
        getDelegate();

        @ColorInt int textColorPrimary = ContextCompat.getColor(this, R.color.text_primary);
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

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        boolean isAccessEnabled = isAccessibilityServiceEnabled();
        mFirebaseAnalytics.setUserProperty("accessibility_enabled", String.valueOf(isAccessEnabled));
        Bundle runBundle = new Bundle();
        runBundle.putBoolean("accessibility_enabled", isAccessEnabled);
        mFirebaseAnalytics.logEvent("app_run", runBundle);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        rightStatusTv = findViewById(R.id.rightStatus);
        dataList = findViewById(R.id.dataList);

        showPkgBtn = findViewById(R.id.show_pkg);
        showProcBtn = findViewById(R.id.show_proc);
        sortBtn = findViewById(R.id.sort_by);
        settingsBtn = findViewById(R.id.settings_icon);
        stopAppsBadge = findViewById(R.id.stop_apps_badge);
        findRunningBtn = findViewById(R.id.find_running);

        myActivityManager = (ActivityManager) getSystemService(Activity.ACTIVITY_SERVICE);
        appPackageManager = new AppPackageManager(this);
        appProcessManager = new AppProcessManager(this);

        findViewById(R.id.show_pkg).setOnClickListener(this);
        findViewById(R.id.show_proc).setOnClickListener(this);
        findViewById(R.id.stop_apps).setOnClickListener(this);
        findViewById(R.id.find_running).setOnClickListener(this);
        findViewById(R.id.EditBlackList).setOnClickListener(this);
        findViewById(R.id.settings_icon).setOnClickListener(this);
        sortBtn.setOnClickListener(this);

        sortBtn.setImageResource(sortMode.iconRes);

        appPackageManager.loadPackageIcons(0, (count) -> runOnUiThread(this::updateList));
        loadStopList(this);
        loadDisplaySettings();
        setupRecyclerView();
        updateBottomBarVisibility();
    }

    @Override
    public void onClick(@NonNull View v) {
        int id = v.getId();

        if (id == R.id.show_pkg) {
            displayType = DisplayType.Packages;
            saveDisplaySettings();
            updateList();
        } else if (id == R.id.show_proc) {
            displayType = DisplayType.Processes;
            saveDisplaySettings();
            updateList();
        } else if (id == R.id.stop_apps) {
            mFirebaseAnalytics.logEvent("stop_apps_clicked", null);
            if (isAccessibilityServiceEnabled()) {
                appProcessManager.stopProcesses();
                updateList();
            } else {
                showAccessibilityDialog();
            }
        } else if (id == R.id.find_running) {
            if (isAccessibilityServiceEnabled()) {
                appProcessManager.scanProcesses();
                updateList();
            }
            /* Keep for later
            final Intent intentStopService = new Intent(MainActivity.this, StopService.class);
            startService(intentStopService);
             */
        } else if (id == R.id.EditBlackList) {
            final Intent intentStopList = new Intent(MainActivity.this, SafeListActivity.class);
            startActivity(intentStopList);
        } else if (id == R.id.settings_icon) {
            new SettingDialog().show(MainActivity.this);
        } else if (id == R.id.sort_by) {
            sortMode = sortMode.next();
            sortBtn.setImageResource(sortMode.iconRes);
            saveDisplaySettings();
            updateList();
        }
    }

    private void updateBottomBarVisibility() {
        boolean isEnabled = isAccessibilityServiceEnabled();
        if (mFirebaseAnalytics != null) {
            mFirebaseAnalytics.setUserProperty("accessibility_enabled", String.valueOf(isEnabled));
        }
        if (stopAppsBadge != null) {
            stopAppsBadge.setVisibility(isEnabled ? View.GONE : View.VISIBLE);
        }
        if (findRunningBtn != null) {
            findRunningBtn.setEnabled(isEnabled);
        }
        if (settingsBtn != null) {
            if (isEnabled) {
                settingsBtn.clearColorFilter();
            } else {
                settingsBtn.setColorFilter(0xFFFF0000);
            }
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
        new AlertDialog.Builder(this).setTitle(R.string.accessibility_dialog_title)
                .setMessage(R.string.accessibility_dialog_message)
                .setPositiveButton(R.string.go_to_settings, (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                    accessibilitySettingsLauncher.launch(intent);
                }).setNegativeButton(R.string.cancel, (dialog, which) -> updateBottomBarVisibility()).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateBottomBarVisibility();
        Log.i(APP_TAG, "onResume, job="+ appProcessManager.isJobRunning + " scan=" + appProcessManager.isScanRunning);
        if (appProcessManager.isJobRunning) {
            appProcessManager.stopContinue();
        }
        if (appProcessManager.isScanRunning) {
            appProcessManager.scanContinue();
        }
    }

    @Override
    protected void onPause() {
        Log.i(APP_TAG, "onPause, jobRunning="+ appProcessManager.isJobRunning);
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
        Log.i(APP_TAG, "onStop, jobRunning="+ appProcessManager.isJobRunning);
        saveState();
        super.onStop();
    }
    public void onDestroy() {
        super.onDestroy();
        saveStopList();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        appProcessManager.saveToBundle(outState);
        outState.putInt("lastListIndex", lastListIndex);
        outState.putInt("lastTopOff", lastTopOff);
        outState.putInt("focusID", focusID);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle inState) {
        super.onRestoreInstanceState(inState);
        appProcessManager.loadFromBundle(inState);
        lastListIndex = inState.getInt("lastListIndex", 0);
        lastTopOff = inState.getInt("lastTopOff", 0);
        focusID = inState.getInt("focusID", 0);
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

    private void onListItemClick(@NonNull DataItem dataItem) {
        switch (displayType) {
            case Packages -> openAppDetailSettings(dataItem);
            case Processes -> processStopDialog(dataItem);
        }
    }

    private boolean onListItemLongClick(@NonNull DataItem dataItem) {
        processExtraDialog(dataItem);
        return true;
    }

    // ---------------------------------------------------------------------------------------------

    private void processExtraDialog(@NonNull final DataItem dataItem) {
        final String processName = dataItem.name;
        android.content.DialogInterface.OnClickListener listener1 = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    AppUtils.stopProcess(MainActivity.this, myActivityManager, processName);
                    updateList();
                    makeSnackbar(getString(R.string.stopped_for, processName));
                } else if (which == 1) {
                    showDetails(dataItem);
                } else if (which == 2) {
                    stopList.add(processName);
                    saveStopList();
                    makeSnackbar(getString(R.string.new_stop_list_size, stopList.size()));
                }
            }
        };

        new AlertDialog.Builder(MainActivity.this).setTitle(processName).setItems(R.array.operation, listener1).setPositiveButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        }).show();
    }

    private void processStopDialog(@NonNull DataItem dataItem) {
        final String processName = dataItem.name;
        new AlertDialog.Builder(MainActivity.this).setMessage(getString(R.string.stop_process_title, processName)).setPositiveButton(R.string.stop_btn, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                AppUtils.stopProcess(MainActivity.this, myActivityManager, processName);
                updateList();
                makeSnackbar(getString(R.string.stopped_for, processName));
            }
        }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        }).create().show();
    }

    private String str(@Nullable String s) {
        return s == null ? "" : s;
    }

    private void highlightText(@NonNull TextView textView, @NonNull String fullText, @Nullable String subText) {
        if (subText == null || subText.isEmpty()) {
            textView.setText(fullText);
            return;
        }
        SpannableString spannable = new SpannableString(fullText);
        int start = fullText.indexOf(subText);
        if (start != -1) {
            int highlightColor = ContextCompat.getColor(this, R.color.sort_highlight);
            spannable.setSpan(new ForegroundColorSpan(highlightColor), start, start + subText.length(), 0);
        }
        textView.setText(spannable);
    }

    private void updateList() {
        dataList.setAdapter(null);

        switch (displayType) {
            case Packages -> appPackageManager.loadList((mgr, status, msg) -> runOnUiThread(() -> {
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
                                        java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("MM/dd/yyyy  HH:mm", Locale.US);
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
                    makeSnackbar(getString(R.string.refresh_completed));
                    updateMemInfo();
                } else {
                    makeSnackbar(getString(R.string.failed_to_load_packages, msg));
                }
            }));
            case Processes -> appProcessManager.loadListAsync((mgr, status, msg) -> runOnUiThread(() -> {
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
                                    case Date -> "StartTm: " + new java.text.SimpleDateFormat("EEE  hh:mm a z", Locale.US).format(item.startTime);
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
                    makeSnackbar(getString(R.string.refresh_completed));
                    updateMemInfo();
                } else {
                    makeSnackbar(getString(R.string.failed_to_load_process, msg));
                }
            }));
        }
    }

    private void openAppDetailSettings(@NonNull DataItem dataItem) {
        // Manual open from list - do NOT enable automation
        StopProcByAccessibilityService.setRunning(false, StopProcByAccessibilityService.ServiceMode.SCAN);

        Intent intent = new Intent("android.settings.APPLICATION_DETAILS_SETTINGS", Uri.fromParts("package", dataItem.pkgName, null));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        appDetailsLauncher.launch(intent);
        makeSnackbar(dataItem.pkgName);
    }

    private void updateMemInfo() {
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        myActivityManager.getMemoryInfo(memoryInfo);
        long memSize = memoryInfo.availMem;
        String freeMemSize = Formatter.formatFileSize(getBaseContext(), memSize);
        rightStatusTv.setText(getString(R.string.free_memory_format, freeMemSize));

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

    private void makeSnackbar(String str) {
        AppUtils.showStatus(findViewById(android.R.id.content), str);
    }

    //  ProcInfo processInfo = adapterProcList.get(position);
    private void showDetails(@NonNull DataItem dataItem) {
        Intent intentStopService = new Intent(this, ShowDetailActivity.class);
        intentStopService.putExtra(ShowDetailActivity.EXTRA_PROCESS_NAME, dataItem.name);

        if (dataItem instanceof ProcInfo procInfo) {
            intentStopService.putExtra(ShowDetailActivity.EXTRA_PROCESS_PID, procInfo.pid + "");
            intentStopService.putExtra(ShowDetailActivity.EXTRA_PROCESS_IMPORTANCE, procInfo.importance + "");
        }
        startActivity(intentStopService);
    }

    private void saveStopList() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        preferences.edit().putStringSet("stop_list", new java.util.HashSet<>(stopList)).apply();
    }

    private void loadStopList(@NonNull Context context) {
        SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(context);
        java.util.Set<String> set = preference.getStringSet("stop_list", null);
        stopList.clear();
        if (set != null) {
            stopList.addAll(set);
        }
    }

    private void saveDisplaySettings() {
        PreferenceManager.getDefaultSharedPreferences(this).edit()
                .putInt("DisplayType", displayType.ordinal())
                .putInt("SortMode", sortMode.ordinal())
                .apply();
    }

    private void loadDisplaySettings() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        int displayOrdinal = preferences.getInt("DisplayType", DisplayType.Packages.ordinal());
        int sortOrdinal = preferences.getInt("SortMode", SortMode.AppName.ordinal());

        displayType = DisplayType.values()[displayOrdinal % DisplayType.values().length];
        sortMode = SortMode.values()[sortOrdinal % SortMode.values().length];

        if (showPkgBtn != null && showProcBtn != null) {
            if (displayType == DisplayType.Packages) {
                showPkgBtn.setChecked(true);
            } else {
                showProcBtn.setChecked(true);
            }
        }
        if (sortBtn != null) {
            sortBtn.setImageResource(sortMode.iconRes);
        }
    }
}
