package com.nmmart.retailos.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Category {
    @SerializedName("id")
    private String id;
    
    @SerializedName("name")
    private String name;
    
    @SerializedName("icon_url")
    private String iconUrl;

    @SerializedName("parent_id")
    private String parentId;

    public Category() {}

    public Category(String id, String name, String iconUrl, String parentId) {
        this.id = id;
        this.name = name;
        this.iconUrl = iconUrl;
        this.parentId = parentId;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getIconUrl() { return iconUrl; }
    public void setIconUrl(String iconUrl) { this.iconUrl = iconUrl; }

    public String getParentId() { return parentId; }
    public void setParentId(String parentId) { this.parentId = parentId; }
}
