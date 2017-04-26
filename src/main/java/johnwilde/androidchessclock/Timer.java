package johnwilde.androidchessclock;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.SystemClock;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.FrameLayout;
import android.widget.TextView;

import johnwilde.androidchessclock.ChessTimerActivity.DelayType;
import johnwilde.androidchessclock.ChessTimerActivity.GameState;

// This class updates each player's clock.
// These variables maintain the clock state:
//
// mMsToGo: ms until the timer reaches 0
// mMsDelayToGo: ms until the Bronstein delay interval reaches 0
//
//
final class Timer implements OnClickListener, OnLongClickListener {
    private ChessTimerActivity mChessTimerActivity;
    private TextView mView;
    private FrameLayout mSpinContainer;
    private long mBronsteinMs;

    // maintain state
    private long mMsToGo;
    private long mMsDelayToGo = 0;
    private InnerTimer mCountDownTimer;
    private boolean isRunning = false;

    private String mPlayerColor;

    Timer(ChessTimerActivity chessTimerActivity, int clockId, int spinId, String playerColor) {
        mChessTimerActivity = chessTimerActivity;
        mView = (TextView) mChessTimerActivity.findViewById(clockId);
        mView.setFocusable(false);
        mSpinContainer = (FrameLayout) mChessTimerActivity.findViewById(spinId);
        mPlayerColor = playerColor;
        initialize();
    }

    @Override
    public void onClick(View v) {
        mChessTimerActivity.transitionToPauseAndToast();
        mChessTimerActivity.launchAdjustPlayerClockActivity(mPlayerColor, mMsToGo);
    }

    @Override
    public boolean onLongClick(View v) {
        mChessTimerActivity.transitionToPauseAndToast();
        mChessTimerActivity.launchPreferencesActivity();
        return true;
    }

    public void initialize() {
        mBronsteinMs = (
                mChessTimerActivity.mDelayType == DelayType.BRONSTEIN) ? mChessTimerActivity.mIncrementSeconds * 1000
                : 0;
        mMsDelayToGo = mBronsteinMs;
        initializeWithValue(mChessTimerActivity.mInitialDurationSeconds * 1000, mBronsteinMs);
        if (mChessTimerActivity.mDelayType == DelayType.FISCHER) {
            increment(mChessTimerActivity.mIncrementSeconds);
        }
    }

    public void initializeWithValue(long msToGo, long msDelayToGo) {
        mView.setOnClickListener(this);
        mView.setOnLongClickListener(this);
        mMsToGo = msToGo;
        mMsDelayToGo = msDelayToGo;
        mCountDownTimer = new InnerTimer();
        isRunning = false;
        mView.setTextColor(Color.BLACK);

        updateTimerText();
    }

    // Public Interface Methods

    void moveStarted() {
        start();
    }

    void moveFinished() {
        // when a move finishes we must reset the delay timer
        mMsDelayToGo = mBronsteinMs;
        pause();
    }

    void start() {
        mCountDownTimer.start();
        isRunning = true;
    }

    void pause() {
        if (mCountDownTimer != null) {
            mCountDownTimer.kill();
        }
        isRunning = false;
    }

    void reset() {
        mCountDownTimer.kill();
        initialize();
    }

    long getMsDelayToGo() {
        return mMsDelayToGo;
    }

    long getMsToGo() {
        return mMsToGo;
    }

    boolean isRunning() {
        return isRunning;
    }

    private boolean getAllowNegativeTime() {
        return mChessTimerActivity.mAllowNegativeTime;
    }

    //
    // Callbacks invoked by the inner timer
    //

    // callback that is invoked when the clock starts moving (after
    // Bronstein delay)
    private void clockStarted() {
        mSpinContainer.removeAllViews();
    }

    // callback that is invoked when clock pauses or is otherwise stopped
    private void clockStopped() {
        mSpinContainer.removeAllViews();
    }

    // callback that is invoked when clock reaches 0
    private void done() {
        mView.setText("0.0");
        mView.setTextColor(Color.RED);
        if (mChessTimerActivity.shouldPlaySoundAtEnd()) {
            mChessTimerActivity.playBell();
        }
        mChessTimerActivity.transitionTo(GameState.DONE);
    }

    void increment(int incrementSeconds) {
        mMsToGo += incrementSeconds * 1000;
        updateTimerText();
    }

    private void updateTimerText() {
        if (getMsToGo() < 10000) {
            mView.setTextColor(Color.RED);
        } else {
            mView.setTextColor(Color.BLACK);
        }

        mView.setText(Utils.formatTime(mMsToGo));
    }

    public View getView() {
        return mView;
    }

    /*
     * Inner class to handle the update of the timer text and playing the
     * buzzer. The timer text updates at faster rate during the last 10
     * seconds.
     */
    class InnerTimer {
        Handler mHandler = new Handler();
        private UpdateTimeTask mUpdateTimeTask;
        boolean mPlayedBuzzer = false;

        void start() {
            mUpdateTimeTask = new InnerTimer.UpdateTimeTask();
            mHandler.post(mUpdateTimeTask);
        }

        void kill() {
            clockStopped();
            if (mUpdateTimeTask != null) {
                mUpdateTimeTask.kill();
            }
            mHandler.removeCallbacks(mUpdateTimeTask);
        }

        // this class will update itself (and call
        // updateTimerText) accordingly:
        // if getMsToGo() > 10 * 1000, every 1000 ms
        // if getMsToGo() < 10 * 1000, every 100 ms
        // if getMsToGo() < 0 and getAllowNegativeTime is true, every 1000
        // ms
        class UpdateTimeTask implements Runnable {
            boolean startOfMove = true;

            public synchronized void setMoveStartFlag(boolean value) {
                this.startOfMove = value;
            }

            long startedAt;
            long lastUpdate;
            InnerTimer.SpinnerView spinner;
            static final int POST_SLOW = 1000;
            static final int POST_FAST = 100;

            UpdateTimeTask() {
                this.startedAt = SystemClock.uptimeMillis();
                this.lastUpdate = startedAt;
                if (mMsDelayToGo > 0) {
                    spinner = new InnerTimer.SpinnerView(mChessTimerActivity,
                            mBronsteinMs);
                    mSpinContainer.addView(spinner);
                }
            }

            void kill() {
                long dt = SystemClock.uptimeMillis() - lastUpdate;
                mMsDelayToGo -= dt;
            }

            public void run() {
                long now = SystemClock.uptimeMillis();
                long dt = now - lastUpdate;
                lastUpdate = now;
                // Are we in Bronstein delay period?
                if (startOfMove && mMsDelayToGo > 0) {
                    mMsDelayToGo -= dt;
                    spinner.setElapsedMilliseconds(mMsDelayToGo);
                    spinner.postInvalidate();
                    mHandler.postDelayed(mUpdateTimeTask, POST_FAST);
                    return;
                }

                if (startOfMove) {
                    Timer.this.clockStarted(); // invoke callback
                    setMoveStartFlag(false);
                    mMsDelayToGo = mBronsteinMs; // reset
                }
                mMsToGo -= dt;

                if (getMsToGo() > 0) {
                    updateTimerText();
                    mHandler.postDelayed(mUpdateTimeTask, POST_FAST);
                } else if (getMsToGo() < 0 && getAllowNegativeTime()) {
                    updateTimerText();
                    if (mChessTimerActivity.shouldPlaySoundAtEnd() && mPlayedBuzzer == false) {
                        mChessTimerActivity.playBell();
                        mPlayedBuzzer = true;
                    }

                    mHandler.postDelayed(mUpdateTimeTask, POST_SLOW);
                } else {
                    mHandler.removeCallbacks(mUpdateTimeTask);
                    done();
                }
            }
        }

        private class SpinnerView extends View {
            @Override
            protected void onMeasure(int widthMeasureSpec,
                    int heightMeasureSpec) {
                setMeasuredDimension(50, 50);
            }

            private Paint mPaint;
            private RectF mOval;
            private long msTotal;
            private long msSoFar;

            public SpinnerView(Context context, long msToComplete) {
                super(context);
                this.msTotal = msToComplete;
                this.msSoFar = 0;
                mPaint = new Paint();
                mPaint.setAntiAlias(true);
                mPaint.setStyle(Paint.Style.FILL);
                mPaint.setColor(0x88FF0000);
                mOval = new RectF(0, 0, 50, 50);
            }

            private void drawArcs(Canvas canvas, float sweep) {
                canvas.drawArc(mOval, 0, sweep, true, mPaint);
            }

            synchronized void setElapsedMilliseconds(long elapsed) {
                this.msSoFar = elapsed;
            }

            @Override
            protected void onDraw(Canvas canvas) {
                // Log.d(TAG,String.format("drawing spinner msTotal:%d, msSoFar:%d",
                // msTotal, msSoFar));
                double sweep = 360.0 * (1.0 - ((float) (msTotal - msSoFar) / (float) msTotal));
                drawArcs(canvas, (float) sweep);
            }
        }
    }

}
