package com.nmmart.retailos.utils;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.nmmart.retailos.models.WalletTransaction;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class WalletTransactionStorage {
    private static final String PREF_NAME = "walletTransactions";
    private static final String KEY_TRANSACTIONS = "transactions";
    private static WalletTransactionStorage instance;
    private SharedPreferences prefs;
    private Gson gson;

    private WalletTransactionStorage(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public static synchronized WalletTransactionStorage getInstance(Context context) {
        if (instance == null) {
            instance = new WalletTransactionStorage(context.getApplicationContext());
        }
        return instance;
    }

    public List<WalletTransaction> getTransactions() {
        String json = prefs.getString(KEY_TRANSACTIONS, null);
        Type type = new TypeToken<List<WalletTransaction>>() {}.getType();
        List<WalletTransaction> transactions = gson.fromJson(json, type);
        return transactions != null ? transactions : new ArrayList<>();
    }

    public void saveTransaction(WalletTransaction transaction) {
        List<WalletTransaction> transactions = getTransactions();
        transactions.add(0, transaction);
        if (transactions.size() > 100) {
            transactions.remove(transactions.size() - 1);
        }
        saveTransactions(transactions);
    }

    public void saveTransactions(List<WalletTransaction> transactions) {
        String json = gson.toJson(transactions);
        prefs.edit().putString(KEY_TRANSACTIONS, json).apply();
    }

    public void clearAll() {
        prefs.edit().remove(KEY_TRANSACTIONS).apply();
    }
    
    public void deleteTransaction(String id) {
        List<WalletTransaction> transactions = getTransactions();
        transactions.removeIf(item -> item.getId().equals(id));
        saveTransactions(transactions);
    }
}
