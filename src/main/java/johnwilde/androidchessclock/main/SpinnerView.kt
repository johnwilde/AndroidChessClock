package johnwilde.androidchessclock.main

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import android.util.AttributeSet
import timber.log.Timber

class SpinnerView @JvmOverloads constructor(
            context: Context,
            attrs: AttributeSet? = null,
            defStyleAttr: Int = 0
    ) : View(context, attrs, defStyleAttr) {

    private val mPaint: Paint = Paint()
    private val mOval: RectF = RectF(0f, 0f, 50f, 50f)
    var msSoFar: Long = 0
    var msTotal: Long = 0

    init {
        mPaint.isAntiAlias = true
        mPaint.style = Paint.Style.FILL
        mPaint.color = 0x88FF0000.toInt()
    }

    override fun onMeasure(widthMeasureSpec: Int,
                           heightMeasureSpec: Int) {
        setMeasuredDimension(50, 50)
    }

    override fun onDraw(canvas: Canvas) {
        val sweep = 360.0 * (1.0 - (msTotal - msSoFar).toFloat() / msTotal.toFloat())
        canvas.drawArc(mOval, 0f, sweep.toFloat(), true, mPaint)
    }
}