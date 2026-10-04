package pynith.apps.nextel.games.ludo

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.min

/**
 * Renders a classic 15x15 Ludo board from [LudoGame] state: yards, track,
 * home columns, center triangles and tokens. The tokens the human may move
 * are highlighted, taps on them are reported through [onTokenPicked], and
 * moves are played as a short hop animation.
 */
class LudoBoardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    /** Invoked with the token index when the player taps one of the highlighted tokens. */
    var onTokenPicked: ((tokenIndex: Int) -> Unit)? = null

    private var game: LudoGame? = null
    private var highlighted: Set<Int> = emptySet()

    private val boardPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val cellBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
        color = Color.parseColor("#C9D4CE")
    }
    private val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        color = Color.parseColor("#8FA39A")
    }
    private val yardStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        color = Color.WHITE
    }
    private val tokenPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tokenStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = Color.WHITE
    }
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        color = Color.parseColor("#FFB300")
    }
    private val cellRect = RectF()
    private val centerPath = Path()

    // Geometry, recomputed in onSizeChanged.
    private var cell = 0f
    private var originX = 0f
    private var originY = 0f

    private class AnimState(
        val owner: Int,
        val tokenIndex: Int,
        val points: List<Pair<Float, Float>>,
        var x: Float,
        var y: Float
    )

    private var anim: AnimState? = null
    private var animator: ValueAnimator? = null
    private var suppressAnimEnd = false

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        cell = min(w, h) / 15f
        originX = (w - cell * 15f) / 2f
        originY = (h - cell * 15f) / 2f
    }

    fun setState(game: LudoGame, highlighted: Set<Int>) {
        this.game = game
        this.highlighted = highlighted
        invalidate()
    }

    /** Pixel center of a track/home-column cell. */
    fun centerOf(col: Int, row: Int): Pair<Float, Float> =
        (originX + (col + 0.5f) * cell) to (originY + (row + 0.5f) * cell)

    /**
     * Pixel position of a token at a given step (yard slots and home slots
     * included), without stacking offsets.
     */
    fun positionForStep(owner: Int, tokenIndex: Int, step: Int): Pair<Float, Float> {
        val g = game ?: return originX to originY
        return when {
            step == -1 -> centerOfCellUnits(YARD_SLOTS[owner][tokenIndex])
            step <= 50 -> {
                val (col, row) = g.trackCell(owner, step) ?: return centerOfCellUnits(YARD_SLOTS[owner][tokenIndex])
                centerOf(col, row)
            }
            step <= 55 -> {
                val (col, row) = g.players[owner].homeColumn[step - 51]
                centerOf(col, row)
            }
            else -> centerOfCellUnits(HOME_SLOTS[owner][tokenIndex])
        }
    }

    fun tokenPixelPosition(owner: Int, tokenIndex: Int): Pair<Float, Float> =
        positionForStep(owner, tokenIndex, game?.tokenStep(owner, tokenIndex) ?: -1)

    /**
     * Animates one token from [startPoint] along [waypoints] (pixel centers,
     * excluding the start) and reports completion on the main thread. The
     * start position is passed in explicitly because the game state already
     * holds the destination step by the time this runs.
     */
    fun animateToken(
        owner: Int,
        tokenIndex: Int,
        startPoint: Pair<Float, Float>,
        waypoints: List<Pair<Float, Float>>,
        onDone: () -> Unit
    ) {
        val points = mutableListOf(startPoint)
        points += waypoints

        anim = AnimState(owner, tokenIndex, points, startPoint.first, startPoint.second)

        // Cancel any running animation without triggering its completion callback.
        animator?.let {
            suppressAnimEnd = true
            it.cancel()
        }
        suppressAnimEnd = false
        val duration = (points.size * 130L).coerceIn(180L, 1400L)
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            setDuration(duration)
            interpolator = LinearInterpolator()
            addUpdateListener { animation ->
                val state = anim ?: return@addUpdateListener
                val fraction = animation.animatedValue as Float
                val segments = (state.points.size - 1).coerceAtLeast(1)
                val position = fraction * segments
                val index = position.toInt().coerceAtMost(segments - 1)
                val local = position - index
                val (x1, y1) = state.points[index]
                val (x2, y2) = state.points[index + 1]
                state.x = x1 + (x2 - x1) * local
                state.y = y1 + (y2 - y1) * local
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (suppressAnimEnd) {
                        suppressAnimEnd = false
                        return
                    }
                    anim = null
                    invalidate()
                    onDone()
                }
            })
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val g = game ?: return
        if (cell <= 0f) return

        drawBoardBase(canvas, g)
        drawTokens(canvas, g)

        // Highlight movable human tokens.
        for (tokenIndex in highlighted) {
            val (x, y) = tokenPixelPosition(0, tokenIndex)
            highlightPaint.strokeWidth = cell * 0.1f
            canvas.drawCircle(x, y, cell * 0.46f, highlightPaint)
        }

        // The token currently being animated rides on top.
        anim?.let { state ->
            drawTokenCircle(
                canvas,
                state.x,
                state.y,
                PLAYER_COLORS[state.owner],
                cell * 0.34f
            )
        }
    }

    private fun drawBoardBase(canvas: Canvas, g: LudoGame) {
        // Board plate.
        boardPaint.color = Color.WHITE
        canvas.drawRoundRect(
            originX + cell * 0.2f,
            originY + cell * 0.2f,
            originX + cell * 14.8f,
            originY + cell * 14.8f,
            cell * 0.6f,
            cell * 0.6f,
            boardPaint
        )

        // Main track cells.
        for (index in LudoBoard.TRACK.indices) {
            val (col, row) = LudoBoard.TRACK[index]
            cellRect.set(
                originX + col * cell,
                originY + row * cell,
                originX + (col + 1) * cell,
                originY + (row + 1) * cell
            )
            cellPaint.color = when {
                START_CELL_OWNER.containsKey(index) -> PLAYER_COLORS[START_CELL_OWNER.getValue(index)]
                index in LudoBoard.SAFE_TRACK_INDEXES -> Color.parseColor("#EDF2EF")
                else -> Color.WHITE
            }
            canvas.drawRect(cellRect, cellPaint)
            canvas.drawRect(cellRect, cellBorderPaint)

            if (index in LudoBoard.SAFE_TRACK_INDEXES && !START_CELL_OWNER.containsKey(index)) {
                val (cx, cy) = centerOf(col, row)
                starPaint.textSize = cell * 0.55f
                canvas.drawText(STAR, cx, cy + cell * 0.2f, starPaint)
            }
        }

        // Home columns.
        for (player in 0 until 4) {
            for ((col, row) in g.players[player].homeColumn) {
                cellRect.set(
                    originX + col * cell,
                    originY + row * cell,
                    originX + (col + 1) * cell,
                    originY + (row + 1) * cell
                )
                cellPaint.color = PLAYER_COLORS[player]
                canvas.drawRect(cellRect, cellPaint)
                canvas.drawRect(cellRect, cellBorderPaint)
            }
        }

        // Center home triangles: red bottom, green left, yellow top, blue right.
        val left = originX + 6 * cell
        val top = originY + 6 * cell
        val right = originX + 9 * cell
        val bottom = originY + 9 * cell
        val midX = originX + 7.5f * cell
        val midY = originY + 7.5f * cell
        drawTriangle(canvas, left, top, left, bottom, midX, midY, PLAYER_COLORS[1])   // green left
        drawTriangle(canvas, left, top, right, top, midX, midY, PLAYER_COLORS[2])     // yellow top
        drawTriangle(canvas, right, top, right, bottom, midX, midY, PLAYER_COLORS[3]) // blue right
        drawTriangle(canvas, left, bottom, right, bottom, midX, midY, PLAYER_COLORS[0]) // red bottom

        // Yards.
        for (player in 0 until 4) {
            drawYard(canvas, player, g)
        }
    }

    private fun drawTriangle(
        canvas: Canvas,
        x1: Float, y1: Float,
        x2: Float, y2: Float,
        x3: Float, y3: Float,
        color: Int
    ) {
        centerPath.reset()
        centerPath.moveTo(x1, y1)
        centerPath.lineTo(x2, y2)
        centerPath.lineTo(x3, y3)
        centerPath.close()
        cellPaint.color = color
        canvas.drawPath(centerPath, cellPaint)
        canvas.drawPath(centerPath, cellBorderPaint)
    }

    private fun drawYard(canvas: Canvas, player: Int, g: LudoGame) {
        val (startCol, startRow) = when (player) {
            0 -> 0 to 9    // red, bottom-left
            1 -> 0 to 0    // green, top-left
            2 -> 9 to 0    // yellow, top-right
            else -> 9 to 9 // blue, bottom-right
        }
        val inset = cell * 0.35f
        val left = originX + startCol * cell + inset
        val top = originY + startRow * cell + inset
        val right = originX + (startCol + 6) * cell - inset
        val bottom = originY + (startRow + 6) * cell - inset
        val radius = cell * 1.4f

        cellPaint.color = PLAYER_COLORS[player]
        canvas.drawRoundRect(left, top, right, bottom, radius, radius, cellPaint)

        // The current player's yard gets a bright outline.
        if (g.currentPlayer == player) {
            yardStrokePaint.strokeWidth = cell * 0.16f
            yardStrokePaint.color = if (g.players[player].human) {
                Color.parseColor("#FFB300")
            } else {
                Color.WHITE
            }
            canvas.drawRoundRect(left, top, right, bottom, radius, radius, yardStrokePaint)
        }

        // Inner white panel + four parking slots.
        val inner = cell * 0.9f
        boardPaint.color = Color.WHITE
        canvas.drawRoundRect(
            left + inner, top + inner, right - inner, bottom - inner,
            radius * 0.7f, radius * 0.7f, boardPaint
        )
        for (slot in YARD_SLOTS[player]) {
            val (cx, cy) = centerOfCellUnits(slot)
            tokenPaint.color = PLAYER_COLORS[player]
            canvas.drawCircle(cx, cy, cell * 0.36f, tokenPaint)
            tokenPaint.color = Color.WHITE
            canvas.drawCircle(cx, cy, cell * 0.26f, tokenPaint)
        }
    }

    private fun drawTokens(canvas: Canvas, g: LudoGame) {
        // Compute all resting positions, then fan out tokens sharing a cell.
        data class Placed(val owner: Int, val index: Int, val x: Float, val y: Float)

        val placed = mutableListOf<Placed>()
        for (owner in 0 until 4) {
            for (index in 0 until 4) {
                if (anim != null && anim!!.owner == owner && anim!!.tokenIndex == index) continue
                val step = g.tokenStep(owner, index)
                if (step == 56) {
                    // Home tokens are drawn small inside their center triangle.
                    val (x, y) = centerOfCellUnits(HOME_SLOTS[owner][index])
                    drawTokenCircle(canvas, x, y, PLAYER_COLORS[owner], cell * 0.17f)
                    continue
                }
                val (x, y) = positionForStep(owner, index, step)
                placed += Placed(owner, index, x, y)
            }
        }

        val groups = placed.groupBy { it.x to it.y }
        for (group in groups.values) {
            group.forEachIndexed { positionInGroup, token ->
                val offset = (positionInGroup - (group.size - 1) / 2f) * cell * 0.22f
                drawTokenCircle(canvas, token.x + offset, token.y, PLAYER_COLORS[token.owner], cell * 0.32f)
            }
        }
    }

    private fun drawTokenCircle(canvas: Canvas, x: Float, y: Float, color: Int, radius: Float) {
        tokenPaint.color = color
        canvas.drawCircle(x, y, radius, tokenPaint)
        tokenStrokePaint.strokeWidth = radius * 0.22f
        canvas.drawCircle(x, y, radius * 0.92f, tokenStrokePaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_DOWN) {
            return super.onTouchEvent(event)
        }

        var bestToken = -1
        var bestDistance = Float.MAX_VALUE
        for (tokenIndex in highlighted) {
            val (x, y) = tokenPixelPosition(0, tokenIndex)
            val distance = Math.hypot((event.x - x).toDouble(), (event.y - y).toDouble()).toFloat()
            if (distance < cell * 0.9f && distance < bestDistance) {
                bestDistance = distance
                bestToken = tokenIndex
            }
        }

        if (bestToken >= 0) {
            performClick()
            onTokenPicked?.invoke(bestToken)
            return true
        }
        return false
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onDetachedFromWindow() {
        suppressAnimEnd = true
        animator?.cancel()
        animator = null
        anim = null
        super.onDetachedFromWindow()
    }

    private fun centerOfCellUnits(center: Pair<Float, Float>): Pair<Float, Float> =
        (originX + center.first * cell) to (originY + center.second * cell)

    companion object {
        const val STAR = "★"

        /** Player colors: red, green, yellow, blue (matches [LudoGame] player order). */
        val PLAYER_COLORS = intArrayOf(
            Color.parseColor("#E53935"), // red   — You (bottom-left)
            Color.parseColor("#43A047"), // green — CPU (top-left)
            Color.parseColor("#FBC02D"), // yellow— CPU (top-right)
            Color.parseColor("#1E88E5")  // blue  — CPU (bottom-right)
        )

        /** Which player owns each colored start cell on the track. */
        private val START_CELL_OWNER: Map<Int, Int> = mapOf(
            39 to 0, // red start
            0 to 1,  // green start
            13 to 2, // yellow start
            26 to 3  // blue start
        )

        /** Yard parking slots as (x, y) centers in cell units, one per token. */
        private val YARD_SLOTS: List<List<Pair<Float, Float>>> = listOf(
            listOf(2.1f to 11.1f, 3.9f to 11.1f, 2.1f to 12.9f, 3.9f to 12.9f), // red
            listOf(2.1f to 2.1f, 3.9f to 2.1f, 2.1f to 3.9f, 3.9f to 3.9f),     // green
            listOf(11.1f to 2.1f, 12.9f to 2.1f, 11.1f to 3.9f, 12.9f to 3.9f), // yellow
            listOf(11.1f to 11.1f, 12.9f to 11.1f, 11.1f to 12.9f, 12.9f to 12.9f) // blue
        )

        /** Resting spots for finished tokens inside the center, in cell units. */
        private val HOME_SLOTS: List<List<Pair<Float, Float>>> = listOf(
            listOf(6.85f to 8.32f, 7.28f to 8.32f, 7.72f to 8.32f, 8.15f to 8.32f), // red bottom
            listOf(6.68f to 6.85f, 6.68f to 7.28f, 6.68f to 7.72f, 6.68f to 8.15f), // green left
            listOf(6.85f to 6.68f, 7.28f to 6.68f, 7.72f to 6.68f, 8.15f to 6.68f), // yellow top
            listOf(8.32f to 6.85f, 8.32f to 7.28f, 8.32f to 7.72f, 8.32f to 8.15f)  // blue right
        )
    }
}
