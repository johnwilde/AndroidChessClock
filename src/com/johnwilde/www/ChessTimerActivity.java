package com.johnwilde.www;

import java.text.DecimalFormat;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

/**
 * Activity holding two clocks and two buttons.
 */
public class ChessTimerActivity extends Activity {
	Timer mTimer1, mTimer2;
	ImageButton mButton1, mButton2;
	Button mResetButton;
	ToggleButton mPauseButton;

	boolean mStarted = false;
	int mInitialDurationSeconds = 60; // retrieve from preferences
	int mIncrementSeconds; // retrieve from preferences
	public Context mContext;
	private static final String TAG = "ChessTimerActivity";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		mContext = this;
	}

	/*
	 * Placed initialization code here instead of in onCreate() because the call
	 * to findViewById() was returning null, because the view had not finished
	 * inflating.
	 * 
	 * @see android.app.Activity#onStart()
	 */
	@Override
	public void onStart() {
		super.onStart();

		mTimer1 = new Timer(R.id.clock1);
		mTimer2 = new Timer(R.id.clock2);

		mButton1 = (ImageButton) findViewById(R.id.button1);
		mButton2 = (ImageButton) findViewById(R.id.button2);

		mResetButton = (Button) findViewById(R.id.reset_button);
		mPauseButton = (ToggleButton) findViewById(R.id.pause_button);
		
		// reset the timers
		setClocksUsingPreferenceSettings();

		mButton1.setOnClickListener(new ButtonClickListener(mTimer1, mTimer2));
		mButton2.setOnClickListener(new ButtonClickListener(mTimer2, mTimer1));
		mResetButton.setOnClickListener(new ResetButtonClickListener());

	}

	public void initializeClocks() {
		if (mTimer1 != null)
			mTimer1.reset();
		if (mTimer2 != null)
			mTimer2.reset();
		
		mStarted = false;

		mPauseButton.setOnClickListener(null);
		
		mButton1.getDrawable().setLevel(5000);
		mButton2.getDrawable().setLevel(5000);
		
//		mButton1.getBackground().setLevel(5000);
//		mButton1.getBackground().invalidateSelf();
//
//		mButton2.getBackground().setLevel(5000);
//		mButton2.getBackground().invalidateSelf();

	}

//	void displayButtonStates() {
//		StringBuilder sb = new StringBuilder();
//
//		String button1Left = Integer.toString(mButton1.getLeft());
//		String button2Left = Integer.toString(mButton2.getLeft());
//
//		sb.append("Button1 left = " + button1Left);
//		sb.append(" Button2 left = " + button2Left);
//
//		if (mButton1.isChecked())
//			sb.append("Button1 ON ");
//		else
//			sb.append("Button1 OFF ");
//
//		if (mButton2.isChecked())
//			sb.append("Button2 ON ");
//		else
//			sb.append("Button2 OFF");
//
//		Log.d(TAG, sb.toString());
//	}

	ImageButton getButton(Timer timer) {
		if (timer == mTimer1)
			return mButton1;
		else
			return mButton2;
	}

	/**
	 * Pauses one clock and starts the other when a button is clicked.
	 */
	final class ButtonClickListener implements OnClickListener {
		Timer myTimer, otherTimer;
		ImageButton myButton, otherButton;

		public ButtonClickListener(Timer mine, Timer other) {
			myTimer = mine;
			otherTimer = other;
			myButton = getButton(mine);
			otherButton = getButton(other);
		}

		@Override
		public void onClick(View v) {

			if (mStarted) {
				if (myTimer.isRunning()) {
					myTimer.pause();
					myTimer.increment(mIncrementSeconds);
					otherTimer.start();
				}
			} else {
				// allow pause button to be used
				mPauseButton.setOnClickListener(new PauseButtonClickListener());
				
				// start the other player's timer
				mStarted = true;
				otherTimer.start();
			}
			int lp = ViewGroup.LayoutParams.FILL_PARENT;
			//int lp = ViewGroup.LayoutParams.WRAP_CONTENT;
			myButton.setLayoutParams(new RadioGroup.LayoutParams(lp,lp, 5));
			otherButton.setLayoutParams(new RadioGroup.LayoutParams(lp,lp, 1));
			
			myButton.getDrawable().setLevel(1);
			otherButton.getDrawable().setLevel(10000);
			
			myButton.getDrawable().invalidateSelf();
			otherButton.getDrawable().invalidateSelf();
			// a lower layout weight will make the image bigger
			// a higher level makes the image bigger
			
		}

	}

	/**
	 * Reset the clocks.
	 */
	final class ResetButtonClickListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			setClocksUsingPreferenceSettings();
		}

	}

	/**
	 * Pause the clock that is running.
	 */
	final class PauseButtonClickListener implements OnClickListener {
		Timer clockRunningWhenPaused;
		@Override
		public void onClick(View v) {
			ToggleButton tb = (ToggleButton) v;
			
			if ( tb.isChecked() == false ){
				// We just became un-paused, start clock again
				clockRunningWhenPaused.start();
			}
			else{
				if (mTimer1.isRunning)
					clockRunningWhenPaused = mTimer1;
				else
					clockRunningWhenPaused = mTimer2;
				
				clockRunningWhenPaused.pause();
			}
		}

	}

	final class Timer implements OnLongClickListener {
		TextView mView;
		long mMillisUntilFinished;
		InnerTimer mCountDownTimer;
		boolean isRunning = false;

		// formatters for displaying text in timer
		DecimalFormat dfSecondsNearEnd = new DecimalFormat("0.0");
		DecimalFormat dfSeconds = new DecimalFormat("00");
		DecimalFormat dfMinAndHours = new DecimalFormat("##");
		private int mNormalTextColor;

		Timer(int id) {
			mView = (TextView) findViewById(id);
			mNormalTextColor = mView.getCurrentTextColor();
			initialize();
		}

		public void initialize() {
			mView.setLongClickable(true);
			mView.setOnLongClickListener(this);
			mMillisUntilFinished = mInitialDurationSeconds * 1000;
			mCountDownTimer = new InnerTimer();
			isRunning = false;
			mView.setTextColor( mNormalTextColor );
			
			updateTimerText();
		}

		public void increment(int mIncrementSeconds) {
			mMillisUntilFinished += mIncrementSeconds * 1000;
			updateTimerText();
		}

		public boolean isRunning() {
			return isRunning;
		}

		public void start() {
			mCountDownTimer.go();
			isRunning = true;
		}

		public void pause() {
			if (mCountDownTimer != null)
				mCountDownTimer.cancel();
			isRunning = false;
		}

		public void reset() {
			mCountDownTimer.cancel();
			initialize();
		}

		public void updateTimerText() {
			mView.setText(formatTime(mMillisUntilFinished));
		}

		private String formatTime(long millis) {
			// 1000 ms in 1 second
			// 60*1000 ms in 1 minute
			// 60*60*1000 ms in 1 hour

			String stringSec, stringMin, stringHr;

			// Parse the input (in ms) into integer hour, minute, and second
			// values
			long hours = millis / (1000 * 60 * 60);
			millis -= hours * (1000 * 60 * 60);

			long min = millis / (1000 * 60);
			millis -= min * (1000 * 60);

			long sec = millis / 1000;
			millis -= sec * 1000;

			// Construct the output string
			if (hours > 0)
				stringHr = dfMinAndHours.format(hours) + ":";
			else
				stringHr = "";

			if (hours > 0 || min > 0)
				stringMin = dfMinAndHours.format(min) + ":";
			else
				stringMin = "";

			if (min > 0)
				stringSec = dfSeconds.format(sec);
			else if (sec < 10)
				stringSec = dfSecondsNearEnd.format((double) sec
						+ (double) millis / 1000.0);
			else
				stringSec = dfSeconds.format(sec);

			return stringHr + stringMin + stringSec;

		}

		public void done() {
			mView.setText("0.0");
			mNormalTextColor  = mView.getCurrentTextColor();
			mView.setTextColor(Color.RED);
		}

		class InnerTimer {
			long mInterval = 50; // rate (ms) at which text is updated
			CountDownTimer mTimer;

			void go() {
				mTimer = new CountDownTimer(mMillisUntilFinished, mInterval) {

					public void onTick(long millisUntilFinished) {
						mMillisUntilFinished = millisUntilFinished;
						updateTimerText();
					}

					public void onFinish() {
						done();
					}
				}.start();
			}

			void cancel() {
				if (mTimer != null)
					mTimer.cancel();
			}

		}

		@Override
		public boolean onLongClick(View v) {
			// launch activity that allows user to set time and increment values
			launchPreferencesActivity();
			return true;
		}

		public View getView() {
			return mView;
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}

	private static final int REQUEST_CODE_PREFERENCES = 1;

	public void launchPreferencesActivity() {
		// launch an activity through this intent
		Intent launchPreferencesIntent = new Intent().setClass(this,
				TimerOptions.class);
		// Make it a subactivity so we know when it returns
		startActivityForResult(launchPreferencesIntent,
				REQUEST_CODE_PREFERENCES);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		switch (itemId) {
		case R.id.preferences:
			launchPreferencesActivity();
			break;

		// Generic catch all for all the other menu resources
		default:
			// Don't toast text when a submenu is clicked
			if (!item.hasSubMenu()) {
				Toast.makeText(this, item.getTitle(), Toast.LENGTH_SHORT)
						.show();
				return true;
			}
			break;
		}

		return false;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		// check which activity has returned (for now we only have one, so
		// it isn't really necessary).
		if (requestCode == REQUEST_CODE_PREFERENCES) {
			// reset clocks using (possibly new) settings
			setClocksUsingPreferenceSettings();
		}
	}

	private void setClocksUsingPreferenceSettings() {
		// Since we're in the same package, we can use this context to get
		// the default shared preferences (?)
		SharedPreferences sharedPref = PreferenceManager
				.getDefaultSharedPreferences(this);
		int minutes = Integer.parseInt(sharedPref.getString(
				TimerOptions.KEY_MINUTES, "2"));
		int seconds = Integer.parseInt(sharedPref.getString(
				TimerOptions.KEY_SECONDS, "0"));
		
		setInitialDuration(minutes * 60 + seconds);

		seconds = Integer.parseInt(sharedPref.getString(
				TimerOptions.KEY_INCREMENT_SECONDS, "0"));
		setIncrement(seconds);

		initializeClocks();
	}

	private void setInitialDuration(int seconds) {
		mInitialDurationSeconds = seconds;
	}

	private void setIncrement(int seconds) {
		mIncrementSeconds = seconds;
	}
}