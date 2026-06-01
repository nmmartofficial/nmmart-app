package com.nmmart.retailos.models;

import com.google.gson.annotations.SerializedName;

public class WalletTransaction {
    @SerializedName("id")
    public String id;

    @SerializedName("user_id")
    public String userId;

    @SerializedName("amount")
    public double amount;

    @SerializedName("transaction_type")
    public String transactionType;

    @SerializedName("description")
    public String description;

    @SerializedName("created_at")
    public String createdAt;

    public WalletTransaction(String id, String userId, double amount, String transactionType, String description, String createdAt) {
        this.id = id;
        this.userId = userId;
        this.amount = amount;
        this.transactionType = transactionType;
        this.description = description;
        this.createdAt = createdAt;
    }
}
