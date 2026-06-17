package com.nmmart.retailos.ui.activities;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ItemTouchHelper;
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
        cartAdapter = new CartAdapter();
        binding.rvCartItems.setAdapter(cartAdapter);
        
        binding.rvSavedItems.setLayoutManager(new LinearLayoutManager(this));
        savedAdapter = new SavedAdapter();
        binding.rvSavedItems.setAdapter(savedAdapter);
        
        setupSwipeToDelete();
    }
    
    private void setupSwipeToDelete() {
        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }
            
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Product product = cartAdapter.items.get(position);
                cartManager.removeFromCart(product);
                viewModel.refreshTotal();
                updateUIBasedOnItems();
                Toast.makeText(CartActivity.this, product.name + " removed from cart", Toast.LENGTH_SHORT).show();
            }
            
            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                View itemView = viewHolder.itemView;
                float itemHeight = itemView.getBottom() - itemView.getTop();
                
                Paint paint = new Paint();
                paint.setColor(Color.parseColor("#F44336"));
                
                RectF background = new RectF(itemView.getRight() + dX, itemView.getTop(), itemView.getRight(), itemView.getBottom());
                c.drawRect(background, paint);
                
                Paint textPaint = new Paint();
                textPaint.setColor(Color.WHITE);
                textPaint.setTextSize(48f);
                textPaint.setTextAlign(Paint.Align.CENTER);
                String text = "DELETE";
                float textWidth = textPaint.measureText(text);
                float x = itemView.getRight() - (textWidth / 2) - 48;
                float y = itemView.getTop() + (itemHeight / 2) + (textPaint.descent() + textPaint.ascent()) / 2;
                c.drawText(text, x, y, textPaint);
                
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        };
        
        new ItemTouchHelper(callback).attachToRecyclerView(binding.rvCartItems);
    }

    private void setupObservers() {
        viewModel.getCartItems().observe(this, items -> {
            updateUIBasedOnItems();
            cartAdapter.updateItems(items);
        });
        updateSavedItems();
        viewModel.getTotalPrice().observe(this, itemsTotal -> {
            double total = itemsTotal != null ? itemsTotal : 0.0;
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
            } else {
                binding.rvCartItems.setVisibility(View.GONE);
                binding.bottomLayout.setVisibility(View.GONE);
                binding.emptyCartLayout.setVisibility(View.VISIBLE);
            }
        }
    }

    private void revalidateCoupon(double itemsTotal) {
        repository.validateAndApplyCoupon(appliedCouponCode, itemsTotal, new Callback<List<CouponValidationResult>>() {
            @Override
            public void onResponse(@NonNull Call<List<CouponValidationResult>> call, @NonNull Response<List<CouponValidationResult>> response) {
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
            public void onFailure(@NonNull Call<List<CouponValidationResult>> call, @NonNull Throwable t) {
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
        binding.tvDiscount.setText(String.format("-%s", PriceUtils.formatPrice(appliedDiscount)));

        if (itemsTotal > 0 && itemsTotal < minFreeAmount) {
            binding.cardFreeDelivery.setVisibility(View.VISIBLE);
            double remaining = minFreeAmount - itemsTotal;
            binding.tvFreeDeliveryMsg.setText(getString(R.string.add_more_for_free_delivery, PriceUtils.formatPrice(remaining)));
            binding.cardFreeDelivery.setCardBackgroundColor(getResources().getColor(android.R.color.white));
        } else if (itemsTotal >= minFreeAmount && itemsTotal > 0) {
            binding.cardFreeDelivery.setVisibility(View.VISIBLE);
            binding.tvFreeDeliveryMsg.setText(R.string.congrats_free_delivery);
            binding.cardFreeDelivery.setCardBackgroundColor(android.graphics.Color.parseColor("#E8F5E9"));
        } else {
            binding.cardFreeDelivery.setVisibility(View.GONE);
        }

        double toPay = Math.max(0.0, itemsTotal + handlingCharge + deliveryCharge - appliedDiscount);
        binding.tvToPay.setText(PriceUtils.formatPrice(toPay));
        binding.tvTotalPrice.setText(PriceUtils.formatPrice(toPay));

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
            binding.tvYouSave.setText(getString(R.string.save_amount, PriceUtils.formatPrice(savings)));
        } else {
            binding.tvYouSave.setVisibility(View.GONE);
        }
    }

    private void setupListeners() {
        binding.btnCheckout.setOnClickListener(v -> {
            Double price = viewModel.getTotalPrice().getValue();
            double minOrder = cartManager.getMinOrderCheckout();
            if (price != null && price < minOrder) {
                Toast.makeText(this, getString(R.string.min_order_required, PriceUtils.formatPrice(minOrder)), Toast.LENGTH_SHORT).show();
                return;
            }

            if (sessionManager.isLoggedIn()) {
                Intent intent = new Intent(this, CheckoutActivity.class);
                intent.putExtra("COUPON_CODE", appliedCouponCode);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            } else {
                Toast.makeText(this, R.string.login_required, Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, LoginActivity.class));
            }
        });

        binding.btnApplyCoupon.setOnClickListener(v -> applyCoupon());
        
        binding.etCoupon.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 0) {
                    appliedCouponCode = "";
                    appliedDiscount = 0.0;
                    binding.tvCouponStatus.setVisibility(View.GONE);
                    viewModel.refreshTotal();
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        binding.btnShopNow.setOnClickListener(v -> finish());
    }
    
    private void applyCoupon() {
        if (!sessionManager.isLoggedIn()) {
            Toast.makeText(this, R.string.login_required, Toast.LENGTH_SHORT).show();
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
        binding.tvCouponStatus.setText(R.string.validating_coupon);
        binding.tvCouponStatus.setTextColor(getResources().getColor(android.R.color.darker_gray));
        
        repository.validateAndApplyCoupon(code, cartManager.getTotalPrice(), new Callback<List<CouponValidationResult>>() {
            @Override
            public void onResponse(@NonNull Call<List<CouponValidationResult>> call, @NonNull Response<List<CouponValidationResult>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    CouponValidationResult result = response.body().get(0);
                    if (result.isValid) {
                        appliedCouponCode = code;
                        appliedDiscount = result.discountAmount;
                        binding.tvCouponStatus.setText(getString(R.string.coupon_applied, PriceUtils.formatPrice(appliedDiscount)));
                        binding.tvCouponStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                        viewModel.refreshTotal();
                    } else {
                        binding.tvCouponStatus.setText(result.message);
                        binding.tvCouponStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                        appliedCouponCode = "";
                        appliedDiscount = 0.0;
                    }
                } else {
                    binding.tvCouponStatus.setText(R.string.invalid_coupon);
                    binding.tvCouponStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                }
            }
            @Override
            public void onFailure(@NonNull Call<List<CouponValidationResult>> call, @NonNull Throwable t) {
                binding.tvCouponStatus.setText(R.string.network_error);
                binding.tvCouponStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            }
        });
    }

    private class CartAdapter extends RecyclerView.Adapter<CartAdapter.ViewHolder> {
        private final List<Product> items = new ArrayList<>();

        public void updateItems(List<Product> newItems) {
            DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new ProductDiffCallback(this.items, newItems));
            this.items.clear();
            if (newItems != null) this.items.addAll(newItems);
            diffResult.dispatchUpdatesTo(this);
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
            holder.binding.tvQty.setText(String.valueOf(cartManager.getQuantity(product)));

            Glide.with(holder.itemView.getContext())
                    .load(product.getImageUrl())
                    .placeholder(R.drawable.ic_grocery_bag)
                    .into(holder.binding.ivCartProduct);
            
            holder.binding.tvAction.setText(R.string.save_for_later);
            holder.binding.tvAction.setOnClickListener(v -> {
                cartManager.saveForLater(product);
                viewModel.refreshTotal();
                updateUIBasedOnItems();
                updateSavedItems();
            });
            
            holder.binding.layoutQty.setVisibility(View.VISIBLE);
            holder.binding.btnPlus.setOnClickListener(v -> {
                if (!viewModel.addToCart(product)) Toast.makeText(CartActivity.this, R.string.out_of_stock, Toast.LENGTH_SHORT).show();
            });
            holder.binding.btnMinus.setOnClickListener(v -> viewModel.removeFromCart(product));
        }

        @Override public int getItemCount() { return items.size(); }
        class ViewHolder extends RecyclerView.ViewHolder {
            ItemCartProductBinding binding;
            ViewHolder(ItemCartProductBinding binding) { super(binding.getRoot()); this.binding = binding; }
        }
    }
    
    private class SavedAdapter extends RecyclerView.Adapter<SavedAdapter.ViewHolder> {
        private final List<Product> items = new ArrayList<>();
        
        public void updateItems(List<Product> newItems) {
            DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new ProductDiffCallback(this.items, newItems));
            this.items.clear();
            if (newItems != null) this.items.addAll(newItems);
            diffResult.dispatchUpdatesTo(this);
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
            holder.binding.tvQty.setText(String.valueOf(cartManager.getSavedQuantity(product)));
            
            Glide.with(holder.itemView.getContext())
                    .load(product.getImageUrl())
                    .placeholder(R.drawable.ic_grocery_bag)
                    .into(holder.binding.ivCartProduct);
            
            holder.binding.tvAction.setText(R.string.move_to_cart);
            holder.binding.tvAction.setOnClickListener(v -> {
                cartManager.moveToCart(product);
                viewModel.refreshTotal();
                updateUIBasedOnItems();
                updateSavedItems();
            });
            holder.binding.layoutQty.setVisibility(View.GONE);
        }
        
        @Override public int getItemCount() { return items.size(); }
        class ViewHolder extends RecyclerView.ViewHolder {
            ItemCartProductBinding binding;
            ViewHolder(ItemCartProductBinding binding) { super(binding.getRoot()); this.binding = binding; }
        }
    }

    private static class ProductDiffCallback extends DiffUtil.Callback {
        private final List<Product> oldList;
        private final List<Product> newList;
        public ProductDiffCallback(List<Product> oldList, List<Product> newList) { this.oldList = oldList; this.newList = newList; }
        @Override public int getOldListSize() { return oldList.size(); }
        @Override public int getNewListSize() { return newList.size(); }
        @Override public boolean areItemsTheSame(int oldPos, int newPos) { return oldList.get(oldPos).getId().equals(newList.get(newPos).getId()); }
        @Override public boolean areContentsTheSame(int oldPos, int newPos) {
            Product oldItem = oldList.get(oldPos);
            Product newItem = newList.get(newPos);
            return oldItem.getName().equals(newItem.getName()) && oldItem.getNmPrice() == newItem.getNmPrice();
        }
    }
}
