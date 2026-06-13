package com.nmmart.retailos;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.nmmart.retailos.data.SupabaseConfig;
import com.nmmart.retailos.utils.NMMartLogger;
import com.nmmart.retailos.ui.activities.MainActivity;

public class NMMartApplication extends Application {
    public static final String CHANNEL_ID_ORDERS = "orders_channel";
    public static final String CHANNEL_ID_OFFERS = "offers_channel";
    private static final String TAG = "NMMartApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        
        // Initialize Stetho (Android's Eruda) only if available
        if (BuildConfig.DEBUG) {
            try {
                Class<?> stethoClass = Class.forName("com.facebook.stetho.Stetho");
                stethoClass.getMethod("initializeWithDefaults", Context.class).invoke(null, this);
            } catch (Exception e) {
                Log.w(TAG, "Stetho not available", e);
            }
        }
        
        // Initialize dependencies first before using them
        SupabaseConfig.init(this);
        NMMartLogger.init(this);
        
        // Set Global Uncaught Exception Handler
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            private Thread.UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();

            @Override
            public void uncaughtException(Thread thread, Throwable ex) {
                // Log the exception
                Log.e(TAG, "Uncaught Exception in thread: " + thread.getName(), ex);
                
                // Send to NMMartLogger for Supabase
                try {
                    NMMartLogger.logError("NMMartApplication.java", "uncaughtException", 
                        ex.getClass().getName() + ": " + ex.getMessage());
                } catch (Exception e) {
                    Log.e(TAG, "Failed to log error to Supabase", e);
                }
                
                // Show toast to user
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(getApplicationContext(), 
                        "App encountered an error. Restarting...", 
                        Toast.LENGTH_LONG).show();
                });
                
                // Restart the app
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                
                // Kill the current process
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(0);
            }
        });
        
        createNotificationChannels();
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Orders Channel
            NotificationChannel ordersChannel = new NotificationChannel(
                    CHANNEL_ID_ORDERS,
                    "Order Updates",
                    NotificationManager.IMPORTANCE_HIGH
            );
            ordersChannel.setDescription("Notifications for your order status updates");

            // Offers Channel
            NotificationChannel offersChannel = new NotificationChannel(
                    CHANNEL_ID_OFFERS,
                    "Offers & Promotions",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            offersChannel.setDescription("Notifications for offers, discounts, and promotions");

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(ordersChannel);
                manager.createNotificationChannel(offersChannel);
            }
        }
    }
}

