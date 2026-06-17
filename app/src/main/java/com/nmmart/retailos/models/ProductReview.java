package com.nmmart.retailos.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class ProductReview implements Serializable {
    public String id;
    
    @SerializedName("product_id")
    public String productId;
    
    @SerializedName("user_id")
    public String userId;
    
    @SerializedName("user_name")
    public String userName;
    
    public Integer rating;
    
    @SerializedName("review_text")
    public String reviewText;
    
    @SerializedName("created_at")
    public String createdAt;

    public ProductReview() {
    }

    // Null-safe getters
    public String getId() {
        return id != null ? id : "";
    }

    public String getProductId() {
        return productId != null ? productId : "";
    }

    public String getUserId() {
        return userId != null ? userId : "";
    }

    public String getUserName() {
        return userName != null ? userName : "User";
    }

    public int getRating() {
        return rating != null ? rating : 0;
    }

    public String getReviewText() {
        return reviewText != null ? reviewText : "";
    }

    public String getCreatedAt() {
        return createdAt != null ? createdAt : "";
    }
}
