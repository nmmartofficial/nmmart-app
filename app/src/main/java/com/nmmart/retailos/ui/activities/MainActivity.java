package com.nmmart.retailos.ui.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.view.GravityCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.messaging.FirebaseMessaging;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import com.nmmart.retailos.R;
import com.nmmart.retailos.data.SelfCheckoutCartManager;
import com.nmmart.retailos.data.SupabaseRepository;
import com.bumptech.glide.Glide;
import com.nmmart.retailos.data.SessionManager;
import com.nmmart.retailos.databinding.ActivityMainBinding;
import com.nmmart.retailos.models.Category;
import com.nmmart.retailos.models.Product;
import com.nmmart.retailos.ui.adapters.CategoryAdapter;
import com.nmmart.retailos.ui.adapters.ProductListAdapter;
import com.nmmart.retailos.ui.viewmodels.MainViewModel;
import com.nmmart.retailos.utils.ThemeManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends BaseActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "MainActivity";

    private ActivityResultLauncher<String> notificationPermissionLauncher;
    private ActivityResultLauncher<ScanOptions> barcodeLauncher;
    private ActivityResultLauncher<Intent> speechRecognizerLauncher;
    
    private ActivityMainBinding binding;
    private SessionManager sessionManager;
    private MainViewModel viewModel;
    private ThemeManager themeManager;
    
    private CategoryAdapter categoryAdapter;
    private ProductListAdapter trendingAdapter;
    private ProductListAdapter productGridAdapter;
    private ProductListAdapter recentlyViewedAdapter;
    private com.nmmart.retailos.data.RecentlyViewedManager recentlyViewedManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        long startTime = System.currentTimeMillis();
        logDebug("onCreate: start");
        try {
            logDebug("onCreate: setupLaunchers start");
            setupLaunchers();
            logDebug("onCreate: setupLaunchers end (" + (System.currentTimeMillis() - startTime) + "ms)");

            logDebug("onCreate: inflate binding start");
            binding = ActivityMainBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());
            logDebug("onCreate: inflate binding end (" + (System.currentTimeMillis() - startTime) + "ms)");

            logDebug("onCreate: init sessionManager/viewModel start");
            sessionManager = new SessionManager(this);
            viewModel = new ViewModelProvider(this).get(MainViewModel.class);
            themeManager = ThemeManager.getInstance(this);
            recentlyViewedManager = com.nmmart.retailos.data.RecentlyViewedManager.getInstance(this);
            logDebug("onCreate: init sessionManager/viewModel end (" + (System.currentTimeMillis() - startTime) + "ms)");

            logDebug("onCreate: setupNavigation start");
            setupNavigation();
            logDebug("onCreate: setupNavigation end (" + (System.currentTimeMillis() - startTime) + "ms)");

            logDebug("onCreate: setupHeader start");
            setupHeader();
            logDebug("onCreate: setupHeader end (" + (System.currentTimeMillis() - startTime) + "ms)");

            logDebug("onCreate: setupAdapters start");
            setupAdapters();
            logDebug("onCreate: setupAdapters end (" + (System.currentTimeMillis() - startTime) + "ms)");

            logDebug("onCreate: setupRecyclerViews start");
            setupRecyclerViews();
            logDebug("onCreate: setupRecyclerViews end (" + (System.currentTimeMillis() - startTime) + "ms)");

        logDebug("onCreate: setupBottomNavigation start");
            setupBottomNavigation();
            logDebug("onCreate: setupBottomNavigation end (" + (System.currentTimeMillis() - startTime) + "ms)");

            logDebug("onCreate: setupObservers start");
            setupObservers();
            logDebug("onCreate: setupObservers end (" + (System.currentTimeMillis() - startTime) + "ms)");

            logDebug("onCreate: setupClickListeners start");
            setupClickListeners();
            logDebug("onCreate: setupClickListeners end (" + (System.currentTimeMillis() - startTime) + "ms)");

            logDebug("onCreate: fetchHomeData start");
            viewModel.fetchHomeData();
            if (sessionManager.isLoggedIn()) {
                viewModel.fetchWallet();
            }
            logDebug("onCreate: fetchHomeData end (" + (System.currentTimeMillis() - startTime) + "ms)");

            logDebug("onCreate: updateCartBadge start");
            updateCartBadge();
            logDebug("onCreate: updateCartBadge end (" + (System.currentTimeMillis() - startTime) + "ms)");

            logDebug("onCreate: getFcmToken start");
            getFcmToken();
            logDebug("onCreate: getFcmToken end (" + (System.currentTimeMillis() - startTime) + "ms)");

            // Apply initial theme
            applyTheme();

            logDebug("onCreate: total time (" + (System.currentTimeMillis() - startTime) + "ms)");
        } catch (Exception e) {
            logError("Error in onCreate", e);
        }
    }
    
    private void getFcmToken() {
        FirebaseMessaging.getInstance().getToken()
            .addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    logError("Fetching FCM token failed", task.getException());
                    return;
                }
                
                String token = task.getResult();
                logDebug("FCM token: " + token);
                
                sessionManager.setFcmToken(token);
                
                if (sessionManager.isLoggedIn() && sessionManager.getUserId() != null && !sessionManager.getUserId().isEmpty()) {
                    SupabaseRepository repository = new SupabaseRepository();
                    repository.updateUserFcmToken(sessionManager.getUserId(), token, null);
                }
            });
    }

    private void setupLaunchers() {
        notificationPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (!isGranted) {
                    Toast.makeText(this, R.string.notification_disabled_msg, Toast.LENGTH_LONG).show();
                }
            }
        );

        barcodeLauncher = registerForActivityResult(new ScanContract(), result -> {
            if (result.getContents() != null) {
                fetchProductByBarcode(result.getContents());
            }
        });

        speechRecognizerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    List<String> matches = result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    if (matches != null && !matches.isEmpty()) {
                        String spokenText = matches.get(0);
                        binding.etSearch.setText(spokenText);
                        // Automatically trigger search
                        Intent intent = new Intent(this, ProductListActivity.class);
                        intent.putExtra("SEARCH_QUERY", spokenText);
                        startActivity(intent);
                    }
                }
            }
        );
    }

    private void setupNavigation() {
        binding.navView.setNavigationItemSelectedListener(this);
    }

    private void setupHeader() {
        updateNavHeader();
    }

    private void updateNavHeader() {
        View headerView = binding.navView.getHeaderView(0);
        if (headerView != null) {
            TextView navUserName = headerView.findViewById(R.id.nav_user_name);
            TextView navUserMobile = headerView.findViewById(R.id.nav_user_mobile);
            View cardNavProfilePic = headerView.findViewById(R.id.cardNavProfilePic);
            ImageView ivNavProfilePic = headerView.findViewById(R.id.ivNavProfilePic);
            
            if (sessionManager.isLoggedIn()) {
                if (navUserName != null) {
                    navUserName.setText(sessionManager.getUserName());
                }
                String mobile = sessionManager.getMobile();
                if (navUserMobile != null) {
                    navUserMobile.setText(mobile != null && !mobile.isEmpty() ? "+91 " + mobile : sessionManager.getEmail());
                    navUserMobile.setVisibility(View.VISIBLE);
                }
                
                if (cardNavProfilePic != null) {
                    cardNavProfilePic.setVisibility(View.VISIBLE);
                }
                
                String profilePicUri = sessionManager.getProfilePicUri();
                if (ivNavProfilePic != null) {
                    if (profilePicUri != null && !profilePicUri.isEmpty()) {
                        ivNavProfilePic.setPadding(0, 0, 0, 0);
                        Glide.with(this)
                             .load(Uri.parse(profilePicUri))
                             .placeholder(android.R.drawable.ic_menu_myplaces)
                             .error(android.R.drawable.ic_menu_myplaces)
                             .circleCrop()
                             .into(ivNavProfilePic);
                    } else {
                        // Reset to default icon when no profile pic
                        ivNavProfilePic.setPadding(12, 12, 12, 12);
                        ivNavProfilePic.setImageResource(android.R.drawable.ic_menu_myplaces);
                    }
                }
            } else {
                if (navUserName != null) {
                    navUserName.setText(R.string.welcome_to_nm_mart);
                }
                if (navUserMobile != null) {
                    navUserMobile.setText(R.string.login_to_continue);
                    navUserMobile.setVisibility(View.VISIBLE);
                }
                
                // Hide profile pic when logged out to avoid showing old data
                if (cardNavProfilePic != null) {
                    cardNavProfilePic.setVisibility(View.GONE);
                }
            }
        }
    }

    private void setupAdapters() {
        categoryAdapter = new CategoryAdapter(new ArrayList<>(), this::openProductList);

        trendingAdapter = createProductAdapter();
        productGridAdapter = createProductAdapter();
        recentlyViewedAdapter = createProductAdapter();
    }

    private ProductListAdapter createProductAdapter() {
        ProductListAdapter adapter = new ProductListAdapter(this);
        adapter.setOnProductClickListener(product -> {
            Intent intent = new Intent(this, ProductDetailActivity.class);
            intent.putExtra("PRODUCT", product);
            startActivity(intent);
        });
        adapter.setOnCartUpdateListener(this::updateCartBadge);
        return adapter;
    }

    private void setupRecyclerViews() {
        binding.rvCategories.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.rvCategories.setAdapter(categoryAdapter);

        setupHorizontalRV(binding.rvBestSelling, trendingAdapter);
        setupHorizontalRV(binding.rvRecentlyViewed, recentlyViewedAdapter);

        GridLayoutManager glm = new GridLayoutManager(this, 2);
        glm.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (productGridAdapter != null && position >= 0 && position < productGridAdapter.getItemCount()) {
                    return productGridAdapter.getItemViewType(position) == ProductListAdapter.VIEW_TYPE_LOADING ? 2 : 1;
                }
                return 1;
            }
        });
        binding.rvProducts.setLayoutManager(glm);
        binding.rvProducts.setAdapter(productGridAdapter);
    }

    private void loadRecentlyViewed() {
        List<Product> recentProducts = recentlyViewedManager.getRecentProducts();
        if (recentProducts.isEmpty()) {
            binding.sectionRecentlyViewed.setVisibility(View.GONE);
        } else {
            binding.sectionRecentlyViewed.setVisibility(View.VISIBLE);
            recentlyViewedAdapter.setProducts(recentProducts);
        }
    }

    private void setupHorizontalRV(androidx.recyclerview.widget.RecyclerView rv, ProductListAdapter adapter) {
        rv.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rv.setAdapter(adapter);
    }

    private void setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) return true;
            if (id == R.id.nav_categories) {
                startActivity(new Intent(this, CategoriesActivity.class));
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

    private void applyTheme() {
        // Apply colors to various UI elements
        binding.bottomNavigation.setBackgroundColor(themeManager.getPrimaryColor());
        binding.fabScan.setBackgroundColor(themeManager.getAccentColor());
        binding.swipeRefreshLayout.setColorSchemeColors(themeManager.getPrimaryColor(), themeManager.getAccentColor());
    }

    private void setupObservers() {
        // 1. App Configuration Observer (Theme & Style)
        viewModel.getAppConfig().observe(this, config -> {
            if (config != null) {
                themeManager.setAppConfig(config);
                applyTheme();
                // Sirf refresh ke liye adapter notify karein
                if (categoryAdapter != null) {
                    categoryAdapter.notifyDataSetChanged();
                }
            }
        });

        // 2. Categories Observer
        viewModel.getCategories().observe(this, cats -> {
            if (cats != null) {
                categoryAdapter.setCategories(cats);
            }
        });
        
        // 3. Trending Products Observer
        viewModel.getTrendingProducts().observe(this, products -> {
            if (products != null) {
                trendingAdapter.setProducts(products);
                productGridAdapter.setProducts(products);
            }
        });

        // 4. Loading State Observer
        viewModel.getIsLoading().observe(this, isLoading -> {
            if (isLoading != null && isLoading) {
                binding.shimmerView.setVisibility(View.VISIBLE);
                binding.shimmerView.startShimmer();
                binding.nestedScrollView.setVisibility(View.GONE);
            } else {
                binding.shimmerView.stopShimmer();
                binding.shimmerView.setVisibility(View.GONE);
                binding.nestedScrollView.setVisibility(View.VISIBLE);
                binding.swipeRefreshLayout.setRefreshing(false);
            }
        });

        // 5. Error Message Observer
        viewModel.getErrorMessage().observe(this, msg -> {
            if (msg != null) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupClickListeners() {
        binding.btnMenu.setOnClickListener(v -> binding.drawerLayout.openDrawer(GravityCompat.START));
        binding.btnCart.setOnClickListener(v -> startActivity(new Intent(this, CartActivity.class)));
        binding.btnScanSearch.setOnClickListener(v -> startBarcodeScanner());
        binding.btnVoiceSearch.setOnClickListener(v -> startVoiceSearch());
        
        binding.etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                Editable text = binding.etSearch.getText();
                if (text != null && !text.toString().trim().isEmpty()) {
                    String query = text.toString().trim();
                    Intent intent = new Intent(this, ProductListActivity.class);
                    intent.putExtra("SEARCH_QUERY", query);
                    startActivity(intent);
                }
                return true;
            }
            return false;
        });
        
        binding.swipeRefreshLayout.setOnRefreshListener(() -> viewModel.fetchHomeData());
        binding.fabScan.setOnClickListener(v -> {
            if (SelfCheckoutCartManager.getInstance(this).getCartCount() > 0) {
                startActivity(new Intent(this, SelfCheckoutCartActivity.class));
            } else {
                startBarcodeScanner();
            }
        });
    }

    private void startBarcodeScanner() {
        ScanOptions options = new ScanOptions();
        options.setPrompt(getString(R.string.scan_barcode_prompt));
        options.setBeepEnabled(true);
        options.setBarcodeImageEnabled(false);
        options.setCameraId(0);
        options.setOrientationLocked(false);
        barcodeLauncher.launch(options);
    }

    private void startVoiceSearch() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN"); // Hindi as default, supports English too
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Kya kharidna chahte hain?");
        
        try {
            speechRecognizerLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Voice search not available on this device", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchProductByBarcode(String barcode) {
        if (barcode == null || barcode.isEmpty()) return;
        
        // Show loading
        binding.swipeRefreshLayout.setRefreshing(true);
        
        SupabaseRepository repository = new SupabaseRepository();
        repository.getProductByBarcode(barcode, new Callback<List<Product>>() {
            @Override
            public void onResponse(@NonNull Call<List<Product>> call, @NonNull Response<List<Product>> response) {
                binding.swipeRefreshLayout.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    // Product found, open detail activity directly
                    Product product = response.body().get(0);
                    Intent intent = new Intent(MainActivity.this, ProductDetailActivity.class);
                    intent.putExtra("PRODUCT", product);
                    startActivity(intent);
                } else {
                    Toast.makeText(MainActivity.this, "Product not found!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Product>> call, @NonNull Throwable t) {
                binding.swipeRefreshLayout.setRefreshing(false);
                Toast.makeText(MainActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openProductList(Category category) {
        if (category == null || category.getId() == null) return;
        Intent intent = new Intent(this, ProductListActivity.class);
        intent.putExtra("CATEGORY_ID", category.getId());
        intent.putExtra("CATEGORY_NAME", category.getName());
        startActivity(intent);
    }

    public void updateCartBadge() {
        int count = com.nmmart.retailos.data.CartManager.getInstance(this).getCartCount();
        if (binding.tvCartBadge != null) {
            if (count > 0) {
                binding.tvCartBadge.setText(String.valueOf(count));
                binding.tvCartBadge.setVisibility(View.VISIBLE);
            } else {
                binding.tvCartBadge.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.nav_orders) {
            startActivity(new Intent(this, OrderHistoryActivity.class));
        } else if (id == R.id.nav_profile) {
            startActivity(new Intent(this, ProfileActivity.class));
        } else if (id == R.id.nav_logout) {
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
    protected void onResume() {
        super.onResume();
        updateNavHeader();
        updateCartBadge();
        loadRecentlyViewed();
    }

}
