package com.nmmart.retailos.ui.activities;

import com.nmmart.retailos.R;
import com.nmmart.retailos.data.SessionManager;
import com.nmmart.retailos.data.SupabaseConfig;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SignupActivity extends AppCompatActivity {

    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        sessionManager = new SessionManager(this);

        TextInputEditText etName = findViewById(R.id.etName);
        TextInputEditText etMobile = findViewById(R.id.etEmail); // Use etEmail for mobile (optional)
        MaterialButton btnSignup = findViewById(R.id.btnSignup);
        TextView tvLoginLink = findViewById(R.id.tvLoginLink);

        // Update hint for mobile
        etMobile.setHint("Mobile Number (Optional)");

        btnSignup.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String mobile = etMobile.getText().toString().trim();

            if (name.isEmpty()) {
                Toast.makeText(this, "Please enter your name!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Create a simple user without OTP
            createSimpleUser(name, mobile);
        });

        tvLoginLink.setOnClickListener(v -> {
            finish(); // Go back to Login
        });
    }

    private void createSimpleUser(String name, String mobile) {
        // Generate a simple user ID
        String userId = UUID.randomUUID().toString();
        
        // Set session with very long expiry (10 years)
        long now = System.currentTimeMillis() / 1000L;
        long expiresAt = now + (10L * 365L * 24L * 60L * 60L); // 10 years

        // Save to session manager
        sessionManager.setLogin(true, mobile.isEmpty() ? "guest" : mobile, "");
        sessionManager.setAuthSession(
                userId,
                mobile.isEmpty() ? "" : mobile,
                "simple_auth_token_" + userId,
                "simple_refresh_token_" + userId,
                expiresAt
        );
        sessionManager.setUserName(name);

        // Save user to Supabase
        saveUserToSupabase(name, mobile, userId);

        // Navigate to main activity
        Intent intent = new Intent(SignupActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
        
        Toast.makeText(this, "Welcome " + name + "!", Toast.LENGTH_SHORT).show();
    }

    private void saveUserToSupabase(String name, String mobile, String userId) {
        Map<String, String> userData = new HashMap<>();
        userData.put("id", userId);
        userData.put("name", name);
        if (!mobile.isEmpty()) {
            userData.put("mobile", mobile);
        }

        SupabaseConfig.getService().insertUser(
                SupabaseConfig.getApiKey(),
                SupabaseConfig.getAuthorizationHeader(),
                userData
        ).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                // User saved successfully (background)
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                // Log failure, but don't block user
            }
        });
    }
}
