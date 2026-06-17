package com.nmmart.retailos.ui.activities;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.nmmart.retailos.R;
import com.nmmart.retailos.data.SelfCheckoutCartManager;
import com.nmmart.retailos.data.SessionManager;
import com.nmmart.retailos.data.SupabaseRepository;
import com.nmmart.retailos.models.Product;
import com.nmmart.retailos.ui.adapters.SelfCheckoutCartAdapter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SelfCheckoutCartActivity extends AppCompatActivity {
    private SelfCheckoutCartManager cartManager;
    private SelfCheckoutCartAdapter adapter;
    private android.widget.TextView tvTotal;
    private SupabaseRepository supabaseRepository;
    private SessionManager sessionManager;
    private ActivityResultLauncher<Intent> paymentLauncher;
    private double orderTotal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_self_checkout_cart);

        cartManager = SelfCheckoutCartManager.getInstance(this);
        supabaseRepository = new SupabaseRepository();
        sessionManager = new SessionManager(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        RecyclerView rvCartItems = findViewById(R.id.rvCartItems);
        tvTotal = findViewById(R.id.tvTotal);
        MaterialButton btnProceedToPay = findViewById(R.id.btnProceedToPay);
        FloatingActionButton fabAddMore = findViewById(R.id.fabAddMore);

        adapter = new SelfCheckoutCartAdapter(this, this::updateTotal);
        rvCartItems.setLayoutManager(new LinearLayoutManager(this));
        rvCartItems.setAdapter(adapter);

        loadCartItems();
        updateTotal();

        btnProceedToPay.setOnClickListener(v -> proceedToPayment());
        fabAddMore.setOnClickListener(v -> {
            finish();
        });

        paymentLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK) {
                Intent data = result.getData();
                if (data != null) {
                    String response = data.getStringExtra("response");
                    if (response != null && response.toLowerCase().contains("success")) {
                        placeOrder();
                    } else {
                        Toast.makeText(this, "Payment failed or cancelled", Toast.LENGTH_SHORT).show();
                    }
                }
            } else {
                Toast.makeText(this, "Payment cancelled", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCartItems();
        updateTotal();
    }

    private void loadCartItems() {
        List<Product> items = cartManager.getCartItems();
        adapter.setItems(items);
    }

    private void updateTotal() {
        orderTotal = cartManager.getTotalPrice();
        tvTotal.setText(String.format("₹%.2f", orderTotal));
    }

    private void proceedToPayment() {
        if (cartManager.getCartCount() == 0) {
            Toast.makeText(this, "Cart is empty!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Enter your store's UPI ID here!
        String upiId = "paytmqr5fwdiq@ptys";
        String name = "NM Mart";
        String transactionNote = "Self Checkout Payment";
        String transactionRef = "TXN" + System.currentTimeMillis();

        Uri uri = Uri.parse("upi://pay").buildUpon()
                .appendQueryParameter("pa", upiId)
                .appendQueryParameter("pn", name)
                .appendQueryParameter("tn", transactionNote)
                .appendQueryParameter("am", String.format("%.2f", orderTotal))
                .appendQueryParameter("cu", "INR")
                .appendQueryParameter("tr", transactionRef)
                .build();

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(uri);
        Intent chooser = Intent.createChooser(intent, "Pay with");

        if (chooser.resolveActivity(getPackageManager()) != null) {
            paymentLauncher.launch(chooser);
        } else {
            Toast.makeText(this, "No UPI app found on your device", Toast.LENGTH_SHORT).show();
        }
    }

    private void placeOrder() {
        List<Product> items = cartManager.getCartItems();
        Map<String, Integer> quantities = cartManager.getCartQuantities();

        Map<String, Object> orderData = new HashMap<>();
        orderData.put("user_id", sessionManager.getUserId());
        orderData.put("user_mobile", sessionManager.getMobile());
        orderData.put("customer_name", sessionManager.getUserName());
        orderData.put("subtotal", orderTotal);
        orderData.put("delivery_charge", 0.0);
        orderData.put("discount", 0.0);
        orderData.put("total_amount", orderTotal);
        orderData.put("payment_mode", "UPI");
        orderData.put("payment_status", "paid");
        orderData.put("order_status", "completed");

        // Award loyalty points: 1 point for every ₹10 spent
        int pointsEarned = (int) (orderTotal / 10);
        sessionManager.addLoyaltyPoints(pointsEarned);

        supabaseRepository.placeOrder(orderData, new retrofit2.Callback<Void>() {
            @Override
            public void onResponse(retrofit2.Call<Void> call, retrofit2.Response<Void> response) {
                if (response.isSuccessful()) {
                    decrementStockForItems(items, quantities);
                } else {
                    Toast.makeText(SelfCheckoutCartActivity.this, "Failed to place order", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(retrofit2.Call<Void> call, Throwable t) {
                Toast.makeText(SelfCheckoutCartActivity.this, "Failed to place order: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void decrementStockForItems(List<Product> items, Map<String, Integer> quantities) {
        final int[] successCount = {0};
        final int totalItems = items.size();

        for (Product product : items) {
            int qty = quantities.getOrDefault(product.id, 1);
            supabaseRepository.decrementStock(product.id, qty, new retrofit2.Callback<Void>() {
                @Override
                public void onResponse(retrofit2.Call<Void> call, retrofit2.Response<Void> response) {
                    successCount[0]++;
                    if (successCount[0] == totalItems) {
                        cartManager.clearCart();
                        Intent intent = new Intent(SelfCheckoutCartActivity.this, ExitPassActivity.class);
                        startActivity(intent);
                        finish();
                    }
                }

                @Override
                public void onFailure(retrofit2.Call<Void> call, Throwable t) {
                    successCount[0]++;
                    if (successCount[0] == totalItems) {
                        cartManager.clearCart();
                        Intent intent = new Intent(SelfCheckoutCartActivity.this, ExitPassActivity.class);
                        startActivity(intent);
                        finish();
                    }
                }
            });
        }
    }
}
