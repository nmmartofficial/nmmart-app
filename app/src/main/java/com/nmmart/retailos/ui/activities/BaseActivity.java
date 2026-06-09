package com.nmmart.retailos.ui.activities;

import android.os.Bundle;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.nmmart.retailos.data.SessionManager;

public abstract class BaseActivity extends AppCompatActivity {
    protected SessionManager sessionManager;
    protected final String TAG = getClass().getSimpleName();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        // Initialize session manager first (before any UI)
        sessionManager = new SessionManager(this);
        
        // Set dark mode BEFORE calling super.onCreate()
        if (sessionManager.isDarkMode()) {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
        }
        
        super.onCreate(savedInstanceState);
        
        logDebug("onCreate started");
        logDebug("onCreate completed");
    }

    @Override
    protected void onStart() {
        super.onStart();
        logDebug("onStart");
    }

    @Override
    protected void onResume() {
        super.onResume();
        logDebug("onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        logDebug("onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        logDebug("onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        logDebug("onDestroy");
    }

    protected void logDebug(String message) {
        Log.d(TAG, message);
    }

    protected void logInfo(String message) {
        Log.i(TAG, message);
    }

    protected void logWarning(String message) {
        Log.w(TAG, message);
    }

    protected void logError(String message, Throwable throwable) {
        Log.e(TAG, message, throwable);
    }

    protected void logError(String message) {
        Log.e(TAG, message);
    }

    protected void logButtonClick(String buttonName) {
        logInfo("Button clicked: " + buttonName);
    }

    protected void logNavigation(String destination) {
        logInfo("Navigating to: " + destination);
    }

    protected void restartApp() {
        logDebug("restartApp called");
        android.content.Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
        if (intent != null) {
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP | android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        } else {
            logError("Failed to get launch intent for package");
        }
    }
}
