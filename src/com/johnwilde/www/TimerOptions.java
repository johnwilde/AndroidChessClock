package com.johnwilde.www;

import android.os.Bundle;
import android.preference.PreferenceActivity;

/**  Activity that inflates the preferences from XML.
 */
public class TimerOptions extends PreferenceActivity {
	public static final String KEY_MINUTES= "initial_minutes_preference";
	public static final String KEY_SECONDS= "initial_seconds_preference";
	public static final String KEY_INCREMENT_SECONDS= "increment_preference";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
    }

}
