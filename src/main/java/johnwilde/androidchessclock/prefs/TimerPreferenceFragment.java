package johnwilde.androidchessclock.prefs;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.widget.BaseAdapter;

import johnwilde.androidchessclock.R;
import timber.log.Timber;

/**  Activity that inflates the preferences from XML.
 * 
 * It also updates the view with the current preference value
 * and reports back which prefs were changed. 
 */
public class TimerPreferenceFragment extends PreferenceFragment {

    // This enum stores the preference keys
	// that are defined in preferences.xml
	public enum Key{
		MINUTES("initial_minutes_preference"),
		SECONDS("initial_seconds_preference"),
		INCREMENT_SECONDS("increment_preference"),
		DELAY_TYPE("delay_type_preference"),
		NEGATIVE_TIME("allow_negative_time_preference"),
        PLAY_CLICK("audible_notification_preference_click"),
		PLAY_BELL("audible_notification_preference_bell"),
		TIMECONTROL_TYPE("timecontrol_type_preference"),
		FIDE_MOVES_PHASE1("fide_n_moves"),
		FIDE_MIN_PHASE1("fide_minutes1"),
		FIDE_MIN_PHASE2("fide_minutes2"),
		ADV_INCREMENT_SECONDS("advanced_increment_preference"),
		ADV_DELAY_TYPE("advanced_delay_type_preference"),
		ADV_NEGATIVE_TIME("advanced_allow_negative_time_preference"),
		BASIC_SCREEN("basic_time_control_preference_screen"),
        SIMPLE_TIME_CONTROL_CHECKBOX("simple_time_control_checkbox"),
        TOURNAMENT_TIME_CONTROL_CHECKBOX("tournament_time_control_checkbox"),
		ADVANCED_SCREEN("advanced_time_control_preference_screen"),
        TIME_GAP("show_time_gap");
		private String mValue;

		public String toString(){
			return mValue;
		}
		
		Key(String value){
			mValue = value;
		}
		
		public static Key fromString(String s){
		
		    for (Key k : Key.values()){
		        if (k.toString().equalsIgnoreCase(s))
		            return k;
		    }
		        
		    throw new IllegalArgumentException();
		
		}
	}
	
	public enum TimeControl{FIDE, CUSTOM};
	
    @Override
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
        final PreferenceScreen rootPref = (PreferenceScreen) findPreference(Key.ADVANCED_SCREEN.toString());
        findPreference(Key.TIMECONTROL_TYPE.toString())
                .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                preference.setSummary((String) newValue);
                rootPref.setSummary((String) newValue);
                ((BaseAdapter) getPreferenceScreen().getRootAdapter()).notifyDataSetChanged();
                return true;
            }
        });
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

    	boolean v2 = false;
    	switch (getTimeControlType()){
    		case FIDE:
    		    v2 =  setPreferenceEnabledUsingKey(fideKeys, false);
    			break;
    		case CUSTOM:
    			// allow user to set the values  
    		    v2 = setPreferenceEnabledUsingKey(fideKeys, true);
    			break;
    	}
    	
    	setFideMovesPhase1SummaryText();
    	setFirstScreenSummaryText();
    }

    private TimeControl getTimeControlType() {
        TimeControl timeControlType = TimeControl.FIDE;
        String type = PreferenceManager.getDefaultSharedPreferences(getActivity())
                .getString(Key.TIMECONTROL_TYPE.toString(), TimeControl.FIDE.toString());
        try {
            timeControlType = TimeControl.valueOf(type);
        } catch (Exception e) {
            PreferenceManager.getDefaultSharedPreferences(getActivity())
                    .edit()
                    .putString(Key.TIMECONTROL_TYPE.toString(), TimeControl.FIDE.toString())
                    .apply();
        }
        return timeControlType;
    }

    private void setFideMovesPhase1SummaryText() {
    	String keyMin = Key.FIDE_MIN_PHASE1.toString();
    	String keyMoves = Key.FIDE_MOVES_PHASE1.toString();

    	String s1 = getString(R.string.fide_minutes1_title_part1) + " " +
			PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(keyMoves, "N") + " " +
			getString(R.string.fide_minutes1_title_part2);

		EditTextPreference e = (EditTextPreference)findPreference(keyMin);		
		e.setTitle(s1);
	}

    private void setFirstScreenSummaryText(){
	    ListPreference lp = (ListPreference) findPreference(Key.TIMECONTROL_TYPE.toString());
	    Preference p = findPreference(Key.ADVANCED_SCREEN.toString());
    	p.setSummary(lp.getValue());
    }
	
    private void setFidePreferences(){
    	// These are the official FIDE tournament time control values
    	int FIDE_PHASE1_MIN = 90;
        int FIDE_PHASE2_MIN = 30;
        int FIDE_PHASE1_MOVES = 40;
        int FIDE_INCREMENT_SEC = 30;
        String delayType = PreferencesUtil.DelayType.FISCHER.name();
        boolean allowNegativeTime = false;
        
        setEditTextValue(Key.FIDE_MIN_PHASE1, "" + FIDE_PHASE1_MIN);
        setEditTextValue(Key.FIDE_MIN_PHASE2, "" + FIDE_PHASE2_MIN);
        setEditTextValue(Key.FIDE_MOVES_PHASE1, "" + FIDE_PHASE1_MOVES);
        setEditTextValue(Key.ADV_INCREMENT_SECONDS, "" + FIDE_INCREMENT_SEC);
        setListPreferenceValue(Key.ADV_DELAY_TYPE, delayType);
        setCheckBoxValue(Key.ADV_NEGATIVE_TIME, allowNegativeTime);
    }

    // Helper functions that checks value before making a change.
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
    
    private boolean setPreferenceEnabledUsingKey(Key[] keys, boolean value){
    	boolean changedStatusOfAKey = false;
        for (Key key : keys){
    		Preference p = findPreference(key.toString());
    		boolean originalValue = p.isEnabled(); 
    		changedStatusOfAKey = changedStatusOfAKey || (originalValue != value);
    		p.setEnabled(value);
    		
    	}
        return changedStatusOfAKey;
    }
	/*
	 * Make any necessary changes to preference values.
	 * 
	 * Ensure a value was entered for all EditText fields.
	 * Set default FIDE preferences, if needed.
	 */
    public void doValidationAndInitialization(){
        for (Key k : Key.values()){
            Timber.d("Key:", k.toString());
            Preference p = findPreference(k.toString());
            if (p instanceof EditTextPreference){
                EditTextPreference pref = (EditTextPreference)p;
                // handle case where user entered no text
                if (pref.getText().trim().length() == 0){
                    pref.setText("0");
                }
            }
        }

        switch (getTimeControlType()){
            case FIDE:
                setFidePreferences();
        }
    }

    private RadioCheckBoxPreference getCheckbox(Key key) {
        return (RadioCheckBoxPreference) findPreference(key.toString());
    }

//    Preference.OnPreferenceClickListener ignore = new Preference.OnPreferenceClickListener() {
//        @Override
//        public boolean onPreferenceClick(Preference preference) {
//            return true;
//        }
//    };

    public void simpleTimeChecked() {
        RadioCheckBoxPreference sc= getCheckbox(Key.SIMPLE_TIME_CONTROL_CHECKBOX);
        Preference sp = findPreference(Key.BASIC_SCREEN.toString());
        RadioCheckBoxPreference tc = getCheckbox(Key.TOURNAMENT_TIME_CONTROL_CHECKBOX);
        Preference tp = findPreference(Key.ADVANCED_SCREEN.toString());

        if (sc.isChecked()) {
            sc.setEnabled(false);
            tc.setEnabled(true);
            tc.setChecked(false);
            sp.setEnabled(true);
            tp.setEnabled(false);
        }
    }

    public void tournamentTimeChecked() {
        CheckBoxPreference sc= getCheckbox(Key.SIMPLE_TIME_CONTROL_CHECKBOX);
        Preference sp = findPreference(Key.BASIC_SCREEN.toString());
        CheckBoxPreference tc = getCheckbox(Key.TOURNAMENT_TIME_CONTROL_CHECKBOX);
        Preference tp = findPreference(Key.ADVANCED_SCREEN.toString());

        if (tc.isChecked()) {
            tc.setEnabled(false);
            sc.setEnabled(true);
            sc.setChecked(false);
            tp.setEnabled(true);
            sp.setEnabled(false);
        }
    }

    public void updateCheckbox(String key) {
        if (key == null) {
            simpleTimeChecked();
            tournamentTimeChecked();
        } else if (key.equals(Key.SIMPLE_TIME_CONTROL_CHECKBOX.toString())) {
            simpleTimeChecked();
        } else if (key.equals(Key.TOURNAMENT_TIME_CONTROL_CHECKBOX.toString())) {
            tournamentTimeChecked();
        }
    }

    /*
     * Make any necessary changes to summary text
     * and enabled/disabled status of preference fields.
    */
    public void doSummaryTextUpdates(){
        // Populate the preference text descriptions with helpful text
        // that lets user know the current value of the various options.
        setTimeControlSummaryText();
        setAdvancedTimeControlPreferences();
    }

    private void setTimeControlSummaryText() {
    	// Setup the initial values
        for (Key k : Key.values()){
        	Preference p = findPreference(k.toString());
        	if (p instanceof EditTextPreference){
        		EditTextPreference pref = (EditTextPreference)p;
        		pref.setSummary(pref.getText());
        	}
            if (k == Key.DELAY_TYPE || k == Key.ADV_DELAY_TYPE){
            	ListPreference listPref = (ListPreference) p;
            	String s =  getString(R.string.summary_delay_type_preference) + " " +
            	listPref.getValue();
            	listPref.setSummary(s);
            }
            if (k == Key.TIMECONTROL_TYPE){
	        	ListPreference listPref = (ListPreference) p;
	        	listPref.setSummary(listPref.getValue());
            }
        }
	}
}
