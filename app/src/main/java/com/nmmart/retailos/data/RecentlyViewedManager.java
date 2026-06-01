package com.nmmart.retailos.data;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.nmmart.retailos.models.Product;
import java.util.ArrayList;
import java.util.List;

public class RecentlyViewedManager {
    private static final String PREF_NAME = "RecentPrefs";
    private static final String KEY_RECENT_ITEMS = "recentItems";
    private static final int MAX_ITEMS = 10;
    private static RecentlyViewedManager instance;
    private final SharedPreferences prefs;
    private final Gson gson;
    private List<Product> recentList;

    private RecentlyViewedManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
        String json = prefs.getString(KEY_RECENT_ITEMS, "[]");
        recentList = gson.fromJson(json, new TypeToken<List<Product>>(){}.getType());
        if (recentList == null) recentList = new ArrayList<>();
    }

    public static synchronized RecentlyViewedManager getInstance(Context context) {
        if (instance == null) instance = new RecentlyViewedManager(context.getApplicationContext());
        return instance;
    }

    public void addProduct(Product product) {
        // Remove if already exists to move to top
        for (int i = 0; i < recentList.size(); i++) {
            if (recentList.get(i).id.equals(product.id)) {
                recentList.remove(i);
                break;
            }
        }
        
        // Add to top
        recentList.add(0, product);
        
        // Keep max limit
        if (recentList.size() > MAX_ITEMS) {
            recentList.remove(recentList.size() - 1);
        }
        
        save();
    }

    public List<Product> getRecentProducts() {
        return recentList;
    }

    private void save() {
        prefs.edit().putString(KEY_RECENT_ITEMS, gson.toJson(recentList)).apply();
    }
}