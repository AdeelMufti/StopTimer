package com.synappze.stoptimer.providers;

import java.util.HashMap;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

import com.synappze.stoptimer.Stopwatch.Stopwatches;


public class StopwatchContentProvider extends ContentProvider {

    //private static final String TAG = "StopwatchContentProvider";

    private static final String DATABASE_NAME = "stopwatches.db";

    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_NAME = "stopwatches";

    public static final String AUTHORITY = "com.synappze.stoptimer.providers.StopwatchContentProvider";

    private static final UriMatcher sUriMatcher;

    private static final int STOPWATCHES = 1;

    public static HashMap<String, String> stopWatchesProjectionMap;

    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + TABLE_NAME 
            		+ " ( " + Stopwatches.STOPWATCH_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," 
            		+        Stopwatches.TITLE + " VARCHAR(255), "
            		+        Stopwatches.START_TIME + " INTEGER, "
            		+        Stopwatches.STOP_TIME + " INTEGER, "
            		+        Stopwatches.RUNNING + " BOOLEAN "
                    + " ); ");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
            onCreate(db);
        }
    }

    private DatabaseHelper dbHelper;

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int count;
        switch (sUriMatcher.match(uri)) {
            case STOPWATCHES:
                count = db.delete(TABLE_NAME, where, whereArgs);
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case STOPWATCHES:
                return Stopwatches.CONTENT_TYPE;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        if (sUriMatcher.match(uri) != STOPWATCHES) { throw new IllegalArgumentException("Unknown URI " + uri); }

        ContentValues values;
        if (initialValues != null) {
            values = new ContentValues(initialValues);
        } else {
            values = new ContentValues();
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long rowId = db.insert(TABLE_NAME, null, values);
        if (rowId > 0) {
            Uri stopWatchesUri = ContentUris.withAppendedId(Stopwatches.CONTENT_URI, rowId);
            getContext().getContentResolver().notifyChange(stopWatchesUri, null);
            return stopWatchesUri;
        }

        throw new SQLException("Failed to insert row into " + uri);
    }

    @Override
    public boolean onCreate() {
        dbHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        switch (sUriMatcher.match(uri)) {
            case STOPWATCHES:
                qb.setTables(TABLE_NAME);
                qb.setProjectionMap(stopWatchesProjectionMap);
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);

        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int count;
        switch (sUriMatcher.match(uri)) {
            case STOPWATCHES:
                count = db.update(TABLE_NAME, values, where, whereArgs);
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(AUTHORITY, TABLE_NAME, STOPWATCHES);

        stopWatchesProjectionMap = new HashMap<String, String>();
        stopWatchesProjectionMap.put(Stopwatches.STOPWATCH_ID, Stopwatches.STOPWATCH_ID);
        stopWatchesProjectionMap.put(Stopwatches.TITLE, Stopwatches.TITLE);
        stopWatchesProjectionMap.put(Stopwatches.START_TIME, Stopwatches.START_TIME);
        stopWatchesProjectionMap.put(Stopwatches.STOP_TIME, Stopwatches.STOP_TIME);
        stopWatchesProjectionMap.put(Stopwatches.RUNNING, Stopwatches.RUNNING);
    }
}

