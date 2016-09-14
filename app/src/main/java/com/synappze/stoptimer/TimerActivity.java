package com.synappze.stoptimer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Rect;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.EditTextPreference;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnTouchListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.synappze.stoptimer.Timer.Timers;

import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;

public class TimerActivity extends Activity implements OnClickListener {

	public static final Uri URI = Timer.Timers.CONTENT_URI;
	public static final int ACTIVITY_TAB_NUMBER = 0;

	private class TimerUI {
		public EditText timerTitleEditText = null;
		public ImageButton timerAddButton = null;
		public ImageButton timerXButton = null;
		public ImageButton timerLeftButton = null;
		public ImageButton timerRightButton = null;
		public LinearLayout timerDaysLinearLayout = null;
		public LinearLayout timerHrsLinearLayout = null;
		public LinearLayout timerMinsLinearLayout = null;
		public LinearLayout timerSecsLinearLayout = null;
		public LinearLayout timerTenthsLinearLayout = null;
		public TextView timerDaysTextView = null;
		public TextView timerHrsTextView = null;
		public TextView timerMinsTextView = null;
		public TextView timerSecsTextView = null;
		public TextView timerTenthsTextView = null;
		public Button timerStartStopResumeButton = null;
		public Button timerResetButton = null;
		public LinearLayout timerMainRowLinearLayout = null;
		public LinearLayout timerMainRowEditTitleLinearLayout = null;
		public TextView timerTitleTextView = null;
		public Button timerPlusDays = null;
		public Button timerPlusHrs = null;
		public Button timerPlusMins = null;
		public Button timerPlusSecs = null;
		public Button timerMinusDays = null;
		public Button timerMinusHrs = null;
		public Button timerMinusMins = null;
		public Button timerMinusSecs = null;
		public TextView timerIntervalTitleTextView = null;
		public ImageButton timerIntervalAddButton = null;
		public ImageButton timerIntervalXButton = null;
		public ImageButton timerIntervalLeftButton = null;
		public ImageButton timerIntervalRightButton = null;
		public CheckBox timerIntervalRepeatCheckBox = null;
		public ProgressBar timerProgressBar = null;
		public TextView timerNumBeepsTextView = null;
		public Button timerBeepsPlusButton = null;
		public Button timerBeepsMinusButton = null;
		public CheckBox timerVibrateCheckBox = null;
		public TextView timerBeepTextView = null;
	}
	
	private Timer timer1 = null;
	private Timer timer2 = null;
	private Timer timer3 = null;
	private View timer1View = null;
	private View timer2View = null;
	private View timer3View = null;
	private TimerUI timer1UI = new TimerUI();
	private TimerUI timer2UI = new TimerUI();
	private TimerUI timer3UI = new TimerUI();
	private Handler timer1Handler = null;
	private Handler timer2Handler = null;
	private Handler timer3Handler = null;
	private RealViewSwitcher realViewSwitcher = null;
	private boolean deleteTimer = false;
	
	private Handler screenSwitchHandler = null;
	private Handler timePlusMinusLongPressHandler = null;
	private int timePlusMinusLongPressIteration = 0;
	private ContentValues contentValues = null;
	private SharedPreferences sharedPrefs = null;
	private SharedPreferences.Editor sharedPrefsEditor = null; 
	private TimerActivityBroadcastReceiver timerActivityBroadcastReceiver = null; 
	private Cursor cursor = null;
	private int timePlusMinusButtonId = 0;
	private Toast toast;
	private DialogInterface.OnClickListener alertDialogClickListener;
	private AlertDialog.Builder alertDialog;
	public static MediaPlayer mediaPlayer = null;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {

    	super.onCreate(savedInstanceState);
		setContentView(R.layout.timer);

		timer1Handler = new Handler();
		timer2Handler = new Handler();
		timer3Handler = new Handler();

		timePlusMinusLongPressHandler = new Handler();
		
		screenSwitchHandler = new Handler();

    	timer1View = getLayoutInflater().inflate(R.layout.timer, null);
    	timer2View = getLayoutInflater().inflate(R.layout.timer, null);
    	timer3View = getLayoutInflater().inflate(R.layout.timer, null);
		timer1UI = new TimerUI();
		timer2UI = new TimerUI();
		timer3UI = new TimerUI();
    	setupTimerViewUI(timer1View,timer1UI);
    	setupTimerViewUI(timer2View,timer2UI);
    	setupTimerViewUI(timer3View,timer3UI);
    	
    	realViewSwitcher = new RealViewSwitcher(getApplicationContext());
    	realViewSwitcher.setupTimerActivity(this);
		realViewSwitcher.setOnScreenSwitchListener(realViewOnScreenSwitchListener);
   		realViewSwitcher.addView(timer1View);
    	realViewSwitcher.addView(timer2View);
   		realViewSwitcher.addView(timer3View);

   		toast = Toast.makeText(this, "", Toast.LENGTH_LONG);
   		
		sharedPrefs = getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE);
		sharedPrefsEditor = sharedPrefs.edit();
		
		timerActivityBroadcastReceiver = new TimerActivityBroadcastReceiver(this);

		setVolumeControlStream(AudioManager.STREAM_MUSIC);
	}

	public boolean onKeyDown(int keyCode, KeyEvent event) {
	    if (keyCode == KeyEvent.KEYCODE_VOLUME_UP && sharedPrefs.getString("volumeButtonsPref", getString(R.string.volumeButtonsPrefDefaultValue)).equals("0")) 
	    {
	    	timer2UI.timerStartStopResumeButton.performClick();
	        return(true);
	    }
	    else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN && sharedPrefs.getString("volumeButtonsPref", getString(R.string.volumeButtonsPrefDefaultValue)).equals("0"))
	    {
	    	timer2UI.timerResetButton.performClick();
	        return(true);
	    }
	    return super.onKeyDown(keyCode, event);
	}

	public static int getBeepResourceId(String beep) {
		if(beep.equals("beep1"))
			return(R.raw.beep1);
		else if(beep.equals("beep2"))
			return(R.raw.beep2);
		else if(beep.equals("beep3"))
			return(R.raw.beep3);
		else if(beep.equals("beep4"))
			return(R.raw.beep4);
		else if(beep.equals("beep5"))
			return(R.raw.beep5);
		else if(beep.equals("beep6"))
			return(R.raw.beep6);
		else if(beep.equals("beep7"))
			return(R.raw.beep7);
		else if(beep.equals("beep8"))
			return(R.raw.beep8);
		else if(beep.equals("beep9"))
			return(R.raw.beep9);
		else if(beep.equals("beep10"))
			return(R.raw.beep10);
		return R.raw.beep1;		
	}
    
    public static void displayBeepPicker(final Context context, final boolean forTimer, final Timer timer, final TimerUI timerUI, final EditTextPreference pref) {
    	Toast.makeText(context, "Please make sure that your Media Volume is turned up.", Toast.LENGTH_LONG).show();

    	ArrayList<String> array=new ArrayList<String>();
    	int selectedIndex = -1;
    	
    	Field[] fields=R.raw.class.getFields();
        for(int count=0; count < fields.length; count++){
        	if(fields[count].getName().substring(0, 4).equalsIgnoreCase("beep"))
        		array.add("Beep "+fields[count].getName().substring(4));
        }

		Collections.sort(array, new Comparator<String>(){
			public int compare(String s1, String s2) {
				int i1 = Integer.parseInt(s1.substring(5,(s1.indexOf(":")==-1?s1.length():s1.indexOf(":"))));
				int i2 = Integer.parseInt(s2.substring(5,(s2.indexOf(":")==-1?s2.length():s2.indexOf(":"))));
				return (i1<i2?-1:1);
			}

		});

		CharSequence[] items = new CharSequence[array.size()];
        for(int i=0; i<array.size(); i++) {
        	items[i]=array.get(i);
        	if(		( forTimer && ((array.get(i).substring(0, 4).toLowerCase()+array.get(i).substring(5,array.get(i).indexOf(":")==-1?array.get(i).length():array.get(i).indexOf(":"))).equals(timer.beep)))
        		||	(!forTimer && ((array.get(i).substring(0, 4).toLowerCase()+array.get(i).substring(5,array.get(i).indexOf(":")==-1?array.get(i).length():array.get(i).indexOf(":"))).equals(pref.getText())))
        			)
        		selectedIndex=i;
        }
        
		final ArrayList<String> finalArray = array;

		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle("Pick Beep");
		builder.setSingleChoiceItems(items, selectedIndex, new DialogInterface.OnClickListener() {
		    public void onClick(DialogInterface dialog, int item) {
		        if(forTimer)
		        	timer.beep=finalArray.get(item).substring(0, 4).toLowerCase()+finalArray.get(item).substring(5,finalArray.get(item).indexOf(":")==-1?finalArray.get(item).length():finalArray.get(item).indexOf(":"));
		        else
		        	pref.setText(finalArray.get(item).substring(0, 4).toLowerCase()+finalArray.get(item).substring(5,finalArray.get(item).indexOf(":")==-1?finalArray.get(item).length():finalArray.get(item).indexOf(":")));
                if(mediaPlayer!=null) {
                	mediaPlayer.stop();
                	mediaPlayer = null;
                }
				if(forTimer)
					mediaPlayer = MediaPlayer.create(context, getBeepResourceId(timer.beep));
				else
					mediaPlayer = MediaPlayer.create(context, getBeepResourceId(pref.getText()));
				if(mediaPlayer!=null)
					mediaPlayer.start();
		    	if(forTimer) {
		    		Timer.updateTimerRecord(false,false,timer,context,URI);
		    		timerUI.timerBeepTextView.setText(finalArray.get(item).substring(0,(finalArray.get(item).indexOf(":")==-1?finalArray.get(item).length():finalArray.get(item).indexOf(":"))));
		    	}
		    	else {
					String currentTitle = (String) pref.getTitle();
					currentTitle = currentTitle.substring(0,currentTitle.indexOf(":"));
		    		pref.setTitle(currentTitle+": "+convertBeepToFormatedString(pref.getText()));
		    	}
		    }
		});
		builder.setPositiveButton("Done", new DialogInterface.OnClickListener() {
           public void onClick(DialogInterface dialog, int id) {
                if(mediaPlayer!=null) {
                	mediaPlayer.stop();
                	mediaPlayer.release();
                	mediaPlayer = null;
                }
           }
       });

		AlertDialog alert = builder.create();
		alert.show();
    	
    }
    
    public static String convertBeepToFormatedString(String beep) {
    	String formatted;
    	formatted = beep.substring(0, 1).toUpperCase() + beep.substring(1,4) + " " + beep.substring(4);
    	return(formatted);
    }
    
	private final RealViewSwitcher.OnScreenSwitchListener realViewOnScreenSwitchListener = new RealViewSwitcher.OnScreenSwitchListener() {
		public void onScreenSwitched(int screen) {
			if(screen==0)
				screenSwitchHandler.post(navigateToPrevViewTask);
			else if(screen==2) 
				screenSwitchHandler.post(navigateToNextViewTask);
		}
	};

    public void onPause() {
    	//Log.d("blah",this.getClass().getSimpleName()+" onPause called");

    	super.onPause();
    	
		timer2UI.timerTitleEditText.removeTextChangedListener(titleTextWatcher);
    	
		timer1Handler.removeCallbacks(timer1UpdateTimeTask);
		timer2Handler.removeCallbacks(timer2UpdateTimeTask);
		timer3Handler.removeCallbacks(timer3UpdateTimeTask);
        
		this.unregisterReceiver(this.timerActivityBroadcastReceiver);
    }
    
    public void onResume() {
    	super.onResume();
		sharedPrefsEditor.putInt("LastMainActivity", ACTIVITY_TAB_NUMBER);
		sharedPrefsEditor.commit();
		
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(MainActivity.class.getPackage().getName()+".timer_expired");
		this.registerReceiver(this.timerActivityBroadcastReceiver,intentFilter);

		int count;
		cursor = this.getContentResolver().query(URI, new String[] {Timers.TIMER_ID}, null, null, null);
		count = cursor.getCount();
		cursor.close();
		if(count==0) {
			//first time app called!
			timer2=addTimer();
		}
		else {
			int timerLastViewed;
			if(sharedPrefs.getInt("BringTimerToFront", 0) != 0) {
				timerLastViewed = sharedPrefs.getInt("BringTimerToFront", 1);
				sharedPrefsEditor.remove("BringTimerToFront");
				sharedPrefsEditor.commit();
			}
			else {
				timerLastViewed = sharedPrefs.getInt("TimerLastViewed", 1);
			}
			
			timer2=Timer.fetchTimerFromDB(this, URI, timerLastViewed);
			if(timer2==null)
				timer2=Timer.fetchTimerFromDB(this, URI, 1);
			timer2=fetchRunningOrStoppedInvervalIfAny(timer2);
		}
		
		setupRealViewSwitcher();
	
		timer2UI.timerTitleEditText.addTextChangedListener(titleTextWatcher);

    }
    
    private void setupTimerViewUI(View v, final TimerUI ui) {
        ui.timerTitleEditText = (EditText) v.findViewById(R.id.timerTitleEditText);
        ui.timerAddButton = (ImageButton) v.findViewById(R.id.timerAddButton);
        ui.timerXButton = (ImageButton) v.findViewById(R.id.timerXButton);
        ui.timerLeftButton = (ImageButton) v.findViewById(R.id.timerLeftImageButton);
        ui.timerRightButton = (ImageButton) v.findViewById(R.id.timerRightImageButton);
		ui.timerDaysLinearLayout = (LinearLayout) v.findViewById(R.id.timerDaysLinearLayout);
		ui.timerHrsLinearLayout = (LinearLayout) v.findViewById(R.id.timerHrsLinearLayout);
		ui.timerMinsLinearLayout = (LinearLayout) v.findViewById(R.id.timerMinsLinearLayout);
		ui.timerSecsLinearLayout = (LinearLayout) v.findViewById(R.id.timerSecsLinearLayout);
		ui.timerTenthsLinearLayout = (LinearLayout) v.findViewById(R.id.timerTenthsLinearLayout);
		ui.timerDaysTextView = (TextView) v.findViewById(R.id.timerDaysTextView);
        ui.timerHrsTextView = (TextView) v.findViewById(R.id.timerHrsTextView);
        ui.timerMinsTextView = (TextView) v.findViewById(R.id.timerMinsTextView);
        ui.timerSecsTextView = (TextView) v.findViewById(R.id.timerSecsTextView);
        ui.timerTenthsTextView = (TextView) v.findViewById(R.id.timerTenthsTextView);
        ui.timerStartStopResumeButton = (Button) v.findViewById(R.id.timerStartStopResumeButton);
        ui.timerResetButton = (Button) v.findViewById(R.id.timerResetButton);
        ui.timerMainRowLinearLayout = (LinearLayout) v.findViewById(R.id.timerMainRowLinearLayout);
        ui.timerMainRowEditTitleLinearLayout = (LinearLayout) v.findViewById(R.id.timerMainRowEditTitleLinearLayout);
        ui.timerTitleTextView = (TextView) v.findViewById(R.id.timerTitleTextView);
        ui.timerPlusDays = (Button) v.findViewById(R.id.timerPlusDays);
        ui.timerPlusHrs = (Button) v.findViewById(R.id.timerPlusHrs);
        ui.timerPlusMins = (Button) v.findViewById(R.id.timerPlusMins);
        
        ui.timerPlusSecs = (Button) v.findViewById(R.id.timerPlusSecs);
        ui.timerMinusDays = (Button) v.findViewById(R.id.timerMinusDays);
        ui.timerMinusHrs = (Button) v.findViewById(R.id.timerMinusHrs);
        ui.timerMinusMins = (Button) v.findViewById(R.id.timerMinusMins);
        ui.timerMinusSecs = (Button) v.findViewById(R.id.timerMinusSecs);
        ui.timerIntervalTitleTextView = (TextView) v.findViewById(R.id.timerIntervalTitleTextView);
        ui.timerIntervalAddButton = (ImageButton) v.findViewById(R.id.timerIntervalAddButton);
        ui.timerIntervalXButton = (ImageButton) v.findViewById(R.id.timerIntervalXButton);
        ui.timerIntervalLeftButton = (ImageButton) v.findViewById(R.id.timerIntervalLeftButton);
        ui.timerIntervalRightButton = (ImageButton) v.findViewById(R.id.timerIntervalRightButton);
        ui.timerIntervalRepeatCheckBox = (CheckBox) v.findViewById(R.id.timerIntervalRepeatCheckBox);
        ui.timerProgressBar = (ProgressBar) v.findViewById(R.id.timerProgressBar);
        ui.timerNumBeepsTextView = (TextView) v.findViewById(R.id.timerNumBeepsTextView);
        ui.timerBeepsPlusButton = (Button) v.findViewById(R.id.timerBeepsPlusButton);
        ui.timerBeepsMinusButton = (Button) v.findViewById(R.id.timerBeepsMinusButton);
        ui.timerVibrateCheckBox = (CheckBox) v.findViewById(R.id.timerVibrateCheckBox);
        ui.timerBeepTextView = (TextView) v.findViewById(R.id.timerBeepTextView);
        
        ui.timerAddButton.setOnClickListener(this);
        ui.timerXButton.setOnClickListener(this);
        ui.timerLeftButton.setOnClickListener(this);
        ui.timerRightButton.setOnClickListener(this);
        ui.timerIntervalAddButton.setOnClickListener(this);
        ui.timerIntervalXButton.setOnClickListener(this);
        ui.timerIntervalLeftButton.setOnClickListener(this);
        ui.timerIntervalRightButton.setOnClickListener(this);
        ui.timerStartStopResumeButton.setOnClickListener(this);
        ui.timerResetButton.setOnClickListener(this);
        ui.timerTitleTextView.setOnClickListener(this);
        ui.timerIntervalRepeatCheckBox.setOnClickListener(this);
        ui.timerBeepsPlusButton.setOnClickListener(this);
        ui.timerBeepsMinusButton.setOnClickListener(this);
        ui.timerVibrateCheckBox.setOnClickListener(this);
        ui.timerBeepTextView.setOnClickListener(this);
		ui.timerDaysLinearLayout.setOnClickListener(this);
		ui.timerHrsLinearLayout.setOnClickListener(this);
		ui.timerMinsLinearLayout.setOnClickListener(this);
		ui.timerSecsLinearLayout.setOnClickListener(this);
		ui.timerTenthsLinearLayout.setOnClickListener(this);
		
        ui.timerPlusDays.setOnTouchListener(timePlusMinusOnTouchListener);
        ui.timerPlusHrs.setOnTouchListener(timePlusMinusOnTouchListener);
        ui.timerPlusMins.setOnTouchListener(timePlusMinusOnTouchListener);
        ui.timerPlusSecs.setOnTouchListener(timePlusMinusOnTouchListener);
        ui.timerMinusDays.setOnTouchListener(timePlusMinusOnTouchListener);
        ui.timerMinusHrs.setOnTouchListener(timePlusMinusOnTouchListener);
        ui.timerMinusMins.setOnTouchListener(timePlusMinusOnTouchListener);
        ui.timerMinusSecs.setOnTouchListener(timePlusMinusOnTouchListener);

        ui.timerPlusDays.setOnClickListener(timePlusMinusOnClickListener);
        ui.timerPlusHrs.setOnClickListener(timePlusMinusOnClickListener);
        ui.timerPlusMins.setOnClickListener(timePlusMinusOnClickListener);
        ui.timerPlusSecs.setOnClickListener(timePlusMinusOnClickListener);
        ui.timerMinusDays.setOnClickListener(timePlusMinusOnClickListener);
        ui.timerMinusHrs.setOnClickListener(timePlusMinusOnClickListener);
        ui.timerMinusMins.setOnClickListener(timePlusMinusOnClickListener);
        ui.timerMinusSecs.setOnClickListener(timePlusMinusOnClickListener);

        ui.timerDaysTextView.setTypeface(MainActivity.getDigitsTypeface(this));
        ui.timerHrsTextView.setTypeface(MainActivity.getDigitsTypeface(this));
        ui.timerMinsTextView.setTypeface(MainActivity.getDigitsTypeface(this));
        ui.timerSecsTextView.setTypeface(MainActivity.getDigitsTypeface(this));
        ui.timerTenthsTextView.setTypeface(MainActivity.getDigitsTypeface(this));
        ((TextView)v.findViewById(R.id.timerDaysBGTextView)).setTypeface(MainActivity.getDigitsTypeface(this));
        ((TextView)v.findViewById(R.id.timerHrsBGTextView)).setTypeface(MainActivity.getDigitsTypeface(this));
        ((TextView)v.findViewById(R.id.timerMinsBGTextView)).setTypeface(MainActivity.getDigitsTypeface(this));
        ((TextView)v.findViewById(R.id.timerSecsBGTextView)).setTypeface(MainActivity.getDigitsTypeface(this));
        ((TextView)v.findViewById(R.id.timerTenthsBGTextView)).setTypeface(MainActivity.getDigitsTypeface(this));
        ((TextView)v.findViewById(R.id.timerDaysBG2TextView)).setTypeface(MainActivity.getDigitsTypeface(this));
        ((TextView)v.findViewById(R.id.timerHrsBG2TextView)).setTypeface(MainActivity.getDigitsTypeface(this));
        ((TextView)v.findViewById(R.id.timerMinsBG2TextView)).setTypeface(MainActivity.getDigitsTypeface(this));
        ((TextView)v.findViewById(R.id.timerSecsBG2TextView)).setTypeface(MainActivity.getDigitsTypeface(this));
        ((TextView)v.findViewById(R.id.timerTenthsBG2TextView)).setTypeface(MainActivity.getDigitsTypeface(this));
        
        ui.timerTitleEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
		    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
		        if (actionId == EditorInfo.IME_ACTION_DONE) {
		        	saveTitle();
		            return true;
		        }
		        return false;
		    }
		});
        ui.timerTitleEditText.setOnKeyListener(new OnKeyListener() {
		    public boolean onKey(View v, int keyCode, KeyEvent event) {
		        if (event.getAction() == KeyEvent.ACTION_DOWN)
		            if (keyCode == KeyEvent.KEYCODE_ENTER) {
		            	saveTitle();
		                return true;
		            }
		        return false;
		    }
		});
		ui.timerTitleEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if(!hasFocus)
					saveTitle();
			}
		});
	}
    
    public void startLeftRightScreenHandlers() {
    	if(timer1!=null && timer1.running) {
			timer1Handler.post(timer1UpdateTimeTask);
    	}
    	if(timer3!=null && timer3.running) {
			timer3Handler.post(timer3UpdateTimeTask);
    	}
    }

    public void stopLeftRightScreenHandlers() {
    	if(timer1!=null) {
			timer1Handler.removeCallbacks(timer1UpdateTimeTask);
    	}
    	if(timer3!=null) {
    		timer3Handler.removeCallbacks(timer3UpdateTimeTask);
    	}
    }
    
    private void showToast(String text) {
		toast.cancel();
		toast.setText(text);
		toast.show();
    }
    
    
	Runnable navigateToPrevViewTask = new Runnable() {
		public void run() {
	    	if(deleteTimer==true) {
				int thisIntervalParentId = timer2.intervalParentId;
				String thisTimerTitle = timer2.title;
				resetAllOtherIntervals();
				timer2.reset();
				Timer.removeAlarmManager(TimerActivity.this, timer2);
				timer2=fetchPrevTimer(timer2.intervalParentId);
				getContentResolver().delete(URI, Timers.INTERVAL_PARENT_ID+"="+thisIntervalParentId, null);
				showToast("Timer '"+thisTimerTitle+"' deleted.");
				
		    	if(nextTimerExists(timer2.intervalParentId)) {
		    		timer3=fetchNextTimer(timer2.intervalParentId);
		        	setupDisplay(timer3UI, timer3, timer3Handler, null);
		    		realViewSwitcher.allowScrollNextView=true;
		    	}
		    	else {
		    		timer3=null;
		    		timer3Handler.removeCallbacks(timer3UpdateTimeTask);
		    		realViewSwitcher.allowScrollNextView=false;
		    	}
				
	    		deleteTimer=false;
	    	}
	    	else {
				timer3=timer2;
	        	setupDisplay(timer3UI, timer3, timer3Handler, null);
	    		realViewSwitcher.allowScrollNextView=true;
	    		
				timer2=fetchPrevTimer(timer2.intervalParentId);
	    	}

	    	setupDisplay(timer2UI, timer2, timer2Handler, timer2UpdateTimeTask);
			sharedPrefsEditor.putInt("TimerLastViewed", timer2.id);
			sharedPrefsEditor.commit();

	    	if(prevTimerExists(timer2.intervalParentId)) {
	    		timer1=fetchPrevTimer(timer2.intervalParentId);
	        	setupDisplay(timer1UI, timer1, timer1Handler, null);
	    		realViewSwitcher.allowScrollPrevView=true;
	    	}
	    	else {
	    		timer1=null;
	    		timer1Handler.removeCallbacks(timer1UpdateTimeTask);
	    		realViewSwitcher.allowScrollPrevView=false;
	    	}

	    	realViewSwitcher.setCurrentScreen(1);
	    }
	};
	
	Runnable navigateToNextViewTask = new Runnable() {
		public void run() {
	    	timer1=timer2;
	    	setupDisplay(timer1UI, timer1, timer1Handler, null);
			realViewSwitcher.allowScrollPrevView=true;

	    	timer2=fetchNextTimer(timer2.intervalParentId);
	    	setupDisplay(timer2UI, timer2, timer2Handler, timer2UpdateTimeTask);
			sharedPrefsEditor.putInt("TimerLastViewed", timer2.id);
			sharedPrefsEditor.commit();
	    	
	    	if(nextTimerExists(timer2.intervalParentId)) {
	    		timer3=fetchNextTimer(timer2.intervalParentId);
	        	setupDisplay(timer3UI, timer3, timer3Handler, null);
	    		realViewSwitcher.allowScrollNextView=true;
	    	}
	    	else {
	    		timer3=null;
	    		timer3Handler.removeCallbacks(timer3UpdateTimeTask);
	    		realViewSwitcher.allowScrollNextView=false;
	    	}
	    	
	    	realViewSwitcher.setCurrentScreen(1);
	    }
	};


    private void setupRealViewSwitcher() {
    	if(prevTimerExists(timer2.intervalParentId)) {
    		timer1=fetchPrevTimer(timer2.intervalParentId);
        	setupDisplay(timer1UI, timer1, timer1Handler, null);
    		realViewSwitcher.allowScrollPrevView=true;
    	}
    	else {
    		timer1=null;
    		realViewSwitcher.allowScrollPrevView=false;
    	}

    	setupDisplay(timer2UI, timer2, timer2Handler, timer2UpdateTimeTask);
		sharedPrefsEditor.putInt("TimerLastViewed", timer2.id);
		sharedPrefsEditor.commit();
    	
    	if(nextTimerExists(timer2.intervalParentId)) {
    		timer3=fetchNextTimer(timer2.intervalParentId);
    		setupDisplay(timer3UI, timer3, timer3Handler, null);
    		realViewSwitcher.allowScrollNextView=true;
    	}
    	else {
    		timer3=null;
    		realViewSwitcher.allowScrollNextView=false;
    	}

   		realViewSwitcher.setCurrentScreen(1);
    	
    	setContentView(realViewSwitcher);
    }
    
    public void showBeepMediaVolumeToast() {
    	DateFormat dateFormat = new SimpleDateFormat("MM-dd-yyyy");
    	Date date = new Date();
    	if(!sharedPrefs.getString("LastBeepMediaVolumeToastShown", "").equals(dateFormat.format(date))) {
			showToast("Please make sure that your Media Volume is turned up.");
			
			sharedPrefsEditor.putString("LastBeepMediaVolumeToastShown", dateFormat.format(date));
			sharedPrefsEditor.commit();
		}
    }

    public Timer fetchNextTimer(int intervalParentId) {
    	Timer timer = null;
    	cursor = this.getContentResolver().query(
    			URI, 
    			new String[] {Timers.TIMER_ID}, 
    			Timers.INTERVAL_PARENT_ID+">"+intervalParentId+" and "+Timers.INTERVAL+"=1", 
    			null, 
    			null);
    	if(cursor.moveToFirst())
    		timer=Timer.fetchTimerFromDB(this, URI, cursor.getInt(cursor.getColumnIndex(Timers.TIMER_ID)));
    	else
    		timer=Timer.fetchTimerFromDB(this, URI, 1);
    	cursor.close();
    	timer=fetchRunningOrStoppedInvervalIfAny(timer);
    	return(timer);
    }
    
    public Timer fetchPrevTimer(int intervalParentId) {
    	Timer timer = null;
    	cursor = this.getContentResolver().query(
    			URI, 
    			new String[] {Timers.TIMER_ID}, 
    			Timers.INTERVAL_PARENT_ID+"<"+intervalParentId+" and "+Timers.INTERVAL+"=1", 
    			null, 
    			Timers.INTERVAL_PARENT_ID+" desc");
    	if(cursor.moveToFirst())
    		timer=Timer.fetchTimerFromDB(this, URI, cursor.getInt(cursor.getColumnIndex(Timers.TIMER_ID)));
    	else
    		timer=Timer.fetchTimerFromDB(this, URI, 1);
    	cursor.close();
		timer=fetchRunningOrStoppedInvervalIfAny(timer);
		return(timer);
    }
    
    public boolean nextTimerExists(int intervalParentId) {
    	boolean exists=false;
    	cursor = this.getContentResolver().query(
    			URI, 
    			new String[] {Timers.TIMER_ID}, 
    			Timers.INTERVAL_PARENT_ID+">"+intervalParentId, 
    			null, 
    			null);
    	if(cursor.moveToFirst()) {
    		exists=true;
    	}
    	cursor.close();
    	return(exists);
    }
    
    public boolean prevTimerExists(int intervalParentId) {
    	boolean exists=false;
    	cursor = this.getContentResolver().query(
    			URI, 
    			new String[] {Timers.TIMER_ID}, 
    			Timers.INTERVAL_PARENT_ID+"<"+intervalParentId, 
    			null, 
    			null);
    	if(cursor.moveToFirst()) {
    		exists=true;
    	}
    	cursor.close();
    	return(exists);
    }

    public void navigateToNextInterval() {
    	cursor = this.getContentResolver().query(
    			URI, 
    			new String[] {Timers.TIMER_ID}, 
    			Timers.INTERVAL_PARENT_ID+"="+timer2.intervalParentId+" and "+Timers.INTERVAL+">"+timer2.interval,
    			null, 
    			null);
    	if(cursor.moveToFirst()) {
    		timer2=Timer.fetchTimerFromDB(this, URI, cursor.getInt(cursor.getColumnIndex(Timers.TIMER_ID)));
			sharedPrefsEditor.putInt("TimerLastViewed", timer2.id);
			sharedPrefsEditor.commit();
    	}
    	cursor.close();
    }

    public void navigateToPrevInterval() {
    	cursor = this.getContentResolver().query(
    			URI, 
    			new String[] {Timers.TIMER_ID}, 
    			Timers.INTERVAL_PARENT_ID+"="+timer2.intervalParentId+" and "+Timers.INTERVAL+"<"+timer2.interval,
    			null, 
    			Timers.INTERVAL+" desc");
    	if(cursor.moveToFirst()) {
    		timer2=Timer.fetchTimerFromDB(this, URI, cursor.getInt(cursor.getColumnIndex(Timers.TIMER_ID)));
			sharedPrefsEditor.putInt("TimerLastViewed", timer2.id);
			sharedPrefsEditor.commit();
    	}
    	cursor.close();
    }
    
    public boolean nextIntervalExists(int intervalParentId, int interval) {
    	boolean exists=false;
    	cursor = this.getContentResolver().query(
    			URI, 
    			new String[] {Timers.TIMER_ID}, 
    			Timers.INTERVAL_PARENT_ID+"="+intervalParentId+" and "+Timers.INTERVAL+">"+interval,
    			null, 
    			null);
    	if(cursor.moveToFirst()) {
    		exists=true;
    	}
    	cursor.close();
    	return(exists);
    }
    
    public Timer fetchRunningOrStoppedInvervalIfAny(Timer timer) {
    	Timer thisTimer = null;
    	cursor = this.getContentResolver().query(
    			URI, 
    			new String[] {Timers.TIMER_ID},
    			Timers.INTERVAL_PARENT_ID+"="+timer.intervalParentId+" and ("+Timers.REMAINING_TIME+"<>0 or "+Timers.RUNNING+"=1)", 
    			null, 
    			null);
    	if(cursor.moveToFirst()) {
    		thisTimer=Timer.fetchTimerFromDB(this, URI, cursor.getInt(cursor.getColumnIndex(Timers.TIMER_ID)));
    	}
    	cursor.close();
    	if(thisTimer==null)
    		return(timer);
    	else
    		return(thisTimer);
    }

    public void resetAllOtherIntervals() {
    	int currentTimerId = timer2.id;
    	cursor = this.getContentResolver().query(
    			URI, 
    			new String[] {Timers.TIMER_ID}, 
    			Timers.INTERVAL_PARENT_ID+"="+timer2.intervalParentId+" and "+Timers.INTERVAL+"<>"+timer2.interval+" and ("+Timers.REMAINING_TIME+"<>0 or "+Timers.RUNNING+"=1)", 
    			null, 
    			null);
    	if(cursor.moveToFirst()) {
    		timer2=Timer.fetchTimerFromDB(this, URI, cursor.getInt(cursor.getColumnIndex(Timers.TIMER_ID)));
    		cursor.close();
    		timer2.reset();
    		Timer.removeAlarmManager(TimerActivity.this, timer2);
    		Timer.updateTimerRecord(false,false,timer2,TimerActivity.this,URI);
    		timer2=Timer.fetchTimerFromDB(this, URI, currentTimerId);
    	}
    	else
    		cursor.close();
    }
    
    public void updateAllIntervalNbrOnDelete() {
    	cursor = this.getContentResolver().query(
    			URI, 
    			new String[] {Timers.TIMER_ID, Timers.INTERVAL}, 
    			Timers.INTERVAL_PARENT_ID+"="+timer2.intervalParentId+" and "+Timers.INTERVAL+">"+timer2.interval, 
    			null, 
    			null);
    	for(cursor.moveToFirst();!cursor.isAfterLast();cursor.moveToNext()) {
    		contentValues = new ContentValues();
    		contentValues.put(Timer.Timers.INTERVAL, (cursor.getInt(cursor.getColumnIndex(Timers.INTERVAL))-1));
    		this.getContentResolver().update(URI, contentValues, Timers.TIMER_ID+"="+cursor.getInt(cursor.getColumnIndex(Timers.TIMER_ID)), null);
    	}
    	cursor.close();
    }
    
    public int getNumIntervals(int intervalParentId) {
    	int numIntervals;
    	cursor=this.getContentResolver().query(
    			URI, 
    			new String[] {Timers.TIMER_ID}, 
    			Timers.INTERVAL_PARENT_ID+"="+intervalParentId, 
    			null, 
    			null);
    	numIntervals=cursor.getCount();
    	cursor.close();
    	return(numIntervals);
    }
    
	public Timer addTimer() {
		int lastTimerTitleNbr = sharedPrefs.getInt("LastTimerTitleNbr", 0)+1;
		sharedPrefsEditor.putInt("LastTimerTitleNbr", lastTimerTitleNbr);
		sharedPrefsEditor.commit();
		Timer timer = new Timer();
		contentValues = new ContentValues();
		contentValues.put(Timer.Timers.TITLE, "");
		contentValues.put(Timer.Timers.START_TIME, 0);
		contentValues.put(Timer.Timers.STOP_TIME, 0);
		contentValues.put(Timer.Timers.RUNNING, false);
		contentValues.put(Timer.Timers.INTERVAL, 1);
		contentValues.put(Timer.Timers.INTERVAL_PARENT_ID, 0);
		contentValues.put(Timer.Timers.REMAINING_TIME, 0);
		contentValues.put(Timer.Timers.LENGTH, 0);
		contentValues.put(Timer.Timers.REPEAT, false);
		contentValues.put(Timer.Timers.NUM_BEEPS, Integer.parseInt(sharedPrefs.getString("timerDefaultNumBeepsPref", getString(R.string.timerDefaultNumBeepsPrefDefaultValue)))); 
		contentValues.put(Timer.Timers.VIBRATE, sharedPrefs.getBoolean("timerDefaultVibratePref", (getString(R.string.timerDefaultVibratePrefDefaultValue).equals("true")?true:false)));
		contentValues.put(Timer.Timers.BEEP, sharedPrefs.getString("timerDefaultBeepPref", getString(R.string.timerDefaultBeepPrefDefaultValue)));
		timer.id=Integer.parseInt(this.getContentResolver().insert(URI, contentValues).getPathSegments().get(1));
		if(timer.id==1) 
			timer.title="Timer #1 (tap to edit)";
		else
			timer.title="Timer #"+lastTimerTitleNbr;
		timer.interval=1;
		timer.intervalParentId=timer.id;
		timer.numBeeps=Integer.parseInt(sharedPrefs.getString("timerDefaultNumBeepsPref", getString(R.string.timerDefaultNumBeepsPrefDefaultValue)));
		timer.vibrate=sharedPrefs.getBoolean("timerDefaultVibratePref", (getString(R.string.timerDefaultVibratePrefDefaultValue).equals("true")?true:false));
		timer.beep=sharedPrefs.getString("timerDefaultBeepPref", getString(R.string.timerDefaultBeepPrefDefaultValue));
		Timer.updateTimerRecord(true,false,timer,TimerActivity.this,URI);
		return(timer);
	}
	
	public Timer addInterval(String title, int interval, int intervalParentId, boolean currentRepeat) {
		Timer timer = new Timer();
		contentValues = new ContentValues();
		contentValues.put(Timer.Timers.TITLE, title);
		contentValues.put(Timer.Timers.START_TIME, 0);
		contentValues.put(Timer.Timers.STOP_TIME, 0);
		contentValues.put(Timer.Timers.RUNNING, false);
		contentValues.put(Timer.Timers.INTERVAL, interval);
		contentValues.put(Timer.Timers.INTERVAL_PARENT_ID, intervalParentId);
		contentValues.put(Timer.Timers.REMAINING_TIME, 0);
		contentValues.put(Timer.Timers.LENGTH, 0);
		contentValues.put(Timer.Timers.REPEAT, currentRepeat);
		contentValues.put(Timer.Timers.NUM_BEEPS, Integer.parseInt(sharedPrefs.getString("timerDefaultNumBeepsPref", getString(R.string.timerDefaultNumBeepsPrefDefaultValue))));
		contentValues.put(Timer.Timers.VIBRATE, sharedPrefs.getBoolean("timerDefaultVibratePref", (getString(R.string.timerDefaultVibratePrefDefaultValue).equals("true")?true:false)));
		contentValues.put(Timer.Timers.BEEP, sharedPrefs.getString("timerDefaultBeepPref", getString(R.string.timerDefaultBeepPrefDefaultValue)));
		timer.id=Integer.parseInt(this.getContentResolver().insert(URI, contentValues).getPathSegments().get(1));
		timer.title=title;
		timer.interval=interval;
		timer.intervalParentId=intervalParentId;
		timer.repeat=currentRepeat;
		timer.numBeeps=Integer.parseInt(sharedPrefs.getString("timerDefaultNumBeepsPref", getString(R.string.timerDefaultNumBeepsPrefDefaultValue)));
		timer.vibrate=sharedPrefs.getBoolean("timerDefaultVibratePref", (getString(R.string.timerDefaultVibratePrefDefaultValue).equals("true")?true:false));
		timer.beep=sharedPrefs.getString("timerDefaultBeepPref", getString(R.string.timerDefaultBeepPrefDefaultValue));
		Timer.updateTimerRecord(false,false,timer,TimerActivity.this,URI);
		return(timer);
	}
	
	public boolean isAnyIntervalForTimerRunning(int intervalParentId) {
        int runningIntervals;
		cursor = 
        	this.getContentResolver().query(
    			URI, 
    			new String[] {Timers.TIMER_ID,Timers.INTERVAL,Timers.RUNNING},
    			Timer.Timers.INTERVAL_PARENT_ID+"="+intervalParentId+" and "+Timer.Timers.RUNNING+"=1",
    			null, 
    			null
    		);  
		runningIntervals = cursor.getCount();
		cursor.close();
        if(runningIntervals>0)
        	return(true);
		return(false);
	}
	
	public void setupDisplay(TimerUI ui, Timer timer, Handler timerHandler, Runnable timerUpdateTimeTask) {
		ui.timerTitleEditText.setText(timer.title);
		ui.timerTitleTextView.setText(timer.title);
		ui.timerTitleEditText.setSelection(ui.timerTitleEditText.getText().length());
		ui.timerIntervalTitleTextView.setText("Interval "+timer.interval+"/"+getNumIntervals(timer.intervalParentId));
		if(timer.repeat)
			ui.timerIntervalRepeatCheckBox.setChecked(true);
		else
			ui.timerIntervalRepeatCheckBox.setChecked(false);
		setTime(ui,timer);
		if(timer.running==true) {
			ui.timerStartStopResumeButton.setText("Stop");
			ui.timerStartStopResumeButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_media_pause,0,0,0);
			timer.resume=true;
			timerHandler.post(timerUpdateTimeTask);
		}
		else {
			if(timer.remainingTime!=0)
				ui.timerStartStopResumeButton.setText("Resume");
			else
				ui.timerStartStopResumeButton.setText("Start");
			ui.timerStartStopResumeButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_media_play,0,0,0);
			timerHandler.removeCallbacks(timerUpdateTimeTask);
		}
		if(isAnyIntervalForTimerRunning(timer.intervalParentId)) {
			ui.timerProgressBar.setVisibility(View.VISIBLE);
		}
		else
			ui.timerProgressBar.setVisibility(View.GONE);
		ui.timerNumBeepsTextView.setText(""+timer.numBeeps);
		if(timer.vibrate)
			ui.timerVibrateCheckBox.setChecked(true);
		else
			ui.timerVibrateCheckBox.setChecked(false);
		
		ui.timerBeepTextView.setText(convertBeepToFormatedString(timer.beep));

		ui.timerXButton.setEnabled(false);
		ui.timerLeftButton.setEnabled(false);
		ui.timerXButton.setVisibility(View.VISIBLE);
		ui.timerLeftButton.setVisibility(View.VISIBLE);
		if(!prevTimerExists(timer.intervalParentId)) {
			ui.timerXButton.setEnabled(false);
			ui.timerLeftButton.setEnabled(false);
			ui.timerXButton.setVisibility(View.GONE);
			ui.timerLeftButton.setVisibility(View.GONE);
		}
		else {
			ui.timerXButton.setEnabled(true);
			ui.timerLeftButton.setEnabled(true);
			ui.timerXButton.setVisibility(View.VISIBLE);
			ui.timerLeftButton.setVisibility(View.VISIBLE);
		}
		if(!nextTimerExists(timer.intervalParentId)) {
			ui.timerRightButton.setEnabled(false);
			ui.timerRightButton.setVisibility(View.GONE);
		}
		else {
			ui.timerRightButton.setEnabled(true);
			ui.timerRightButton.setVisibility(View.VISIBLE);
		}

		ui.timerIntervalXButton.setEnabled(false);
		ui.timerIntervalLeftButton.setEnabled(false);
		ui.timerIntervalXButton.setVisibility(View.VISIBLE);
		ui.timerIntervalLeftButton.setVisibility(View.VISIBLE);
		if(timer.interval==1) {
			ui.timerIntervalXButton.setEnabled(false);
			ui.timerIntervalLeftButton.setEnabled(false);
			ui.timerIntervalXButton.setVisibility(View.GONE);
			ui.timerIntervalLeftButton.setVisibility(View.GONE);
		}
		else {
			ui.timerIntervalXButton.setEnabled(true);
			ui.timerIntervalLeftButton.setEnabled(true);
			ui.timerIntervalXButton.setVisibility(View.VISIBLE);
			ui.timerIntervalLeftButton.setVisibility(View.VISIBLE);
		}
		if(!nextIntervalExists(timer.intervalParentId, timer.interval)) {
			ui.timerIntervalRightButton.setEnabled(false);
			ui.timerIntervalRightButton.setVisibility(View.GONE);
		}
		else {
			ui.timerIntervalRightButton.setEnabled(true);
			ui.timerIntervalRightButton.setVisibility(View.VISIBLE);
		}
	}
	
	public void setTime(TimerUI ui, Timer timer) {
		long milliseconds = 0;
		if(timer.getElapsedTime()>0)
			milliseconds = timer.getElapsedTime();
		else
			milliseconds = timer.length;
		HashMap<String,Integer> timeValuesHashMap = calculateTime(milliseconds);
		int tenth_of_seconds = timeValuesHashMap.get("tenth_of_seconds");
		int seconds = timeValuesHashMap.get("seconds");
		int minutes = timeValuesHashMap.get("minutes");
		int hours   = timeValuesHashMap.get("hours");
		int days    = timeValuesHashMap.get("days");
		ui.timerDaysTextView.setText(""+(days<10?"0"+days:days));
		ui.timerHrsTextView.setText(""+(hours<10?"0"+hours:hours));
		ui.timerMinsTextView.setText(""+(minutes<10?"0"+minutes:minutes));
		ui.timerSecsTextView.setText(""+(seconds<10?"0"+seconds:seconds));
		ui.timerTenthsTextView.setText(""+(tenth_of_seconds<10?"0"+tenth_of_seconds:tenth_of_seconds));
	}

	public HashMap<String,Integer> calculateTime(long milliseconds)
	{
		HashMap<String,Integer> timeValuesHashMap = new HashMap<String,Integer>();
		int tenth_of_seconds = (int) (milliseconds % 1000 / 10);
		int seconds = (int) ((milliseconds / 1000) % 60);
		int minutes = (int) (((milliseconds / 1000) / 60) % 60);
		int hours   = (int) (((milliseconds / 1000) / 3600) % 24);
		int days    = (int) ((milliseconds / 1000) / 86400);
		timeValuesHashMap.put("tenth_of_seconds",tenth_of_seconds);
		timeValuesHashMap.put("seconds",seconds);
		timeValuesHashMap.put("minutes",minutes);
		timeValuesHashMap.put("hours",hours);
		timeValuesHashMap.put("days",days);
		return timeValuesHashMap;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {    
		MainActivity.hapticFeedback(TimerActivity.this);
		switch (item.getItemId()) {    
		case R.id.timerStopAllMenuItem:        
			Timer.stopAllTimers(this,URI);
			if(timer1!=null) {
				timer1=Timer.fetchTimerFromDB(this, URI, timer1.id);
				setupDisplay(timer1UI, timer1, timer1Handler, null);
			}

			timer2=Timer.fetchTimerFromDB(this, URI, timer2.id);
			setupDisplay(timer2UI, timer2, timer2Handler, timer2UpdateTimeTask);
			
			if(timer3!=null) {
				timer3=Timer.fetchTimerFromDB(this, URI, timer3.id);
				setupDisplay(timer3UI, timer3, timer3Handler, null);
			}
			return true;
		case R.id.prefsMenuItem:
            startActivity(new Intent(this, PreferencesActivity.class));
			return true;
		case R.id.whatsNewMenuItem:
			MainActivity.showChangeLog(this, true);
			return true;
		default:        
			return false;
		}
	}
	
	TextWatcher titleTextWatcher = new TextWatcher() {
    	public void afterTextChanged(Editable s) {
    		if(!timer2.title.equals(s.toString())) {
	    		timer2.title=s.toString();
	    		Timer.updateTimerRecord(true,false,timer2,TimerActivity.this,URI);
    		}
    	}                
    	public void beforeTextChanged(CharSequence s, int start, int count, int after) {                
    	}                
    	public void onTextChanged(CharSequence s, int start, int before, int count) {                    
    	}            
	};
	
	OnClickListener timePlusMinusOnClickListener = new OnClickListener() {
		public void onClick(View v) {
			switchPlusMinusTime(v.getId());
		}
	};
	
	OnTouchListener timePlusMinusOnTouchListener = new OnTouchListener() {
		public boolean onTouch(View v, MotionEvent event) {
			if(event.getAction() == MotionEvent.ACTION_DOWN) {
				MainActivity.hapticFeedback(TimerActivity.this);
				timePlusMinusButtonId = v.getId();
				timePlusMinusLongPressIteration=0;
				timePlusMinusLongPressHandler.post(timePlusMinusLongPressTask);
				v.setPressed(true);
				
			} 
			else if (event.getAction() == MotionEvent.ACTION_UP) {            
				timePlusMinusButtonId = 0;
				timePlusMinusLongPressIteration=0;
				timePlusMinusLongPressHandler.removeCallbacks(timePlusMinusLongPressTask);
				v.setPressed(false);
			}
			return(true);
		}		
	};

	Runnable timer1UpdateTimeTask = new Runnable() {
		public void run() {
			setTime(timer1UI,timer1);
			if(timer1.getElapsedTime() > 0) {
				timer1Handler.postDelayed(this, MainActivity.DISPLAY_UPDATE_INTERVAL);
			}
		}
	};	

	Runnable timer2UpdateTimeTask = new Runnable() {
		public void run() {
			setTime(timer2UI,timer2);
			if(timer2.getElapsedTime() > 0) {
				timer2Handler.postDelayed(this, MainActivity.DISPLAY_UPDATE_INTERVAL);
			}
		}
	};	

	Runnable timer3UpdateTimeTask = new Runnable() {
		public void run() {
			setTime(timer3UI,timer3);
			if(timer3.getElapsedTime() > 0) {
				timer3Handler.postDelayed(this, MainActivity.DISPLAY_UPDATE_INTERVAL);
			}
		}
	};	

	
	Runnable timePlusMinusLongPressTask = new Runnable() {
		public void run() {
			if(timePlusMinusButtonId == 0) {
				timePlusMinusLongPressHandler.removeCallbacks(timePlusMinusLongPressTask);
				return;
			}
			else {
				switchPlusMinusTime(timePlusMinusButtonId);
				if(timePlusMinusLongPressIteration<3) {
					timePlusMinusLongPressIteration++;
					timePlusMinusLongPressHandler.postDelayed(this,500);
				}
				else if(timePlusMinusLongPressIteration<10) {
					timePlusMinusLongPressIteration++;
					timePlusMinusLongPressHandler.postDelayed(this,100);
				}
				else {
					timePlusMinusLongPressHandler.post(this);
				}
			}

		}
	};

	private void switchPlusMinusTime(int whichOne) {
		long increment = 0;
		switch(whichOne) {
		case R.id.timerPlusDays:
			increment=86400000;
			break;
		case R.id.timerPlusHrs:
			increment=3600000; 
			break;
		case R.id.timerPlusMins:
			increment=60000;
			break;
		case R.id.timerPlusSecs:
			increment=1000;
			break;
		case R.id.timerMinusDays: 
			increment=-86400000;
			break;
		case R.id.timerMinusHrs:
			increment=-3600000;
			break;
		case R.id.timerMinusMins: 
			increment=-60000;
			break;
		case R.id.timerMinusSecs:
			increment=-1000;
			break;
		}
		if(timer2.running) {
			Timer.removeAlarmManager(TimerActivity.this, timer2);
			timer2.stop();
			if(timer2.remainingTime+increment>0)
				timer2.remainingTime+=increment;
			timer2.start();
			Timer.addAlarmManager(TimerActivity.this, timer2);
		}
		else if(!timer2.running && timer2.remainingTime>0) {
			if(timer2.remainingTime+increment>0)
				timer2.remainingTime+=increment;
			else if(timer2.remainingTime+increment<=0) {
	            showToast("Timer reset to original length.");
	            timer2UI.timerResetButton.performClick();
			}
		}
		else {
			if(timer2.length+increment>=0)
				timer2.length+=increment;
		}
		Timer.updateTimerRecord(false,false,timer2,TimerActivity.this,URI);
		setTime(timer2UI,timer2);
	}
	
	private void receivedBroadcast(Intent intent) {
		if(intent.getAction().equalsIgnoreCase(MainActivity.class.getPackage().getName()+".timer_expired")) {
	        if(timer1!=null && intent.getExtras().getInt(Timer.Timers.INTERVAL_PARENT_ID)==timer1.intervalParentId) {
	        	if(intent.getExtras().getInt("next"+Timer.Timers.TIMER_ID, 0)!=0) {
		        	timer1=Timer.fetchTimerFromDB(TimerActivity.this, URI, intent.getExtras().getInt("next"+Timer.Timers.TIMER_ID));
	        	}
	        	setupDisplay(timer1UI, timer1, timer1Handler, null);
	        }
	        else if(intent.getExtras().getInt(Timer.Timers.INTERVAL_PARENT_ID)==timer2.intervalParentId) {
	        	if(intent.getExtras().getInt("next"+Timer.Timers.TIMER_ID, 0)!=0) {
		        	timer2=Timer.fetchTimerFromDB(TimerActivity.this, URI, intent.getExtras().getInt("next"+Timer.Timers.TIMER_ID));
	        	}
	        	setupDisplay(timer2UI, timer2, timer2Handler, timer2UpdateTimeTask);
	        }
	        else if(timer3!=null && intent.getExtras().getInt(Timer.Timers.INTERVAL_PARENT_ID)==timer3.intervalParentId) {
	        	if(intent.getExtras().getInt("next"+Timer.Timers.TIMER_ID, 0)!=0) {
		        	timer3=Timer.fetchTimerFromDB(TimerActivity.this, URI, intent.getExtras().getInt("next"+Timer.Timers.TIMER_ID));
	        	}
	        	setupDisplay(timer3UI, timer3, timer3Handler, null);
	        }
		}
	}

    public static class TimerActivityBroadcastReceiver extends BroadcastReceiver {
		private TimerActivity timerActivity;

		public TimerActivityBroadcastReceiver() {
		}

		public TimerActivityBroadcastReceiver(TimerActivity timerActivity) {
			this.timerActivity = timerActivity;
		}

    	@Override        
    	public void onReceive(Context context, Intent intent) {            
    		timerActivity.receivedBroadcast(intent);
    	}    
    }

	@Override
	public boolean dispatchTouchEvent(MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			View v = getCurrentFocus();
			if ( v instanceof EditText) {
				Rect outRect = new Rect();
				v.getGlobalVisibleRect(outRect);
				if (!outRect.contains((int)event.getRawX(), (int)event.getRawY())) {
					v.clearFocus();
					InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
				}
			}
		}
		return super.dispatchTouchEvent(event);
	}

	private void saveTitle()
	{
		timer2UI.timerTitleTextView.setText(timer2UI.timerTitleEditText.getText());
		timer2UI.timerMainRowEditTitleLinearLayout.setVisibility(View.GONE);
		timer2UI.timerMainRowLinearLayout.setVisibility(View.VISIBLE);
	}

	public class InputFilterMinMax implements InputFilter {
		private int min;
		private int max;

		public InputFilterMinMax(int min, int max) {
			this.min = min;
			this.max = max;
		}

		@Override
		public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
			try {
				int input = Integer.parseInt(dest.subSequence(0, dstart).toString() + source + dest.subSequence(dend, dest.length()));
				if (isInRange(min, max, input))
					return null;
			} catch (NumberFormatException nfe) { }
			return "";
		}

		private boolean isInRange(int a, int b, int c) {
			return b > a ? c >= a && c <= b : c >= b && c <= a;
		}

	}

	private void displayEnterTimeDialog()
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		LayoutInflater inflater = this.getLayoutInflater();

		View timerTimeEntryView = inflater.inflate(R.layout.timer_time_entry, null);
		final EditText daysEditText = (EditText) timerTimeEntryView.findViewById(R.id.daysEditText);
		final EditText hrsEditText = (EditText) timerTimeEntryView.findViewById(R.id.hrsEditText);
		final EditText minsEditText = (EditText) timerTimeEntryView.findViewById(R.id.minsEditText);
		final EditText secsEditText = (EditText) timerTimeEntryView.findViewById(R.id.secsEditText);

		long milliseconds = timer2.length;
		HashMap<String,Integer> timeValuesHashMap = calculateTime(milliseconds);
		int seconds = timeValuesHashMap.get("seconds");
		int minutes = timeValuesHashMap.get("minutes");
		int hours   = timeValuesHashMap.get("hours");
		int days    = timeValuesHashMap.get("days");
		daysEditText.setText(""+days);
		hrsEditText.setText(""+hours);
		minsEditText.setText(""+minutes);
		secsEditText.setText(""+seconds);

		View.OnFocusChangeListener onFocusChangeListener = new View.OnFocusChangeListener() {
			public void onFocusChange(View v, boolean hasFocus) {
				if(!hasFocus) {
					if (((EditText)v).getText().toString().length()==0)
						((EditText)v).setText(""+0);
				}
			}
		};
		daysEditText.setOnFocusChangeListener(onFocusChangeListener);
		hrsEditText.setOnFocusChangeListener(onFocusChangeListener);
		minsEditText.setOnFocusChangeListener(onFocusChangeListener);
		secsEditText.setOnFocusChangeListener(onFocusChangeListener);

		hrsEditText.setFilters(new InputFilter[]{new InputFilterMinMax(0, 23)});
		minsEditText.setFilters(new InputFilter[]{new InputFilterMinMax(0, 59)});
		secsEditText.setFilters(new InputFilter[]{new InputFilterMinMax(0, 59)});

		builder.setView(timerTimeEntryView)
				.setMessage("Enter a time")
				.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int id) {
						if(timer2.running) {
							timer2UI.timerResetButton.performClick();
						}
						int days = Integer.parseInt((daysEditText.getText().toString().length()==0?"0":daysEditText.getText().toString()));
						int hrs = Integer.parseInt((hrsEditText.getText().toString().length()==0?"0":hrsEditText.getText().toString()));
						int mins = Integer.parseInt((minsEditText.getText().toString().length()==0?"0":minsEditText.getText().toString()));
						int secs = Integer.parseInt((secsEditText.getText().toString().length()==0?"0":secsEditText.getText().toString()));
						timer2.length = (days*86400000) + (hrs*3600000) + (mins*60000) + (secs*1000);
						Timer.updateTimerRecord(false,false,timer2,TimerActivity.this,URI);
						setTime(timer2UI,timer2);
					}
				})
				.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {

					}
				});
		builder.create().show();

	}

	public void onClick(View v) {
		MainActivity.hapticFeedback(TimerActivity.this);
		switch(v.getId()) {
			case R.id.timerAddButton:
				timer3=addTimer();
				setupDisplay(timer3UI, timer3, timer3Handler, null);
				timer2=fetchPrevTimer(timer3.intervalParentId);
				setupDisplay(timer2UI, timer2, timer2Handler, null);
				realViewSwitcher.snapToScreenExternal(2);
				showToast("New Timer added.");
				break;
			case R.id.timerXButton:
				alertDialogClickListener = new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						MainActivity.hapticFeedback(TimerActivity.this);
						switch (which) {
						case DialogInterface.BUTTON_POSITIVE:
							deleteTimer=true;
							if(timer1!=null && timer1.running)
								timer1Handler.post(timer1UpdateTimeTask);
							realViewSwitcher.snapToScreenExternal(0);
							break;
						case DialogInterface.BUTTON_NEGATIVE:
							break;
						}
					}};
				alertDialog = new AlertDialog.Builder(TimerActivity.this);
				alertDialog.setIcon(android.R.drawable.ic_dialog_alert);
				alertDialog.setTitle("Confirm Delete Timer");
				alertDialog.setMessage("Are you sure you want to delete this timer and all its intervals?");
				alertDialog.setPositiveButton("Yes", alertDialogClickListener);
				alertDialog.setNegativeButton("No", alertDialogClickListener);
				alertDialog.setCancelable(true);
				alertDialog.create().show();
				break;
			case R.id.timerLeftImageButton:
				if(timer1!=null) {
					if(timer1.running)
						timer1Handler.post(timer1UpdateTimeTask);
					setTime(timer1UI,timer1);
					realViewSwitcher.snapToScreenExternal(0);
				}
				break;
			case R.id.timerRightImageButton:
				if(timer3!=null) {
					if(timer3.running)
						timer3Handler.post(timer3UpdateTimeTask);
					setTime(timer3UI,timer3);
					realViewSwitcher.snapToScreenExternal(2);
				}
				break;
			case R.id.timerIntervalAddButton:
				String currentTimerTitle = timer2.title;
				int nextInterval = getNumIntervals(timer2.intervalParentId)+1;
				int currentIntervalParentId = timer2.intervalParentId;
				boolean currentRepeat = timer2.repeat;
				timer2=addInterval(currentTimerTitle, nextInterval, currentIntervalParentId, currentRepeat);
				setupDisplay(timer2UI, timer2, timer2Handler, timer2UpdateTimeTask);
				showToast("New interval added. Note: each interval has its own beep and vibrate settings.");
				break;
			case R.id.timerIntervalXButton:
				alertDialogClickListener = new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						MainActivity.hapticFeedback(TimerActivity.this);
						switch (which) {
						case DialogInterface.BUTTON_POSITIVE:
							int thisTimerId=timer2.id;
							timer2.reset();
							Timer.removeAlarmManager(TimerActivity.this, timer2);
							updateAllIntervalNbrOnDelete();
							navigateToPrevInterval();
							getContentResolver().delete(URI, Timers.TIMER_ID+"="+thisTimerId, null);
							setupDisplay(timer2UI, timer2, timer2Handler, timer2UpdateTimeTask);
							break;
						case DialogInterface.BUTTON_NEGATIVE:
							break;
						}
					}};
				alertDialog = new AlertDialog.Builder(TimerActivity.this);
				alertDialog.setIcon(android.R.drawable.ic_dialog_alert);
				alertDialog.setTitle("Confirm Delete Interval");
				alertDialog.setMessage("Are you sure you want to delete this interval?");
				alertDialog.setPositiveButton("Yes", alertDialogClickListener);
				alertDialog.setNegativeButton("No", alertDialogClickListener);
				alertDialog.setCancelable(true);
				alertDialog.create().show();
				break;
			case R.id.timerIntervalLeftButton:
				navigateToPrevInterval();
				setupDisplay(timer2UI, timer2, timer2Handler, timer2UpdateTimeTask);
				break;
			case R.id.timerIntervalRightButton:
				navigateToNextInterval();
				setupDisplay(timer2UI, timer2, timer2Handler, timer2UpdateTimeTask);
				break;
			case R.id.timerStartStopResumeButton:
				if(!timer2.resume && timer2.length>0) {
					resetAllOtherIntervals();
					timer2UI.timerStartStopResumeButton.setText("Stop");
					timer2UI.timerStartStopResumeButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_media_pause,0,0,0);
					timer2.resume=true;
					timer2.start();
					Timer.addAlarmManager(TimerActivity.this, timer2);
					Timer.updateTimerRecord(false,false,timer2,TimerActivity.this,URI);
					timer2UI.timerProgressBar.setVisibility(View.VISIBLE);
					timer2Handler.post(timer2UpdateTimeTask);
				}
				else if(timer2.length>0) {
					timer2UI.timerStartStopResumeButton.setText("Resume");
					timer2UI.timerStartStopResumeButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_media_play,0,0,0);
					timer2.resume=false;
					timer2.stop();
					Timer.removeAlarmManager(TimerActivity.this, timer2);
					Timer.updateTimerRecord(false,false,timer2,TimerActivity.this,URI);
					timer2Handler.removeCallbacks(timer2UpdateTimeTask);
					timer2UI.timerProgressBar.setVisibility(View.GONE);
					setTime(timer2UI,timer2);
				}
				else if(timer2.length<=0) {
					showToast("Timer has no duration. You may add time with the + buttons.");
				}
				break;
			case R.id.timerResetButton:
				timer2UI.timerStartStopResumeButton.setText("Start");
				timer2UI.timerStartStopResumeButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_media_play,0,0,0);
				timer2.reset();
				Timer.removeAlarmManager(TimerActivity.this, timer2);
				resetAllOtherIntervals();
				Timer.updateTimerRecord(false,false,timer2,TimerActivity.this,URI);
				timer2Handler.removeCallbacks(timer2UpdateTimeTask);
				timer2UI.timerProgressBar.setVisibility(View.GONE);
				setTime(timer2UI,timer2);
				break;
			case R.id.timerTitleTextView:
				timer2UI.timerTitleEditText.requestFocus();
				timer2UI.timerTitleEditText.requestFocusFromTouch();
				timer2UI.timerMainRowLinearLayout.setVisibility(View.GONE);
				timer2UI.timerMainRowEditTitleLinearLayout.setVisibility(View.VISIBLE);
				break;
			case R.id.timerIntervalRepeatCheckBox:
				timer2.repeat=timer2UI.timerIntervalRepeatCheckBox.isChecked();
				Timer.updateTimerRecord(false,true,timer2,TimerActivity.this,URI);
				String text = null;
				if(timer2.repeat) {
					if(getNumIntervals(timer2.intervalParentId)==1)
						text = "This timer will repeat once it has expired.";
					else
						text = "This timer will repeat once all its intervals have expired.";
				}
				else
					text = "This timer will not repeat.";
				showToast(text);
				break;
			case R.id.timerBeepsPlusButton:
				timer2.numBeeps++;
				Timer.updateTimerRecord(false,false,timer2,TimerActivity.this,URI);
				timer2UI.timerNumBeepsTextView.setText(""+timer2.numBeeps);
				showBeepMediaVolumeToast();
				break;
			case R.id.timerBeepsMinusButton:
				if(timer2.numBeeps>0) {
					timer2.numBeeps--;
					Timer.updateTimerRecord(false,false,timer2,TimerActivity.this,URI);
					timer2UI.timerNumBeepsTextView.setText(""+timer2.numBeeps);
					if(timer2.numBeeps>0)
						showBeepMediaVolumeToast();
				}
				break;
			case R.id.timerVibrateCheckBox:
				timer2.vibrate=timer2UI.timerVibrateCheckBox.isChecked();
				Timer.updateTimerRecord(false,false,timer2,TimerActivity.this,URI);
				if(timer2.vibrate) {
					if(getNumIntervals(timer2.intervalParentId)==1)
						text = "This timer will vibrate"+(timer2.numBeeps>0?" (and beep) ":" ")+"once it has expired.";
					else
						text = "Each interval in this timer will vibrate"+(timer2.numBeeps>0?" (and beep) ":" ")+"once it expires.";
				}
				else
					text = "This timer will not vibrate.";
				showToast(text);
				break;
			case R.id.timerBeepTextView:
				displayBeepPicker(this,true,timer2,timer2UI,null);
				break;
			case R.id.timerDaysLinearLayout:
				displayEnterTimeDialog();
				break;
			case R.id.timerHrsLinearLayout:
				displayEnterTimeDialog();
				break;
			case R.id.timerMinsLinearLayout:
				displayEnterTimeDialog();
				break;
			case R.id.timerSecsLinearLayout:
				displayEnterTimeDialog();
				break;
			case R.id.timerTenthsLinearLayout:
				displayEnterTimeDialog();
				break;

		}
	}
}
