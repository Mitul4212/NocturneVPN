package com.nocturnevpn.db;

public class HistoryDatabase {
    private HistoryDatabase() {}

    static final String SQL_CREATE_HISTORY =
            "CREATE TABLE " + HistoryContract.HistoryEntry.TABLE_NAME + " (" +
                    HistoryContract.HistoryEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    HistoryContract.HistoryEntry.COLUMN_NAME_SERVER_NAME + " TEXT," +
                    HistoryContract.HistoryEntry.COLUMN_NAME_SERVER_COUNTRY + " TEXT," +
                    HistoryContract.HistoryEntry.COLUMN_NAME_SERVER_IP + " TEXT," +
                    HistoryContract.HistoryEntry.COLUMN_NAME_CONNECTION_DATE + " INTEGER," +
                    HistoryContract.HistoryEntry.COLUMN_NAME_DURATION + " INTEGER," +
                    HistoryContract.HistoryEntry.COLUMN_NAME_STATUS + " TEXT," +
                    HistoryContract.HistoryEntry.COLUMN_NAME_DATA_USED + " INTEGER" +
                    ")";

    static final String SQL_DELETE_HISTORY =
            "DROP TABLE IF EXISTS " + HistoryContract.HistoryEntry.TABLE_NAME;
} 