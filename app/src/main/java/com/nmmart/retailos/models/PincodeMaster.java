package com.nmmart.retailos.models;

import com.google.gson.annotations.SerializedName;

public class PincodeMaster {
    @SerializedName("id")
    public String id;

    @SerializedName("pincode")
    public String pincode;

    @SerializedName("is_allowed")
    public boolean isAllowed;

    @SerializedName("delivery_charge")
    public double deliveryCharge;

    public PincodeMaster(String id, String pincode, boolean isAllowed, double deliveryCharge) {
        this.id = id;
        this.pincode = pincode;
        this.isAllowed = isAllowed;
        this.deliveryCharge = deliveryCharge;
    }
}
