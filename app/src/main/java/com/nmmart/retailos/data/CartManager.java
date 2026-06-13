package com.nmmart.retailos.data;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.nmmart.retailos.models.Product;
import java.io.IOException;
import java.lang.reflect.Type;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CartManager {
    private static CartManager instance;
    private Map<String, Product> cartItems;
    private Map<String, Integer> cartQuantities;
    private Map<String, Product> savedItems;
    private Map<String, Integer> savedQuantities;
    private SharedPreferences sharedPreferences;
    private Gson gson;
    private static final String PREF_NAME = "CartPrefs";
    private static final String KEY_CART_ITEMS = "cartItems";
    private static final String KEY_CART_QUANTITIES = "cartQuantities";
    private static final String KEY_SAVED_ITEMS = "savedItems";
    private static final String KEY_SAVED_QUANTITIES = "savedQuantities";
    
    private double minFreeDeliveryAmount = 500.0;
    private double deliveryCharge = 40.0;
    private double minOrderCheckout = 499.0;
    private double handlingCharge = 5.0;
    private double cashbackPercentage = 2.0;

    private CartManager(Context context) {
        this.sharedPreferences = getEncryptedSharedPreferences(context);
        this.gson = new Gson();
        loadCartFromPrefs();
        loadConfigFromPrefs();
    }

    private SharedPreferences getEncryptedSharedPreferences(Context context) {
        try {
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);

            return EncryptedSharedPreferences.create(
                    PREF_NAME,
                    masterKeyAlias,
                    context,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            // Fallback to regular SharedPreferences if encryption fails
            return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        }
    }

    private void loadConfigFromPrefs() {
        minFreeDeliveryAmount = sharedPreferences.getFloat("minFreeDelivery", 500.0f);
        deliveryCharge = sharedPreferences.getFloat("deliveryCharge", 40.0f);
        minOrderCheckout = sharedPreferences.getFloat("minOrderCheckout", 499.0f);
        handlingCharge = sharedPreferences.getFloat("handlingCharge", 5.0f);
        cashbackPercentage = sharedPreferences.getFloat("cashbackPercentage", 2.0f);
    }

    public void updateAppConfig(double minFree, double charge, double minCheckout, double handling, double cashbackPercent) {
        this.minFreeDeliveryAmount = minFree;
        this.deliveryCharge = charge;
        this.minOrderCheckout = minCheckout;
        this.handlingCharge = handling;
        this.cashbackPercentage = cashbackPercent;
        sharedPreferences.edit()
                .putFloat("minFreeDelivery", (float) minFree)
                .putFloat("deliveryCharge", (float) charge)
                .putFloat("minOrderCheckout", (float) minCheckout)
                .putFloat("handlingCharge", (float) handling)
                .putFloat("cashbackPercentage", (float) cashbackPercent)
                .apply();
    }

    public double getMinOrderCheckout() { return minOrderCheckout; }
    public double getHandlingCharge() { return handlingCharge; }
    public double getCashbackPercentage() { return cashbackPercentage; }
    public double getMinFreeDeliveryAmount() { return minFreeDeliveryAmount; }

    public static synchronized CartManager getInstance(Context context) {
        if (instance == null) instance = new CartManager(context.getApplicationContext());
        return instance;
    }

    private void loadCartFromPrefs() {
        String itemsJson = sharedPreferences.getString(KEY_CART_ITEMS, null);
        String quantitiesJson = sharedPreferences.getString(KEY_CART_QUANTITIES, null);
        String savedItemsJson = sharedPreferences.getString(KEY_SAVED_ITEMS, null);
        String savedQuantitiesJson = sharedPreferences.getString(KEY_SAVED_QUANTITIES, null);
        
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
        
        if (savedItemsJson != null && savedQuantitiesJson != null) {
            try {
                Type productType = new TypeToken<HashMap<String, Product>>() {}.getType();
                Type quantityType = new TypeToken<HashMap<String, Integer>>() {}.getType();
                savedItems = gson.fromJson(savedItemsJson, productType);
                savedQuantities = gson.fromJson(savedQuantitiesJson, quantityType);
            } catch (Exception e) {
                savedItems = new HashMap<>();
                savedQuantities = new HashMap<>();
            }
        } else {
            savedItems = new HashMap<>();
            savedQuantities = new HashMap<>();
        }
        if (savedItems == null) savedItems = new HashMap<>();
        if (savedQuantities == null) savedQuantities = new HashMap<>();
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
                .putString(KEY_SAVED_ITEMS, gson.toJson(savedItems))
                .putString(KEY_SAVED_QUANTITIES, gson.toJson(savedQuantities))
                .apply();
    }
    
    public void saveForLater(Product product) {
        if (product == null || product.id == null) return;
        String key = getCartKey(product);
        int qty = cartQuantities.getOrDefault(key, 1);
        cartItems.remove(key);
        cartQuantities.remove(key);
        savedItems.put(key, product);
        savedQuantities.put(key, qty);
        saveCartToPrefs();
    }
    
    public void moveToCart(Product product) {
        if (product == null || product.id == null) return;
        String key = getCartKey(product);
        int qty = savedQuantities.getOrDefault(key, 1);
        savedItems.remove(key);
        savedQuantities.remove(key);
        cartItems.put(key, product);
        cartQuantities.put(key, qty);
        saveCartToPrefs();
    }
    
    public List<Product> getSavedItems() { return new ArrayList<>(savedItems.values()); }
    public int getSavedQuantity(String key) { return savedQuantities.getOrDefault(key, 0); }
    public int getSavedQuantity(Product product) { return getSavedQuantity(getCartKey(product)); }
    public int getQuantity(Product product) { 
        return cartQuantities.getOrDefault(getCartKey(product), 0); 
    }
    public int getQuantity(String key) { return cartQuantities.getOrDefault(key, 0); }

    public boolean addToCart(Product product) {
        if (product == null || product.id == null) return false;
        String key = getCartKey(product);
        int currentQty = cartQuantities.getOrDefault(key, 0);
        if (product.getStock() > 0 && currentQty >= product.getStock()) return false;
        cartItems.put(key, product);
        cartQuantities.put(key, currentQty + 1);
        saveCartToPrefs();
        return true;
    }

    public void removeFromCart(Product product) {
        if (product == null || product.id == null) return;
        removeFromCart(getCartKey(product));
    }

    public void removeFromCart(String key) {
        if (key == null) return;
        cartItems.remove(key);
        cartQuantities.remove(key);
        saveCartToPrefs();
    }

    public void updateQuantity(Product product, int newQuantity) {
        if (product == null || product.id == null) return;
        String key = getCartKey(product);
        if (newQuantity <= 0) {
            removeFromCart(key);
        } else {
            cartQuantities.put(key, newQuantity);
            saveCartToPrefs();
        }
    }

    public void updateQuantity(String key, int newQuantity) {
        if (key == null) return;
        if (newQuantity <= 0) {
            removeFromCart(key);
        } else {
            cartQuantities.put(key, newQuantity);
            saveCartToPrefs();
        }
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

    public double getDeliveryCharge() {
        double subtotal = getTotalPrice();
        return (subtotal == 0 || subtotal >= minFreeDeliveryAmount) ? 0 : deliveryCharge;
    }

    public List<Product> getCartItems() { return new ArrayList<>(cartItems.values()); }
    public Map<String, Integer> getCartQuantities() { return new HashMap<>(cartQuantities); }
    public void clearCart() { cartItems.clear(); cartQuantities.clear(); saveCartToPrefs(); }
    public int getCartCount() {
        int count = 0;
        for (int qty : cartQuantities.values()) count += qty;
        return count;
    }
}
