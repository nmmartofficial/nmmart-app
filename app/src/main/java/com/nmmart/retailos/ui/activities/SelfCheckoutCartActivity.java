package com.nmmart.retailos.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

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
            // Go back to MainActivity to scan more items
            finish();
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
        double total = cartManager.getTotalPrice();
        tvTotal.setText(String.format("₹%.2f", total));
    }

    private void proceedToPayment() {
        if (cartManager.getCartCount() == 0) {
            Toast.makeText(this, "Cart is empty!", Toast.LENGTH_SHORT).show();
            return;
        }

        // For demo purposes, we'll assume payment is successful
        // In real app, integrate UPI/Payment gateway here
        placeOrder();
    }

    private void placeOrder() {
        List<Product> items = cartManager.getCartItems();
        Map<String, Integer> quantities = cartManager.getCartQuantities();
        double total = cartManager.getTotalPrice();

        // Create order data
        Map<String, Object> orderData = new HashMap<>();
        orderData.put("user_id", sessionManager.getUserId());
        orderData.put("user_mobile", sessionManager.getMobile());
        orderData.put("customer_name", sessionManager.getUserName());
        orderData.put("subtotal", total);
        orderData.put("delivery_charge", 0.0);
        orderData.put("discount", 0.0);
        orderData.put("total_amount", total);
        orderData.put("payment_mode", "Self-Checkout (App)");
        orderData.put("payment_status", "paid");
        orderData.put("order_status", "completed");

        // First, place the order
        supabaseRepository.placeOrder(orderData, new retrofit2.Callback<Void>() {
            @Override
            public void onResponse(retrofit2.Call<Void> call, retrofit2.Response<Void> response) {
                if (response.isSuccessful()) {
                    // Now decrement stock for each product
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
                        // All stock decremented, show exit pass
                        Intent intent = new Intent(SelfCheckoutCartActivity.this, ExitPassActivity.class);
                        startActivity(intent);
                        finish();
                    }
                }

                @Override
                public void onFailure(retrofit2.Call<Void> call, Throwable t) {
                    successCount[0]++;
                    if (successCount[0] == totalItems) {
                        // Even if some fail, proceed (stock can be updated manually)
                        Intent intent = new Intent(SelfCheckoutCartActivity.this, ExitPassActivity.class);
                        startActivity(intent);
                        finish();
                    }
                }
            });
        }
    }
}
