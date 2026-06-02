package com.nmmart.retailos.data;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.ArrayList;
import java.util.List;

public class SearchHistoryManager {
    private static final String PREF_NAME = "search_history";
    private static final String KEY_HISTORY = "search_queries";
    private static SearchHistoryManager instance;
    private SharedPreferences prefs;
    private Gson gson;

    private SearchHistoryManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
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
