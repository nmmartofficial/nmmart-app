package com.nmmart.retailos.ui.viewmodels;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.nmmart.retailos.data.SupabaseRepository;
import com.nmmart.retailos.models.Category;
import com.nmmart.retailos.models.Product;
import com.nmmart.retailos.utils.NetworkUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProductListViewModel extends AndroidViewModel {
    private SupabaseRepository repository;
    
    private List<Call<?>> activeCalls = new ArrayList<>();
    
    private MutableLiveData<List<Product>> products = new MutableLiveData<>();
    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private MutableLiveData<Boolean> isLoadingMore = new MutableLiveData<>();
    private MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private MutableLiveData<List<Category>> subcategories = new MutableLiveData<>();
    
    private String currentCategoryId;
    private String currentSubcategoryId;
    private String currentBrandId;
    private String currentSearchQuery;
    private int currentOffset = 0;
    private static final int PAGE_SIZE = 30;
    private boolean isLastPage = false;

    public ProductListViewModel(@NonNull Application application) {
        super(application);
        repository = new SupabaseRepository();
    }

    public LiveData<List<Product>> getProducts() { return products; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<Boolean> getIsLoadingMore() { return isLoadingMore; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<List<Category>> getSubcategories() { return subcategories; }
    public boolean isLastPage() { return isLastPage; }

    public void fetchProducts(String categoryId) {
        fetchProducts(categoryId, null, null, PAGE_SIZE, 0);
    }
    
    public void fetchProductsBySubcategory(String subcategoryId) {
        fetchProducts(null, subcategoryId, null, PAGE_SIZE, 0);
    }
    
    public void fetchProductsByBrand(String brandId) {
        fetchProducts(null, null, brandId, PAGE_SIZE, 0);
    }
    
    // Compatibility method for existing calls
    public void fetchProducts(String categoryId, String brandId, int limit, int offset) {
        fetchProducts(categoryId, null, brandId, limit, offset);
    }

    public void fetchProducts(String categoryId, String subcategoryId, String brandId, int limit, int offset) {
        if (!NetworkUtils.isNetworkAvailable(getApplication())) {
            errorMessage.setValue("No internet connection.");
            isLoading.setValue(false);
            return;
        }
        cancelAllCalls();
        currentCategoryId = categoryId;
        currentSubcategoryId = subcategoryId;
        currentBrandId = brandId;
        currentSearchQuery = null;
        currentOffset = offset;
        isLastPage = false;
        isLoading.setValue(true);

        // Use subcategoryId if available, else categoryId
        String catIdToUse = subcategoryId != null ? subcategoryId : categoryId;
        Call<List<Product>> call = repository.getProductsSortedCall(catIdToUse, brandId, null, limit, currentOffset);
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
                } else {
                    // Fallback to show empty list if error
                    products.setValue(new ArrayList<>());
                    errorMessage.setValue("Failed to fetch products: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<List<Product>> call, Throwable t) {
                activeCalls.remove(call);
                isLoading.setValue(false);
                products.setValue(new ArrayList<>());
                errorMessage.setValue("Network error: " + t.getMessage());
            }
        });
    }

    public void fetchSubcategories(String parentId) {
        if (!NetworkUtils.isNetworkAvailable(getApplication())) {
            return;
        }
        Call<List<Category>> call = repository.getSubCategoriesCall(parentId);
        activeCalls.add(call);
        call.enqueue(new Callback<List<Category>>() {
            @Override
            public void onResponse(Call<List<Category>> call, Response<List<Category>> response) {
                activeCalls.remove(call);
                if (response.isSuccessful() && response.body() != null) {
                    subcategories.setValue(response.body());
                } else {
                    subcategories.setValue(new ArrayList<>());
                }
            }

            @Override
            public void onFailure(Call<List<Category>> call, Throwable t) {
                activeCalls.remove(call);
                subcategories.setValue(new ArrayList<>());
            }
        });
    }

    public void searchProducts(String query) {
        if (!NetworkUtils.isNetworkAvailable(getApplication())) {
            errorMessage.setValue("No internet connection.");
            isLoading.setValue(false);
            return;
        }
        cancelAllCalls();
        currentCategoryId = null;
        currentBrandId = null;
        currentSearchQuery = query;
        currentOffset = 0;
        isLastPage = false;
        isLoading.setValue(true);
        
        Call<List<Product>> call = repository.searchProductsCall(query, (Integer) PAGE_SIZE, (Integer) currentOffset);
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
                } else errorMessage.setValue("Search failed: " + response.message());
            }

            @Override
            public void onFailure(Call<List<Product>> call, Throwable t) {
                activeCalls.remove(call);
                isLoading.setValue(false);
                errorMessage.setValue("Network error: " + t.getMessage());
            }
        });
    }

    public void loadMoreProducts() {
        if (!NetworkUtils.isNetworkAvailable(getApplication())) {
            return;
        }
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
        String catIdToUse = currentSubcategoryId != null ? currentSubcategoryId : currentCategoryId;
        if (catIdToUse != null) {
            call = repository.getProductsSortedCall(catIdToUse, null, null, (Integer) PAGE_SIZE, (Integer) currentOffset);
        } else if (currentBrandId != null) {
            call = repository.getProductsSortedCall(null, currentBrandId, null, (Integer) PAGE_SIZE, (Integer) currentOffset);
        } else if (currentSearchQuery != null) {
            call = repository.searchProductsCall(currentSearchQuery, (Integer) PAGE_SIZE, (Integer) currentOffset);
        } else {
            call = repository.getProductsSortedCall(null, null, null, (Integer) PAGE_SIZE, (Integer) currentOffset);
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
