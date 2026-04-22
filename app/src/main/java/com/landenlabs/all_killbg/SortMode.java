package com.landenlabs.all_killbg;

public enum SortMode {
    AppName(android.R.drawable.ic_menu_sort_alphabetically),
    Date(android.R.drawable.ic_menu_today),
    Id(android.R.drawable.ic_menu_edit);

    public final int iconRes;

    SortMode(int iconRes) {
        this.iconRes = iconRes;
    }

    public SortMode next() {
        return values()[(ordinal() + 1) % values().length];
    }
}
