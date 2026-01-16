package com.smsfinance.utils;

import com.smsfinance.database.Transaction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SmsParser {
    
    // Pattern for UPI Sent: "Sent Rs.140.00 From HDFC Bank A/C *3483 To DAKSHIN CAFE On 06/01/26 Ref 696932272808"
    private static final Pattern UPI_SENT_PATTERN = Pattern.compile(
        "Sent\\s+Rs\\.?([\\d,]+\\.?\\d*).*?To\\s+(.+?)\\s+On\\s+(\\d{2}/\\d{2}/\\d{2})\\s+Ref\\s+(\\d+)",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    // Pattern for UPI Received: "Received Rs.500.00 in HDFC Bank A/C *3483 From JOHN DOE On 06/01/26 Ref 123456"
    private static final Pattern UPI_RECEIVED_PATTERN = Pattern.compile(
        "Received\\s+Rs\\.?([\\d,]+\\.?\\d*).*?From\\s+(.+?)\\s+On\\s+(\\d{2}/\\d{2}/\\d{2})\\s+Ref\\s+(\\d+)",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    // Pattern for regular debit: "Rs 1000.00 debited from A/C *3483 on 06-Jan-26" or "Rs.1000 debited"
    private static final Pattern DEBIT_PATTERN = Pattern.compile(
        "Rs\\.?\\s*([\\d,]+\\.?\\d*)\\s+debited.*?(?:on\\s+)?(\\d{2}[-/]\\w{3}[-/]\\d{2,4})?",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    // Pattern for regular credit: "Rs 5000.00 credited to A/C *3483 on 06-Jan-26"
    private static final Pattern CREDIT_PATTERN = Pattern.compile(
        "Rs\\.?\\s*([\\d,]+\\.?\\d*)\\s+credited.*?(?:on\\s+)?(\\d{2}[-/]\\w{3}[-/]\\d{2,4})?",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    // ATM withdrawal pattern
    private static final Pattern ATM_PATTERN = Pattern.compile(
        "Rs\\.?\\s*([\\d,]+\\.?\\d*).*?(?:withdrawn|ATM).*?(?:on\\s+)?(\\d{2}[-/]\\w{3}[-/]\\d{2,4})?",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    // Generic amount pattern as fallback
    private static final Pattern AMOUNT_PATTERN = Pattern.compile(
        "Rs\\.?\\s*([\\d,]+\\.?\\d*)",
        Pattern.CASE_INSENSITIVE
    );

    public static boolean isHdfcSms(String sender) {
        if (sender == null) return false;
        String lowerSender = sender.toLowerCase();
        // Check if sender contains "hdfcbk" after a hyphen
        return lowerSender.contains("-hdfcbk") || 
               lowerSender.contains("hdfcbank") ||
               lowerSender.endsWith("hdfcbk");
    }

    public static Transaction parse(String smsBody) {
        if (smsBody == null || smsBody.isEmpty()) return null;
        
        Transaction transaction = null;
        
        // Try UPI Sent first
        Matcher matcher = UPI_SENT_PATTERN.matcher(smsBody);
        if (matcher.find()) {
            transaction = new Transaction();
            transaction.setAmount(parseAmount(matcher.group(1)));
            transaction.setType("debit");
            transaction.setMerchant(cleanMerchant(matcher.group(2)));
            transaction.setTransactionDate(matcher.group(3));
            transaction.setReference(matcher.group(4));
            transaction.setRawSms(smsBody);
            transaction.setCategory(Categorizer.categorize(transaction.getMerchant(), smsBody));
            return transaction;
        }
        
        // Try UPI Received
        matcher = UPI_RECEIVED_PATTERN.matcher(smsBody);
        if (matcher.find()) {
            transaction = new Transaction();
            transaction.setAmount(parseAmount(matcher.group(1)));
            transaction.setType("credit");
            transaction.setMerchant(cleanMerchant(matcher.group(2)));
            transaction.setTransactionDate(matcher.group(3));
            transaction.setReference(matcher.group(4));
            transaction.setRawSms(smsBody);
            transaction.setCategory("Income");
            return transaction;
        }
        
        // Try ATM withdrawal
        matcher = ATM_PATTERN.matcher(smsBody);
        if (matcher.find() && smsBody.toLowerCase().contains("atm")) {
            transaction = new Transaction();
            transaction.setAmount(parseAmount(matcher.group(1)));
            transaction.setType("debit");
            transaction.setMerchant("ATM Withdrawal");
            transaction.setTransactionDate(matcher.group(2));
            transaction.setRawSms(smsBody);
            transaction.setCategory("Cash");
            return transaction;
        }
        
        // Try regular debit
        matcher = DEBIT_PATTERN.matcher(smsBody);
        if (matcher.find()) {
            transaction = new Transaction();
            transaction.setAmount(parseAmount(matcher.group(1)));
            transaction.setType("debit");
            transaction.setMerchant(extractMerchantFromBody(smsBody));
            transaction.setTransactionDate(matcher.group(2));
            transaction.setRawSms(smsBody);
            transaction.setCategory(Categorizer.categorize(transaction.getMerchant(), smsBody));
            return transaction;
        }
        
        // Try regular credit
        matcher = CREDIT_PATTERN.matcher(smsBody);
        if (matcher.find()) {
            transaction = new Transaction();
            transaction.setAmount(parseAmount(matcher.group(1)));
            transaction.setType("credit");
            transaction.setMerchant(extractMerchantFromBody(smsBody));
            transaction.setTransactionDate(matcher.group(2));
            transaction.setRawSms(smsBody);
            transaction.setCategory("Income");
            return transaction;
        }
        
        return null;
    }

    private static double parseAmount(String amountStr) {
        if (amountStr == null) return 0;
        // Remove commas and parse
        String cleaned = amountStr.replace(",", "").trim();
        try {
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String cleanMerchant(String merchant) {
        if (merchant == null) return "Unknown";
        return merchant.trim()
                .replaceAll("\\s+", " ")
                .replaceAll("[^a-zA-Z0-9\\s@.-]", "");
    }

    private static String extractMerchantFromBody(String body) {
        // Try to find merchant info after common keywords
        Pattern merchantPattern = Pattern.compile(
            "(?:to|at|for|merchant|towards)\\s+([A-Za-z0-9\\s]+?)(?:\\s+on|\\s+ref|\\.|$)",
            Pattern.CASE_INSENSITIVE
        );
        Matcher matcher = merchantPattern.matcher(body);
        if (matcher.find()) {
            return cleanMerchant(matcher.group(1));
        }
        return "Unknown";
    }
}
