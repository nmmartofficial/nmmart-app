package com.nmmart.retailos.ui.activities;

import com.nmmart.retailos.R;
import com.nmmart.retailos.data.SessionManager;
import com.nmmart.retailos.data.SupabaseAuthConfig;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SplashActivity extends AppCompatActivity {

    private int tapCount = 0;
    private long lastTapTime = 0;
    private boolean navigateScheduled = false;
    private Handler handler;
    private Runnable navigateRunnable;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        sessionManager = new SessionManager(this);
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
                if (!sessionManager.isOnboardingCompleted()) {
                    navigateTo(OnboardingActivity.class);
                } else if (sessionManager.isLoggedIn()) {
                    // Check if access token is expired and refresh it
                    long now = System.currentTimeMillis() / 1000L;
                    if (!sessionManager.hasValidAccessToken(now) && !sessionManager.getRefreshToken().isEmpty()) {
                        refreshAccessTokenAndNavigate();
                    } else {
                        navigateToMainOrLocation();
                    }
                } else {
                    navigateTo(LoginActivity.class);
                }
            } catch (Exception e) {
                android.util.Log.e("SplashActivity", "Error navigating", e);
                navigateTo(LoginActivity.class);
            }
        };

        handler.postDelayed(navigateRunnable, 1000);
        navigateScheduled = true;
    }

    private void refreshAccessTokenAndNavigate() {
        SupabaseAuthConfig.RefreshTokenRequest request = new SupabaseAuthConfig.RefreshTokenRequest(
                sessionManager.getRefreshToken()
        );
        SupabaseAuthConfig.getService().refreshAccessToken(
                SupabaseAuthConfig.getApiKey(),
                request
        ).enqueue(new Callback<SupabaseAuthConfig.AuthSessionResponse>() {
            @Override
            public void onResponse(Call<SupabaseAuthConfig.AuthSessionResponse> call, Response<SupabaseAuthConfig.AuthSessionResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().accessToken != null) {
                    // Update session with new tokens
                    SupabaseAuthConfig.AuthSessionResponse newSession = response.body();
                    long now = System.currentTimeMillis() / 1000L;
                    long newExpiresAt = now + Math.max(0L, newSession.expiresInSec - 30L);
                    sessionManager.updateAuthTokens(
                            newSession.accessToken,
                            newSession.refreshToken,
                            newExpiresAt
                    );
                    navigateToMainOrLocation();
                } else {
                    navigateTo(LoginActivity.class);
                }
            }

            @Override
            public void onFailure(Call<SupabaseAuthConfig.AuthSessionResponse> call, Throwable t) {
                android.util.Log.e("SplashActivity", "Failed to refresh token", t);
                navigateTo(LoginActivity.class);
            }
        });
    }

    private void navigateToMainOrLocation() {
        String location = sessionManager.getDeliveryLocation();
        if (location == null || location.isEmpty() || location.equals("Select Location")) {
            navigateTo(LocationSelectionActivity.class);
        } else {
            navigateTo(MainActivity.class);
        }
    }

    private void navigateTo(Class<?> targetActivity) {
        Intent intent = new Intent(SplashActivity.this, targetActivity);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
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
