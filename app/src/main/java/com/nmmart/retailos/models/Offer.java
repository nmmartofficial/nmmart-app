package com.nmmart.retailos.models;

public class Offer {
    private String id;
    private String title;
    private String description;
    private String imageUrl;
    private String discount;

    public Offer(String id, String title, String description, String imageUrl, String discount) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
        this.discount = discount;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getImageUrl() { return imageUrl; }
    public String getDiscount() { return discount; }
}
