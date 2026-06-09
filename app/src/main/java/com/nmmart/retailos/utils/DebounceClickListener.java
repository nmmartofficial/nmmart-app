package com.nmmart.retailos.utils;

import android.os.SystemClock;
import android.view.View;

public class DebounceClickListener implements View.OnClickListener {
    private final long debounceInterval;
    private long lastClickTime = 0;
    private final View.OnClickListener clickListener;

    public DebounceClickListener(View.OnClickListener clickListener) {
        this.clickListener = clickListener;
        this.debounceInterval = 500; // 500ms debounce interval
    }

    public DebounceClickListener(long debounceInterval, View.OnClickListener clickListener) {
        this.clickListener = clickListener;
        this.debounceInterval = debounceInterval;
    }

    @Override
    public void onClick(View view) {
        long currentTime = SystemClock.elapsedRealtime();
        if (currentTime - lastClickTime >= debounceInterval) {
            lastClickTime = currentTime;
            clickListener.onClick(view);
        }
    }
}

