package com.nmmart.retailos.data;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.nmmart.retailos.models.Product;
import java.util.HashMap;
import java.util.Map;

public class WishlistManager {
    private static final String PREF_NAME = "WishlistPrefs";
    private static final String KEY_WISHLIST_ITEMS = "wishlistItems";
    private static WishlistManager instance;
    private final SharedPreferences prefs;
    private final Gson gson;
    private final Map<String, Product> wishlistItems;

    private WishlistManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
        String json = prefs.getString(KEY_WISHLIST_ITEMS, "{}");
        wishlistItems = gson.fromJson(json, new TypeToken<Map<String, Product>>(){}.getType());
    }

    public static synchronized WishlistManager getInstance(Context context) {
        if (instance == null) instance = new WishlistManager(context.getApplicationContext());
        return instance;
    }

    public void toggleWishlist(Product product) {
        if (wishlistItems.containsKey(product.id)) {
            wishlistItems.remove(product.id);
        } else {
            wishlistItems.put(product.id, product);
        }
        save();
    }

    public boolean isInWishlist(String productId) {
        return wishlistItems.containsKey(productId);
    }

    public Map<String, Product> getWishlistItems() {
        return wishlistItems;
    }

    private void save() {
        prefs.edit().putString(KEY_WISHLIST_ITEMS, gson.toJson(wishlistItems)).apply();
    }
}