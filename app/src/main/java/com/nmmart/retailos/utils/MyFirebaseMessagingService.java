package com.nmmart.retailos.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.nmmart.retailos.NMMartApplication;
import com.nmmart.retailos.R;
import com.nmmart.retailos.data.SessionManager;
import com.nmmart.retailos.data.SupabaseRepository;
import com.nmmart.retailos.models.NotificationItem;
import com.nmmart.retailos.ui.activities.NotificationsActivity;

import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Call;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "MyFirebaseMessaging";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        
        if (remoteMessage.getNotification() != null) {
            String title = remoteMessage.getNotification().getTitle();
            String body = remoteMessage.getNotification().getBody();
            if (title != null && body != null) {
                showNotification(title, body);
                saveNotification(title, body);
            }
        }
    }

    private void showNotification(String title, String message) {
        Intent intent = new Intent(this, NotificationsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String channelId = NMMartApplication.CHANNEL_ID_ORDERS;
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_grocery_bag)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, getString(R.string.order_updates_channel), NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription(getString(R.string.order_updates_channel_desc));
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }

    private void saveNotification(String title, String message) {
        NotificationItem item = new NotificationItem(
                String.valueOf(System.currentTimeMillis()),
                title,
                message,
                System.currentTimeMillis(),
                false
        );
        NotificationStorage.getInstance(this).saveNotification(item);
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "New FCM token received: " + token);
        
        SessionManager sessionManager = new SessionManager(this);
        sessionManager.setFcmToken(token);
        
        if (sessionManager.isLoggedIn() && sessionManager.getUserId() != null && !sessionManager.getUserId().isEmpty()) {
            SupabaseRepository repository = new SupabaseRepository();
            repository.updateUserFcmToken(sessionManager.getUserId(), token, new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) {
                        Log.d(TAG, "FCM token saved to Supabase successfully");
                    } else {
                        Log.e(TAG, "Failed to save FCM token: " + response.message());
                    }
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    Log.e(TAG, "Error saving FCM token", t);
                }
            });
        }
    }
}
