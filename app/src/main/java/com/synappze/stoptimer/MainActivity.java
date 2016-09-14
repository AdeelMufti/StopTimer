package com.synappze.stoptimer;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.LocalActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.synappze.stoptimer.Stopwatch.Stopwatches;
import com.synappze.stoptimer.Timer.Timers;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

	public static final String PREFS_NAME = "SynappzeStopTimer";
	public static final int DISPLAY_UPDATE_INTERVAL = 50;
	public static final Uri TIMER_URI = Timer.Timers.CONTENT_URI;
	public static final Uri STOPWATCH_URI = Stopwatch.Stopwatches.CONTENT_URI;
	public static String UNIQUE_ID = null;

	private static SharedPreferences sharedPrefs = null;
	private static SharedPreferences.Editor sharedPrefsEditor = null;

	public static MainActivityBroadcastReceiver mainActivityBroadcastReceiver = null;

	private TabHost tabHost;
	private LocalActivityManager localActivityManager;
	private AdView adView;

    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);

		Integer resultCode = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this);
		if (resultCode != ConnectionResult.SUCCESS)
		{
			Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(this, resultCode, 0);
			dialog.show();
			TextView dynamicTextView = new TextView(this);
			dynamicTextView.setText(" Please update Google Play Service To Use StopTimer. ");
			setContentView(dynamicTextView);
		}
		else
		{
			setContentView(R.layout.main);

			Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
			setSupportActionBar(toolbar);

			/*FloatingActionButton floatingActionButton = (FloatingActionButton) findViewById(R.id.fab);
			floatingActionButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
							.setAction("Action", null).show();
				}
			});*/


			sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
			sharedPrefsEditor = sharedPrefs.edit();

			if(sharedPrefs.getString("uniqueId", "").equals("")) {
				sharedPrefsEditor.putString("uniqueId", UUID.randomUUID().toString());
				sharedPrefsEditor.commit();
			}
			UNIQUE_ID=sharedPrefs.getString("uniqueId", "");

			if(sharedPrefs.getString("screenOrientationPref", getString(R.string.screenOrientationPrefDefaultValue)).equals("0"))
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
			else if(sharedPrefs.getString("screenOrientationPref", getString(R.string.screenOrientationPrefDefaultValue)).equals("1"))
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
			else if(sharedPrefs.getString("screenOrientationPref", getString(R.string.screenOrientationPrefDefaultValue)).equals("2"))
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

			mainActivityBroadcastReceiver = new MainActivityBroadcastReceiver(this);

			tabHost = (TabHost)findViewById(android.R.id.tabhost);
			localActivityManager = new LocalActivityManager(this, true);
			localActivityManager.dispatchCreate(savedInstanceState);
			tabHost.setup(localActivityManager);
			tabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
				@Override
				public void onTabChanged(String tabId) {
					invalidateOptionsMenu();
				}
			});

			SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
				public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
					//if want to do haptic feedback, need to do it for the right keys since some are internal and changed internally
					//MainActivity.hapticFeedback(MainActivity.this);
					if(key.equals("keepScreenOnPref")) {
						if(sharedPrefs.getBoolean("keepScreenOnPref", getString(R.string.keepScreenOnPrefDefaultValue).equals("true")?true:false))
							tabHost.setKeepScreenOn(true);
						else
							tabHost.setKeepScreenOn(false);
					}
					else if(key.equals("screenOrientationPref")) {
						if(sharedPrefs.getString("screenOrientationPref", getString(R.string.screenOrientationPrefDefaultValue)).equals("0"))
							setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
						else if(sharedPrefs.getString("screenOrientationPref", getString(R.string.screenOrientationPrefDefaultValue)).equals("1"))
							setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
						else if(sharedPrefs.getString("screenOrientationPref", getString(R.string.screenOrientationPrefDefaultValue)).equals("2"))
							setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
					}
				}
			};
			sharedPrefs.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);

			if(sharedPrefs.getBoolean("keepScreenOnPref", getString(R.string.keepScreenOnPrefDefaultValue).equals("true")?true:false))
				tabHost.setKeepScreenOn(true);
			else
				tabHost.setKeepScreenOn(false);

			TabHost.TabSpec timerTabSpec = tabHost.newTabSpec("Timer");
			timerTabSpec.setContent(new Intent().setClass(this, TimerActivity.class));
			timerTabSpec.setIndicator(" Timer", ResourcesCompat.getDrawable(getResources(), R.drawable.timer, null));
			tabHost.addTab(timerTabSpec);
			ImageView tabImageView0 = (ImageView) tabHost.getTabWidget().getChildTabViewAt(TimerActivity.ACTIVITY_TAB_NUMBER).findViewById(android.R.id.icon);
			tabImageView0.setVisibility(View.VISIBLE);
			tabImageView0.setImageResource(R.drawable.timer);

			TabHost.TabSpec stopwatchTabSpec = tabHost.newTabSpec("Stopwatch");
			stopwatchTabSpec.setContent(new Intent().setClass(this, StopwatchActivity.class));
			stopwatchTabSpec.setIndicator(" Stopwatch", ResourcesCompat.getDrawable(getResources(), R.drawable.stopwatch, null));
			tabHost.addTab(stopwatchTabSpec);
			ImageView tabImageView1 = (ImageView) tabHost.getTabWidget().getChildTabViewAt(StopwatchActivity.ACTIVITY_TAB_NUMBER).findViewById(android.R.id.icon);
			tabImageView1.setVisibility(View.VISIBLE);
			tabImageView1.setImageResource(R.drawable.stopwatch);

			launchCorrectActivity(getIntent());

			adView = (AdView) findViewById(R.id.adView);
			AdRequest adRequest = new AdRequest.Builder()
					.addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
					.addTestDevice("392193760B2290A398BFE4BE6A5A8605")
					.build();
			adView.loadAd(adRequest);

			showChangeLog(this,false);
		}

    }

    public static void showChangeLog(final Context context, boolean overrideShownAlready) {
    	if(!overrideShownAlready && sharedPrefs.getString("LastChangeLogShown", "").equals(context.getString(R.string.app_version_name)))
    		return;

    	InputStream input = context.getResources().openRawResource(R.raw.change_log);
		DataInputStream in = new DataInputStream(input);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String strLine, changeLog="";
		boolean begunReading=false;
		boolean begunPrevVers=false;
		try {
			while ((strLine = br.readLine()) != null) {
				if(!begunReading && strLine.contains("<v"+context.getString(R.string.app_version_name))) {
					begunReading=true;
					changeLog="The following features are new to this version:\n\n";
				}
				else if(!begunPrevVers && begunReading && ((strLine.contains("<") && strLine.contains(">")) || strLine.length()<=1)) {
					begunPrevVers=true;
					changeLog+="\n\nThe following features were new in previous versions:\n"+strLine+"\n";
				}
				else if(begunReading) {
					changeLog+=strLine+"\n";
				}
			}
			in.close();
			input.close();
		} catch (Exception e){
			return;
		}

		if(changeLog.length()==0)
			return;

    	DialogInterface.OnClickListener alertDialogClickListener = new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				MainActivity.hapticFeedback(context);
				switch (which) {
				case DialogInterface.BUTTON_POSITIVE:
					sharedPrefsEditor.putString("LastChangeLogShown", context.getString(R.string.app_version_name));
					sharedPrefsEditor.commit();
					break;
				}
			}};
		AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
		alertDialog.setIcon(android.R.drawable.ic_dialog_info);
        alertDialog.setTitle("v"+context.getString(R.string.app_version_name)+" Changes");
		alertDialog.setMessage(changeLog);
        alertDialog.setPositiveButton("OK", alertDialogClickListener);
        alertDialog.setCancelable(false);
        alertDialog.create().show();
    }

    public void launchCorrectActivity(Intent intent) {
        int activityToLaunch = sharedPrefs.getInt("LastMainActivity", StopwatchActivity.ACTIVITY_TAB_NUMBER);
        if(intent.getExtras() != null && intent.getExtras().containsKey("LaunchActivity")) {
        	if(intent.getExtras().getInt("LaunchActivity")==TimerActivity.ACTIVITY_TAB_NUMBER) {
            	activityToLaunch = TimerActivity.ACTIVITY_TAB_NUMBER;
        		sharedPrefsEditor.putInt("BringTimerToFront", intent.getExtras().getInt(Timer.Timers.INTERVAL_PARENT_ID));
        		sharedPrefsEditor.commit();
        	}
        }
        tabHost.setCurrentTab(activityToLaunch);
    }

    public void onNewIntent(Intent intent) {
    	launchCorrectActivity(intent);
    }

    public void onPause() {
		if(localActivityManager!=null)
			localActivityManager.dispatchPause(this.isFinishing());

		if (adView != null)
			adView.pause();

		super.onPause();

        if(mainActivityBroadcastReceiver!=null)
			this.unregisterReceiver(this.mainActivityBroadcastReceiver);

        addOrRemoveMainNotification(this,false);
    }

	@Override
	public void onDestroy() {
		if(localActivityManager!=null)
			localActivityManager.dispatchDestroy(this.isFinishing());

		if (adView != null)
			adView.destroy();

		super.onDestroy();
	}

	public static void addOrRemoveMainNotification(Context context, boolean forceRemove) {
        long when = System.currentTimeMillis();
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if(forceRemove) {
        	notificationManager.cancel(0);
        }
        else {
	        boolean running=false;
	        int runningTimers = 0;
	        int runningStopwatches = 0;
	        Cursor cursor;

	        cursor =
	        	context.getContentResolver().query(
        			TIMER_URI,
        			new String[] {Timers.TIMER_ID},
        			Timer.Timers.RUNNING+"=1",
        			null,
        			null
        		);
	        runningTimers = cursor.getCount();
	        cursor.close();

	        cursor = context.getContentResolver().query(
	    			STOPWATCH_URI,
	    			new String[] {Stopwatches.STOPWATCH_ID},
	    			Stopwatches.RUNNING+"=1",
	    			null,
	    			null
	    		);
	        runningStopwatches = cursor.getCount();
	        cursor.close();

	        if(runningTimers!=0)
	        	running=true;
	        else if(runningStopwatches!=0)
	        	running=true;
	        if(running) {
				int smallIcon = (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP?R.drawable.icon_white:R.drawable.icon);
	            CharSequence tickerText = "Active Timer(s): "+runningTimers+", Stopwatch(es): "+runningStopwatches+".";
	            CharSequence contentTitle = context.getResources().getString(R.string.app_name);
	            CharSequence contentText = tickerText;
	            Intent notificationIntent = new Intent(context, MainActivity.class);
	            PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
	            Notification notification;
				Notification.Builder notificationBuilder = new Notification.Builder(context)
						.setContentTitle(contentTitle).setContentText(contentText).setTicker(tickerText).setWhen(when)
						.setSmallIcon(smallIcon).setContentIntent(contentIntent);//.setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.icon))
				if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP)
						notificationBuilder.setColor(ContextCompat.getColor(context, R.color.iconGreen));
				notification = notificationBuilder.getNotification();
				//notification.flags |= Notification.FLAG_AUTO_CANCEL;
	            notification.flags |= Notification.FLAG_NO_CLEAR;
	            notification.flags |= Notification.FLAG_ONGOING_EVENT;
		        notificationManager.notify(0, notification);
	        }
	        else {
	        	notificationManager.cancel(0);
	        }
        }
    }

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return localActivityManager.getCurrentActivity().onOptionsItemSelected(item);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return(setupMenu(menu));
	}

	private boolean setupMenu(Menu menu)
	{
		if(tabHost.getCurrentTab()==StopwatchActivity.ACTIVITY_TAB_NUMBER)
		{
			MenuInflater inflater = getMenuInflater();
			inflater.inflate(R.menu.stopwatch_menu, menu);
			inflater.inflate(R.menu.main_menu, menu);
		}
		else if(tabHost.getCurrentTab()==TimerActivity.ACTIVITY_TAB_NUMBER)
		{
			MenuInflater inflater = getMenuInflater();
			inflater.inflate(R.menu.timer_menu, menu);
			inflater.inflate(R.menu.main_menu, menu);
		}
		return true;
	}

	public void onResume() {
	   	super.onResume();

		if(localActivityManager!=null)
			localActivityManager.dispatchResume();

		if (adView != null)
			adView.resume();

		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(MainActivity.class.getPackage().getName()+".timer_expired");
		this.registerReceiver(this.mainActivityBroadcastReceiver,intentFilter);

		addOrRemoveMainNotification(this,true); //force remove main notification so that it's freshly displayed when user navigates out of stoptimer
    }

	private void receivedBroadcast(Intent intent) {
		if(intent.getAction().equalsIgnoreCase(MainActivity.class.getPackage().getName()+".timer_expired")) {
			Cursor cursor = this.getContentResolver().query(
					Timers.CONTENT_URI,
					new String[] {Timers.TIMER_ID},
					Timers.INTERVAL_PARENT_ID+"="+intent.getExtras().getInt(Timer.Timers.INTERVAL_PARENT_ID)+" and "+Timers.LENGTH+"<>0",
					null,
					null);
			int countIntervals = cursor.getCount();
			cursor.close();
			if(countIntervals==1)
				Toast.makeText(this, "Timer '"+intent.getExtras().getString(Timer.Timers.TITLE)+"' has expired.", Toast.LENGTH_LONG).show();
			else
				Toast.makeText(this, "Timer '"+intent.getExtras().getString(Timer.Timers.TITLE)+"', Interval #"+intent.getExtras().getInt(Timer.Timers.INTERVAL)+" has expired.", Toast.LENGTH_LONG).show();

		}
	}

    public static class MainActivityBroadcastReceiver extends BroadcastReceiver {
		private MainActivity mainActivity;

		public MainActivityBroadcastReceiver() {
		}

		public MainActivityBroadcastReceiver(MainActivity mainActivity) {
			this.mainActivity = mainActivity;
		}

    	@Override
    	public void onReceive(Context context, Intent intent) {
    		mainActivity.receivedBroadcast(intent);
    	}
    }

    public static void hapticFeedback(Context context) {
    	if(sharedPrefs.getBoolean("hapticFeedbackPref", (context.getString(R.string.hapticFeedbackPrefDefaultValue).equals("true")?true:false)))
    		((Vibrator)context.getSystemService(Context.VIBRATOR_SERVICE)).vibrate(35);
    }

    public static Typeface getDigitsTypeface(Context context) {
    	Typeface customFont = Typeface.createFromAsset(context.getAssets(),"custom-font.ttf");
    	return(customFont);
    }
}