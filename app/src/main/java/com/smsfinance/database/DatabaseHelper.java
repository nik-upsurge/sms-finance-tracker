package com.smsfinance.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "sms_finance.db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_TRANSACTIONS = "transactions";
    private static final String COL_ID = "id";
    private static final String COL_AMOUNT = "amount";
    private static final String COL_TYPE = "type";
    private static final String COL_MERCHANT = "merchant";
    private static final String COL_CATEGORY = "category";
    private static final String COL_REFERENCE = "reference";
    private static final String COL_TRANSACTION_DATE = "transaction_date";
    private static final String COL_RAW_SMS = "raw_sms";
    private static final String COL_SYNCED = "synced";
    private static final String COL_CREATED_AT = "created_at";

    private static DatabaseHelper instance;

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    private DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_TRANSACTIONS + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_AMOUNT + " REAL NOT NULL, " +
                COL_TYPE + " TEXT NOT NULL, " +
                COL_MERCHANT + " TEXT, " +
                COL_CATEGORY + " TEXT, " +
                COL_REFERENCE + " TEXT, " +
                COL_TRANSACTION_DATE + " TEXT, " +
                COL_RAW_SMS + " TEXT, " +
                COL_SYNCED + " INTEGER DEFAULT 0, " +
                COL_CREATED_AT + " TEXT DEFAULT CURRENT_TIMESTAMP)";
        db.execSQL(createTable);

        // Index for faster queries
        db.execSQL("CREATE INDEX idx_transaction_date ON " + TABLE_TRANSACTIONS + "(" + COL_TRANSACTION_DATE + ")");
        db.execSQL("CREATE INDEX idx_synced ON " + TABLE_TRANSACTIONS + "(" + COL_SYNCED + ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TRANSACTIONS);
        onCreate(db);
    }

    public long insertTransaction(Transaction transaction) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_AMOUNT, transaction.getAmount());
        values.put(COL_TYPE, transaction.getType());
        values.put(COL_MERCHANT, transaction.getMerchant());
        values.put(COL_CATEGORY, transaction.getCategory());
        values.put(COL_REFERENCE, transaction.getReference());
        values.put(COL_TRANSACTION_DATE, transaction.getTransactionDate());
        values.put(COL_RAW_SMS, transaction.getRawSms());
        values.put(COL_SYNCED, transaction.isSynced() ? 1 : 0);
        
        return db.insert(TABLE_TRANSACTIONS, null, values);
    }

    public List<Transaction> getUnsyncedTransactions() {
        List<Transaction> transactions = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        Cursor cursor = db.query(TABLE_TRANSACTIONS, null, COL_SYNCED + " = 0", 
                null, null, null, COL_ID + " ASC");

        if (cursor.moveToFirst()) {
            do {
                transactions.add(cursorToTransaction(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return transactions;
    }

    public void markAsSynced(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_SYNCED, 1);
        db.update(TABLE_TRANSACTIONS, values, COL_ID + " = ?", new String[]{String.valueOf(id)});
    }

    public int getTodayCount() {
        return getCountForPeriod(0);
    }

    public int getWeekCount() {
        return getCountForPeriod(7);
    }

    public int getMonthCount() {
        return getCountForPeriod(30);
    }

    public double getTodayTotal() {
        return getTotalForPeriod(0);
    }

    public double getWeekTotal() {
        return getTotalForPeriod(7);
    }

    public double getMonthTotal() {
        return getTotalForPeriod(30);
    }

    private int getCountForPeriod(int daysBack) {
        SQLiteDatabase db = this.getReadableDatabase();
        String dateCondition = getDateCondition(daysBack);
        
        Cursor cursor = db.rawQuery(
            "SELECT COUNT(*) FROM " + TABLE_TRANSACTIONS + " WHERE " + dateCondition,
            null
        );
        
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }

    private double getTotalForPeriod(int daysBack) {
        SQLiteDatabase db = this.getReadableDatabase();
        String dateCondition = getDateCondition(daysBack);
        
        Cursor cursor = db.rawQuery(
            "SELECT SUM(amount) FROM " + TABLE_TRANSACTIONS + 
            " WHERE " + COL_TYPE + " = 'debit' AND " + dateCondition,
            null
        );
        
        double total = 0;
        if (cursor.moveToFirst() && !cursor.isNull(0)) {
            total = cursor.getDouble(0);
        }
        cursor.close();
        return total;
    }

    private String getDateCondition(int daysBack) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        Calendar cal = Calendar.getInstance();
        
        if (daysBack == 0) {
            // Today only
            String today = sdf.format(cal.getTime());
            return COL_CREATED_AT + " LIKE '" + today + "%'";
        } else {
            cal.add(Calendar.DAY_OF_YEAR, -daysBack);
            String startDate = sdf.format(cal.getTime());
            return COL_CREATED_AT + " >= '" + startDate + "'";
        }
    }

    public int getTotalTransactionCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_TRANSACTIONS, null);
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }

    public double getCurrentBalance() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
            "SELECT SUM(CASE WHEN type = 'credit' THEN amount ELSE -amount END) FROM " + TABLE_TRANSACTIONS,
            null
        );
        double balance = 0;
        if (cursor.moveToFirst() && !cursor.isNull(0)) {
            balance = cursor.getDouble(0);
        }
        cursor.close();
        return balance;
    }

    private Transaction cursorToTransaction(Cursor cursor) {
        Transaction t = new Transaction();
        t.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID)));
        t.setAmount(cursor.getDouble(cursor.getColumnIndexOrThrow(COL_AMOUNT)));
        t.setType(cursor.getString(cursor.getColumnIndexOrThrow(COL_TYPE)));
        t.setMerchant(cursor.getString(cursor.getColumnIndexOrThrow(COL_MERCHANT)));
        t.setCategory(cursor.getString(cursor.getColumnIndexOrThrow(COL_CATEGORY)));
        t.setReference(cursor.getString(cursor.getColumnIndexOrThrow(COL_REFERENCE)));
        t.setTransactionDate(cursor.getString(cursor.getColumnIndexOrThrow(COL_TRANSACTION_DATE)));
        t.setRawSms(cursor.getString(cursor.getColumnIndexOrThrow(COL_RAW_SMS)));
        t.setSynced(cursor.getInt(cursor.getColumnIndexOrThrow(COL_SYNCED)) == 1);
        t.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow(COL_CREATED_AT)));
        return t;
    }
}
