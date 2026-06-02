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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProductDetailActivity extends AppCompatActivity {

    private Product product;
    private SessionManager sessionManager;
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
        TextView tvStock = findViewById(R.id.tvDetailStock);
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

        sessionManager = new SessionManager(this);
        repository = new SupabaseRepository();
        cartManager = CartManager.getInstance(this);

        // Bind Data
        tvName.setText(product.name != null ? product.name : "Product");
        tvPrice.setText(com.nmmart.retailos.utils.PriceUtils.formatPrice(product.getNmPrice()));
        
        if (product.getMrp() > product.getNmPrice()) {
            tvMrp.setText(com.nmmart.retailos.utils.PriceUtils.formatPrice(product.getMrp()));
            tvMrp.setPaintFlags(tvMrp.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            tvMrp.setVisibility(View.VISIBLE);
        } else {
            tvMrp.setVisibility(View.GONE);
        }
        
        tvUnit.setText(product.unit != null ? product.unit : "1 pcs");
        tvDesc.setText(product.getDescription());
        tvDetailBrand.setText("Brand: " + product.getBrand());

        selectedUnit = product.unit != null ? product.unit : "";

        // Savings Logic
        double savings = product.getMrp() - product.getNmPrice();
        if (savings > 0) {
            tvSavings.setText("SAVE " + com.nmmart.retailos.utils.PriceUtils.formatPrice(savings));
            tvSavings.setVisibility(View.VISIBLE);
        } else {
            tvSavings.setVisibility(View.GONE);
        }

        // Stock Logic
        if (product.getStock() > 0) {
            tvStock.setText("In Stock (" + product.getStock() + " left)");
            tvStock.setTextColor(Color.parseColor("#388E3C"));
            btnAdd.setEnabled(true);
        } else {
            tvStock.setText("Out of Stock");
            tvStock.setTextColor(Color.RED);
            btnAdd.setEnabled(false);
            btnAdd.setText("OUT OF STOCK");
        }

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

        List<String> unitOptions = parseUnitOptions(product.unit);
        if (unitOptions.size() > 1) {
            tvSelectUnitTitle.setVisibility(View.VISIBLE);
            rvUnitOptions.setVisibility(View.VISIBLE);
            rvUnitOptions.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            unitAdapter = new UnitOptionAdapter(unitOptions, selected -> {
                selectedUnit = selected;
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

        toolbar.setNavigationOnClickListener(v -> onBackPressed());

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
                stockUpdateHandler.postDelayed(this, 30000); // Update every 30 seconds
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
                            Product updatedProduct = response.body().get(0);
                            updateStockUI(updatedProduct);
                        }
                    }
                    @Override
                    public void onFailure(Call<List<Product>> call, Throwable t) {}
                });
    }

    private void updateStockUI(Product updatedProduct) {
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
        if (stockUpdateHandler != null && stockUpdateRunnable != null) {
            stockUpdateHandler.removeCallbacks(stockUpdateRunnable); // Remove any existing callbacks to prevent duplicates
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
        if (id == R.id.action_share) {
            shareOnWhatsApp();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateQtyFromCart() {
        int qty = cartManager.getQuantity(product.id);
        tvQtyValue.setText(String.valueOf(qty <= 0 ? 0 : qty));
    }

    private void fetchSimilarProducts() {
        String category = product.category != null ? "eq." + product.category : null;
        String notEqualId = product.id != null ? "neq." + product.id : null;
        SupabaseConfig.getService()
                .getRelatedProducts(SupabaseConfig.getApiKey(), SupabaseConfig.getAuthorizationHeader(), category, notEqualId, 6)
                .enqueue(new Callback<List<Product>>() {
                    @Override
                    public void onResponse(Call<List<Product>> call, Response<List<Product>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            similarAdapter.setProducts(response.body());
                        }
                    }

                    @Override
                    public void onFailure(Call<List<Product>> call, Throwable t) {
                    }
                });
    }

    private static List<String> parseImageUrls(String raw) {
        List<String> out = new ArrayList<>();
        if (raw == null) return out;
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) return out;
        String[] parts = trimmed.split(",");
        for (String p : parts) {
            String s = p != null ? p.trim() : "";
            if (!s.isEmpty()) out.add(s);
        }
        if (out.isEmpty()) out.add(trimmed);
        return out;
    }

    private static List<String> parseUnitOptions(String unit) {
        List<String> out = new ArrayList<>();
        if (unit == null) return out;
        String raw = unit.trim();
        if (raw.isEmpty()) return out;
        String[] parts = raw.split("[,|/]");
        for (String p : parts) {
            String s = p != null ? p.trim() : "";
            if (!s.isEmpty()) out.add(s);
        }
        if (out.isEmpty()) out.add(raw);
        return out;
    }

    private void shareOnWhatsApp() {
        String message = "*NM Mart - " + product.name + "*\n" +
                "💰 NM Price: ₹" + product.nm_price + "\n" +
                "🛒 MRP: ₹" + product.mrp + "\n" +
                "📦 Unit: " + product.unit + "\n\n" +
                "Check it out on NM Mart App! Freshness guaranteed.";
        
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, message);
        sendIntent.setType("text/plain");
        sendIntent.setPackage("com.whatsapp");
        
        try {
            startActivity(sendIntent);
        } catch (Exception e) {
            // If WhatsApp is not installed, use general share
            sendIntent.setPackage(null);
            startActivity(Intent.createChooser(sendIntent, "Share Product"));
        }
    }

    private class ImagePagerAdapter extends RecyclerView.Adapter<ImagePagerAdapter.VH> {
        private final List<String> urls;

        ImagePagerAdapter(List<String> urls) {
            this.urls = urls != null ? urls : new ArrayList<>();
        }

        @androidx.annotation.NonNull
        @Override
        public VH onCreateViewHolder(@androidx.annotation.NonNull ViewGroup parent, int viewType) {
            ImageView iv = new ImageView(parent.getContext());
            iv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            iv.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            iv.setBackgroundColor(android.graphics.Color.WHITE);
            return new VH(iv);
        }

        @Override
        public void onBindViewHolder(@androidx.annotation.NonNull VH holder, int position) {
            String url = urls.isEmpty() ? null : urls.get(position);
            Glide.with(holder.imageView.getContext())
                    .load(url)
                    .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.ic_grocery_bag)
                    .error(R.drawable.ic_grocery_bag)
                    .dontAnimate()
                    .into(holder.imageView);
        }

        @Override
        public int getItemCount() {
            return Math.max(1, urls.size());
        }

        class VH extends RecyclerView.ViewHolder {
            ImageView imageView;

            VH(@androidx.annotation.NonNull ImageView itemView) {
                super(itemView);
                imageView = itemView;
            }
        }
    }

    interface OnSelect {
        void onSelect(String selected);
    }

    private class UnitOptionAdapter extends RecyclerView.Adapter<UnitOptionAdapter.VH> {
        private final List<String> options;
        private final OnSelect listener;
        private int selectedIndex = 0;

        UnitOptionAdapter(List<String> options, OnSelect listener) {
            this.options = options != null ? options : new ArrayList<>();
            this.listener = listener;
            for (int i = 0; i < this.options.size(); i++) {
                if (this.options.get(i).equalsIgnoreCase(selectedUnit)) {
                    selectedIndex = i;
                    break;
                }
            }
        }

        @androidx.annotation.NonNull
        @Override
        public VH onCreateViewHolder(@androidx.annotation.NonNull ViewGroup parent, int viewType) {
            View v = getLayoutInflater().inflate(R.layout.item_unit_option, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@androidx.annotation.NonNull VH holder, int position) {
            String option = options.get(position);
            holder.btn.setText(option);
            boolean selected = position == selectedIndex;
            holder.btn.setChecked(selected);
            holder.btn.setOnClickListener(v -> {
                int old = selectedIndex;
                selectedIndex = holder.getAdapterPosition();
                notifyItemChanged(old);
                notifyItemChanged(selectedIndex);
                if (listener != null && selectedIndex != RecyclerView.NO_POSITION) {
                    listener.onSelect(options.get(selectedIndex));
                }
            });
        }

        @Override
        public int getItemCount() {
            return options.size();
        }

        class VH extends RecyclerView.ViewHolder {
            com.google.android.material.button.MaterialButton btn;

            VH(@androidx.annotation.NonNull View itemView) {
                super(itemView);
                btn = itemView.findViewById(R.id.btnUnitOption);
            }
        }
    }
}
