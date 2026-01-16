package com.smsfinance.database;

public class Transaction {
    private long id;
    private double amount;
    private String type; // "debit" or "credit"
    private String merchant;
    private String category;
    private String reference;
    private String transactionDate;
    private String rawSms;
    private boolean synced;
    private String createdAt;

    public Transaction() {}

    public Transaction(double amount, String type, String merchant, String category, 
                       String reference, String transactionDate, String rawSms) {
        this.amount = amount;
        this.type = type;
        this.merchant = merchant;
        this.category = category;
        this.reference = reference;
        this.transactionDate = transactionDate;
        this.rawSms = rawSms;
        this.synced = false;
    }

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getMerchant() { return merchant; }
    public void setMerchant(String merchant) { this.merchant = merchant; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public String getTransactionDate() { return transactionDate; }
    public void setTransactionDate(String transactionDate) { this.transactionDate = transactionDate; }

    public String getRawSms() { return rawSms; }
    public void setRawSms(String rawSms) { this.rawSms = rawSms; }

    public boolean isSynced() { return synced; }
    public void setSynced(boolean synced) { this.synced = synced; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
