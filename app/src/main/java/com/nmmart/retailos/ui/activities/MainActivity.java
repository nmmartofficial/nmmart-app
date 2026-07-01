package com.nmmart.retailos.ui.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.TextWatcher;
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
import com.nmmart.retailos.data.SearchHistoryManager;
import com.nmmart.retailos.data.SelfCheckoutCartManager;
import com.nmmart.retailos.data.SupabaseRepository;
import com.bumptech.glide.Glide;
import com.nmmart.retailos.data.SessionManager;
import com.nmmart.retailos.databinding.ActivityMainBinding;
import com.nmmart.retailos.models.Category;
import com.nmmart.retailos.models.Product;
import com.nmmart.retailos.ui.adapters.CategoryAdapter;
import com.nmmart.retailos.ui.adapters.ProductListAdapter;
import com.nmmart.retailos.ui.adapters.SearchHistoryAdapter;
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
    private ActivityResultLauncher<Intent> locationSelectionLauncher;
    
    private ActivityMainBinding binding;
    private SessionManager sessionManager;
    private MainViewModel viewModel;
    private ThemeManager themeManager;
    
    private CategoryAdapter categoryAdapter;
    private com.nmmart.retailos.ui.adapters.BrandAdapter brandAdapter;
    private ProductListAdapter trendingAdapter;
    private ProductListAdapter productGridAdapter;
    private ProductListAdapter recentlyViewedAdapter;
    private ProductListAdapter everydayEssentialsAdapter;
    private ProductListAdapter discountedProductsAdapter;
    private ProductListAdapter newArrivalsAdapter;
    private ProductListAdapter featuredProductsAdapter;
    private ProductListAdapter flashSaleAdapter;
    private ProductListAdapter aiRecommendationsAdapter;
    private com.nmmart.retailos.ui.adapters.ComboOfferAdapter comboOfferAdapter;
    private SearchHistoryManager searchHistoryManager;
    private SearchHistoryAdapter searchHistoryAdapter;
    private final List<String> searchHistory = new ArrayList<>();
    private com.nmmart.retailos.data.RecentlyViewedManager recentlyViewedManager;

    private com.nmmart.retailos.ui.adapters.BannerAdapter bannerAdapter;
    
    private android.os.Handler countdownHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable countdownRunnable;
    private long flashSaleEndTime = System.currentTimeMillis() + (24 * 60 * 60 * 1000); // 24 hours from now

    private android.os.Handler bannerAutoScrollHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable bannerAutoScrollRunnable;
    private int currentBannerIndex = 0;
    private static final int BANNER_AUTO_SCROLL_DELAY_MS = 4000;

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
            setupSearchSuggestions();

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
            viewModel.loadInitialAllProducts();
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

        locationSelectionLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                updateLocationDisplay();
            }
        );
    }

    private void setupNavigation() {
        binding.navView.setNavigationItemSelectedListener(this);
    }

    private void setupHeader() {
        updateNavHeader();
        
        // Location bar click listener
        binding.locationBar.setOnClickListener(v -> {
            Intent intent = new Intent(this, LocationSelectionActivity.class);
            locationSelectionLauncher.launch(intent);
        });
        
        updateLocationDisplay();
    }
    
    private void updateLocationDisplay() {
        String location = sessionManager.getDeliveryLocation();
        if (binding.tvLocation != null) {
            binding.tvLocation.setText(location);
        }
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
        brandAdapter = new com.nmmart.retailos.ui.adapters.BrandAdapter(this::openBrandProducts);
        bannerAdapter = new com.nmmart.retailos.ui.adapters.BannerAdapter(this, new ArrayList<>());
        comboOfferAdapter = new com.nmmart.retailos.ui.adapters.ComboOfferAdapter(comboOffer -> {
            // Handle combo offer click
            android.widget.Toast.makeText(this, "Combo offer selected: " + comboOffer.getTitle(), android.widget.Toast.LENGTH_SHORT).show();
        });

        trendingAdapter = createProductAdapter();
        productGridAdapter = createProductAdapter();
        recentlyViewedAdapter = createProductAdapter();
        everydayEssentialsAdapter = createProductAdapter();
        discountedProductsAdapter = createProductAdapter();
        newArrivalsAdapter = createProductAdapter();
        featuredProductsAdapter = createProductAdapter();
        flashSaleAdapter = createProductAdapter();
        aiRecommendationsAdapter = createProductAdapter();
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

        binding.rvBrands.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.rvBrands.setAdapter(brandAdapter);

        binding.rvBanners.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.rvBanners.setAdapter(bannerAdapter);

        binding.rvFlashSale.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.rvFlashSale.setAdapter(flashSaleAdapter);

        binding.rvComboOffers.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.rvComboOffers.setAdapter(comboOfferAdapter);

        binding.rvAIRecommendations.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.rvAIRecommendations.setAdapter(aiRecommendationsAdapter);

        setupHorizontalRV(binding.rvBestSelling, trendingAdapter);
        setupHorizontalRV(binding.rvEverydayEssentials, everydayEssentialsAdapter);
        setupHorizontalRV(binding.rvDiscountedProducts, discountedProductsAdapter);
        setupHorizontalRV(binding.rvNewArrivals, newArrivalsAdapter);
        setupHorizontalRV(binding.rvFeaturedProducts, featuredProductsAdapter);
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

        binding.rvProducts.addOnScrollListener(new androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull androidx.recyclerview.widget.RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 0) { // Only scroll down
                    int visibleItemCount = glm.getChildCount();
                    int totalItemCount = glm.getItemCount();
                    int firstVisibleItemPosition = glm.findFirstVisibleItemPosition();

                    if (!viewModel.isAllProductsLastPage() && (visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 5) {
                        viewModel.loadMoreAllProducts();
                    }
                }
            }
        });
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

        // 2. Banners Observer
        viewModel.getBanners().observe(this, banners -> {
            if (banners != null && bannerAdapter != null) {
                bannerAdapter.setBanners(banners);
                currentBannerIndex = 0;
                if (banners.size() > 1) {
                    startBannerAutoScroll();
                } else {
                    stopBannerAutoScroll();
                }
            }
        });

        // 3. Categories Observer
        viewModel.getCategories().observe(this, cats -> {
            if (cats != null) {
                categoryAdapter.setCategories(cats);
            }
        });

        // 4. Brands Observer
        viewModel.getBrands().observe(this, brands -> {
            if (brands != null) {
                brandAdapter.setBrands(brands);
            }
        });

        // 5. Trending Products Observer
        viewModel.getTrendingProducts().observe(this, products -> {
            if (products != null) {
                trendingAdapter.setProducts(products);
                productGridAdapter.setProducts(products);
            }
        });

        // 6. Everyday Essentials Observer
        viewModel.getEverydayEssentials().observe(this, products -> {
            if (products != null) {
                everydayEssentialsAdapter.setProducts(products);
            }
        });

        // 7. Discounted Products Observer
        viewModel.getDiscountedProducts().observe(this, products -> {
            if (products != null) {
                discountedProductsAdapter.setProducts(products);
            }
        });

        // 8. New Arrivals Observer
        viewModel.getNewArrivals().observe(this, products -> {
            if (products != null) {
                newArrivalsAdapter.setProducts(products);
            }
        });

        // 9. Featured Products Observer
        viewModel.getFeaturedProducts().observe(this, products -> {
            if (products != null) {
                featuredProductsAdapter.setProducts(products);
            }
        });
        
        // 10. Flash Sale Products Observer
        viewModel.getFlashSaleProducts().observe(this, products -> {
            if (products != null) {
                flashSaleAdapter.setProducts(products);
            }
        });
        
        // 11. Combo Offers Observer
        viewModel.getComboOffers().observe(this, offers -> {
            if (offers != null) {
                comboOfferAdapter.setComboOffers(offers);
            }
        });
        
        // 12. AI Recommendations Observer
        viewModel.getAIRecommendations().observe(this, products -> {
            if (products != null) {
                aiRecommendationsAdapter.setProducts(products);
            }
        });

        // 13. All Products Observer
        viewModel.getAllProducts().observe(this, products -> {
            if (products != null) {
                productGridAdapter.setProducts(products);
            }
        });

        // 14. All Products Loading More Observer
        viewModel.getIsAllProductsLoadingMore().observe(this, isLoading -> {
            productGridAdapter.setLoading(isLoading != null && isLoading);
        });

        // 13. Loading State Observer
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

        // 14. Error Message Observer
        viewModel.getErrorMessage().observe(this, msg -> {
            if (msg != null) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void startCountdownTimer() {
        countdownRunnable = new Runnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                long diff = flashSaleEndTime - now;
                
                if (diff > 0) {
                    long hours = diff / (60 * 60 * 1000);
                    long minutes = (diff % (60 * 60 * 1000)) / (60 * 1000);
                    long seconds = (diff % (60 * 1000)) / 1000;
                    
                    String timeString = String.format("%02d:%02d:%02d", hours, minutes, seconds);
                    binding.tvFlashSaleCountdown.setText(timeString);
                    
                    countdownHandler.postDelayed(this, 1000);
                } else {
                    binding.tvFlashSaleCountdown.setText("00:00:00");
                }
            }
        };
        countdownHandler.post(countdownRunnable);
    }
    
    private void stopCountdownTimer() {
        if (countdownRunnable != null) {
            countdownHandler.removeCallbacks(countdownRunnable);
        }
    }

    private void startBannerAutoScroll() {
        stopBannerAutoScroll();
        if (bannerAdapter == null || bannerAdapter.getItemCount() <= 1) {
            return;
        }
        bannerAutoScrollRunnable = new Runnable() {
            @Override
            public void run() {
                int itemCount = bannerAdapter.getItemCount();
                if (itemCount <= 1) {
                    return;
                }
                currentBannerIndex = (currentBannerIndex + 1) % itemCount;
                binding.rvBanners.smoothScrollToPosition(currentBannerIndex);
                bannerAutoScrollHandler.postDelayed(this, BANNER_AUTO_SCROLL_DELAY_MS);
            }
        };
        bannerAutoScrollHandler.postDelayed(bannerAutoScrollRunnable, BANNER_AUTO_SCROLL_DELAY_MS);
    }

    private void stopBannerAutoScroll() {
        if (bannerAutoScrollRunnable != null) {
            bannerAutoScrollHandler.removeCallbacks(bannerAutoScrollRunnable);
        }
    }

    private void setupClickListeners() {
        binding.btnMenu.setOnClickListener(v -> binding.drawerLayout.openDrawer(GravityCompat.START));
        binding.btnCart.setOnClickListener(v -> startActivity(new Intent(this, CartActivity.class)));
        binding.btnScanSearch.setOnClickListener(v -> startBarcodeScanner());
        binding.btnVoiceSearch.setOnClickListener(v -> startVoiceSearch());
        
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                showSearchSuggestions(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        binding.etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                Editable text = binding.etSearch.getText();
                if (text != null && !text.toString().trim().isEmpty()) {
                    startSearch(text.toString().trim());
                }
                return true;
            }
            return false;
        });
        
        binding.swipeRefreshLayout.setOnRefreshListener(() -> {
            viewModel.fetchHomeData();
            viewModel.loadInitialAllProducts();
        });
    }

    private void setupSearchSuggestions() {
        searchHistoryManager = SearchHistoryManager.getInstance(this);
        searchHistory.clear();
        searchHistory.addAll(searchHistoryManager.getHistory());

        binding.rvSearchSuggestions.setLayoutManager(new LinearLayoutManager(this));
        searchHistoryAdapter = new SearchHistoryAdapter(searchHistory, query -> {
            binding.etSearch.setText(query);
            binding.etSearch.setSelection(query.length());
            startSearch(query);
        });
        binding.rvSearchSuggestions.setAdapter(searchHistoryAdapter);
    }

    private void showSearchSuggestions(String query) {
        if (query == null) query = "";
        String normalized = query.trim().toLowerCase();
        List<String> suggestions = new ArrayList<>();

        if (normalized.isEmpty()) {
            suggestions.addAll(searchHistory);
        } else {
            for (String history : searchHistory) {
                if (history.toLowerCase().contains(normalized)) {
                    suggestions.add(history);
                }
                if (suggestions.size() >= 10) break;
            }
            if (suggestions.isEmpty()) {
                suggestions.add(query);
            }
        }

        searchHistoryAdapter.updateHistory(suggestions);
        binding.rvSearchSuggestions.setVisibility(suggestions.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void startSearch(String query) {
        if (query == null || query.trim().isEmpty()) return;
        query = query.trim();
        searchHistoryManager.addToHistory(query);
        searchHistory.clear();
        searchHistory.addAll(searchHistoryManager.getHistory());
        showSearchSuggestions(query);

        binding.rvSearchSuggestions.setVisibility(View.GONE);
        binding.etSearch.clearFocus();

        Intent intent = new Intent(this, ProductListActivity.class);
        intent.putExtra("SEARCH_QUERY", query);
        startActivity(intent);
    }

    private void startBarcodeScanner() {
        ScanOptions options = new ScanOptions();
        options.setPrompt(getString(R.string.scan_barcode_prompt));
        options.setBeepEnabled(true);
        options.setBarcodeImageEnabled(false);
        options.setCameraId(0);
        options.setOrientationLocked(true);
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
        Intent intent = new Intent(this, CategoriesActivity.class); 
        intent.putExtra("selected_category_id", category.getId()); 
        startActivity(intent); 
    }

    private void openBrandProducts(com.nmmart.retailos.models.Brand brand) { 
        if (brand == null || brand.getId() == null) return;
        Intent intent = new Intent(this, ProductListActivity.class); 
        intent.putExtra("BRAND_ID", brand.getId()); 
        intent.putExtra("BRAND_NAME", brand.getName()); 
        startActivity(intent); 
    }
    
    private void shareApp(String message) {
        try {
            // First try to share directly via WhatsApp
            Intent whatsappIntent = new Intent(Intent.ACTION_SEND);
            whatsappIntent.setType("text/plain");
            whatsappIntent.setPackage("com.whatsapp");
            whatsappIntent.putExtra(Intent.EXTRA_TEXT, message);
            
            startActivity(whatsappIntent);
        } catch (android.content.ActivityNotFoundException e) {
            // If WhatsApp not installed, show all options
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, message);
            startActivity(Intent.createChooser(shareIntent, "Share via"));
        }
    } 

    public void updateCartBadge() { 
        int count = com.nmmart.retailos.data.CartManager.getInstance(this).getCartCount(); 
        if (count > 0) { 
            binding.tvCartBadge.setText(String.valueOf(count)); 
            binding.tvCartBadge.setVisibility(View.VISIBLE); 
        } else { 
            binding.tvCartBadge.setVisibility(View.GONE); 
        } 
    } 

    @Override 
    public boolean onNavigationItemSelected(@NonNull MenuItem item) { 
        int id = item.getItemId(); 
        if (id == R.id.nav_profile) { 
            startActivity(new Intent(this, ProfileActivity.class)); 
        } else if (id == R.id.nav_notifications) { 
            startActivity(new Intent(this, NotificationsActivity.class)); 
        } else if (id == R.id.nav_orders) { 
            startActivity(new Intent(this, OrderHistoryActivity.class)); 
        } else if (id == R.id.nav_addresses) { 
            startActivity(new Intent(this, AddressListActivity.class)); 
        } else if (id == R.id.nav_wallet) { 
            startActivity(new Intent(this, WalletActivity.class)); 
        } else if (id == R.id.nav_coupons) { 
            startActivity(new Intent(this, CouponsActivity.class)); 
        } else if (id == R.id.nav_refer) { 
            startActivity(new Intent(this, ReferEarnActivity.class)); 
        } else if (id == R.id.nav_help) { 
            startActivity(new Intent(this, CustomerSupportActivity.class)); 
        } else if (id == R.id.nav_about) { 
            startActivity(new Intent(this, AboutUsActivity.class)); 
        } else if (id == R.id.nav_settings) { 
            startActivity(new Intent(this, SettingsActivity.class)); 
        } else if (id == R.id.nav_share) { 
            String shareBody = "Download NM Mart app for best deals on groceries and daily essentials!"; 
            shareApp(shareBody); 
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
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleDeepLink(intent);
    }

    private void handleDeepLink(Intent intent) {
        Uri data = intent.getData();
        if (data == null) return;

        String path = data.getPath();
        if (path == null) return;

        try {
            if (path.startsWith("/product")) {
                String productId = data.getQueryParameter("id");
                if (productId != null && !productId.isEmpty()) {
                    fetchProductByIdAndOpen(productId);
                }
            } else if (path.startsWith("/category")) {
                String categoryId = data.getQueryParameter("id");
                String categoryName = data.getQueryParameter("name");
                if (categoryId != null && !categoryId.isEmpty()) {
                    Intent catIntent = new Intent(this, ProductListActivity.class);
                    catIntent.putExtra("CATEGORY_ID", categoryId);
                    if (categoryName != null) {
                        catIntent.putExtra("CATEGORY_NAME", categoryName);
                    }
                    startActivity(catIntent);
                }
            } else if (path.startsWith("/brand")) {
                String brandId = data.getQueryParameter("id");
                String brandName = data.getQueryParameter("name");
                if (brandId != null && !brandId.isEmpty()) {
                    Intent brandIntent = new Intent(this, ProductListActivity.class);
                    brandIntent.putExtra("BRAND_ID", brandId);
                    if (brandName != null) {
                        brandIntent.putExtra("BRAND_NAME", brandName);
                    }
                    startActivity(brandIntent);
                }
            } else if (path.startsWith("/cart")) {
                startActivity(new Intent(this, CartActivity.class));
            } else if (path.startsWith("/orders")) {
                startActivity(new Intent(this, OrderHistoryActivity.class));
            } else if (path.startsWith("/profile")) {
                startActivity(new Intent(this, ProfileActivity.class));
            }
        } catch (Exception e) {
            logError("Deep link error", e);
        }
    }

    private void fetchProductByIdAndOpen(String productId) {
        SupabaseRepository repository = new SupabaseRepository();
        repository.getProductById(productId, new Callback<List<Product>>() {
            @Override
            public void onResponse(@NonNull Call<List<Product>> call, @NonNull Response<List<Product>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    Product product = response.body().get(0);
                    Intent intent = new Intent(MainActivity.this, ProductDetailActivity.class);
                    intent.putExtra("PRODUCT", product);
                    startActivity(intent);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Product>> call, @NonNull Throwable t) {
                Toast.makeText(MainActivity.this, "Product not found!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateNavHeader();
        updateCartBadge();
        loadRecentlyViewed();
        updateLocationDisplay();
        startCountdownTimer();
        startBannerAutoScroll();
        
        // Handle deep link if activity was launched with one
        handleDeepLink(getIntent());
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        stopCountdownTimer();
        stopBannerAutoScroll();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopCountdownTimer();
    }
}
