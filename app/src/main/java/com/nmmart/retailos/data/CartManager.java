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

public class CartManager {
    private static CartManager instance;
    private Map<String, Product> cartItems;
    private Map<String, Integer> cartQuantities;
    private SharedPreferences sharedPreferences;
    private Gson gson;
    private static final String PREF_NAME = "CartPrefs";
    private static final String KEY_CART_ITEMS = "cartItems";
    private static final String KEY_CART_QUANTITIES = "cartQuantities";
    
    private double minFreeDeliveryAmount = 500.0;
    private double deliveryCharge = 40.0;
    private double minOrderCheckout = 499.0;
    private double handlingCharge = 5.0;
    private double cashbackPercentage = 2.0;

    private CartManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
        loadCartFromPrefs();
        loadConfigFromPrefs();
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

    private void saveCartToPrefs() {
        sharedPreferences.edit()
                .putString(KEY_CART_ITEMS, gson.toJson(cartItems))
                .putString(KEY_CART_QUANTITIES, gson.toJson(cartQuantities))
                .apply();
    }

    public boolean addToCart(Product product) {
        if (product == null || product.id == null) return false;
        int currentQty = cartQuantities.getOrDefault(product.id, 0);
        if (product.getStock() > 0 && currentQty >= product.getStock()) return false;
        cartItems.put(product.id, product);
        cartQuantities.put(product.id, currentQty + 1);
        saveCartToPrefs();
        return true;
    }

    public void removeFromCart(Product product) {
        if (product == null || product.id == null) return;
        int currentQty = cartQuantities.getOrDefault(product.id, 0);
        if (currentQty > 1) {
            cartQuantities.put(product.id, currentQty - 1);
        } else {
            cartItems.remove(product.id);
            cartQuantities.remove(product.id);
        }
        saveCartToPrefs();
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

    public int getQuantity(String productId) { return cartQuantities.getOrDefault(productId, 0); }
    public List<Product> getCartItems() { return new ArrayList<>(cartItems.values()); }
    public Map<String, Integer> getCartQuantities() { return new HashMap<>(cartQuantities); }
    public void clearCart() { cartItems.clear(); cartQuantities.clear(); saveCartToPrefs(); }
    public int getCartCount() {
        int count = 0;
        for (int qty : cartQuantities.values()) count += qty;
        return count;
    }
}
