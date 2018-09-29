package johnwilde.androidchessclock.clock

import android.animation.*
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.support.annotation.ColorInt
import android.util.FloatProperty

class FloodForeground(
    @ColorInt val color: Int,
    private val owner: FloodDrawable,
    var bounds: Rect,
    private var startX: Float,
    private var startY: Float
) {
    private val paint = Paint()
    private val targetRadius: Float
    private val radiusDuration: Long
    var hasFinished: Boolean = false

    // Values used to tween between the start and end positions.
    private var tweenRadius = 0f

    init {
        paint.color = color
        paint.alpha = (255 * 0.8).toInt()
        targetRadius = getTargetRadius(bounds)
        radiusDuration = 400 // ms
    }

    fun draw(canvas: Canvas?, flooded: Boolean) {
        if (!hasFinished) {
            val radius = getRadius()
            canvas?.drawCircle(0f, 0f, radius, paint)
        } else {
            if (flooded) {
                canvas?.drawColor(paint.color)
            }
        }
    }

    private fun getRadius(): Float {
        return lerp(0f, targetRadius, tweenRadius)
    }

    private val mAnimationListener = object : AnimatorListenerAdapter() {
        override fun onAnimationEnd(animator: Animator) {
            hasFinished = true
        }
    }

    private fun lerp(start: Float, stop: Float, amount: Float): Float {
        return start + (stop - start) * amount
    }

    private fun getTargetRadius(bounds: Rect): Float {
        val maxX = Math.max(startX, bounds.width() - startX)
        val maxY = Math.max(startY, bounds.height() - startY)
        return Math.sqrt((maxX * maxX + maxY * maxY).toDouble()).toFloat()
    }

    fun createFlood(): Animator {
        val tweenRadius = ObjectAnimator.ofFloat(this, TWEEN_RADIUS, 1f)
        tweenRadius.duration = radiusDuration
        tweenRadius.interpolator = DECELERATE_INTERPOLATOR

        val set = AnimatorSet()
        set.play(tweenRadius)
        set.addListener(mAnimationListener)

        return set
    }

    fun invalidateSelf() {
        owner.invalidateSelf()
    }

    /**
     * Interpolator with a smooth log deceleration.
     */
    private class LogDecelerateInterpolator(
        private val mBase: Float,
        timeScale: Float,
        private val mDrift: Float
    ) : TimeInterpolator {
        private val mTimeScale: Float
        private val mOutputScale: Float

        init {
            mTimeScale = 1f / timeScale

            mOutputScale = 1f / computeLog(1f)
        }

        private fun computeLog(t: Float): Float {
            return 1f - Math.pow(mBase.toDouble(), (-t * mTimeScale).toDouble()).toFloat() + mDrift * t
        }

        override fun getInterpolation(t: Float): Float {
            return computeLog(t) * mOutputScale
        }
    }

    companion object {
        private val DECELERATE_INTERPOLATOR = LogDecelerateInterpolator(
                400f, 1.4f, 0f)

        /**
         * Property for animating radius between its initial and target values.
         */
        private val TWEEN_RADIUS = object : FloatProperty<FloodForeground>("tweenRadius") {
            override fun setValue(o: FloodForeground, value: Float) {
                o.tweenRadius = value
                o.invalidateSelf()
            }

            override fun get(o: FloodForeground): Float? {
                return o.tweenRadius
            }
        }
    }
}