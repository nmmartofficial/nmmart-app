package com.nmmart.retailos.models;

public class PaymentMethod {
    public String name;
    public int iconRes;
    public boolean isComingSoon;

    public PaymentMethod(String name, int iconRes, boolean isComingSoon) {
        this.name = name;
        this.iconRes = iconRes;
        this.isComingSoon = isComingSoon;
    }
}
