package johnwilde.androidchessclock.clock

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.support.v4.content.res.ResourcesCompat
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
import java.util.concurrent.TimeUnit
import javax.inject.Inject


// This "View" represents one button and the TextView that shows the time remaining
class ClockFragment : MviFragment<ClockView, ClockViewPresenter>(), ClockView {
    lateinit var color : ClockView.Color
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
        val drawable = ResourcesCompat.getDrawable(
                resources,
                if (color == ClockView.Color.WHITE) R.drawable.white else R.drawable.black,
                null)
        button.setImageDrawable(drawable)
        if (Build.VERSION.SDK_INT >= 23) {
            button.foreground = FloodDrawable(Color.BLACK, resources.getDimension(R.dimen.buttonRadius))
        }
        clock.setOnClickListener { launchAdjustPlayerClockActivity() }
    }

    // Player tapped the button to end their turn
    override fun clickIntent(): Observable<Any> {
        return RxView.touches(button)
                .throttleFirst(100, TimeUnit.MILLISECONDS)
                .map {
                    if (Build.VERSION.SDK_INT >= 23) {
                        (button.foreground as FloodDrawable).hotspot(it.x, it.y)
                    }
                }
                .map { 1 }
    }

    // Update the button's visible viewState and the time text
    override fun render(viewState: ClockViewState) {
        renderButton(viewState.button)
        if (viewState.timeGap.show) {
            renderTimeGap(viewState.timeGap)
        } else {
            renderClock(viewState.time)
        }
        viewState.prompt?.let { renderSnackbar(it) }
        renderMoveCount(viewState.moveCount)
    }

    private fun renderTimeGap(viewState: ClockViewState.TimeGap) {
        clock.alpha = 1.0f
        clock.setTextColor(
                ResourcesCompat.getColorStateList(resources, R.color.time_gap_text_color, null))
        clock.text = Utils.formatTimeGap(viewState.msGap)
        clock.isChecked = viewState.msGap < 0
    }

    private fun renderButton(buttonState: ClockViewState.Button) {
        //TODO:
        // forward and backward controls for moves
        // crash (in hourglass?)
        // time input is awkward
        button.isSelected = buttonState.enabled
        if (Build.VERSION.SDK_INT >= 23) {
            (button.foreground as FloodDrawable).flooded = !buttonState.enabled
        }
        clock.isSelected = buttonState.enabled
    }

    private fun renderClock(time: ClockViewState.Time) {
        clock.setTextColor(
                ResourcesCompat.getColorStateList(resources, R.color.clock_text_color, null))
        clock.text = Utils.formatClockTime(time.msToGo)
        clock.isChecked = time.msToGo < 10_000
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
        // launch an activity through this intent (not while playing though)
        if (clockManager.stateHolder.gameState != GameStateHolder.GameState.FINISHED &&
             clockManager.stateHolder.gameState != GameStateHolder.GameState.PLAYING) {
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
}
