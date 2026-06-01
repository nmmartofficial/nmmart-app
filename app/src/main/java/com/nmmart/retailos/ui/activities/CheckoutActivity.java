package com.nmmart.retailos.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.nmmart.retailos.R;
import com.nmmart.retailos.data.CartManager;
import com.nmmart.retailos.data.CouponValidationResult;
import com.nmmart.retailos.data.SupabaseRepository;
import com.nmmart.retailos.models.PincodeMaster;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CheckoutActivity extends BaseActivity {

    private static final double HANDLING_CHARGE = 5.0;
    private static final double DELIVERY_CHARGE = 10.0;

    private SupabaseRepository repository;
    private List<PincodeMaster> pincodes;
    private List<com.nmmart.retailos.models.Address> userAddresses;
    private com.nmmart.retailos.models.Address selectedAddress;
    private android.widget.TextView tvToPay, tvSelectedName, tvSelectedDetails;
    private android.view.View layoutSelectedAddress;
    private MaterialButton btnAddAddress;
    private String appliedCouponCode = "";
    private double appliedDiscount = 0.0;
    private double itemsTotal = 0.0;
    private double toPay = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        repository = new SupabaseRepository();

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        tvSelectedName = findViewById(R.id.tvSelectedAddressName);
        tvSelectedDetails = findViewById(R.id.tvSelectedAddressDetails);
        layoutSelectedAddress = findViewById(R.id.layoutSelectedAddress);
        btnAddAddress = findViewById(R.id.btnAddAddress);
        
        tvToPay = findViewById(R.id.tvToPay);
        MaterialButton btnPlaceOrder = findViewById(R.id.btnPlaceOrder);

        itemsTotal = CartManager.getInstance(this).getTotalPrice();
        appliedCouponCode = getIntent().getStringExtra("COUPON_CODE");
        recalcToPay();
        applyCouponIfAny();

        fetchPincodes();
        fetchAddresses();
        setupDateTimePickers();

        btnAddAddress.setOnClickListener(v -> {
            startActivity(new Intent(this, AddressActivity.class));
        });

        findViewById(R.id.btnChangeAddress).setOnClickListener(v -> {
            showAddressSelectionDialog();
        });

        btnPlaceOrder.setOnClickListener(v -> {
            if (!sessionManager.isLoggedIn()) {
                Toast.makeText(this, "Please login to place your order!", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, LoginActivity.class));
                return;
            }

            if (selectedAddress == null) {
                Toast.makeText(this, "Please add a delivery address!", Toast.LENGTH_SHORT).show();
                return;
            }

            placeOrder(selectedAddress.fullName, selectedAddress.houseNo, selectedAddress.landmark != null ? selectedAddress.landmark : "", selectedAddress.pincode, toPay);
        });
    }

    private void fetchAddresses() {
        if (!sessionManager.isLoggedIn()) return;
        
        repository.getUserAddresses(sessionManager.getUserId(), new Callback<List<com.nmmart.retailos.models.Address>>() {
            @Override
            public void onResponse(Call<List<com.nmmart.retailos.models.Address>> call, Response<List<com.nmmart.retailos.models.Address>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    userAddresses = response.body();
                    // Auto-select default or first address
                    selectedAddress = userAddresses.get(0);
                    for (com.nmmart.retailos.models.Address a : userAddresses) {
                        if (a.isDefault) {
                            selectedAddress = a;
                            break;
                        }
                    }
                    updateAddressUI();
                } else {
                    layoutSelectedAddress.setVisibility(android.view.View.GONE);
                    btnAddAddress.setVisibility(android.view.View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<List<com.nmmart.retailos.models.Address>> call, Throwable t) {}
        });
    }

    private void updateAddressUI() {
        if (selectedAddress != null) {
            layoutSelectedAddress.setVisibility(android.view.View.VISIBLE);
            btnAddAddress.setVisibility(android.view.View.GONE);
            tvSelectedName.setText(selectedAddress.fullName);
            tvSelectedDetails.setText(selectedAddress.houseNo + ", " + selectedAddress.city + " - " + selectedAddress.pincode);
        }
    }

    private void showAddressSelectionDialog() {
        if (userAddresses == null || userAddresses.isEmpty()) return;

        String[] items = new String[userAddresses.size()];
        for (int i = 0; i < userAddresses.size(); i++) {
            items[i] = userAddresses.get(i).fullName + "\n" + userAddresses.get(i).houseNo;
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Select Address")
            .setItems(items, (dialog, which) -> {
                selectedAddress = userAddresses.get(which);
                updateAddressUI();
            })
            .setPositiveButton("+ Add New", (dialog, which) -> {
                startActivity(new Intent(this, AddressActivity.class));
            })
            .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchAddresses();
    }

    private void setupDateTimePickers() {
        // Month Spinner
        android.widget.Spinner spinnerMonth = findViewById(R.id.spinnerMonth);
        java.util.List<String> months = new java.util.ArrayList<>();
        months.add("June 2026");
        android.widget.ArrayAdapter<String> monthAdapter = new android.widget.ArrayAdapter<>(this, android.R.layout.simple_spinner_item, months);
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMonth.setAdapter(monthAdapter);

        // Dates RecyclerView
        androidx.recyclerview.widget.RecyclerView rvDates = findViewById(R.id.rvDates);
        rvDates.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this, androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false));
        // Use a simple adapter or just placeholder for now
        
        // Time Slots RecyclerView
        androidx.recyclerview.widget.RecyclerView rvTimeSlots = findViewById(R.id.rvTimeSlots);
        rvTimeSlots.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this, androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false));
    }

    private void recalcToPay() {
        toPay = Math.max(0.0, itemsTotal + HANDLING_CHARGE + DELIVERY_CHARGE - appliedDiscount);
        tvToPay.setText(String.format("₹%.0f", toPay));
    }

    private void applyCouponIfAny() {
        if (appliedCouponCode == null || appliedCouponCode.trim().isEmpty()) return;
        if (!sessionManager.isLoggedIn()) return;

        String code = appliedCouponCode.trim();
        repository.validateAndApplyCoupon(code, itemsTotal, new Callback<List<CouponValidationResult>>() {
            @Override
            public void onResponse(Call<List<CouponValidationResult>> call, Response<List<CouponValidationResult>> response) {
                if (!response.isSuccessful() || response.body() == null || response.body().isEmpty()) {
                    appliedCouponCode = "";
                    appliedDiscount = 0.0;
                    recalcToPay();
                    return;
                }

                CouponValidationResult result = response.body().get(0);
                if (!result.isValid) {
                    appliedCouponCode = "";
                    appliedDiscount = 0.0;
                    recalcToPay();
                    return;
                }

                appliedDiscount = Math.max(0.0, result.discountAmount);
                recalcToPay();
            }

            @Override
            public void onFailure(Call<List<CouponValidationResult>> call, Throwable t) {
            }
        });
    }

    private void fetchPincodes() {
        repository.getPincodes(new Callback<List<PincodeMaster>>() {
            @Override
            public void onResponse(Call<List<PincodeMaster>> call, Response<List<PincodeMaster>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    pincodes = response.body();
                }
            }

            @Override
            public void onFailure(Call<List<PincodeMaster>> call, Throwable t) {
            }
        });
    }

    private void placeOrder(String name, String house, String landmark, String pin, double toPay) {
        Toast.makeText(this, "Placing your order... Please wait", Toast.LENGTH_SHORT).show();

        Map<String, Object> orderData = new HashMap<>();
        orderData.put("customer_name", name);
        orderData.put("address", landmark.isEmpty() ? house : (house + ", " + landmark));
        orderData.put("pincode", pin);
        orderData.put("user_mobile", sessionManager.getMobile());
        orderData.put("total_amount", toPay);
        orderData.put("status", "Pending");

        repository.placeOrder(orderData, new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    float cashback = (float) (CartManager.getInstance(CheckoutActivity.this).getTotalPrice() * 0.02);
                    sessionManager.setWalletBalance(sessionManager.getWalletBalance() + cashback);

                    CartManager.getInstance(CheckoutActivity.this).clearCart();

                    com.nmmart.retailos.utils.NotificationHelper.showOrderNotification(CheckoutActivity.this, "Order Placed! 🎉", "Your order has been placed successfully and will be delivered soon.");
                    Intent intent = new Intent(CheckoutActivity.this, OrderSuccessActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    finish();
                } else {
                    Toast.makeText(CheckoutActivity.this, "Order failed! Please try again.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(CheckoutActivity.this, "Network error! Check your connection.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
