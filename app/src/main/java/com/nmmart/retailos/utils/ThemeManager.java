package com.nmmart.retailos.utils;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;

import com.nmmart.retailos.models.AppConfig;

public class ThemeManager {
    private static ThemeManager instance;
    private AppConfig appConfig;
    private Context context;

    private ThemeManager(Context context) {
        this.context = context.getApplicationContext();
        // Default config
        this.appConfig = getDefaultConfig();
    }

    public static synchronized ThemeManager getInstance(Context context) {
        if (instance == null) {
            instance = new ThemeManager(context);
        }
        return instance;
    }

    public void setAppConfig(AppConfig config) {
        if (config != null) {
            this.appConfig = config;
        }
    }

    public AppConfig getAppConfig() {
        return appConfig;
    }

    public int parseColor(String colorString, int defaultColor) {
        if (colorString == null || colorString.isEmpty()) {
            return defaultColor;
        }
        try {
            return Color.parseColor(colorString);
        } catch (Exception e) {
            return defaultColor;
        }
    }

    public int getPrimaryColor() {
        return parseColor(appConfig.primaryColor, Color.parseColor("#2563EB")); // Default blue
    }

    public int getSecondaryColor() {
        return parseColor(appConfig.secondaryColor, Color.parseColor("#DBEAFE")); // Default light blue
    }

    public int getAccentColor() {
        return parseColor(appConfig.accentColor, Color.parseColor("#4CAF50")); // Default green
    }

    public int getBackgroundColor() {
        return parseColor(appConfig.backgroundColor, Color.WHITE);
    }

    public int getTextColorPrimary() {
        return parseColor(appConfig.textColorPrimary, Color.parseColor("#212121"));
    }

    public int getTextColorSecondary() {
        return parseColor(appConfig.textColorSecondary, Color.parseColor("#757575"));
    }

    public String getCategoryShape() {
        return (appConfig.categoryShape != null && !appConfig.categoryShape.isEmpty()) 
                ? appConfig.categoryShape : "rounded_square";
    }

    public String getBrandShape() {
        return (appConfig.brandShape != null && !appConfig.brandShape.isEmpty()) 
                ? appConfig.brandShape : "circle";
    }

    public GradientDrawable getShapeDrawable(String shapeType, int backgroundColor, int cornerRadiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(backgroundColor);
        
        float cornerRadiusPx = dpToPx(cornerRadiusDp);
        
        switch (shapeType) {
            case "circle":
                drawable.setShape(GradientDrawable.OVAL);
                break;
            case "square":
                drawable.setShape(GradientDrawable.RECTANGLE);
                drawable.setCornerRadius(0);
                break;
            case "rounded_square":
            default:
                drawable.setShape(GradientDrawable.RECTANGLE);
                drawable.setCornerRadius(cornerRadiusPx);
                break;
        }
        return drawable;
    }

    private float dpToPx(int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return dp * density;
    }

    private AppConfig getDefaultConfig() {
        AppConfig defaultConfig = new AppConfig();
        defaultConfig.primaryColor = "#2563EB";
        defaultConfig.secondaryColor = "#DBEAFE";
        defaultConfig.accentColor = "#10B981";
        defaultConfig.backgroundColor = "#FFFFFF";
        defaultConfig.textColorPrimary = "#1F2937";
        defaultConfig.textColorSecondary = "#6B7280";
        defaultConfig.categoryShape = "rounded_square";
        defaultConfig.brandShape = "circle";
        defaultConfig.bannerStyle = "default";
        return defaultConfig;
    }
}
