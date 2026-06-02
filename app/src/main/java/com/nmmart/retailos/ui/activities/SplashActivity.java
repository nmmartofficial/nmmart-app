package com.nmmart.retailos.ui.activities;

import com.bumptech.glide.Glide;
import com.nmmart.retailos.R;
import com.nmmart.retailos.data.SessionManager;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ImageView logo = findViewById(R.id.ivSplashLogo);

        SessionManager sessionManager = new SessionManager(this);
        String logoUrl = sessionManager.getStoreLogoUrl();
        
        if (logoUrl != null && !logoUrl.isEmpty()) {
            Glide.with(this)
                .load(logoUrl)
                .placeholder(R.drawable.nm_mart_logo)
                .error(R.drawable.nm_mart_logo)
                .into(logo);
        }

        // Simple Fade Animation
        Animation fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        logo.startAnimation(fadeIn);

        // 2 Seconds delay ke baad check login state
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
        }, 2000);
    }
}
