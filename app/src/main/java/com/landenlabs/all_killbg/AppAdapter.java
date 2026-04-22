package com.landenlabs.all_killbg;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class AppAdapter<T extends DataItem> extends RecyclerView.Adapter<AppAdapter.ViewHolder> {

    public interface OnItemClickListener<T> {
        void onItemClick(T item);
    }

    public interface OnItemLongClickListener<T> {
        boolean onItemLongClick(T item);
    }

    public interface Binder<T> {
        void bind(ViewHolder holder, T item);
    }

    private final List<T> items = new ArrayList<>();
    private final int layoutResId;
    private final int textResId;
    private final int imageResId;
    private final Binder<T> binder;
    private OnItemClickListener<T> clickListener;
    private OnItemLongClickListener<T> longClickListener;

    public AppAdapter(int layoutResId, int textResId, int imageResId, Binder<T> binder) {
        this.layoutResId = layoutResId;
        this.textResId = textResId;
        this.imageResId = imageResId;
        this.binder = binder;
    }

    public void setItems(List<T> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    public void setOnItemClickListener(OnItemClickListener<T> listener) {
        this.clickListener = listener;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener<T> listener) {
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
        binder.bind(holder, item);

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
        holder.itemView.setBackgroundColor((position & 1) == 1 ? Color.WHITE : 0xffddffdd);
        holder.itemView.setBackgroundResource(R.drawable.list_color_state);
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
