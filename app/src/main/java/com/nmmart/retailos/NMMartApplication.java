package com.nmmart.retailos;

import android.app.Application;

import com.nmmart.retailos.data.SupabaseConfig;

public class NMMartApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        SupabaseConfig.init(this);
    }
}

