package com.nmmart.retailos.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import org.json.JSONObject;

public class SessionManager {
    private static final String PREF_NAME = "NMMartPrefs";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_MOBILE = "mobile";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_PHONE_E164 = "phoneE164";
    private static final String KEY_ACCESS_TOKEN = "accessToken";
    private static final String KEY_REFRESH_TOKEN = "refreshToken";
    private static final String KEY_EXPIRES_AT = "expiresAtEpochSec";
    private static final String KEY_ROLE = "role";
    private static final String KEY_USER_NAME = "userName";
    private static final String KEY_DELIVERY_LOCATION = "deliveryLocation";
    private static final String KEY_WALLET_BALANCE = "walletBalance";
    private static final String KEY_IS_VIP = "isVip";
    private static final String KEY_CAT_SHAPE = "categoryShape"; // circle or square
    private static final String KEY_BANNER_TEXT = "bannerText";
    private static final String KEY_BANNER_COLOR = "bannerColor";
    private static final String KEY_SHOW_TRENDING = "showTrending";
    private static final String KEY_SHOW_NEW_STOCK = "showNewStock";
    private static final String KEY_SHOW_BUY_AGAIN = "showBuyAgain";
    private static final String KEY_SHOW_DEAL = "showDeal";
    private static final String KEY_STORE_THEME = "storeTheme";
    private static final String KEY_DARK_MODE = "darkMode";
    private static final String KEY_STORE_LOGO = "storeLogoUrl";
    private static final String KEY_ONBOARDING_COMPLETED = "onboardingCompleted";

    // Default logo link provided by user
    private static final String DEFAULT_LOGO_URL = "https://i.postimg.cc/1XFXZgzX/logo-nm-mart-app.png";
    
    private SharedPreferences pref;
    private SharedPreferences.Editor editor;
    private Context context;

    public SessionManager(Context context) {
        this.context = context;
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    public void setLogin(boolean isLoggedIn, String mobile, String email) {
        editor.putBoolean(KEY_IS_LOGGED_IN, isLoggedIn);
        editor.putString(KEY_MOBILE, mobile);
        editor.putString(KEY_EMAIL, email);
        editor.apply();
    }

    public void setAuthSession(String userId, String phoneE164, String accessToken, String refreshToken, long expiresAtEpochSec) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_USER_ID, userId);
        editor.putString(KEY_PHONE_E164, phoneE164);
        editor.putString(KEY_ACCESS_TOKEN, accessToken);
        editor.putString(KEY_REFRESH_TOKEN, refreshToken);
        editor.putLong(KEY_EXPIRES_AT, expiresAtEpochSec);

        String role = extractRoleFromJwt(accessToken);
        if (role != null) {
            editor.putString(KEY_ROLE, role);
        } else {
            editor.remove(KEY_ROLE);
        }

        editor.apply();
    }

    public void updateAuthTokens(String accessToken, String refreshToken, long expiresAtEpochSec) {
        editor.putString(KEY_ACCESS_TOKEN, accessToken);
        editor.putString(KEY_REFRESH_TOKEN, refreshToken);
        editor.putLong(KEY_EXPIRES_AT, expiresAtEpochSec);

        String role = extractRoleFromJwt(accessToken);
        if (role != null) {
            editor.putString(KEY_ROLE, role);
        } else {
            editor.remove(KEY_ROLE);
        }

        editor.apply();
    }

    public String getEmail() {
        return pref.getString(KEY_EMAIL, "");
    }

    public String getRole() {
        return pref.getString(KEY_ROLE, "");
    }

    public String getUserId() {
        return pref.getString(KEY_USER_ID, "");
    }

    public void setDarkMode(boolean isDark) {
        editor.putBoolean(KEY_DARK_MODE, isDark);
        editor.apply();
    }

    public boolean isDarkMode() {
        return pref.getBoolean(KEY_DARK_MODE, false);
    }

    public String getPhoneE164() {
        return pref.getString(KEY_PHONE_E164, "");
    }

    public String getAccessToken() {
        try {
            return pref.getString(KEY_ACCESS_TOKEN, "");
        } catch (Exception e) {
            return "";
        }
    }

    public String getRefreshToken() {
        return pref.getString(KEY_REFRESH_TOKEN, "");
    }

    public long getExpiresAtEpochSec() {
        return pref.getLong(KEY_EXPIRES_AT, 0L);
    }

    public boolean hasValidAccessToken(long nowEpochSec) {
        String token = getAccessToken();
        if (token == null || token.isEmpty()) return false;
        long exp = getExpiresAtEpochSec();
        return exp > 0 && nowEpochSec < exp;
    }

    public void setWalletBalance(float balance) {
        editor.putFloat(KEY_WALLET_BALANCE, balance);
        editor.apply();
    }

    public float getWalletBalance() {
        return pref.getFloat(KEY_WALLET_BALANCE, 0.0f);
    }

    public void setVipStatus(boolean isVip) {
        editor.putBoolean(KEY_IS_VIP, isVip);
        editor.apply();
    }

    public boolean isVip() {
        return pref.getBoolean(KEY_IS_VIP, false);
    }

    public void setCategoryShape(String shape) {
        editor.putString(KEY_CAT_SHAPE, shape);
        editor.apply();
    }

    public String getCategoryShape() {
        return pref.getString(KEY_CAT_SHAPE, "square"); // Default square
    }

    public void setBannerSettings(String text, String color) {
        editor.putString(KEY_BANNER_TEXT, text);
        editor.putString(KEY_BANNER_COLOR, color);
        editor.apply();
    }

    public String getBannerText() {
        return pref.getString(KEY_BANNER_TEXT, "FESTIVE DHAMAKA!\nUP TO 50% OFF");
    }

    public String getBannerColor() {
        return pref.getString(KEY_BANNER_COLOR, "#FF9800");
    }

    public void setSectionVisibility(String section, boolean isVisible) {
        switch (section) {
            case "trending": editor.putBoolean(KEY_SHOW_TRENDING, isVisible); break;
            case "new_stock": editor.putBoolean(KEY_SHOW_NEW_STOCK, isVisible); break;
            case "buy_again": editor.putBoolean(KEY_SHOW_BUY_AGAIN, isVisible); break;
            case "deal": editor.putBoolean(KEY_SHOW_DEAL, isVisible); break;
        }
        editor.apply();
    }

    public boolean isSectionVisible(String section) {
        switch (section) {
            case "trending": return pref.getBoolean(KEY_SHOW_TRENDING, true);
            case "new_stock": return pref.getBoolean(KEY_SHOW_NEW_STOCK, true);
            case "buy_again": return pref.getBoolean(KEY_SHOW_BUY_AGAIN, true);
            case "deal": return pref.getBoolean(KEY_SHOW_DEAL, true);
            default: return true;
        }
    }

    public void setUserName(String name) {
        editor.putString(KEY_USER_NAME, name);
        editor.apply();
    }

    public String getUserName() {
        return pref.getString(KEY_USER_NAME, "Customer");
    }

    public void setDeliveryLocation(String location) {
        editor.putString(KEY_DELIVERY_LOCATION, location);
        editor.apply();
    }

    public String getDeliveryLocation() {
        try {
            return pref.getString(KEY_DELIVERY_LOCATION, "Naya Nagar, Manjhanpur");
        } catch (Exception e) {
            return "Naya Nagar, Manjhanpur";
        }
    }

    public void setStoreTheme(String color) {
        editor.putString(KEY_STORE_THEME, color);
        editor.apply();
    }

    public String getStoreTheme() {
        return pref.getString(KEY_STORE_THEME, "#FF9800");
    }

    public void setStoreLogoUrl(String url) {
        editor.putString(KEY_STORE_LOGO, url);
        editor.apply();
    }

    public String getStoreLogoUrl() {
        return pref.getString(KEY_STORE_LOGO, DEFAULT_LOGO_URL);
    }

    public boolean isLoggedIn() {
        try {
            return pref.getBoolean(KEY_IS_LOGGED_IN, false) && !getAccessToken().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    public String getMobile() {
        return pref.getString(KEY_MOBILE, null);
    }

    public void setOnboardingCompleted(boolean completed) {
        editor.putBoolean(KEY_ONBOARDING_COMPLETED, completed);
        editor.apply();
    }

    public boolean isOnboardingCompleted() {
        return pref.getBoolean(KEY_ONBOARDING_COMPLETED, false);
    }

    public void logout() {
        // Preserve user settings, only clear auth/data
        editor.remove(KEY_IS_LOGGED_IN);
        editor.remove(KEY_MOBILE);
        editor.remove(KEY_EMAIL);
        editor.remove(KEY_USER_ID);
        editor.remove(KEY_PHONE_E164);
        editor.remove(KEY_ACCESS_TOKEN);
        editor.remove(KEY_REFRESH_TOKEN);
        editor.remove(KEY_EXPIRES_AT);
        editor.remove(KEY_ROLE);
        editor.remove(KEY_USER_NAME);
        editor.remove(KEY_WALLET_BALANCE);
        editor.remove(KEY_IS_VIP);
        editor.apply();
    }



    private static String extractRoleFromJwt(String jwt) {
        try {
            if (jwt == null) return null;
            String[] parts = jwt.split("\\.");
            if (parts.length < 2) return null;

            byte[] decoded = Base64.decode(parts[1], Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
            String payload = new String(decoded);
            JSONObject json = new JSONObject(payload);

            if (json.has("app_metadata")) {
                JSONObject appMeta = json.getJSONObject("app_metadata");
                if (appMeta.has("role")) {
                    String role = appMeta.optString("role", null);
                    return role == null || role.isEmpty() ? null : role;
                }
            }

            if (json.has("user_metadata")) {
                JSONObject userMeta = json.getJSONObject("user_metadata");
                if (userMeta.has("role")) {
                    String role = userMeta.optString("role", null);
                    return role == null || role.isEmpty() ? null : role;
                }
            }

            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
