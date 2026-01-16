package com.smsfinance.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.smsfinance.database.DatabaseHelper;
import com.smsfinance.database.Transaction;
import com.smsfinance.services.SyncWorker;
import com.smsfinance.utils.SmsParser;

public class SmsReceiver extends BroadcastReceiver {
    private static final String TAG = "SmsReceiver";
    private static final String SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!SMS_RECEIVED.equals(intent.getAction())) {
            return;
        }

        Bundle bundle = intent.getExtras();
        if (bundle == null) return;

        Object[] pdus = (Object[]) bundle.get("pdus");
        if (pdus == null) return;

        String format = bundle.getString("format");
        
        for (Object pdu : pdus) {
            SmsMessage smsMessage = SmsMessage.createFromPdu((byte[]) pdu, format);
            String sender = smsMessage.getDisplayOriginatingAddress();
            String body = smsMessage.getMessageBody();
            
            Log.d(TAG, "SMS from: " + sender);
            
            // Check if it's from HDFC
            if (SmsParser.isHdfcSms(sender)) {
                Log.d(TAG, "HDFC SMS detected, parsing...");
                processHdfcSms(context, body);
            }
        }
    }

    private void processHdfcSms(Context context, String body) {
        try {
            // Parse the SMS
            Transaction transaction = SmsParser.parse(body);
            
            if (transaction != null) {
                // Save to database
                DatabaseHelper db = DatabaseHelper.getInstance(context);
                long id = db.insertTransaction(transaction);
                
                Log.d(TAG, "Transaction saved with ID: " + id + 
                      ", Amount: " + transaction.getAmount() + 
                      ", Type: " + transaction.getType() +
                      ", Category: " + transaction.getCategory());
                
                // Trigger sync to Google Sheets
                scheduleSyncWork(context);
            } else {
                Log.d(TAG, "Could not parse transaction from SMS: " + body);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing SMS", e);
        }
    }

    private void scheduleSyncWork(Context context) {
        OneTimeWorkRequest syncRequest = new OneTimeWorkRequest.Builder(SyncWorker.class)
                .build();
        
        WorkManager.getInstance(context).enqueue(syncRequest);
        Log.d(TAG, "Sync work scheduled");
    }
}
