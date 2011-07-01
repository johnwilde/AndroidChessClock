package androidchessclock;

import java.text.DecimalFormat;

import com.johnwilde.www.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

/**
 * Activity holding two clocks and two buttons.
 */
public class ChessTimerActivity extends Activity {
	
	/**
	 * The 4 states are:
	 * 
	 * IDLE: 
	 *  	Waiting for a player to make the first move.
	 * 
	 * RUNNING:
	 * 		The timer for one player is running. 
	 * 
	 * PAUSED:
	 * 		Neither timer is running, but mActive stores
	 *      the button of the player whose timer will start
	 *      when play is resumed.
	 *      
	 * DONE:
	 *      Neither timer is running and one timer has reached
	 *      0.0.  mActive stores the player whose timer ran 
	 *      out.
	 *
	 */
	enum GameState {IDLE, RUNNING, PAUSED, DONE;}
	GameState mCurrentState = GameState.IDLE;

	PlayerButton mButton1, mButton2;  	// The two big buttons
	Button mResetButton;				
	ToggleButton mPauseButton;
	
	// Holds a reference to either mButton1 or mButton2.
	//
	// if mCurrentState == IDLE:
	//		it will be null.
	// if mCurrentState == RUNNING:
	//		it will point to the player whose clock is running
	// if mCurrentState == PAUSED:
	//		it will point to the player whose clock was running
	//		when paused
	// if mCurrentState == DONE:
	//		it will point to the player whose clock ran out of fime
	PlayerButton mActive; 
	
	// these values are populated from the user preferences
	int mInitialDurationSeconds = 60; 
	int mIncrementSeconds; 
	
	// Constants 
	private static final String TAG = "ChessTimerActivity";
	private static final int BUTTON_FADED = 25;
	private static final int BUTTON_VISIBLE = 255;
	private static final int REQUEST_CODE_PREFERENCES = 1;
	
	// Create all the objects and enter IDLE state
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		mButton1 = new PlayerButton( new Timer(R.id.clock1), R.id.button1);
		mButton2 = new PlayerButton( new Timer(R.id.clock2), R.id.button2);

		mResetButton = (Button) findViewById(R.id.reset_button);
		mPauseButton = (ToggleButton) findViewById(R.id.pause_button);
		mPauseButton.setOnClickListener(new PauseButtonClickListener());
		
		mButton1.setButtonListener(new PlayerButtonClickListener(mButton1, mButton2));
		mButton2.setButtonListener(new PlayerButtonClickListener(mButton2, mButton1));

		mResetButton.setOnClickListener(new ResetButtonClickListener());

		loadUserPreferences();

		transitionTo(GameState.IDLE);
		Log.d(TAG, "Finished onCreate()");
	}

	// Save data needed to recreate activity.  Enter PAUSED state
	// if we are currently RUNNING.
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		if (mCurrentState == GameState.RUNNING)
			mPauseButton.performClick(); // pause, if not IDLE
		
		outState.putLong("Timer1", mButton1.timer.getMsToGo() );
		outState.putLong("Timer2", mButton2.timer.getMsToGo() );
		outState.putString("State", mCurrentState.toString());
		
		// if IDLE, the current state is NULL
		if (mCurrentState != GameState.IDLE)
			outState.putInt("ActiveButton", mActive.getButtonId()); 
	}
	
	// This is called after onCreate() and restores the activity state
	// using data saved in onSaveInstanceState().  The activity will
	// never be in the RUNNING state after this method.
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		
		GameState stateToRestore = GameState.valueOf(savedInstanceState.getString("State"));
		
		// onCreate() puts us in IDLE and we don't need to do anything else
		if (stateToRestore == GameState.IDLE)
			return;
		
		long activeButtonId = savedInstanceState.getInt("ActiveButton");
		boolean button1Active = (mButton1.getButtonId() == activeButtonId ? true : false);
		boolean button2Active = (mButton2.getButtonId() == activeButtonId ? true : false);
		
		mButton1.setTimeAndState(savedInstanceState.getLong("Timer1"), button1Active);
		mButton2.setTimeAndState(savedInstanceState.getLong("Timer2"), button2Active);

		if (stateToRestore == GameState.DONE){
			transitionTo(GameState.DONE);
			return;
		}
			
		if (stateToRestore == GameState.PAUSED){
			transitionTo(GameState.PAUSED);
			Toast.makeText(this, "Paused. Press to resume.", Toast.LENGTH_SHORT).show();
			return;
		}

	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		switch (itemId) {
		case R.id.optionsmenu_preferences:
			launchPreferencesActivity();
			break;
		case R.id.optionsmenu_about:
			showAboutDialog();
			break;			
		// Generic catch all for all the other menu resources
		default:
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
			loadUserPreferences();
			transitionTo(GameState.IDLE);
		}
	}


	// All state transitions are occur here.  The logic that controls
	// the UI elements is here.
	public void transitionTo(GameState state){
		GameState start = mCurrentState;
		
		switch (state){
		case IDLE:
			mCurrentState = GameState.IDLE;
			mPauseButton.setClickable(false); // disable pause when IDLE
			mButton1.setTransparency(BUTTON_VISIBLE);
			mButton2.setTransparency(BUTTON_VISIBLE);
			mButton1.timer.reset();
			mButton2.timer.reset();
			break;
			
		case RUNNING:
			mCurrentState = GameState.RUNNING;
			mPauseButton.setClickable(true); // enable 'pause'
			mPauseButton.setChecked(false);
			
			// start the clock
			mActive.timer.start();
			break;
			
		case PAUSED:
			mCurrentState = GameState.PAUSED;
			mPauseButton.setChecked(true); // Changes text on Pause button
			mPauseButton.setClickable(true); // enable 'resume'
			// pause the clock
			mActive.timer.pause();
			break;
			
		case DONE:
			if (mActive != null){
				mCurrentState = GameState.DONE;
				mActive.timer.done();
				mPauseButton.setClickable(false); // disable pause when DONE
				break;
			}
			else{
				Log.d(TAG, "Can't tranition to DONE when neither player is active");
				return;
			}
			
		}
		
		Log.d(TAG, "Transition from " + start + " to " + mCurrentState);
			
	}

	public void setActiveButton(PlayerButton button) {
		mActive = button;

		// Give visual indication of which player goes next by fading
		// the button of the player who just moved
		mActive.setTransparency(BUTTON_VISIBLE);
		PlayerButton other = (mButton1 == mActive ? mButton2 : mButton1);
		other.setTransparency(BUTTON_FADED);

		Log.d(TAG, "Setting active button = " + button.getButtonId() );
	}


	public void launchPreferencesActivity() {
		// launch an activity through this intent
		Intent launchPreferencesIntent = new Intent().setClass(this,
				TimerOptions.class);
		// Make it a subactivity so we know when it returns
		startActivityForResult(launchPreferencesIntent,
				REQUEST_CODE_PREFERENCES);
	}

	public void showAboutDialog(){
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		String title = getString(R.string.app_name);
		String msg = getString(R.string.about_dialog);
		builder.setMessage(title + ", version: " + getPackageVersion() + "\n\n" + msg)
		       .setPositiveButton("OK", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		                dialog.cancel();
		           }
		       });
		AlertDialog alert = builder.create();		
		alert.show();
	}
	
	private String getPackageVersion(){

		try {
			PackageInfo manager=getPackageManager().getPackageInfo(getPackageName(), 0);
			return manager.versionName;
		} catch (NameNotFoundException e) {
			return "Unknown";
		}
	}

	private void loadUserPreferences() {
		// Since we're in the same package, we can use this context to get
		// the default shared preferences (?)
		SharedPreferences sharedPref = PreferenceManager
				.getDefaultSharedPreferences(this);
		
		int minutes = Integer.parseInt(sharedPref.getString(
				TimerOptions.Key.MINUTES.toString(), "2"));
		int seconds = Integer.parseInt(sharedPref.getString(
				TimerOptions.Key.SECONDS.toString(), "0"));
		
		setInitialDuration(minutes * 60 + seconds);
	
		seconds = Integer.parseInt(sharedPref.getString(
				TimerOptions.Key.INCREMENT_SECONDS.toString(), "5"));
		setIncrement(seconds);
	}


	private void setInitialDuration(int seconds) {
		mInitialDurationSeconds = seconds;
	}


	private void setIncrement(int seconds) {
		mIncrementSeconds = seconds;
	}


	// Class to aggregate a button and a timer.  It provides
	// a method for setting the time which is used when the 
	// activity must be recreated after it had been started.
	class PlayerButton{
		Timer timer;
		ImageButton button;
		boolean isFaded = false;
		private int mId;
		
		PlayerButton(Timer timer, int buttonId){
			this.timer = timer;
			button = (ImageButton)findViewById(buttonId);
			mId = buttonId;
		}		
		
		public int getButtonId(){
			return mId;
		}
		
		void setButtonListener(PlayerButtonClickListener listener){
			button.setOnClickListener(listener);
		}

		// 0 is fully transparent, 255 is fully opaque
		private void setTransparency(int alpha) {
			if (button == null){
				Log.d(TAG, "Button is NULL");
			}
			
			button.getDrawable().setAlpha(alpha);
			button.invalidateDrawable(button.getDrawable());
		}
		

		public void setTimeAndState(long time, boolean isActive){
			if (isActive)
				setActiveButton(this);
			
			timer.initializeWithValue(time);
		}
	}
	
	/**
	 * Pause one clock and start the other when a button is clicked.
	 */
	final class PlayerButtonClickListener implements OnClickListener {
		PlayerButton mine, other;
		
		public PlayerButtonClickListener(PlayerButton mine, PlayerButton other) {
			this.mine = mine;
			this.other = other;
		}

		@Override
		public void onClick(View v) {

			switch (mCurrentState){

			case PAUSED: 
				// alternate way to un-pause the activity
				// the primary way is to click the "Pause-Reset" toggle button
				mPauseButton.performClick();
				return;

			case DONE: // do nothing
				return;
				
			case RUNNING:
				if (mine.timer.isRunning()) {
					mine.timer.pause();
					mine.timer.increment(mIncrementSeconds);
					other.timer.start();
					setActiveButton(other);
				}
				break;

			case IDLE:
				// the game just started 
				setActiveButton(other);
				transitionTo(GameState.RUNNING);
				break;
			}
		}
	}

	/**
	 * Reset the clocks.
	 */
	final class ResetButtonClickListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			loadUserPreferences();
			transitionTo(GameState.IDLE);
		}
	}

	/**
	 * Pause the clock that is running.
	 */
	final class PauseButtonClickListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			
			if (mCurrentState == GameState.DONE ||
				mCurrentState == GameState.IDLE)
				return;
			
			if ( mCurrentState == GameState.PAUSED ){
				transitionTo(GameState.RUNNING);
			}
			else{
				transitionTo(GameState.PAUSED);	
			}
		}
	}

	// This class updates each player's clock.
	final class Timer implements OnLongClickListener {
		TextView mView;
		long mMillisUntilFinished;
		InnerTimer mCountDownTimer;
		boolean isRunning = false;

		// formatters for displaying text in timer
		DecimalFormat dfSecondsNearEnd = new DecimalFormat("0.0");
		DecimalFormat dfSeconds = new DecimalFormat("00");
		DecimalFormat dfMinAndHours = new DecimalFormat("##");
		private int mNormalTextColor = Color.BLACK;

		Timer(int id) {
			mView = (TextView) findViewById(id);
			initialize();
		}

		public void initializeWithValue(long msToGo) {
			mView.setLongClickable(true);
			mView.setOnLongClickListener(this);
			mMillisUntilFinished = msToGo;
			mCountDownTimer = new InnerTimer();
			isRunning = false;
			mView.setTextColor( mNormalTextColor );
			
			updateTimerText();			
		}

		
		public void initialize() {
			initializeWithValue(mInitialDurationSeconds * 1000);
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

		long getMsToGo(){
			return mMillisUntilFinished;
		}

		public void done() {
			mView.setText("0.0");
			mView.setTextColor(Color.RED);
		}

		class InnerTimer {
			long mInterval = 50; // interval (in ms) at which  text is updated
			CountDownTimer mTimer;

			void go() {
				mTimer = new CountDownTimer(mMillisUntilFinished, mInterval) {

					public void onTick(long millisUntilFinished) {
						mMillisUntilFinished = millisUntilFinished;
						updateTimerText();
					}

					public void onFinish() {
						transitionTo(GameState.DONE);
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
	
}