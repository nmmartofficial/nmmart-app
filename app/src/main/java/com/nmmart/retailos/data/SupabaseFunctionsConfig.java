package com.nmmart.retailos.data;

import com.google.gson.annotations.SerializedName;
import com.nmmart.retailos.BuildConfig;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Header;

public class SupabaseFunctionsConfig {

    private static final String BASE_URL = BuildConfig.SUPABASE_FUNCTIONS_URL;

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

    public static RoleService getRoleService() {
        return getClient().create(RoleService.class);
    }

    public interface RoleService {
        @GET("is_admin")
        Call<IsAdminResponse> isAdmin(@Header("Authorization") String bearerToken);
    }

    public static class IsAdminResponse {
        @SerializedName("is_admin")
        public boolean isAdmin;
    }
}

