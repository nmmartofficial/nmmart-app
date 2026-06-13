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
    
    @SerializedName("position")
    public int position; // For ordering (upar/niche)
    
    @SerializedName("banner_type")
    public String bannerType; // e.g., "top", "middle", "bottom"
    
    @SerializedName("created_at")
    public String createdAt;
    
    @SerializedName("updated_at")
    public String updatedAt;
}
