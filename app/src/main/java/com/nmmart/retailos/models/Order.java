package com.nmmart.retailos.models;

import com.google.gson.annotations.SerializedName;

public class Order {
    public String id;
    public String status;
    
    @SerializedName("created_at")
    public String createdAt;
    
    @SerializedName("total_amount")
    public double totalAmount;
    
    @SerializedName("items_summary")
    public String itemsSummary;
    
    @SerializedName("expected_delivery")
    public String expected_delivery;
}