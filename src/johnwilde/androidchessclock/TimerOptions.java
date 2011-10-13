package johnwilde.androidchessclock;

import java.util.Arrays;
import java.util.List;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

/**  Activity that inflates the preferences from XML.
 * 
 * It also updates the view with the current preference value
 * and reports back which prefs were changed. 
 */
public class TimerOptions extends PreferenceActivity
  implements OnSharedPreferenceChangeListener {
	
	// This enum stores the preference keys 
	// that are defined in preferences.xml
	public enum Key{
		MINUTES("initial_minutes_preference"),
		SECONDS("initial_seconds_preference"),
		INCREMENT_SECONDS("increment_preference"),
		DELAY_TYPE("delay_type_preference"),
		NEGATIVE_TIME("allow_negative_time_preference"),
		SCREEN_DIM("screen_dim_preference"),
		SWAP_SIDES("white_on_left_preference"),
		PLAY_SOUND("audible_notification_preference"),
		SHOW_MOVE_COUNTER("show_move_count_preference"),
		TIMECONTROL_TYPE("timecontrol_type_preference"),
		FIDE_MOVES_PHASE1("fide_n_moves"),
		FIDE_MIN_PHASE1("fide_minutes1"),
		FIDE_MIN_PHASE2("fide_minutes2"),
		ADV_INCREMENT_SECONDS("advanced_increment_preference"),
		ADV_DELAY_TYPE("advanced_delay_type_preference"),
		ADV_NEGATIVE_TIME("advanced_allow_negative_time_preference"),
		BASIC_SCREEN("basic_time_control_preference_screen"),
		ADVANCED_SCREEN("advanced_time_control_preference_screen");
		private String mValue;

		public String toString(){
			return mValue;
		}
		
		Key(String value){
			mValue = value;
		}
	}
	
	// This enum is used to tag boolean flags in the
	// Intent that is returned by this activity.  The ChessTimer
	// can then choose to reset the clocks only when needed.
	public enum TimerPref{
		LOAD_ALL("johnwilde.androidchessclock.LoadAll"),			
		LOAD_UI("johnwilde.androidchessclock.LoadUi");
		private String mValue;

		public String toString(){
			return mValue;
		}
		TimerPref(String value){
			mValue = value;
		}    
	}

	public enum TimeControl{FIDE, CUSTOM, DISABLED};
	
	SharedPreferences mSharedPreferences;
	
    @Override
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, 
    		String key) {
        
        List<Key> uiKeys = Arrays.asList( new Key[] {
                Key.SHOW_MOVE_COUNTER, 
                Key.SWAP_SIDES, 
                Key.SCREEN_DIM,
                Key.PLAY_SOUND});

        if (uiKeys.contains(Key.valueOf(key))){
            setResult(RESULT_OK, 
                    getIntent().putExtra(TimerPref.LOAD_UI.toString(), true));
        }
        else{
            setResult(RESULT_OK, 
                    getIntent().putExtra(TimerPref.LOAD_ALL.toString(), true));
        }
    	updateAllPreferenceScreens();
    	onContentChanged();
    }    
    
    
	private void setAdvancedTimeControlPreferences() {
		
    	Key[] fideKeys = {
    	        Key.FIDE_MIN_PHASE1, 
    			Key.FIDE_MIN_PHASE2,
    			Key.FIDE_MOVES_PHASE1,
    			Key.ADV_DELAY_TYPE,
    			Key.ADV_INCREMENT_SECONDS,
    			Key.ADV_NEGATIVE_TIME,
    			};
    	
    	TimeControl type = TimeControl.valueOf(mSharedPreferences.getString(
    	        Key.TIMECONTROL_TYPE.toString(), "DISABLED"));
    	switch (type){
    		case FIDE:
        		setFidePreferences();
        		setPreferenceEnabledUsingKey(new Key[]{Key.BASIC_SCREEN}, false);
        		setPreferenceEnabledUsingKey(fideKeys, false);
    			break;
    		case CUSTOM:
    			// allow user to set the values  
    			setPreferenceEnabledUsingKey(new Key[]{Key.BASIC_SCREEN}, false);
    			setPreferenceEnabledUsingKey(fideKeys, true);
    			break;
    		case DISABLED:
    			setPreferenceEnabledUsingKey(new Key[]{Key.BASIC_SCREEN}, true);
    			setPreferenceEnabledUsingKey(fideKeys, false);
    			break;
    	}
    	
    	setFideMovesPhase1SummaryText();
    	setFirstScreenSummaryText();
    	
	}

    private void setFideMovesPhase1SummaryText() {
    	String keyMin = Key.FIDE_MIN_PHASE1.toString();
    	String keyMoves = Key.FIDE_MOVES_PHASE1.toString();

    	String s1 = getString(R.string.fide_minutes1_title_part1) + " " +
			PreferenceManager.getDefaultSharedPreferences(this).getString(keyMoves, "N") + " " +
			getString(R.string.fide_minutes1_title_part2);

		EditTextPreference e = (EditTextPreference)findPreference(keyMin);		
		e.setTitle(s1);
	}

    private void setFirstScreenSummaryText(){
    	Preference p = findPreference(Key.BASIC_SCREEN.toString());
    	if (p.isEnabled())
    		p.setSummary(getString(R.string.basic_screen_enabled_summary));
    	else
			p.setSummary(getString(R.string.basic_screen_disabled_summary));

	    ListPreference lp = (ListPreference) findPreference(Key.TIMECONTROL_TYPE.toString());
	    p = findPreference(Key.ADVANCED_SCREEN.toString());	    	
    	p.setSummary(getString(R.string.advanced_screen_enabled_summary) + 
    					" Currently " +  lp.getValue() + ".");
    }
	
    private void setFidePreferences(){
    	// These are the official FIDE tournament time control values
    	int FIDE_PHASE1_MIN = 90;
        int FIDE_PHASE2_MIN = 30;
        int FIDE_PHASE1_MOVES = 40;
        int FIDE_INCREMENT_SEC = 30;
        String delayType = ChessTimerActivity.DelayType.FISCHER.name();
        boolean allowNegativeTime = false;
        
        setEditTextValue(Key.FIDE_MIN_PHASE1, "" + FIDE_PHASE1_MIN);
        setEditTextValue(Key.FIDE_MIN_PHASE2, "" + FIDE_PHASE2_MIN);
        setEditTextValue(Key.FIDE_MOVES_PHASE1, "" + FIDE_PHASE1_MOVES);
        setEditTextValue(Key.ADV_INCREMENT_SECONDS, "" + FIDE_INCREMENT_SEC);
        setListPreferenceValue(Key.ADV_DELAY_TYPE, delayType);
        setCheckBoxValue(Key.ADV_NEGATIVE_TIME, allowNegativeTime);
    }

    // Helper functions that check value before making a change
    // This prevents stack overflow on API 7 devices
    private void setEditTextValue(Key key, String value){
    	EditTextPreference e = (EditTextPreference)findPreference(key.toString());
    	if ( !e.getText().equals(value) )
    		e.setText(value);
    }

    private void setListPreferenceValue(Key key, String value){
    	ListPreference lp = (ListPreference)findPreference(key.toString());
    	if ( ! lp.getValue().equals(value) )
    		lp.setValue(value);
    }
    private void setCheckBoxValue(Key key, Boolean checked){
    	CheckBoxPreference cb = (CheckBoxPreference)findPreference(key.toString());
    	if ( !( cb.isChecked() == checked) )
    		cb.setChecked(checked);
    }
    
    private void setPreferenceEnabledUsingKey(Key[] keys, boolean value){
    	for (Key key : keys){
    		Preference p = findPreference(key.toString());
    		p.setEnabled(value);
    	}
    }
	
    private void updateAllPreferenceScreens(){
        // Populate the preference text descriptions with helpful text
        // that lets user know the current value of the various options.
        setFirstScreenSummaryText();
        setTimeControlSummaryText();
        setAdvancedTimeControlPreferences();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        updateAllPreferenceScreens();

        // Set up a listener whenever a key changes            
        mSharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    private void setTimeControlSummaryText() {
    	// Setup the initial values
        for (Key k : Key.values()){
        	Preference p = findPreference(k.toString());
        	if (p instanceof EditTextPreference){
        		EditTextPreference pref = (EditTextPreference)p;
        		// handle case where user entered no text
        		if (pref.getText().trim().length() == 0)
        			mSharedPreferences.edit().putString(k.toString(),"0").apply();
        		pref.setSummary("Current value is: " + pref.getText());
        	}
            if (k == Key.DELAY_TYPE || k == Key.ADV_DELAY_TYPE){
            	ListPreference listPref = (ListPreference) p;
            	String s =  getString(R.string.summary_delay_type_preference) +
            	" Currently " + listPref.getValue() + ".";
            	listPref.setSummary(s);
            }
            if (k == Key.TIMECONTROL_TYPE){
	        	ListPreference listPref = (ListPreference) p;
	        	String s =  getString(R.string.summary_advanced_time_preference_description) +
	        	" Currently " + listPref.getValue() + ".";
	        	listPref.setSummary(s);
            }
        }
	}

	@Override
    protected void onPause() {
        super.onPause();

        // Unregister the listener whenever a key changes            
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
    }
	
}
