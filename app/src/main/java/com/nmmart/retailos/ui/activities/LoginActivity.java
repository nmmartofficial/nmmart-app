package com.nmmart.retailos.ui.activities;

import android.util.Log;
import com.nmmart.retailos.BuildConfig;
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

    // Bypass number (only for DEBUG)
    private static final String BYPASS_NUMBER = "7081154604";

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
                String input = etMobile.getText().toString().trim();
                
                // Check for bypass (only in DEBUG mode)
                if (BuildConfig.DEBUG && input.equals(BYPASS_NUMBER)) {
                    bypassLogin();
                    return;
                }
                
                handleSendOtp(input);
            }
        });

        // Verify OTP button click listener
        btnVerifyOtp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String input = etMobile.getText().toString().trim();
                String enteredOtp = etOtp.getText().toString().trim();
                
                // Check for bypass (only in DEBUG mode)
                if (BuildConfig.DEBUG && input.equals(BYPASS_NUMBER)) {
                    bypassLogin();
                    return;
                }
                
                handleVerifyOtp(input, enteredOtp);
            }
        });

        findViewById(R.id.tvSignupLink).setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
            startActivity(intent);
        });
    }

    private void bypassLogin() {
        // Bypass login - no OTP needed
        sessionManager.setLogin(true, BYPASS_NUMBER, "");
        
        // Set dummy auth session to satisfy isLoggedIn() check
        long dummyExpiresAt = (System.currentTimeMillis() / 1000L) + (365 * 24 * 60 * 60L); // 1 year
        sessionManager.setAuthSession(
                "bypass_user_id", 
                "+91" + BYPASS_NUMBER, 
                "dummy_access_token", 
                "dummy_refresh_token", 
                dummyExpiresAt
        );
        
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
        
        Toast.makeText(LoginActivity.this, "Bypass login successful!", Toast.LENGTH_SHORT).show();
    }

    private void handleSendOtp(String input) {
        if (input.isEmpty()) {
            etMobile.setError("Mobile/Email is required");
            return;
        }

        etMobile.setError(null);
        btnSendOtp.setEnabled(false);
        
        boolean isEmail = android.util.Patterns.EMAIL_ADDRESS.matcher(input).matches();
        
        Call<Void> call;
        if (isEmail) {
            call = SupabaseAuthConfig.getService().requestEmailOtp(
                    SupabaseAuthConfig.getApiKey(),
                    new SupabaseAuthConfig.EmailOtpRequest(input, true)
            );
        } else {
            String phoneE164 = toE164India(input);
            call = SupabaseAuthConfig.getService().requestOtp(
                    SupabaseAuthConfig.getApiKey(),
                    new SupabaseAuthConfig.OtpRequest(phoneE164, true)
            );
        }

        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                btnSendOtp.setEnabled(true);
                if (response.isSuccessful()) {
                    tilOtp.setVisibility(View.VISIBLE);
                    btnVerifyOtp.setVisibility(View.VISIBLE);
                    btnSendOtp.setVisibility(View.GONE);
                    Toast.makeText(LoginActivity.this, "OTP sent to " + input, Toast.LENGTH_SHORT).show();
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

    private void handleVerifyOtp(String input, String enteredOtp) {
        if (enteredOtp.isEmpty()) {
            etOtp.setError("Please enter OTP");
            return;
        }

        if (enteredOtp.length() < 6) {
            etOtp.setError("OTP must be 6 digits");
            return;
        }

        etOtp.setError(null);
        btnVerifyOtp.setEnabled(false);

        boolean isEmail = android.util.Patterns.EMAIL_ADDRESS.matcher(input).matches();
        
        Call<SupabaseAuthConfig.AuthSessionResponse> call;
        if (isEmail) {
            call = SupabaseAuthConfig.getService().verifyEmailOtp(
                    SupabaseAuthConfig.getApiKey(),
                    new SupabaseAuthConfig.VerifyEmailRequest("email", input, enteredOtp)
            );
        } else {
            String phoneE164 = toE164India(input);
            call = SupabaseAuthConfig.getService().verifyOtp(
                    SupabaseAuthConfig.getApiKey(),
                    new SupabaseAuthConfig.VerifyRequest("sms", phoneE164, enteredOtp)
            );
        }

        call.enqueue(new Callback<SupabaseAuthConfig.AuthSessionResponse>() {
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
                if (isEmail) {
                    sessionManager.setAuthSession(userId, "", session.accessToken, session.refreshToken, expiresAt);
                    sessionManager.setLogin(true, "", input);
                } else {
                    sessionManager.setAuthSession(userId, input, session.accessToken, session.refreshToken, expiresAt);
                    sessionManager.setLogin(true, input, "");
                }

                saveUserToSupabase(input, session.accessToken);

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

    private void saveUserToSupabase(String input, String accessToken) {
        Map<String, String> user = new HashMap<>();
        boolean isEmail = android.util.Patterns.EMAIL_ADDRESS.matcher(input).matches();
        if (isEmail) {
            user.put("email", input);
        } else {
            user.put("mobile", input);
        }

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
