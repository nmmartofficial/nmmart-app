package com.nmmart.retailos.ui.activities;

import android.os.Handler;
import com.nmmart.retailos.R;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import com.bumptech.glide.Glide;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.nmmart.retailos.data.CartManager;
import com.nmmart.retailos.data.SessionManager;
import com.nmmart.retailos.data.SupabaseConfig;
import com.nmmart.retailos.data.SupabaseRepository;
import com.nmmart.retailos.models.Product;
import com.nmmart.retailos.ui.adapters.ProductListAdapter;
import com.nmmart.retailos.utils.PriceUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProductDetailActivity extends BaseActivity {

    private Product product;
    private SupabaseRepository repository;
    private CartManager cartManager;
    private TextView tvQtyValue;
    private ProductListAdapter similarAdapter;
    private UnitOptionAdapter unitAdapter;
    private String selectedUnit;
    private Handler stockUpdateHandler;
    private Runnable stockUpdateRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);

        product = (Product) getIntent().getSerializableExtra("PRODUCT");

        if (product == null) {
            Toast.makeText(this, "Product not found!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        com.nmmart.retailos.data.RecentlyViewedManager.getInstance(this).addProduct(product);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("");
        }

        ViewPager2 vpImages = findViewById(R.id.vpProductImages);
        TabLayout tabDots = findViewById(R.id.tabImageDots);
        TextView tvName = findViewById(R.id.tvDetailName);
        TextView tvPrice = findViewById(R.id.tvDetailPrice);
        TextView tvMrp = findViewById(R.id.tvDetailMrp);
        TextView tvUnit = findViewById(R.id.tvDetailUnit);
        TextView tvDesc = findViewById(R.id.tvDetailDesc);
        TextView tvStockLabel = findViewById(R.id.tvDetailStock);
        TextView tvSavings = findViewById(R.id.tvDetailSavings);
        TextView tvBadge = findViewById(R.id.tvDetailBadge);
        TextView tvDetailBrand = findViewById(R.id.tvDetailBrand);
        MaterialButton btnAdd = findViewById(R.id.btnAddToCartDetail);
        MaterialButton btnBuyNow = findViewById(R.id.btnBuyNow);
        ImageButton btnQtyMinus = findViewById(R.id.btnQtyMinus);
        ImageButton btnQtyPlus = findViewById(R.id.btnQtyPlus);
        tvQtyValue = findViewById(R.id.tvQtyValue);
        RecyclerView rvUnitOptions = findViewById(R.id.rvUnitOptions);
        TextView tvSelectUnitTitle = findViewById(R.id.tvSelectUnitTitle);
        RecyclerView rvSimilar = findViewById(R.id.rvSimilarProducts);

        repository = new SupabaseRepository();
        cartManager = CartManager.getInstance(this);

        // Bind Data
        tvName.setText(product.name != null ? product.name : "Product");
        tvPrice.setText(PriceUtils.formatPrice(product.getNmPrice()));
        
        if (product.getMrp() > product.getNmPrice()) {
            tvMrp.setText(PriceUtils.formatPrice(product.getMrp()));
            tvMrp.setPaintFlags(tvMrp.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            tvMrp.setVisibility(View.VISIBLE);
        } else {
            tvMrp.setVisibility(View.GONE);
        }
        
        // Setup Unit Options and Select first one by default
        List<String> unitOptions = parseUnitOptions(product.unit);
        if (!unitOptions.isEmpty()) {
            selectedUnit = unitOptions.get(0);
            product.unit = selectedUnit; // Object update taki cart mein pehla unit jaye
            tvUnit.setText(selectedUnit);
        } else {
            selectedUnit = product.unit != null ? product.unit : "1 pcs";
            tvUnit.setText(selectedUnit);
        }

        tvDesc.setText(product.getDescription());
        tvDetailBrand.setText("Brand: " + product.getBrand());

        // Savings Logic
        double savings = product.getMrp() - product.getNmPrice();
        if (savings > 0) {
            tvSavings.setText("SAVE " + PriceUtils.formatPrice(savings));
            tvSavings.setVisibility(View.VISIBLE);
        } else {
            tvSavings.setVisibility(View.GONE);
        }

        updateStockUI(product);

        // Badge Logic
        if (product.badge != null && !product.badge.isEmpty()) {
            tvBadge.setVisibility(View.VISIBLE);
            tvBadge.setText(product.badge);
        } else {
            tvBadge.setVisibility(View.GONE);
        }

        List<String> imageUrls = parseImageUrls(product.image_url);
        ImagePagerAdapter pagerAdapter = new ImagePagerAdapter(imageUrls);
        vpImages.setAdapter(pagerAdapter);
        new TabLayoutMediator(tabDots, vpImages, (tab, position) -> tab.setText("•")).attach();

        if (unitOptions.size() > 1) {
            tvSelectUnitTitle.setVisibility(View.VISIBLE);
            rvUnitOptions.setVisibility(View.VISIBLE);
            rvUnitOptions.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            unitAdapter = new UnitOptionAdapter(unitOptions, selected -> {
                selectedUnit = selected;
                product.unit = selected; // Object update taki cart mein sahi unit jaye
                tvUnit.setText(selected);
            });
            rvUnitOptions.setAdapter(unitAdapter);
        } else {
            tvSelectUnitTitle.setVisibility(View.GONE);
            rvUnitOptions.setVisibility(View.GONE);
        }

        rvSimilar.setLayoutManager(new GridLayoutManager(this, 2));
        rvSimilar.setNestedScrollingEnabled(false);
        similarAdapter = new ProductListAdapter(new ArrayList<>(), product1 -> {
            Intent intent = new Intent(this, ProductDetailActivity.class);
            intent.putExtra("PRODUCT", product1);
            startActivity(intent);
        });
        rvSimilar.setAdapter(similarAdapter);
        fetchSimilarProducts();

        btnQtyMinus.setOnClickListener(v -> {
            cartManager.removeFromCart(product);
            updateQtyFromCart();
        });

        btnQtyPlus.setOnClickListener(v -> {
            if (cartManager.addToCart(product)) {
                updateQtyFromCart();
            } else {
                Toast.makeText(this, "Only " + product.getStock() + " units available", Toast.LENGTH_SHORT).show();
            }
        });

        btnAdd.setOnClickListener(v -> {
            if (cartManager.addToCart(product)) {
                updateQtyFromCart();
                Toast.makeText(this, product.name + " added to cart!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Cannot add more. Out of stock!", Toast.LENGTH_SHORT).show();
            }
        });

        btnBuyNow.setOnClickListener(v -> {
            if (cartManager.getQuantity(product.id) == 0) {
                if (!cartManager.addToCart(product)) {
                    Toast.makeText(this, "Out of stock!", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            updateQtyFromCart();
            startActivity(new Intent(this, CartActivity.class));
        });

        updateQtyFromCart();
        setupStockAutoUpdate();
    }

    private void setupStockAutoUpdate() {
        stockUpdateHandler = new Handler(android.os.Looper.getMainLooper());
        stockUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                refreshProductData();
                stockUpdateHandler.postDelayed(this, 30000);
            }
        };
    }

    private void refreshProductData() {
        if (product == null || product.id == null) return;
        SupabaseConfig.getService()
                .searchProducts(SupabaseConfig.getApiKey(), SupabaseConfig.getAuthorizationHeader(), "eq." + product.id, 1, 0)
                .enqueue(new Callback<List<Product>>() {
                    @Override
                    public void onResponse(Call<List<Product>> call, Response<List<Product>> response) {
                        if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                            updateStockUI(response.body().get(0));
                        }
                    }
                    @Override
                    public void onFailure(Call<List<Product>> call, Throwable t) {}
                });
    }

    private void updateStockUI(Product updatedProduct) {
        if (updatedProduct == null) return;
        this.product.stock = updatedProduct.stock;
        TextView tvStock = findViewById(R.id.tvDetailStock);
        MaterialButton btnAdd = findViewById(R.id.btnAddToCartDetail);
        
        if (product.getStock() > 0) {
            tvStock.setText("In Stock (" + product.getStock() + " left)");
            tvStock.setTextColor(Color.parseColor("#388E3C"));
            btnAdd.setEnabled(true);
            btnAdd.setText("ADD TO CART");
        } else {
            tvStock.setText("Out of Stock");
            tvStock.setTextColor(Color.RED);
            btnAdd.setEnabled(false);
            btnAdd.setText("OUT OF STOCK");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateQtyFromCart();
        if (stockUpdateHandler != null && stockUpdateRunnable != null) {
            stockUpdateHandler.removeCallbacks(stockUpdateRunnable);
            stockUpdateHandler.post(stockUpdateRunnable);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (stockUpdateHandler != null && stockUpdateRunnable != null) {
            stockUpdateHandler.removeCallbacks(stockUpdateRunnable);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_product_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
            return true;
        } else if (id == R.id.action_share) {
            shareOnWhatsApp();
            return true;
        } else if (id == R.id.action_edit) {
            Toast.makeText(this, "Edit function coming soon", Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateQtyFromCart() {
        int qty = cartManager.getQuantity(product.id);
        tvQtyValue.setText(String.valueOf(Math.max(0, qty)));
    }

    private void fetchSimilarProducts() {
        String cat = product.category != null ? "eq." + product.category : null;
        String idNot = product.id != null ? "neq." + product.id : null;
        SupabaseConfig.getService()
                .getRelatedProducts(SupabaseConfig.getApiKey(), SupabaseConfig.getAuthorizationHeader(), cat, idNot, 6)
                .enqueue(new Callback<List<Product>>() {
                    @Override
                    public void onResponse(Call<List<Product>> call, Response<List<Product>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            similarAdapter.setProducts(response.body());
                        }
                    }
                    @Override public void onFailure(Call<List<Product>> call, Throwable t) {}
                });
    }

    private static List<String> parseImageUrls(String raw) {
        List<String> out = new ArrayList<>();
        if (raw == null || raw.trim().isEmpty()) return out;
        String[] parts = raw.split(",");
        for (String p : parts) if (p != null && !p.trim().isEmpty()) out.add(p.trim());
        return out;
    }

    private static List<String> parseUnitOptions(String unit) {
        List<String> out = new ArrayList<>();
        if (unit == null || unit.trim().isEmpty()) return out;
        String[] parts = unit.split("[,|/]");
        for (String p : parts) if (p != null && !p.trim().isEmpty()) out.add(p.trim());
        return out;
    }

    private void shareOnWhatsApp() {
        String msg = "*NM Mart - " + product.name + "*\n" +
                "💰 NM Price: " + PriceUtils.formatPrice(product.getNmPrice()) + "\n" +
                "🛒 MRP: " + PriceUtils.formatPrice(product.getMrp()) + "\n" +
                "📦 Unit: " + product.unit + "\n\n" +
                "Download NM Mart App now!";
        Intent si = new Intent(Intent.ACTION_SEND);
        si.putExtra(Intent.EXTRA_TEXT, msg);
        si.setType("text/plain");
        si.setPackage("com.whatsapp");
        try { startActivity(si); } catch (Exception e) {
            si.setPackage(null);
            startActivity(Intent.createChooser(si, "Share Product"));
        }
    }

    private class ImagePagerAdapter extends RecyclerView.Adapter<ImagePagerAdapter.VH> {
        private final List<String> urls;
        ImagePagerAdapter(List<String> urls) { this.urls = urls; }
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int vt) {
            ImageView iv = new ImageView(p.getContext());
            iv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            iv.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            return new VH(iv);
        }
        @Override public void onBindViewHolder(@NonNull VH h, int pos) {
            Glide.with(h.imageView.getContext()).load(urls.isEmpty() ? null : urls.get(pos))
                    .placeholder(R.drawable.ic_grocery_bag).error(R.drawable.ic_grocery_bag).into(h.imageView);
        }
        @Override public int getItemCount() { return Math.max(1, urls.size()); }
        class VH extends RecyclerView.ViewHolder {
            ImageView imageView;
            VH(@NonNull ImageView iv) { super(iv); imageView = iv; }
        }
    }

    private class UnitOptionAdapter extends RecyclerView.Adapter<UnitOptionAdapter.VH> {
        private final List<String> options;
        private final OnSelect listener;
        private int selectedIndex = 0;
        UnitOptionAdapter(List<String> ops, OnSelect l) {
            this.options = ops; this.listener = l;
            for (int i = 0; i < options.size(); i++) if (options.get(i).equalsIgnoreCase(selectedUnit)) selectedIndex = i;
        }
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int vt) {
            return new VH(getLayoutInflater().inflate(R.layout.item_unit_option, p, false));
        }
        @Override public void onBindViewHolder(@NonNull VH h, int pos) {
            h.btn.setText(options.get(pos));
            h.btn.setChecked(pos == selectedIndex);
            h.btn.setOnClickListener(v -> {
                int old = selectedIndex; selectedIndex = h.getAdapterPosition();
                notifyItemChanged(old); notifyItemChanged(selectedIndex);
                if (listener != null && selectedIndex != -1) listener.onSelect(options.get(selectedIndex));
            });
        }
        @Override public int getItemCount() { return options.size(); }
        class VH extends RecyclerView.ViewHolder {
            MaterialButton btn;
            VH(@NonNull View v) { super(v); btn = v.findViewById(R.id.btnUnitOption); }
        }
    }
    interface OnSelect { void onSelect(String s); }
}
