package com.nmmart.retailos.ui.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.nmmart.retailos.R;
import com.nmmart.retailos.models.NotificationItem;
import com.nmmart.retailos.ui.adapters.NotificationsAdapter;

import java.util.ArrayList;
import java.util.List;

public class NotificationsActivity extends AppCompatActivity {

    private RecyclerView rvNotifications;
    private LinearLayout layoutEmptyState;
    private NotificationsAdapter adapter;
    private List<NotificationItem> notificationList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        rvNotifications = findViewById(R.id.rvNotifications);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);

        setupRecyclerView();
        loadSampleNotifications();
    }

    private void setupRecyclerView() {
        adapter = new NotificationsAdapter(this, notificationList);
        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        rvNotifications.setAdapter(adapter);
    }

    private void loadSampleNotifications() {
        // Sample notifications - in real app, load from local storage or backend
        long now = System.currentTimeMillis();
        
        notificationList.add(new NotificationItem(
            "1",
            "Order Placed! 🎉",
            "Your order #1234 has been placed successfully and will be delivered soon.",
            now - 3600000, // 1 hour ago
            false
        ));
        
        notificationList.add(new NotificationItem(
            "2",
            "Special Offer! 🔥",
            "Get 20% off on all grocery items. Use code: SAVE20",
            now - 86400000, // 1 day ago
            true
        ));
        
        notificationList.add(new NotificationItem(
            "3",
            "Wallet Top-up Successful",
            "Your wallet has been credited with ₹500",
            now - 172800000, // 2 days ago
            true
        ));

        updateUI();
    }

    private void updateUI() {
        if (notificationList.isEmpty()) {
            rvNotifications.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.VISIBLE);
        } else {
            rvNotifications.setVisibility(View.VISIBLE);
            layoutEmptyState.setVisibility(View.GONE);
            adapter.updateNotifications(notificationList);
        }
    }
}
