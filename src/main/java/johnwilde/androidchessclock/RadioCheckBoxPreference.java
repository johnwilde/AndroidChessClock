package johnwilde.androidchessclock;

import android.annotation.TargetApi;
import android.content.Context;
import android.preference.CheckBoxPreference;
import android.util.AttributeSet;

public class RadioCheckBoxPreference extends CheckBoxPreference {

    private boolean mEnabled = true;
    public RadioCheckBoxPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(21)
    public RadioCheckBoxPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public RadioCheckBoxPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setEnabled(boolean value) {
        mEnabled = value;
    }

    @Override
    protected void onClick() {
        if (mEnabled) {
            super.onClick();
        }
    }

    public RadioCheckBoxPreference(Context context) {
        super(context);
    }
}
