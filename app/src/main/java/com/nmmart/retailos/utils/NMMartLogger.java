package com.nmmart.retailos.utils;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.nmmart.retailos.data.SupabaseRepository;
import com.nmmart.retailos.models.AppError;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NMMartLogger {
    private static final String TAG = "NMMart_Logger";
    private static SupabaseRepository repository;

    public static void init(Context context) {
        if (repository == null) {
            repository = new SupabaseRepository();
        }
    }

    public static void logClick(String buttonName) {
        Log.i(TAG, "[CLICK] Button clicked: " + buttonName);
    }

    public static void logFunction(String functionName) {
        Log.i(TAG, "[FUNCTION] Function triggered: " + functionName);
    }

    public static void logDataFetch(String dataType) {
        Log.i(TAG, "[DATA_FETCH] Fetching data: " + dataType);
    }

    public static void logError(String fileName, String functionName, String errorMessage) {
        Log.e(TAG, String.format("[ERROR] %s -> %s -> %s", fileName, functionName, errorMessage));
        
        // Send to Supabase
        try {
            if (repository != null) {
                String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
                String deviceInfo = getDeviceInfo();
                
                AppError error = new AppError(fileName, functionName, errorMessage, timestamp, deviceInfo);
                repository.insertAppError(error);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to send error to Supabase", e);
        }
    }

    private static String getDeviceInfo() {
        return String.format(
            "Model: %s, Manufacturer: %s, Android Version: %s (API %d)",
            Build.MODEL,
            Build.MANUFACTURER,
            Build.VERSION.RELEASE,
            Build.VERSION.SDK_INT
        );
    }
}
