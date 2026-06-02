package com.nmmart.retailos.data;

import com.google.gson.annotations.SerializedName;
import com.nmmart.retailos.BuildConfig;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public class SupabaseAuthConfig {

    private static final String BASE_URL = BuildConfig.SUPABASE_AUTH_URL;
    private static final String API_KEY = BuildConfig.SUPABASE_KEY;

    private static Retrofit retrofit = null;

    public static Retrofit getClient() {
        if (retrofit == null) {
            HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
            interceptor.setLevel(BuildConfig.DEBUG ? HttpLoggingInterceptor.Level.BODY : HttpLoggingInterceptor.Level.NONE);
            OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor).build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build();
        }
        return retrofit;
    }

    public static SupabaseAuthService getService() {
        return getClient().create(SupabaseAuthService.class);
    }

    public static String getApiKey() {
        return API_KEY;
    }

    public interface SupabaseAuthService {
        @POST("otp")
        Call<Void> requestOtp(
                @Header("apikey") String apiKey,
                @Body OtpRequest body
        );

        @POST("verify")
        Call<AuthSessionResponse> verifyOtp(
                @Header("apikey") String apiKey,
                @Body VerifyRequest body
        );

        @POST("otp")
        Call<Void> requestEmailOtp(
                @Header("apikey") String apiKey,
                @Body EmailOtpRequest body
        );

        @POST("verify")
        Call<AuthSessionResponse> verifyEmailOtp(
                @Header("apikey") String apiKey,
                @Body VerifyEmailRequest body
        );
    }

    public static class OtpRequest {
        @SerializedName("phone")
        public final String phone;

        @SerializedName("create_user")
        public final boolean createUser;

        public OtpRequest(String phone, boolean createUser) {
            this.phone = phone;
            this.createUser = createUser;
        }
    }

    public static class EmailOtpRequest {
        @SerializedName("email")
        public final String email;

        @SerializedName("create_user")
        public final boolean createUser;

        public EmailOtpRequest(String email, boolean createUser) {
            this.email = email;
            this.createUser = createUser;
        }
    }

    public static class VerifyRequest {
        @SerializedName("type")
        public final String type;

        @SerializedName("phone")
        public final String phone;

        @SerializedName("token")
        public final String token;

        public VerifyRequest(String type, String phone, String token) {
            this.type = type;
            this.phone = phone;
            this.token = token;
        }
    }

    public static class VerifyEmailRequest {
        @SerializedName("type")
        public final String type;

        @SerializedName("email")
        public final String email;

        @SerializedName("token")
        public final String token;

        public VerifyEmailRequest(String type, String email, String token) {
            this.type = type;
            this.email = email;
            this.token = token;
        }
    }

    public static class AuthSessionResponse {
        @SerializedName("access_token")
        public String accessToken;

        @SerializedName("token_type")
        public String tokenType;

        @SerializedName("expires_in")
        public long expiresInSec;

        @SerializedName("refresh_token")
        public String refreshToken;

        @SerializedName("user")
        public User user;
    }

    public static class User {
        @SerializedName("id")
        public String id;

        @SerializedName("phone")
        public String phone;

        @SerializedName("email")
        public String email;
    }
}

