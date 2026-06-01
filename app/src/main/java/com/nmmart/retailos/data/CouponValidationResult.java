package com.nmmart.retailos.data;

import com.google.gson.annotations.SerializedName;

public class CouponValidationResult {
    @SerializedName("is_valid")
    public boolean isValid;

    @SerializedName("discount_amount")
    public double discountAmount;

    @SerializedName("message")
    public String message;
}

