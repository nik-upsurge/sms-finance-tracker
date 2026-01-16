package com.smsfinance.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class Categorizer {
    
    // Category definitions with keywords (smart keyword-based "ML")
    private static final Map<String, String[]> CATEGORY_KEYWORDS = new HashMap<>();
    
    static {
        // Food & Dining
        CATEGORY_KEYWORDS.put("Food", new String[]{
            "cafe", "coffee", "restaurant", "food", "zomato", "swiggy", "dominos", "pizza",
            "burger", "mcdonalds", "kfc", "starbucks", "subway", "dakshin", "biryani",
            "kitchen", "eatery", "dhaba", "bakery", "chai", "tea", "juice", "freshly",
            "barbeque", "grill", "diner", "canteen", "mess", "tiffin", "hotel", "eats"
        });
        
        // Shopping
        CATEGORY_KEYWORDS.put("Shopping", new String[]{
            "amazon", "flipkart", "myntra", "ajio", "nykaa", "meesho", "snapdeal",
            "mall", "store", "mart", "retail", "shop", "bazaar", "market", "dmart",
            "reliance", "bigbasket", "grofers", "blinkit", "zepto", "instamart",
            "decathlon", "croma", "vijay sales", "fashion", "clothing", "apparel"
        });
        
        // Utilities & Bills
        CATEGORY_KEYWORDS.put("Utilities", new String[]{
            "electricity", "electric", "power", "bescom", "water", "gas", "pipeline",
            "internet", "broadband", "wifi", "jio", "airtel", "vodafone", "bsnl",
            "recharge", "mobile", "postpaid", "prepaid", "dth", "tatasky", "dish",
            "bill", "payment", "utility"
        });
        
        // Transport & Travel
        CATEGORY_KEYWORDS.put("Transport", new String[]{
            "uber", "ola", "rapido", "cab", "taxi", "auto", "metro", "bus", "train",
            "irctc", "railway", "flight", "airline", "indigo", "spicejet", "vistara",
            "makemytrip", "goibibo", "cleartrip", "yatra", "redbus", "abhibus",
            "petrol", "diesel", "fuel", "hp", "bharat petroleum", "iocl", "parking"
        });
        
        // Entertainment
        CATEGORY_KEYWORDS.put("Entertainment", new String[]{
            "netflix", "spotify", "amazon prime", "hotstar", "disney", "zee5",
            "youtube", "premium", "movie", "cinema", "pvr", "inox", "bookmyshow",
            "theatre", "concert", "event", "ticket", "gaming", "playstation", "xbox"
        });
        
        // Health & Fitness
        CATEGORY_KEYWORDS.put("Health", new String[]{
            "pharmacy", "medical", "medicine", "apollo", "medplus", "netmeds",
            "hospital", "clinic", "doctor", "diagnostic", "lab", "test", "health",
            "gym", "fitness", "cult", "yoga", "wellness", "insurance", "policy"
        });
        
        // Transfer
        CATEGORY_KEYWORDS.put("Transfer", new String[]{
            "transfer", "sent to", "paid to", "upi", "neft", "imps", "rtgs",
            "self transfer", "own account"
        });
        
        // Education
        CATEGORY_KEYWORDS.put("Education", new String[]{
            "school", "college", "university", "course", "udemy", "coursera",
            "unacademy", "byju", "book", "stationery", "tuition", "class", "coaching"
        });
        
        // Subscriptions
        CATEGORY_KEYWORDS.put("Subscription", new String[]{
            "subscription", "membership", "annual", "monthly", "renewal", "plan"
        });
    }
    
    public static String categorize(String merchant, String smsBody) {
        if (merchant == null && smsBody == null) return "Other";
        
        String searchText = ((merchant != null ? merchant : "") + " " + 
                           (smsBody != null ? smsBody : "")).toLowerCase();
        
        // Calculate match scores for each category
        String bestCategory = "Other";
        int bestScore = 0;
        
        for (Map.Entry<String, String[]> entry : CATEGORY_KEYWORDS.entrySet()) {
            int score = calculateMatchScore(searchText, entry.getValue());
            if (score > bestScore) {
                bestScore = score;
                bestCategory = entry.getKey();
            }
        }
        
        // Require minimum confidence
        return bestScore >= 1 ? bestCategory : "Other";
    }
    
    private static int calculateMatchScore(String text, String[] keywords) {
        int score = 0;
        for (String keyword : keywords) {
            if (text.contains(keyword.toLowerCase())) {
                // Longer keywords get higher score (more specific)
                score += keyword.length() > 5 ? 2 : 1;
            }
        }
        return score;
    }
    
    // Get all available categories
    public static String[] getCategories() {
        return new String[]{
            "Food", "Shopping", "Utilities", "Transport", "Entertainment",
            "Health", "Transfer", "Education", "Subscription", "Cash", "Income", "Other"
        };
    }
}
