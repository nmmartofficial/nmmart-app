package com.nmmart.retailos.models;

import com.google.gson.annotations.SerializedName;

public class Order {
    public String id;
    
    @SerializedName("order_number")
    public String orderNumber;
    
    @SerializedName("user_id")
    public String userId;
    
    @SerializedName("customer_name")
    public String customerName;
    
    @SerializedName("user_mobile")
    public String userMobile;
    
    public String address;
    public String pincode;
    public Double subtotal;
    
    @SerializedName("delivery_charge")
    public Double deliveryCharge;
    
    public Double discount;
    
    @SerializedName("total_amount")
    public Double totalAmount;
    
    @SerializedName("payment_mode")
    public String paymentMode;
    
    @SerializedName("payment_status")
    public String paymentStatus;
    
    @SerializedName("order_status")
    public String orderStatus;
    
    @SerializedName("delivery_boy_id")
    public String deliveryBoyId;
    
    @SerializedName("created_at")
    public String createdAt;
    
    @SerializedName("updated_at")
    public String updatedAt;

    // Backward compatibility fields
    public String status;
    @SerializedName("items_summary")
    public String itemsSummary;
    @SerializedName("expected_delivery")
    public String expectedDelivery;
}