package com.nocturnevpn.db;

import android.provider.BaseColumns;

public class HistoryContract {
    public HistoryContract() {}

    static abstract class HistoryEntry implements BaseColumns {
        static final String TABLE_NAME = "connection_history";
        static final String COLUMN_NAME_SERVER_NAME = "server_name";
        static final String COLUMN_NAME_SERVER_COUNTRY = "server_country";
        static final String COLUMN_NAME_SERVER_IP = "server_ip";
        static final String COLUMN_NAME_CONNECTION_DATE = "connection_date";
        static final String COLUMN_NAME_DURATION = "duration";
        static final String COLUMN_NAME_STATUS = "status";
        static final String COLUMN_NAME_DATA_USED = "data_used";

        static final String[] ALL_COLUMNS = {
                _ID,
                COLUMN_NAME_SERVER_NAME,
                COLUMN_NAME_SERVER_COUNTRY,
                COLUMN_NAME_SERVER_IP,
                COLUMN_NAME_CONNECTION_DATE,
                COLUMN_NAME_DURATION,
                COLUMN_NAME_STATUS,
                COLUMN_NAME_DATA_USED
        };
    }
} 