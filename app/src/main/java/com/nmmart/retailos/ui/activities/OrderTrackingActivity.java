package com.nmmart.retailos.ui.activities;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.nmmart.retailos.R;

public class OrderTrackingActivity extends AppCompatActivity {

    private TextView tvOrderNumber;
    private TextView tvExpectedDelivery;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_tracking);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        tvOrderNumber = findViewById(R.id.tvOrderNumber);
        tvExpectedDelivery = findViewById(R.id.tvExpectedDelivery);

        // Get data from intent
        String orderId = getIntent().getStringExtra("order_id");
        String orderStatus = getIntent().getStringExtra("order_status");
        String expectedDelivery = getIntent().getStringExtra("expected_delivery");

        // Set order number
        if (orderId != null && !orderId.isEmpty()) {
            tvOrderNumber.setText(getString(R.string.order_number_label) + (orderId.length() > 8 ? orderId.substring(0, 8) : orderId));
        }

        // Set expected delivery
        if (expectedDelivery != null && !expectedDelivery.isEmpty()) {
            tvExpectedDelivery.setText(getString(R.string.expected_delivery, expectedDelivery));
        }

        // Map order status to completed steps
        int completedSteps = getCompletedStepsFromStatus(orderStatus);
        updateStatusUI(completedSteps);
    }

    private int getCompletedStepsFromStatus(String status) {
        if (status == null) return 1;
        status = status.toLowerCase();
        if (status.contains("pending")) return 1;
        if (status.contains("processing")) return 2;
        if (status.contains("packed")) return 3;
        if (status.contains("shipped") || status.contains("out")) return 4;
        if (status.contains("delivered")) return 5;
        return 1;
    }

    private void updateStatusUI(int completedSteps) {
        int[] statusImages = {
                R.id.ivStatus1,
                R.id.ivStatus2,
                R.id.ivStatus3,
                R.id.ivStatus4,
                R.id.ivStatus5
        };
        int[] statusTexts = {
                R.id.tvStatus1,
                R.id.tvStatus2,
                R.id.tvStatus3,
                R.id.tvStatus4,
                R.id.tvStatus5
        };

        for (int i = 0; i < 5; i++) {
            ImageView iv = findViewById(statusImages[i]);
            TextView tv = findViewById(statusTexts[i]);

            if (i < completedSteps) {
                iv.setImageResource(android.R.drawable.checkbox_on_background);
                iv.setColorFilter(getResources().getColor(R.color.primary));
                tv.setTextColor(getResources().getColor(R.color.black));
            } else {
                iv.setImageResource(android.R.drawable.checkbox_off_background);
                iv.setColorFilter(getResources().getColor(R.color.slate));
                tv.setTextColor(getResources().getColor(R.color.slate));
            }
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
