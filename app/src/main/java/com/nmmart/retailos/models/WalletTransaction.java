package com.nmmart.retailos.models;

public class WalletTransaction {
    private String id;
    private String type; // "credit" or "debit"
    private double amount;
    private String description;
    private long timestamp;
    private boolean isRead;

    public WalletTransaction(String id, String type, double amount, String description, long timestamp, boolean isRead) {
        this.id = id;
        this.type = type;
        this.amount = amount;
        this.description = description;
        this.timestamp = timestamp;
        this.isRead = isRead;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
}
