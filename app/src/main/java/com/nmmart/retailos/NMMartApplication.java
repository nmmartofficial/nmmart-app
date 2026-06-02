package com.nmmart.retailos;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

import com.nmmart.retailos.data.SupabaseConfig;

public class NMMartApplication extends Application {
    public static final String CHANNEL_ID_ORDERS = "orders_channel";
    public static final String CHANNEL_ID_OFFERS = "offers_channel";

    @Override
    public void onCreate() {
        super.onCreate();
        SupabaseConfig.init(this);
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

