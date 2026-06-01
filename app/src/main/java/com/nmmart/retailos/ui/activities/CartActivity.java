package com.nmmart.retailos.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.nmmart.retailos.R;
import com.nmmart.retailos.data.CouponValidationResult;
import com.nmmart.retailos.data.SessionManager;
import com.nmmart.retailos.data.SupabaseRepository;
import com.nmmart.retailos.databinding.ActivityCartBinding;
import com.nmmart.retailos.databinding.ItemCartProductBinding;
import com.nmmart.retailos.models.Product;
import com.nmmart.retailos.ui.viewmodels.CartViewModel;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CartActivity extends BaseActivity {

    private ActivityCartBinding binding;
    private CartViewModel viewModel;
    private CartAdapter adapter;
    private String appliedCouponCode = "";
    private double appliedDiscount = 0.0;
    private static final double HANDLING_CHARGE = 5.0;
    private SupabaseRepository repository;
    private com.nmmart.retailos.data.CartManager cartManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCartBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(CartViewModel.class);
        viewModel.init(this);
        repository = new SupabaseRepository();
        cartManager = com.nmmart.retailos.data.CartManager.getInstance(this);

        setupToolbar();
        setupRecyclerView();
        setupObservers();
        setupListeners();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupRecyclerView() {
        binding.rvCartItems.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupObservers() {
        viewModel.getCartItems().observe(this, items -> {
            if (items.isEmpty()) {
                binding.rvCartItems.setVisibility(View.GONE);
                binding.bottomLayout.setVisibility(View.GONE);
                binding.emptyCartLayout.setVisibility(View.VISIBLE);
            } else {
                binding.rvCartItems.setVisibility(View.VISIBLE);
                binding.bottomLayout.setVisibility(View.VISIBLE);
                binding.emptyCartLayout.setVisibility(View.GONE);
                
                adapter = new CartAdapter(items);
                binding.rvCartItems.setAdapter(adapter);
            }
        });

        viewModel.getTotalPrice().observe(this, price -> {
            double itemsTotal = price != null ? price : 0.0;
            double deliveryCharge = cartManager.getDeliveryCharge();
            double minFreeAmount = cartManager.getMinFreeDeliveryAmount();

            binding.tvItemsTotal.setText(String.format("₹%.0f", itemsTotal));
            binding.tvHandlingCharge.setText(String.format("₹%.0f", HANDLING_CHARGE));
            binding.tvDeliveryCharge.setText(deliveryCharge > 0 ? String.format("₹%.0f", deliveryCharge) : "FREE");
            binding.tvDiscount.setText(String.format("-₹%.0f", appliedDiscount));

            // Free Delivery Message logic
            if (itemsTotal > 0 && itemsTotal < minFreeAmount) {
                binding.cardFreeDelivery.setVisibility(View.VISIBLE);
                double remaining = minFreeAmount - itemsTotal;
                binding.tvFreeDeliveryMsg.setText(String.format("Add ₹%.0f more for FREE delivery", remaining));
            } else if (itemsTotal >= minFreeAmount) {
                binding.cardFreeDelivery.setVisibility(View.VISIBLE);
                binding.tvFreeDeliveryMsg.setText("Congratulations! You got FREE delivery 🚚");
                binding.cardFreeDelivery.setCardBackgroundColor(android.graphics.Color.parseColor("#E8F5E9")); // Light green
            } else {
                binding.cardFreeDelivery.setVisibility(View.GONE);
            }

            double toPay = Math.max(0.0, itemsTotal + HANDLING_CHARGE + deliveryCharge - appliedDiscount);
            binding.tvToPay.setText(String.format("₹%.0f", toPay));
            binding.tvTotalPrice.setText(String.format("₹%.0f", toPay));

            // Calculate Savings
            double mrpTotal = 0;
            com.nmmart.retailos.data.CartManager cm = com.nmmart.retailos.data.CartManager.getInstance(this);
            for (String id : cm.getCartItems().keySet()) {
                mrpTotal += cm.getCartItems().get(id).getMrp() * cm.getCartQuantities().get(id);
            }
            double savings = Math.max(0, mrpTotal - itemsTotal + appliedDiscount);
            if (savings > 0) {
                binding.tvYouSave.setVisibility(View.VISIBLE);
                binding.tvYouSave.setText(String.format("You Save ₹%.0f", savings));
            } else {
                binding.tvYouSave.setVisibility(View.GONE);
            }
        });

        binding.btnApplyCoupon.setOnClickListener(v -> {
            String code = binding.etCouponCode.getText().toString().trim();
            if (code.isEmpty()) return;

            repository.getCoupons(new retrofit2.Callback<List<com.nmmart.retailos.models.Coupon>>() {
                @Override
                public void onResponse(retrofit2.Call<List<com.nmmart.retailos.models.Coupon>> call, retrofit2.Response<List<com.nmmart.retailos.models.Coupon>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        for (com.nmmart.retailos.models.Coupon coupon : response.body()) {
                            if (coupon.code.equalsIgnoreCase(code)) {
                                if (cartManager.getTotalPrice() >= coupon.minOrderValue) {
                                    appliedDiscount = coupon.discountAmount;
                                    appliedCouponCode = coupon.code;
                                    Toast.makeText(CartActivity.this, "Coupon Applied!", Toast.LENGTH_SHORT).show();
                                    viewModel.refreshTotal(); // Trigger observer to update UI
                                } else {
                                    Toast.makeText(CartActivity.this, "Min order ₹" + coupon.minOrderValue + " required", Toast.LENGTH_SHORT).show();
                                }
                                return;
                            }
                        }
                        Toast.makeText(CartActivity.this, "Invalid Coupon", Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onFailure(retrofit2.Call<List<com.nmmart.retailos.models.Coupon>> call, Throwable t) {}
            });
        });

        setupRecommended();
    }

    private void setupRecommended() {
        ProductListAdapter recommendedAdapter = new ProductListAdapter(new java.util.ArrayList<>(), product -> {
            Intent intent = new Intent(this, ProductDetailActivity.class);
            intent.putExtra("PRODUCT", product);
            startActivity(intent);
        });
        binding.rvRecommended.setAdapter(recommendedAdapter);
        
        repository.fetchLiveProducts("Everyday Essentials", 10, 0, new retrofit2.Callback<List<Product>>() {
            @Override
            public void onResponse(retrofit2.Call<List<Product>> call, retrofit2.Response<List<Product>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    recommendedAdapter.setProducts(response.body());
                }
            }
            @Override
            public void onFailure(retrofit2.Call<List<Product>> call, Throwable t) {}
        });
    }

    private void setupListeners() {
        binding.btnCheckout.setOnClickListener(v -> {
            Double price = viewModel.getTotalPrice().getValue();
            if (price != null && price < 499) {
                Toast.makeText(this, "Minimum order of ₹499 is required!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (sessionManager.isLoggedIn()) {
                Intent intent = new Intent(this, CheckoutActivity.class);
                intent.putExtra("COUPON_CODE", appliedCouponCode);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            } else {
                Toast.makeText(this, "Please login to place your order!", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, LoginActivity.class));
            }
        });

        binding.cardCoupon.setOnClickListener(v -> showCouponDialog());
        
        // If there's a Shop Now button in empty layout
        View btnShopNow = findViewById(R.id.btnShopNow);
        if (btnShopNow != null) {
            btnShopNow.setOnClickListener(v -> finish());
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private class CartAdapter extends RecyclerView.Adapter<CartAdapter.ViewHolder> {
        private List<Product> items;

        public CartAdapter(List<Product> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemCartProductBinding itemBinding = ItemCartProductBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new ViewHolder(itemBinding);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Product product = items.get(position);
            holder.itemBinding.tvCartProductName.setText(product.name);
            holder.itemBinding.tvCartProductUnit.setText(product.unit);
            
            // Re-calculate line price if needed, or just show unit price
            holder.itemBinding.tvCartProductPrice.setText("₹" + product.nm_price);

            int qty = com.nmmart.retailos.data.CartManager.getInstance(holder.itemView.getContext()).getQuantity(product.id);
            holder.itemBinding.tvQty.setText(String.valueOf(Math.max(0, qty)));
            
            // In a real app, you might want to show (qty * price) here
            // For now, keeping it simple as per original logic
            
            if (product.image_url != null && !product.image_url.isEmpty()) {
                Glide.with(holder.itemView.getContext()).load(product.image_url).into(holder.itemBinding.ivCartProduct);
            }

            holder.itemBinding.btnPlus.setOnClickListener(v -> {
                if (!viewModel.addToCart(product)) {
                    Toast.makeText(CartActivity.this, "Only " + product.getStock() + " in stock", Toast.LENGTH_SHORT).show();
                }
            });
            holder.itemBinding.btnMinus.setOnClickListener(v -> viewModel.removeFromCart(product));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ItemCartProductBinding itemBinding;

            public ViewHolder(@NonNull ItemCartProductBinding itemBinding) {
                super(itemBinding.getRoot());
                this.itemBinding = itemBinding;
            }
        }
    }

    private void showCouponDialog() {
        android.widget.EditText etCoupon = new android.widget.EditText(this);
        etCoupon.setHint("Enter coupon code");
        etCoupon.setText(appliedCouponCode);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Apply Coupon")
                .setView(etCoupon)
                .setPositiveButton("Apply", (dialog, which) -> {
                    String code = etCoupon.getText() != null ? etCoupon.getText().toString().trim() : "";
                    if (code.isEmpty()) {
                        appliedCouponCode = "";
                        appliedDiscount = 0.0;
                        binding.tvCouponValue.setText("Select");
                        viewModel.updateCartData();
                        Toast.makeText(this, "Coupon cleared", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Double itemsTotal = viewModel.getTotalPrice().getValue();
                    double cartTotal = itemsTotal != null ? itemsTotal : 0.0;

                    repository.validateAndApplyCoupon(code, cartTotal, new Callback<List<CouponValidationResult>>() {
                        @Override
                        public void onResponse(Call<List<CouponValidationResult>> call, Response<List<CouponValidationResult>> response) {
                            if (!response.isSuccessful() || response.body() == null || response.body().isEmpty()) {
                                Toast.makeText(CartActivity.this, "Coupon validation failed", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            CouponValidationResult result = response.body().get(0);
                            if (!result.isValid) {
                                appliedCouponCode = "";
                                appliedDiscount = 0.0;
                                binding.tvCouponValue.setText("Select");
                                viewModel.updateCartData();
                                Toast.makeText(CartActivity.this, result.message != null ? result.message : "Invalid Coupon", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            appliedCouponCode = code;
                            appliedDiscount = Math.max(0.0, result.discountAmount);
                            binding.tvCouponValue.setText(code);
                            viewModel.updateCartData();
                            Toast.makeText(CartActivity.this, "Coupon applied", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onFailure(Call<List<CouponValidationResult>> call, Throwable t) {
                            Toast.makeText(CartActivity.this, "Network error. Try again.", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
