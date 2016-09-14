package com.synappze.stoptimer;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.BaseColumns;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.synappze.stoptimer.providers.TimerContentProvider;
import com.synappze.stoptimer.receivers.TimerExpiryBroadcastReceiver;


public class Timer {

	public int id = 0;
	public int intervalParentId = 0;
	public int interval = 0;
    public long startTime = 0;
	public long stopTime = 0;
    public long length = 0;
    public long remainingTime = 0;
    public boolean running = false;
    public boolean resume = false;
    public String title = null;
    public boolean repeat = false;
    public int numBeeps = 0;
    public boolean vibrate = false;
    public String beep = "";
    
	public Timer() {
	}
	
    public void start() {
   		startTime=System.currentTimeMillis();
    	if(stopTime==0)
   	    	stopTime = System.currentTimeMillis() + length;
   		else
   			stopTime = System.currentTimeMillis() + remainingTime;
        running = true;
    }
    
    public void stop() {
   		remainingTime = stopTime - System.currentTimeMillis();
        running = false;
    }
    
    public void reset() {
    	startTime=0;
    	stopTime=0;
    	remainingTime = 0;
    	running=false;
    	resume=false;
    }
    
    public long getElapsedTime() {
        long elapsed;
        if(running && System.currentTimeMillis()>=stopTime) {
        	elapsed=0;
        	reset();
        }
        else if(!running && stopTime!=0)
        	elapsed=remainingTime;
        else if(!running && stopTime==0)
        	elapsed=0;
        else
            elapsed = (stopTime - System.currentTimeMillis());
        return elapsed;
    }
    
    public static Timer fetchTimerFromDB(Context context, Uri URI, int timerId) {
    	Timer timer = null;
    	
    	Cursor cursor = context.getContentResolver().query(
    			URI, 
				null, 
				Timers.TIMER_ID+"="+timerId, 
				null, 
				null);
		
		if(cursor.moveToFirst()) {
			timer=new Timer();
			timer.id=cursor.getInt(cursor.getColumnIndex(Timers.TIMER_ID));
			timer.title=cursor.getString(cursor.getColumnIndex(Timers.TITLE));
			timer.startTime=cursor.getLong(cursor.getColumnIndex(Timers.START_TIME));
			timer.stopTime=cursor.getLong(cursor.getColumnIndex(Timers.STOP_TIME));
			timer.running=(cursor.getInt(cursor.getColumnIndex(Timers.RUNNING))==0?false:true);
			timer.interval=cursor.getInt(cursor.getColumnIndex(Timers.INTERVAL));
			timer.intervalParentId=cursor.getInt(cursor.getColumnIndex(Timers.INTERVAL_PARENT_ID));
			timer.remainingTime=cursor.getLong(cursor.getColumnIndex(Timers.REMAINING_TIME));
			timer.length=cursor.getLong(cursor.getColumnIndex(Timers.LENGTH));
			timer.repeat=(cursor.getInt(cursor.getColumnIndex(Timers.REPEAT))==0?false:true);
			timer.numBeeps=cursor.getInt(cursor.getColumnIndex(Timers.NUM_BEEPS));
			timer.vibrate=(cursor.getInt(cursor.getColumnIndex(Timers.VIBRATE))==0?false:true);
			timer.beep=cursor.getString(cursor.getColumnIndex(Timers.BEEP));
			if(timer.beep.equals("") || timer.beep.contains("ring")) timer.beep=context.getString(R.string.timerDefaultBeepPrefDefaultValue);
		}
		cursor.close();
    	
    	return timer;
    }
    
	public static void addAlarmManager(Context context, Timer timer) {
    	Intent intent = 
    		new Intent(context, TimerExpiryBroadcastReceiver.class);
    	intent.putExtra(Timer.Timers.TIMER_ID, timer.id);

    	PendingIntent pendingIntent = 
    		PendingIntent.getBroadcast(
    				context,
    		  timer.id,
              intent, 
              PendingIntent.FLAG_ONE_SHOT);

    	AlarmManager alarmManager = 
    		(AlarmManager)
    		     context.getSystemService(Context.ALARM_SERVICE);

		if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			alarmManager.setExact(AlarmManager.RTC_WAKEUP,
					timer.stopTime,
					pendingIntent);
		}
		else
			alarmManager.set(AlarmManager.RTC_WAKEUP,
					timer.stopTime,
					pendingIntent);
	}
	
	public static void removeAlarmManager(Context context, Timer timer) {
    	Intent intent = 
    		new Intent(context, TimerExpiryBroadcastReceiver.class);
    	intent.putExtra(Timer.Timers.TIMER_ID, timer.id);

    	PendingIntent pendingIntent = 
    		PendingIntent.getBroadcast(
    		  context,
    		  timer.id,
              intent, 
              PendingIntent.FLAG_ONE_SHOT);
		
		AlarmManager alarmManager = 
    		(AlarmManager)
    		     context.getSystemService(Context.ALARM_SERVICE);

    	alarmManager.cancel(pendingIntent);
	}

	public static void stopAllTimers(Context context, Uri URI) {
		Cursor cursor;
		Timer timer;
		
		cursor = context.getContentResolver().query(
				URI, 
				new String[] {Timers.TIMER_ID}, 
				Timers.RUNNING+"=1", 
				null, 
				null
			);
		
		for(cursor.moveToFirst();!cursor.isAfterLast();cursor.moveToNext()) {
			timer=fetchTimerFromDB(context,URI,cursor.getInt(cursor.getColumnIndex(Timers.TIMER_ID)));
			timer.stop();
			Timer.removeAlarmManager(context, timer);
			Timer.updateTimerRecord(false,false,timer,context,URI);
		}
		cursor.close();
	}
	
	public static void putTimerExpiredNotification(Context context, String timerTitle, int timerInterval, int timerIntervalParentId, Date time) {
        int smallIcon = (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP?R.drawable.icon_check_white:R.drawable.icon_check);
		Cursor cursor = context.getContentResolver().query(
				Timers.CONTENT_URI, 
				new String[] {Timers.TIMER_ID}, 
				Timers.INTERVAL_PARENT_ID+"="+timerIntervalParentId+" and "+Timers.LENGTH+"<>0", 
				null, 
				null);
		int countIntervals = cursor.getCount();
		cursor.close();
        SimpleDateFormat sdf = new SimpleDateFormat(context.getResources().getString(R.string.date_time_format));
        CharSequence tickerText;
        if(countIntervals==1)
        	tickerText = "Timer '"+timerTitle+"' expired at "+sdf.format(time)+".";
        else
        	tickerText = "Timer '"+timerTitle+"', Interval #"+timerInterval+" expired at "+sdf.format(time)+".";
        long when = System.currentTimeMillis();
        CharSequence contentTitle = "StopTimer";
        CharSequence contentText = tickerText;
        Intent notificationIntent = new Intent(context, MainActivity.class);
        notificationIntent.putExtra(Timer.Timers.INTERVAL_PARENT_ID,timerIntervalParentId);
        notificationIntent.putExtra("LaunchActivity",TimerActivity.ACTIVITY_TAB_NUMBER);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification;
		Notification.Builder notificationBuilder = new Notification.Builder(context)
				.setContentTitle(contentTitle).setContentText(contentText)
				.setSmallIcon(smallIcon).setContentIntent(contentIntent).setTicker(tickerText)
				.setWhen(when);
		if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP)
			notificationBuilder.setColor(ContextCompat.getColor(context, R.color.iconGreen));
		notification = notificationBuilder.getNotification();
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(timerIntervalParentId, notification);
	}
	
	public static void updateTimerRecord(boolean includeTitle, boolean includeRepeat, Timer timer, Context context, Uri URI) {
		ContentValues contentValues;
		contentValues = new ContentValues();
		if(includeTitle)
			contentValues.put(Timer.Timers.TITLE, timer.title);
		contentValues.put(Timer.Timers.START_TIME, timer.startTime);
		contentValues.put(Timer.Timers.STOP_TIME, timer.stopTime);
		contentValues.put(Timer.Timers.RUNNING, timer.running);
		contentValues.put(Timer.Timers.INTERVAL, timer.interval);
		contentValues.put(Timer.Timers.INTERVAL_PARENT_ID, timer.intervalParentId);
		contentValues.put(Timer.Timers.REMAINING_TIME, timer.remainingTime);
		contentValues.put(Timer.Timers.LENGTH, timer.length);
		if(includeRepeat) {
			contentValues.put(Timer.Timers.REPEAT, timer.repeat);
		}
		contentValues.put(Timer.Timers.NUM_BEEPS, timer.numBeeps);
		contentValues.put(Timer.Timers.VIBRATE, timer.vibrate);
		contentValues.put(Timer.Timers.BEEP, timer.beep);

		context.getContentResolver().update(URI, contentValues, Timers.TIMER_ID+"="+timer.id, null);
	}

    
	public static class Timers implements BaseColumns {
		private Timers() {
		}

		public static final Uri CONTENT_URI = Uri.parse("content://"
				+ TimerContentProvider.AUTHORITY + "/timers");

		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.synappze.stoptimer";
		
		public static final String TIMER_ID = "_id";
		
		public static final String TITLE = "title";

	    public static final String START_TIME = "start_time";
		
	    public static final String STOP_TIME = "stop_time";
	    
	    public static final String RUNNING = "running";
	    
	    public static final String INTERVAL = "interval";
	    
		public static final String INTERVAL_PARENT_ID = "interval_parent_id";

	    public static final String REMAINING_TIME = "remaining_time";

	    public static final String LENGTH = "length";
	    
	    public static final String REPEAT = "repeat";

	    public static final String NUM_BEEPS = "num_beeps";
	    
	    public static final String VIBRATE = "vibrate";
	    
	    public static final String BEEP = "beep";

	}


}
