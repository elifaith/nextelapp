package pynith.apps.nextel.games.hangman

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

/**
 * Draws the Hangman gallows for states 0..6 (the Flutter module used the
 * images h_0.png .. h_6.png): the gallows itself plus one more body part per
 * wrong guess — head, body, left arm, right arm, left leg, right leg.
 */
class HangmanView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    /** 0 = empty gallows … 6 = complete figure (lost life). */
    var state: Int = 0
        set(value) {
            field = value.coerceIn(0, 6)
            invalidate()
        }

    private val woodPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#8D6E63")
        strokeWidth = 10f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val ropePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#BCAAA4")
        strokeWidth = 6f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val facePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val size = min(width, height).toFloat()
        if (size <= 0f) return
        bodyPaint.strokeWidth = size * 0.035f

        val unit = size / 100f
        val left = (width - size) / 2f
        val top = (height - size) / 2f

        fun px(x: Float) = left + x * unit
        fun py(y: Float) = top + y * unit

        // Gallows.
        woodPaint.strokeWidth = 7f * unit
        canvas.drawLine(px(12f), py(92f), px(40f), py(92f), woodPaint)   // base
        canvas.drawLine(px(26f), py(92f), px(26f), py(10f), woodPaint)   // pole
        canvas.drawLine(px(26f), py(10f), px(66f), py(10f), woodPaint)   // beam
        canvas.drawLine(px(26f), py(20f), px(36f), py(10f), woodPaint)   // brace
        ropePaint.strokeWidth = 4f * unit
        canvas.drawLine(px(66f), py(10f), px(66f), py(22f), ropePaint)   // rope

        if (state >= 1) {
            // Head.
            bodyPaint.style = Paint.Style.STROKE
            canvas.drawCircle(px(66f), py(30f), 8f * unit, bodyPaint)
            if (state >= 6) {
                // Dead face: x eyes.
                facePaint.style = Paint.Style.STROKE
                facePaint.strokeWidth = 1.8f * unit
                canvas.drawLine(px(62.5f), py(28f), px(65f), py(30.5f), facePaint)
                canvas.drawLine(px(65f), py(28f), px(62.5f), py(30.5f), facePaint)
                canvas.drawLine(px(67f), py(28f), px(69.5f), py(30.5f), facePaint)
                canvas.drawLine(px(69.5f), py(28f), px(67f), py(30.5f), facePaint)
                canvas.drawLine(px(62.5f), py(34f), px(69.5f), py(34f), facePaint)
            }
        }
        if (state >= 2) {
            // Body.
            canvas.drawLine(px(66f), py(38f), px(66f), py(62f), bodyPaint)
        }
        if (state >= 3) {
            // Left arm.
            canvas.drawLine(px(66f), py(44f), px(54f), py(54f), bodyPaint)
        }
        if (state >= 4) {
            // Right arm.
            canvas.drawLine(px(66f), py(44f), px(78f), py(54f), bodyPaint)
        }
        if (state >= 5) {
            // Left leg.
            canvas.drawLine(px(66f), py(62f), px(56f), py(80f), bodyPaint)
        }
        if (state >= 6) {
            // Right leg.
            canvas.drawLine(px(66f), py(62f), px(76f), py(80f), bodyPaint)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val size = min(measuredWidth, measuredHeight)
        if (size > 0) {
            setMeasuredDimension(size, size)
        }
    }
}
