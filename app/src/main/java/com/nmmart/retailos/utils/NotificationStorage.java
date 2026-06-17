package com.nmmart.retailos.utils;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.nmmart.retailos.models.NotificationItem;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class NotificationStorage {
    private static final String PREF_NAME = "notifications_pref";
    private static final String KEY_NOTIFICATIONS = "notifications_list";
    private static NotificationStorage instance;
    private SharedPreferences prefs;
    private Gson gson;

    private NotificationStorage(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public static synchronized NotificationStorage getInstance(Context context) {
        if (instance == null) {
            instance = new NotificationStorage(context.getApplicationContext());
        }
        return instance;
    }

    public List<NotificationItem> getNotifications() {
        String json = prefs.getString(KEY_NOTIFICATIONS, null);
        Type type = new TypeToken<List<NotificationItem>>() {}.getType();
        List<NotificationItem> notifications = gson.fromJson(json, type);
        return notifications != null ? notifications : new ArrayList<>();
    }

    public void saveNotification(NotificationItem notification) {
        List<NotificationItem> notifications = getNotifications();
        notifications.add(0, notification); // Add to top
        if (notifications.size() > 50) {
            notifications.remove(notifications.size() - 1); // Keep last 50
        }
        saveNotifications(notifications);
    }

    public void saveNotifications(List<NotificationItem> notifications) {
        String json = gson.toJson(notifications);
        prefs.edit().putString(KEY_NOTIFICATIONS, json).apply();
    }

    public void markAsRead(String id) {
        List<NotificationItem> notifications = getNotifications();
        for (NotificationItem item : notifications) {
            if (item.getId().equals(id)) {
                item.setRead(true);
                break;
            }
        }
        saveNotifications(notifications);
    }

    public void deleteNotification(String id) {
        List<NotificationItem> notifications = getNotifications();
        notifications.removeIf(item -> item.getId().equals(id));
        saveNotifications(notifications);
    }

    public void clearAll() {
        prefs.edit().remove(KEY_NOTIFICATIONS).apply();
    }
}