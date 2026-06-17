package com.nmmart.retailos.ui.activities;

import com.nmmart.retailos.R;
import com.nmmart.retailos.data.SupabaseRepository;
import com.nmmart.retailos.models.WalletMaster;
import com.nmmart.retailos.utils.PriceUtils;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.nmmart.retailos.data.SessionManager;

import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WalletActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private SupabaseRepository repository;
    private TextView tvBalance;
    private TextView tvLoyaltyPoints;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallet);

        sessionManager = new SessionManager(this);
        repository = new SupabaseRepository();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("NM Wallet");
        }

        tvBalance = findViewById(R.id.tvWalletBalance);
        tvLoyaltyPoints = findViewById(R.id.tvLoyaltyPoints);
        updateBalanceUI();
        updateLoyaltyPointsUI();

        findViewById(R.id.btnAddMoney).setOnClickListener(v -> {
            android.widget.EditText et = new android.widget.EditText(this);
            et.setHint("Enter amount (e.g. 500)");
            et.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
            new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Add Money to Wallet")
                .setView(et)
                .setPositiveButton("Add", (dialog, which) -> {
                    String amountStr = et.getText().toString().trim();
                    if (!amountStr.isEmpty()) {
                        double amount = Double.parseDouble(amountStr);
                        double newBalance = sessionManager.getWalletBalance() + amount;
                        sessionManager.setWalletBalance(newBalance);
                        updateBalanceUI();
                        Toast.makeText(this, "₹" + amount + " added successfully!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
        });

        fetchWallet();
    }

    private void updateBalanceUI() {
        tvBalance.setText(PriceUtils.formatPrice(sessionManager.getWalletBalance()));
    }

    private void updateLoyaltyPointsUI() {
        tvLoyaltyPoints.setText(sessionManager.getLoyaltyPoints() + " Points");
    }

    private void fetchWallet() {
        repository.getWallets(new Callback<List<WalletMaster>>() {
            @Override
            public void onResponse(@NonNull Call<List<WalletMaster>> call, @NonNull Response<List<WalletMaster>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    WalletMaster wallet = response.body().get(0);
                    sessionManager.setWalletBalance(wallet.currentBalance);
                    sessionManager.setLoyaltyPoints(wallet.loyaltyPoints);
                    updateBalanceUI();
                    updateLoyaltyPointsUI();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<WalletMaster>> call, @NonNull Throwable t) {
                Toast.makeText(WalletActivity.this, "Couldn't load wallet balance", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
