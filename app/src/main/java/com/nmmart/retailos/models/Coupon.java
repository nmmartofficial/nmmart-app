package com.nmmart.retailos.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class Coupon implements Serializable {
    public String id;
    
    @SerializedName("code")
    public String code;
    
    @SerializedName("description")
    public String description;
    
    @SerializedName("discount_amount")
    public double discountAmount;
    
    @SerializedName("min_order_value")
    public double minOrderValue;
}