package com.android.contacts.numberarea;

import com.android.contacts.numberarea.NumberArea.Tables;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.util.Log;

public class NumberAreaProvider extends ContentProvider {

    private static final String TAG = NumberAreaProvider.class.getSimpleName();

    // Database instance
    private static SQLiteDatabase mSQLiteDatabase = null;
    private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    private static final int AREA = 1;

    private static final int PHONE_NUMBER = 2;

    private static final int PHONE_NUMBER_AREA = 3;
    static {
        sURIMatcher.addURI(NumberArea.AUTHORITY, "area", AREA);
        sURIMatcher.addURI(NumberArea.AUTHORITY, "phone_number", PHONE_NUMBER);
        sURIMatcher.addURI(NumberArea.AUTHORITY, "phone_number_area", PHONE_NUMBER_AREA);
    }

    public void init() throws SQLException {
        if (mSQLiteDatabase == null) {
            try {
                mSQLiteDatabase = SQLiteDatabase.openDatabase(NumberArea.DB_DIR + NumberArea.DB_FILE, null,
                        SQLiteDatabase.OPEN_READONLY);
            } catch (SQLiteException e) {
                Log.e(TAG, "number area database open fail.");
            }
        }
    }

    @Override
    public boolean onCreate() {
        init();
        return mSQLiteDatabase != null;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        if (mSQLiteDatabase == null) {
            return null;
        }
        int match = sURIMatcher.match(uri);
        String table;
        switch (match) {
            case AREA:
                table = Tables.AREA;
                break;

            case PHONE_NUMBER:
                table = Tables.PHONE_NUMBER;
                break;

            case PHONE_NUMBER_AREA:
                table = Tables.PHONE_NUMBER_JOIN_AREA;
                break;
            default:
                return null;
        }
        return mSQLiteDatabase.query(table, projection, selection, selectionArgs, null, null, null);
    }

    @Override
    public String getType(Uri uri) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        return 0;
    }

    
}
