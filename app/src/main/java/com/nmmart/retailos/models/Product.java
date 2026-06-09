package com.nmmart.retailos.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class Product implements Serializable {
    public String id;
    
    @SerializedName("item_name")
    public String name;
    
    @SerializedName("description")
    public String description;
    
    @SerializedName("category_id")
    public String categoryId;
    
    @SerializedName("category")
    public String category;
    
    @SerializedName("brand_id")
    public String brandId;
    
    @SerializedName("brand")
    public String brand;
    
    @SerializedName("mrp")
    public Double mrp;
    
    @SerializedName("sale_rate")
    public Double nm_price;
    
    @SerializedName("discount")
    public Double discount;
    
    @SerializedName("image_url")
    public String image_url;

    @SerializedName("unit_id")
    public String unitId;

    @SerializedName("unit")
    public String unit;

    @SerializedName("stock")
    public Double stock;

    @SerializedName("is_featured")
    public Boolean is_featured;

    @SerializedName("badge")
    public String badge;

    @SerializedName("barcode")
    public String barcode;

    // Null-safe getters for all fields
    public String getId() {
        return id != null ? id : "";
    }

    public String getName() {
        return name != null ? name : "Product";
    }

    public String getCategoryId() {
        return categoryId != null ? categoryId : "";
    }

    public String getCategory() {
        return category != null ? category : "";
    }

    public String getBrandId() {
        return brandId != null ? brandId : "";
    }

    public double getMrp() {
        return mrp != null ? mrp : 0.0;
    }

    public double getNmPrice() {
        return nm_price != null ? nm_price : 0.0;
    }

    public double getDiscount() {
        return discount != null ? discount : 0.0;
    }

    public String getImageUrl() {
        return image_url != null ? image_url : "";
    }

    public String getUnitId() {
        return unitId != null ? unitId : "";
    }

    public String getUnit() {
        return unit != null ? unit : "pcs";
    }

    public double getStock() {
        return stock != null ? stock : 0.0;
    }

    public Boolean getIsFeatured() {
        return is_featured != null ? is_featured : false;
    }

    public String getBadge() {
        return badge != null ? badge : "";
    }

    public String getBarcode() {
        return barcode != null ? barcode : "";
    }

    public String getBrand() {
        return brand != null ? brand : "No Brand";
    }

    public String getDescription() {
        return description != null ? description : "No description available.";
    }

    public Product() {
    }
}