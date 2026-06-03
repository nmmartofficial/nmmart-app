package com.nmmart.retailos.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class Category implements Serializable {
    @SerializedName("id")
    private String id;
    
    @SerializedName("name")
    private String name;
    
    @SerializedName("image_url")
    private String imageUrl;

    @SerializedName("icon_url")
    private String iconUrl;

    @SerializedName("position")
    private Integer position;

    @SerializedName("parent_id")
    private String parentId;

    @SerializedName("is_active")
    private Boolean isActive;

    public Category() {}

    // Old constructor for backward compatibility
    public Category(String id, String name, String iconUrl, String parentId) {
        this.id = id;
        this.name = name;
        this.iconUrl = iconUrl;
        this.imageUrl = iconUrl;
        this.parentId = parentId;
    }

    // New constructor
    public Category(String id, String name, String imageUrl, Integer position) {
        this.id = id;
        this.name = name;
        this.imageUrl = imageUrl;
        this.position = position;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getIconUrl() { 
        if (iconUrl == null || iconUrl.isEmpty()) {
            return imageUrl;
        }
        return iconUrl; 
    }
    public void setIconUrl(String iconUrl) { 
        this.iconUrl = iconUrl; 
        if (this.imageUrl == null || this.imageUrl.isEmpty()) {
            this.imageUrl = iconUrl;
        }
    }

    public Integer getPosition() { return position; }
    public void setPosition(Integer position) { this.position = position; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public String getParentId() { return parentId; }
    public void setParentId(String parentId) { this.parentId = parentId; }
}
