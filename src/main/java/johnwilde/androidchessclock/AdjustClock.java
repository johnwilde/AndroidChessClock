package johnwilde.androidchessclock;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Activity that lets user adjust a player's clock
 */
public class AdjustClock extends Activity {

    private static final String TAG = "AdjustClockActivity";
    private TextView mView;
    Button mAddTime, mSubtractTime, mDone, mCancel;
    ImageView mImageView;

    String mColor;
    long mStartTime, mTime;
    public final static String NEW_TIME = "johnwilde.androidchessclock.NEW_TIME";
    public final static String COLOR = "johnwilde.androidchessclock.COLOR";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        mColor = intent.getStringExtra(ChessTimerActivity.EXTRA_COLOR);
        long time = intent.getLongExtra(ChessTimerActivity.EXTRA_TIME, 0);

        setContentView(R.layout.adjust_time);
        mStartTime = time;
        mTime = time;
        mView = (TextView) findViewById(R.id.adjustTimeTextView);
        mAddTime = (Button) findViewById(R.id.add_time_button);
        mSubtractTime = (Button) findViewById(R.id.subtract_time_button);
        mDone = (Button) findViewById(R.id.ok_button);
        mCancel = (Button) findViewById(R.id.cancel_button);

        mAddTime.setOnClickListener(new AdjustTimeListener(10));
        mSubtractTime.setOnClickListener(new AdjustTimeListener(-10));
        mDone.setOnClickListener(new OnDoneListener());
        mCancel.setOnClickListener(new OnCancelListener());
        
        mImageView = (ImageView) findViewById(R.id.adjustTimePlayerView);
        if (mColor.equalsIgnoreCase("white"))
            mImageView.setImageResource(R.drawable.white);
        else
            mImageView.setImageResource(R.drawable.black);
        
        mImageView.getDrawable().setAlpha(255);
        mImageView.invalidate();

        updateTimeView();

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        setResult(RESULT_OK, getIntent().putExtra(COLOR, mColor));
    }
    
    protected void resetTime() {
        mTime = mStartTime;
        updateTimeView();
        setResult(RESULT_OK, getIntent().putExtra(NEW_TIME, mTime));
    }

    protected void adjustTime(long msec) {
        mTime += msec;
        updateTimeView();
        setResult(RESULT_OK, getIntent().putExtra(NEW_TIME, mTime));
  
    }

    protected void updateTimeView() {
        mView.setText(Utils.formatTime(mTime));
        mView.invalidate();
    }
    
    final class OnDoneListener implements OnClickListener {
        public void onClick(View v) {
            finish();
        }
    }
    
    final class OnCancelListener implements OnClickListener {
        public void onClick(View v) {
            resetTime();
            finish();
        }
    }
    
    final class AdjustTimeListener implements OnClickListener {
        long mIncrementMsec;

        public AdjustTimeListener(long incrementSec) {
            mIncrementMsec = incrementSec * 1000;
        }

        public void onClick(View v) {
            adjustTime(mIncrementMsec);

        }
    }
}
