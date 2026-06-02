package com.nmmart.retailos.ui.activities;

import android.util.Log;
import com.nmmart.retailos.R;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.nmmart.retailos.data.SessionManager;
import com.nmmart.retailos.data.SupabaseAuthConfig;
import com.nmmart.retailos.data.SupabaseConfig;

import java.util.HashMap;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etMobile, etOtp;
    private TextInputLayout tilOtp;
    private Button btnSendOtp, btnVerifyOtp;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Views ko initialize kar rahe hain
        etMobile = findViewById(R.id.etMobile);
        etOtp = findViewById(R.id.etOtp);
        tilOtp = findViewById(R.id.tilOtp);
        btnSendOtp = findViewById(R.id.btnSendOtp);
        btnVerifyOtp = findViewById(R.id.btnVerifyOtp);
        sessionManager = new SessionManager(this);

        // Send OTP button click listener
        btnSendOtp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleSendOtp();
            }
        });

        // Verify OTP button click listener
        btnVerifyOtp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleVerifyOtp();
            }
        });

        findViewById(R.id.tvSignupLink).setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
            startActivity(intent);
        });
    }

    private void handleSendOtp() {
        String mobile = etMobile.getText().toString().trim();

        if (mobile.isEmpty()) {
            etMobile.setError("Mobile number is required");
            return;
        }

        if (mobile.length() != 10) {
            etMobile.setError("Enter a valid 10-digit number");
            return;
        }

        etMobile.setError(null);
        String phoneE164 = toE164India(mobile);
        btnSendOtp.setEnabled(false);

        SupabaseAuthConfig.getService().requestOtp(
                SupabaseAuthConfig.getApiKey(),
                new SupabaseAuthConfig.OtpRequest(phoneE164, true)
        ).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                btnSendOtp.setEnabled(true);
                if (response.isSuccessful()) {
                    tilOtp.setVisibility(View.VISIBLE);
                    btnVerifyOtp.setVisibility(View.VISIBLE);
                    btnSendOtp.setVisibility(View.GONE);
                    Toast.makeText(LoginActivity.this, "OTP sent to " + mobile, Toast.LENGTH_SHORT).show();
                } else {
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "No error body";
                        Log.e("LoginActivity", "OTP send failed: " + response.code() + " - " + errorBody);
                        Toast.makeText(LoginActivity.this, "Failed to send OTP: " + errorBody, Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Log.e("LoginActivity", "Error reading error body", e);
                        Toast.makeText(LoginActivity.this, "Failed to send OTP. Try again.", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                btnSendOtp.setEnabled(true);
                Log.e("LoginActivity", "OTP network error", t);
                Toast.makeText(LoginActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void handleVerifyOtp() {
        String mobile = etMobile.getText().toString().trim();
        String enteredOtp = etOtp.getText().toString().trim();

        if (mobile.length() != 10) {
            etMobile.setError("Enter valid mobile number");
            return;
        }

        if (enteredOtp.isEmpty()) {
            etOtp.setError("Please enter OTP");
            return;
        }

        if (enteredOtp.length() < 6) {
            etOtp.setError("OTP must be 6 digits");
            return;
        }

        etOtp.setError(null);
        String phoneE164 = toE164India(mobile);
        btnVerifyOtp.setEnabled(false);

        SupabaseAuthConfig.getService().verifyOtp(
                SupabaseAuthConfig.getApiKey(),
                new SupabaseAuthConfig.VerifyRequest("sms", phoneE164, enteredOtp)
        ).enqueue(new Callback<SupabaseAuthConfig.AuthSessionResponse>() {
            @Override
            public void onResponse(Call<SupabaseAuthConfig.AuthSessionResponse> call, Response<SupabaseAuthConfig.AuthSessionResponse> response) {
                btnVerifyOtp.setEnabled(true);
                if (!response.isSuccessful() || response.body() == null || response.body().accessToken == null) {
                    Toast.makeText(LoginActivity.this, "Invalid OTP or login failed.", Toast.LENGTH_SHORT).show();
                    return;
                }

                SupabaseAuthConfig.AuthSessionResponse session = response.body();
                long now = System.currentTimeMillis() / 1000L;
                long expiresAt = now + Math.max(0L, session.expiresInSec - 30L);

                String userId = session.user != null ? session.user.id : "";
                sessionManager.setAuthSession(userId, phoneE164, session.accessToken, session.refreshToken, expiresAt);
                sessionManager.setLogin(true, mobile, ""); // Empty email for phone login

                saveUserToSupabase(mobile, session.accessToken);

                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void onFailure(Call<SupabaseAuthConfig.AuthSessionResponse> call, Throwable t) {
                btnVerifyOtp.setEnabled(true);
                Toast.makeText(LoginActivity.this, "Network error. Try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveUserToSupabase(String mobile, String accessToken) {
        Map<String, String> user = new HashMap<>();
        user.put("mobile", mobile);

        SupabaseConfig.getService().insertUser(
                SupabaseConfig.getApiKey(),
                "Bearer " + accessToken,
                user
        ).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                // Background mein ho jayega, user ko disturbance nahi hogi
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                // Log failure if needed
            }
        });
    }

    private static String toE164India(String mobile10) {
        if (mobile10 == null) return "";
        String trimmed = mobile10.trim();
        if (trimmed.startsWith("+")) return trimmed;
        return "+91" + trimmed;
    }
}
