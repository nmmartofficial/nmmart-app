package com.nmmart.retailos.ui.viewmodels;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.nmmart.retailos.data.SupabaseRepository;
import com.nmmart.retailos.models.AppConfig;
import com.nmmart.retailos.models.Banner;
import com.nmmart.retailos.models.Brand;
import com.nmmart.retailos.models.Category;
import com.nmmart.retailos.models.Product;
import com.nmmart.retailos.models.WalletMaster;
import com.nmmart.retailos.utils.NetworkUtils;
import com.nmmart.retailos.utils.OfflineStorage;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainViewModel extends AndroidViewModel {
    private static final String TAG = "MainViewModel";
    private final SupabaseRepository repository;
    private final List<Call<?>> activeCalls = new ArrayList<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private static final long STAGGER_DELAY_MS = 300;
    private final com.nmmart.retailos.utils.ThemeManager themeManager;

    private final MutableLiveData<List<Banner>> banners = new MutableLiveData<>();
    private final MutableLiveData<List<Category>> categories = new MutableLiveData<>();
    private final MutableLiveData<List<Brand>> brands = new MutableLiveData<>();
    private final MutableLiveData<List<Product>> everydayEssentials = new MutableLiveData<>();
    private final MutableLiveData<List<Product>> trendingProducts = new MutableLiveData<>();
    private final MutableLiveData<List<Product>> discountedProducts = new MutableLiveData<>();
    private final MutableLiveData<List<Product>> newArrivals = new MutableLiveData<>();
    private final MutableLiveData<List<Product>> featuredProducts = new MutableLiveData<>();
    private final MutableLiveData<Double> walletBalance = new MutableLiveData<>();
    private final MutableLiveData<AppConfig> appConfig = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public MainViewModel(@NonNull Application application) {
        super(application);
        repository = new SupabaseRepository();
        themeManager = com.nmmart.retailos.utils.ThemeManager.getInstance(application);
    }

    // Getters
    public LiveData<List<Banner>> getBanners() { return banners; }
    public LiveData<List<Category>> getCategories() { return categories; }
    public LiveData<List<Brand>> getBrands() { return brands; }
    public LiveData<List<Product>> getEverydayEssentials() { return everydayEssentials; }
    public LiveData<List<Product>> getTrendingProducts() { return trendingProducts; }
    public LiveData<List<Product>> getDiscountedProducts() { return discountedProducts; }
    public LiveData<List<Product>> getNewArrivals() { return newArrivals; }
    public LiveData<List<Product>> getFeaturedProducts() { return featuredProducts; }
    public LiveData<Double> getWalletBalance() { return walletBalance; }
    public LiveData<AppConfig> getAppConfig() { return appConfig; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    // Compatibility methods for MainActivity
    public LiveData<List<Product>> getBestSelling() { return trendingProducts; }
    public LiveData<List<Product>> getNewStockProducts() { return newArrivals; }

    public void fetchHomeData() {
        OfflineStorage offlineStorage = OfflineStorage.getInstance(getApplication());
        
        // First try to load from offline
        List<Banner> offlineBanners = offlineStorage.getBanners();
        if (!offlineBanners.isEmpty()) {
            banners.setValue(offlineBanners);
            Log.d(TAG, "Banners loaded from offline storage");
        }
        
        List<Category> offlineCategories = offlineStorage.getCategories();
        if (!offlineCategories.isEmpty()) {
            categories.setValue(offlineCategories);
            Log.d(TAG, "Categories loaded from offline storage");
        }
        
        List<Product> offlineTrending = offlineStorage.getProducts("trending");
        if (!offlineTrending.isEmpty()) {
            trendingProducts.setValue(offlineTrending);
        }
        
        List<Product> offlineEveryday = offlineStorage.getProducts("everyday");
        if (!offlineEveryday.isEmpty()) {
            everydayEssentials.setValue(offlineEveryday);
        }
        
        List<Product> offlineDiscounted = offlineStorage.getProducts("discounted");
        if (!offlineDiscounted.isEmpty()) {
            discountedProducts.setValue(offlineDiscounted);
        }
        
        List<Product> offlineNewArrivals = offlineStorage.getProducts("new_arrivals");
        if (!offlineNewArrivals.isEmpty()) {
            newArrivals.setValue(offlineNewArrivals);
        }
        
        List<Product> offlineFeatured = offlineStorage.getProducts("featured");
        if (!offlineFeatured.isEmpty()) {
            featuredProducts.setValue(offlineFeatured);
        }

        if (!NetworkUtils.isNetworkAvailable(getApplication())) {
            Log.w(TAG, "No internet connection, using offline data");
            if (categories.getValue() == null || categories.getValue().isEmpty()) {
                errorMessage.setValue("No internet connection and no offline data available.");
            }
            isLoading.setValue(false);
            return;
        }

        isLoading.setValue(true);
        cancelAllCalls();

        // Step 0: Fetch AppConfig FIRST to apply theme immediately
        Log.d(TAG, "Fetching AppConfig first");
        enqueueCall(repository.getHomeConfigCall(), new Callback<List<com.nmmart.retailos.models.HomeConfig>>() {
            @Override
            public void onResponse(@NonNull Call<List<com.nmmart.retailos.models.HomeConfig>> call, @NonNull Response<List<com.nmmart.retailos.models.HomeConfig>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    // Assuming HomeConfig has AppConfig fields or we need to fetch AppConfig separately
                    Log.d(TAG, "HomeConfig fetched");
                }
            }
            @Override public void onFailure(@NonNull Call<List<com.nmmart.retailos.models.HomeConfig>> call, @NonNull Throwable t) {
                Log.e(TAG, "Error fetching HomeConfig", t);
            }
        });

        // Also fetch AppConfig directly
        repository.getAppConfig(new Callback<List<AppConfig>>() {
            @Override
            public void onResponse(@NonNull Call<List<AppConfig>> call, @NonNull Response<List<AppConfig>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    AppConfig config = response.body().get(0);
                    appConfig.setValue(config);
                    themeManager.setAppConfig(config);
                    Log.d(TAG, "AppConfig fetched and applied to ThemeManager");
                }
            }
            @Override
            public void onFailure(@NonNull Call<List<AppConfig>> call, @NonNull Throwable t) {
                Log.e(TAG, "Error fetching AppConfig", t);
            }
        });

        // Step 1: Fetch critical data first (banners, categories, brands)
        Log.d(TAG, "Fetching critical data (banners, categories, brands)");
        enqueueCall(repository.getLiveBannersCall(), new Callback<List<Banner>>() {
            @Override
            public void onResponse(@NonNull Call<List<Banner>> call, @NonNull Response<List<Banner>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Banner> sortedBanners = new java.util.ArrayList<>(response.body());
                    // Sort banners by position in ascending order
                    java.util.Collections.sort(sortedBanners, (b1, b2) -> Integer.compare(b1.position, b2.position));
                    banners.setValue(sortedBanners);
                    offlineStorage.saveBanners(sortedBanners);
                    Log.d(TAG, "Banners fetched, sorted, and saved to offline storage successfully");
                } else {
                    Log.e(TAG, "Failed to fetch banners: " + response.code());
                }
            }
            @Override public void onFailure(@NonNull Call<List<Banner>> call, @NonNull Throwable t) {
                Log.e(TAG, "Error fetching banners", t);
            }
        });

        enqueueCall(repository.getCategoriesCall(), new Callback<List<Category>>() {
            @Override
            public void onResponse(@NonNull Call<List<Category>> call, @NonNull Response<List<Category>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    categories.setValue(response.body());
                    offlineStorage.saveCategories(response.body());
                    Log.d(TAG, "Categories fetched and saved to offline storage successfully");
                } else {
                    Log.e(TAG, "Failed to fetch categories: " + response.code());
                }
            }
            @Override public void onFailure(@NonNull Call<List<Category>> call, @NonNull Throwable t) {
                Log.e(TAG, "Error fetching categories", t);
            }
        });

        enqueueCall(repository.getBrandsCall(), new Callback<List<Brand>>() {
            @Override
            public void onResponse(@NonNull Call<List<Brand>> call, @NonNull Response<List<Brand>> response) {
                if (response.isSuccessful()) {
                    brands.setValue(response.body());
                    Log.d(TAG, "Brands fetched successfully");
                } else {
                    Log.e(TAG, "Failed to fetch brands: " + response.code());
                }
            }
            @Override public void onFailure(@NonNull Call<List<Brand>> call, @NonNull Throwable t) {
                Log.e(TAG, "Error fetching brands", t);
            }
        });

        // Step 2: Fetch product sections with staggered delays
        fetchSectionsWithDelay();
    }

    private void fetchSectionsWithDelay() {
        OfflineStorage offlineStorage = OfflineStorage.getInstance(getApplication());
        
        // Fetch trending products first (sets isLoading to false when done)
        handler.postDelayed(() -> {
            Log.d(TAG, "Fetching trending products");
            enqueueCall(repository.getTrendingProductsCall(10), new Callback<List<Product>>() {
                @Override
                public void onResponse(@NonNull Call<List<Product>> call, @NonNull Response<List<Product>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        trendingProducts.setValue(response.body());
                        offlineStorage.saveProducts("trending", response.body());
                        Log.d(TAG, "Trending products fetched and saved to offline storage successfully");
                    } else {
                        Log.e(TAG, "Failed to fetch trending products: " + response.code());
                    }
                    isLoading.setValue(false);
                }
                @Override public void onFailure(@NonNull Call<List<Product>> call, @NonNull Throwable t) {
                    Log.e(TAG, "Error fetching trending products", t);
                    isLoading.setValue(false);
                }
            });
        }, 0);

        // Fetch everyday essentials after a small delay
        handler.postDelayed(() -> {
            Log.d(TAG, "Fetching everyday essentials");
            enqueueCall(repository.getProductsSortedCall(null, null, "sale_rate.asc", 10, 0), new Callback<List<Product>>() {
                @Override
                public void onResponse(@NonNull Call<List<Product>> call, @NonNull Response<List<Product>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        everydayEssentials.setValue(response.body());
                        offlineStorage.saveProducts("everyday", response.body());
                        Log.d(TAG, "Everyday essentials fetched and saved to offline storage successfully");
                    } else {
                        Log.e(TAG, "Failed to fetch everyday essentials: " + response.code());
                    }
                }
                @Override public void onFailure(@NonNull Call<List<Product>> call, @NonNull Throwable t) {
                    Log.e(TAG, "Error fetching everyday essentials", t);
                }
            });
        }, STAGGER_DELAY_MS);

        // Fetch discounted products
        handler.postDelayed(() -> {
            Log.d(TAG, "Fetching discounted products");
            enqueueCall(repository.getDiscountProductsCall(10), new Callback<List<Product>>() {
                @Override
                public void onResponse(@NonNull Call<List<Product>> call, @NonNull Response<List<Product>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        discountedProducts.setValue(response.body());
                        offlineStorage.saveProducts("discounted", response.body());
                        Log.d(TAG, "Discounted products fetched and saved to offline storage successfully");
                    } else {
                        Log.e(TAG, "Failed to fetch discounted products: " + response.code());
                    }
                }
                @Override public void onFailure(@NonNull Call<List<Product>> call, @NonNull Throwable t) {
                    Log.e(TAG, "Error fetching discounted products", t);
                }
            });
        }, STAGGER_DELAY_MS * 2);

        // Fetch new arrivals
        handler.postDelayed(() -> {
            Log.d(TAG, "Fetching new arrivals");
            enqueueCall(repository.getNewArrivalProductsCall(10), new Callback<List<Product>>() {
                @Override
                public void onResponse(@NonNull Call<List<Product>> call, @NonNull Response<List<Product>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        newArrivals.setValue(response.body());
                        offlineStorage.saveProducts("new_arrivals", response.body());
                        Log.d(TAG, "New arrivals fetched and saved to offline storage successfully");
                    } else {
                        Log.e(TAG, "Failed to fetch new arrivals: " + response.code());
                    }
                }
                @Override public void onFailure(@NonNull Call<List<Product>> call, @NonNull Throwable t) {
                    Log.e(TAG, "Error fetching new arrivals", t);
                }
            });
        }, STAGGER_DELAY_MS * 3);

        // Fetch featured products
        handler.postDelayed(() -> {
            Log.d(TAG, "Fetching featured products");
            enqueueCall(repository.getFeaturedProductsCall(10), new Callback<List<Product>>() {
                @Override
                public void onResponse(@NonNull Call<List<Product>> call, @NonNull Response<List<Product>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        featuredProducts.setValue(response.body());
                        offlineStorage.saveProducts("featured", response.body());
                        Log.d(TAG, "Featured products fetched and saved to offline storage successfully");
                    } else {
                        Log.e(TAG, "Failed to fetch featured products: " + response.code());
                    }
                }
                @Override public void onFailure(@NonNull Call<List<Product>> call, @NonNull Throwable t) {
                    Log.e(TAG, "Error fetching featured products", t);
                }
            });
        }, STAGGER_DELAY_MS * 4);
    }

    private <T> void enqueueCall(Call<T> call, Callback<T> callback) {
        activeCalls.add(call);
        call.enqueue(new Callback<T>() {
            @Override
            public void onResponse(@NonNull Call<T> call, @NonNull Response<T> response) {
                activeCalls.remove(call);
                callback.onResponse(call, response);
            }
            @Override
            public void onFailure(@NonNull Call<T> call, @NonNull Throwable t) {
                activeCalls.remove(call);
                callback.onFailure(call, t);
            }
        });
    }

    public void fetchWallet() {
        repository.getWallets(new Callback<List<WalletMaster>>() {
            @Override
            public void onResponse(@NonNull Call<List<WalletMaster>> call, @NonNull Response<List<WalletMaster>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    walletBalance.setValue(response.body().get(0).currentBalance);
                    Log.d(TAG, "Wallet balance fetched successfully");
                }
            }
            @Override
            public void onFailure(@NonNull Call<List<WalletMaster>> call, @NonNull Throwable t) {
                Log.e(TAG, "Error fetching wallet", t);
            }
        });
    }

    public void fetchProductByBarcode(String barcode, OnProductFetchedListener listener) {
        repository.getProductByBarcode(barcode, new Callback<List<Product>>() {
            @Override
            public void onResponse(@NonNull Call<List<Product>> call, @NonNull Response<List<Product>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    listener.onFetched(response.body().get(0));
                } else {
                    listener.onFetched(null);
                }
            }
            @Override
            public void onFailure(@NonNull Call<List<Product>> call, @NonNull Throwable t) {
                Log.e(TAG, "Error fetching product by barcode", t);
                listener.onFetched(null);
            }
        });
    }

    public interface OnProductFetchedListener {
        void onFetched(Product product);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        handler.removeCallbacksAndMessages(null);
        cancelAllCalls();
    }

    private void cancelAllCalls() {
        for (Call<?> call : activeCalls) {
            if (call != null && !call.isCanceled()) call.cancel();
        }
        activeCalls.clear();
    }
}
