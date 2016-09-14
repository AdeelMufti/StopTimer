package com.synappze.stoptimer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Rect;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.synappze.stoptimer.Stopwatch.Stopwatches;

public class StopwatchActivity extends Activity implements OnClickListener {

	private class StopwatchUI {
		
		public EditText swTitleEditText = null;
		public ImageButton swAddButton = null;
		public ImageButton swXButton = null;
		public ImageButton swLeftButton = null;
		public ImageButton swRightButton = null;
		public TextView swDaysTextView = null;
		private TextView swHrsTextView = null;
		public TextView swMinsTextView = null;
		public TextView swSecsTextView = null;
		public TextView swTenthsTextView = null;
		public Button swStartStopResumeButton = null;
		public Button swResetButton = null;
		public LinearLayout swMainRowLinearLayout = null;
		public LinearLayout swMainRowEditTitleLinearLayout = null;
		public TextView swTitleTextView = null;
		//public Button swLapButton = null;
		public ProgressBar swProgressBar = null;
	}
	
	private Stopwatch stopwatch1 = null;
	private Stopwatch stopwatch2 = null;
	private Stopwatch stopwatch3 = null;
	private View stopwatch1View = null;
	private View stopwatch2View = null;
	private View stopwatch3View = null;
	private StopwatchUI stopwatch1UI = new StopwatchUI();
	private StopwatchUI stopwatch2UI = new StopwatchUI();
	private StopwatchUI stopwatch3UI = new StopwatchUI();
	private Handler stopwatch1Handler = null;
	private Handler stopwatch2Handler = null;
	private Handler stopwatch3Handler = null;
	private RealViewSwitcher realViewSwitcher = null;
	private boolean deleteStopwatch = false;

	private Handler screenSwitchHandler = null;
	
	private ContentValues contentValues = null;
	private Cursor cursor = null;
	private SharedPreferences sharedPrefs = null;
	private SharedPreferences.Editor sharedPrefsEditor = null; 
	private DialogInterface.OnClickListener alertDialogClickListener = null;
	private AlertDialog.Builder alertDialog = null;
	private Toast toast = null;
	
	//private static final String TAG = "StopwatchActivity";
	public static final Uri URI = Stopwatch.Stopwatches.CONTENT_URI;
	public static final int ACTIVITY_TAB_NUMBER = 1;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.stopwatch);

		screenSwitchHandler = new Handler();

		stopwatch1Handler = new Handler();
		stopwatch2Handler = new Handler();
		stopwatch3Handler = new Handler();

    	stopwatch1View = getLayoutInflater().inflate(R.layout.stopwatch, null);
    	stopwatch2View = getLayoutInflater().inflate(R.layout.stopwatch, null);
    	stopwatch3View = getLayoutInflater().inflate(R.layout.stopwatch, null);
		stopwatch1UI = new StopwatchUI();
		stopwatch2UI = new StopwatchUI();
		stopwatch3UI = new StopwatchUI();
    	setupStopwatchViewUI(stopwatch1View,stopwatch1UI);
    	setupStopwatchViewUI(stopwatch2View,stopwatch2UI);
    	setupStopwatchViewUI(stopwatch3View,stopwatch3UI);

    	realViewSwitcher = new RealViewSwitcher(getApplicationContext());
    	realViewSwitcher.setupStopwatchActivity(this);
		realViewSwitcher.setOnScreenSwitchListener(realViewOnScreenSwitchListener);
   		realViewSwitcher.addView(stopwatch1View);
    	realViewSwitcher.addView(stopwatch2View);
   		realViewSwitcher.addView(stopwatch3View);

		sharedPrefs = getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE);
		sharedPrefsEditor = sharedPrefs.edit();

   		toast = Toast.makeText(this, "", Toast.LENGTH_LONG);

   		setVolumeControlStream(AudioManager.STREAM_MUSIC);
	}


	public boolean onKeyDown(int keyCode, KeyEvent event) {
	    if (keyCode == KeyEvent.KEYCODE_VOLUME_UP && sharedPrefs.getString("volumeButtonsPref", getString(R.string.volumeButtonsPrefDefaultValue)).equals("0")) 
	    {
	    	stopwatch2UI.swStartStopResumeButton.performClick();
	        return(true);
	    }
	    else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN && sharedPrefs.getString("volumeButtonsPref", getString(R.string.volumeButtonsPrefDefaultValue)).equals("0"))
	    {
	    	stopwatch2UI.swResetButton.performClick();
	        return(true);
	    }
	    return super.onKeyDown(keyCode, event);
	}

	
	private final RealViewSwitcher.OnScreenSwitchListener realViewOnScreenSwitchListener = new RealViewSwitcher.OnScreenSwitchListener() {
		public void onScreenSwitched(int screen) {
			if(screen==0)
				screenSwitchHandler.post(navigateToPrevViewTask);
			else if(screen==2) 
				screenSwitchHandler.post(navigateToNextViewTask);
		}
	};
	
    private void showToast(String text) {
		toast.cancel();
		toast.setText(text);
		toast.show();
    }
	
	public void onPause() {
		super.onPause();
		
		stopwatch2UI.swTitleEditText.removeTextChangedListener(titleTextWatcher);
    	
		stopwatch1Handler.removeCallbacks(stopwatch1UpdateTimeTask);
		stopwatch2Handler.removeCallbacks(stopwatch2UpdateTimeTask);
		stopwatch3Handler.removeCallbacks(stopwatch3UpdateTimeTask);
	}

	@Override
    public void onResume() {
    	super.onResume();
		sharedPrefsEditor.putInt("LastMainActivity", ACTIVITY_TAB_NUMBER);
		sharedPrefsEditor.commit();
		
		int count;
		cursor = this.getContentResolver().query(URI, new String[] {Stopwatches.STOPWATCH_ID}, null, null, null);
		count = cursor.getCount();
		cursor.close();
		if(count==0) {
			stopwatch2=addRecord();
		}
		else {

			int stopWatchLastViewed = sharedPrefs.getInt("StopwatchLastViewed", 1);
			
			stopwatch2=Stopwatch.fetchStopwatchFromDB(this, URI, stopWatchLastViewed);
			if(stopwatch2==null)
				stopwatch2=Stopwatch.fetchStopwatchFromDB(this, URI, 1);
		}
		
		setupRealViewSwitcher();
		
		stopwatch2UI.swTitleEditText.addTextChangedListener(titleTextWatcher);
    }	
    
    private void setupStopwatchViewUI(View v, final StopwatchUI ui) {
		ui.swTitleEditText = (EditText) v.findViewById(R.id.swTitleEditText);
		ui.swAddButton = (ImageButton) v.findViewById(R.id.swAddButton);
		ui.swXButton = (ImageButton) v.findViewById(R.id.swXButton);
		ui.swLeftButton = (ImageButton) v.findViewById(R.id.swLeftImageButton);
		ui.swRightButton = (ImageButton) v.findViewById(R.id.swRightImageButton);
		ui.swDaysTextView = (TextView) v.findViewById(R.id.swDaysTextView);
		ui.swHrsTextView = (TextView) v.findViewById(R.id.swHrsTextView);
		ui.swMinsTextView = (TextView) v.findViewById(R.id.swMinsTextView);
		ui.swSecsTextView = (TextView) v.findViewById(R.id.swSecsTextView);
		ui.swTenthsTextView = (TextView) v.findViewById(R.id.swTenthsTextView);
		ui.swStartStopResumeButton = (Button) v.findViewById(R.id.swStartStopResumeButton);
		ui.swResetButton = (Button) v.findViewById(R.id.swResetButton);
		ui.swMainRowLinearLayout = (LinearLayout) v.findViewById(R.id.swMainRowLinearLayout);
		ui.swMainRowEditTitleLinearLayout = (LinearLayout) v.findViewById(R.id.swMainRowEditTitleLinearLayout);
		ui.swTitleTextView = (TextView) v.findViewById(R.id.swTitleTextView);
		//ui.swLapButton = (Button) v.findViewById(R.id.swLapButton);
		ui.swProgressBar = (ProgressBar) v.findViewById(R.id.swProgressBar);
		
		ui.swAddButton.setOnClickListener(this);
		ui.swXButton.setOnClickListener(this);
		ui.swLeftButton.setOnClickListener(this);
		ui.swRightButton.setOnClickListener(this);
		ui.swStartStopResumeButton.setOnClickListener(this);
		ui.swResetButton.setOnClickListener(this);
		ui.swTitleTextView.setOnClickListener(this);
		//ui.swLapButton.setOnClickListener(this);
		
		ui.swDaysTextView.setTypeface(MainActivity.getDigitsTypeface(this));
		ui.swHrsTextView.setTypeface(MainActivity.getDigitsTypeface(this));
		ui.swMinsTextView.setTypeface(MainActivity.getDigitsTypeface(this));
		ui.swSecsTextView.setTypeface(MainActivity.getDigitsTypeface(this));
		ui.swTenthsTextView.setTypeface(MainActivity.getDigitsTypeface(this));
        ((TextView)v.findViewById(R.id.swDaysBGTextView)).setTypeface(MainActivity.getDigitsTypeface(this));
        ((TextView)v.findViewById(R.id.swHrsBGTextView)).setTypeface(MainActivity.getDigitsTypeface(this));
        ((TextView)v.findViewById(R.id.swMinsBGTextView)).setTypeface(MainActivity.getDigitsTypeface(this));
        ((TextView)v.findViewById(R.id.swSecsBGTextView)).setTypeface(MainActivity.getDigitsTypeface(this));
        ((TextView)v.findViewById(R.id.swTenthsBGTextView)).setTypeface(MainActivity.getDigitsTypeface(this));
        ((TextView)v.findViewById(R.id.swDaysBG2TextView)).setTypeface(MainActivity.getDigitsTypeface(this));
        ((TextView)v.findViewById(R.id.swHrsBG2TextView)).setTypeface(MainActivity.getDigitsTypeface(this));
        ((TextView)v.findViewById(R.id.swMinsBG2TextView)).setTypeface(MainActivity.getDigitsTypeface(this));
        ((TextView)v.findViewById(R.id.swSecsBG2TextView)).setTypeface(MainActivity.getDigitsTypeface(this));
        ((TextView)v.findViewById(R.id.swTenthsBG2TextView)).setTypeface(MainActivity.getDigitsTypeface(this));
		
        ui.swTitleEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
		    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
		        if (actionId == EditorInfo.IME_ACTION_DONE) {
		        	saveTitle();
		            return true;
		        }
		        return false;
		    }
		});
        ui.swTitleEditText.setOnKeyListener(new OnKeyListener() {
		    public boolean onKey(View v, int keyCode, KeyEvent event) {
		        if (event.getAction() == KeyEvent.ACTION_DOWN)
		            if (keyCode == KeyEvent.KEYCODE_ENTER) {
		            	saveTitle();
		                return true;
		            }
		        return false;
		    }
		});
		ui.swTitleEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if(!hasFocus)
					saveTitle();
			}
		});
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

	public void startLeftRightScreenHandlers() {
    	if(stopwatch1!=null && stopwatch1.running) {
    		stopwatch1Handler.post(stopwatch1UpdateTimeTask);
    	}
    	if(stopwatch3!=null && stopwatch3.running) {
    		stopwatch3Handler.post(stopwatch3UpdateTimeTask);
    	}
    }

    public void stopLeftRightScreenHandlers() {
    	if(stopwatch1!=null) {
    		stopwatch1Handler.removeCallbacks(stopwatch1UpdateTimeTask);
    	}
    	if(stopwatch3!=null) {
    		stopwatch3Handler.removeCallbacks(stopwatch3UpdateTimeTask);
    	}
    }

	Runnable navigateToPrevViewTask = new Runnable() {
		public void run() {
	    	if(deleteStopwatch==true) {
				int thisStopwatchId = stopwatch2.id;
				String thisStopwatchTitle = stopwatch2.title;
				stopwatch2.reset();
				stopwatch2=fetchPrevStopwatch(stopwatch2.id);
				getContentResolver().delete(URI, Stopwatches.STOPWATCH_ID+"="+thisStopwatchId, null);
				showToast("Stopwatch '"+thisStopwatchTitle+"' deleted.");
				
		    	if(nextStopwatchExists(stopwatch2.id)) {
		    		stopwatch3=fetchNextStopwatch(stopwatch2.id);
		        	setupDisplay(stopwatch3UI, stopwatch3, stopwatch3Handler, null);
		    		realViewSwitcher.allowScrollNextView=true;
		    	}
		    	else {
		    		stopwatch3=null;
		    		stopwatch3Handler.removeCallbacks(stopwatch3UpdateTimeTask);
		    		realViewSwitcher.allowScrollNextView=false;
		    	}
				
	    		deleteStopwatch=false;
	    	}
	    	else {
	    		stopwatch3=stopwatch2;
	        	setupDisplay(stopwatch3UI, stopwatch3, stopwatch3Handler, null);
	    		realViewSwitcher.allowScrollNextView=true;
	    		
	    		stopwatch2=fetchPrevStopwatch(stopwatch2.id);
	    	}

	    	setupDisplay(stopwatch2UI, stopwatch2, stopwatch2Handler, stopwatch2UpdateTimeTask);
			sharedPrefsEditor.putInt("StopwatchLastViewed", stopwatch2.id);
			sharedPrefsEditor.commit();

	    	if(prevStopwatchExists(stopwatch2.id)) {
	    		stopwatch1=fetchPrevStopwatch(stopwatch2.id);
	        	setupDisplay(stopwatch1UI, stopwatch1, stopwatch1Handler, null);
	    		realViewSwitcher.allowScrollPrevView=true;
	    	}
	    	else {
	    		stopwatch1=null;
	    		stopwatch1Handler.removeCallbacks(stopwatch1UpdateTimeTask);
	    		realViewSwitcher.allowScrollPrevView=false;
	    	}

	    	realViewSwitcher.setCurrentScreen(1);
		}
	};

	Runnable navigateToNextViewTask = new Runnable() {
		public void run() {

	    	stopwatch1=stopwatch2;
	    	setupDisplay(stopwatch1UI, stopwatch1, stopwatch1Handler, null);
			realViewSwitcher.allowScrollPrevView=true;

			stopwatch2=fetchNextStopwatch(stopwatch2.id);
	    	setupDisplay(stopwatch2UI, stopwatch2, stopwatch2Handler, stopwatch2UpdateTimeTask);
			sharedPrefsEditor.putInt("StopwatchLastViewed", stopwatch2.id);
			sharedPrefsEditor.commit();
	    	
	    	if(nextStopwatchExists(stopwatch2.id)) {
	    		stopwatch3=fetchNextStopwatch(stopwatch2.id);
	        	setupDisplay(stopwatch3UI, stopwatch3, stopwatch3Handler, null);
	    		realViewSwitcher.allowScrollNextView=true;
	    	}
	    	else {
	    		stopwatch3=null;
	    		stopwatch3Handler.removeCallbacks(stopwatch3UpdateTimeTask);
	    		realViewSwitcher.allowScrollNextView=false;
	    	}
	    	
	    	realViewSwitcher.setCurrentScreen(1);
			
		}
	};

    private void setupRealViewSwitcher() {
    	if(prevStopwatchExists(stopwatch2.id)) {
    		stopwatch1=fetchPrevStopwatch(stopwatch2.id);
        	setupDisplay(stopwatch1UI, stopwatch1, stopwatch1Handler, null);
    		realViewSwitcher.allowScrollPrevView=true;
    	}
    	else {
    		stopwatch1=null;
    		realViewSwitcher.allowScrollPrevView=false;
    	}

    	setupDisplay(stopwatch2UI, stopwatch2, stopwatch2Handler, stopwatch2UpdateTimeTask);
		sharedPrefsEditor.putInt("StopwatchLastViewed", stopwatch2.id);
		sharedPrefsEditor.commit();
    	
    	if(nextStopwatchExists(stopwatch2.id)) {
    		stopwatch3=fetchNextStopwatch(stopwatch2.id);
    		setupDisplay(stopwatch3UI, stopwatch3, stopwatch3Handler, null);
    		realViewSwitcher.allowScrollNextView=true;
    	}
    	else {
    		stopwatch3=null;
    		realViewSwitcher.allowScrollNextView=false;
    	}

   		realViewSwitcher.setCurrentScreen(1);

    	setContentView(realViewSwitcher);
    }
    
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {    
		MainActivity.hapticFeedback(StopwatchActivity.this);
		switch (item.getItemId()) {    
		case R.id.swStopAllMenuItem:       
			Stopwatch.stopAllStopwatches(this,URI);
			if(stopwatch1!=null) {
				stopwatch1=Stopwatch.fetchStopwatchFromDB(this, URI, stopwatch1.id);
				setupDisplay(stopwatch1UI, stopwatch1, stopwatch1Handler, null);
			}

			stopwatch2=Stopwatch.fetchStopwatchFromDB(this, URI, stopwatch2.id);
			setupDisplay(stopwatch2UI, stopwatch2, stopwatch2Handler, stopwatch2UpdateTimeTask);
			
			if(stopwatch3!=null) {
				stopwatch3=Stopwatch.fetchStopwatchFromDB(this, URI, stopwatch3.id);
				setupDisplay(stopwatch3UI, stopwatch3, stopwatch3Handler, null);
			}
			return true;
		case R.id.prefsMenuItem:
			startActivity(new Intent(this, PreferencesActivity.class));
			return true;
		case R.id.whatsNewMenuItem:
			MainActivity.showChangeLog(this, true);
		default:        
			return false;
		}
	}

	public Stopwatch fetchPrevStopwatch(int id) {
    	Stopwatch stopwatch = null;
		cursor = this.getContentResolver().query(
    			URI, 
    			new String[] {Stopwatches.STOPWATCH_ID}, 
    			Stopwatches.STOPWATCH_ID+"<"+id, 
    			null, 
    			Stopwatches.STOPWATCH_ID+" desc");
    	if(cursor.moveToFirst())
    		stopwatch=Stopwatch.fetchStopwatchFromDB(this, URI, cursor.getInt(cursor.getColumnIndex(Stopwatches.STOPWATCH_ID)));
    	else
    		stopwatch=Stopwatch.fetchStopwatchFromDB(this, URI, 1);
    	cursor.close();
    	return(stopwatch);
	}

	public Stopwatch fetchNextStopwatch(int id) {
    	Stopwatch stopwatch = null;
    	cursor = this.getContentResolver().query(
    			URI, 
    			new String[] {Stopwatches.STOPWATCH_ID}, 
    			Stopwatches.STOPWATCH_ID+">"+id, 
    			null, 
    			null);
    	if(cursor.moveToFirst())
    		stopwatch=Stopwatch.fetchStopwatchFromDB(this, URI, cursor.getInt(cursor.getColumnIndex(Stopwatches.STOPWATCH_ID)));
    	else
    		stopwatch=Stopwatch.fetchStopwatchFromDB(this, URI, 1);
    	cursor.close();
    	return(stopwatch);
	}

	
	public boolean prevStopwatchExists(int id) {
    	boolean exists=false;
    	cursor = this.getContentResolver().query(
    			URI, 
    			new String[] {Stopwatches.STOPWATCH_ID}, 
    			Stopwatches.STOPWATCH_ID+"<"+id, 
    			null, 
    			null);
    	if(cursor.moveToFirst()) {
    		exists=true;
    	}
    	cursor.close();
    	return(exists);

	}
	
	public boolean nextStopwatchExists(int id) {
    	boolean exists=false;
    	cursor = this.getContentResolver().query(
    			URI, 
    			new String[] {Stopwatches.STOPWATCH_ID}, 
    			Stopwatches.STOPWATCH_ID+">"+id, 
    			null, 
    			null);
    	if(cursor.moveToFirst()) {
    		exists=true;
    	}
    	cursor.close();
    	return(exists);
	}
    
	public Stopwatch addRecord() {
		Stopwatch stopwatch = new Stopwatch();
		contentValues = new ContentValues();
		contentValues.put(Stopwatch.Stopwatches.TITLE, "");
		contentValues.put(Stopwatch.Stopwatches.START_TIME, 0);
		contentValues.put(Stopwatch.Stopwatches.STOP_TIME, 0);
		contentValues.put(Stopwatch.Stopwatches.RUNNING, false);
		stopwatch.id=Integer.parseInt(this.getContentResolver().insert(URI, contentValues).getPathSegments().get(1));
		if(stopwatch.id==1) 
			stopwatch.title="Stopwatch #1 (tap to edit)";
		else
			stopwatch.title="Stopwatch #"+stopwatch.id;
		Stopwatch.updateStopwatchRecord(stopwatch,StopwatchActivity.this,URI);
		return(stopwatch);
	}
	
	public void setupDisplay(StopwatchUI ui, Stopwatch stopwatch, Handler stopwatchHandler, Runnable stopwatchUpdateTimeTask) {
		ui.swTitleEditText.setText(stopwatch.title);
		ui.swTitleTextView.setText(stopwatch.title);
		ui.swTitleEditText.setSelection(ui.swTitleEditText.getText().length());
		setTime(ui,stopwatch);
		if(stopwatch.running==true) {
			ui.swStartStopResumeButton.setText("Stop");
			ui.swStartStopResumeButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_media_pause,0,0,0);
			stopwatch.resume=true;
			stopwatchHandler.post(stopwatchUpdateTimeTask);
			ui.swProgressBar.setVisibility(View.VISIBLE);
		}
		else {
			if(stopwatch.stopTime==0)
				ui.swStartStopResumeButton.setText("Start");
			else
				ui.swStartStopResumeButton.setText("Resume");
			ui.swStartStopResumeButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_media_play,0,0,0);
			stopwatchHandler.removeCallbacks(stopwatchUpdateTimeTask);
			ui.swProgressBar.setVisibility(View.GONE);
		}
		
		ui.swXButton.setEnabled(false);
		ui.swLeftButton.setEnabled(false);
		ui.swXButton.setVisibility(View.VISIBLE);
		ui.swLeftButton.setVisibility(View.VISIBLE);

		if(!prevStopwatchExists(stopwatch.id)) {
			ui.swXButton.setEnabled(false);
			ui.swLeftButton.setEnabled(false);
			ui.swXButton.setVisibility(View.GONE);
			ui.swLeftButton.setVisibility(View.GONE);

		}
		else {
			ui.swXButton.setEnabled(true);
			ui.swLeftButton.setEnabled(true);
			ui.swXButton.setVisibility(View.VISIBLE);
			ui.swLeftButton.setVisibility(View.VISIBLE);
		}

		if(!nextStopwatchExists(stopwatch.id)) {
			ui.swRightButton.setEnabled(false);
			ui.swRightButton.setVisibility(View.GONE);
		}
		else {
			ui.swRightButton.setEnabled(true);
			ui.swRightButton.setVisibility(View.VISIBLE);
		}

	}
	
	public void setTime(StopwatchUI ui, Stopwatch stopwatch) {
		long milliseconds = stopwatch.getElapsedTime();
		int tenth_of_seconds = (int) (milliseconds % 1000 / 10);
		int seconds = (int) ((milliseconds / 1000) % 60);
		int minutes = (int) (((milliseconds / 1000) / 60) % 60);
		int hours   = (int) (((milliseconds / 1000) / 3600) % 24);
		int days    = (int) ((milliseconds / 1000) / 86400);
		ui.swDaysTextView.setText(""+(days<10?"0"+days:days));
		ui.swHrsTextView.setText(""+(hours<10?"0"+hours:hours));
		ui.swMinsTextView.setText(""+(minutes<10?"0"+minutes:minutes));
		ui.swSecsTextView.setText(""+(seconds<10?"0"+seconds:seconds));
		ui.swTenthsTextView.setText(""+(tenth_of_seconds<10?"0"+tenth_of_seconds:tenth_of_seconds));
	}
	
	TextWatcher titleTextWatcher = new TextWatcher() {
    	public void afterTextChanged(Editable s) {
    		if(!stopwatch2.title.equals(s.toString())) {
    			stopwatch2.title=s.toString();
    			Stopwatch.updateStopwatchRecord(stopwatch2,StopwatchActivity.this,URI);
    		}
    	}                
    	public void beforeTextChanged(CharSequence s, int start, int count, int after) {                
    	}                
    	public void onTextChanged(CharSequence s, int start, int before, int count) {                    
    	}            
	};
	
	Runnable stopwatch1UpdateTimeTask = new Runnable() {
		public void run() {
			setTime(stopwatch1UI,stopwatch1);
			stopwatch1Handler.postDelayed(this, MainActivity.DISPLAY_UPDATE_INTERVAL);
		}
	};

	Runnable stopwatch2UpdateTimeTask = new Runnable() {
		public void run() {
			setTime(stopwatch2UI,stopwatch2);
			stopwatch2Handler.postDelayed(this, MainActivity.DISPLAY_UPDATE_INTERVAL);
		}
	};

	Runnable stopwatch3UpdateTimeTask = new Runnable() {
		public void run() {
			setTime(stopwatch3UI,stopwatch3);
			stopwatch3Handler.postDelayed(this, MainActivity.DISPLAY_UPDATE_INTERVAL);
		}
	};

	private void saveTitle()
	{
		stopwatch2UI.swTitleTextView.setText(stopwatch2UI.swTitleEditText.getText());
		stopwatch2UI.swMainRowEditTitleLinearLayout.setVisibility(View.GONE);
		stopwatch2UI.swMainRowLinearLayout.setVisibility(View.VISIBLE);
	}
	
	public void onClick(View v) {
		MainActivity.hapticFeedback(StopwatchActivity.this);
		switch(v.getId()) {
		case R.id.swAddButton:
			stopwatch3=addRecord();
			setupDisplay(stopwatch3UI, stopwatch3, stopwatch3Handler, null);
			stopwatch2=fetchPrevStopwatch(stopwatch3.id);
			setupDisplay(stopwatch2UI, stopwatch2, stopwatch2Handler, null);
			realViewSwitcher.snapToScreenExternal(2);
			showToast("New Stopwatch added.");
			break;
		case R.id.swXButton:
			alertDialogClickListener = new DialogInterface.OnClickListener() {    
				public void onClick(DialogInterface dialog, int which) {
					MainActivity.hapticFeedback(StopwatchActivity.this);
					switch (which) {        
					case DialogInterface.BUTTON_POSITIVE:            
						deleteStopwatch=true;
						if(stopwatch1!=null && stopwatch1.running)
							stopwatch1Handler.post(stopwatch1UpdateTimeTask);
						realViewSwitcher.snapToScreenExternal(0);
						break;        
					case DialogInterface.BUTTON_NEGATIVE:            
						break;        
					}    
				}};
			alertDialog = new AlertDialog.Builder(StopwatchActivity.this);
			alertDialog.setIcon(android.R.drawable.ic_dialog_alert);
	        alertDialog.setTitle("Confirm Delete Stopwatch");
			alertDialog.setMessage("Are you sure you want to delete this Stopwatch?");
	        alertDialog.setPositiveButton("Yes", alertDialogClickListener);
	        alertDialog.setNegativeButton("No", alertDialogClickListener);
	        alertDialog.setCancelable(true);
	        alertDialog.create().show();
			break;
		case R.id.swLeftImageButton:
			if(stopwatch1!=null) {
				if(stopwatch1.running)
					stopwatch1Handler.post(stopwatch1UpdateTimeTask);
				setTime(stopwatch1UI,stopwatch1);
				realViewSwitcher.snapToScreenExternal(0);
			}
			break;
		case R.id.swRightImageButton:
			if(stopwatch3!=null) {
				if(stopwatch3.running)
					stopwatch3Handler.post(stopwatch3UpdateTimeTask);
				setTime(stopwatch3UI,stopwatch3);
				realViewSwitcher.snapToScreenExternal(2);
			}
			break;
		case R.id.swStartStopResumeButton:
			if(!stopwatch2.resume) {
				stopwatch2UI.swStartStopResumeButton.setText("Stop");
				stopwatch2UI.swStartStopResumeButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_media_pause,0,0,0);
				stopwatch2.resume=true;
				stopwatch2.start();
				Stopwatch.updateStopwatchRecord(stopwatch2,StopwatchActivity.this,URI);
				stopwatch2UI.swProgressBar.setVisibility(View.VISIBLE);
				stopwatch2Handler.post(stopwatch2UpdateTimeTask);
			}
			else {
				stopwatch2UI.swStartStopResumeButton.setText("Resume");
				stopwatch2UI.swStartStopResumeButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_media_play,0,0,0);
				stopwatch2.resume=false;
				stopwatch2.stop();
				Stopwatch.updateStopwatchRecord(stopwatch2,StopwatchActivity.this,URI);
				stopwatch2UI.swProgressBar.setVisibility(View.GONE);
				stopwatch2Handler.removeCallbacks(stopwatch2UpdateTimeTask);
			}
			break;
		case R.id.swResetButton:
			stopwatch2UI.swStartStopResumeButton.setText("Start");
			stopwatch2UI.swStartStopResumeButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_media_play,0,0,0);
			stopwatch2.reset();
			Stopwatch.updateStopwatchRecord(stopwatch2,StopwatchActivity.this,URI);
			stopwatch2Handler.removeCallbacks(stopwatch2UpdateTimeTask);
			stopwatch2UI.swProgressBar.setVisibility(View.GONE);
			setTime(stopwatch2UI,stopwatch2);
			break;
		case R.id.swTitleTextView:
			stopwatch2UI.swTitleEditText.requestFocus();
			stopwatch2UI.swTitleEditText.requestFocusFromTouch();
			stopwatch2UI.swMainRowLinearLayout.setVisibility(View.GONE);
			stopwatch2UI.swMainRowEditTitleLinearLayout.setVisibility(View.VISIBLE);
			break;
		/*case R.id.swLapButton:
			alertDialogClickListener = new DialogInterface.OnClickListener() {    
				public void onClick(DialogInterface dialog, int which) {
					MainActivity.hapticFeedback(StopwatchActivity.this);
				}};
			alertDialog = new AlertDialog.Builder(StopwatchActivity.this);
	        alertDialog.setTitle("StopTimer");
			alertDialog.setMessage("Stopwatch laps are not yet supported. Laps feature will be implemented in a future release. Stay tuned!");
	        alertDialog.setPositiveButton("OK", alertDialogClickListener);
	        alertDialog.setCancelable(true);
	        alertDialog.create().show();
			break;*/
		}
	}

}
