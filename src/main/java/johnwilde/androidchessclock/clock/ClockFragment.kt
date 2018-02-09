package johnwilde.androidchessclock.clock

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.hannesdorfmann.mosby3.mvi.MviFragment
import com.jakewharton.rxbinding2.view.RxView
import dagger.android.support.AndroidSupportInjection
import io.reactivex.Observable
import johnwilde.androidchessclock.AdjustClock
import johnwilde.androidchessclock.R
import johnwilde.androidchessclock.Utils
import johnwilde.androidchessclock.logic.ClockManager
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
        button.setImageResource(if (color == ClockView.Color.WHITE) R.drawable.white else R.drawable.black)
        clock.setOnClickListener({ launchAdjustPlayerClockActivity() })
    }

    // Player tapped the button to end their turn
    override fun clickIntent(): Observable<MotionEvent> {
        return RxView.touches(button)
    }

    // Update the button's enabled state and the time text
    override fun render(viewState: ClockViewState) {
        val state = viewState as FullViewState
        renderClock(state.button)
        renderTimeGap(state.timeGap)
        if (state.prompt != null) renderPromptToMove(state.prompt)
    }

    private fun renderTimeGap(viewState: TimeGapViewState) {
        timeGap.visibility = if (viewState.show) {
            View.VISIBLE
        } else {
            View.GONE
        }
        timeGap.text = Utils.formatTimeGap(viewState.msGap)
        timeGap.isChecked = viewState.msGap < 0
    }

    private fun renderPromptToMove(viewState: PromptToMove) {
        Toast.makeText(activity!!.applicationContext, R.string.tap_other_button, Toast.LENGTH_SHORT)
                .show()
    }

    private fun renderClock(buttonViewState: ButtonViewState) {
        clock.text = Utils.formatClockTime(buttonViewState.msToGo)
        clock.alpha = if (buttonViewState.enabled) 1.0f else .1f
        clock.isChecked = buttonViewState.msToGo < 10_000
        button.drawable.alpha = if (buttonViewState.enabled) BUTTON_VISIBLE else BUTTON_FADED
        moveCount.text = buttonViewState.moveCount
        moveCount.visibility = if (moveCount.text.isBlank()) View.INVISIBLE else View.VISIBLE
    }

    private fun launchAdjustPlayerClockActivity() {
        // launch an activity through this intent
        val launchAdjustPlayerClockIntent = Intent().setClass(activity, AdjustClock::class.java)
        launchAdjustPlayerClockIntent.putExtra(AdjustClock.EXTRA_COLOR, color.toString())
        launchAdjustPlayerClockIntent.putExtra(AdjustClock.EXTRA_TIME, clockManager.forColor(color).msToGo)
        clockManager.pause()
        startActivityForResult(launchAdjustPlayerClockIntent, REQUEST_CODE_ADJUST_TIME)
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
                val timer = clockManager.forColor(color)
                timer.setNewTime(newTime)
            }
        }
    }

}
