package johnwilde.androidchessclock;

import android.test.ActivityInstrumentationTestCase2;

/**
 * This is a simple framework for a test of an Application.  See
 * {@link android.test.ApplicationTestCase ApplicationTestCase} for more information on
 * how to write and extend Application tests.
 * <p/>
 * To run this test, you can type:
 * adb shell am instrument -w \
 * -e class johnwilde.androidchessclock.ChessTimerActivityTest \
 * johnwilde.androidchessclock.tests/android.test.InstrumentationTestRunner
 */
public class ChessTimerActivityTest extends ActivityInstrumentationTestCase2<ChessTimerActivity> {

    public ChessTimerActivityTest() {
        super("johnwilde.androidchessclock", ChessTimerActivity.class);
    }

}
