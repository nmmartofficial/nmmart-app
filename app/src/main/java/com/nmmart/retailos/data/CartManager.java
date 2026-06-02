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
    
    // Delivery Logic (Updated dynamically from AppConfig)
    private double minFreeDeliveryAmount = 500.0;
    private double deliveryCharge = 40.0;

    private CartManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
        loadCartFromPrefs();
        loadConfigFromPrefs();
    }

    private void loadConfigFromPrefs() {
        minFreeDeliveryAmount = sharedPreferences.getFloat("minFreeDelivery", 500.0f);
        deliveryCharge = sharedPreferences.getFloat("deliveryCharge", 40.0f);
    }

    public void updateDeliveryConfig(double minFree, double charge) {
        this.minFreeDeliveryAmount = minFree;
        this.deliveryCharge = charge;
        sharedPreferences.edit()
                .putFloat("minFreeDelivery", (float) minFree)
                .putFloat("deliveryCharge", (float) charge)
                .apply();
    }

    public static synchronized CartManager getInstance(Context context) {
        if (instance == null) {
            instance = new CartManager(context.getApplicationContext());
        }
        return instance;
    }

    private void loadCartFromPrefs() {
        String itemsJson = sharedPreferences.getString(KEY_CART_ITEMS, null);
        String quantitiesJson = sharedPreferences.getString(KEY_CART_QUANTITIES, null);

        if (itemsJson != null && quantitiesJson != null) {
            Type productType = new TypeToken<HashMap<String, Product>>() {}.getType();
            Type quantityType = new TypeToken<HashMap<String, Integer>>() {}.getType();
            cartItems = gson.fromJson(itemsJson, productType);
            cartQuantities = gson.fromJson(quantitiesJson, quantityType);
        } else {
            cartItems = new HashMap<>();
            cartQuantities = new HashMap<>();
        }

        if (cartItems == null) cartItems = new HashMap<>();
        if (cartQuantities == null) cartQuantities = new HashMap<>();
    }

    private void saveCartToPrefs() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        String itemsJson = gson.toJson(cartItems);
        String quantitiesJson = gson.toJson(cartQuantities);
        editor.putString(KEY_CART_ITEMS, itemsJson);
        editor.putString(KEY_CART_QUANTITIES, quantitiesJson);
        editor.apply();
    }

    public boolean addToCart(Product product) {
        String id = product.id;
        int currentQty = cartQuantities.getOrDefault(id, 0);
        int stock = product.getStock();

        if (stock > 0 && currentQty >= stock) {
            return false; // Cannot add more than available stock
        }

        if (cartItems.containsKey(id)) {
            cartQuantities.put(id, currentQty + 1);
        } else {
            cartItems.put(id, product);
            cartQuantities.put(id, 1);
        }
        saveCartToPrefs();
        return true;
    }

    public void removeFromCart(Product product) {
        String id = product.id;
        if (cartItems.containsKey(id)) {
            int qty = cartQuantities.get(id);
            if (qty > 1) {
                cartQuantities.put(id, qty - 1);
            } else {
                cartItems.remove(id);
                cartQuantities.remove(id);
            }
            saveCartToPrefs();
        }
    }

    public void removeCompletely(Product product) {
        cartItems.remove(product.id);
        cartQuantities.remove(product.id);
        saveCartToPrefs();
    }

    public List<Product> getCartItems() {
        return new ArrayList<>(cartItems.values());
    }

    public int getQuantity(String productId) {
        return cartQuantities.getOrDefault(productId, 0);
    }

    public Map<String, Integer> getCartQuantities() {
        return new HashMap<>(cartQuantities);
    }

    public double getTotalPrice() {
        double total = 0;
        for (String id : cartItems.keySet()) {
            total += cartItems.get(id).nm_price * cartQuantities.get(id);
        }
        return total;
    }

    public double getDeliveryCharge() {
        double subtotal = getTotalPrice();
        if (subtotal == 0 || subtotal >= minFreeDeliveryAmount) {
            return 0;
        }
        return deliveryCharge;
    }

    public double getGrandTotal() {
        return getTotalPrice() + getDeliveryCharge();
    }

    public double getMinFreeDeliveryAmount() {
        return minFreeDeliveryAmount;
    }

    public void clearCart() {
        cartItems.clear();
        cartQuantities.clear();
        saveCartToPrefs();
    }

    public int getCartCount() {
        int count = 0;
        for (int qty : cartQuantities.values()) {
            count += qty;
        }
        return count;
    }
}
