package com.nmmart.retailos.data;

import android.content.Context;

import com.nmmart.retailos.BuildConfig;
import com.nmmart.retailos.models.Address;
import com.nmmart.retailos.models.AppConfig;
import com.nmmart.retailos.models.Banner;
import com.nmmart.retailos.models.Brand;
import com.nmmart.retailos.models.Category;
import com.nmmart.retailos.models.Coupon;
import com.nmmart.retailos.models.HomeConfig;
import com.nmmart.retailos.models.Order;
import com.nmmart.retailos.models.Product;
import com.nmmart.retailos.models.WalletMaster;
import com.nmmart.retailos.models.WalletTransaction;
import com.nmmart.retailos.models.PincodeMaster;
import com.nmmart.retailos.data.CouponValidationResult;

import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import okhttp3.Authenticator;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.Route;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Query;

public class SupabaseConfig {

    // Supabase Credentials from BuildConfig
    private static final String BASE_URL = BuildConfig.SUPABASE_URL;
    private static final String API_KEY = BuildConfig.SUPABASE_KEY;
    private static final String AUTH_URL = BuildConfig.SUPABASE_AUTH_URL;

    private static Retrofit retrofit = null;
    private static Context appContext = null;

    public static void init(Context context) {
        if (context == null) return;
        appContext = context.getApplicationContext();
    }

    public static Retrofit getClient() {
        if (retrofit == null) {
            HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
            interceptor.setLevel(BuildConfig.DEBUG ? HttpLoggingInterceptor.Level.BODY : HttpLoggingInterceptor.Level.NONE);
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(interceptor)
                    .authenticator(new SupabaseTokenAuthenticator())
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build();
        }
        return retrofit;
    }

    public interface SupabaseService {
        @GET("products")
        Call<List<Product>> searchProducts(
            @Header("apikey") String apiKey,
            @Header("Authorization") String auth,
            @Query("name") String nameQuery,
            @Query("limit") int limit,
            @Query("offset") int offset
        );

        @GET("products")
        Call<List<Product>> getProductsSorted(
            @Header("apikey") String apiKey,
            @Header("Authorization") String auth,
            @Query("category") String category,
            @Query("brand") String brand,
            @Query("order") String order,
            @Query("limit") int limit,
            @Query("offset") int offset
        );

        @GET("products")
        Call<List<Product>> getRelatedProducts(
            @Header("apikey") String apiKey,
            @Header("Authorization") String auth,
            @Query("category") String category,
            @Query("id") String notEqualId,
            @Query("limit") int limit
        );

        @GET("home_config")
        Call<List<HomeConfig>> getHomeConfig(
            @Header("apikey") String apiKey,
            @Header("Authorization") String auth
        );

        @GET("products?is_trending=eq.true")
        Call<List<Product>> getTrendingProducts(
            @Header("apikey") String apiKey,
            @Header("Authorization") String auth,
            @Query("order") String order,
            @Query("limit") int limit
        );

        @GET("products")
        Call<List<Product>> getLatestProducts(
            @Header("apikey") String apiKey,
            @Header("Authorization") String auth,
            @Query("order") String order,
            @Query("limit") int limit
        );

        @GET("products")
        Call<List<Product>> getProducts(
            @Header("apikey") String apiKey,
            @Header("Authorization") String auth,
            @Query("category") String category
        );

        @GET("products")
        Call<List<Product>> getAllProducts(
            @Header("apikey") String apiKey,
            @Header("Authorization") String auth,
            @Query("limit") int limit,
            @Query("offset") int offset
        );

        @GET("orders")
        Call<List<Order>> getLiveOrders(
            @Header("apikey") String apiKey,
            @Header("Authorization") String auth,
            @Query("user_mobile") String mobile,
            @Query("status") String status
        );

        @GET("orders")
        Call<List<Order>> getUserOrders(
            @Header("apikey") String apiKey,
            @Header("Authorization") String auth,
            @Query("user_id") String userId,
            @Query("order") String orderBy
        );

        @POST("users")
        Call<Void> insertUser(
            @Header("apikey") String apiKey,
            @Header("Authorization") String auth,
            @Body Map<String, String> userData
        );

        @POST("orders")
        Call<Void> placeOrder(
            @Header("apikey") String apiKey,
            @Header("Authorization") String auth,
            @Body Map<String, Object> orderData
        );
        
        @GET("app_config")
        Call<List<AppConfig>> getAppConfig(
            @Header("apikey") String apiKey,
            @Header("Authorization") String auth,
            @Query("limit") int limit
        );

        @GET("banners?is_active=eq.true&order=position.asc")
        Call<List<Banner>> getBanners(
            @Header("apikey") String apiKey,
            @Header("Authorization") String auth
        );

        @GET("coupons?is_active=eq.true")
        Call<List<Coupon>> getCoupons(
            @Header("apikey") String apiKey,
            @Header("Authorization") String auth
        );

        @GET("addresses")
        Call<List<Address>> getUserAddresses(
            @Header("apikey") String apiKey,
            @Header("Authorization") String auth,
            @Query("user_id") String userId
        );

        @POST("addresses")
        Call<Void> addAddress(
            @Header("apikey") String apiKey,
            @Header("Authorization") String auth,
            @Body Map<String, Object> addressData
        );

        @PATCH("addresses")
        Call<Void> updateAddress(
            @Header("apikey") String apiKey,
            @Header("Authorization") String auth,
            @Query("id") String addressId,
            @Body Map<String, Object> addressData
        );

        @DELETE("addresses")
        Call<Void> deleteAddress(
            @Header("apikey") String apiKey,
            @Header("Authorization") String auth,
            @Query("id") String addressId
        );

        @GET("wallet_master")
        Call<List<WalletMaster>> getWallets(
            @Header("apikey") String apiKey,
            @Header("Authorization") String auth
        );

        @GET("wallet_transactions")
        Call<List<WalletTransaction>> getWalletTransactions(
            @Header("apikey") String apiKey,
            @Header("Authorization") String auth,
            @Query("user_id") String userId
        );

        @GET("pincode_master")
        Call<List<PincodeMaster>> getPincodes(
            @Header("apikey") String apiKey,
            @Header("Authorization") String auth
        );
        
        @GET("categories?is_active=eq.true&order=position.asc")
        Call<List<Category>> getCategories(
            @Header("apikey") String apiKey,
            @Header("Authorization") String auth
        );
        
        @GET("brands?is_active=eq.true&order=position.asc")
        Call<List<Brand>> getBrands(
            @Header("apikey") String apiKey,
            @Header("Authorization") String auth
        );

        @POST("rpc/validate_and_apply_coupon")
        Call<List<CouponValidationResult>> validateAndApplyCoupon(
            @Header("apikey") String apiKey,
            @Header("Authorization") String auth,
            @Body Map<String, Object> body
        );

        @PATCH("orders")
        Call<Void> updateOrder(
            @Header("apikey") String apiKey,
            @Header("Authorization") String auth,
            @Query("id") String orderId,
            @Body Map<String, Object> data
        );
    }

    public static SupabaseService getService() {
        return getClient().create(SupabaseService.class);
    }

    public static String getApiKey() {
        return API_KEY;
    }

    public static String getAuthorizationHeader() {
        String accessToken = getAccessTokenOrEmpty();
        String authHeader;
        if (accessToken.isEmpty()) {
            authHeader = "Bearer " + API_KEY;
            android.util.Log.d("SupabaseConfig", "Using anon key for auth");
        } else {
            // Quick sanity check: is this a valid JWT? (should have 3 parts separated by dots)
            if (accessToken.split("\\.").length != 3) {
                android.util.Log.e("SupabaseConfig", "Invalid JWT token found! Clearing session!");
                // Clear invalid session
                if (appContext != null) {
                    new SessionManager(appContext).logout();
                }
                authHeader = "Bearer " + API_KEY;
            } else {
                authHeader = "Bearer " + accessToken;
                android.util.Log.d("SupabaseConfig", "Using user access token");
            }
        }
        android.util.Log.d("SupabaseConfig", "Auth Header: " + (authHeader.length() > 50 ? authHeader.substring(0, 50) + "..." : authHeader));
        return authHeader;
    }

    public static String getUserAuthorizationHeaderOrEmpty() {
        String accessToken = getAccessTokenOrEmpty();
        if (accessToken.isEmpty()) return "";
        return "Bearer " + accessToken;
    }

    private static String getAccessTokenOrEmpty() {
        try {
            if (appContext == null) return "";
            SessionManager sm = new SessionManager(appContext);
            return sm.getAccessToken();
        } catch (Exception e) {
            return "";
        }
    }

    private static class SupabaseTokenAuthenticator implements Authenticator {
        private final OkHttpClient refreshClient;
        private final MediaType jsonMediaType;

        SupabaseTokenAuthenticator() {
            this.refreshClient = new OkHttpClient.Builder().build();
            this.jsonMediaType = MediaType.parse("application/json; charset=utf-8");
        }

        @Override
        public Request authenticate(Route route, Response response) {
            try {
                if (appContext == null) return null;
                if (responseCount(response) >= 2) return null;

                SessionManager sm = new SessionManager(appContext);
                String refreshToken = sm.getRefreshToken();
                if (refreshToken == null || refreshToken.isEmpty()) return null;

                JSONObject bodyJson = new JSONObject();
                bodyJson.put("refresh_token", refreshToken);
                RequestBody reqBody = RequestBody.create(bodyJson.toString(), jsonMediaType);

                String url = AUTH_URL + "token?grant_type=refresh_token";
                Request refreshRequest = new Request.Builder()
                        .url(url)
                        .post(reqBody)
                        .header("apikey", API_KEY)
                        .build();

                okhttp3.Response refreshResp = refreshClient.newCall(refreshRequest).execute();
                if (!refreshResp.isSuccessful() || refreshResp.body() == null) return null;

                String raw = refreshResp.body().string();
                JSONObject json = new JSONObject(raw);

                String newAccess = json.optString("access_token", "");
                String newRefresh = json.optString("refresh_token", "");
                long expiresIn = json.optLong("expires_in", 0L);

                if (newAccess.isEmpty()) return null;

                long now = System.currentTimeMillis() / 1000L;
                long expiresAt = now + Math.max(0L, expiresIn - 30L);

                sm.updateAuthTokens(newAccess, newRefresh.isEmpty() ? refreshToken : newRefresh, expiresAt);

                return response.request().newBuilder()
                        .header("Authorization", "Bearer " + newAccess)
                        .build();
            } catch (Exception e) {
                return null;
            }
        }

        private static int responseCount(Response response) {
            int count = 1;
            while ((response = response.priorResponse()) != null) {
                count++;
            }
            return count;
        }
    }
}
