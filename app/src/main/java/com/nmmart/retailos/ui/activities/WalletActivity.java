package com.nmmart.retailos.ui.activities;

import com.nmmart.retailos.R;
import com.nmmart.retailos.data.SupabaseRepository;
import com.nmmart.retailos.models.WalletMaster;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
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
        tvBalance.setText("₹" + sessionManager.getWalletBalance());

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
                        float amount = Float.parseFloat(amountStr);
                        float newBalance = sessionManager.getWalletBalance() + amount;
                        sessionManager.setWalletBalance(newBalance);
                        tvBalance.setText("₹" + newBalance);
                        Toast.makeText(this, "₹" + amount + " added successfully!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
        });

        // Fetch wallet from Supabase
        fetchWallet();
    }

    private void fetchWallet() {
        repository.getWallets(new Callback<List<WalletMaster>>() {
            @Override
            public void onResponse(Call<List<WalletMaster>> call, Response<List<WalletMaster>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<WalletMaster> wallets = response.body();
                    if (!wallets.isEmpty()) {
                        WalletMaster wallet = wallets.get(0); // For simplicity, use first wallet (or find by user_id)
                        tvBalance.setText("₹" + wallet.currentBalance);
                        sessionManager.setWalletBalance((float) wallet.currentBalance);
                    }
                }
            }

            @Override
            public void onFailure(Call<List<WalletMaster>> call, Throwable t) {
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
