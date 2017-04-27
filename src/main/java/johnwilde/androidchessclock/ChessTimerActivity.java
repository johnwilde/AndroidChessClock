package johnwilde.androidchessclock;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.IOException;

import johnwilde.androidchessclock.TimerOptions.TimeControl;

/**
 * Activity holding two clocks and two buttons.
 */
public class ChessTimerActivity extends Activity {

    /**
     * The 4 states are:
     * 
     * IDLE: Waiting for a player to make the first move.
     * 
     * RUNNING: The timer for one player is running.
     * 
     * PAUSED: Neither timer is running, but mActive stores the button of the
     * player whose timer will start when play is resumed.
     * 
     * DONE: Neither timer is running and one timer has reached 0.0. mActive
     * stores the player whose timer ran out.
     * 
     */
    enum GameState {
        IDLE, RUNNING, PAUSED, DONE
    };

    GameState mCurrentState = GameState.IDLE;

    enum TimeControlType {
        BASIC, TOURNAMENT
    };

    TimeControlType mTimeControlType = TimeControlType.BASIC;

    enum DelayType {
        FISCHER, BRONSTEIN;
    }

    DelayType mDelayType;

    PlayerButton mButton1, mButton2; // The two big buttons
    Button mResetButton;
    Button mStartButton;
    ImageButton mOverflowButton;
    ToggleButton mPauseButton;
    AlertDialog mPauseDialog;

    // This field holds a reference to either mButton1 or mButton2.
    //
    // if mCurrentState == IDLE:
    // it will be null.
    // if mCurrentState == RUNNING:
    // it will point to the player whose clock is running
    // if mCurrentState == PAUSED:
    // it will point to the player whose clock was running
    // when paused
    // if mCurrentState == DONE:
    // it will point to the player whose clock ran out of fime
    PlayerButton mActive = null;

    private SharedPreferences mSharedPref;

    // The values below are populated from the user preferences
    int mInitialDurationSeconds = 60;
    int mIncrementSeconds;
    boolean mAllowNegativeTime = false;
    boolean mShowMoveCounter = false;
    private boolean mWhiteOnLeft = false;
    private int mWakeLockType;
    // set when using TOURNAMENT time control
    private int mPhase1NumberMoves;
    private int mPhase2Minutes;

    // used to keep the screen bright during play
    private WakeLock mWakeLock;
    // for sounding buzzer
    int mBellId;
    int mClickId;
    SoundPool mSoundPool;

    public boolean shouldPlaySoundAtEnd() {
        return mPlaySoundAtEnd;
    }

    private boolean mPlaySoundAtEnd;


    private boolean mPlaySoundOnClick;

    // Constants
    public final static String EXTRA_COLOR = "johnwilde.androidchessclock.COLOR";
    public final static String EXTRA_TIME = "johnwilde.androidchessclock.TIME";

    private static final String TAG = "ChessTimerActivity";
    private static final int BUTTON_FADED = 25;
    private static final int BUTTON_VISIBLE = 255;
    private static final int REQUEST_CODE_PREFERENCES = 1;
    private static final int REQUEST_CODE_ADJUST_TIME = 2;

    // Create all the objects and enter IDLE state
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // the layout looks best in landscape orientation
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        // set default values (for first run)
        mSharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        loadAllUserPreferences();

        Timer whiteTimer = new Timer(this, R.id.whiteClock,
                R.id.whiteSpinnerContainer, "white");
        mButton1 = new PlayerButton(whiteTimer, R.id.whiteButton,
                R.id.whiteMoveCounter);
        Timer blackTimer = new Timer(this, R.id.blackClock,
                R.id.blackSpinnerContainer, "black");
        mButton2 = new PlayerButton(blackTimer, R.id.blackButton,
                R.id.blackMoveCounter);

        mResetButton = (Button) findViewById(R.id.reset_button);
        mStartButton = (Button) findViewById(R.id.start_button);
        mStartButton.setOnClickListener(new StartButtonClickListener());

        mPauseButton = (ToggleButton) findViewById(R.id.pause_button);
        mPauseButton.setOnClickListener(new PauseButtonClickListener());
        mPauseButton.setVisibility(View.GONE);

        mButton1.setButtonListener(new PlayerButtonClickListener(mButton1,
                mButton2));
        mButton2.setButtonListener(new PlayerButtonClickListener(mButton2,
                mButton1));

        mResetButton.setOnClickListener(new ResetButtonClickListener());

        mOverflowButton = (ImageButton) findViewById(R.id.overflowButton);
        mOverflowButton.setOnClickListener(new OverflowButtonClickListener());

        // enable following line to clear settings if they are in a bad state
        // mSharedPref.edit().clear().apply();

        transitionTo(GameState.IDLE);

        acquireWakeLock();
        acquireMediaPlayer();

        Log.d(TAG, "Finished onCreate()");
    }

    @Override
    public void onPause() {
        releaseWakeLock();
        releaseMediaPlayer();
        super.onPause();
    }

    @Override
    public void onResume() {
        acquireWakeLock();
        acquireMediaPlayer();
        super.onResume();
    }

    @Override
    public void onDestroy() {
        releaseWakeLock();
        releaseMediaPlayer();
        super.onDestroy();
    }

    // Save data needed to recreate activity. Enter PAUSED state
    // if we are currently RUNNING.
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mCurrentState == GameState.RUNNING)
            mPauseButton.performClick(); // pause, if not IDLE

        outState.putLong("Timer1", mButton1.timer.getMsToGo());
        outState.putLong("Timer1Delay", mButton1.timer.getMsDelayToGo());
        outState.putLong("Timer2", mButton2.timer.getMsToGo());
        outState.putLong("Timer2Delay", mButton2.timer.getMsDelayToGo());
        outState.putInt("MoveCounter1", mButton1.mMoveNumber);
        outState.putInt("MoveCounter2", mButton2.mMoveNumber);
        outState.putString("State", mCurrentState.toString());

        // if IDLE, the current state is NULL
        if (mCurrentState != GameState.IDLE)
            outState.putInt("ActiveButton", mActive.getButtonId());
    }

    // This is called after onCreate() and restores the activity state
    // using data saved in onSaveInstanceState(). The activity will
    // never be in the RUNNING state after this method.
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        GameState stateToRestore = GameState.valueOf(savedInstanceState
                .getString("State"));

        // onCreate() puts us in IDLE and we don't need to do anything else
        if (stateToRestore == GameState.IDLE)
            return;

        long activeButtonId = savedInstanceState.getInt("ActiveButton");
        boolean button1Active = mButton1.getButtonId() == activeButtonId;
        boolean button2Active = mButton2.getButtonId() == activeButtonId;

        mButton1.setTimeAndState(savedInstanceState.getLong("Timer1"),
                savedInstanceState.getLong("Timer1Delay"),
                savedInstanceState.getInt("MoveCounter1"), button1Active);
        mButton2.setTimeAndState(savedInstanceState.getLong("Timer2"),
                savedInstanceState.getLong("Timer2Delay"),
                savedInstanceState.getInt("MoveCounter2"), button2Active);

        if (stateToRestore == GameState.DONE) {
            transitionTo(GameState.DONE);
            return;
        }

        if (stateToRestore == GameState.PAUSED) {
            transitionToPauseAndToast();
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
    public boolean onPrepareOptionsMenu(Menu menu) {
        transitionToPauseAndToast();
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

    // This method is called when the user preferences activity returns. That
    // activity set fields in the Intent data to indicate what preferences
    // have been changed. The method takes the action appropriate for what
    // has changed. In some cases the clocks are reset.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // check which activity has returned (for now we only have one, so
        // it isn't really necessary).
        if (requestCode == REQUEST_CODE_PREFERENCES) {

            if (data == null)
                return; // no preferences were changed

            // reset clocks using new settings
            if (data.getBooleanExtra(
                    TimerOptions.TimerPref.LOAD_ALL.toString(), false)) {
                loadAllUserPreferences();
                transitionTo(GameState.IDLE);
                return; // exit early
            } else {
                loadUiPreferences();
            }
        } else if (requestCode == REQUEST_CODE_ADJUST_TIME) {
            if (data == null)
                return; // no change
            long newTime = data.getLongExtra(AdjustClock.NEW_TIME, 0);
            String playerColor = data.getStringExtra(AdjustClock.COLOR);
            PlayerButton button;
            if (playerColor.equalsIgnoreCase("white"))
                button = mButton1;
            else
                button = mButton2;

            button.adjustTime(newTime);
            if (mActive != null && mActive != button) {
                button.setTransparency(BUTTON_FADED);
            }

        }
    }

    private void acquireMediaPlayer() {
        mSoundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 100);
        if (mPlaySoundAtEnd) {
            mBellId = getMediaPlayer(R.raw.bell);
        }
        if (mPlaySoundOnClick) {
            mClickId = getMediaPlayer(R.raw.click);
        }

    }

    private int getMediaPlayer(int media) {
        int id = -1;
        try {
            AssetFileDescriptor afd = getResources().openRawResourceFd(media);
            if (afd == null)
                return id;
            id = mSoundPool.load(afd, 1);
            afd.close();
        } catch (IOException ex) {
            Log.d(TAG, "create failed:", ex);
            // fall through
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "create failed:", ex);
            // fall through
        } catch (SecurityException ex) {
            Log.d(TAG, "create failed:", ex);
            // fall through
        }
        return id;
    }

    private void releaseMediaPlayer() {
        if (mSoundPool != null) {
            mSoundPool.release();
            mSoundPool = null;
            mClickId = -1;
            mBellId = -1;
        }
    }

    void playBell() {
        playSound(mBellId);
    }

    void playClick() {
        playSound(mClickId);
    }

    private void playSound(int soundID) {
        if (mSoundPool == null) {
            return;
        }
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        float curVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        float maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        float leftVolume = curVolume / maxVolume;
        float rightVolume = curVolume / maxVolume;
        int priority = 1;
        int no_loop = 0;
        float normal_playback_rate = 1f;
        mSoundPool.play(soundID, leftVolume, rightVolume, priority, no_loop, normal_playback_rate);
    }
    private void releaseWakeLock() {
        if (mWakeLock != null) {
            if (mWakeLock.isHeld()) {
                mWakeLock.release();
                Log.d(TAG, "released wake lock " + mWakeLock);
            }
        }
    }

    private void acquireWakeLock() {
        releaseWakeLock();
        PowerManager pm = (PowerManager) getSystemService(ChessTimerActivity.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(mWakeLockType, TAG);
        mWakeLock.acquire();
        Log.d(TAG, "acquired wake lock " + mWakeLock);
    }

    // All state transitions occur here. The logic that controls
    // the UI elements is here.
    public void transitionTo(GameState state) {
        GameState start = mCurrentState;

        switch (state) {
        case IDLE:
            mCurrentState = GameState.IDLE;
            mStartButton.setEnabled(true);
            mStartButton.setVisibility(View.VISIBLE);
            mResetButton.setEnabled(false);
            mPauseButton.setVisibility(View.GONE);
            mButton1.reset();
            mButton2.reset();
            break;

        case RUNNING:
            mCurrentState = GameState.RUNNING;
            mResetButton.setEnabled(true);
            mStartButton.setEnabled(false);
            mStartButton.setVisibility(View.GONE);
            mPauseButton.setVisibility(View.VISIBLE);
            mPauseButton.setClickable(true); // enable 'pause'
            mPauseButton.setChecked(false); // set toggle to show "pause" text

            // start the clock
            mActive.moveStarted();
            break;

        case PAUSED:
            mCurrentState = GameState.PAUSED;
            mStartButton.setVisibility(View.GONE);
            mPauseButton.setVisibility(View.VISIBLE);
            mPauseButton.setChecked(true); // Changes text on Pause button
            mPauseButton.setClickable(true); // enable 'resume'
            // pause the clock
            mActive.timer.pause();
            break;

        case DONE:
            if (mActive != null) {
                mCurrentState = GameState.DONE;
                mStartButton.setEnabled(true);
                mStartButton.setVisibility(View.VISIBLE);
                mPauseButton.setVisibility(View.GONE);
                mResetButton.setEnabled(true);
                mPauseButton.setClickable(false); // disable pause when DONE
                break;
            } else {
                Log.d(TAG,
                        "Can't tranition to DONE when neither player is active");
                return;
            }

        }

        Log.d(TAG, "Transition from " + start + " to " + mCurrentState);

    }

    public void setActiveButtonAndMoveCount(PlayerButton button) {
        mActive = button;

        // Give visual indication of which player goes next by fading
        // the button of the player who just moved
        mActive.setTransparency(BUTTON_VISIBLE);
        PlayerButton other = (mButton1 == mActive ? mButton2 : mButton1);
        other.setTransparency(BUTTON_FADED);

        if (mShowMoveCounter) {
            mActive.mMoveCounter.setVisibility(View.VISIBLE);
            String s = getString(R.string.move_counter_text) + " "
                    + mActive.mMoveNumber;
            mActive.mMoveCounter.setText(s);
            other.mMoveCounter.setVisibility(View.GONE);
        } else {
            mActive.mMoveCounter.setVisibility(View.GONE);
            other.mMoveCounter.setVisibility(View.GONE);
        }

        Log.d(TAG, "Setting active button = " + button.getButtonId());
    }

    public void launchPreferencesActivity() {
        // launch an activity through this intent
        Intent launchPreferencesIntent = new Intent().setClass(this,
                TimerOptions.class);
        // Make it a subactivity so we know when it returns
        startActivityForResult(launchPreferencesIntent,
                REQUEST_CODE_PREFERENCES);
    }

    public void launchAdjustPlayerClockActivity(String color, long time) {
        // launch an activity through this intent
        Intent launchAdjustPlayerClockIntent = new Intent().setClass(this,
                AdjustClock.class);
        launchAdjustPlayerClockIntent.putExtra(EXTRA_COLOR, color);
        launchAdjustPlayerClockIntent.putExtra(EXTRA_TIME, time);
        // Make it a subactivity so we know when it returns
        startActivityForResult(launchAdjustPlayerClockIntent,
                REQUEST_CODE_ADJUST_TIME);
    }

    public void showAboutDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String title = getString(R.string.app_name);
        String about = getString(R.string.about_dialog);
        String message = title + ", " + getString(R.string.version) + ": "
                + getPackageVersion() + "\n\n" + about;
        builder.setMessage(message).setPositiveButton(getString(R.string.OK),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private String getPackageVersion() {

        try {
            PackageInfo manager = getPackageManager().getPackageInfo(
                    getPackageName(), 0);
            return manager.versionName;
        } catch (NameNotFoundException e) {
            return getString(R.string.unknown);
        }
    }

    // Methods for loading USER PREFERENCES
    //
    // These methods are run after the user has changed
    // a preference and during onCreate(). The onActivityResult()
    // method is responsible for calling the method that matches
    // the preference that was changed.
    //
    // Note: the default values required by the SharedPreferences getXX
    // methods are not used. The SharedPreferences will have their default
    // values set (in onCreate() ) and those defaults are saved in
    // preferences.xml

    private void loadAllUserPreferences() {
        loadTimeControlPreferences();
        loadUiPreferences();
    }

    private void loadUiPreferences() {
        loadMoveCounterUserPreference();
        loadSwapSidesUserPreference();
        loadAudibleNotificationUserPreference();
        loadScreenDimUserPreference();
    }

    // determine whether we're using BASIC or TOURNAMENT time control
    private void loadTimeControlPreferences() {
        TimerOptions.TimeControl timeControl = TimerOptions.TimeControl
                .valueOf(mSharedPref.getString(
                        TimerOptions.Key.TIMECONTROL_TYPE.toString(),
                        "DISABLED"));

        if (timeControl == TimeControl.DISABLED) {
            mTimeControlType = TimeControlType.BASIC;
            loadBasicTimeControlUserPreference();
        } else {
            mTimeControlType = TimeControlType.TOURNAMENT;
            loadAdvancedTimeControlUserPreference();
        }
    }

    private void loadBasicTimeControlUserPreference() {
        loadInitialTimeUserPreferences();
        loadIncrementUserPreference(TimerOptions.Key.INCREMENT_SECONDS);
        loadDelayTypeUserPreference(TimerOptions.Key.DELAY_TYPE);
        loadNegativeTimeUserPreference(TimerOptions.Key.NEGATIVE_TIME);
    }

    private void loadAdvancedTimeControlUserPreference() {

        int minutes1 = getTimerOptionsValue(TimerOptions.Key.FIDE_MIN_PHASE1);

        setInitialDuration(minutes1 * 60);

        mPhase1NumberMoves = getTimerOptionsValue(TimerOptions.Key.FIDE_MOVES_PHASE1);
        mPhase2Minutes = getTimerOptionsValue(TimerOptions.Key.FIDE_MIN_PHASE2);

        loadDelayTypeUserPreference(TimerOptions.Key.ADV_DELAY_TYPE);
        loadIncrementUserPreference(TimerOptions.Key.ADV_INCREMENT_SECONDS);
        loadNegativeTimeUserPreference(TimerOptions.Key.ADV_NEGATIVE_TIME);
    }

    private void loadMoveCounterUserPreference() {
        mShowMoveCounter = mSharedPref.getBoolean(
                TimerOptions.Key.SHOW_MOVE_COUNTER.toString(), false);
        if (mCurrentState == GameState.PAUSED)
            setActiveButtonAndMoveCount(mActive);
    }

    private void loadSwapSidesUserPreference() {
        mWhiteOnLeft = mSharedPref.getBoolean(
                TimerOptions.Key.SWAP_SIDES.toString(), false);
        configureSides();
    }

    private void loadAudibleNotificationUserPreference() {
        mPlaySoundAtEnd = mSharedPref.getBoolean(
                TimerOptions.Key.PLAY_BELL.toString(), false);
        mPlaySoundOnClick = mSharedPref.getBoolean(
                TimerOptions.Key.PLAY_CLICK.toString(), false);
        acquireMediaPlayer();
    }

    private void configureSides() {
        View whiteClock = findViewById(R.id.whiteClock);
        View blackClock = findViewById(R.id.blackClock);

        View whiteButton = findViewById(R.id.whiteButton);
        View blackButton = findViewById(R.id.blackButton);

        View whiteMoveCounter = findViewById(R.id.whiteMoveCounter);
        View blackMoveCounter = findViewById(R.id.blackMoveCounter);

        FrameLayout whiteSpinnerContainer = (FrameLayout) findViewById(R.id.whiteSpinnerContainer);
        FrameLayout blackSpinnerContainer = (FrameLayout) findViewById(R.id.blackSpinnerContainer);

        FrameLayout leftClockContainer = (FrameLayout) findViewById(R.id.leftClockContainer);
        FrameLayout rightClockContainer = (FrameLayout) findViewById(R.id.rightClockContainer);
        leftClockContainer.removeAllViewsInLayout();
        rightClockContainer.removeAllViewsInLayout();

        FrameLayout leftButtonContainer = (FrameLayout) findViewById(R.id.frameLayoutLeft);
        FrameLayout rightButtonContainer = (FrameLayout) findViewById(R.id.frameLayoutRight);
        leftButtonContainer.removeAllViewsInLayout();
        rightButtonContainer.removeAllViewsInLayout();

        LinearLayout leftSpinnerContainer = (LinearLayout) findViewById(R.id.leftSpinnerContainer);
        LinearLayout rightSpinnerContainer = (LinearLayout) findViewById(R.id.rightSpinnerContainer);
        leftSpinnerContainer.removeAllViewsInLayout();
        rightSpinnerContainer.removeAllViewsInLayout();

        if (mWhiteOnLeft) {
            leftSpinnerContainer.addView(whiteSpinnerContainer);
            rightSpinnerContainer.addView(blackSpinnerContainer);
            leftClockContainer.addView(whiteClock);
            rightClockContainer.addView(blackClock);
            leftButtonContainer.addView(whiteButton);
            leftButtonContainer.addView(whiteMoveCounter);
            rightButtonContainer.addView(blackButton);
            rightButtonContainer.addView(blackMoveCounter);
        } else {
            leftSpinnerContainer.addView(blackSpinnerContainer);
            rightSpinnerContainer.addView(whiteSpinnerContainer);
            leftClockContainer.addView(blackClock);
            rightClockContainer.addView(whiteClock);
            leftButtonContainer.addView(blackButton);
            leftButtonContainer.addView(blackMoveCounter);
            rightButtonContainer.addView(whiteButton);
            rightButtonContainer.addView(whiteMoveCounter);
        }

    }

    private void loadNegativeTimeUserPreference(TimerOptions.Key key) {
        mAllowNegativeTime = mSharedPref.getBoolean(key.toString(), false);
    }

    private void loadDelayTypeUserPreference(TimerOptions.Key key) {
        String delayTypeString = mSharedPref.getString(key.toString(),
                "FISCHER");
        mDelayType = DelayType.valueOf(delayTypeString.toUpperCase());
    }

    private void loadScreenDimUserPreference() {
        boolean allowScreenToDim = mSharedPref.getBoolean(
                TimerOptions.Key.SCREEN_DIM.toString(), true);
        /** Create a PowerManager object so we can get the wakelock */
        mWakeLockType = allowScreenToDim ? PowerManager.SCREEN_DIM_WAKE_LOCK
                : PowerManager.SCREEN_BRIGHT_WAKE_LOCK;
    }

    private void loadIncrementUserPreference(TimerOptions.Key key) {
        int seconds = getTimerOptionsValue(key);
        setIncrement(seconds);
    }

    private void loadInitialTimeUserPreferences() {
        int minutes = getTimerOptionsValue(TimerOptions.Key.MINUTES);
        int seconds = getTimerOptionsValue(TimerOptions.Key.SECONDS);
        setInitialDuration(minutes * 60 + seconds);
    }

    private int getTimerOptionsValue(TimerOptions.Key key) {
        try {
            String s = mSharedPref.getString(key.toString(), "0");

            if (s.length() == 0) {
                s = "0";
            }

            return (Integer.parseInt(s));

        } catch (NumberFormatException ex) {
            Log.d(TAG, ex.getMessage());
            return 0;
        }
    }

    private void setInitialDuration(int seconds) {
        mInitialDurationSeconds = seconds;
    }

    private void setIncrement(int seconds) {
        mIncrementSeconds = seconds;
    }

    // Class to aggregate a button, a timer and a move counter.
    // It provides a method for setting the time which is used when the
    // activity must be recreated after it had been started.
    // The methods of this class implement the logic of when time
    // should be added to each clock according to the style of time
    // control that the user configured.
    class PlayerButton {
        Timer timer;
        ImageButton button;
        TextView mMoveCounter;
        private int mId;
        private int mMoveNumber;

        PlayerButton(Timer timer, int buttonId, int moveCounterId) {
            this.timer = timer;
            button = (ImageButton) findViewById(buttonId);
            mMoveCounter = (TextView) findViewById(moveCounterId);
            mId = buttonId;
            mMoveNumber = 1;
        }

        public int getButtonId() {
            return mId;
        }

        void setButtonListener(PlayerButtonClickListener listener) {
            button.setOnClickListener(listener);
        }

        // 0 is fully transparent, 255 is fully opaque
        private void setTransparency(int alpha) {
            if (button == null) {
                Log.e(TAG, "Button is NULL");
            }

            button.getDrawable().setAlpha(alpha);
            button.invalidateDrawable(button.getDrawable());
        }

        public void setTimeAndState(long time, long delay, int moveCount,
                boolean isActive) {
            if (isActive)
                setActiveButtonAndMoveCount(this);
            mMoveNumber = moveCount;
            timer.initializeWithValue(time, delay);
        }

        public void adjustTime(long time) {
            timer.initializeWithValue(time, timer.getMsDelayToGo());
        }

        // Put the button into the initial 'IDLE' configuration
        public void reset() {
            mMoveNumber = 1;
            timer.reset();
            setTransparency(BUTTON_VISIBLE);
            mMoveCounter.setVisibility(View.GONE);
        }

        public void moveFinished() {

            mMoveNumber++;

            if (mTimeControlType == TimeControlType.TOURNAMENT) {
                if (mMoveNumber == (mPhase1NumberMoves + 1)) {
                    timer.increment(mPhase2Minutes * 60);
                }
            }

            timer.moveFinished();

            if (mDelayType == DelayType.FISCHER)
                timer.increment(mIncrementSeconds);

        }

        public void moveStarted() {
            if (mPlaySoundOnClick) {
                playClick();
            }
            timer.moveStarted();
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

            switch (mCurrentState) {

            case PAUSED:
                // alternate way to un-pause the activity
                // the primary way is to click the "Pause-Reset" toggle button
                mPauseButton.performClick();
                return;

            case DONE: // do nothing
                return;

            case RUNNING:
                if (mine.timer.isRunning()) {
                    mine.moveFinished();
                    other.moveStarted();
                    setActiveButtonAndMoveCount(other);
                }
                break;

            case IDLE:
                // the game just started
                setActiveButtonAndMoveCount(other);
                transitionTo(GameState.RUNNING);
                break;
            }
        }
    }

    public void confirmAndReset() {
        mPauseButton.performClick();
        // Ask the user if they want to reset
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(R.string.reset)
                .setMessage(R.string.really_reset)
                .setPositiveButton(R.string.yes,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                if (mPauseDialog != null)
                                    mPauseDialog.cancel();
                                loadAllUserPreferences();
                                transitionTo(GameState.IDLE);
                            }
                        }).setNegativeButton(R.string.no, null).show();
    }

    /**
     *  Show preferences
     */
    final class OverflowButtonClickListener implements OnClickListener {
        @Override
        public void onClick(View v) {
            transitionToPauseAndToast();
            launchPreferencesActivity();
        }
    }

    /**
     * Reset the clocks after confirmation.
     */
    final class ResetButtonClickListener implements OnClickListener {
        @Override
        public void onClick(View v) {
            confirmAndReset();
        }
    }

    /**
     * Pause the clock that is running.
     */
    final class StartButtonClickListener implements OnClickListener {
        @Override
        public void onClick(View v) {

            if (mCurrentState == GameState.DONE)
                return;
            if (mCurrentState == GameState.IDLE) {
                setActiveButtonAndMoveCount(mButton1);
                transitionTo(GameState.RUNNING);
                return;
            }
        }
    }

    /**
     * Pause the clock that is running.
     */
    final class PauseButtonClickListener implements OnClickListener {
        @Override
        public void onClick(View v) {

            if (mCurrentState == GameState.DONE)
                return;
            if (mCurrentState == GameState.PAUSED) {
                transitionTo(GameState.RUNNING);
            } else {
                transitionToPauseAndToast();
            }
        }
    }

    public void transitionToPauseAndToast() {
        if (mCurrentState == GameState.DONE || mCurrentState == GameState.IDLE)
            return;
        transitionTo(GameState.PAUSED);
        Toast.makeText(this, getString(R.string.pause_toast),
                Toast.LENGTH_SHORT).show();
    }

}