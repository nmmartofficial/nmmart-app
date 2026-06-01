package com.nmmart.retailos.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class Product implements Serializable {
    public String id;
    
    @SerializedName("name")
    public String name;
    
    @SerializedName("description")
    public String description;
    
    @SerializedName("category")
    public String category;
    
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

    @SerializedName("unit")
    public String unit;

    @SerializedName("stock")
    public Integer stock;

    @SerializedName("is_featured")
    public Boolean is_featured;

    @SerializedName("badge")
    public String badge;

    // Null-safe getters for all fields
    public double getMrp() {
        return mrp != null ? mrp : 0.0;
    }

    public double getNmPrice() {
        return nm_price != null ? nm_price : 0.0;
    }

    public double getDiscount() {
        return discount != null ? discount : 0.0;
    }

    public int getStock() {
        return stock != null ? stock : 0;
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