package com.nmmart.retailos.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class Address implements Serializable {
    @SerializedName("id")
    public String id;
    
    @SerializedName("user_id")
    public String userId;
    
    @SerializedName("full_name")
    public String fullName;
    
    public String mobile;
    
    @SerializedName("address_line1")
    public String addressLine1;
    
    @SerializedName("address_line2")
    public String addressLine2;
    
    // Backward compatibility fields
    @SerializedName("house_no")
    public String houseNo;
    
    @SerializedName("landmark")
    public String landmark;
    
    @SerializedName("pincode")
    public String pincode;
    
    @SerializedName("city")
    public String city;
    
    public String state;
    
    @SerializedName("is_default")
    public boolean isDefault;
    
    @SerializedName("created_at")
    public String createdAt;
    
    @SerializedName("updated_at")
    public String updatedAt;

    public Address() {}
}