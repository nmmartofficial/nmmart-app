package com.nmmart.retailos.ui.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.nmmart.retailos.data.SupabaseRepository;
import com.nmmart.retailos.models.Banner;
import com.nmmart.retailos.models.Category;
import com.nmmart.retailos.models.HomeConfig;
import com.nmmart.retailos.models.Product;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainViewModel extends ViewModel {
    private SupabaseRepository repository;
    
    private MutableLiveData<List<HomeConfig>> homeConfig = new MutableLiveData<>();
    private MutableLiveData<List<Category>> categories = new MutableLiveData<>();
    private MutableLiveData<List<Product>> trendingProducts = new MutableLiveData<>();
    private MutableLiveData<List<Product>> newStockProducts = new MutableLiveData<>();
    private MutableLiveData<List<Product>> discountedProducts = new MutableLiveData<>();
    private MutableLiveData<List<Banner>> banners = new MutableLiveData<>();
    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public MainViewModel() {
        repository = new SupabaseRepository();
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
        isLoading.setValue(true);
        repository.getHomeConfig(new Callback<List<HomeConfig>>() {
            @Override
            public void onResponse(Call<List<HomeConfig>> call, Response<List<HomeConfig>> response) {
                if (response.isSuccessful()) homeConfig.setValue(response.body());
                else errorMessage.setValue("Failed to fetch config");
            }

            @Override
            public void onFailure(Call<List<HomeConfig>> call, Throwable t) {
                errorMessage.setValue(t.getMessage());
            }
        });
        
        repository.getCategories(new Callback<List<Category>>() {
            @Override
            public void onResponse(Call<List<Category>> call, Response<List<Category>> response) {
                if (response.isSuccessful()) categories.setValue(response.body());
            }

            @Override
            public void onFailure(Call<List<Category>> call, Throwable t) {}
        });

        repository.getTrendingProducts(30, new Callback<List<Product>>() {
            @Override
            public void onResponse(Call<List<Product>> call, Response<List<Product>> response) {
                if (response.isSuccessful()) {
                    trendingProducts.setValue(response.body());
                    filterDiscountedProducts(response.body());
                }
                isLoading.setValue(false);
            }

            @Override
            public void onFailure(Call<List<Product>> call, Throwable t) {
                isLoading.setValue(false);
            }
        });

        repository.getLatestProducts(30, new Callback<List<Product>>() {
            @Override
            public void onResponse(Call<List<Product>> call, Response<List<Product>> response) {
                if (response.isSuccessful()) newStockProducts.setValue(response.body());
            }

            @Override
            public void onFailure(Call<List<Product>> call, Throwable t) {}
        });
        
        repository.getLiveBanners(new Callback<List<Banner>>() {
            @Override
            public void onResponse(Call<List<Banner>> call, Response<List<Banner>> response) {
                if (response.isSuccessful()) banners.setValue(response.body());
            }

            @Override
            public void onFailure(Call<List<Banner>> call, Throwable t) {}
        });
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
