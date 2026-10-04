package pynith.apps.nextel.games.widget

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.min
import kotlin.math.sin

/**
 * A square dice face that draws classic pips for values 1..6, shared by the
 * Ludo and Dice games. [highlight] draws a pulsing ring around the dice (the
 * Flutter module showed a ripple while waiting for a roll).
 */
class DiceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var value: Int = 6
        set(value) {
            field = value.coerceIn(1, 6)
            invalidate()
        }

    /** Accent color used for the border and the pips. */
    var accentColor: Int = Color.parseColor("#198754")
        set(value) {
            field = value
            invalidate()
        }

    /** Shows a pulsing ring around the dice (e.g. "tap to roll"). */
    var highlight: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            if (value) {
                pulseStart = System.currentTimeMillis()
                pulseAnimator.start()
            } else {
                pulseAnimator.cancel()
            }
            invalidate()
        }

    private val facePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val pipPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val faceRect = RectF()

    private var pulseStart = 0L
    private val pulseAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 900
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener { invalidate() }
    }

    override fun onDetachedFromWindow() {
        pulseAnimator.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val size = min(width, height).toFloat()
        if (size <= 0f) return

        if (highlight) {
            val phase = ((System.currentTimeMillis() - pulseStart) % 900) / 900f
            val pulse = 0.5f + 0.5f * sin((phase * 2 * Math.PI).toFloat())
            ringPaint.color = accentColor
            ringPaint.alpha = (140 + 90 * pulse).toInt().coerceAtMost(255)
            ringPaint.strokeWidth = size * (0.03f + 0.02f * pulse)
            canvas.drawCircle(size / 2f, size / 2f, size * (0.52f + 0.05f * pulse), ringPaint)
        }

        val inset = size * 0.07f
        val corner = size * 0.18f
        faceRect.set(inset, inset, size - inset, size - inset)

        facePaint.color = Color.WHITE
        canvas.drawRoundRect(faceRect, corner, corner, facePaint)

        borderPaint.strokeWidth = size * 0.045f
        borderPaint.color = accentColor
        canvas.drawRoundRect(faceRect, corner, corner, borderPaint)

        pipPaint.color = accentColor

        val center = size / 2f
        val pipRadius = size * 0.075f
        val spread = size * 0.22f
        for ((dx, dy) in PIP_POSITIONS.getValue(value)) {
            canvas.drawCircle(center + dx * spread, center + dy * spread, pipRadius, pipPaint)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val size = min(measuredWidth, measuredHeight)
        if (size > 0) {
            setMeasuredDimension(size, size)
        }
    }

    companion object {
        /** Pip offsets on a 3x3 grid: -1 = top/left, 0 = center, 1 = bottom/right. */
        private val PIP_POSITIONS: Map<Int, List<Pair<Float, Float>>> = mapOf(
            1 to listOf(0f to 0f),
            2 to listOf(-1f to -1f, 1f to 1f),
            3 to listOf(-1f to -1f, 0f to 0f, 1f to 1f),
            4 to listOf(-1f to -1f, 1f to -1f, -1f to 1f, 1f to 1f),
            5 to listOf(-1f to -1f, 1f to -1f, 0f to 0f, -1f to 1f, 1f to 1f),
            6 to listOf(-1f to -1f, 1f to -1f, -1f to 0f, 1f to 0f, -1f to 1f, 1f to 1f)
        )
    }
}
