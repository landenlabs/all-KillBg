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

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Manage Stop and Safe lists using Regular Expression patterns.
 */
public class SafeListActivity extends AppCompatActivity {

    private RegexAdapter stopAdapter;
    private RegexAdapter safeAdapter;
    private RecyclerView recyclerView;
    private ImageButton delBtn;
    private TextView descTv;
    private ImageButton expandBtn;

    private static final String PREF_STOP_LIST = "stop_regex_list";
    private static final String PREF_SAFE_LIST = "safe_regex_list";
    private static final String PREF_DESC_EXPANDED = "desc_expanded";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_list);

        recyclerView = findViewById(R.id.regex_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        delBtn = findViewById(R.id.del_pattern_btn);
        ImageButton addBtn = findViewById(R.id.add_pattern_btn);
        ImageButton clearBtn = findViewById(R.id.clear_pattern_btn);

        descTv = findViewById(R.id.desc);
        expandBtn = findViewById(R.id.expand_desc_btn);
        
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isExpanded = prefs.getBoolean(PREF_DESC_EXPANDED, false);
        setDescExpanded(isExpanded);

        expandBtn.setOnClickListener(v -> {
            boolean current = descTv.getVisibility() == View.VISIBLE;
            setDescExpanded(!current);
            prefs.edit().putBoolean(PREF_DESC_EXPANDED, !current).apply();
        });

        // Initialize Adapters
        stopAdapter = new RegexAdapter(loadList(PREF_STOP_LIST), this::onSelectionChanged);
        safeAdapter = new RegexAdapter(loadList(PREF_SAFE_LIST), this::onSelectionChanged);

        stopAdapter.setOnItemDoubleClickListener((item, pos) -> showEditDialog(item, pos, stopAdapter, PREF_STOP_LIST));
        safeAdapter.setOnItemDoubleClickListener((item, pos) -> showEditDialog(item, pos, safeAdapter, PREF_SAFE_LIST));

        // Default to Stop List
        recyclerView.setAdapter(stopAdapter);

        RadioGroup toggleGroup = findViewById(R.id.list_toggle_group);
        toggleGroup.setOnCheckedChangeListener((group, checkedId) -> {
            RegexAdapter activeAdapter;
            if (checkedId == R.id.toggle_stop) {
                activeAdapter = stopAdapter;
            } else {
                activeAdapter = safeAdapter;
            }
            recyclerView.setAdapter(activeAdapter);
            onSelectionChanged(activeAdapter.getSelectedCount());
        });

        addBtn.setOnClickListener(v -> {
            RegexAdapter activeAdapter = getActiveAdapter();
            String key = activeAdapter == stopAdapter ? PREF_STOP_LIST : PREF_SAFE_LIST;
            showEditDialog(null, -1, activeAdapter, key);
        });

        delBtn.setOnClickListener(v -> {
            RegexAdapter activeAdapter = getActiveAdapter();
            String key = activeAdapter == stopAdapter ? PREF_STOP_LIST : PREF_SAFE_LIST;
            int count = activeAdapter.getSelectedCount();
            new AlertDialog.Builder(this)
                    .setMessage(getString(R.string.delete_selected_confirm, count))
                    .setPositiveButton(R.string.delete_btn, (d, w) -> {
                        activeAdapter.deleteSelected();
                        saveList(key, activeAdapter.getItems());
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        });

        clearBtn.setOnClickListener(v -> {
            RegexAdapter activeAdapter = getActiveAdapter();
            String key = activeAdapter == stopAdapter ? PREF_STOP_LIST : PREF_SAFE_LIST;
            new AlertDialog.Builder(this)
                    .setMessage(R.string.clear_all_confirm)
                    .setPositiveButton(R.string.delete_btn, (d, w) -> {
                        activeAdapter.clearAll();
                        saveList(key, activeAdapter.getItems());
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        });
    }

    private RegexAdapter getActiveAdapter() {
        return (RegexAdapter) recyclerView.getAdapter();
    }

    private void onSelectionChanged(int count) {
        delBtn.setEnabled(count > 0);
    }

    private void setDescExpanded(boolean expanded) {
        descTv.setVisibility(expanded ? View.VISIBLE : View.GONE);
        expandBtn.setImageResource(expanded ? android.R.drawable.arrow_up_float : android.R.drawable.arrow_down_float);
    }

    private void showEditDialog(@Nullable String initialText, int position, RegexAdapter adapter, String prefKey) {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint(R.string.regex_hint);
        if (initialText != null) {
            input.setText(initialText);
        }

        new AlertDialog.Builder(this)
                .setTitle(initialText == null ? R.string.add_pattern : R.string.edit_pattern)
                .setView(input)
                .setPositiveButton(R.string.save_btn, (dialog, which) -> {
                    String pattern = input.getText().toString().trim();
                    if (isValidRegex(pattern)) {
                        if (position == -1) {
                            adapter.addItem(pattern);
                        } else {
                            adapter.updateItem(position, pattern);
                        }
                        saveList(prefKey, adapter.getItems());
                    } else {
                        AppUtils.showStatus(findViewById(android.R.id.content), getString(R.string.invalid_regex));
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private boolean isValidRegex(String regex) {
        if (regex.isEmpty()) return false;
        try {
            Pattern.compile(regex);
            return true;
        } catch (PatternSyntaxException e) {
            return false;
        }
    }

    private List<String> loadList(String key) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Set<String> set = prefs.getStringSet(key, new HashSet<>());
        return new ArrayList<>(set);
    }

    private void saveList(String key, List<String> list) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.edit().putStringSet(key, new HashSet<>(list)).apply();
    }

    // --- Inner classes for RecyclerView

    interface SelectionListener {
        void onSelectionChanged(int selectedCount);
    }

    interface DoubleClickListener {
        void onItemDoubleClick(String item, int position);
    }

    private static class RegexAdapter extends RecyclerView.Adapter<RegexAdapter.Holder> {
        private final List<String> items;
        private final Set<Integer> selectedPositions = new HashSet<>();
        private final SelectionListener selectionListener;
        private DoubleClickListener doubleClickListener;
        private long lastClickTime = 0;

        RegexAdapter(List<String> items, SelectionListener listener) {
            this.items = items;
            this.selectionListener = listener;
        }

        void setOnItemDoubleClickListener(DoubleClickListener listener) {
            this.doubleClickListener = listener;
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_row_regex, parent, false);
            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            String item = items.get(position);
            holder.textView.setText(item);
            
            int highlightColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.regex_item_selected);
            holder.itemView.setBackgroundColor(selectedPositions.contains(position) ? highlightColor : Color.TRANSPARENT);

            holder.itemView.setOnClickListener(v -> {
                long clickTime = System.currentTimeMillis();
                int currentPos = holder.getBindingAdapterPosition();
                if (currentPos == RecyclerView.NO_POSITION) return;

                if (clickTime - lastClickTime < 300) {
                    if (doubleClickListener != null) doubleClickListener.onItemDoubleClick(item, currentPos);
                } else {
                    if (selectedPositions.contains(currentPos)) {
                        selectedPositions.remove(currentPos);
                    } else {
                        selectedPositions.add(currentPos);
                    }
                    notifyItemChanged(currentPos);
                    selectionListener.onSelectionChanged(selectedPositions.size());
                }
                lastClickTime = clickTime;
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        List<String> getItems() { return items; }
        int getSelectedCount() { return selectedPositions.size(); }

        void addItem(String item) {
            items.add(item);
            notifyItemInserted(items.size() - 1);
        }

        void updateItem(int position, String item) {
            items.set(position, item);
            notifyItemChanged(position);
        }

        void deleteSelected() {
            List<String> toRemove = new ArrayList<>();
            for (int pos : selectedPositions) {
                toRemove.add(items.get(pos));
            }
            items.removeAll(toRemove);
            selectedPositions.clear();
            notifyDataSetChanged();
            selectionListener.onSelectionChanged(0);
        }

        void clearAll() {
            items.clear();
            selectedPositions.clear();
            notifyDataSetChanged();
            selectionListener.onSelectionChanged(0);
        }

        static class Holder extends RecyclerView.ViewHolder {
            TextView textView;
            Holder(View v) {
                super(v);
                textView = v.findViewById(R.id.regex_text);
            }
        }
    }
}
