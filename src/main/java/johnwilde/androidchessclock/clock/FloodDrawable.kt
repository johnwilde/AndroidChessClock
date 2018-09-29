package johnwilde.androidchessclock.clock

import android.animation.Animator
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.support.annotation.ColorInt
import androidx.graphics.toRectF

class FloodDrawable(
    @ColorInt val color: Int,
    val radiusPx: Float
) : Drawable() {
    var flood: FloodForeground? = null
    var animator: Animator? = null
    var flooded = false
        set(setFlooded) {
            if (setFlooded && !flooded) {
                // show animation when setting to flooded
                startFlood()
            } else if (!setFlooded) {
                // cancel animation (if it's running) when clearing flood
                animator?.cancel()
            }
            field = setFlooded
            invalidateSelf()
        }
    var x: Float = 0f
    var y: Float = 0f

    override fun draw(canvas: Canvas?) {
        // Clip the canvas to match the round borders on background
        val p = Path()
        p.addRoundRect(bounds.toRectF(), radiusPx, radiusPx, Path.Direction.CCW)
        canvas?.clipPath(p)
        canvas?.translate(x, y)
        flood?.draw(canvas, flooded)
        canvas?.translate(-x, -y)
    }

    override fun setAlpha(alpha: Int) {
    }

    override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
    }

    fun hotspot(x: Float, y: Float) {
        this.x = x
        this.y = y
    }

    private fun startFlood() {
        flood = FloodForeground(color, this, bounds, x, y)
        animator = flood?.createFlood()
        animator?.start()
    }
}