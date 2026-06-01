package com.nmmart.retailos.models;

import com.google.gson.annotations.SerializedName;

public class WalletMaster {
    @SerializedName("id")
    public String id;

    @SerializedName("user_id")
    public String userId;

    @SerializedName("current_balance")
    public double currentBalance;

    @SerializedName("updated_at")
    public String updatedAt;

    public WalletMaster(String id, String userId, double currentBalance, String updatedAt) {
        this.id = id;
        this.userId = userId;
        this.currentBalance = currentBalance;
        this.updatedAt = updatedAt;
    }
}
