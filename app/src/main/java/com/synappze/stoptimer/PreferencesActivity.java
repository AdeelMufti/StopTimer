package com.synappze.stoptimer;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.text.InputType;
import android.view.WindowManager;

public class PreferencesActivity extends PreferenceActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getPreferenceManager().setSharedPreferencesName(MainActivity.PREFS_NAME);
        
        addPreferencesFromResource(R.xml.preferences);
        
        setTitle(getString(R.string.app_name)+" v"+getString(R.string.app_version_name)+" Preferences");
        //getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        final ListPreference screenOrientationPref = (ListPreference)findPreference("screenOrientationPref");
        if(screenOrientationPref.getValue().equals("0"))
        	setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        else if(screenOrientationPref.getValue().equals("1"))
        	setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        else if(screenOrientationPref.getValue().equals("2"))
        	setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        screenOrientationPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
			public boolean onPreferenceChange(Preference preference, Object newValue) {
		        if(((String)newValue).equals("0"))
		        	setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
		        else if(((String)newValue).equals("1"))
		        	setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		        else if(((String)newValue).equals("2"))
		        	setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
				return(true);
			}
		});

        
        final EditTextPreference timerDefaultNumBeepsPref = (EditTextPreference)findPreference("timerDefaultNumBeepsPref");
        timerDefaultNumBeepsPref.setTitle(timerDefaultNumBeepsPref.getTitle()+": "+timerDefaultNumBeepsPref.getText()+" beeps");
        timerDefaultNumBeepsPref.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);
        timerDefaultNumBeepsPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				String currentTitle = ((String)timerDefaultNumBeepsPref.getTitle());
				currentTitle = currentTitle.substring(0,currentTitle.indexOf(":"));
				timerDefaultNumBeepsPref.setTitle(currentTitle+": "+(((String)newValue).length()==0?"0":((String)newValue))+" beeps");
				if(((String)newValue).length()==0) {
					timerDefaultNumBeepsPref.setText("0");
					return(false);
				}
				else
					return(true);
			}
		});
        
        final EditTextPreference timerDefaultBeepPref = (EditTextPreference)findPreference("timerDefaultBeepPref");
        timerDefaultBeepPref.setTitle(timerDefaultBeepPref.getTitle()+": "+TimerActivity.convertBeepToFormatedString(timerDefaultBeepPref.getText()));
        timerDefaultBeepPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
		        timerDefaultBeepPref.getDialog().dismiss();
		        TimerActivity.displayBeepPicker(PreferencesActivity.this,false,null,null,(EditTextPreference)preference);
				return(true);
			}

        });

    }


}