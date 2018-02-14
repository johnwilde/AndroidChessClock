package johnwilde.androidchessclock

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import dagger.android.AndroidInjection
import johnwilde.androidchessclock.clock.ClockView
import johnwilde.androidchessclock.logic.ClockManager
import kotlinx.android.synthetic.main.bar_and_time.view.*
import kotlinx.android.synthetic.main.bar_chart_container.*
import kotlinx.android.synthetic.main.two_bar.view.*
import javax.inject.Inject

class BarChartActivity : Activity() {
    companion object {
        @JvmStatic
        fun createIntent(context: Context) : Intent {
            val intent = Intent(context, BarChartActivity::class.java)
            return intent
        }
    }

    @Inject lateinit var clockManager : ClockManager
    private lateinit var blackMs : LongArray
    private lateinit var whiteMs : LongArray

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.bar_chart_container)
    }

    override fun onResume() {
        super.onResume()
        bar_chart_container.removeAllViews()
        whiteMs = clockManager.timerForColor(ClockView.Color.WHITE).moveTimes
        blackMs = clockManager.timerForColor(ClockView.Color.BLACK).moveTimes
        addViews(whiteMs, blackMs)
    }

    private fun addViews(whiteMoves: LongArray, blackMoves: LongArray) {
        val maxValue = Math.max(whiteMoves.max()?:0, blackMoves.max()?:0).toFloat()
        for (i in 0..whiteMoves.lastIndex) {
            val bars = layoutInflater.inflate(R.layout.two_bar, null)
            bars.two_bar_container.addView(
                    addBar(whiteMoves[i].toFloat() / maxValue,
                            Color.WHITE,
                            Utils.formatClockTime(whiteMoves[i])))

            // check size of black's list, it might be less than white
            if (i <= blackMoves.lastIndex) {
                bars.two_bar_container.addView(
                        addBar(blackMoves[i].toFloat() / maxValue,
                                Color.BLACK,
                                Utils.formatClockTime(blackMoves[i])))
            }
            bars.move_count.text = ((i + 1).toString())
            bar_chart_container.addView(bars)
        }
    }

    private fun addBar(percent: Float, colorInt: Int, time: String): View? {
        val view = layoutInflater.inflate(R.layout.bar_and_time, null)
        view.bar_text.text = time
        view.bar.setBackgroundColor(colorInt)
        setWeight(view.padding, (1 - percent))
        setWeight(view.bar, percent)
        return view
    }

    private fun setWeight(v: View, w: Float) {
        // Assigning a weight of 0 breaks layout for some reason
        val w1 = if (w == 0f) 0.01f else w
        val p1: LinearLayout.LayoutParams = v.layoutParams as LinearLayout.LayoutParams
        p1.weight = w1;
    }
}
