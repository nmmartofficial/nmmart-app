package com.nmmart.retailos.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.view.GravityCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
import com.nmmart.retailos.ui.viewmodels.MainViewModel;
import com.nmmart.retailos.ui.viewmodels.ProductListViewModel;

import java.util.ArrayList;
import java.util.List;

import android.text.Editable;
import android.text.TextWatcher;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends BaseActivity implements NavigationView.OnNavigationItemSelectedListener {

    // Views & Binding
    private ActivityMainBinding binding;
    
    // Managers & Repos
    private SessionManager sessionManager;
    private SupabaseRepository supabaseRepository;
    
    // Search Debouncing
    private Timer searchTimer;
    private final long SEARCH_DELAY = 600; // 600ms delay
    
    // Adapters
    private ProductListViewModel productListViewModel;
    private CategoryAdapter categoryAdapter;
    private BrandAdapter brandAdapter;
    private ProductListAdapter productListAdapter;
    
    // Data
    private List<Category> categories = new ArrayList<>();
    private List<Brand> brands = new ArrayList<>();
    private List<Product> everydayEssentials = new ArrayList<>();
    private List<Product> bestSelling = new ArrayList<>();
    private List<Product> recentlyViewed = new ArrayList<>();
    private ProductListAdapter everydayAdapter;
    private ProductListAdapter bestSellingAdapter;
    private ProductListAdapter recentAdapter;
    
    // Auto-scroll Banner
    private int currentBannerPosition = 0;
    private Handler bannerHandler;
    private Runnable bannerRunnable;
    private java.util.Timer bannerTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            // Initialize UI
            binding = ActivityMainBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());
            
            // Initialize dependencies
            initDependencies();
            
            // Setup all UI components
            setupAllUI();
            
            // Setup data observers
            setupObservers();
            
            // Setup click listeners
            setupClickListeners();
            
            // Load initial data
            loadInitialData();
            
            // Update cart badge
            updateCartBadge();
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "Error in onCreate", e);
            Toast.makeText(this, "Error starting app: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }



    /**
     * Initialize all dependencies like session manager, repo, view models
     */
    private void initDependencies() {
        sessionManager = new SessionManager(this);
        supabaseRepository = new SupabaseRepository();
        productListViewModel = new ViewModelProvider(this).get(ProductListViewModel.class);
    }

    /**
     * Setup all UI components in one place
     */
    private void setupAllUI() {
        setupNavigation();
        setupHeader();
        setupCategories();
        setupRecentlyViewed();
        setupEverydayEssentials();
        setupBestSelling();
        setupBrands();
        setupProductGrid();
        setupBottomNavigation();
    }

    /**
     * Setup navigation drawer
     */
    private void setupNavigation() {
        binding.navView.setNavigationItemSelectedListener(this);
    }

    /**
     * Setup header section with location, user name, wallet balance
     */
    private void setupHeader() {
        try {
            String location = sessionManager.getDeliveryLocation();
            if (location != null) binding.tvLocation.setText(location);
            
            String userName = sessionManager.getUserName();
            if (userName == null || userName.isEmpty()) userName = "Guest";
            binding.tvUserName.setText("Hello, " + userName + "! 👋");
            
            float walletBalance = sessionManager.getWalletBalance();
            binding.tvWalletBalance.setText("₹" + (int)walletBalance);
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "Error in setupHeader", e);
        }
        
        // Voice Search Listener
        try {
            if (binding.textInputLayout != null) {
                binding.textInputLayout.setEndIconOnClickListener(v -> {
                    Intent intent = new Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                    intent.putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                    intent.putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "Say product name...");
                    try {
                        startActivityForResult(intent, 101);
                    } catch (Exception e) {
                        android.widget.Toast.makeText(this, "Voice search not supported", android.widget.Toast.LENGTH_SHORT).show();
                    }
                });
            }
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "Error in voice search setup", e);
        }

        // Update Navigation Drawer Header
        try {
            updateNavHeader();
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "Error in updateNavHeader", e);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 101 && resultCode == RESULT_OK && data != null) {
            java.util.ArrayList<String> result = data.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS);
            if (result != null && !result.isEmpty()) {
                String query = result.get(0);
                Intent intent = new Intent(this, ProductListActivity.class);
                intent.putExtra("SEARCH_QUERY", query);
                startActivity(intent);
            }
        }
    }

    /**
     * Update nav header with real user info
     */
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

    /**
     * Update cart badge on bottom navigation
     */
    private void updateCartBadge() {
        try {
            com.nmmart.retailos.data.CartManager cartManager = com.nmmart.retailos.data.CartManager.getInstance(this);
            int cartCount = cartManager.getCartCount();
            
            if (binding != null && binding.bottomNavigation != null) {
                // Get the cart menu item
                MenuItem cartItem = binding.bottomNavigation.getMenu().findItem(R.id.nav_cart);
                if (cartItem != null) {
                    if (cartCount > 0) {
                        // Show badge with count
                        cartItem.setActionView(R.layout.badge_layout);
                        View badgeView = cartItem.getActionView();
                        if (badgeView != null) {
                            TextView badgeText = badgeView.findViewById(android.R.id.text1);
                            if (badgeText != null) {
                                badgeText.setText(String.valueOf(cartCount));
                            }
                            // Add click listener to badge to open cart
                            badgeView.setOnClickListener(v -> onNavigationItemSelected(cartItem));
                        }
                    } else {
                        // Hide badge
                        cartItem.setActionView(null);
                    }
                }
            }
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "Error in updateCartBadge", e);
        }
    }

    /**
     * Setup Recently Viewed section
     */
    private void setupRecentlyViewed() {
        recentAdapter = new ProductListAdapter(recentlyViewed, product -> {
            Intent intent = new Intent(this, ProductDetailActivity.class);
            intent.putExtra("PRODUCT", product);
            startActivity(intent);
        });
        recentAdapter.setOnCartUpdateListener(this::updateCartBadge);
        
        binding.rvRecentlyViewed.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.rvRecentlyViewed.setAdapter(recentAdapter);
        
        updateRecentlyViewed();
    }

    private void updateRecentlyViewed() {
        List<Product> items = com.nmmart.retailos.data.RecentlyViewedManager.getInstance(this).getRecentProducts();
        if (items != null && !items.isEmpty()) {
            binding.sectionRecentlyViewed.setVisibility(View.VISIBLE);
            recentlyViewed.clear();
            recentlyViewed.addAll(items);
            recentAdapter.notifyDataSetChanged();
        } else {
            binding.sectionRecentlyViewed.setVisibility(View.GONE);
        }
    }

    /**
     * Setup categories RecyclerView
     */
    private void setupCategories() {
        categoryAdapter = new CategoryAdapter(categories, this::openProductList);
        binding.rvCategories.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.rvCategories.setAdapter(categoryAdapter);
    }

    /**
     * Setup Everyday Essentials section
     */
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

    /**
     * Setup Best Selling section
     */
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

    /**
     * Setup brands RecyclerView
     */
    private void setupBrands() {
        brandAdapter = new BrandAdapter(brand -> {
            Intent intent = new Intent(this, ProductListActivity.class);
            intent.putExtra("BRAND_NAME", brand.getName());
            startActivity(intent);
        });
        
        binding.rvBrands.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.rvBrands.setAdapter(brandAdapter);
    }

    /**
     * Setup products grid
     */
    private void setupProductGrid() {
        productListAdapter = new ProductListAdapter(new ArrayList<>(), product -> {
            Intent intent = new Intent(this, ProductDetailActivity.class);
            intent.putExtra("PRODUCT", product);
            startActivity(intent);
        });
        
        // Set cart update listener
        productListAdapter.setOnCartUpdateListener(this::updateCartBadge);
        
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 2);
        gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return productListAdapter.getItemViewType(position) == ProductListAdapter.VIEW_TYPE_LOADING ? 2 : 1;
            }
        });
        
        binding.rvProducts.setLayoutManager(gridLayoutManager);
        binding.rvProducts.setAdapter(productListAdapter);
        
        // Load more products on scroll
        setupProductScrollListener(gridLayoutManager);
    }

    /**
     * Setup scroll listener for infinite scroll
     */
    private void setupProductScrollListener(GridLayoutManager gridLayoutManager) {
        binding.rvProducts.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                
                int visibleItemCount = gridLayoutManager.getChildCount();
                int totalItemCount = gridLayoutManager.getItemCount();
                int firstVisibleItemPosition = gridLayoutManager.findFirstVisibleItemPosition();
                
                Boolean isLoading = productListViewModel.getIsLoading().getValue();
                Boolean isLoadingMore = productListViewModel.getIsLoadingMore().getValue();
                
                if ((isLoading == null || !isLoading) && 
                    (isLoadingMore == null || !isLoadingMore) && 
                    !productListViewModel.isLastPage()) {
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount && firstVisibleItemPosition >= 0) {
                        productListViewModel.loadMoreProducts();
                    }
                }
            }
        });
    }

    /**
     * Setup bottom navigation
     */
    private void setupBottomNavigation() {
        binding.bottomNavigation.setOnNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            
            if (id == R.id.nav_home) {
                return true;
            } else if (id == R.id.nav_categories) {
                // TODO: Open Categories Activity
                return true;
            } else if (id == R.id.nav_cart) {
                startActivity(new Intent(this, CartActivity.class));
                return true;
            } else if (id == R.id.nav_wishlist) {
                startActivity(new Intent(this, WishlistActivity.class));
                return true;
            }
            return false;
        });
    }

    /**
     * Setup all click listeners
     */
    private void setupClickListeners() {
        // Menu button
        binding.btnMenu.setOnClickListener(v -> binding.drawerLayout.openDrawer(GravityCompat.START));
        
        // Cart button
        binding.btnCart.setOnClickListener(v -> {
            startActivity(new Intent(this, CartActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
        
        // Search
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {}
        });

        binding.etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                String query = binding.etSearch.getText().toString().trim();
                if (!query.isEmpty()) {
                    Intent intent = new Intent(this, ProductListActivity.class);
                    intent.putExtra("SEARCH_QUERY", query);
                    startActivity(intent);
                }
                return true;
            }
            return false;
        });
        
        // Swipe to refresh
        binding.swipeRefreshLayout.setOnRefreshListener(this::refreshData);
    }

    /**
     * Setup ViewModel observers
     */
    private void setupObservers() {
        productListViewModel.getProducts().observe(this, products -> {
            if (products != null) {
                productListAdapter.setProducts(products);
            }
            binding.swipeRefreshLayout.setRefreshing(false);
        });
        
        productListViewModel.getIsLoading().observe(this, isLoading -> {
            if (isLoading == null || !isLoading) {
                binding.swipeRefreshLayout.setRefreshing(false);
            }
        });
        
        productListViewModel.getIsLoadingMore().observe(this, isLoadingMore -> {
            productListAdapter.setLoading(isLoadingMore != null && isLoadingMore);
        });

        // MainViewModel error observer
        MainViewModel mainViewModel = new ViewModelProvider(this).get(MainViewModel.class);
        mainViewModel.getErrorMessage().observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Load initial data when app starts
     */
    private void loadInitialData() {
        productListViewModel.fetchProducts(null, null, 50, 0);
        fetchWallet();
        fetchBannersAndCategories();
    }

    /**
     * Refresh all data on swipe
     */
    private void refreshData() {
        if (isNetworkAvailable()) {
            productListViewModel.fetchProducts(null, null, 50, 0);
            fetchWallet();
            fetchBannersAndCategories();
        } else {
            binding.swipeRefreshLayout.setRefreshing(false);
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Check if network is available
     */
    private boolean isNetworkAvailable() {
        android.net.ConnectivityManager cm = 
            (android.net.ConnectivityManager) getSystemService(android.content.Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        android.net.NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    /**
     * Fetch wallet balance
     */
    private void fetchWallet() {
        try {
            supabaseRepository.getWallets(new retrofit2.Callback<List<WalletMaster>>() {
                @Override
                public void onResponse(retrofit2.Call<List<WalletMaster>> call, retrofit2.Response<List<WalletMaster>> response) {
                    if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                        WalletMaster wallet = response.body().get(0);
                        sessionManager.setWalletBalance((float)wallet.currentBalance);
                        binding.tvWalletBalance.setText("₹" + (int)wallet.currentBalance);
                    }
                }

                @Override
                public void onFailure(retrofit2.Call<List<WalletMaster>> call, Throwable t) {
                    android.util.Log.e("MainActivity", "Wallet fetch failed", t);
                    // No need to show toast to user, silently fail
                }
            });
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "Error calling fetchWallet", e);
        }
    }

    /**
     * Fetch banners and categories
     */
    private void fetchBannersAndCategories() {
        try {
            binding.shimmerView.setVisibility(View.VISIBLE);
            binding.shimmerView.startShimmer();
            
            // Fetch App Config for Dynamic Delivery Time and Charges
            supabaseRepository.getAppConfig(new retrofit2.Callback<List<AppConfig>>() {
                @Override
                public void onResponse(retrofit2.Call<List<AppConfig>> call, retrofit2.Response<List<AppConfig>> response) {
                    if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                        AppConfig config = response.body().get(0);
                        binding.tvDeliveryTime.setText(config.deliveryTimeMsg);
                        
                        // Update CartManager delivery config
                        com.nmmart.retailos.data.CartManager.getInstance(MainActivity.this)
                            .updateDeliveryConfig(config.minOrderFreeDelivery, config.deliveryCharge);
                    }
                }
                @Override
                public void onFailure(retrofit2.Call<List<AppConfig>> call, Throwable t) {
                    android.util.Log.e("MainActivity", "AppConfig fetch failed", t);
                }
            });

            supabaseRepository.getLiveBanners(new retrofit2.Callback<List<Banner>>() {
                @Override
                public void onResponse(retrofit2.Call<List<Banner>> call, retrofit2.Response<List<Banner>> response) {
                    binding.swipeRefreshLayout.setRefreshing(false);
                    if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                        binding.rvBanners.setLayoutManager(new LinearLayoutManager(MainActivity.this, LinearLayoutManager.HORIZONTAL, false));
                        BannerAdapter bannerAdapter = new BannerAdapter(MainActivity.this, response.body());
                        binding.rvBanners.setAdapter(bannerAdapter);
                        startBannerAutoScroll(response.body().size());
                    }
                }

                @Override
                public void onFailure(retrofit2.Call<List<Banner>> call, Throwable t) {
                    android.util.Log.e("MainActivity", "Banners fetch failed", t);
                    binding.swipeRefreshLayout.setRefreshing(false);
                    Toast.makeText(MainActivity.this, "Failed to load banners. Check internet.", Toast.LENGTH_SHORT).show();
                }
            });
            
            supabaseRepository.getCategories(new retrofit2.Callback<List<Category>>() {
                @Override
                public void onResponse(retrofit2.Call<List<Category>> call, retrofit2.Response<List<Category>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        categories.clear();
                        categories.addAll(response.body());
                        categoryAdapter.notifyDataSetChanged();
                        
                        // After categories, fetch brands from Supabase if needed, 
                        // or use categories to derive brands
                    }
                }

                @Override
                public void onFailure(retrofit2.Call<List<Category>> call, Throwable t) {
                    android.util.Log.e("MainActivity", "Categories fetch failed", t);
                    Toast.makeText(MainActivity.this, "Failed to load categories. Check internet.", Toast.LENGTH_SHORT).show();
                }
            });

            // Real data for sections from Supabase
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
                public void onFailure(retrofit2.Call<List<Product>> call, Throwable t) {
                    android.util.Log.e("MainActivity", "Everyday Essentials fetch failed", t);
                }
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
                }
                @Override
                public void onFailure(retrofit2.Call<List<Product>> call, Throwable t) {
                    android.util.Log.e("MainActivity", "Trending products fetch failed", t);
                    binding.shimmerView.stopShimmer();
                    binding.shimmerView.setVisibility(View.GONE);
                    Toast.makeText(MainActivity.this, "Failed to load products. Check internet.", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "Error in fetchBannersAndCategories", e);
            binding.shimmerView.stopShimmer();
            binding.shimmerView.setVisibility(View.GONE);
        }
    }

    /**
     * Start auto-scroll for banners
     */
    private void startBannerAutoScroll(int totalItems) {
        if (totalItems <= 0) return;
        
        stopBannerAutoScroll();
        
        bannerHandler = new Handler(Looper.getMainLooper());
        bannerRunnable = () -> {
            if (currentBannerPosition == totalItems) {
                currentBannerPosition = 0;
            }
            if (binding != null && binding.rvBanners != null && binding.rvBanners.getLayoutManager() != null) {
                binding.rvBanners.smoothScrollToPosition(currentBannerPosition++);
            }
        };
        
        bannerTimer = new java.util.Timer();
        bannerTimer.schedule(new java.util.TimerTask() {
            @Override
            public void run() {
                if (bannerHandler != null) {
                    bannerHandler.post(bannerRunnable);
                }
            }
        }, 3000, 3000);
    }

    /**
     * Stop banner auto-scroll
     */
    private void stopBannerAutoScroll() {
        if (bannerTimer != null) {
            bannerTimer.cancel();
            bannerTimer.purge();
            bannerTimer = null;
        }
        if (bannerHandler != null && bannerRunnable != null) {
            bannerHandler.removeCallbacks(bannerRunnable);
            bannerHandler = null;
            bannerRunnable = null;
        }
    }

    /**
     * Show login prompt dialog
     */
    private void showLoginPrompt() {
        new AlertDialog.Builder(this)
                .setTitle("Login Required")
                .setMessage("Please login to use this feature.")
                .setPositiveButton("Login", (d, w) -> startActivity(new Intent(this, LoginActivity.class)))
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Open product list for a category
     */
    private void openProductList(Category category) {
        if (category == null || category.getName() == null) {
            return; // Prevent crash
        }
        Intent intent = new Intent(this, ProductListActivity.class);
        intent.putExtra("CATEGORY_NAME", category.getName());
        startActivity(intent);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.nav_profile) {
            if (sessionManager.isLoggedIn()) startActivity(new Intent(this, ProfileActivity.class));
            else showLoginPrompt();
        } else if (id == R.id.nav_orders) {
            if (sessionManager.isLoggedIn()) startActivity(new Intent(this, OrderHistoryActivity.class));
            else showLoginPrompt();
        } else if (id == R.id.nav_addresses) {
            if (sessionManager.isLoggedIn()) startActivity(new Intent(this, AddressActivity.class));
            else showLoginPrompt();
        } else if (id == R.id.nav_wallet) {
            if (sessionManager.isLoggedIn()) startActivity(new Intent(this, WalletActivity.class));
            else showLoginPrompt();
        } else if (id == R.id.nav_refer) {
            startActivity(new Intent(this, ReferEarnActivity.class));
        } else if (id == R.id.nav_help) {
            startActivity(new Intent(this, CustomerSupportActivity.class));
        } else if (id == R.id.nav_about) {
            startActivity(new Intent(this, AboutUsActivity.class));
        } else if (id == R.id.nav_share) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "NM Mart - Local Grocery App");
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Download NM Mart to get fresh groceries delivered in minutes!");
            startActivity(Intent.createChooser(shareIntent, "Share via"));
        } else if (id == R.id.nav_logout) {
            if (sessionManager.isLoggedIn()) {
                sessionManager.logout();
                Toast.makeText(this, "Logged out!", Toast.LENGTH_SHORT).show();
                recreate();
            } else {
                startActivity(new Intent(this, LoginActivity.class));
            }
        }
        
        binding.drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    // Lifecycle methods to handle banner scroll
    @Override
    protected void onPause() {
        super.onPause();
        stopBannerAutoScroll();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (binding.rvBanners.getAdapter() != null) {
            int itemCount = binding.rvBanners.getAdapter().getItemCount();
            if (itemCount > 0) {
                startBannerAutoScroll(itemCount);
            }
        }
        // Update recently viewed products
        updateRecentlyViewed();
        // Update header in case user logged in/out
        updateNavHeader();
        // Update cart badge
        updateCartBadge();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopBannerAutoScroll();
    }
}
