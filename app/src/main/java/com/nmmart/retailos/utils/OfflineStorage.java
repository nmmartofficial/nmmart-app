package com.nmmart.retailos.utils;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.nmmart.retailos.models.Banner;
import com.nmmart.retailos.models.Category;
import com.nmmart.retailos.models.Product;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class OfflineStorage {
    private static final String PREF_NAME = "nm_mart_offline";
    private static final String KEY_PRODUCTS = "products_";
    private static final String KEY_CATEGORIES = "categories";
    private static final String KEY_BANNERS = "banners";
    private static final String KEY_SUBCATEGORIES = "subcats_";
    private static OfflineStorage instance;
    private SharedPreferences prefs;
    private Gson gson;

    private OfflineStorage(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public static synchronized OfflineStorage getInstance(Context context) {
        if (instance == null) {
            instance = new OfflineStorage(context.getApplicationContext());
        }
        return instance;
    }

    public void saveProducts(String key, List<Product> products) {
        String json = gson.toJson(products);
        prefs.edit().putString(KEY_PRODUCTS + key, json).apply();
    }

    public List<Product> getProducts(String key) {
        String json = prefs.getString(KEY_PRODUCTS + key, null);
        Type type = new TypeToken<List<Product>>() {}.getType();
        List<Product> products = gson.fromJson(json, type);
        return products != null ? products : new ArrayList<>();
    }

    public void saveCategories(List<Category> categories) {
        String json = gson.toJson(categories);
        prefs.edit().putString(KEY_CATEGORIES, json).apply();
    }

    public List<Category> getCategories() {
        String json = prefs.getString(KEY_CATEGORIES, null);
        Type type = new TypeToken<List<Category>>() {}.getType();
        List<Category> categories = gson.fromJson(json, type);
        return categories != null ? categories : new ArrayList<>();
    }
    
    public void saveBanners(List<Banner> banners) {
        String json = gson.toJson(banners);
        prefs.edit().putString(KEY_BANNERS, json).apply();
    }

    public List<Banner> getBanners() {
        String json = prefs.getString(KEY_BANNERS, null);
        Type type = new TypeToken<List<Banner>>() {}.getType();
        List<Banner> banners = gson.fromJson(json, type);
        return banners != null ? banners : new ArrayList<>();
    }
    
    public void saveSubcategories(String parentId, List<Category> subcategories) {
        String json = gson.toJson(subcategories);
        prefs.edit().putString(KEY_SUBCATEGORIES + parentId, json).apply();
    }
    
    public List<Category> getSubcategories(String parentId) {
        String json = prefs.getString(KEY_SUBCATEGORIES + parentId, null);
        Type type = new TypeToken<List<Category>>() {}.getType();
        List<Category> categories = gson.fromJson(json, type);
        return categories != null ? categories : new ArrayList<>();
    }

    public void clearAll() {
        prefs.edit().clear().apply();
    }
}
