package com.nmmart.retailos.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import org.json.JSONObject;

import java.io.IOException;
import java.security.GeneralSecurityException;

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
    private static final String KEY_WALLET_BALANCE = "walletBalanceLong";
    private static final String KEY_IS_VIP = "isVip";
    private static final String KEY_CAT_SHAPE = "categoryShape";
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

    private static final String DEFAULT_LOGO_URL = "https://i.postimg.cc/1XFXZgzX/logo-nm-mart-app.png";
    
    private final SharedPreferences pref;
    private final SharedPreferences.Editor editor;
    private final Context context;

    public SessionManager(Context context) {
        this.context = context;
        this.pref = getEncryptedSharedPreferences(context);
        this.editor = pref.edit();
    }

    private SharedPreferences getEncryptedSharedPreferences(Context context) {
        try {
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);

            return EncryptedSharedPreferences.create(
                    PREF_NAME,
                    masterKeyAlias,
                    context,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            // Fallback to regular SharedPreferences if encryption fails
            return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        }
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
        if (role != null) editor.putString(KEY_ROLE, role);
        else editor.remove(KEY_ROLE);
        editor.apply();
    }

    public void updateAuthTokens(String accessToken, String refreshToken, long expiresAtEpochSec) {
        editor.putString(KEY_ACCESS_TOKEN, accessToken);
        editor.putString(KEY_REFRESH_TOKEN, refreshToken);
        editor.putLong(KEY_EXPIRES_AT, expiresAtEpochSec);
        String role = extractRoleFromJwt(accessToken);
        if (role != null) editor.putString(KEY_ROLE, role);
        else editor.remove(KEY_ROLE);
        editor.apply();
    }

    public String getEmail() { return pref.getString(KEY_EMAIL, ""); }
    public String getRole() { return pref.getString(KEY_ROLE, ""); }
    public String getUserId() { return pref.getString(KEY_USER_ID, ""); }
    public void setDarkMode(boolean isDark) { editor.putBoolean(KEY_DARK_MODE, isDark).apply(); }
    public boolean isDarkMode() { return pref.getBoolean(KEY_DARK_MODE, false); }
    public String getPhoneE164() { return pref.getString(KEY_PHONE_E164, ""); }
    public String getAccessToken() { return pref.getString(KEY_ACCESS_TOKEN, ""); }
    public String getRefreshToken() { return pref.getString(KEY_REFRESH_TOKEN, ""); }
    public long getExpiresAtEpochSec() { return pref.getLong(KEY_EXPIRES_AT, 0L); }

    public boolean hasValidAccessToken(long nowEpochSec) {
        String token = getAccessToken();
        if (token == null || token.isEmpty()) return false;
        long exp = getExpiresAtEpochSec();
        return exp > 0 && nowEpochSec < exp;
    }

    public void setWalletBalance(double balance) {
        editor.putLong(KEY_WALLET_BALANCE, Double.doubleToRawLongBits(balance));
        editor.apply();
    }

    public double getWalletBalance() {
        return Double.longBitsToDouble(pref.getLong(KEY_WALLET_BALANCE, Double.doubleToRawLongBits(0.0)));
    }

    public void setVipStatus(boolean isVip) { editor.putBoolean(KEY_IS_VIP, isVip).apply(); }
    public boolean isVip() { return pref.getBoolean(KEY_IS_VIP, false); }
    public void setCategoryShape(String shape) { editor.putString(KEY_CAT_SHAPE, shape).apply(); }
    public String getCategoryShape() { return pref.getString(KEY_CAT_SHAPE, "square"); }
    public String getBannerText() { return pref.getString(KEY_BANNER_TEXT, "FESTIVE DHAMAKA!\nUP TO 50% OFF"); }
    public String getBannerColor() { return pref.getString(KEY_BANNER_COLOR, "#FF9800"); }

    public void setUserName(String name) { editor.putString(KEY_USER_NAME, name).apply(); }
    public String getUserName() { return pref.getString(KEY_USER_NAME, "Customer"); }
    public void setDeliveryLocation(String location) { editor.putString(KEY_DELIVERY_LOCATION, location).apply(); }
    public String getDeliveryLocation() { return pref.getString(KEY_DELIVERY_LOCATION, "Naya Nagar, Manjhanpur"); }
    public void setStoreLogoUrl(String url) { editor.putString(KEY_STORE_LOGO, url).apply(); }
    public String getStoreLogoUrl() { return pref.getString(KEY_STORE_LOGO, DEFAULT_LOGO_URL); }

    public boolean isLoggedIn() {
        // Accept both proper JWT tokens and our simple tokens
        return pref.getBoolean(KEY_IS_LOGGED_IN, false) && !getAccessToken().isEmpty();
    }

    public String getMobile() { return pref.getString(KEY_MOBILE, null); }
    public void setOnboardingCompleted(boolean completed) { editor.putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply(); }
    public boolean isOnboardingCompleted() { return pref.getBoolean(KEY_ONBOARDING_COMPLETED, false); }

    public void logout() {
        editor.remove(KEY_IS_LOGGED_IN).remove(KEY_MOBILE).remove(KEY_EMAIL).remove(KEY_USER_ID)
              .remove(KEY_PHONE_E164).remove(KEY_ACCESS_TOKEN).remove(KEY_REFRESH_TOKEN)
              .remove(KEY_EXPIRES_AT).remove(KEY_ROLE).remove(KEY_USER_NAME)
              .remove(KEY_WALLET_BALANCE).remove(KEY_IS_VIP).apply();
    }

    private static String extractRoleFromJwt(String jwt) {
        try {
            if (jwt == null) return null;
            String[] parts = jwt.split("\\.");
            if (parts.length < 2) return null;
            byte[] decoded = Base64.decode(parts[1], Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
            JSONObject json = new JSONObject(new String(decoded));
            if (json.has("app_metadata")) return json.getJSONObject("app_metadata").optString("role", null);
            if (json.has("user_metadata")) return json.getJSONObject("user_metadata").optString("role", null);
            return null;
        } catch (Exception e) { return null; }
    }
}
