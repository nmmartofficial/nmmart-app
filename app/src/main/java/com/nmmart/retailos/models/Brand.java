package com.nmmart.retailos.models;

public class Brand {
    private String name;
    private int logoResId;

    public Brand(String name, int logoResId) {
        this.name = name;
        this.logoResId = logoResId;
    }

    public String getName() {
        return name;
    }

    public int getLogoResId() {
        return logoResId;
    }
}
