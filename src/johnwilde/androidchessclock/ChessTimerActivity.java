package johnwilde.androidchessclock;

import java.io.IOException;
import java.text.DecimalFormat;

import johnwilde.androidchessclock.TimerOptions.TimeControl;
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
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
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
    enum GameState {IDLE, RUNNING, PAUSED, DONE};
    GameState mCurrentState = GameState.IDLE;

    enum TimeControlType {BASIC, TOURNAMENT};
    TimeControlType mTimeControlType = TimeControlType.BASIC;

    enum DelayType {FISCHER, BRONSTEIN;}
    DelayType mDelayType;

    PlayerButton mButton1, mButton2;  	// The two big buttons
    Button mResetButton;				
    ToggleButton mPauseButton;
    AlertDialog mPauseDialog; 
    
    // This field holds a reference to either mButton1 or mButton2.
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
    MediaPlayer mMediaPlayer;

    private boolean mPlaySoundAtEnd;

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

        // the layout looks best in landscape orientation
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        mButton1 = new PlayerButton( new Timer(R.id.whiteClock, R.id.whiteSpinnerContainer), R.id.whiteButton, R.id.whiteMoveCounter);
        mButton2 = new PlayerButton( new Timer(R.id.blackClock, R.id.blackSpinnerContainer), R.id.blackButton, R.id.blackMoveCounter);

        mResetButton = (Button) findViewById(R.id.reset_button);
        mPauseButton = (ToggleButton) findViewById(R.id.pause_button);
        mPauseButton.setOnClickListener(new PauseButtonClickListener());

        mButton1.setButtonListener(new PlayerButtonClickListener(mButton1, mButton2));
        mButton2.setButtonListener(new PlayerButtonClickListener(mButton2, mButton1));

        mResetButton.setOnClickListener(new ResetButtonClickListener());
        mSharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        // enable following line to clear settings if they are in a bad state
        //mSharedPref.edit().clear().apply();

        // set default values (for first run)
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        loadAllUserPreferences();

        transitionTo(GameState.IDLE);

        acquireWakeLock();
        acquireMediaPlayer();

        Log.d(TAG, "Finished onCreate()");
    }
    private static class SpinnerView extends View {
        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            setMeasuredDimension(50, 50);
        }

        private Paint mPaint;
        private RectF mOval;
        private long mStartTime;
        private int mDurationMs;
        public SpinnerView(Context context, int durationMs) {
            super(context);
            mDurationMs = durationMs;
            mStartTime = SystemClock.uptimeMillis();
            mPaint = new Paint();
            mPaint.setAntiAlias(true);
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setColor(0x88FF0000);
            mOval = new RectF( 0, 0, 50, 50);

        }

        private void drawArcs(Canvas canvas, float sweep) {
            canvas.drawArc(mOval, 0, sweep, true, mPaint);
        }

        @Override protected void onDraw(Canvas canvas) {
            double ellapsedTime =  (double) (SystemClock.uptimeMillis() - mStartTime); 
            double sweep = 360.0 *( 1.0 - ( (mDurationMs - ellapsedTime ) / mDurationMs) );
            drawArcs(canvas, (float)sweep);
            invalidate();
        }
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

    // Save data needed to recreate activity.  Enter PAUSED state
    // if we are currently RUNNING.
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mCurrentState == GameState.RUNNING)
            mPauseButton.performClick(); // pause, if not IDLE

        outState.putLong("Timer1", mButton1.timer.getMsToGo() );
        outState.putLong("Timer2", mButton2.timer.getMsToGo() );
        outState.putInt("MoveCounter1", mButton1.mMoveNumber );
        outState.putInt("MoveCounter2", mButton2.mMoveNumber );
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

        mButton1.setTimeAndState(savedInstanceState.getLong("Timer1"),
                savedInstanceState.getInt("MoveCounter1"), button1Active);
        mButton2.setTimeAndState(savedInstanceState.getLong("Timer2"), 
                savedInstanceState.getInt("MoveCounter2"), button2Active);

        if (stateToRestore == GameState.DONE){
            transitionTo(GameState.DONE);
            return;
        }

        if (stateToRestore == GameState.PAUSED){
            transitionToPauseAndShowDialog();
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

    // This method is called when the user preferences activity returns.  That
    // activity set fields in the Intent data to indicate what preferences
    // have been changed.  The method takes the action appropriate for what
    // has changed.  In some cases the clocks are reset.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // check which activity has returned (for now we only have one, so
        // it isn't really necessary).
        if (requestCode == REQUEST_CODE_PREFERENCES) {

            if (data == null)
                return; // no preferences were changed

            // reset clocks using new settings
            if (data.getBooleanExtra(TimerOptions.TimerPref.LOAD_ALL.toString(), false)){
                loadAllUserPreferences();
                transitionTo(GameState.IDLE);
                return; // exit early
            }
            else{
                loadUiPreferences();
            }
        }
    }

    private void acquireMediaPlayer() {
        releaseMediaPlayer();
        if (mPlaySoundAtEnd) {
            try {
                AssetFileDescriptor afd = getResources().openRawResourceFd(
                        R.raw.buzzer);
                if (afd == null)
                    return;
                mMediaPlayer = new MediaPlayer();
                mMediaPlayer.setDataSource(afd.getFileDescriptor(), afd
                        .getStartOffset(), afd.getLength());
                afd.close();
                mMediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
                mMediaPlayer.prepareAsync();
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
        }
    }

    private void releaseMediaPlayer() {
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    private void releaseWakeLock() {
        if (mWakeLock != null){
            if ( mWakeLock.isHeld() ) {
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

    // All state transitions occur here.  The logic that controls
    // the UI elements is here.
    public void transitionTo(GameState state){
        GameState start = mCurrentState;

        switch (state){
        case IDLE:
            mCurrentState = GameState.IDLE;
            mResetButton.setEnabled(false);
            mPauseButton.setClickable(true); 
            mPauseButton.setTextOff(getString(R.string.pauseinit_button));
            mPauseButton.setChecked(false); // set to 'off' state
            mButton1.reset();
            mButton2.reset();
            break;

        case RUNNING:
            mCurrentState = GameState.RUNNING;
            mResetButton.setEnabled(true);
            mPauseButton.setClickable(true); // enable 'pause'
            mPauseButton.setChecked(false);
            mPauseButton.setTextOff(getString(R.string.pauseoff_button));

            // start the clock
            mActive.moveStarted();
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
                mResetButton.setEnabled(true);
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

    public void setActiveButtonAndMoveCount(PlayerButton button) {
        mActive = button;

        // Give visual indication of which player goes next by fading
        // the button of the player who just moved
        mActive.setTransparency(BUTTON_VISIBLE);
        PlayerButton other = (mButton1 == mActive ? mButton2 : mButton1);
        other.setTransparency(BUTTON_FADED);

        if (mShowMoveCounter){
            mActive.mMoveCounter.setVisibility(View.VISIBLE);
            String s = getString(R.string.move_counter_text) + " " + mActive.mMoveNumber;
            mActive.mMoveCounter.setText(s);
            other.mMoveCounter.setVisibility(View.GONE);
        }
        else{
            mActive.mMoveCounter.setVisibility(View.GONE);
            other.mMoveCounter.setVisibility(View.GONE);
        }

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

    public void showPauseDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.pause_dialog);
        builder.setPositiveButton(getString(R.string.pauseon_button),
                new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
                mPauseButton.performClick();
            }
        });
        mPauseDialog = builder.create();
        mPauseDialog.show();
    }

    private String getPackageVersion(){

        try {
            PackageInfo manager=getPackageManager().getPackageInfo(getPackageName(), 0);
            return manager.versionName;
        } catch (NameNotFoundException e) {
            return getString(R.string.unknown);
        }
    }


    // Methods for loading USER PREFERENCES
    //
    // These methods are run after the user has changed
    // a preference and during onCreate().  The onActivityResult()
    // method is responsible for calling the method that matches
    // the preference that was changed.
    //
    // Note: the default values required by the SharedPreferences getXX 
    // methods are not used.  The SharedPreferences will have  their default 
    // values set (in onCreate() ) and those defaults are saved in preferences.xml

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
        TimerOptions.TimeControl timeControl = TimerOptions.TimeControl.valueOf(
                mSharedPref.getString(TimerOptions.Key.TIMECONTROL_TYPE.toString(),
                "DISABLED")
        );

        if (timeControl == TimeControl.DISABLED){
            mTimeControlType = TimeControlType.BASIC;
            loadBasicTimeControlUserPreference();
        }else{
            mTimeControlType = TimeControlType.TOURNAMENT;
            loadAdvancedTimeControlUserPreference();
        }
    }

    private void loadBasicTimeControlUserPreference(){
        loadInitialTimeUserPreferences();
        loadIncrementUserPreference(TimerOptions.Key.INCREMENT_SECONDS);
        loadDelayTypeUserPreference(TimerOptions.Key.DELAY_TYPE);
        loadNegativeTimeUserPreference(TimerOptions.Key.NEGATIVE_TIME);
    }

    private void loadAdvancedTimeControlUserPreference() {

        int minutes1 = getTimerOptionsValue( TimerOptions.Key.FIDE_MIN_PHASE1 );

        setInitialDuration( minutes1 * 60 );

        mPhase1NumberMoves = getTimerOptionsValue( TimerOptions.Key.FIDE_MOVES_PHASE1 );
        mPhase2Minutes = getTimerOptionsValue( TimerOptions.Key.FIDE_MIN_PHASE2 );

        loadDelayTypeUserPreference(TimerOptions.Key.ADV_DELAY_TYPE);
        loadIncrementUserPreference(TimerOptions.Key.ADV_INCREMENT_SECONDS);
        loadNegativeTimeUserPreference( TimerOptions.Key.ADV_NEGATIVE_TIME );
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
        mPlaySoundAtEnd= mSharedPref.getBoolean(TimerOptions.Key.PLAY_SOUND.toString(), false);
    }

    private void configureSides() {
        View whiteClock = findViewById(R.id.whiteClock);
        View blackClock = findViewById(R.id.blackClock);

        View whiteButton = findViewById(R.id.whiteButton);
        View blackButton = findViewById(R.id.blackButton);

        View whiteMoveCounter = findViewById(R.id.whiteMoveCounter);
        View blackMoveCounter = findViewById(R.id.blackMoveCounter);

        FrameLayout whiteSpinnerContainer = (FrameLayout)findViewById(R.id.whiteSpinnerContainer);
        FrameLayout blackSpinnerContainer = (FrameLayout)findViewById(R.id.blackSpinnerContainer);

        FrameLayout leftClockContainer = (FrameLayout)findViewById(R.id.leftClockContainer);
        FrameLayout rightClockContainer = (FrameLayout)findViewById(R.id.rightClockContainer);
        leftClockContainer.removeAllViewsInLayout();
        rightClockContainer.removeAllViewsInLayout();	

        FrameLayout leftButtonContainer = (FrameLayout)findViewById(R.id.frameLayoutLeft);
        FrameLayout rightButtonContainer = (FrameLayout)findViewById(R.id.frameLayoutRight);
        leftButtonContainer.removeAllViewsInLayout();
        rightButtonContainer.removeAllViewsInLayout();	

        LinearLayout leftSpinnerContainer = (LinearLayout)findViewById(R.id.leftSpinnerContainer);
        LinearLayout rightSpinnerContainer = (LinearLayout)findViewById(R.id.rightSpinnerContainer);        
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
        } else 
        {
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

    private void loadDelayTypeUserPreference(TimerOptions.Key key){
        String[] delayTypes = getResources().getStringArray(R.array.delay_type_values);
        String delayTypeString = mSharedPref.getString(key.toString(), delayTypes[0]);

        mDelayType = DelayType.valueOf(delayTypeString.toUpperCase());
    }

    private void loadScreenDimUserPreference() {
        boolean allowScreenToDim = mSharedPref.getBoolean(TimerOptions.Key.SCREEN_DIM.toString(), true);
        /** Create a PowerManager object so we can get the wakelock */
        mWakeLockType = allowScreenToDim ? PowerManager.SCREEN_DIM_WAKE_LOCK:
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK;
    }

    private void loadIncrementUserPreference(TimerOptions.Key key) {
        int seconds = getTimerOptionsValue( key );
        setIncrement(seconds);
    }

    private void loadInitialTimeUserPreferences() {
        int minutes = getTimerOptionsValue( TimerOptions.Key.MINUTES );
        int seconds = getTimerOptionsValue(TimerOptions.Key.SECONDS );
        setInitialDuration(minutes * 60 + seconds);
    }

    private int getTimerOptionsValue( TimerOptions.Key key ){
        try{
            String s = mSharedPref.getString(key.toString(), "0" );

            if (s.length() == 0){
                s = "0";
            }

            return ( Integer.parseInt( s ) );

        }catch (NumberFormatException ex){
            Log.d(TAG, ex.getMessage() );
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
    class PlayerButton{
        Timer timer;
        ImageButton button;
        TextView mMoveCounter;
        boolean isFaded = false;
        private int mId;
        private int mMoveNumber;

        PlayerButton(Timer timer, int buttonId, int moveCounterId){
            this.timer = timer;
            button = (ImageButton)findViewById(buttonId);
            mMoveCounter = (TextView)findViewById(moveCounterId);
            mId = buttonId;
            mMoveNumber = 1;
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
                Log.e(TAG, "Button is NULL");
            }

            button.getDrawable().setAlpha(alpha);
            button.invalidateDrawable(button.getDrawable());
        }


        public void setTimeAndState(long time, int moveCount, boolean isActive){
            if (isActive)
                setActiveButtonAndMoveCount(this);
            mMoveNumber = moveCount;
            timer.initializeWithValue(time);
        }

        // Put the button into the initial 'IDLE' configuration
        public void reset(){
            mMoveNumber = 1;
            timer.reset();
            setTransparency(BUTTON_VISIBLE);
            mMoveCounter.setVisibility(View.GONE);
        }

        public void moveFinished() {
            mMoveNumber++;

            if ( mTimeControlType == TimeControlType.TOURNAMENT ){
                if ( mMoveNumber == (mPhase1NumberMoves + 1) ){
                    timer.increment(mPhase2Minutes * 60);
                }
            }

            timer.pause();

            if (mDelayType == DelayType.FISCHER)
                timer.increment(mIncrementSeconds);

        }

        public void moveStarted() {
            if (mDelayType == DelayType.BRONSTEIN){
                timer.start(mIncrementSeconds*1000);
            }
            else{
                timer.start(0);
            }
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

    public void confirmAndReset(){
        mPauseButton.performClick();
        //Ask the user if they want to reset
        new AlertDialog.Builder(this)
        .setIcon(android.R.drawable.ic_dialog_alert)
        .setTitle(R.string.reset)
        .setMessage(R.string.really_reset)
        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override public void onClick(DialogInterface dialog, int which) {
                mPauseDialog.cancel();
                loadAllUserPreferences();
                transitionTo(GameState.IDLE);
            }
        })
        .setNegativeButton(R.string.no, null)
        .show();
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
    final class PauseButtonClickListener implements OnClickListener {
        @Override
        public void onClick(View v) {

            if (mCurrentState == GameState.DONE)
                return;
            if (mCurrentState == GameState.IDLE){
                setActiveButtonAndMoveCount(mButton1);
                transitionTo(GameState.RUNNING);
                mPauseButton.setChecked(false); // set toggle to show "pause" text
                return;
            }
            if ( mCurrentState == GameState.PAUSED ){
                transitionTo(GameState.RUNNING);
            }
            else{
                transitionToPauseAndShowDialog();
            }
        }
    }

    public void transitionToPauseAndShowDialog(){
        transitionTo(GameState.PAUSED);
        showPauseDialog();
    }

    public void transitionToPauseAndToast(){
        if (mCurrentState == GameState.DONE ||
                mCurrentState == GameState.IDLE)
            return;
        transitionTo(GameState.PAUSED);
        Toast.makeText(this, getString(R.string.pause_toast), Toast.LENGTH_SHORT).show();
    }

    // This class updates each player's clock.
    final class Timer implements OnLongClickListener {
        TextView mView;
        SpinnerView mSpinView;
        FrameLayout mSpinContainer;
        long mMillisUntilFinished;
        InnerTimer mCountDownTimer;
        boolean isRunning = false;

        // formatters for displaying text in timer
        DecimalFormat dfOneDecimal = new DecimalFormat("0.0");
        DecimalFormat dfOneDigit = new DecimalFormat("0");
        DecimalFormat dfTwoDigit = new DecimalFormat("00");

        Timer(int id, int spinId) {
            mView = (TextView) findViewById(id);
            mSpinContainer = (FrameLayout)findViewById(spinId);
            initialize();
        }

        // callback that is invoked when a move starts
        public void moveStarted(){
            mSpinContainer.removeAllViews();
        }
        // callback that is invoked when clock pauses or is otherwise stopped
        public void clockStopped() {
            mSpinContainer.removeAllViews();
        }

        public void initializeWithValue(long msToGo) {
            mView.setLongClickable(true);
            mView.setOnLongClickListener(this);
            mMillisUntilFinished = msToGo;
            mCountDownTimer = new InnerTimer();
            isRunning = false;
            mView.setTextColor( Color.BLACK );

            updateTimerText();			
        }

        public void initialize() {
            initializeWithValue(mInitialDurationSeconds * 1000);
            if (mDelayType ==  DelayType.FISCHER)
                increment(mIncrementSeconds);
        }

        public void increment(int mIncrementSeconds) {
            mMillisUntilFinished += mIncrementSeconds * 1000;
            updateTimerText();
        }

        public boolean isRunning() {
            return isRunning;
        }

        public void start(int delayMillis) {
            if (delayMillis > 0){
                mSpinContainer.addView(new SpinnerView(ChessTimerActivity.this, delayMillis));
            }
            mCountDownTimer.startAfterDelay(delayMillis);
            isRunning = true;
        }

        public void pause() {
            if (mCountDownTimer != null)
                mCountDownTimer.pause();
            isRunning = false;
        }

        public void reset() {
            mCountDownTimer.cancel();
            initialize();
        }

        public void updateTimerText() {
            if (getMsToGo() < 10000)
                mView.setTextColor(Color.RED);
            else
                mView.setTextColor(Color.BLACK);

            mView.setText(formatTime(mMillisUntilFinished));
        }

        private String formatTime(long millisIn) {
            // 1000 ms in 1 second
            // 60*1000 ms in 1 minute
            // 60*60*1000 ms in 1 hour

            String stringSec, stringMin, stringHr;
            long millis = Math.abs(millisIn);

            // Parse the input (in ms) into integer hour, minute, and second
            // values
            long hours = millis / (1000 * 60 * 60);
            millis -= hours * (1000 * 60 * 60);

            long min = millis / (1000 * 60);
            millis -= min * (1000 * 60);

            long sec = millis / 1000;
            millis -= sec * 1000;

            // Construct string
            if (hours > 0)
                stringHr = dfOneDigit.format(hours) + ":";
            else
                stringHr = "";

            if (hours > 0 )
                stringMin = dfTwoDigit.format(min) + ":";
            else if ( min > 0)
                stringMin = dfOneDigit.format(min) + ":";
            else	
                stringMin = "";

            stringSec = dfTwoDigit.format(sec);  

            if (hours==0 && min==0){
                // Desired behavior:
                // 
                // for 0 <= millisIn < 10000 (between 0 and 10 seconds)
                //   clock should read like: "N.N"
                // for -999 <= millisIn <= -1 (the second after passing 0.0)
                //   clock should read "0"
                // for millisIn < -999 (all time less than -1 seconds)
                //   clock should read like : "-N"

                // modify formatting when less than 10 seconds
                if (sec < 10 && millisIn >= 0) // between 0 and 9 seconds
                    stringSec = dfOneDecimal.format((double) sec
                            + (double) millis / 1000.0);
                else if (sec < 10 && millisIn < 0) //  between -1 and -9
                    stringSec = dfOneDigit.format((double) sec
                            + (double) millis / 1000.0);
            }

            // clock is <= -1 second, prepend a minus sign
            if (millisIn <= -1000){
                return "-" + stringHr + stringMin + stringSec;
            }

            return stringHr + stringMin + stringSec;

        }

        long getMsToGo(){
            return mMillisUntilFinished;
        }

        boolean getAllowNegativeTime(){
            return mAllowNegativeTime;
        }

        public void done() {
            mView.setText("0.0");
            mView.setTextColor(Color.RED);
            if (mMediaPlayer != null)
                mMediaPlayer.start();
            transitionTo(GameState.DONE);
        }

        @Override
        public boolean onLongClick(View v) {
            // launch activity that allows user to set time and increment values
            transitionToPauseAndToast();
            launchPreferencesActivity();
            return true;
        }

        public View getView() {
            return mView;
        }

        /*
         * Inner class to handle the update of the timer text and
         * playing the buzzer. The timer text updates at faster 
         * rate during the last 10 seconds.
         */
        class InnerTimer {
            long mLastUpdateTime = 0L;
            Handler mHandler = new Handler();
            private final UpdateTimeTask mUpdateTimeTask = new UpdateTimeTask();
            // this class will update itself (and call
            // updateTimerText) accordingly:
            //     if getMsToGo() > 10 * 1000, every 1000 ms
            //     if getMsToGo() < 10 * 1000, every 100 ms
            //     if getMsToGo() < 0 and getAllowNegativeTime is true, every 1000 ms
            class UpdateTimeTask implements Runnable {
                boolean startOfMove = true;
                public synchronized void setMoveStartFlag(boolean value){this.startOfMove = value;}
                boolean playedBuzzer = false;
                public void run(){
                    if (startOfMove){
                        Timer.this.moveStarted(); // invoke callback
                        setMoveStartFlag(false);
                    }
                    long ellapsedTime = SystemClock.uptimeMillis() - mLastUpdateTime;
                    mMillisUntilFinished -= ellapsedTime;
                    mLastUpdateTime = SystemClock.uptimeMillis();

                    if (getMsToGo() > 10000){
                        updateTimerText();
                        mHandler.postDelayed(mUpdateTimeTask, 1000);
                    }
                    else if (getMsToGo() < 10000 && getMsToGo() > 0){
                        updateTimerText();
                        mHandler.postDelayed(mUpdateTimeTask, 100);
                    }
                    else if (getMsToGo() < 0 && getAllowNegativeTime()){
                        updateTimerText();
                        if (mMediaPlayer != null && playedBuzzer == false){
                            mMediaPlayer.start();
                            playedBuzzer = true;
                        }

                        mHandler.postDelayed(mUpdateTimeTask, 1000);
                    }
                    else{
                        mHandler.removeCallbacks(mUpdateTimeTask);
                        done();
                        return;
                    }
                }
            };

            void startAfterDelay(int delayMillis){
                mUpdateTimeTask.setMoveStartFlag(true);
                mLastUpdateTime = SystemClock.uptimeMillis() + delayMillis;
                mHandler.postDelayed(mUpdateTimeTask, delayMillis);
            }

            void pause() {
                Timer.this.clockStopped();
                mHandler.removeCallbacks(mUpdateTimeTask);
                // if called from onRestoreInstanceState(), mLastUpdateTime == 0 because
                // its value is not persisted.  No need to do anything else.
                if (mLastUpdateTime != 0L){
                    // account for the time that has elapsed since our last update and pause the clock
                    // if using BRONSTEIN this may be negative, so check for that case.
                    long msSinceLastUpdate = ( SystemClock.uptimeMillis() - mLastUpdateTime );
                    if (msSinceLastUpdate > 0)
                        mMillisUntilFinished -= msSinceLastUpdate;
                }
            }

            void cancel() {
                Timer.this.clockStopped();
                mHandler.removeCallbacks(mUpdateTimeTask);
            }

        }


    }

}