package com.nmmart.retailos.ui.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.nmmart.retailos.R;
import com.nmmart.retailos.data.CartManager;
import com.nmmart.retailos.data.CouponValidationResult;
import com.nmmart.retailos.data.SupabaseRepository;
import com.nmmart.retailos.models.PincodeMaster;
import com.nmmart.retailos.models.Product;
import com.nmmart.retailos.ui.adapters.DateAdapter;
import com.nmmart.retailos.ui.adapters.TimeSlotAdapter;
import com.nmmart.retailos.ui.adapters.PaymentMethodAdapter;
import com.nmmart.retailos.models.PaymentMethod;
import com.nmmart.retailos.models.WalletTransaction;
import com.nmmart.retailos.utils.WalletTransactionStorage;
import com.google.gson.Gson;
import com.razorpay.Checkout;
import com.razorpay.PaymentResultListener;
import com.nmmart.retailos.BuildConfig;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CheckoutActivity extends BaseActivity implements PaymentResultListener {

    private SupabaseRepository repository;
    private List<PincodeMaster> pincodes;
    private List<com.nmmart.retailos.models.Address> userAddresses;
    private com.nmmart.retailos.models.Address selectedAddress;
    private android.widget.TextView tvToPay, tvSelectedName, tvSelectedDetails, tvCouponApplied;
    private android.view.View layoutSelectedAddress;
    private MaterialButton btnAddAddress, btnPlaceOrder;
    private String appliedCouponCode = "";
    private double appliedDiscount = 0.0;
    private double itemsTotal = 0.0;
    private double toPay = 0.0;
    private CartManager cartManager;
    private Gson gson;
    
    private String selectedDate = "";
    private String selectedTimeSlot = "";
    private String selectedPaymentMethod = "Cash on Delivery";
    private boolean isPlacingOrder = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        repository = new SupabaseRepository();
        cartManager = CartManager.getInstance(this);
        gson = new Gson();

        Checkout.preload(getApplicationContext());

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
        tvCouponApplied = findViewById(R.id.tvCouponApplied);
        btnPlaceOrder = findViewById(R.id.btnPlaceOrder);

        itemsTotal = cartManager.getTotalPrice();
        appliedCouponCode = getIntent().getStringExtra("COUPON_CODE");
        recalcToPay();
        applyCouponIfAny();

        fetchPincodes();
        fetchAddresses();
        setupDateTimePickers();
        setupPaymentMethods();

        btnAddAddress.setOnClickListener(v -> {
            startActivity(new Intent(this, AddressActivity.class));
        });

        findViewById(R.id.btnChangeAddress).setOnClickListener(v -> {
            showAddressSelectionDialog();
        });

        btnPlaceOrder.setOnClickListener(v -> {
            if (isPlacingOrder) {
                Toast.makeText(this, "Please wait, placing your order...", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!isOrderValid()) {
                return;
            }
            
            if (selectedPaymentMethod.startsWith("Wallet")) {
                // Deduct from wallet and save transaction
                WalletTransactionStorage txStorage = WalletTransactionStorage.getInstance(this);
                WalletTransaction tx = new WalletTransaction(
                    String.valueOf(System.currentTimeMillis()),
                    "debit",
                    toPay,
                    "Order Payment",
                    System.currentTimeMillis(),
                    false
                );
                txStorage.saveTransaction(tx);

                double newBalance = sessionManager.getWalletBalance() - toPay;
                sessionManager.setWalletBalance(newBalance);
                Toast.makeText(this, "Wallet deducted: ₹" + String.format("%.2f", toPay), Toast.LENGTH_SHORT).show();
                placeOrder(selectedAddress.fullName, selectedAddress.houseNo, selectedAddress.landmark != null ? selectedAddress.landmark : "", selectedAddress.pincode, toPay, null);
            } else if (selectedPaymentMethod.equals("Cash on Delivery")) {
                placeOrder(selectedAddress.fullName, selectedAddress.houseNo, selectedAddress.landmark != null ? selectedAddress.landmark : "", selectedAddress.pincode, toPay, null);
            } else {
                startRazorpayPayment();
            }
        });
    }

    private boolean isOrderValid() {
        if (selectedAddress == null) {
            Toast.makeText(this, "Please add a delivery address!", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (selectedPaymentMethod == null || selectedPaymentMethod.isEmpty()) {
            Toast.makeText(this, "Please select a payment method!", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (selectedDate == null || selectedDate.isEmpty()) {
            Toast.makeText(this, "Please select a delivery date!", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (selectedTimeSlot == null || selectedTimeSlot.isEmpty()) {
            Toast.makeText(this, "Please select a delivery time slot!", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }
    
    private void setupPaymentMethods() {
        androidx.recyclerview.widget.RecyclerView rvPaymentMethods = findViewById(R.id.rvPaymentMethods);
        rvPaymentMethods.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        
        List<PaymentMethod> paymentMethods = new ArrayList<>();
        paymentMethods.add(new PaymentMethod("Cash on Delivery", R.drawable.ic_cod, false));
        paymentMethods.add(new PaymentMethod("Wallet (₹" + String.format("%.0f", sessionManager.getWalletBalance()) + ")", R.drawable.ic_wallet, sessionManager.getWalletBalance() < toPay));
        paymentMethods.add(new PaymentMethod("UPI", R.drawable.ic_upi, false));
        paymentMethods.add(new PaymentMethod("PhonePe", R.drawable.ic_phonepe, false));
        paymentMethods.add(new PaymentMethod("Google Pay", R.drawable.ic_gpay, false));
        paymentMethods.add(new PaymentMethod("Paytm", R.drawable.ic_paytm, false));
        
        PaymentMethodAdapter adapter = new PaymentMethodAdapter(this, paymentMethods, method -> {
            if (method.isComingSoon) {
                if (method.name.startsWith("Wallet")) {
                    Toast.makeText(this, "Insufficient wallet balance!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, method.name + " integration coming soon!", Toast.LENGTH_SHORT).show();
                }
                return;
            }
            selectedPaymentMethod = method.name;
        });
        rvPaymentMethods.setAdapter(adapter);
    }

    private void fetchAddresses() {
        if (!sessionManager.isLoggedIn()) {
            // For guest checkout, show add address button
            layoutSelectedAddress.setVisibility(android.view.View.GONE);
            btnAddAddress.setVisibility(android.view.View.VISIBLE);
            return;
        }
        
        repository.getUserAddresses(sessionManager.getUserId(), new Callback<List<com.nmmart.retailos.models.Address>>() {
            @Override
            public void onResponse(Call<List<com.nmmart.retailos.models.Address>> call, Response<List<com.nmmart.retailos.models.Address>> response) {
                if (isFinishing()) return;
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    userAddresses = response.body();
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
        androidx.recyclerview.widget.RecyclerView rvDates = findViewById(R.id.rvDates);
        rvDates.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this, androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false));
        List<String> datesList = new ArrayList<>();
        Calendar today = Calendar.getInstance();
        for (int i = 0; i < 7; i++) {
            Calendar cal = (Calendar) today.clone();
            cal.add(Calendar.DAY_OF_MONTH, i);
            String dayName = android.text.format.DateFormat.format("EEE", cal).toString();
            String dateNum = android.text.format.DateFormat.format("dd", cal).toString();
            datesList.add(dateNum + " " + dayName);
        }
        DateAdapter dateAdapter = new DateAdapter(this, datesList, date -> selectedDate = date);
        rvDates.setAdapter(dateAdapter);
        selectedDate = datesList.get(0);
        
        androidx.recyclerview.widget.RecyclerView rvTimeSlots = findViewById(R.id.rvTimeSlots);
        rvTimeSlots.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this, androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false));
        List<String> timeSlots = new ArrayList<>();
        timeSlots.add("9:00 AM - 11:00 AM");
        timeSlots.add("11:00 AM - 1:00 PM");
        timeSlots.add("1:00 PM - 3:00 PM");
        timeSlots.add("3:00 PM - 5:00 PM");
        timeSlots.add("5:00 PM - 7:00 PM");
        timeSlots.add("7:00 PM - 9:00 PM");
        TimeSlotAdapter timeSlotAdapter = new TimeSlotAdapter(this, timeSlots, time -> selectedTimeSlot = time);
        rvTimeSlots.setAdapter(timeSlotAdapter);
        selectedTimeSlot = timeSlots.get(0);
    }

    private void recalcToPay() {
        double deliveryCharge = cartManager.getDeliveryCharge();
        double handlingCharge = cartManager.getHandlingCharge();
        toPay = Math.max(0.0, itemsTotal + handlingCharge + deliveryCharge - appliedDiscount);
        tvToPay.setText(String.format("₹%.2f", toPay));
    }

    private void applyCouponIfAny() {
        if (appliedCouponCode == null || appliedCouponCode.trim().isEmpty()) return;
        if (!sessionManager.isLoggedIn()) return;

        String code = appliedCouponCode.trim();
        repository.validateAndApplyCoupon(code, itemsTotal, new Callback<List<CouponValidationResult>>() {
            @Override
            public void onResponse(Call<List<CouponValidationResult>> call, Response<List<CouponValidationResult>> response) {
                if (isFinishing()) return;
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

                appliedDiscount = result.discountAmount;
                recalcToPay();
                tvCouponApplied.setText("Coupon Applied! -₹" + String.format("%.2f", appliedDiscount));
                tvCouponApplied.setVisibility(android.view.View.VISIBLE);
            }

            @Override
            public void onFailure(Call<List<CouponValidationResult>> call, Throwable t) {}
        });
    }

    private void fetchPincodes() {
        repository.getPincodes(new Callback<List<PincodeMaster>>() {
            @Override
            public void onResponse(Call<List<PincodeMaster>> call, Response<List<PincodeMaster>> response) {
                if (isFinishing()) return;
                if (response.isSuccessful() && response.body() != null) {
                    pincodes = response.body();
                }
            }

            @Override
            public void onFailure(Call<List<PincodeMaster>> call, Throwable t) {}
        });
    }
    
    private void startRazorpayPayment() {
        Checkout checkout = new Checkout();
        checkout.setKeyID(BuildConfig.RAZORPAY_KEY_ID);
        checkout.setImage(R.drawable.ic_launcher_foreground);

        try {
            JSONObject options = new JSONObject();
            options.put("name", "NM Mart");
            options.put("description", "Order Payment");
            options.put("currency", "INR");
            options.put("amount", (int) (toPay * 100)); // Amount in paise
            options.put("prefill.email", sessionManager.getEmail());
            options.put("prefill.contact", sessionManager.getMobile());
            
            JSONObject retryObj = new JSONObject();
            retryObj.put("enabled", true);
            retryObj.put("max_count", 4);
            options.put("retry", retryObj);

            checkout.open(this, options);
        } catch (Exception e) {
            Log.e("CheckoutActivity", "Error in starting Razorpay Checkout", e);
            Toast.makeText(this, "Error in payment: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onPaymentSuccess(String razorpayPaymentID) {
        Toast.makeText(this, "Payment Successful: " + razorpayPaymentID, Toast.LENGTH_SHORT).show();
        placeOrder(selectedAddress.fullName, selectedAddress.houseNo, selectedAddress.landmark != null ? selectedAddress.landmark : "", selectedAddress.pincode, toPay, razorpayPaymentID);
    }

    @Override
    public void onPaymentError(int code, String response) {
        try {
            Toast.makeText(this, "Payment failed: " + code + " " + response, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e("CheckoutActivity", "Exception in onPaymentError", e);
        }
    }

    private void startUpiPayment() {
        String upiId = "nmmart@upi";
        String merchantName = "NM Mart";
        String transactionNote = "Order Payment";
        String amount = String.format("%.2f", toPay);
        
        Uri upiUri = Uri.parse("upi://pay?pa=" + upiId +
            "&pn=" + Uri.encode(merchantName) +
            "&tn=" + Uri.encode(transactionNote) +
            "&am=" + amount +
            "&cu=INR");
            
        Intent upiIntent = new Intent(Intent.ACTION_VIEW, upiUri);
        try {
            startActivity(upiIntent);
        } catch (Exception e) {
            Toast.makeText(this, "No UPI app found on device!", Toast.LENGTH_SHORT).show();
        }
    }

    private void placeOrder(String name, String house, String landmark, String pin, double toPay, String razorpayPaymentId) {
        // Pincode Verification
        boolean isServiceable = true;
        if (pincodes != null && !pincodes.isEmpty()) {
            isServiceable = false;
            for (PincodeMaster p : pincodes) {
                if (p.pincode.equals(pin)) {
                    isServiceable = true;
                    break;
                }
            }
        }
        if (!isServiceable) {
            Toast.makeText(this, "Sorry, we don't deliver to this pincode yet!", Toast.LENGTH_SHORT).show();
            return;
        }

        isPlacingOrder = true;
        btnPlaceOrder.setEnabled(false);
        Toast.makeText(this, "Placing your order... Please wait", Toast.LENGTH_SHORT).show();

        List<Map<String, Object>> orderItems = new ArrayList<>();
        List<Product> cartProducts = cartManager.getCartItems();
        Map<String, Integer> quantities = cartManager.getCartQuantities();
        for (Product product : cartProducts) {
            Map<String, Object> item = new HashMap<>();
            item.put("product_id", product.id);
            item.put("product_name", product.name);
            item.put("quantity", quantities.get(product.id));
            item.put("price", product.nm_price);
            orderItems.add(item);
        }

        Map<String, Object> orderData = new HashMap<>();
        orderData.put("customer_name", name);
        orderData.put("address", landmark.isEmpty() ? house : (house + ", " + landmark));
        orderData.put("pincode", pin);
        orderData.put("delivery_date", selectedDate);
        orderData.put("delivery_time", selectedTimeSlot);
        orderData.put("payment_method", selectedPaymentMethod);
        if (razorpayPaymentId != null) {
            orderData.put("payment_id", razorpayPaymentId);
            orderData.put("status", "Confirmed");
        } else {
            orderData.put("status", "Pending");
        }
        
        String userIdentifier = sessionManager.getMobile();
        if (userIdentifier == null || userIdentifier.isEmpty()) {
            userIdentifier = sessionManager.getEmail();
        }
        orderData.put("user_mobile", userIdentifier);
        orderData.put("total_amount", toPay);
        orderData.put("items", gson.toJson(orderItems));
        orderData.put("user_id", sessionManager.getUserId());

        repository.placeOrder(orderData, new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (isFinishing()) return;
                isPlacingOrder = false;
                btnPlaceOrder.setEnabled(true);
                if (response.isSuccessful()) {
                    double cashback = itemsTotal * (cartManager.getCashbackPercentage() / 100.0);
                    if (cashback > 0) {
                    WalletTransactionStorage txStorage = WalletTransactionStorage.getInstance(CheckoutActivity.this);
                    WalletTransaction cashbackTx = new WalletTransaction(
                            String.valueOf(System.currentTimeMillis()),
                            "credit",
                            cashback,
                            "Cashback on Order",
                            System.currentTimeMillis(),
                            false
                    );
                    txStorage.saveTransaction(cashbackTx);
                }
                    double newWalletBalance = sessionManager.getWalletBalance() + cashback;
                    sessionManager.setWalletBalance(newWalletBalance);
                    
                    // Award loyalty points: 1 point for every ₹10 spent
                    int pointsEarned = (int) (itemsTotal / 10);
                    sessionManager.addLoyaltyPoints(pointsEarned);
                    
                    // Update wallet balance in Supabase
                    repository.updateWalletBalance(sessionManager.getUserId(), newWalletBalance, new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> walletCall, Response<Void> walletResponse) {
                            if (isFinishing()) return;
                            Log.d("CheckoutActivity", "Wallet balance updated in DB");
                        }

                        @Override
                        public void onFailure(Call<Void> walletCall, Throwable t) {
                            if (isFinishing()) return;
                            Log.e("CheckoutActivity", "Failed to update wallet balance in DB", t);
                        }
                    });
                    
                    // Insert wallet transaction in Supabase
                    if (cashback > 0) {
                        Map<String, Object> transactionData = new HashMap<>();
                        transactionData.put("user_id", sessionManager.getUserId());
                        transactionData.put("amount", cashback);
                        transactionData.put("type", "Credit");
                        transactionData.put("description", "Cashback on order");
                        transactionData.put("status", "Completed");
                        
                        repository.insertWalletTransaction(transactionData, new Callback<Void>() {
                            @Override
                            public void onResponse(Call<Void> txnCall, Response<Void> txnResponse) {
                                if (isFinishing()) return;
                                Log.d("CheckoutActivity", "Wallet transaction inserted in DB");
                            }

                            @Override
                            public void onFailure(Call<Void> txnCall, Throwable t) {
                                if (isFinishing()) return;
                                Log.e("CheckoutActivity", "Failed to insert wallet transaction in DB", t);
                            }
                        });
                    }

                    cartManager.clearCart();

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
                if (isFinishing()) return;
                isPlacingOrder = false;
                btnPlaceOrder.setEnabled(true);
                Toast.makeText(CheckoutActivity.this, "Network error! Check your connection and try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
