package com.nmmart.retailos.ui.viewmodels;

import android.content.Context;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.nmmart.retailos.data.CartManager;
import com.nmmart.retailos.models.Product;

import java.util.List;

public class CartViewModel extends ViewModel {
    private MutableLiveData<List<Product>> cartItems = new MutableLiveData<>();
    private MutableLiveData<Double> totalPrice = new MutableLiveData<>();
    private CartManager cartManager;

    public void init(Context context) {
        if (cartManager == null) {
            cartManager = CartManager.getInstance(context);
            updateCartData();
        }
    }

    public LiveData<List<Product>> getCartItems() { return cartItems; }
    public LiveData<Double> getTotalPrice() { return totalPrice; }

    public void updateCartData() {
        cartItems.setValue(cartManager.getCartItems());
        totalPrice.setValue(cartManager.getTotalPrice());
    }

    public void addToCart(Product product) {
        cartManager.addToCart(product);
        updateCartData();
    }

    public void removeFromCart(Product product) {
        cartManager.removeFromCart(product);
        updateCartData();
    }

    public void clearCart() {
        cartManager.clearCart();
        updateCartData();
    }
}
