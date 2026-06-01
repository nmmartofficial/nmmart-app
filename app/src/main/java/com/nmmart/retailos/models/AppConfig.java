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
    
    @SerializedName("delivery_charge")
    public double deliveryCharge;
    
    @SerializedName("is_service_active")
    public boolean isServiceActive;
}