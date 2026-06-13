package com.nmmart.retailos.data;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.lang.reflect.Type;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

public class SearchHistoryManager {
    private static SearchHistoryManager instance;
    private SharedPreferences prefs;
    private Gson gson;

    private static final String PREF_NAME = "search_history";
    private static final String KEY_HISTORY = "search_queries";

    private SearchHistoryManager(Context context) {
        this.prefs = getEncryptedSharedPreferences(context);
        this.gson = new Gson();
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

    public static synchronized SearchHistoryManager getInstance(Context context) {
        if (instance == null) {
            instance = new SearchHistoryManager(context.getApplicationContext());
        }
        return instance;
    }

    public void addToHistory(String query) {
        if (query == null || query.trim().isEmpty()) return;

        List<String> history = getHistory();
        
        // Remove if already exists to move to top
        history.remove(query);
        
        // Add to top
        history.add(0, query);
        
        // Keep only last 10 searches
        if (history.size() > 10) {
            history = history.subList(0, 10);
        }
        
        saveHistory(history);
    }

    public List<String> getHistory() {
        String historyJson = prefs.getString(KEY_HISTORY, null);
        if (historyJson != null) {
            TypeToken<List<String>> token = new TypeToken<List<String>>() {};
            return gson.fromJson(historyJson, token.getType());
        }
        return new ArrayList<>();
    }

    public void clearHistory() {
        prefs.edit().remove(KEY_HISTORY).apply();
    }

    private void saveHistory(List<String> history) {
        prefs.edit().putString(KEY_HISTORY, gson.toJson(history)).apply();
    }
}
