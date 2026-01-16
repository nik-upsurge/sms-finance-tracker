package com.smsfinance.services;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.smsfinance.database.DatabaseHelper;
import com.smsfinance.database.Transaction;
import com.smsfinance.utils.SheetsHelper;

import java.util.List;

public class SyncWorker extends Worker {
    private static final String TAG = "SyncWorker";

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Starting sync work");
        
        try {
            DatabaseHelper db = DatabaseHelper.getInstance(getApplicationContext());
            List<Transaction> unsynced = db.getUnsyncedTransactions();
            
            if (unsynced.isEmpty()) {
                Log.d(TAG, "No unsynced transactions");
                return Result.success();
            }
            
            Log.d(TAG, "Found " + unsynced.size() + " unsynced transactions");
            
            // Initialize Google Sheets
            SheetsHelper sheets = new SheetsHelper(getApplicationContext());
            if (!sheets.initialize()) {
                Log.e(TAG, "Failed to initialize Sheets service");
                return Result.retry();
            }
            
            // Sync each transaction
            int syncedCount = 0;
            for (Transaction t : unsynced) {
                if (sheets.appendTransaction(t)) {
                    db.markAsSynced(t.getId());
                    syncedCount++;
                    Log.d(TAG, "Synced transaction ID: " + t.getId());
                } else {
                    Log.e(TAG, "Failed to sync transaction ID: " + t.getId());
                }
            }
            
            Log.d(TAG, "Sync complete. Synced: " + syncedCount + "/" + unsynced.size());
            
            return syncedCount == unsynced.size() ? Result.success() : Result.retry();
            
        } catch (Exception e) {
            Log.e(TAG, "Sync failed with exception", e);
            return Result.retry();
        }
    }
}
