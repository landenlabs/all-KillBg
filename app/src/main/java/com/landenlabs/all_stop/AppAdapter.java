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

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class AppAdapter<T extends DataItem> extends RecyclerView.Adapter<AppAdapter.ViewHolder> {

    public interface OnItemClickListener<T> {
        void onItemClick(@NonNull T item);
    }

    public interface OnItemLongClickListener<T> {
        boolean onItemLongClick(@NonNull T item);
    }

    public interface Binder<T> {
        void bind(@NonNull ViewHolder holder, @NonNull T item, @NonNull SortMode sortMode);
    }

    private final List<T> items = new ArrayList<>();
    private final int layoutResId;
    private final int textResId;
    private final int imageResId;
    private final Binder<T> binder;
    private OnItemClickListener<T> clickListener;
    private OnItemLongClickListener<T> longClickListener;
    private SortMode sortMode = SortMode.AppName;

    public AppAdapter(int layoutResId, int textResId, int imageResId, Binder<T> binder) {
        this.layoutResId = layoutResId;
        this.textResId = textResId;
        this.imageResId = imageResId;
        this.binder = binder;
    }

    public void setItems(@Nullable List<T> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    public void setSortMode(@NonNull SortMode sortMode) {
        this.sortMode = sortMode;
    }

    public void setOnItemClickListener(@Nullable OnItemClickListener<T> listener) {
        this.clickListener = listener;
    }

    public void setOnItemLongClickListener(@Nullable OnItemLongClickListener<T> listener) {
        this.longClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutResId, parent, false);
        return new ViewHolder(view, textResId, imageResId);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        T item = items.get(position);
        binder.bind(holder, item, sortMode);

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onItemClick(item);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                return longClickListener.onItemLongClick(item);
            }
            return false;
        });

        // Alternating background color
        int colorRes = (position % 2 == 1) ? R.color.row_bg_odd : R.color.row_bg_even;
        holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), colorRes));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView textView;
        public final ImageView imageView;

        public ViewHolder(@NonNull View itemView, int textResId, int imageResId) {
            super(itemView);
            textView = itemView.findViewById(textResId);
            imageView = itemView.findViewById(imageResId);
        }
    }
}
