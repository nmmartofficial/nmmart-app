package com.nmmart.retailos.ui.activities;

import com.nmmart.retailos.R;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.nmmart.retailos.data.SessionManager;

public class ProfileActivity extends AppCompatActivity {

    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        sessionManager = new SessionManager(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("My Profile");
        }

        TextView tvName = findViewById(R.id.tvProfileName);
        TextView tvMobile = findViewById(R.id.tvProfileMobile);

        if (sessionManager.isLoggedIn()) {
            tvMobile.setText("+91 " + sessionManager.getMobile());
            tvName.setText(sessionManager.getUserName() != null ? sessionManager.getUserName() : "NM Mart User");
        }

        findViewById(R.id.btnEditProfile).setOnClickListener(v -> {
            android.widget.EditText et = new android.widget.EditText(this);
            et.setText(sessionManager.getUserName());
            new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Update Name")
                .setView(et)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newName = et.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        sessionManager.setUserName(newName);
                        tvName.setText(newName);
                        Toast.makeText(this, "Profile Updated!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
        });

        com.google.android.material.switchmaterial.SwitchMaterial switchDark = findViewById(R.id.switchDarkMode);
        switchDark.setChecked(sessionManager.isDarkMode());
        switchDark.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sessionManager.setDarkMode(isChecked);
            if (isChecked) {
                androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
