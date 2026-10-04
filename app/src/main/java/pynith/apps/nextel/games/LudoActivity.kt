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
import pynith.apps.nextel.games.ludo.LudoBoardView
import pynith.apps.nextel.games.ludo.LudoGame
import pynith.apps.nextel.games.widget.DiceView
import pynith.apps.nextel.views.BaseActivity

/**
 * Full classic Ludo game: the human (red, bottom-left) against three computer
 * players. Rules and board geometry live in [LudoGame]; rendering and hop
 * animation live in [LudoBoardView].
 */
class LudoActivity : BaseActivity() {

    private val handler = Handler(Looper.getMainLooper())

    private lateinit var game: LudoGame
    private lateinit var boardView: LudoBoardView
    private lateinit var diceView: DiceView
    private lateinit var statusText: TextView
    private lateinit var rollButton: MaterialButton
    private val chips = mutableListOf<TextView>()

    private var awaitingPick = false
    private var animating = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ludo)

        findViewById<MaterialToolbar>(R.id.ludoToolbar).setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        boardView = findViewById(R.id.boardView)
        diceView = findViewById(R.id.diceView)
        statusText = findViewById(R.id.statusText)
        rollButton = findViewById(R.id.rollButton)
        chips.add(findViewById(R.id.chip0))
        chips.add(findViewById(R.id.chip1))
        chips.add(findViewById(R.id.chip2))
        chips.add(findViewById(R.id.chip3))

        rollButton.setOnClickListener { humanRoll() }
        findViewById<MaterialButton>(R.id.restartButton).setOnClickListener { confirmRestart() }
        boardView.onTokenPicked = { tokenIndex -> humanPick(tokenIndex) }

        newGame()
    }

    private fun newGame() {
        handler.removeCallbacksAndMessages(null)
        game = LudoGame()
        awaitingPick = false
        animating = false
        diceView.accentColor = LudoBoardView.PLAYER_COLORS[0]
        updateUi(highlight = emptySet())
        setStatus("Your turn — roll the dice. A six brings a token out of the yard.")
    }

    private fun updateUi(highlight: Set<Int>) {
        boardView.setState(game, highlight)

        for (player in 0 until 4) {
            val chip = chips[player]
            chip.text = "● ${game.players[player].name} ${game.homeCount(player)}/4"
            chip.setTextColor(
                if (game.currentPlayer == player) {
                    LudoBoardView.PLAYER_COLORS[player]
                } else {
                    MUTED_CHIP_COLOR
                }
            )
            chip.setTypeface(null, if (game.currentPlayer == player) Typeface.BOLD else Typeface.NORMAL)
        }

        val humanTurn = game.players[game.currentPlayer].human
        rollButton.isEnabled = !game.isGameOver && humanTurn && !animating && !awaitingPick
    }

    private fun setStatus(message: String) {
        statusText.text = message
    }

    // ------------------------------------------------------------------
    // Turn flow
    // ------------------------------------------------------------------

    private fun humanRoll() {
        if (game.isGameOver || animating || awaitingPick) return
        if (!game.players[game.currentPlayer].human) return

        rollButton.isEnabled = false
        val roll = game.rollDice()
        animateDiceTo(roll) { resolveRoll(roll) }
    }

    private fun animateDiceTo(finalValue: Int, onSettled: () -> Unit) {
        var ticks = 0
        val step = object : Runnable {
            override fun run() {
                ticks += 1
                if (ticks >= SHUFFLE_TICKS) {
                    diceView.value = finalValue
                    onSettled()
                } else {
                    diceView.value = (1..6).random()
                    handler.postDelayed(this, SHUFFLE_INTERVAL)
                }
            }
        }
        handler.post(step)
    }

    private fun resolveRoll(roll: Int) {
        if (game.isGameOver) return

        diceView.accentColor = LudoBoardView.PLAYER_COLORS[game.currentPlayer]

        if (game.mustForfeitTurn) {
            updateUi(emptySet())
            setStatus("Three sixes in a row — turn forfeited.")
            handler.postDelayed({ passTurn() }, TURN_PAUSE)
            return
        }

        val movable = game.movableTokens(game.currentPlayer, roll)
        if (movable.isEmpty()) {
            updateUi(emptySet())
            setStatus(
                if (roll == 6) {
                    "A six, but no token can move."
                } else {
                    "No possible move with a $roll."
                }
            )
            handler.postDelayed({ passTurn() }, TURN_PAUSE)
            return
        }

        if (game.players[game.currentPlayer].human) {
            awaitingPick = true
            updateUi(movable.toSet())
            setStatus("You rolled $roll — tap a highlighted token.")
        } else {
            updateUi(emptySet())
            setStatus("${game.players[game.currentPlayer].name} rolled $roll…")
            handler.postDelayed({ cpuMove(movable) }, CPU_MOVE_DELAY)
        }
    }

    private fun humanPick(tokenIndex: Int) {
        if (!awaitingPick || animating || game.isGameOver) return
        if (tokenIndex !in game.movableTokens(game.currentPlayer, game.lastRoll)) return

        awaitingPick = false
        applyMove(tokenIndex)
    }

    private fun cpuMove(movable: List<Int>) {
        if (game.isGameOver || animating) return
        applyMove(chooseCpuMove(movable))
    }

    /** Simple heuristic: finish a token > capture > leave the yard > advance the furthest token. */
    private fun chooseCpuMove(movable: List<Int>): Int {
        val player = game.currentPlayer
        val roll = game.lastRoll

        var best = movable.first()
        var bestScore = Int.MIN_VALUE
        for (tokenIndex in movable) {
            val step = game.tokenStep(player, tokenIndex)
            val score = when {
                step != -1 && step + roll == HOME_STEP -> 100
                game.wouldCapture(player, tokenIndex, roll) -> 90
                step == -1 -> 60
                else -> step
            }
            if (score > bestScore) {
                bestScore = score
                best = tokenIndex
            }
        }
        return best
    }

    private fun applyMove(tokenIndex: Int) {
        val player = game.currentPlayer
        val roll = game.lastRoll
        val fromStep = game.tokenStep(player, tokenIndex)
        val startPoint = boardView.positionForStep(player, tokenIndex, fromStep)
        val result = game.moveToken(player, tokenIndex, roll)

        val waypoints = result.pathSteps.map { step ->
            boardView.positionForStep(player, tokenIndex, step)
        }

        animating = true
        updateUi(emptySet())
        setStatus("${game.players[player].name} moves…")

        boardView.animateToken(player, tokenIndex, startPoint, waypoints) {
            animating = false
            if (result.wonGame) {
                showWinner(player)
                return@animateToken
            }
            if (result.extraTurn) {
                updateUi(emptySet())
                if (game.players[player].human) {
                    setStatus(
                        if (result.captures.isNotEmpty()) {
                            "Capture! Roll again."
                        } else {
                            "A six — roll again!"
                        }
                    )
                } else {
                    setStatus("${game.players[player].name} rolls again…")
                    handler.postDelayed({ cpuRoll() }, CPU_MOVE_DELAY)
                }
            } else {
                passTurn()
            }
        }
    }

    private fun cpuRoll() {
        if (game.isGameOver || animating) return
        if (game.players[game.currentPlayer].human) return

        val roll = game.rollDice()
        animateDiceTo(roll) { resolveRoll(roll) }
    }

    private fun passTurn() {
        if (game.isGameOver) return
        game.nextPlayer()
        updateUi(emptySet())

        val player = game.players[game.currentPlayer]
        if (player.human) {
            diceView.accentColor = LudoBoardView.PLAYER_COLORS[game.currentPlayer]
            setStatus("Your turn — roll the dice.")
        } else {
            setStatus("${player.name} is thinking…")
            handler.postDelayed({ cpuRoll() }, CPU_MOVE_DELAY)
        }
    }

    private fun showWinner(player: Int) {
        updateUi(emptySet())
        val humanWon = game.players[player].human
        setStatus(
            if (humanWon) {
                "All four of your tokens are home — you win! 🎉"
            } else {
                "${game.players[player].name} got all tokens home first."
            }
        )

        MaterialAlertDialogBuilder(this)
            .setTitle(if (humanWon) "You win!" else "${game.players[player].name} wins")
            .setMessage(
                if (humanWon) {
                    "You brought all four tokens home first. Well played!"
                } else {
                    "${game.players[player].name} finished first. Better luck next time?"
                }
            )
            .setCancelable(false)
            .setPositiveButton("Play again") { _, _ -> newGame() }
            .setNegativeButton("Exit") { _, _ -> finish() }
            .show()
    }

    private fun confirmRestart() {
        if (game.isGameOver) {
            newGame()
            return
        }
        MaterialAlertDialogBuilder(this)
            .setTitle("Restart game?")
            .setMessage("The current game will be lost.")
            .setPositiveButton("Restart") { _, _ -> newGame() }
            .setNegativeButton("Keep playing", null)
            .show()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    companion object {
        private const val HOME_STEP = 56
        private const val SHUFFLE_TICKS = 6
        private const val SHUFFLE_INTERVAL = 70L
        private const val TURN_PAUSE = 1000L
        private const val CPU_MOVE_DELAY = 800L
        private val MUTED_CHIP_COLOR = android.graphics.Color.parseColor("#8A9691")
    }
}
