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
 * @see http://LanDenLabs.com/
 */

package com.landenlabs.all_killbg;

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