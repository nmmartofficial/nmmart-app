package com.nmmart.retailos.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

public class PriceUtils {

    public static String formatPrice(double price) {
        return "₹" + String.format("%.2f", price);
    }

    public static String formatPriceWithDecimal(double price) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        return formatter.format(price);
    }

    public static double calculateDiscountPercentage(double mrp, double salePrice) {
        if (mrp <= 0) return 0;
        double discount = mrp - salePrice;
        return (discount / mrp) * 100;
    }

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();
        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}
