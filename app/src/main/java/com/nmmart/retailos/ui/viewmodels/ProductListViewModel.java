package com.nmmart.retailos.ui.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.nmmart.retailos.data.SupabaseRepository;
import com.nmmart.retailos.models.Product;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProductListViewModel extends ViewModel {
    private SupabaseRepository repository;
    
    private List<Call<?>> activeCalls = new ArrayList<>();
    
    private MutableLiveData<List<Product>> products = new MutableLiveData<>();
    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private MutableLiveData<Boolean> isLoadingMore = new MutableLiveData<>();
    private MutableLiveData<String> errorMessage = new MutableLiveData<>();
    
    private String currentCategory;
    private String currentBrand;
    private String currentSearchQuery;
    private int currentOffset = 0;
    private static final int PAGE_SIZE = 30;
    private boolean isLastPage = false;

    public ProductListViewModel() {
        repository = new SupabaseRepository();
    }

    public LiveData<List<Product>> getProducts() { return products; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<Boolean> getIsLoadingMore() { return isLoadingMore; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public boolean isLastPage() { return isLastPage; }

    public void fetchProducts(String category) {
        fetchProducts(category, null, PAGE_SIZE, 0);
    }
    
    public void fetchProductsByBrand(String brand) {
        fetchProducts(null, brand, PAGE_SIZE, 0);
    }
    
    public void fetchProducts(String category, String brand, int limit, int offset) {
        cancelAllCalls();
        currentCategory = category;
        currentBrand = brand;
        currentSearchQuery = null;
        currentOffset = offset;
        isLastPage = false;
        isLoading.setValue(true);
        
        Call<List<Product>> call = repository.getProductsSortedCall(category, brand, "sale_rate.asc", limit, currentOffset);
        activeCalls.add(call);
        call.enqueue(new Callback<List<Product>>() {
            @Override
            public void onResponse(Call<List<Product>> call, Response<List<Product>> response) {
                activeCalls.remove(call);
                isLoading.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    products.setValue(response.body());
                    if (response.body().size() < limit) isLastPage = true;
                    currentOffset += limit;
                } else errorMessage.setValue("Failed to fetch products");
            }

            @Override
            public void onFailure(Call<List<Product>> call, Throwable t) {
                activeCalls.remove(call);
                isLoading.setValue(false);
                errorMessage.setValue(t.getMessage());
            }
        });
    }

    public void searchProducts(String query) {
        cancelAllCalls();
        currentCategory = null;
        currentSearchQuery = query;
        currentOffset = 0;
        isLastPage = false;
        isLoading.setValue(true);
        
        Call<List<Product>> call = repository.searchProductsCall(query, PAGE_SIZE, currentOffset);
        activeCalls.add(call);
        call.enqueue(new Callback<List<Product>>() {
            @Override
            public void onResponse(Call<List<Product>> call, Response<List<Product>> response) {
                activeCalls.remove(call);
                isLoading.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    products.setValue(response.body());
                    if (response.body().size() < PAGE_SIZE) isLastPage = true;
                    currentOffset = PAGE_SIZE;
                } else errorMessage.setValue("Search failed");
            }

            @Override
            public void onFailure(Call<List<Product>> call, Throwable t) {
                activeCalls.remove(call);
                isLoading.setValue(false);
                errorMessage.setValue(t.getMessage());
            }
        });
    }

    public void loadMoreProducts() {
        Boolean currentIsLoading = isLoading.getValue();
        if (currentIsLoading != null && currentIsLoading) return;
        if (isLastPage) return;
        isLoadingMore.setValue(true);

        Callback<List<Product>> callback = new Callback<List<Product>>() {
            @Override
            public void onResponse(Call<List<Product>> call, Response<List<Product>> response) {
                activeCalls.remove(call);
                isLoadingMore.setValue(false);
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    List<Product> currentList = products.getValue() != null ? new ArrayList<>(products.getValue()) : new ArrayList<>();
                    currentList.addAll(response.body());
                    products.setValue(currentList);
                    if (response.body().size() < PAGE_SIZE) isLastPage = true;
                    currentOffset += PAGE_SIZE;
                }
            }

            @Override
            public void onFailure(Call<List<Product>> call, Throwable t) {
                activeCalls.remove(call);
                isLoadingMore.setValue(false);
            }
        };

        Call<List<Product>> call = null;
        if (currentCategory != null) {
            call = repository.getProductsSortedCall(currentCategory, null, "sale_rate.asc", PAGE_SIZE, currentOffset);
        } else if (currentBrand != null) {
            call = repository.getProductsSortedCall(null, currentBrand, "sale_rate.asc", PAGE_SIZE, currentOffset);
        } else if (currentSearchQuery != null) {
            call = repository.searchProductsCall(currentSearchQuery, PAGE_SIZE, currentOffset);
        } else {
            call = repository.getProductsSortedCall(null, null, "sale_rate.asc", PAGE_SIZE, currentOffset);
        }
        
        if (call != null) {
            activeCalls.add(call);
            call.enqueue(callback);
        }
    }

    public void sortProducts(int position) {
        List<Product> currentList = products.getValue();
        if (currentList == null || currentList.isEmpty()) return;

        switch (position) {
            case 1: // Price: Low to High
                Collections.sort(currentList, (p1, p2) -> {
                    return Double.compare(p1.getNmPrice(), p2.getNmPrice());
                });
                break;
            case 2: // Price: High to Low
                Collections.sort(currentList, (p1, p2) -> {
                    return Double.compare(p2.getNmPrice(), p1.getNmPrice());
                });
                break;
            case 3: // Discount: High to Low
                Collections.sort(currentList, (p1, p2) -> {
                    return Double.compare(p2.getDiscount(), p1.getDiscount());
                });
                break;
        }
        products.setValue(currentList);
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        cancelAllCalls();
    }
    
    private void cancelAllCalls() {
        for (Call<?> call : activeCalls) {
            if (!call.isCanceled()) {
                call.cancel();
            }
        }
        activeCalls.clear();
    }
}
