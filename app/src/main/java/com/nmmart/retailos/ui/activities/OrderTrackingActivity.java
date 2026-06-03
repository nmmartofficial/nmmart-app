package com.nmmart.retailos.ui.activities;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.nmmart.retailos.R;

public class OrderTrackingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_tracking);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Update order status UI (for demo, we'll mark first 3 steps as completed)
        updateStatusUI(3);
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
                iv.setColorFilter(getResources().getColor(R.color.orange_primary));
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
