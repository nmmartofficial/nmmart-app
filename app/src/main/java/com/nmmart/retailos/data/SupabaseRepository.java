package com.nmmart.retailos.data;

import android.util.Log;
import com.nmmart.retailos.models.Address;
import com.nmmart.retailos.models.AppConfig;
import com.nmmart.retailos.models.Banner;
import com.nmmart.retailos.models.Brand;
import com.nmmart.retailos.models.Category;
import com.nmmart.retailos.models.HomeConfig;
import com.nmmart.retailos.models.Order;
import com.nmmart.retailos.models.Product;
import com.nmmart.retailos.models.WalletMaster;
import com.nmmart.retailos.models.WalletTransaction;
import com.nmmart.retailos.models.PincodeMaster;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SupabaseRepository {
    private static final String TAG = "SupabaseRepository";
    private SupabaseConfig.SupabaseService apiService;
    private String apiKey;

    public SupabaseRepository() {
        Log.d(TAG, "Initializing SupabaseRepository");
        this.apiService = SupabaseConfig.getService();
        this.apiKey = SupabaseConfig.getApiKey();
    }

    private String anonOrUserAuth() {
        return SupabaseConfig.getAuthorizationHeader();
    }

    private String requireUserAuth() {
        String auth = SupabaseConfig.getUserAuthorizationHeaderOrEmpty();
        if (auth.isEmpty()) throw new IllegalStateException("Not logged in");
        return auth;
    }
    
    private <T> Callback<T> wrapCallback(String methodName, Callback<T> originalCallback) {
        return new Callback<T>() {
            @Override
            public void onResponse(Call<T> call, Response<T> response) {
                Log.d(TAG, methodName + " - Response: success: " + response.isSuccessful() + ", code: " + response.code());
                if (response.isSuccessful()) {
                    Log.d(TAG, methodName + " - Response body: " + (response.body() != null ? response.body().getClass().getSimpleName() : "null"));
                } else {
                    Log.e(TAG, methodName + " - Error response: " + response.message());
                }
                originalCallback.onResponse(call, response);
            }
            
            @Override
            public void onFailure(Call<T> call, Throwable t) {
                Log.e(TAG, methodName + " - API call failed", t);
                originalCallback.onFailure(call, t);
            }
        };
    }

    // --- Methods that return Call objects (for ViewModel tracking) ---
    public Call<List<Product>> getProductsSortedCall(String category, String brand, String order, int limit, int offset) {
        return apiService.getProductsSorted(apiKey, anonOrUserAuth(), category, brand, order, limit, offset);
    }

    public Call<List<Product>> searchProductsCall(String query, int limit, int offset) {
        return apiService.searchProducts(apiKey, anonOrUserAuth(), "ilike.%" + query + "%", limit, offset);
    }

    public Call<List<HomeConfig>> getHomeConfigCall() {
        return apiService.getHomeConfig(apiKey, anonOrUserAuth());
    }

    public Call<List<Category>> getCategoriesCall() {
        return apiService.getCategories(apiKey, anonOrUserAuth());
    }
    
    public Call<List<Brand>> getBrandsCall() {
        return apiService.getBrands(apiKey, anonOrUserAuth());
    }

    public Call<List<Product>> getTrendingProductsCall(int limit) {
        return apiService.getTrendingProducts(apiKey, anonOrUserAuth(), "stock.desc", limit);
    }

    public Call<List<Product>> getLatestProductsCall(int limit) {
        return apiService.getLatestProducts(apiKey, anonOrUserAuth(), "id.desc", limit);
    }

    public Call<List<Banner>> getLiveBannersCall() {
        return apiService.getBanners(apiKey, anonOrUserAuth());
    }

    // --- Legacy enqueue methods (keep for backward compatibility) ---
    public void getProducts(String category, Callback<List<Product>> callback) {
        Log.d(TAG, "getProducts called with category: " + category);
        apiService.getProducts(apiKey, anonOrUserAuth(), "eq." + category).enqueue(wrapCallback("getProducts", callback));
    }

    public void searchProducts(String query, int limit, int offset, Callback<List<Product>> callback) {
        Log.d(TAG, "searchProducts called with query: " + query + ", limit: " + limit + ", offset: " + offset);
        searchProductsCall(query, limit, offset).enqueue(wrapCallback("searchProducts", callback));
    }

    public void getHomeConfig(Callback<List<HomeConfig>> callback) {
        Log.d(TAG, "getHomeConfig called");
        apiService.getHomeConfig(apiKey, anonOrUserAuth()).enqueue(wrapCallback("getHomeConfig", callback));
    }

    public void getTrendingProducts(int limit, Callback<List<Product>> callback) {
        Log.d(TAG, "getTrendingProducts called with limit: " + limit);
        apiService.getTrendingProducts(apiKey, anonOrUserAuth(), "stock.desc", limit).enqueue(wrapCallback("getTrendingProducts", callback));
    }

    public void getLatestProducts(int limit, Callback<List<Product>> callback) {
        Log.d(TAG, "getLatestProducts called with limit: " + limit);
        apiService.getLatestProducts(apiKey, anonOrUserAuth(), "id.desc", limit).enqueue(wrapCallback("getLatestProducts", callback));
    }

    public void getProductsSorted(String category, String brand, String order, int limit, int offset, Callback<List<Product>> callback) {
        Log.d(TAG, "getProductsSorted called - category: " + category + ", brand: " + brand + ", order: " + order + ", limit: " + limit + ", offset: " + offset);
        getProductsSortedCall(category, brand, order, limit, offset).enqueue(wrapCallback("getProductsSorted", callback));
    }

    public void getAllProducts(int limit, int offset, Callback<List<Product>> callback) {
        Log.d(TAG, "getAllProducts called with limit: " + limit + ", offset: " + offset);
        apiService.getAllProducts(apiKey, anonOrUserAuth(), limit, offset).enqueue(wrapCallback("getAllProducts", callback));
    }

    public void placeOrder(Map<String, Object> orderData, Callback<Void> callback) {
        Log.d(TAG, "placeOrder called");
        apiService.placeOrder(apiKey, requireUserAuth(), orderData).enqueue(wrapCallback("placeOrder", callback));
    }

    public void getLiveOrders(String mobile, String status, Callback<List<Order>> callback) {
        Log.d(TAG, "getLiveOrders called with mobile: " + mobile + ", status: " + status);
        apiService.getLiveOrders(apiKey, requireUserAuth(), "eq." + mobile, "eq." + status).enqueue(wrapCallback("getLiveOrders", callback));
    }

    public void getUserOrders(String userId, Callback<List<Order>> callback) {
        Log.d(TAG, "getUserOrders called with userId: " + userId);
        apiService.getUserOrders(apiKey, requireUserAuth(), "eq." + userId, "created_at.desc").enqueue(wrapCallback("getUserOrders", callback));
    }
    
    public void fetchLiveProducts(String category, int limit, int offset, Callback<List<Product>> callback) {
        Log.d(TAG, "fetchLiveProducts called with category: " + category + ", limit: " + limit + ", offset: " + offset);
        getProductsSorted(category, null, "sale_rate.asc", limit, offset, callback);
    }
    
    public void getAppConfig(Callback<List<AppConfig>> callback) {
        Log.d(TAG, "getAppConfig called");
        apiService.getAppConfig(apiKey, anonOrUserAuth(), 1).enqueue(wrapCallback("getAppConfig", callback));
    }

    public void getLiveBanners(Callback<List<Banner>> callback) {
        Log.d(TAG, "getLiveBanners called");
        apiService.getBanners(apiKey, anonOrUserAuth()).enqueue(wrapCallback("getLiveBanners", callback));
    }

    public void getUserAddresses(String userId, Callback<List<Address>> callback) {
        Log.d(TAG, "getUserAddresses called with userId: " + userId);
        apiService.getUserAddresses(apiKey, requireUserAuth(), "eq." + userId).enqueue(wrapCallback("getUserAddresses", callback));
    }

    public void addAddress(Map<String, Object> addressData, Callback<Void> callback) {
        Log.d(TAG, "addAddress called");
        apiService.addAddress(apiKey, requireUserAuth(), addressData).enqueue(wrapCallback("addAddress", callback));
    }

    public void updateAddress(String addressId, Map<String, Object> addressData, Callback<Void> callback) {
        Log.d(TAG, "updateAddress called with addressId: " + addressId);
        apiService.updateAddress(apiKey, requireUserAuth(), "eq." + addressId, addressData).enqueue(wrapCallback("updateAddress", callback));
    }

    public void deleteAddress(String addressId, Callback<Void> callback) {
        Log.d(TAG, "deleteAddress called with addressId: " + addressId);
        apiService.deleteAddress(apiKey, requireUserAuth(), "eq." + addressId).enqueue(wrapCallback("deleteAddress", callback));
    }

    public void getWallets(Callback<List<WalletMaster>> callback) {
        Log.d(TAG, "getWallets called");
        apiService.getWallets(apiKey, requireUserAuth()).enqueue(wrapCallback("getWallets", callback));
    }

    public void getWalletTransactions(String userId, Callback<List<WalletTransaction>> callback) {
        Log.d(TAG, "getWalletTransactions called with userId: " + userId);
        apiService.getWalletTransactions(apiKey, requireUserAuth(), "eq." + userId).enqueue(wrapCallback("getWalletTransactions", callback));
    }

    public void getPincodes(Callback<List<PincodeMaster>> callback) {
        Log.d(TAG, "getPincodes called");
        apiService.getPincodes(apiKey, anonOrUserAuth()).enqueue(wrapCallback("getPincodes", callback));
    }
    
    public void getCategories(Callback<List<Category>> callback) {
        Log.d(TAG, "getCategories called");
        apiService.getCategories(apiKey, anonOrUserAuth()).enqueue(wrapCallback("getCategories", callback));
    }
    
    public void getBrands(Callback<List<Brand>> callback) {
        Log.d(TAG, "getBrands called");
        apiService.getBrands(apiKey, anonOrUserAuth()).enqueue(wrapCallback("getBrands", callback));
    }

    public void validateAndApplyCoupon(String code, double cartTotal, Callback<List<CouponValidationResult>> callback) {
        Log.d(TAG, "validateAndApplyCoupon called with code: " + code + ", cartTotal: " + cartTotal);
        Map<String, Object> body = new HashMap<>();
        body.put("p_code", code);
        body.put("p_cart_total", cartTotal);
        apiService.validateAndApplyCoupon(apiKey, requireUserAuth(), body).enqueue(wrapCallback("validateAndApplyCoupon", callback));
    }

    public void getCoupons(Callback<List<com.nmmart.retailos.models.Coupon>> callback) {
        Log.d(TAG, "getCoupons called");
        apiService.getCoupons(apiKey, requireUserAuth()).enqueue(wrapCallback("getCoupons", callback));
    }

    public void updateOrderStatus(String orderId, Map<String, Object> data, Callback<Void> callback) {
        Log.d(TAG, "updateOrderStatus called with orderId: " + orderId);
        apiService.updateOrder(apiKey, requireUserAuth(), "eq." + orderId, data).enqueue(wrapCallback("updateOrderStatus", callback));
    }
}
