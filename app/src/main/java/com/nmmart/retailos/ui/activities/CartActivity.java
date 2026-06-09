package com.nmmart.retailos.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.nmmart.retailos.R;
import com.nmmart.retailos.data.CartManager;
import com.nmmart.retailos.data.CouponValidationResult;
import com.nmmart.retailos.data.SupabaseRepository;
import com.nmmart.retailos.databinding.ActivityCartBinding;
import com.nmmart.retailos.databinding.ItemCartProductBinding;
import com.nmmart.retailos.models.Product;
import com.nmmart.retailos.ui.viewmodels.CartViewModel;
import com.nmmart.retailos.utils.PriceUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CartActivity extends BaseActivity {

    private ActivityCartBinding binding;
    private CartViewModel viewModel;
    private CartAdapter cartAdapter;
    private SavedAdapter savedAdapter;
    private String appliedCouponCode = "";
    private double appliedDiscount = 0.0;
    private SupabaseRepository repository;
    private CartManager cartManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCartBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(CartViewModel.class);
        viewModel.init(this);
        repository = new SupabaseRepository();
        cartManager = CartManager.getInstance(this);

        setupToolbar();
        setupRecyclerViews();
        setupObservers();
        setupListeners();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupRecyclerViews() {
        binding.rvCartItems.setLayoutManager(new LinearLayoutManager(this));
        cartAdapter = new CartAdapter(new ArrayList<>(), false);
        binding.rvCartItems.setAdapter(cartAdapter);
        
        binding.rvSavedItems.setLayoutManager(new LinearLayoutManager(this));
        savedAdapter = new SavedAdapter(new ArrayList<>());
        binding.rvSavedItems.setAdapter(savedAdapter);
    }

    private void setupObservers() {
        viewModel.getCartItems().observe(this, items -> {
            updateUIBasedOnItems();
            cartAdapter.updateItems(items);
        });
        updateSavedItems();
        viewModel.getTotalPrice().observe(this, itemsTotal -> {
            double total = itemsTotal != null ? itemsTotal : 0.0;
            
            // Re-validate coupon if applied
            if (!appliedCouponCode.isEmpty() && sessionManager.isLoggedIn()) {
                revalidateCoupon(total);
            } else {
                updateBillUI(total);
            }
        });
    }
    
    private void updateSavedItems() {
        List<Product> savedItems = cartManager.getSavedItems();
        if (savedItems != null && !savedItems.isEmpty()) {
            binding.sectionSaved.setVisibility(View.VISIBLE);
            savedAdapter.updateItems(savedItems);
        } else {
            binding.sectionSaved.setVisibility(View.GONE);
        }
    }
    
    private void updateUIBasedOnItems() {
        List<Product> items = viewModel.getCartItems().getValue();
        boolean hasCartItems = items != null && !items.isEmpty();
        if (!hasCartItems && cartManager.getSavedItems().isEmpty()) {
            binding.rvCartItems.setVisibility(View.GONE);
            binding.bottomLayout.setVisibility(View.GONE);
            binding.emptyCartLayout.setVisibility(View.VISIBLE);
            binding.cardFreeDelivery.setVisibility(View.GONE);
            binding.cardCoupon.setVisibility(View.GONE);
            binding.cardBillDetails.setVisibility(View.GONE);
        } else {
            binding.cardCoupon.setVisibility(View.VISIBLE);
            binding.cardBillDetails.setVisibility(View.VISIBLE);
            if (hasCartItems) {
                binding.rvCartItems.setVisibility(View.VISIBLE);
                binding.bottomLayout.setVisibility(View.VISIBLE);
                binding.emptyCartLayout.setVisibility(View.GONE);
            }
        }
    }

    private void revalidateCoupon(double itemsTotal) {
        repository.validateAndApplyCoupon(appliedCouponCode, itemsTotal, new Callback<List<CouponValidationResult>>() {
            @Override
            public void onResponse(Call<List<CouponValidationResult>> call, Response<List<CouponValidationResult>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    CouponValidationResult result = response.body().get(0);
                    if (!result.isValid) {
                        appliedCouponCode = "";
                        appliedDiscount = 0.0;
                        binding.tvCouponStatus.setVisibility(View.GONE);
                        binding.etCoupon.setText("");
                        Toast.makeText(CartActivity.this, "Coupon no longer valid", Toast.LENGTH_SHORT).show();
                    } else {
                        appliedDiscount = result.discountAmount;
                    }
                } else {
                    appliedCouponCode = "";
                    appliedDiscount = 0.0;
                    binding.tvCouponStatus.setVisibility(View.GONE);
                    binding.etCoupon.setText("");
                }
                updateBillUI(itemsTotal);
            }

            @Override
            public void onFailure(Call<List<CouponValidationResult>> call, Throwable t) {
                updateBillUI(itemsTotal);
            }
        });
    }

    private void updateBillUI(double itemsTotal) {
        double deliveryCharge = cartManager.getDeliveryCharge();
        double handlingCharge = cartManager.getHandlingCharge();
        double minFreeAmount = cartManager.getMinFreeDeliveryAmount();

        binding.tvItemsTotal.setText(PriceUtils.formatPrice(itemsTotal));
        binding.tvHandlingCharge.setText(PriceUtils.formatPrice(handlingCharge));
        binding.tvDeliveryCharge.setText(deliveryCharge > 0 ? PriceUtils.formatPrice(deliveryCharge) : "FREE");
        binding.tvDiscount.setText("-" + PriceUtils.formatPrice(appliedDiscount));

        if (itemsTotal > 0 && itemsTotal < minFreeAmount) {
            binding.cardFreeDelivery.setVisibility(View.VISIBLE);
            double remaining = minFreeAmount - itemsTotal;
            binding.tvFreeDeliveryMsg.setText(String.format("Add %s more for FREE delivery", PriceUtils.formatPrice(remaining)));
            binding.cardFreeDelivery.setCardBackgroundColor(getResources().getColor(android.R.color.white));
        } else if (itemsTotal >= minFreeAmount && itemsTotal > 0) {
            binding.cardFreeDelivery.setVisibility(View.VISIBLE);
            binding.tvFreeDeliveryMsg.setText("Congratulations! You got FREE delivery 🚚");
            binding.cardFreeDelivery.setCardBackgroundColor(android.graphics.Color.parseColor("#E8F5E9"));
        } else {
            binding.cardFreeDelivery.setVisibility(View.GONE);
        }

        double toPay = Math.max(0.0, itemsTotal + handlingCharge + deliveryCharge - appliedDiscount);
        binding.tvToPay.setText(PriceUtils.formatPrice(toPay));
        binding.tvTotalPrice.setText(PriceUtils.formatPrice(toPay));

        // Savings Calculation
        double mrpTotal = 0;
        Map<String, Integer> quantities = cartManager.getCartQuantities();
        for (Product p : cartManager.getCartItems()) {
            if (p != null) {
                int qty = quantities.getOrDefault(p.id, 0);
                mrpTotal += p.getMrp() * qty;
            }
        }
        double savings = Math.max(0, mrpTotal - itemsTotal + appliedDiscount);
        if (savings > 0) {
            binding.tvYouSave.setVisibility(View.VISIBLE);
            binding.tvYouSave.setText(String.format("You Save %s", PriceUtils.formatPrice(savings)));
        } else {
            binding.tvYouSave.setVisibility(View.GONE);
        }
    }

    private void setupListeners() {
        binding.btnCheckout.setOnClickListener(v -> {
            Double price = viewModel.getTotalPrice().getValue();
            double minOrder = cartManager.getMinOrderCheckout();
            if (price != null && price < minOrder) {
                Toast.makeText(this, "Minimum order of " + PriceUtils.formatPrice(minOrder) + " is required!", Toast.LENGTH_SHORT).show();
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

        binding.btnApplyCoupon.setOnClickListener(v -> {
            applyCoupon();
        });
        
        binding.etCoupon.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 0) {
                    appliedCouponCode = "";
                    appliedDiscount = 0.0;
                    binding.tvCouponStatus.setVisibility(View.GONE);
                    viewModel.refreshTotal();
                }
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });

        binding.btnShopNow.setOnClickListener(v -> finish());
    }
    
    private void applyCoupon() {
        if (!sessionManager.isLoggedIn()) {
            Toast.makeText(this, "Please login to apply coupons!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            return;
        }
        
        String code = binding.etCoupon.getText().toString().trim();
        if (code.isEmpty()) {
            appliedCouponCode = "";
            appliedDiscount = 0.0;
            binding.tvCouponStatus.setVisibility(View.GONE);
            viewModel.refreshTotal();
            return;
        }
        
        binding.tvCouponStatus.setVisibility(View.VISIBLE);
        binding.tvCouponStatus.setText("Validating coupon...");
        binding.tvCouponStatus.setTextColor(getResources().getColor(android.R.color.darker_gray));
        
        repository.validateAndApplyCoupon(code, cartManager.getTotalPrice(), new Callback<List<CouponValidationResult>>() {
            @Override
            public void onResponse(Call<List<CouponValidationResult>> call, Response<List<CouponValidationResult>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    CouponValidationResult result = response.body().get(0);
                    if (result.isValid) {
                        appliedCouponCode = code;
                        appliedDiscount = result.discountAmount;
                        binding.tvCouponStatus.setText("Coupon applied! -" + PriceUtils.formatPrice(appliedDiscount));
                        binding.tvCouponStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                        viewModel.refreshTotal();
                    } else {
                        binding.tvCouponStatus.setText(result.message);
                        binding.tvCouponStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                        appliedCouponCode = "";
                        appliedDiscount = 0.0;
                    }
                } else {
                    binding.tvCouponStatus.setText("Invalid coupon code");
                    binding.tvCouponStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                }
            }
            
            @Override
            public void onFailure(Call<List<CouponValidationResult>> call, Throwable t) {
                binding.tvCouponStatus.setText("Failed to validate coupon");
                binding.tvCouponStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            }
        });
    }

    private class CartAdapter extends RecyclerView.Adapter<CartAdapter.ViewHolder> {
        private List<Product> items;
        private boolean isSaved;

        public CartAdapter(List<Product> items, boolean isSaved) {
            this.items = items;
            this.isSaved = isSaved;
        }

        public void updateItems(List<Product> newItems) {
            this.items = newItems;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(ItemCartProductBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Product product = items.get(position);
            holder.binding.tvCartProductName.setText(product.name);
            holder.binding.tvCartProductUnit.setText(product.unit);
            holder.binding.tvCartProductPrice.setText(PriceUtils.formatPrice(product.getNmPrice()));

            int qty = cartManager.getQuantity(product);
            holder.binding.tvQty.setText(String.valueOf(qty));

            Glide.with(holder.itemView.getContext())
                    .load(product.image_url)
                    .placeholder(R.drawable.ic_grocery_bag)
                    .into(holder.binding.ivCartProduct);
            
            holder.binding.tvAction.setText("Save for later");
            holder.binding.tvAction.setOnClickListener(v -> {
                cartManager.saveForLater(product);
                viewModel.refreshTotal();
                updateUIBasedOnItems();
                updateSavedItems();
                cartAdapter.notifyDataSetChanged();
                savedAdapter.notifyDataSetChanged();
            });
            
            holder.binding.layoutQty.setVisibility(View.VISIBLE);

            holder.binding.btnPlus.setOnClickListener(v -> {
                if (!viewModel.addToCart(product)) {
                    Toast.makeText(CartActivity.this, "Out of stock!", Toast.LENGTH_SHORT).show();
                }
            });
            holder.binding.btnMinus.setOnClickListener(v -> viewModel.removeFromCart(product));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ItemCartProductBinding binding;
            ViewHolder(ItemCartProductBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }
    
    private class SavedAdapter extends RecyclerView.Adapter<SavedAdapter.ViewHolder> {
        private List<Product> items;
        
        public SavedAdapter(List<Product> items) {
            this.items = items;
        }
        
        public void updateItems(List<Product> newItems) {
            this.items = newItems;
            notifyDataSetChanged();
        }
        
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(ItemCartProductBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }
        
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Product product = items.get(position);
            holder.binding.tvCartProductName.setText(product.name);
            holder.binding.tvCartProductUnit.setText(product.unit);
            holder.binding.tvCartProductPrice.setText(PriceUtils.formatPrice(product.getNmPrice()));
            
            int qty = cartManager.getSavedQuantity(product);
            holder.binding.tvQty.setText(String.valueOf(qty));
            
            Glide.with(holder.itemView.getContext())
                    .load(product.image_url)
                    .placeholder(R.drawable.ic_grocery_bag)
                    .into(holder.binding.ivCartProduct);
            
            holder.binding.tvAction.setText("Move to cart");
            holder.binding.tvAction.setOnClickListener(v -> {
                cartManager.moveToCart(product);
                viewModel.refreshTotal();
                updateUIBasedOnItems();
                updateSavedItems();
                cartAdapter.notifyDataSetChanged();
                savedAdapter.notifyDataSetChanged();
            });
            
            holder.binding.layoutQty.setVisibility(View.GONE);
        }
        
        @Override
        public int getItemCount() {
            return items.size();
        }
        
        class ViewHolder extends RecyclerView.ViewHolder {
            ItemCartProductBinding binding;
            ViewHolder(ItemCartProductBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }
}
