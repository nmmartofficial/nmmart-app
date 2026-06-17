package com.nmmart.retailos.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import com.nmmart.retailos.R;
import com.nmmart.retailos.models.NotificationItem;
import com.nmmart.retailos.ui.activities.MainActivity;
import com.nmmart.retailos.ui.activities.OrderHistoryActivity;

public class NotificationHelper {
    private static final String CHANNEL_ID = "nmmart_orders";
    private static final String CHANNEL_NAME = "Order Updates";

    public static void showOrderNotification(Context context, String title, String message) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        Intent intent = new Intent(context, OrderHistoryActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_grocery_bag)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        // Save to local storage
        NotificationItem notificationItem = new NotificationItem(
                String.valueOf(System.currentTimeMillis()),
                title,
                message,
                System.currentTimeMillis(),
                false
        );
        NotificationStorage.getInstance(context).saveNotification(notificationItem);

        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }
}