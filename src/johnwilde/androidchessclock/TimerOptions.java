package johnwilde.androidchessclock;

import johnwilde.androidchessclock.R;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;

/**  Activity that inflates the preferences from XML.
 */
public class TimerOptions extends PreferenceActivity
  implements OnSharedPreferenceChangeListener {
	
	public enum Key{
		MINUTES("initial_minutes_preference"),
		SECONDS("initial_seconds_preference"),
		INCREMENT_SECONDS("increment_preference");
		private String mValue;

		public String toString(){
			return mValue;
		}
		
		Key(String value){
			mValue = value;
		}
	}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
        
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, 
    		String key) {
        Preference pref = findPreference(key);

        if (pref instanceof EditTextPreference) {
        	EditTextPreference editTextPref = (EditTextPreference) pref;
        	String s = editTextPref.getText();
        	if (s.trim().isEmpty())
        		editTextPref.setText("0");
        	editTextPref.setSummary("Current value is: " + editTextPref.getText());
        }
    }    
    
    @Override
    protected void onResume() {
        super.onResume();
        // Setup the initial values
        for (Key k : Key.values()){
        	EditTextPreference pref = (EditTextPreference)findPreference(k.toString());
        	pref.setSummary("Current value is: " + pref.getText());
        }
        
        // Set up a listener whenever a key changes            
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Unregister the listener whenever a key changes            
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);    
    }
    
}
