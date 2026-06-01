package com.nmmart.retailos.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class Banner implements Serializable {
    public String id;
    
    @SerializedName("title")
    public String title;
    
    @SerializedName("redirect_path")
    public String redirectPath;
    
    @SerializedName("image_url")
    public String imageUrl;
    
    @SerializedName("is_active")
    public boolean isActive;
    
    @SerializedName("created_at")
    public String createdAt;
}
