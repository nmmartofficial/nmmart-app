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
    
    @SerializedName("house_no")
    public String houseNo;
    
    @SerializedName("landmark")
    public String landmark;
    
    @SerializedName("pincode")
    public String pincode;
    
    @SerializedName("city")
    public String city;
    
    @SerializedName("is_default")
    public boolean isDefault;

    public Address() {}
}