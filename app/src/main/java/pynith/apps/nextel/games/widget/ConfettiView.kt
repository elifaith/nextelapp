package pynith.apps.nextel.games.widget

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Lightweight confetti burst (stands in for the Flutter confetti package).
 * Call [burst] to launch a salvo from the top corners.
 */
class ConfettiView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private class Piece(
        var x: Float,
        var y: Float,
        val vx: Float,
        val vy: Float,
        val color: Int,
        val size: Float,
        var rotation: Float,
        val rotationSpeed: Float,
        val wobble: Float
    )

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val pieces = mutableListOf<Piece>()
    private var elapsed = 0f

    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = DURATION
        interpolator = LinearInterpolator()
        addUpdateListener {
            if (pieces.isEmpty()) return@addUpdateListener
            elapsed += FRAME_MS / DURATION.toFloat()
            for (piece in pieces) {
                piece.x += piece.vx * FRAME_MS / 1000f
                piece.y += piece.vy * FRAME_MS / 1000f
                piece.vy += GRAVITY * FRAME_MS / 1000f
                piece.rotation += piece.rotationSpeed * FRAME_MS / 1000f
            }
            pieces.removeAll { it.y > height + 40f }
            if (pieces.isEmpty()) {
                it.cancel()
            }
            invalidate()
        }
    }

    fun burst() {
        if (width == 0 || height == 0) return
        repeat(PIECE_COUNT) {
            val fromLeft = it % 2 == 0
            val angle = if (fromLeft) {
                Math.toRadians(30.0 + Random.nextDouble() * 40.0)
            } else {
                Math.toRadians(110.0 + Random.nextDouble() * 40.0)
            }
            val speed = (width * 0.35f..width * 0.65f).random()
            pieces += Piece(
                x = if (fromLeft) 0f else width.toFloat(),
                y = height * 0.15f,
                vx = (speed * cos(angle)).toFloat(),
                vy = (speed * sin(angle)).toFloat() - height * 0.10f,
                color = COLORS[Random.nextInt(COLORS.size)],
                size = 8f + Random.nextFloat() * 8f,
                rotation = Random.nextFloat() * 360f,
                rotationSpeed = 180f + Random.nextFloat() * 420f,
                wobble = 6f + Random.nextFloat() * 10f
            )
        }
        if (!animator.isStarted) {
            animator.start()
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (piece in pieces) {
            paint.color = piece.color
            canvas.save()
            canvas.rotate(piece.rotation, piece.x, piece.y)
            rect.set(
                piece.x - piece.size / 2f,
                piece.y - piece.wobble / 2f,
                piece.x + piece.size / 2f,
                piece.y + piece.wobble / 2f
            )
            canvas.drawRoundRect(rect, 2f, 2f, paint)
            canvas.restore()
        }
    }

    override fun onDetachedFromWindow() {
        animator.cancel()
        super.onDetachedFromWindow()
    }

    companion object {
        private const val DURATION = 2600L
        private const val FRAME_MS = 16f
        private const val GRAVITY = 420f
        private const val PIECE_COUNT = 90
        private val COLORS = intArrayOf(
            0xFFF44336.toInt(), 0xFFE91E63.toInt(), 0xFFFF9800.toInt(),
            0xFFFFEB3B.toInt(), 0xFF4CAF50.toInt(), 0xFF2196F3.toInt(),
            0xFF9C27B0.toInt()
        )
    }
}
