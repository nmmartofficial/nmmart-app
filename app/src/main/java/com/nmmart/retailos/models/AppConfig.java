package com.nmmart.retailos.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class AppConfig implements Serializable {
    @SerializedName("id")
    public String id;
    
    @SerializedName("delivery_time_msg")
    public String deliveryTimeMsg;
    
    @SerializedName("min_order_free_delivery")
    public double minOrderFreeDelivery;
    
    @SerializedName("min_order_checkout")
    public double minOrderCheckout;
    
    @SerializedName("delivery_charge")
    public double deliveryCharge;
    
    @SerializedName("handling_charge")
    public double handlingCharge;
    
    @SerializedName("cashback_percentage")
    public double cashbackPercentage;
    
    @SerializedName("is_service_active")
    public boolean isServiceActive;

    @SerializedName("store_logo_url")
    public String storeLogoUrl;

    // THEME & COLORS
    @SerializedName("primary_color")
    public String primaryColor; // e.g., "#FF5722" (deep orange)
    
    @SerializedName("secondary_color")
    public String secondaryColor; // e.g., "#FF9800" (orange)
    
    @SerializedName("accent_color")
    public String accentColor; // e.g., "#4CAF50" (green)
    
    @SerializedName("background_color")
    public String backgroundColor; // e.g., "#FFFFFF" (white)
    
    @SerializedName("text_color_primary")
    public String textColorPrimary; // e.g., "#212121" (dark gray)
    
    @SerializedName("text_color_secondary")
    public String textColorSecondary; // e.g., "#757575" (light gray)

    // SHAPES
    @SerializedName("category_shape")
    public String categoryShape; // "circle", "square", "rounded_square"
    
    @SerializedName("brand_shape")
    public String brandShape; // "circle", "square", "rounded_square"

    // BANNER
    @SerializedName("banner_style")
    public String bannerStyle; // "default", "card", "minimal"
}