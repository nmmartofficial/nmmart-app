package com.nmmart.retailos.models;

import com.google.gson.annotations.SerializedName;

public class OrderItem {
    public String id;
    
    @SerializedName("order_id")
    public String orderId;
    
    @SerializedName("product_id")
    public String productId;
    
    @SerializedName("product_name")
    public String productName;
    
    public Double quantity;
    public Double rate;
    public Double total;
    
    @SerializedName("created_at")
    public String createdAt;
}