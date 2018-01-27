package johnwilde.androidchessclock.main

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import android.util.AttributeSet
import johnwilde.androidchessclock.R
import timber.log.Timber

class SpinnerView @JvmOverloads constructor(
            context: Context,
            attrs: AttributeSet? = null,
            defStyleAttr: Int = 0
    ) : View(context, attrs, defStyleAttr) {

    private val mOval: RectF
    private val mPaint: Paint = Paint()
    var msSoFar: Long = 0
    var msTotal: Long = 0

    init {
        mPaint.isAntiAlias = true
        mPaint.style = Paint.Style.FILL
        mPaint.color = Color.WHITE
        val dp = context.resources.getDimension(R.dimen.spinnerWidth)
        mOval = RectF(0f, 0f, dp, dp)
    }

    override fun onMeasure(widthMeasureSpec: Int,
                           heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val dp = context.resources.getDimensionPixelSize(R.dimen.spinnerWidth)
        setMeasuredDimension(dp, dp)
    }

    override fun onDraw(canvas: Canvas) {
        if (msTotal > 0) {
            val sweep = 360.0f * (1.0f - (msTotal - msSoFar).toFloat() / msTotal.toFloat())
            canvas.drawArc(mOval, 0f, sweep, true, mPaint)
        }
    }
}