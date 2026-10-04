package pynith.apps.nextel.games

import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import pynith.apps.nextel.R
import pynith.apps.nextel.games.widget.DiceView
import pynith.apps.nextel.views.BaseActivity
import kotlin.random.Random

/**
 * Classic Pig-style dice game against the computer: keep rolling to build
 * round points, hold to bank them — but rolling a 1 loses the round's points.
 * First to [TARGET_SCORE] wins.
 */
class DiceActivity : BaseActivity() {

    private val handler = Handler(Looper.getMainLooper())

    private lateinit var diceView: DiceView
    private lateinit var statusText: TextView
    private lateinit var playerScoreText: TextView
    private lateinit var cpuScoreText: TextView
    private lateinit var turnTotalText: TextView
    private lateinit var rollButton: MaterialButton
    private lateinit var holdButton: MaterialButton

    private var playerScore = 0
    private var cpuScore = 0
    private var turnTotal = 0
    private var playerTurn = true
    private var busy = false
    private var gameOver = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dice)

        findViewById<MaterialToolbar>(R.id.diceToolbar).setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        diceView = findViewById(R.id.diceView)
        statusText = findViewById(R.id.statusText)
        playerScoreText = findViewById(R.id.playerScoreText)
        cpuScoreText = findViewById(R.id.cpuScoreText)
        turnTotalText = findViewById(R.id.turnTotalText)
        rollButton = findViewById(R.id.rollButton)
        holdButton = findViewById(R.id.holdButton)

        rollButton.setOnClickListener { roll() }
        holdButton.setOnClickListener { hold() }
        findViewById<MaterialButton>(R.id.newGameButton).setOnClickListener { resetGame() }

        resetGame()
    }

    private fun resetGame() {
        handler.removeCallbacksAndMessages(null)
        playerScore = 0
        cpuScore = 0
        turnTotal = 0
        playerTurn = true
        busy = false
        gameOver = false
        diceView.value = 6
        updateScores()
        setStatus("Your turn — roll the dice. A 1 loses the round points!")
        renderControls()
    }

    private fun roll() {
        if (busy || gameOver || !playerTurn) return
        busy = true
        renderControls()
        animateRoll { value -> resolvePlayerRoll(value) }
    }

    private fun hold() {
        if (busy || gameOver || !playerTurn || turnTotal == 0) return
        playerScore += turnTotal
        turnTotal = 0
        updateScores()
        setStatus("You banked your points.")
        endPlayerTurn()
    }

    private fun animateRoll(onSettled: (Int) -> Unit) {
        val finalValue = Random.nextInt(1, 7)
        var ticks = 0
        val step = object : Runnable {
            override fun run() {
                ticks += 1
                if (ticks >= SHUFFLE_TICKS) {
                    diceView.value = finalValue
                    onSettled(finalValue)
                } else {
                    diceView.value = Random.nextInt(1, 7)
                    handler.postDelayed(this, SHUFFLE_INTERVAL)
                }
            }
        }
        handler.post(step)
    }

    private fun resolvePlayerRoll(value: Int) {
        if (value == 1) {
            turnTotal = 0
            updateScores()
            setStatus("You rolled a 1 — round points lost!")
            endPlayerTurn()
            return
        }

        turnTotal += value
        updateScores()

        if (playerScore + turnTotal >= TARGET_SCORE) {
            playerScore += turnTotal
            turnTotal = 0
            updateScores()
            win(playerWon = true)
            return
        }

        setStatus("You rolled a $value. Roll again or hold.")
        busy = false
        renderControls()
    }

    private fun endPlayerTurn() {
        busy = false
        playerTurn = false
        renderControls()
        handler.postDelayed({ cpuTurn() }, CPU_TURN_DELAY)
    }

    private fun cpuTurn() {
        if (gameOver) return
        animateRoll { value -> resolveCpuRoll(value) }
    }

    private fun resolveCpuRoll(value: Int) {
        if (value == 1) {
            turnTotal = 0
            updateScores()
            setStatus("The computer rolled a 1 — its round points are lost.")
            passToPlayer()
            return
        }

        turnTotal += value
        updateScores()

        if (cpuScore + turnTotal >= TARGET_SCORE) {
            cpuScore += turnTotal
            turnTotal = 0
            updateScores()
            win(playerWon = false)
            return
        }

        if (turnTotal >= CPU_HOLD_AT) {
            cpuScore += turnTotal
            turnTotal = 0
            updateScores()
            setStatus("The computer holds its points.")
            passToPlayer()
        } else {
            setStatus("The computer rolled a $value…")
            handler.postDelayed({ if (!gameOver) cpuTurn() }, CPU_ROLL_INTERVAL)
        }
    }

    private fun passToPlayer() {
        playerTurn = true
        setStatus("Your turn — roll the dice.")
        renderControls()
    }

    private fun win(playerWon: Boolean) {
        gameOver = true
        updateScores()
        renderControls()
        setStatus(
            if (playerWon) {
                "You reached $TARGET_SCORE points — you win!"
            } else {
                "The computer reached $TARGET_SCORE points."
            }
        )

        MaterialAlertDialogBuilder(this)
            .setTitle(if (playerWon) "You win! 🎉" else "Computer wins")
            .setMessage(
                if (playerWon) {
                    "You reached $TARGET_SCORE points first."
                } else {
                    "The computer got to $TARGET_SCORE points first. Try again?"
                }
            )
            .setPositiveButton("Play again") { _, _ -> resetGame() }
            .setNegativeButton("Close") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    private fun updateScores() {
        playerScoreText.text = playerScore.toString()
        cpuScoreText.text = cpuScore.toString()
        turnTotalText.text = "Round points: $turnTotal"
        playerScoreText.setTypeface(null, if (playerTurn) Typeface.BOLD else Typeface.NORMAL)
        cpuScoreText.setTypeface(null, if (!playerTurn) Typeface.BOLD else Typeface.NORMAL)
    }

    private fun renderControls() {
        rollButton.isEnabled = !busy && !gameOver && playerTurn
        holdButton.isEnabled = !busy && !gameOver && playerTurn && turnTotal > 0
    }

    private fun setStatus(message: String) {
        statusText.text = message
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    companion object {
        private const val TARGET_SCORE = 100
        private const val CPU_HOLD_AT = 20
        private const val SHUFFLE_TICKS = 6
        private const val SHUFFLE_INTERVAL = 70L
        private const val CPU_TURN_DELAY = 900L
        private const val CPU_ROLL_INTERVAL = 800L
    }
}
