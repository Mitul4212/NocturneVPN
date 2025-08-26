package com.example.nocturnevpn.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.nocturnevpn.model.History;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class HistoryHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "history.db";
    private static HistoryHelper instance;

    public HistoryHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static synchronized HistoryHelper getInstance(Context context) {
        if (instance == null) {
            instance = new HistoryHelper(context.getApplicationContext());
        }
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(HistoryDatabase.SQL_CREATE_HISTORY);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(HistoryDatabase.SQL_DELETE_HISTORY);
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    public void addHistory(History history) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = getContentValues(history);
        db.insert(HistoryContract.HistoryEntry.TABLE_NAME, null, values);
    }

    public List<History> getRecentHistory(int limit) {
        List<History> historyList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        Cursor cursor = db.query(
                HistoryContract.HistoryEntry.TABLE_NAME,
                HistoryContract.HistoryEntry.ALL_COLUMNS,
                null, null, null, null,
                HistoryContract.HistoryEntry.COLUMN_NAME_CONNECTION_DATE + " DESC",
                String.valueOf(limit)
        );

        if (cursor != null && cursor.moveToFirst()) {
            do {
                historyList.add(cursorToHistory(cursor));
            } while (cursor.moveToNext());
            cursor.close();
        }

        return historyList;
    }

    public List<History> getAllHistory() {
        List<History> historyList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        Cursor cursor = db.query(
                HistoryContract.HistoryEntry.TABLE_NAME,
                HistoryContract.HistoryEntry.ALL_COLUMNS,
                null, null, null, null,
                HistoryContract.HistoryEntry.COLUMN_NAME_CONNECTION_DATE + " DESC"
        );

        if (cursor != null && cursor.moveToFirst()) {
            do {
                historyList.add(cursorToHistory(cursor));
            } while (cursor.moveToNext());
            cursor.close();
        }

        return historyList;
    }

    public void clearHistory() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(HistoryContract.HistoryEntry.TABLE_NAME, null, null);
    }

    public void deleteHistoryById(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(HistoryContract.HistoryEntry.TABLE_NAME,
                HistoryContract.HistoryEntry._ID + " = ?",
                new String[]{String.valueOf(id)});
    }

    private ContentValues getContentValues(History history) {
        ContentValues values = new ContentValues();
        values.put(HistoryContract.HistoryEntry.COLUMN_NAME_SERVER_NAME, history.getServerName());
        values.put(HistoryContract.HistoryEntry.COLUMN_NAME_SERVER_COUNTRY, history.getServerCountry());
        values.put(HistoryContract.HistoryEntry.COLUMN_NAME_SERVER_IP, history.getServerIp());
        values.put(HistoryContract.HistoryEntry.COLUMN_NAME_CONNECTION_DATE, history.getConnectionDate().getTime());
        values.put(HistoryContract.HistoryEntry.COLUMN_NAME_DURATION, history.getDuration());
        values.put(HistoryContract.HistoryEntry.COLUMN_NAME_STATUS, history.getStatus());
        values.put(HistoryContract.HistoryEntry.COLUMN_NAME_DATA_USED, history.getDataUsed());
        return values;
    }

    private History cursorToHistory(Cursor cursor) {
        History history = new History();
        history.setId(cursor.getInt(0));
        history.setServerName(cursor.getString(1));
        history.setServerCountry(cursor.getString(2));
        history.setServerIp(cursor.getString(3));
        history.setConnectionDate(new Date(cursor.getLong(4)));
        history.setDuration(cursor.getLong(5));
        history.setStatus(cursor.getString(6));
        history.setDataUsed(cursor.getLong(7));
        return history;
    }
} 