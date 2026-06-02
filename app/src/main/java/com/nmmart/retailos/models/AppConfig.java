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
}