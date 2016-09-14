package com.synappze.stoptimer.receivers;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.PowerManager;
import android.os.Vibrator;
import android.util.Log;

import com.synappze.stoptimer.MainActivity;
import com.synappze.stoptimer.Timer;
import com.synappze.stoptimer.Timer.Timers;
import com.synappze.stoptimer.TimerActivity;

import java.io.IOException;
import java.util.Date;
import java.util.List;

public class TimerExpiryBroadcastReceiver extends BroadcastReceiver 
{
	
	public static final Uri URI = Timer.Timers.CONTENT_URI;

    @Override
    public void onReceive(final Context context, final Intent intent) 
    {
		//Log.d("blah","TimerExpiryBroadcastReceiver onReceive()");

	    //this background thread is created so that ActivityManager.RunningAppProcessInfo works correctly, ActivityManager.RunningAppProcessInfo will always be IMPORTANCE_FOREGROUND if onReceive is still executing!
    	//the alarmmanager wakes the device up and keeps it awake only till onReceive() of the broadcastreceiver is running. since we're using a background thread, onReceive() will exit right away so we need to use a wakelock
    	final Thread myThread = new Thread(new Runnable() {    
	    	public void run() {   
	    		Cursor cursor; 
	            MediaPlayer mediaPlayer;
	            Timer timer;
	    		PowerManager powerManager;
	    		PowerManager.WakeLock wakeLock;
	        	boolean appInForeground;
	        	ContentValues contentValues;
	        	Timer nextTimer = null;
	        	Vibrator vibrate = null;
	    		
	    		powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
	    		wakeLock = powerManager.newWakeLock(
	    				PowerManager.PARTIAL_WAKE_LOCK |PowerManager.ACQUIRE_CAUSES_WAKEUP,
	    				"TimerBroadcastReceiver"
	    			);
	    		wakeLock.acquire();
	    		
	    		timer=Timer.fetchTimerFromDB(context, URI, intent.getIntExtra(Timer.Timers.TIMER_ID, 0));
	    		if(timer==null) {
	    			wakeLock.release();
	    			return;
	    		}
	            
	    		contentValues = new ContentValues();
	    		contentValues.put(Timer.Timers.START_TIME, 0);
	    		contentValues.put(Timer.Timers.STOP_TIME, 0);
	    		contentValues.put(Timer.Timers.RUNNING, false);
	    		contentValues.put(Timer.Timers.REMAINING_TIME, 0);
	    		context.getContentResolver().update(URI, contentValues, Timers.TIMER_ID+"="+timer.id, null);

	            cursor = context.getContentResolver().query(
	        			URI, 
	        			new String[] {Timers.TIMER_ID},
	        			Timers.INTERVAL_PARENT_ID+"="+timer.intervalParentId+" and "+Timers.INTERVAL+">"+timer.interval+" and "+Timers.LENGTH+"<>0",
	        			null, 
	        			null
	        		);
	            if(cursor.moveToFirst()) {
		    		nextTimer=Timer.fetchTimerFromDB(context, URI, cursor.getInt(cursor.getColumnIndex(Timers.TIMER_ID)));
	            }
	            cursor.close();
	            if(nextTimer==null && timer.repeat) {
		            cursor = context.getContentResolver().query(
		        			URI, 
		        			new String[] {Timers.TIMER_ID},
		        			Timers.INTERVAL_PARENT_ID+"="+timer.intervalParentId+" and "+Timers.INTERVAL+"=1",
		        			null, 
		        			null
		        		);
		            if(cursor.moveToFirst()) {
			    		nextTimer=Timer.fetchTimerFromDB(context, URI, cursor.getInt(cursor.getColumnIndex(Timers.TIMER_ID)));
		            }
		            cursor.close();
	            }
	            
	            if(nextTimer!=null) {
	            	nextTimer.reset();
	            	nextTimer.startTime=timer.stopTime;
	            	nextTimer.stopTime=timer.stopTime+nextTimer.length;
	            	nextTimer.running=true;
	            	//SimpleDateFormat sdf = new SimpleDateFormat(context.getResources().getString(R.string.date_time_format));
	            	//Log.d("TimerBroadcastReceiver","interval="+nextTimer.interval+", startTime="+sdf.format(nextTimer.startTime)+", stopTime="+sdf.format(nextTimer.stopTime));
	            	contentValues = new ContentValues();
		    		contentValues.put(Timer.Timers.START_TIME, nextTimer.startTime);
		    		contentValues.put(Timer.Timers.STOP_TIME, nextTimer.stopTime);
		    		contentValues.put(Timer.Timers.RUNNING, nextTimer.running);
		    		context.getContentResolver().update(URI, contentValues, Timers.TIMER_ID+"="+nextTimer.id, null);
		    		Timer.addAlarmManager(context, nextTimer);
	            }
	            
	    		appInForeground = false;
	    		ActivityManager activityManager = (ActivityManager) context.getSystemService( Context.ACTIVITY_SERVICE );
	    		List<ActivityManager.RunningAppProcessInfo> processList = activityManager.getRunningAppProcesses(); 
	    		for(int i=0; i<processList.size(); i++) {
	    			if(processList.get(i).processName.equalsIgnoreCase(MainActivity.class.getPackage().getName()) && processList.get(i).importance==ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
	    				appInForeground=true;
	    			}
	    		}
	    		if(!appInForeground) {
	    			Timer.putTimerExpiredNotification(context, timer.title, timer.interval, timer.intervalParentId, new Date(timer.stopTime));		            
		    		MainActivity.addOrRemoveMainNotification(context,false);
	    		}
	    		else {
	    			Intent intent = new Intent(MainActivity.class.getPackage().getName()+".timer_expired");
	    			intent.putExtra(Timer.Timers.TIMER_ID, timer.id);
	    			intent.putExtra(Timer.Timers.TITLE, timer.title);
	    			intent.putExtra(Timer.Timers.INTERVAL, timer.interval);
	    			intent.putExtra(Timer.Timers.INTERVAL_PARENT_ID, timer.intervalParentId);
	    			if(nextTimer != null) {
	    				intent.putExtra("next"+Timer.Timers.TIMER_ID, nextTimer.id);
	    			}
	    			context.sendBroadcast(intent);
	    		}
	    		
	    		if(timer.vibrate)
	    			vibrate = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);

	    		if(timer.numBeeps>0) {
		    		final Thread thisThread=Thread.currentThread();
		    		mediaPlayer = MediaPlayer.create(context, TimerActivity.getBeepResourceId(timer.beep));
		    		if(mediaPlayer!=null) {
			            mediaPlayer.setLooping(false);
			            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {                        
			            	public void onCompletion(MediaPlayer arg0) {                       
		            			thisThread.interrupt();
		            		}        
		            	});
			            if(timer.vibrate) {
			            	long[] pattern = { 50, 500 };
			            	vibrate.vibrate(pattern, 0);
			            }
			            for(int i=0; i<timer.numBeeps; i++) {
			            	mediaPlayer.start();
				            try {
				            	Thread.sleep(5000);
							} catch (InterruptedException e) {
							}
							mediaPlayer.stop();
							try {
								mediaPlayer.prepare();
							} catch (IllegalStateException e) {
								return;
							} catch (IOException e) {
								return;
							}
							mediaPlayer.seekTo(0);
			    		}
			            if(timer.vibrate) {
			            	vibrate.cancel();
			            }
	
			    		mediaPlayer.release();
		    		}
	    		}
	    		else if(timer.vibrate) {
	    			vibrate.vibrate(2000);
	    		}
	            
	            wakeLock.release();
	    	}  
	    });
	    myThread.start();
    }
}

