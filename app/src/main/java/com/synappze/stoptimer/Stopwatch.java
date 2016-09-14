package com.synappze.stoptimer;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;

import com.synappze.stoptimer.providers.StopwatchContentProvider;

public class Stopwatch {

	public int id = 0;
    public long startTime = 0;
    public long stopTime = 0;
    public boolean running = false;
    public boolean resume = false;
    public String title = null;

    public void start() {
    	if(startTime==0)
    		//startTime = System.currentTimeMillis()-86340000;
    		startTime = System.currentTimeMillis();
    	else
    		startTime = System.currentTimeMillis() - (stopTime - startTime);
        running = true;
    }

    public void reset() {
    	startTime=0;
    	stopTime=0;
    	running=false;
    	resume=false;
    }
    
    public void stop() {
    	stopTime = System.currentTimeMillis();
        running = false;
    }

    public long getElapsedTime() {
        long elapsed;
        if (running) {
             elapsed = (System.currentTimeMillis() - startTime);
        }
        else {
            elapsed = (stopTime - startTime);
        }
        return elapsed;
    }
    
	public static void stopAllStopwatches(Context context, Uri URI) {
		Cursor cursor;
		Stopwatch stopWatch;
		
		cursor = context.getContentResolver().query(
				URI, 
				new String[] {Stopwatches.STOPWATCH_ID}, 
				Stopwatches.RUNNING+"=1", 
				null, 
				null
			);
		for(cursor.moveToFirst();!cursor.isAfterLast();cursor.moveToNext()) {
			stopWatch=fetchStopwatchFromDB(context,URI,cursor.getInt(cursor.getColumnIndex(Stopwatches.STOPWATCH_ID)));
			stopWatch.stop();
			updateStopwatchRecord(stopWatch,context,URI);

		}
		cursor.close();
	}
	
	public static void updateStopwatchRecord(Stopwatch stopWatch, Context context, Uri URI) {
		ContentValues contentValues;

		contentValues = new ContentValues();
		contentValues.put(Stopwatch.Stopwatches.TITLE, stopWatch.title);
		contentValues.put(Stopwatch.Stopwatches.START_TIME, stopWatch.startTime);
		contentValues.put(Stopwatch.Stopwatches.STOP_TIME, stopWatch.stopTime);
		contentValues.put(Stopwatch.Stopwatches.RUNNING, (stopWatch.running?1:0));

		context.getContentResolver().update(URI, contentValues, Stopwatches.STOPWATCH_ID+"="+stopWatch.id, null);
	}


    public static Stopwatch fetchStopwatchFromDB(Context context, Uri URI, int stopWatchId) {
    	Stopwatch stopWatch = null;
    	
    	Cursor cursor = context.getContentResolver().query(
    			URI, 
				null, 
				Stopwatches.STOPWATCH_ID+"="+stopWatchId, 
				null, 
				null);
		
		if(cursor.moveToFirst()) {
			stopWatch=new Stopwatch();
			stopWatch.id=cursor.getInt(cursor.getColumnIndex(Stopwatches.STOPWATCH_ID));
			stopWatch.title=cursor.getString(cursor.getColumnIndex(Stopwatches.TITLE));
			stopWatch.startTime=cursor.getLong(cursor.getColumnIndex(Stopwatches.START_TIME));
			stopWatch.stopTime=cursor.getLong(cursor.getColumnIndex(Stopwatches.STOP_TIME));
			stopWatch.running=(cursor.getInt(cursor.getColumnIndex(Stopwatches.RUNNING))==0?false:true);
		}
		cursor.close();
    	
    	return stopWatch;
    }

    
	public static class Stopwatches implements BaseColumns {
		private Stopwatches() {
		}

		public static final Uri CONTENT_URI = Uri.parse("content://"
				+ StopwatchContentProvider.AUTHORITY + "/stopwatches");

		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.synappze.stoptimer";
		
		public static final String STOPWATCH_ID = "_id";

		public static final String TITLE = "title";

	    public static final String START_TIME = "start_time";
	    
	    public static final String STOP_TIME = "stop_time";
	    
	    public static final String RUNNING = "running";

	}

    
}