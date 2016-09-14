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

import com.synappze.stoptimer.R;
import com.synappze.stoptimer.Timer.Timers;


public class TimerContentProvider extends ContentProvider {

    //private static final String TAG = "TimerContentProvider";

    private static final String DATABASE_NAME = "timers.db";

    private static final int DATABASE_VERSION = 2;

    private static final String TABLE_NAME = "timers";

    public static final String AUTHORITY = "com.synappze.stoptimer.providers.TimerContentProvider";

    private static final UriMatcher sUriMatcher;

    private static final int TIMERS = 1;

    public static HashMap<String, String> timersProjectionMap;

    private static class DatabaseHelper extends SQLiteOpenHelper {

    	private Context context;
    	
        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        	this.context=context;
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + TABLE_NAME 
            		+ " ( " + Timers.TIMER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," 
            		+        Timers.TITLE + " VARCHAR(255), "
            		+        Timers.START_TIME + " INTEGER, "
            		+        Timers.STOP_TIME + " INTEGER, "
            		+        Timers.RUNNING + " BOOLEAN, "
            		+        Timers.INTERVAL + " INTEGER, "
            		+        Timers.INTERVAL_PARENT_ID + " INTEGER, "
            		+        Timers.REMAINING_TIME + " INTEGER, "
            		+        Timers.LENGTH + " INTEGER, "
            		+        Timers.REPEAT + " BOOLEAN, "
            		+        Timers.NUM_BEEPS + " INTEGER, "
            		+        Timers.VIBRATE + " BOOLEAN, "
            		+        Timers.BEEP + " TEXT "
                    + " ); ");
            db.execSQL("CREATE INDEX "+Timers.INTERVAL_PARENT_ID+"_idx on " + TABLE_NAME + " ("+Timers.INTERVAL_PARENT_ID+");");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        	if(oldVersion==1 && newVersion==2) {
        		db.execSQL("ALTER TABLE " + TABLE_NAME
            			+ " ADD COLUMN " + Timers.BEEP + " TEXT; " );
        		db.execSQL("UPDATE " + TABLE_NAME
            			+ " SET " + Timers.BEEP + " = '"+context.getString(R.string.timerDefaultBeepPrefDefaultValue)+"';" );
                db.execSQL("CREATE INDEX "+Timers.INTERVAL_PARENT_ID+"_idx on " + TABLE_NAME + " ("+Timers.INTERVAL_PARENT_ID+");");
            }
        	else {
        		db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        		onCreate(db);
        	}
            //Log.d("TimerContentProvider","Upgrading from:"+oldVersion+" to:"+newVersion);
        }
    }

    private DatabaseHelper dbHelper;

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int count;
        switch (sUriMatcher.match(uri)) {
            case TIMERS:
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
            case TIMERS:
                return Timers.CONTENT_TYPE;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        if (sUriMatcher.match(uri) != TIMERS) { throw new IllegalArgumentException("Unknown URI " + uri); }

        ContentValues values;
        if (initialValues != null) {
            values = new ContentValues(initialValues);
        } else {
            values = new ContentValues();
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long rowId = db.insert(TABLE_NAME, null, values);
        if (rowId > 0) {
            Uri timersUri = ContentUris.withAppendedId(Timers.CONTENT_URI, rowId);
            getContext().getContentResolver().notifyChange(timersUri, null);
            return timersUri;
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
            case TIMERS:
                qb.setTables(TABLE_NAME);
                qb.setProjectionMap(timersProjectionMap);
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);

        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int count;
        switch (sUriMatcher.match(uri)) {
            case TIMERS:
                count = db.update(TABLE_NAME, values, where, whereArgs);
                if(values.containsKey(Timers.TITLE)) {
            		ContentValues titleValue = new ContentValues();
            		titleValue.put(Timers.TITLE, values.getAsString(Timers.TITLE));
                    count = db.update(TABLE_NAME, titleValue, Timers.INTERVAL_PARENT_ID+"="+values.getAsInteger(Timers.INTERVAL_PARENT_ID), null);
                }
                if(values.containsKey(Timers.REPEAT)) {
            		ContentValues repeatValue = new ContentValues();
            		repeatValue.put(Timers.REPEAT, values.getAsBoolean(Timers.REPEAT));
                    count = db.update(TABLE_NAME, repeatValue, Timers.INTERVAL_PARENT_ID+"="+values.getAsInteger(Timers.INTERVAL_PARENT_ID), null);
                }
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }
    
    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(AUTHORITY, TABLE_NAME, TIMERS);

        timersProjectionMap = new HashMap<String, String>();
        timersProjectionMap.put(Timers.TIMER_ID, Timers.TIMER_ID);
        timersProjectionMap.put(Timers.TITLE, Timers.TITLE);
        timersProjectionMap.put(Timers.START_TIME, Timers.START_TIME);
        timersProjectionMap.put(Timers.STOP_TIME, Timers.STOP_TIME);
        timersProjectionMap.put(Timers.RUNNING, Timers.RUNNING);
        timersProjectionMap.put(Timers.INTERVAL, Timers.INTERVAL);
        timersProjectionMap.put(Timers.INTERVAL_PARENT_ID, Timers.INTERVAL_PARENT_ID);
        timersProjectionMap.put(Timers.REMAINING_TIME, Timers.REMAINING_TIME);
        timersProjectionMap.put(Timers.LENGTH, Timers.LENGTH);
        timersProjectionMap.put(Timers.REPEAT, Timers.REPEAT);
        timersProjectionMap.put(Timers.NUM_BEEPS, Timers.NUM_BEEPS);
        timersProjectionMap.put(Timers.VIBRATE, Timers.VIBRATE);
        timersProjectionMap.put(Timers.BEEP, Timers.BEEP);
    }
    
}

