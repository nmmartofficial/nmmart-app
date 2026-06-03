package com.nmmart.retailos.ui.activities;

import com.nmmart.retailos.R;
import com.nmmart.retailos.data.SessionManager;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private int tapCount = 0;
    private long lastTapTime = 0;
    private boolean navigateScheduled = false;
    private Handler handler;
    private Runnable navigateRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        SessionManager sessionManager = new SessionManager(this);
        handler = new Handler();

        TextView tvSplashTitle = findViewById(R.id.tvSplashTitle);
        tvSplashTitle.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastTapTime < 500) {
                        tapCount++;
                        if (tapCount == 5) {
                            tapCount = 0;
                            cancelNavigation();
                            launchDebugAudit();
                            return true;
                        }
                    } else {
                        tapCount = 1;
                    }
                    lastTapTime = currentTime;
                }
                return false;
            }
        });

        navigateRunnable = () -> {
            try {
                Intent intent;
                if (!sessionManager.isOnboardingCompleted()) {
                    intent = new Intent(SplashActivity.this, OnboardingActivity.class);
                } else if (sessionManager.isLoggedIn()) {
                    String location = sessionManager.getDeliveryLocation();
                    if (location == null || location.isEmpty() || location.equals("Select Location")) {
                        intent = new Intent(SplashActivity.this, LocationSelectionActivity.class);
                    } else {
                        intent = new Intent(SplashActivity.this, MainActivity.class);
                    }
                } else {
                    intent = new Intent(SplashActivity.this, LoginActivity.class);
                }
                
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                finish();
            } catch (Exception e) {
                android.util.Log.e("SplashActivity", "Error navigating", e);
                Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        };

        handler.postDelayed(navigateRunnable, 1000);
        navigateScheduled = true;
    }

    private void cancelNavigation() {
        if (navigateScheduled && handler != null && navigateRunnable != null) {
            handler.removeCallbacks(navigateRunnable);
            navigateScheduled = false;
        }
    }

    private void launchDebugAudit() {
        Toast.makeText(this, "🔍 Opening Debug Audit Panel...", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, DebugAuditActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelNavigation();
    }
}
