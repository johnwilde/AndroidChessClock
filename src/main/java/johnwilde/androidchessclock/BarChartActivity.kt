package johnwilde.androidchessclock

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView



class BarChartActivity : Activity() {
    companion object {
        private val WHITE_MS = "white_ms"
        private val BLACK_MS = "black_ms"

        @JvmStatic
        fun createIntent(context: Context, whiteMs: ArrayList<Long>, blackMs: ArrayList<Long>) : Intent {
            val intent = Intent(context, BarChartActivity::class.java)
            intent.putExtra(WHITE_MS, whiteMs)
            intent.putExtra(BLACK_MS, blackMs)
            return intent
        }
    }
    private var blackMs = ArrayList<Long>()
    private var whiteMs = ArrayList<Long>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("UNCHECKED_CAST")
        whiteMs = intent.getSerializableExtra(WHITE_MS) as ArrayList<Long>
        @Suppress("UNCHECKED_CAST")
        blackMs = intent.getSerializableExtra(BLACK_MS) as ArrayList<Long>
        if (whiteMs.size > blackMs.size) blackMs.add(0)
        if (blackMs.size > whiteMs.size) whiteMs.add(0)
        setContentView(R.layout.bar_chart_container)
    }

    override fun onResume() {
        super.onResume()
        val container = findViewById<LinearLayout>(R.id.container)
        container.removeAllViews()
        addViews(whiteMs, blackMs)
    }

    private fun addViews(p1: ArrayList<Long>, p2: ArrayList<Long>) {
        val container = findViewById<LinearLayout>(R.id.container)
        val maxValue = Math.max(p1.max()?:0, p2.max()?:0).toFloat()
        for (i in 0..p1.lastIndex) {
            val twoBar = layoutInflater.inflate(R.layout.two_bar, null)
            val twoBarContainer = twoBar.findViewById<LinearLayout>(R.id.two_bar_container)
            twoBarContainer.addView(
                    addBar(p1[i].toFloat() / maxValue,
                            Color.WHITE,
                            Utils.formatTime(p1[i])))
            twoBarContainer.addView(
                    addBar(p2[i].toFloat() / maxValue,
                            Color.BLACK,
                            Utils.formatTime(p2[i])))
            twoBar.findViewById<TextView>(R.id.move_count).setText((i + 1).toString())
            container.addView(twoBar)
        }
    }

    private fun addBar(percent: Float, colorInt: Int, time: String): View? {
        val barContainer = layoutInflater.inflate(R.layout.bar_and_time, null)
        val padding = barContainer.findViewById<View>(R.id.padding)
        val bar = barContainer.findViewById<View>(R.id.bar)
        bar.findViewById<TextView>(R.id.bar_text).text = time
        bar.setBackgroundColor(colorInt)
        setWeight(padding, (1 - percent))
        setWeight(bar, percent)
        return barContainer
    }

    private fun setWeight(v: View, w: Float) {
        // Assigning a weight of 0 breaks layout for some reason
        val w1 = if (w == 0f) 0.01f else w
        val p1: LinearLayout.LayoutParams = v.layoutParams as LinearLayout.LayoutParams
        p1.weight = w1;
    }
}
