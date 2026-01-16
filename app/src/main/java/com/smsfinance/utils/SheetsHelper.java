package com.smsfinance.utils;

import android.content.Context;
import android.util.Log;

import com.smsfinance.database.Transaction;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONArray;
import org.json.JSONObject;

public class SheetsHelper {
    private static final String TAG = "SheetsHelper";
    private static final String SPREADSHEET_ID = "1aClBDcWCjpf4BMdOG5W5BZtiSvs6uWxBKGtRvyEppaA";
    
    // Google Apps Script Web App URL for writing to sheets
    // We'll use a simple web app deployment to bypass heavy SDK
    private static final String SCRIPT_URL = "https://script.google.com/macros/s/YOUR_SCRIPT_ID/exec";
    
    // Alternative: Direct Sheets API with API key (simpler)
    private static final String SHEETS_API_URL = "https://sheets.googleapis.com/v4/spreadsheets/" + SPREADSHEET_ID + "/values/Sheet1!A:H:append?valueInputOption=USER_ENTERED&insertDataOption=INSERT_ROWS&key=";
    
    private Context context;
    private String accessToken;

    public SheetsHelper(Context context) {
        this.context = context;
    }

    public boolean initialize() {
        // For service account, we need to get OAuth token
        // This is simplified - in production use proper JWT signing
        try {
            accessToken = getAccessToken();
            return accessToken != null;
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize", e);
            return false;
        }
    }

    private String getAccessToken() {
        // Service account JWT token generation
        // Using the credentials from raw/credentials.json
        try {
            // Read credentials
            java.io.InputStream is = context.getResources().openRawResource(
                context.getResources().getIdentifier("credentials", "raw", context.getPackageName())
            );
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();
            
            JSONObject creds = new JSONObject(new String(buffer, StandardCharsets.UTF_8));
            String clientEmail = creds.getString("client_email");
            String privateKey = creds.getString("private_key");
            String tokenUri = creds.getString("token_uri");
            
            // Create JWT
            long now = System.currentTimeMillis() / 1000;
            JSONObject header = new JSONObject();
            header.put("alg", "RS256");
            header.put("typ", "JWT");
            
            JSONObject claim = new JSONObject();
            claim.put("iss", clientEmail);
            claim.put("scope", "https://www.googleapis.com/auth/spreadsheets");
            claim.put("aud", tokenUri);
            claim.put("iat", now);
            claim.put("exp", now + 3600);
            
            String headerB64 = android.util.Base64.encodeToString(
                header.toString().getBytes(StandardCharsets.UTF_8), 
                android.util.Base64.URL_SAFE | android.util.Base64.NO_WRAP | android.util.Base64.NO_PADDING
            );
            String claimB64 = android.util.Base64.encodeToString(
                claim.toString().getBytes(StandardCharsets.UTF_8),
                android.util.Base64.URL_SAFE | android.util.Base64.NO_WRAP | android.util.Base64.NO_PADDING
            );
            
            String signatureInput = headerB64 + "." + claimB64;
            
            // Sign with private key
            String cleanKey = privateKey
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
            
            byte[] keyBytes = android.util.Base64.decode(cleanKey, android.util.Base64.DEFAULT);
            java.security.spec.PKCS8EncodedKeySpec spec = new java.security.spec.PKCS8EncodedKeySpec(keyBytes);
            java.security.KeyFactory kf = java.security.KeyFactory.getInstance("RSA");
            java.security.PrivateKey key = kf.generatePrivate(spec);
            
            java.security.Signature sig = java.security.Signature.getInstance("SHA256withRSA");
            sig.initSign(key);
            sig.update(signatureInput.getBytes(StandardCharsets.UTF_8));
            byte[] signature = sig.sign();
            
            String signatureB64 = android.util.Base64.encodeToString(
                signature,
                android.util.Base64.URL_SAFE | android.util.Base64.NO_WRAP | android.util.Base64.NO_PADDING
            );
            
            String jwt = signatureInput + "." + signatureB64;
            
            // Exchange JWT for access token
            URL url = new URL(tokenUri);
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setDoOutput(true);
            
            String postData = "grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer&assertion=" + jwt;
            
            try (OutputStream os = conn.getOutputStream()) {
                os.write(postData.getBytes(StandardCharsets.UTF_8));
            }
            
            if (conn.getResponseCode() == 200) {
                java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(conn.getInputStream())
                );
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                
                JSONObject tokenResponse = new JSONObject(response.toString());
                return tokenResponse.getString("access_token");
            } else {
                Log.e(TAG, "Token request failed: " + conn.getResponseCode());
                return null;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting access token", e);
            return null;
        }
    }

    public boolean appendTransaction(Transaction transaction) {
        if (accessToken == null) {
            Log.e(TAG, "No access token");
            return false;
        }

        try {
            JSONArray row = new JSONArray();
            row.put(transaction.getTransactionDate() != null ? transaction.getTransactionDate() : "");
            row.put(transaction.getType() != null ? transaction.getType() : "");
            row.put(transaction.getAmount());
            row.put(transaction.getMerchant() != null ? transaction.getMerchant() : "");
            row.put(transaction.getCategory() != null ? transaction.getCategory() : "");
            row.put(transaction.getReference() != null ? transaction.getReference() : "");
            row.put(transaction.getRawSms() != null ? transaction.getRawSms().replace("\n", " ") : "");
            row.put(LocalDateTime.now().toString());

            JSONArray values = new JSONArray();
            values.put(row);

            JSONObject body = new JSONObject();
            body.put("values", values);

            String apiUrl = "https://sheets.googleapis.com/v4/spreadsheets/" + SPREADSHEET_ID + 
                           "/values/Sheet1!A:H:append?valueInputOption=USER_ENTERED&insertDataOption=INSERT_ROWS";

            URL url = new URL(apiUrl);
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + accessToken);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.toString().getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = conn.getResponseCode();
            Log.d(TAG, "Append response: " + responseCode);
            
            return responseCode == 200;

        } catch (Exception e) {
            Log.e(TAG, "Failed to append transaction", e);
            return false;
        }
    }

    public boolean appendTransactions(List<Transaction> transactions) {
        boolean allSuccess = true;
        for (Transaction t : transactions) {
            if (!appendTransaction(t)) {
                allSuccess = false;
            }
        }
        return allSuccess;
    }
}
