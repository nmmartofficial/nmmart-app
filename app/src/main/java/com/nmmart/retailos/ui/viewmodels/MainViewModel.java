package com.nmmart.retailos.ui.viewmodels;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.nmmart.retailos.data.SupabaseRepository;
import com.nmmart.retailos.models.Banner;
import com.nmmart.retailos.models.Category;
import com.nmmart.retailos.models.HomeConfig;
import com.nmmart.retailos.models.Product;
import com.nmmart.retailos.utils.NetworkUtils;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainViewModel extends AndroidViewModel {
    private SupabaseRepository repository;
    private List<Call<?>> activeCalls = new ArrayList<>();
    private SharedPreferences sharedPreferences;
    private Gson gson;
    private static final String PREF_NAME = "MainCache";

    private MutableLiveData<List<HomeConfig>> homeConfig = new MutableLiveData<>();
    private MutableLiveData<List<Category>> categories = new MutableLiveData<>();
    private MutableLiveData<List<Product>> trendingProducts = new MutableLiveData<>();
    private MutableLiveData<List<Product>> newStockProducts = new MutableLiveData<>();
    private MutableLiveData<List<Product>> discountedProducts = new MutableLiveData<>();
    private MutableLiveData<List<Banner>> banners = new MutableLiveData<>();
    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public MainViewModel(@NonNull Application application) {
        super(application);
        repository = new SupabaseRepository();
        sharedPreferences = application.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
        loadCachedData();
    }

    private void loadCachedData() {
        String catJson = sharedPreferences.getString("categories", null);
        if (catJson != null) {
            Type type = new TypeToken<List<Category>>() {}.getType();
            categories.setValue(gson.fromJson(catJson, type));
        }

        String bannerJson = sharedPreferences.getString("banners", null);
        if (bannerJson != null) {
            Type type = new TypeToken<List<Banner>>() {}.getType();
            banners.setValue(gson.fromJson(bannerJson, type));
        }
    }

    private void cacheData(String key, Object data) {
        sharedPreferences.edit().putString(key, gson.toJson(data)).apply();
    }

    public LiveData<List<HomeConfig>> getHomeConfig() { return homeConfig; }
    public LiveData<List<Category>> getCategories() { return categories; }
    public LiveData<List<Product>> getTrendingProducts() { return trendingProducts; }
    public LiveData<List<Product>> getNewStockProducts() { return newStockProducts; }
    public LiveData<List<Product>> getDiscountedProducts() { return discountedProducts; }
    public LiveData<List<Banner>> getBanners() { return banners; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    public void fetchHomeData() {
        if (!NetworkUtils.isNetworkAvailable(getApplication())) {
            errorMessage.setValue("No internet connection. Please check your network.");
            return;
        }

        isLoading.setValue(true);
        
        Call<List<HomeConfig>> configCall = repository.getHomeConfigCall();
        configCall.enqueue(new Callback<List<HomeConfig>>() {
            @Override
            public void onResponse(Call<List<HomeConfig>> call, Response<List<HomeConfig>> response) {
                activeCalls.remove(call);
                if (response.isSuccessful()) homeConfig.setValue(response.body());
                else errorMessage.setValue("Failed to fetch config: " + response.message());
            }

            @Override
            public void onFailure(Call<List<HomeConfig>> call, Throwable t) {
                activeCalls.remove(call);
                errorMessage.setValue("Network error: " + t.getMessage());
            }
        });
        activeCalls.add(configCall);
        
        Call<List<Category>> categoryCall = repository.getCategoriesCall();
        categoryCall.enqueue(new Callback<List<Category>>() {
            @Override
            public void onResponse(Call<List<Category>> call, Response<List<Category>> response) {
                activeCalls.remove(call);
                if (response.isSuccessful() && response.body() != null) {
                    categories.setValue(response.body());
                    cacheData("categories", response.body());
                } else errorMessage.setValue("Failed to fetch categories");
            }

            @Override
            public void onFailure(Call<List<Category>> call, Throwable t) {
                activeCalls.remove(call);
            }
        });
        activeCalls.add(categoryCall);

        Call<List<Product>> trendingCall = repository.getTrendingProductsCall(30);
        trendingCall.enqueue(new Callback<List<Product>>() {
            @Override
            public void onResponse(Call<List<Product>> call, Response<List<Product>> response) {
                activeCalls.remove(call);
                if (response.isSuccessful() && response.body() != null) {
                    trendingProducts.setValue(response.body());
                    filterDiscountedProducts(response.body());
                } else {
                    errorMessage.setValue("Failed to fetch trending products");
                }
                isLoading.setValue(false);
            }

            @Override
            public void onFailure(Call<List<Product>> call, Throwable t) {
                activeCalls.remove(call);
                isLoading.setValue(false);
            }
        });
        activeCalls.add(trendingCall);

        Call<List<Product>> latestCall = repository.getLatestProductsCall(30);
        latestCall.enqueue(new Callback<List<Product>>() {
            @Override
            public void onResponse(Call<List<Product>> call, Response<List<Product>> response) {
                activeCalls.remove(call);
                if (response.isSuccessful() && response.body() != null) {
                    newStockProducts.setValue(response.body());
                } else errorMessage.setValue("Failed to fetch new arrivals");
            }

            @Override
            public void onFailure(Call<List<Product>> call, Throwable t) {
                activeCalls.remove(call);
            }
        });
        activeCalls.add(latestCall);
        
        Call<List<Banner>> bannerCall = repository.getLiveBannersCall();
        bannerCall.enqueue(new Callback<List<Banner>>() {
            @Override
            public void onResponse(Call<List<Banner>> call, Response<List<Banner>> response) {
                activeCalls.remove(call);
                if (response.isSuccessful() && response.body() != null) {
                    banners.setValue(response.body());
                    cacheData("banners", response.body());
                } else errorMessage.setValue("Failed to fetch banners");
            }

            @Override
            public void onFailure(Call<List<Banner>> call, Throwable t) {
                activeCalls.remove(call);
            }
        });
        activeCalls.add(bannerCall);
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        for (Call<?> call : activeCalls) {
            if (call != null && !call.isCanceled()) {
                call.cancel();
            }
        }
        activeCalls.clear();
    }
    
    private void filterDiscountedProducts(List<Product> allProducts) {
        List<Product> discounted = new ArrayList<>();
        if (allProducts != null) {
            for (Product product : allProducts) {
                if (product.nm_price < product.mrp) {
                    discounted.add(product);
                }
            }
        }
        discountedProducts.setValue(discounted);
    }
}
