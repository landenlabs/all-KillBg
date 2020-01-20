package com.landenlabs.all_killbg;

/*
 * Copyright (C) 2020 Dennis Lang (landenlabs@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.view.View;
import android.widget.AdapterView;

/**
 *
 */
public class Ui {

    private static int CLICK_DELAY_MILLIS = (Build.VERSION.SDK_INT >= 21) ? 2500 : 0;

    private Ui() {
    }

    @SuppressWarnings("unchecked")
    static <E extends View> E viewById(View rootView, int id) {
        return (E) rootView.findViewById(id);
    }

    @SuppressWarnings("unchecked")
    static <E extends View> E viewById(Activity fact, int id) {
        //noinspection unchecked
        return (E) fact.findViewById(id);
    }


    /**
     * Execute ripple effect (v5.0, api 21)
     */
    private static void runRippleAnimation(View view) {
        if (Build.VERSION.SDK_INT >= 21) {
            Drawable background = view.getBackground();
            if (background != null && background instanceof RippleDrawable) {
                final RippleDrawable rippleDrawable = (RippleDrawable) background;
                rippleDrawable.setState(
                        new int[]{android.R.attr.state_pressed, android.R.attr.state_enabled});
                rippleDrawable.setState(new int[]{});
            }
        }
    }

    /**
     * Process View's onClick with delay to allow ripple effect to finish.
     * Implement onClickDelay only.
     */
    public static abstract class OnClickListener implements View.OnClickListener {

        public void onClick(final View v) {
            runRippleAnimation(v);
            v.postDelayed(new Runnable() {
                @Override
                public void run() {
                    onClickDelay(v);
                }
            }, CLICK_DELAY_MILLIS);
        }

        /**
         * Called when a view has been clicked.
         *
         * @param v The view that was clicked.
         */
        abstract void onClickDelay(View v);
    }

    public static abstract class OnItemClickListener implements AdapterView.OnItemClickListener {

        public void  onItemClick(final AdapterView<?> adapterView,
                final View view, final int position, final long arg3) {
            runRippleAnimation(view);
            view.postDelayed(new Runnable() {
                @Override
                public void run() {
                    onItemClickDelay(adapterView, view, position, arg3);
                }
            }, CLICK_DELAY_MILLIS);
        }

        /**
         * Called when a view has been clicked.
         */
        abstract void onItemClickDelay(AdapterView<?> adapterView, View arg1, int position, long arg3);
    }

}