package pynith.apps.nextel.games.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

/**
 * A square dice face that draws classic pips for values 1..6. Shared by the
 * Ludo and Classic Dice games.
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

    private val facePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = accentColor
    }
    private val pipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentColor }
    private val faceRect = RectF()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val size = min(width, height).toFloat()
        if (size <= 0f) return

        val inset = size * 0.06f
        val corner = size * 0.18f
        faceRect.set(inset, inset, size - inset, size - inset)

        facePaint.color = Color.WHITE
        canvas.drawRoundRect(faceRect, corner, corner, facePaint)

        borderPaint.strokeWidth = size * 0.05f
        borderPaint.color = accentColor
        canvas.drawRoundRect(faceRect, corner, corner, borderPaint)

        pipPaint.color = accentColor

        val center = size / 2f
        val pipRadius = size * 0.08f
        val spread = size * 0.24f
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
