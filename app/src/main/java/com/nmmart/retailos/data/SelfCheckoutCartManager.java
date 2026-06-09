package com.nmmart.retailos.data;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.nmmart.retailos.models.Product;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SelfCheckoutCartManager {
    private static SelfCheckoutCartManager instance;
    private Map<String, Product> cartItems;
    private Map<String, Integer> cartQuantities;
    private SharedPreferences sharedPreferences;
    private Gson gson;
    private static final String PREF_NAME = "SelfCheckoutCartPrefs";
    private static final String KEY_CART_ITEMS = "cartItems";
    private static final String KEY_CART_QUANTITIES = "cartQuantities";

    private SelfCheckoutCartManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
        loadCartFromPrefs();
    }

    public static synchronized SelfCheckoutCartManager getInstance(Context context) {
        if (instance == null) instance = new SelfCheckoutCartManager(context.getApplicationContext());
        return instance;
    }

    private void loadCartFromPrefs() {
        String itemsJson = sharedPreferences.getString(KEY_CART_ITEMS, null);
        String quantitiesJson = sharedPreferences.getString(KEY_CART_QUANTITIES, null);
        
        if (itemsJson != null && quantitiesJson != null) {
            try {
                Type productType = new TypeToken<HashMap<String, Product>>() {}.getType();
                Type quantityType = new TypeToken<HashMap<String, Integer>>() {}.getType();
                cartItems = gson.fromJson(itemsJson, productType);
                cartQuantities = gson.fromJson(quantitiesJson, quantityType);
            } catch (Exception e) {
                cartItems = new HashMap<>();
                cartQuantities = new HashMap<>();
            }
        } else {
            cartItems = new HashMap<>();
            cartQuantities = new HashMap<>();
        }
        if (cartItems == null) cartItems = new HashMap<>();
        if (cartQuantities == null) cartQuantities = new HashMap<>();
    }

    private String getCartKey(Product product) {
        if (product == null) return "";
        String unit = product.getUnit() != null ? product.getUnit() : "default";
        return product.id + "_" + unit;
    }

    private String getCartKey(String productId, String unit) {
        if (productId == null) return "";
        String safeUnit = unit != null ? unit : "default";
        return productId + "_" + safeUnit;
    }

    private void saveCartToPrefs() {
        sharedPreferences.edit()
                .putString(KEY_CART_ITEMS, gson.toJson(cartItems))
                .putString(KEY_CART_QUANTITIES, gson.toJson(cartQuantities))
                .apply();
    }

    public boolean addItem(Product product) {
        if (product == null || product.id == null) return false;
        String key = getCartKey(product);
        int currentQty = cartQuantities.getOrDefault(key, 0);
        if (product.getStock() > 0 && currentQty >= product.getStock()) return false;
        cartItems.put(key, product);
        cartQuantities.put(key, currentQty + 1);
        saveCartToPrefs();
        return true;
    }

    public void removeItem(String key) {
        if (key == null) return;
        cartItems.remove(key);
        cartQuantities.remove(key);
        saveCartToPrefs();
    }

    public void removeItem(Product product) {
        if (product == null || product.id == null) return;
        removeItem(getCartKey(product));
    }

    public void updateQuantity(String key, int newQuantity) {
        if (key == null) return;
        if (newQuantity <= 0) {
            removeItem(key);
        } else {
            cartQuantities.put(key, newQuantity);
            saveCartToPrefs();
        }
    }

    public void updateQuantity(Product product, int newQuantity) {
        if (product == null || product.id == null) return;
        updateQuantity(getCartKey(product), newQuantity);
    }

    public int getQuantity(String productId) { 
        return cartQuantities.getOrDefault(productId, 0); 
    }

    public int getQuantity(Product product) {
        return getQuantity(getCartKey(product));
    }

    public double getTotalPrice() {
        double total = 0;
        for (String id : cartItems.keySet()) {
            Product p = cartItems.get(id);
            if (p != null) {
                total += p.getNmPrice() * cartQuantities.getOrDefault(id, 0);
            }
        }
        return total;
    }

    public List<Product> getCartItems() { 
        return new ArrayList<>(cartItems.values()); 
    }
    
    public Map<String, Integer> getCartQuantities() { 
        return new HashMap<>(cartQuantities); 
    }
    
    public void clearCart() { 
        cartItems.clear(); 
        cartQuantities.clear(); 
        saveCartToPrefs(); 
    }
    
    public int getCartCount() {
        int count = 0;
        for (int qty : cartQuantities.values()) count += qty;
        return count;
    }
}
