package com.nmmart.retailos.ui.activities;

import com.nmmart.retailos.R;
import com.nmmart.retailos.data.SessionManager;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SessionManager sessionManager = new SessionManager(this);

        // 1 Second delay to show the logo
        new Handler().postDelayed(() -> {
            try {
                Intent intent;
                if (sessionManager.isLoggedIn()) {
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
        }, 1000);
    }
}
