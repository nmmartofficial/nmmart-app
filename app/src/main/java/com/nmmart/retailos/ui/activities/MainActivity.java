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

            logDebug("onCreate: requestNotificationPermission start");
            requestNotificationPermission();
            logDebug("onCreate: requestNotificationPermission end (" + (System.currentTimeMillis() - startTime) + "ms)");

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
            ImageView ivNavProfilePic = headerView.findViewById(R.id.ivNavProfilePic);
            
            if (sessionManager.isLoggedIn()) {
                navUserName.setText(sessionManager.getUserName());
                String mobile = sessionManager.getMobile();
                navUserMobile.setText(mobile != null ? "+91 " + mobile : sessionManager.getEmail());
                navUserMobile.setVisibility(View.VISIBLE);
                
                String profilePicUri = sessionManager.getProfilePicUri();
                if (profilePicUri != null) {
                    ivNavProfilePic.setPadding(0, 0, 0, 0);
                    Glide.with(this).load(Uri.parse(profilePicUri)).circleCrop().into(ivNavProfilePic);
                }
            } else {
                navUserName.setText(R.string.welcome_to_nm_mart);
                navUserMobile.setText(R.string.login_to_continue);
                navUserMobile.setVisibility(View.GONE);
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
        viewModel.getAppConfig().observe(this, config -> {
            if (config != null) {
                themeManager.setAppConfig(config);
                applyTheme();
                if (categoryAdapter != null) binding.rvCategories.setAdapter(categoryAdapter);
            }
        });

        viewModel.getCategories().observe(this, cats -> { if (cats != null) categoryAdapter.setCategories(cats); });
        
        viewModel.getTrendingProducts().observe(this, products -> {
            if (products != null) {
                trendingAdapter.setProducts(products);
                productGridAdapter.setProducts(products);
            }
        });

        viewModel.getIsLoading().observe(this, isLoading -> {
            if (isLoading != null && isLoading) {
                binding.nestedScrollView.setVisibility(View.GONE);
            } else {
                binding.nestedScrollView.setVisibility(View.VISIBLE);
                binding.swipeRefreshLayout.setRefreshing(false);
            }
        });

        viewModel.getErrorMessage().observe(this, msg -> {
            if (msg != null) Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
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
        viewModel.fetchProductByBarcode(barcode, product -> {
            if (product != null) {
                if (SelfCheckoutCartManager.getInstance(MainActivity.this).addItem(product)) {
                    Toast.makeText(MainActivity.this, getString(R.string.added_to_cart, product.getName()), Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(MainActivity.this, SelfCheckoutCartActivity.class));
                } else {
                    Toast.makeText(MainActivity.this, R.string.out_of_stock, Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(MainActivity.this, R.string.product_not_found, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openProductList(Category category) {
        if (category == null || category.getId() == null) return;
        Intent intent = new Intent(this, CategoriesActivity.class);
        intent.putExtra("selected_category_id", category.getId());
        startActivity(intent);
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_profile && sessionManager.isLoggedIn()) startActivity(new Intent(this, ProfileActivity.class));
        else if (id == R.id.nav_notifications) startActivity(new Intent(this, NotificationsActivity.class));
        else if (id == R.id.nav_orders) startActivity(new Intent(this, OrderHistoryActivity.class));
        else if (id == R.id.nav_addresses && sessionManager.isLoggedIn()) startActivity(new Intent(this, AddressActivity.class));
        else if (id == R.id.nav_wallet && sessionManager.isLoggedIn()) startActivity(new Intent(this, WalletActivity.class));
        else if (id == R.id.nav_coupons) startActivity(new Intent(this, CouponsActivity.class));
        else if (id == R.id.nav_refer) startActivity(new Intent(this, ReferEarnActivity.class));
        else if (id == R.id.nav_help) startActivity(new Intent(this, CustomerSupportActivity.class));
        else if (id == R.id.nav_about) startActivity(new Intent(this, AboutUsActivity.class));
        else if (id == R.id.nav_settings) startActivity(new Intent(this, SettingsActivity.class));
        else if (id == R.id.nav_share) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.sharing_msg));
            shareIntent.setPackage("com.whatsapp");
            try { startActivity(shareIntent); } catch (Exception e) {
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

    private void updateCartBadge() {
        com.nmmart.retailos.data.CartManager cartManager = com.nmmart.retailos.data.CartManager.getInstance(this);
        int cartCount = cartManager.getCartCount();
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
    
    private void updateNotificationBadge() {
        com.nmmart.retailos.utils.NotificationStorage storage = com.nmmart.retailos.utils.NotificationStorage.getInstance(this);
        int unreadCount = 0;
        for (com.nmmart.retailos.models.NotificationItem item : storage.getNotifications()) {
            if (!item.isRead()) {
                unreadCount++;
            }
        }
        // Check if we need to add badge to drawer menu
        com.google.android.material.navigation.NavigationView navView = binding.navView;
        if (navView != null) {
            MenuItem notifItem = navView.getMenu().findItem(R.id.nav_notifications);
            if (notifItem != null) {
                if (unreadCount > 0) {
                    notifItem.setActionView(R.layout.badge_layout);
                    View badgeView = notifItem.getActionView();
                    if (badgeView != null) {
                        TextView badgeText = badgeView.findViewById(android.R.id.text1);
                        if (badgeText != null) badgeText.setText(String.valueOf(unreadCount));
                        badgeView.setOnClickListener(v -> onNavigationItemSelected(notifItem));
                    }
                } else {
                    notifItem.setActionView(null);
                }
            }
        }
    }

    @Override protected void onPause() {
        logDebug("onPause: start");
        super.onPause();
        logDebug("onPause: end");
    }
    @Override protected void onResume() {
        long startTime = System.currentTimeMillis();
        logDebug("onResume: start");
        super.onResume();
        logDebug("onResume: after super.onResume (" + (System.currentTimeMillis() - startTime) + "ms)");
        logDebug("onResume: updateNavHeader start");
        updateNavHeader();
        logDebug("onResume: updateNavHeader end (" + (System.currentTimeMillis() - startTime) + "ms)");
        logDebug("onResume: updateCartBadge start");
        updateCartBadge();
        logDebug("onResume: updateCartBadge end (" + (System.currentTimeMillis() - startTime) + "ms)");
        logDebug("onResume: updateNotificationBadge start");
        updateNotificationBadge();
        logDebug("onResume: updateNotificationBadge end (" + (System.currentTimeMillis() - startTime) + "ms)");
        logDebug("onResume: loadRecentlyViewed start");
        loadRecentlyViewed();
        logDebug("onResume: loadRecentlyViewed end (" + (System.currentTimeMillis() - startTime) + "ms)");
        logDebug("onResume: total (" + (System.currentTimeMillis() - startTime) + "ms)");
    }
    @Override protected void onDestroy() { super.onDestroy(); }
}
