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

        // 2 Seconds delay ke baad always go to LoginActivity
        new Handler().postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, 2000);
    }
}
