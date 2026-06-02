package com.nmmart.retailos.data;

import com.nmmart.retailos.models.Address;
import com.nmmart.retailos.models.AppConfig;
import com.nmmart.retailos.models.Banner;
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

public class SupabaseRepository {
    private SupabaseConfig.SupabaseService apiService;
    private String apiKey;

    public SupabaseRepository() {
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

    // --- Methods that return Call objects (for ViewModel tracking) ---
    public Call<List<Product>> getProductsSortedCall(String category, String brand, String order, int limit, int offset) {
        return apiService.getProductsSorted(apiKey, anonOrUserAuth(), category, brand, order, limit, offset);
    }

    public Call<List<Product>> searchProductsCall(String query, int limit, int offset) {
        return apiService.searchProducts(apiKey, anonOrUserAuth(), "ilike.*" + query + "*", limit, offset);
    }

    public Call<List<HomeConfig>> getHomeConfigCall() {
        return apiService.getHomeConfig(apiKey, anonOrUserAuth());
    }

    public Call<List<Category>> getCategoriesCall() {
        return apiService.getCategories(apiKey, anonOrUserAuth());
    }

    public Call<List<Product>> getTrendingProductsCall(int limit) {
        return apiService.getTrendingProducts(apiKey, anonOrUserAuth(), "stock.desc", limit);
    }

    public Call<List<Product>> getLatestProductsCall(int limit) {
        return apiService.getLatestProducts(apiKey, anonOrUserAuth(), "id.desc", limit);
    }

    public Call<List<Banner>> getLiveBannersCall() {
        return apiService.getBanners(apiKey, anonOrUserAuth(), "eq.true");
    }

    // --- Legacy enqueue methods (keep for backward compatibility) ---
    public void getProducts(String category, Callback<List<Product>> callback) {
        apiService.getProducts(apiKey, anonOrUserAuth(), "eq." + category).enqueue(callback);
    }

    public void searchProducts(String query, int limit, int offset, Callback<List<Product>> callback) {
        searchProductsCall(query, limit, offset).enqueue(callback);
    }

    public void getHomeConfig(Callback<List<HomeConfig>> callback) {
        apiService.getHomeConfig(apiKey, anonOrUserAuth()).enqueue(callback);
    }

    public void getTrendingProducts(int limit, Callback<List<Product>> callback) {
        apiService.getTrendingProducts(apiKey, anonOrUserAuth(), "stock.desc", limit).enqueue(callback);
    }

    public void getLatestProducts(int limit, Callback<List<Product>> callback) {
        apiService.getLatestProducts(apiKey, anonOrUserAuth(), "id.desc", limit).enqueue(callback);
    }

    public void getProductsSorted(String category, String brand, String order, int limit, int offset, Callback<List<Product>> callback) {
        getProductsSortedCall(category, brand, order, limit, offset).enqueue(callback);
    }

    public void getAllProducts(int limit, int offset, Callback<List<Product>> callback) {
        apiService.getAllProducts(apiKey, anonOrUserAuth(), limit, offset).enqueue(callback);
    }

    public void placeOrder(Map<String, Object> orderData, Callback<Void> callback) {
        apiService.placeOrder(apiKey, requireUserAuth(), orderData).enqueue(callback);
    }

    public void getLiveOrders(String mobile, String status, Callback<List<Order>> callback) {
        apiService.getLiveOrders(apiKey, requireUserAuth(), "eq." + mobile, "eq." + status).enqueue(callback);
    }

    public void getUserOrders(String userId, Callback<List<Order>> callback) {
        apiService.getUserOrders(apiKey, requireUserAuth(), "eq." + userId, "created_at.desc").enqueue(callback);
    }
    
    public void fetchLiveProducts(String category, int limit, int offset, Callback<List<Product>> callback) {
        getProductsSorted(category, null, "sale_rate.asc", limit, offset, callback);
    }
    
    public void getAppConfig(Callback<List<AppConfig>> callback) {
        apiService.getAppConfig(apiKey, anonOrUserAuth(), 1).enqueue(callback);
    }

    public void getLiveBanners(Callback<List<Banner>> callback) {
        apiService.getBanners(apiKey, anonOrUserAuth(), "eq.true").enqueue(callback);
    }

    public void getUserAddresses(String userId, Callback<List<Address>> callback) {
        apiService.getUserAddresses(apiKey, requireUserAuth(), "eq." + userId).enqueue(callback);
    }

    public void addAddress(Map<String, Object> addressData, Callback<Void> callback) {
        apiService.addAddress(apiKey, requireUserAuth(), addressData).enqueue(callback);
    }

    public void updateAddress(String addressId, Map<String, Object> addressData, Callback<Void> callback) {
        apiService.updateAddress(apiKey, requireUserAuth(), "eq." + addressId, addressData).enqueue(callback);
    }

    public void deleteAddress(String addressId, Callback<Void> callback) {
        apiService.deleteAddress(apiKey, requireUserAuth(), "eq." + addressId).enqueue(callback);
    }

    public void getWallets(Callback<List<WalletMaster>> callback) {
        apiService.getWallets(apiKey, requireUserAuth()).enqueue(callback);
    }

    public void getWalletTransactions(String userId, Callback<List<WalletTransaction>> callback) {
        apiService.getWalletTransactions(apiKey, requireUserAuth(), "eq." + userId).enqueue(callback);
    }

    public void getPincodes(Callback<List<PincodeMaster>> callback) {
        apiService.getPincodes(apiKey, anonOrUserAuth()).enqueue(callback);
    }
    
    public void getCategories(Callback<List<Category>> callback) {
        apiService.getCategories(apiKey, anonOrUserAuth()).enqueue(callback);
    }

    public void validateAndApplyCoupon(String code, double cartTotal, Callback<List<CouponValidationResult>> callback) {
        Map<String, Object> body = new HashMap<>();
        body.put("p_code", code);
        body.put("p_cart_total", cartTotal);
        apiService.validateAndApplyCoupon(apiKey, requireUserAuth(), body).enqueue(callback);
    }

    public void getCoupons(Callback<List<com.nmmart.retailos.models.Coupon>> callback) {
        apiService.getCoupons(apiKey, requireUserAuth()).enqueue(callback);
    }

    public void updateOrderStatus(String orderId, Map<String, Object> data, Callback<Void> callback) {
        apiService.updateOrder(apiKey, requireUserAuth(), "eq." + orderId, data).enqueue(callback);
    }
}
