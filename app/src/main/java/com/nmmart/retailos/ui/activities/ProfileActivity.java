package com.nmmart.retailos.ui.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.nmmart.retailos.R;
import com.nmmart.retailos.data.SessionManager;

public class ProfileActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private TextView tvName, tvMobile, tvEmail;
    private ImageView ivProfilePic;
    private ActivityResultLauncher<String> pickImageLauncher;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        sessionManager = new SessionManager(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.my_profile);
        }

        tvName = findViewById(R.id.tvProfileName);
        tvMobile = findViewById(R.id.tvProfileMobile);
        tvEmail = findViewById(R.id.tvProfileEmail);
        ivProfilePic = findViewById(R.id.ivProfilePic);

        initLaunchers();
        loadProfileData();

        findViewById(R.id.btnEditProfile).setOnClickListener(v -> showEditProfileDialog());
        findViewById(R.id.btnWalletHistory).setOnClickListener(v -> {
            startActivity(new Intent(this, WalletTransactionHistoryActivity.class));
        });
        findViewById(R.id.cardProfilePic).setOnClickListener(v -> checkPermissionAndPickImage());

        SwitchMaterial switchDark = findViewById(R.id.switchDarkMode);
        switchDark.setChecked(sessionManager.isDarkMode());
        switchDark.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sessionManager.setDarkMode(isChecked);
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(isChecked ? androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES : androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
        });
    }

    private void initLaunchers() {
        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                // Take persistable URI permission
                final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
                getContentResolver().takePersistableUriPermission(uri, takeFlags);
                sessionManager.setProfilePicUri(uri.toString());
                loadProfileData();
                Toast.makeText(this, R.string.profile_updated, Toast.LENGTH_SHORT).show();
            }
        });

        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                pickImage();
            } else {
                Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadProfileData() {
        if (sessionManager.isLoggedIn()) {
            tvMobile.setText(sessionManager.getMobile() != null ? "+91 " + sessionManager.getMobile() : "");
            tvName.setText(sessionManager.getUserName() != null ? sessionManager.getUserName() : getString(R.string.nm_mart_user));
            String email = sessionManager.getEmail();
            tvEmail.setText(email != null && !email.isEmpty() ? email : "");
            tvEmail.setVisibility(email != null && !email.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
            
            String profilePicUri = sessionManager.getProfilePicUri();
            if (profilePicUri != null) {
                ivProfilePic.setPadding(0, 0, 0, 0);
                Glide.with(this).load(Uri.parse(profilePicUri)).circleCrop().into(ivProfilePic);
            }
        }
    }

    private void checkPermissionAndPickImage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED) {
                pickImage();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                pickImage();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }
    }

    private void pickImage() {
        pickImageLauncher.launch("image/*");
    }

    private void showEditProfileDialog() {
        android.view.View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_profile, null);
        android.widget.EditText etName = dialogView.findViewById(R.id.etName);
        android.widget.EditText etEmail = dialogView.findViewById(R.id.etEmail);

        etName.setText(sessionManager.getUserName());
        etEmail.setText(sessionManager.getEmail());

        new AlertDialog.Builder(this)
            .setTitle(R.string.edit_profile)
            .setView(dialogView)
            .setPositiveButton(R.string.save, (dialog, which) -> {
                String newName = etName.getText().toString().trim();
                String newEmail = etEmail.getText().toString().trim();

                if (!newName.isEmpty()) {
                    sessionManager.setUserName(newName);
                    tvName.setText(newName);
                }

                if (!newEmail.isEmpty()) {
                    sessionManager.setEmail(newEmail);
                    tvEmail.setText(newEmail);
                    tvEmail.setVisibility(android.view.View.VISIBLE);
                } else {
                    tvEmail.setVisibility(android.view.View.GONE);
                }

                Toast.makeText(this, R.string.profile_updated, Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
