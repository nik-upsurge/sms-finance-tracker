package com.smsfinance;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.smsfinance.database.DatabaseHelper;
import com.smsfinance.services.SyncWorker;

import java.text.NumberFormat;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.POST_NOTIFICATIONS
    };

    private TextView statusText;
    private TextView todayCount, weekCount, monthCount;
    private TextView todayTotal, weekTotal, monthTotal;
    private TextView totalTransactions;
    private View statusIndicator;
    
    private Handler refreshHandler;
    private Runnable refreshRunnable;
    private DatabaseHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initViews();
        db = DatabaseHelper.getInstance(this);
        
        checkPermissions();
        setupAutoRefresh();
    }

    private void initViews() {
        statusText = findViewById(R.id.statusText);
        statusIndicator = findViewById(R.id.statusIndicator);
        
        todayCount = findViewById(R.id.todayCount);
        weekCount = findViewById(R.id.weekCount);
        monthCount = findViewById(R.id.monthCount);
        
        todayTotal = findViewById(R.id.todayTotal);
        weekTotal = findViewById(R.id.weekTotal);
        monthTotal = findViewById(R.id.monthTotal);
        
        totalTransactions = findViewById(R.id.totalTransactions);
        
        findViewById(R.id.syncButton).setOnClickListener(v -> triggerManualSync());
        findViewById(R.id.refreshButton).setOnClickListener(v -> updateStats());
    }

    private void checkPermissions() {
        boolean allGranted = true;
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (!allGranted) {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSION_REQUEST_CODE);
        } else {
            updateStatus(true);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean smsGranted = false;
            for (int i = 0; i < permissions.length; i++) {
                if (permissions[i].equals(Manifest.permission.RECEIVE_SMS) && 
                    grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    smsGranted = true;
                    break;
                }
            }
            
            if (smsGranted) {
                updateStatus(true);
                Toast.makeText(this, "SMS permission granted. App is now active.", Toast.LENGTH_SHORT).show();
            } else {
                updateStatus(false);
                Toast.makeText(this, "SMS permission required for the app to work.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void updateStatus(boolean active) {
        if (active) {
            statusText.setText("● Active - Listening for HDFC SMS");
            statusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_light));
            statusIndicator.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
        } else {
            statusText.setText("● Inactive - Permission Required");
            statusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_light));
            statusIndicator.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
        }
    }

    private void setupAutoRefresh() {
        refreshHandler = new Handler(Looper.getMainLooper());
        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                updateStats();
                refreshHandler.postDelayed(this, 30000); // Refresh every 30 seconds
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStats();
        refreshHandler.postDelayed(refreshRunnable, 30000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        refreshHandler.removeCallbacks(refreshRunnable);
    }

    private void updateStats() {
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        
        // Transaction counts
        todayCount.setText(String.valueOf(db.getTodayCount()));
        weekCount.setText(String.valueOf(db.getWeekCount()));
        monthCount.setText(String.valueOf(db.getMonthCount()));
        
        // Spending totals
        todayTotal.setText(currencyFormat.format(db.getTodayTotal()));
        weekTotal.setText(currencyFormat.format(db.getWeekTotal()));
        monthTotal.setText(currencyFormat.format(db.getMonthTotal()));
        
        // Total count
        totalTransactions.setText("Total: " + db.getTotalTransactionCount() + " transactions tracked");
    }

    private void triggerManualSync() {
        Toast.makeText(this, "Syncing to Google Sheets...", Toast.LENGTH_SHORT).show();
        
        OneTimeWorkRequest syncRequest = new OneTimeWorkRequest.Builder(SyncWorker.class).build();
        WorkManager.getInstance(this).enqueue(syncRequest);
    }
}
