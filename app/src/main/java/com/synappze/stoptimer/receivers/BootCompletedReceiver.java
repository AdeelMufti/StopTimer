package com.synappze.stoptimer.receivers;

import java.util.Date;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;

import com.synappze.stoptimer.MainActivity;
import com.synappze.stoptimer.Timer;
import com.synappze.stoptimer.Timer.Timers;

public class BootCompletedReceiver extends BroadcastReceiver {

	public static final Uri URI = Timer.Timers.CONTENT_URI;
	
	@Override
	public void onReceive(Context context, Intent intent) {
		Cursor cursor, subCursor;
		Timer timer;
		ContentValues contentValues;
		long cumulativeTimeElapsed;
		int prevTimerId;
		Timer prevTimer;
		long prevTimerStopTime;
		int nextTimerId;
		Timer nextTimer;
		int findNextIntervalIteration;
		
		//SimpleDateFormat sdf = new SimpleDateFormat(context.getResources().getString(R.string.date_time_format));
		
        cursor = context.getContentResolver().query(
    			URI, 
				new String[] {Timers.TIMER_ID}, 
				Timers.RUNNING+"=1", 
    			null, 
    			null
    		);
        for(cursor.moveToFirst();!cursor.isAfterLast();cursor.moveToNext()) {
        	timer=Timer.fetchTimerFromDB(context, URI, cursor.getInt(cursor.getColumnIndex(Timers.TIMER_ID)));
        	if(System.currentTimeMillis()<timer.stopTime) {
        		//Log.d("here","here"+timer.stopTime+":"+System.currentTimeMillis());
        		Timer.addAlarmManager(context, timer);
        	}
        	else {
	    		contentValues = new ContentValues();
	    		contentValues.put(Timer.Timers.START_TIME, 0);
	    		contentValues.put(Timer.Timers.STOP_TIME, 0);
	    		contentValues.put(Timer.Timers.RUNNING, false);
	    		contentValues.put(Timer.Timers.REMAINING_TIME, 0);
	    		context.getContentResolver().update(URI, contentValues, Timers.TIMER_ID+"="+timer.id, null);
        		
	            cumulativeTimeElapsed=timer.stopTime;
	            prevTimerId=timer.id;
	            prevTimerStopTime=timer.stopTime;
	            nextTimerId=0;
	            findNextIntervalIteration=0;
	    		subCursor = context.getContentResolver().query(
	        			URI, 
	        			null,
	        			Timers.INTERVAL_PARENT_ID+"="+timer.intervalParentId+" and "+Timers.LENGTH+"<>0",
	        			null, 
	        			Timers.INTERVAL
	        		);
	    		//Log.d("BootCompletedReceiver","subCursorCount="+subCursor.getCount());
	    		
	            //for(subCursor.moveToFirst();!subCursor.isAfterLast();subCursor.moveToNext()) {
	            for(int i=0; i<subCursor.getCount(); i++) {
	            	subCursor.moveToPosition(i);
	            	//Log.d("BootCompletedReceiver","title="+subCursor.getString(subCursor.getColumnIndex(Timers.TITLE))+", interval="+subCursor.getInt(subCursor.getColumnIndex(Timers.INTERVAL)));
	            	if(findNextIntervalIteration==0) {
	            		//Log.d("BootCompletedReceiver","timer.id="+timer.id+", subCursorTimerId="+subCursor.getInt(subCursor.getColumnIndex(Timers.TIMER_ID)));
	            		if(subCursor.getInt(subCursor.getColumnIndex(Timers.TIMER_ID))==timer.id)
	            			findNextIntervalIteration=1;
		            	if(timer.repeat && subCursor.isLast())
		            		i=-1;
	            	}
	            	else {
		            	cumulativeTimeElapsed+=subCursor.getLong(subCursor.getColumnIndex(Timers.LENGTH));
		            	//Log.d("BootCompletedReceiver","cumulativeTimeElapsed="+sdf.format(cumulativeTimeElapsed)+", title="+subCursor.getString(subCursor.getColumnIndex(Timers.TITLE))+", interval="+subCursor.getInt(subCursor.getColumnIndex(Timers.INTERVAL)));
		            	if(System.currentTimeMillis()<cumulativeTimeElapsed) {
		            		nextTimerId=subCursor.getInt(subCursor.getColumnIndex(Timers.TIMER_ID));
		            		break;
		            	}
		            	prevTimerId=subCursor.getInt(subCursor.getColumnIndex(Timers.TIMER_ID));
		            	prevTimerStopTime+=subCursor.getLong(subCursor.getColumnIndex(Timers.LENGTH));
		            	if(timer.repeat && subCursor.isLast())
		            		i=-1;
	            	}
	            }
	            subCursor.close();

	            //start nextTimer, if any, at the correct remaining time location!
	            if(nextTimerId!=0) {
	            	nextTimer=Timer.fetchTimerFromDB(context, URI, nextTimerId);
	            	nextTimer.startTime=cumulativeTimeElapsed-nextTimer.length;
	            	nextTimer.stopTime=cumulativeTimeElapsed;
	            	nextTimer.running=true;
	            	Timer.updateTimerRecord(false, false, nextTimer, context, URI);
	            	Timer.addAlarmManager(context, nextTimer);
	            	//Log.d("BootCompletedReceiver","Next timer: Title="+nextTimer.title+",startTime="+sdf.format(nextTimer.startTime)+", stopTime="+sdf.format(nextTimer.stopTime));
	            }
	            
        		//add notification prevTimer expired
	            prevTimer=Timer.fetchTimerFromDB(context, URI, prevTimerId);
	            Timer.putTimerExpiredNotification(context, prevTimer.title, prevTimer.interval, prevTimer.intervalParentId, new Date(prevTimerStopTime));

        	}
        }
        cursor.close();

        MainActivity.addOrRemoveMainNotification(context,false);
		
	}
	

}
