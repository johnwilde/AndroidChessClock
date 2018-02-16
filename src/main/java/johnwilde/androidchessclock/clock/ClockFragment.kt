package johnwilde.androidchessclock.clock

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.widget.DrawerLayout
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.hannesdorfmann.mosby3.mvi.MviFragment
import com.jakewharton.rxbinding2.view.RxView
import dagger.android.support.AndroidSupportInjection
import io.reactivex.Observable
import johnwilde.androidchessclock.AdjustClock
import johnwilde.androidchessclock.R
import johnwilde.androidchessclock.Utils
import johnwilde.androidchessclock.logic.ClockManager
import johnwilde.androidchessclock.logic.GameStateHolder
import johnwilde.androidchessclock.main.HasSnackbar
import johnwilde.androidchessclock.main.REQUEST_CODE_ADJUST_TIME
import johnwilde.androidchessclock.prefs.PreferencesUtil
import kotlinx.android.synthetic.main.clock_button.*
import timber.log.Timber
import javax.inject.Inject


// This "View" represents one button and the TextView that shows the time remaining
const val BUTTON_FADED : Int = 25
const val BUTTON_VISIBLE : Int = 255
class ClockFragment : MviFragment<ClockView, ClockViewPresenter>(), ClockView {
    lateinit var color : ClockView.Color
    private lateinit var drawerListener : SimpleDrawerListener
    var leftSlopPx : Int = 0
    @Inject lateinit var clockManager : ClockManager
    @Inject lateinit var preferences : PreferencesUtil

    companion object {
        private val COLOR = "ARG_COLOR"

        fun newInstance(color: ClockView.Color): ClockFragment {
            val args = Bundle()
            args.putSerializable(COLOR, color)
            val fragment = ClockFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun createPresenter(): ClockViewPresenter {
        return ClockViewPresenter(color, clockManager)
    }

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
        color = arguments!!.getSerializable(COLOR) as ClockView.Color
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("ClockFragment create")
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.clock_button, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        leftSlopPx = view.resources.getDimensionPixelSize(R.dimen.left_slop) // left margin so button isn't triggered while swiping drawer
        drawerListener = SimpleDrawerListener()
        activity?.findViewById<DrawerLayout>(R.id.drawerLayout)
                ?.addDrawerListener(drawerListener)
        button.setImageResource(if (color == ClockView.Color.WHITE) R.drawable.white else R.drawable.black)
        clock.setOnClickListener({ launchAdjustPlayerClockActivity() })
    }

    // Player tapped the button to end their turn
    override fun clickIntent(): Observable<Any> {
        return RxView.touches(button)
                // Filter points on left of screen so they don't interfere with drawer
                .filter {
                    val p = clockButton.parent.parent as View
                    val outArray = IntArray(2)
                    button.getLocationOnScreen(outArray)
                    val x = if (p.scaleX < 1) { // in portrait, the view is reversed
                        outArray[0] - it.x
                    } else {
                        it.x + outArray[0]
                    }
                    x > leftSlopPx && !drawerListener.drawerIsDragging
                }
                .map { 1 }
    }

    // Update the button's visible viewState and the time text
    override fun render(viewState: ClockViewState) {
        renderClock(viewState.button)
        renderTimeGap(viewState.timeGap)
        viewState.prompt?.let { renderSnackbar(it) }
        renderMoveCount(viewState.moveCount)
    }

    private fun renderTimeGap(viewState: ClockViewState.TimeGap) {
        timeGap.visibility = if (viewState.show) {
            View.VISIBLE
        } else {
            View.GONE
        }
        timeGap.text = Utils.formatTimeGap(viewState.msGap)
        timeGap.isChecked = viewState.msGap < 0
    }

    private fun renderClock(buttonViewState: ClockViewState.Button) {
        clock.text = Utils.formatClockTime(buttonViewState.msToGo)
        clock.alpha = if (buttonViewState.enabled) 1.0f else .1f
        clock.isChecked = buttonViewState.msToGo < 10_000
        button.drawable.alpha = if (buttonViewState.enabled) BUTTON_VISIBLE else BUTTON_FADED
    }

    private fun renderMoveCount(mc: ClockViewState.MoveCount) {
        moveCount.text = when(mc.message) {
            ClockViewState.MoveCount.Message.REMAINING ->
                resources.getString(R.string.moves_remaining) + " " + mc.count.toString()
            ClockViewState.MoveCount.Message.NONE -> ""
            ClockViewState.MoveCount.Message.TOTAL ->
                resources.getString(R.string.move) + " " + mc.count.toString()
        }
        moveCount.visibility = if (mc.message != ClockViewState.MoveCount.Message.NONE) {
            View.VISIBLE
        } else View.INVISIBLE
    }

    private fun renderSnackbar(viewState: ClockViewState.Snackbar) {
        val a = activity as HasSnackbar // hosting activity will show the snackbar
        if (viewState.show) {
            val message = when (viewState.message) {
                ClockViewState.Snackbar.Message.START -> resources.getString(R.string.tap_other_button_start)
                ClockViewState.Snackbar.Message.RESUME -> resources.getString(R.string.tap_other_button_resume)
                null -> ""
            }
            a.showSnackbar(message)
        } else if (viewState.dismiss) {
            a.hideSnackbar()
        }
    }

    private fun launchAdjustPlayerClockActivity() {
        // launch an activity through this intent
        if (clockManager.stateHolder.gameState != GameStateHolder.GameState.FINISHED) {
            val launchAdjustPlayerClockIntent = Intent().setClass(activity, AdjustClock::class.java)
            launchAdjustPlayerClockIntent.putExtra(AdjustClock.EXTRA_COLOR, color.toString())
            launchAdjustPlayerClockIntent.putExtra(
                    AdjustClock.EXTRA_TIME, clockManager.timerForColor(color).msToGo)
            clockManager.pause()
            startActivityForResult(launchAdjustPlayerClockIntent, REQUEST_CODE_ADJUST_TIME)
        }
    }

    // This method is called when the adjust clock activity returns
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_ADJUST_TIME) {
            if (data == null)
                return  // no change
            val playerColor = data.getStringExtra(AdjustClock.COLOR)
            if (color.toString().equals(playerColor)) {
                val newTime = data.getLongExtra(AdjustClock.NEW_TIME, 0)
                val timer = clockManager.timerForColor(color)
                timer.setNewTime(newTime)
            }
        }
    }

    class SimpleDrawerListener : DrawerLayout.SimpleDrawerListener() {
        var drawerIsDragging = false
        override fun onDrawerStateChanged(newState: Int) {
            super.onDrawerStateChanged(newState)
            // While swiping drawer, a touch event with x > leftSlopPx is passed
            // to button.  This is to catch that case.
            drawerIsDragging = (newState == 1)
        }
    }
}
