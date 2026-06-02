package com.nmmart.retailos.ui.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.navigation.NavigationView;
import com.nmmart.retailos.R;
import com.nmmart.retailos.data.SessionManager;
import com.nmmart.retailos.data.SupabaseRepository;
import com.nmmart.retailos.databinding.ActivityMainBinding;
import com.nmmart.retailos.models.AppConfig;
import com.nmmart.retailos.models.Banner;
import com.nmmart.retailos.models.Brand;
import com.nmmart.retailos.models.Category;
import com.nmmart.retailos.models.Product;
import com.nmmart.retailos.models.WalletMaster;
import com.nmmart.retailos.ui.adapters.BannerAdapter;
import com.nmmart.retailos.ui.adapters.BrandAdapter;
import com.nmmart.retailos.ui.adapters.CategoryAdapter;
import com.nmmart.retailos.ui.adapters.ProductListAdapter;
import com.nmmart.retailos.ui.adapters.SearchHistoryAdapter;
import com.nmmart.retailos.data.SearchHistoryManager;
import com.nmmart.retailos.ui.viewmodels.MainViewModel;
import com.nmmart.retailos.ui.viewmodels.ProductListViewModel;
import com.nmmart.retailos.utils.PriceUtils;

import java.util.ArrayList;
import java.util.List;

import android.text.Editable;
import android.text.TextWatcher;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends BaseActivity implements NavigationView.OnNavigationItemSelectedListener {

    private ActivityResultLauncher<String> notificationPermissionLauncher;
    private ActivityMainBinding binding;
    private SessionManager sessionManager;
    private SupabaseRepository supabaseRepository;
    private ProductListViewModel productListViewModel;
    private CategoryAdapter categoryAdapter;
    private BrandAdapter brandAdapter;
    private ProductListAdapter productListAdapter;
    private SearchHistoryAdapter searchHistoryAdapter;
    
    private List<Category> categories = new ArrayList<>();
    private List<Brand> brands = new ArrayList<>();
    private List<Product> everydayEssentials = new ArrayList<>();
    private List<Product> bestSelling = new ArrayList<>();
    
    private ProductListAdapter everydayAdapter;
    private ProductListAdapter bestSellingAdapter;
    
    private int currentBannerPosition = 0;
    private Handler bannerHandler;
    private Runnable bannerRunnable;
    private java.util.Timer bannerTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            notificationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (!isGranted) {
                        Toast.makeText(this, "Notifications are disabled. You won't receive order updates!", Toast.LENGTH_LONG).show();
                    }
                }
            );
            
            binding = ActivityMainBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());
            
            sessionManager = new SessionManager(this);
            supabaseRepository = new SupabaseRepository();
            productListViewModel = new ViewModelProvider(this).get(ProductListViewModel.class);
            
            setupNavigation();
        setupHeader();
        setupCategories();
        setupEverydayEssentials();
        setupBestSelling();
        // setupBrands();
        setupProductGrid();
        setupSearchHistory();
        setupBottomNavigation();
        setupObservers();
        setupClickListeners();
            
            loadInitialData();
            updateCartBadge();
            requestNotificationPermission();
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "Error in onCreate", e);
        }
    }
    
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    private void setupNavigation() {
        binding.navView.setNavigationItemSelectedListener(this);
    }

    private void setupHeader() {
        binding.tvLocation.setText(sessionManager.getDeliveryLocation());
        binding.tvUserName.setText("Hello, " + sessionManager.getUserName() + "! 👋");
        binding.tvWalletBalance.setText(PriceUtils.formatPrice(sessionManager.getWalletBalance()));
        
        updateNavHeader();
    }

    private void updateNavHeader() {
        if (binding.navView != null) {
            View headerView = binding.navView.getHeaderView(0);
            if (headerView != null) {
                TextView navUserName = headerView.findViewById(R.id.nav_user_name);
                TextView navUserMobile = headerView.findViewById(R.id.nav_user_mobile);
                
                if (sessionManager.isLoggedIn()) {
                    navUserName.setText(sessionManager.getUserName());
                    String mobile = sessionManager.getMobile();
                    navUserMobile.setText(mobile != null ? "+91 " + mobile : sessionManager.getEmail());
                } else {
                    navUserName.setText("Welcome to NM Mart");
                    navUserMobile.setText("Login to continue");
                }
            }
        }
    }

    private void updateCartBadge() {
        com.nmmart.retailos.data.CartManager cartManager = com.nmmart.retailos.data.CartManager.getInstance(this);
        int cartCount = cartManager.getCartCount();
        if (binding.bottomNavigation != null) {
            MenuItem cartItem = binding.bottomNavigation.getMenu().findItem(R.id.nav_cart);
            if (cartItem != null) {
                if (cartCount > 0) {
                    cartItem.setActionView(R.layout.badge_layout);
                    View badgeView = cartItem.getActionView();
                    if (badgeView != null) {
                        TextView badgeText = badgeView.findViewById(android.R.id.text1);
                        if (badgeText != null) badgeText.setText(String.valueOf(cartCount));
                        badgeView.setOnClickListener(v -> onNavigationItemSelected(cartItem));
                    }
                } else {
                    cartItem.setActionView(null);
                }
            }
        }
    }

    private void setupCategories() {
        categoryAdapter = new CategoryAdapter(categories, this::openProductList);
        binding.rvCategories.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.rvCategories.setAdapter(categoryAdapter);
    }

    private void setupEverydayEssentials() {
        everydayAdapter = new ProductListAdapter(everydayEssentials, product -> {
            Intent intent = new Intent(this, ProductDetailActivity.class);
            intent.putExtra("PRODUCT", product);
            startActivity(intent);
        });
        everydayAdapter.setOnCartUpdateListener(this::updateCartBadge);
        binding.rvEverydayEssentials.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.rvEverydayEssentials.setAdapter(everydayAdapter);
    }

    private void setupBestSelling() {
        bestSellingAdapter = new ProductListAdapter(bestSelling, product -> {
            Intent intent = new Intent(this, ProductDetailActivity.class);
            intent.putExtra("PRODUCT", product);
            startActivity(intent);
        });
        bestSellingAdapter.setOnCartUpdateListener(this::updateCartBadge);
        binding.rvBestSelling.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.rvBestSelling.setAdapter(bestSellingAdapter);
    }

    // private void setupBrands() {
    //     brandAdapter = new BrandAdapter(brand -> {
    //         Intent intent = new Intent(this, ProductListActivity.class);
    //         intent.putExtra("BRAND_NAME", brand.getName());
    //         startActivity(intent);
    //     });
    //     binding.rvBrands.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
    //     binding.rvBrands.setAdapter(brandAdapter);
    // }

    private void setupProductGrid() {
        productListAdapter = new ProductListAdapter(new ArrayList<>(), product -> {
            Intent intent = new Intent(this, ProductDetailActivity.class);
            intent.putExtra("PRODUCT", product);
            startActivity(intent);
        });
        productListAdapter.setOnCartUpdateListener(this::updateCartBadge);
        GridLayoutManager glm = new GridLayoutManager(this, 2);
        glm.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return productListAdapter.getItemViewType(position) == ProductListAdapter.VIEW_TYPE_LOADING ? 2 : 1;
            }
        });
        binding.rvProducts.setLayoutManager(glm);
        binding.rvProducts.setAdapter(productListAdapter);
    }

    private void setupBottomNavigation() {
        binding.bottomNavigation.setOnNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) return true;
            if (id == R.id.nav_categories) {
                Toast.makeText(this, "Categories section", Toast.LENGTH_SHORT).show();
                return true;
            }
            if (id == R.id.nav_cart) {
                startActivity(new Intent(this, CartActivity.class));
                return true;
            }
            if (id == R.id.nav_wishlist) {
                startActivity(new Intent(this, WishlistActivity.class));
                return true;
            }
            return false;
        });
    }

    private void setupSearchHistory() {
        searchHistoryAdapter = new SearchHistoryAdapter(SearchHistoryManager.getInstance(this).getHistory(), query -> {
            SearchHistoryManager.getInstance(this).addToHistory(query);
            Intent intent = new Intent(this, ProductListActivity.class);
            intent.putExtra("SEARCH_QUERY", query);
            startActivity(intent);
        });
        
        binding.rvSearchHistory.setLayoutManager(new LinearLayoutManager(this));
        binding.rvSearchHistory.setAdapter(searchHistoryAdapter);
        
        binding.etSearch.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                updateSearchHistoryUI();
            } else {
                binding.rvSearchHistory.setVisibility(View.GONE);
            }
        });
    }
    
    private void updateSearchHistoryUI() {
        List<String> history = SearchHistoryManager.getInstance(this).getHistory();
        if (history.isEmpty()) {
            binding.rvSearchHistory.setVisibility(View.GONE);
        } else {
            searchHistoryAdapter.updateHistory(history);
            binding.rvSearchHistory.setVisibility(View.VISIBLE);
        }
    }
    
    private void setupClickListeners() {
        binding.btnMenu.setOnClickListener(v -> binding.drawerLayout.openDrawer(GravityCompat.START));
        binding.btnCart.setOnClickListener(v -> startActivity(new Intent(this, CartActivity.class)));
        
        binding.etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                String query = binding.etSearch.getText().toString().trim();
                if (!query.isEmpty()) {
                    SearchHistoryManager.getInstance(this).addToHistory(query);
                    Intent intent = new Intent(this, ProductListActivity.class);
                    intent.putExtra("SEARCH_QUERY", query);
                    startActivity(intent);
                }
                return true;
            }
            return false;
        });
        
        binding.swipeRefreshLayout.setOnRefreshListener(this::refreshData);
    }

    private void setupObservers() {
        productListViewModel.getProducts().observe(this, products -> {
            if (products != null) productListAdapter.setProducts(products);
            binding.swipeRefreshLayout.setRefreshing(false);
        });
        productListViewModel.getIsLoadingMore().observe(this, isLoadingMore -> {
            productListAdapter.setLoading(isLoadingMore != null && isLoadingMore);
        });
    }

    private void loadInitialData() {
        productListViewModel.fetchProducts(null, null, 50, 0);
        if (sessionManager.isLoggedIn()) fetchWallet();
        fetchBannersAndCategories();
    }

    private void refreshData() {
        productListViewModel.fetchProducts(null, null, 50, 0);
        if (sessionManager.isLoggedIn()) fetchWallet();
        fetchBannersAndCategories();
    }

    private void fetchWallet() {
        supabaseRepository.getWallets(new retrofit2.Callback<List<WalletMaster>>() {
            @Override
            public void onResponse(retrofit2.Call<List<WalletMaster>> call, retrofit2.Response<List<WalletMaster>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    WalletMaster wallet = response.body().get(0);
                    sessionManager.setWalletBalance((float)wallet.currentBalance);
                    binding.tvWalletBalance.setText(PriceUtils.formatPrice(wallet.currentBalance));
                }
            }
            @Override
            public void onFailure(retrofit2.Call<List<WalletMaster>> call, Throwable t) {}
        });
    }

    private void fetchBannersAndCategories() {
        binding.shimmerView.setVisibility(View.VISIBLE);
        binding.shimmerView.startShimmer();
        binding.nestedScrollView.setVisibility(View.GONE);
        
        supabaseRepository.getAppConfig(new retrofit2.Callback<List<AppConfig>>() {
            @Override
            public void onResponse(retrofit2.Call<List<AppConfig>> call, retrofit2.Response<List<AppConfig>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    AppConfig config = response.body().get(0);
                    binding.tvDeliveryTime.setText(config.deliveryTimeMsg);
                    double minCheckout = config.minOrderCheckout > 0 ? config.minOrderCheckout : 499.0;
                    double handling = config.handlingCharge > 0 ? config.handlingCharge : 5.0;
                    com.nmmart.retailos.data.CartManager.getInstance(MainActivity.this)
                        .updateAppConfig(config.minOrderFreeDelivery, config.deliveryCharge, minCheckout, handling, config.cashbackPercentage);
                    
                    if (config.storeLogoUrl != null && !config.storeLogoUrl.isEmpty()) {
                        sessionManager.setStoreLogoUrl(config.storeLogoUrl);
                        updateNavHeader();
                    }
                }
            }
            @Override
            public void onFailure(retrofit2.Call<List<AppConfig>> call, Throwable t) {}
        });

        supabaseRepository.getLiveBanners(new retrofit2.Callback<List<Banner>>() {
            @Override
            public void onResponse(retrofit2.Call<List<Banner>> call, retrofit2.Response<List<Banner>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    binding.rvBanners.setLayoutManager(new LinearLayoutManager(MainActivity.this, LinearLayoutManager.HORIZONTAL, false));
                    binding.rvBanners.setAdapter(new BannerAdapter(MainActivity.this, response.body()));
                    startBannerAutoScroll(response.body().size());
                }
            }
            @Override
            public void onFailure(retrofit2.Call<List<Banner>> call, Throwable t) {}
        });
        
        supabaseRepository.getCategories(new retrofit2.Callback<List<Category>>() {
            @Override
            public void onResponse(retrofit2.Call<List<Category>> call, retrofit2.Response<List<Category>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    categories.clear();
                    categories.addAll(response.body());
                    categoryAdapter.notifyDataSetChanged();
                }
            }
            @Override
            public void onFailure(retrofit2.Call<List<Category>> call, Throwable t) {}
        });

        supabaseRepository.fetchLiveProducts("Everyday Essentials", 10, 0, new retrofit2.Callback<List<Product>>() {
            @Override
            public void onResponse(retrofit2.Call<List<Product>> call, retrofit2.Response<List<Product>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    everydayEssentials.clear();
                    everydayEssentials.addAll(response.body());
                    everydayAdapter.notifyDataSetChanged();
                }
            }
            @Override
            public void onFailure(retrofit2.Call<List<Product>> call, Throwable t) {}
        });

        supabaseRepository.getTrendingProducts(10, new retrofit2.Callback<List<Product>>() {
            @Override
            public void onResponse(retrofit2.Call<List<Product>> call, retrofit2.Response<List<Product>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    bestSelling.clear();
                    bestSelling.addAll(response.body());
                    bestSellingAdapter.notifyDataSetChanged();
                }
                binding.shimmerView.stopShimmer();
                binding.shimmerView.setVisibility(View.GONE);
                binding.nestedScrollView.setVisibility(View.VISIBLE);
            }
            @Override
            public void onFailure(retrofit2.Call<List<Product>> call, Throwable t) {
                binding.shimmerView.stopShimmer();
                binding.shimmerView.setVisibility(View.GONE);
                binding.nestedScrollView.setVisibility(View.VISIBLE);
            }
        });
    }

    private void startBannerAutoScroll(int totalItems) {
        if (totalItems <= 0) return;
        stopBannerAutoScroll();
        bannerHandler = new Handler(Looper.getMainLooper());
        bannerRunnable = () -> {
            if (currentBannerPosition == totalItems) currentBannerPosition = 0;
            if (binding.rvBanners != null) binding.rvBanners.smoothScrollToPosition(currentBannerPosition++);
        };
        bannerTimer = new java.util.Timer();
        bannerTimer.schedule(new java.util.TimerTask() {
            @Override public void run() { if (bannerHandler != null) bannerHandler.post(bannerRunnable); }
        }, 3000, 3000);
    }

    private void stopBannerAutoScroll() {
        if (bannerTimer != null) { bannerTimer.cancel(); bannerTimer.purge(); bannerTimer = null; }
        if (bannerHandler != null && bannerRunnable != null) { bannerHandler.removeCallbacks(bannerRunnable); bannerHandler = null; }
    }

    private void openProductList(Category category) {
        if (category == null || category.getName() == null) return;
        Intent intent = new Intent(this, ProductListActivity.class);
        intent.putExtra("CATEGORY_NAME", category.getName());
        startActivity(intent);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_profile && sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, ProfileActivity.class));
        } else if (id == R.id.nav_orders) {
            startActivity(new Intent(this, OrderHistoryActivity.class));
        } else if (id == R.id.nav_addresses && sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, AddressActivity.class));
        } else if (id == R.id.nav_wallet && sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, WalletActivity.class));
        } else if (id == R.id.nav_refer) {
            startActivity(new Intent(this, ReferEarnActivity.class));
        } else if (id == R.id.nav_help) {
            startActivity(new Intent(this, CustomerSupportActivity.class));
        } else if (id == R.id.nav_about) {
            startActivity(new Intent(this, AboutUsActivity.class));
        } else if (id == R.id.nav_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
        } else if (id == R.id.nav_share) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "NM Mart");
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Check out NM Mart - Your local grocery app! Download now!");
            // Try to open WhatsApp first
            shareIntent.setPackage("com.whatsapp");
            try {
                startActivity(shareIntent);
            } catch (Exception e) {
                // If WhatsApp not installed, show chooser
                shareIntent.setPackage(null);
                startActivity(Intent.createChooser(shareIntent, "Share via"));
            }
        } else if (id == R.id.nav_logout && sessionManager.isLoggedIn()) {
            sessionManager.logout();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onPause() { super.onPause(); stopBannerAutoScroll(); }
    @Override
    protected void onResume() {
        super.onResume();
        if (binding.rvBanners.getAdapter() != null) startBannerAutoScroll(binding.rvBanners.getAdapter().getItemCount());
        updateNavHeader();
        updateCartBadge();
    }
    @Override
    protected void onDestroy() { super.onDestroy(); stopBannerAutoScroll(); }
}
