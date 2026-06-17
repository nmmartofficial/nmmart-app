package com.nmmart.retailos.ui.activities;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.nmmart.retailos.data.SessionManager;
import com.nmmart.retailos.databinding.ActivityReferEarnBinding;

public class ReferEarnActivity extends AppCompatActivity {
    private ActivityReferEarnBinding binding;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityReferEarnBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Get referral code from SessionManager (generates if needed)
        String referralCode = sessionManager.getReferralCode();
        binding.tvReferralCode.setText(referralCode);

        binding.ivCopyCode.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Referral Code", referralCode);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Code copied to clipboard!", Toast.LENGTH_SHORT).show();
        });

        binding.btnInvite.setOnClickListener(v -> {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            String shareBody = "Hey! Use my referral code " + referralCode + " on NM Mart app to get ₹50 off on your first grocery order. Download now!";
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "NM Mart Referral");
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
            startActivity(Intent.createChooser(shareIntent, "Invite via"));
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}