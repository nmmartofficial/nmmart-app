package com.nmmart.retailos.ui.activities;

import com.nmmart.retailos.R;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.nmmart.retailos.data.SessionManager;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ImageView logo = findViewById(R.id.ivSplashLogo);
        TextView title = findViewById(R.id.tvSplashTitle);

        // Simple Fade & Scale Animation
        Animation fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        logo.startAnimation(fadeIn);
        title.startAnimation(fadeIn);

        // 2 Seconds delay ke baad redirection logic
        new Handler().postDelayed(() -> {
            SessionManager sessionManager = new SessionManager(SplashActivity.this);
            Intent intent;
            
            if (!sessionManager.isLoggedIn()) {
                intent = new Intent(SplashActivity.this, LoginActivity.class);
            } else if (sessionManager.getDeliveryLocation().isEmpty() || sessionManager.getDeliveryLocation().equals("Select Location")) {
                intent = new Intent(SplashActivity.this, LocationSelectionActivity.class);
            } else {
                intent = new Intent(SplashActivity.this, MainActivity.class);
            }
            
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, 2500);
    }
}
