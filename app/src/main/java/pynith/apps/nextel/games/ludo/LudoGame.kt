package pynith.apps.nextel.games.ludo

/**
 * Board geometry for a classic 15x15 Ludo board, shared by the game logic and
 * the board renderer.
 */
object LudoBoard {
    /**
     * The 52 main-track cells as (column, row) pairs, listed clockwise.
     * Index 0 is the bottom-left player's start cell; the other start cells
     * sit 13 cells apart (0, 13, 26, 39).
     */
    val TRACK: List<Pair<Int, Int>> = listOf(
        1 to 6, 2 to 6, 3 to 6, 4 to 6, 5 to 6,
        6 to 5, 6 to 4, 6 to 3, 6 to 2, 6 to 1, 6 to 0,
        7 to 0,
        8 to 0, 8 to 1, 8 to 2, 8 to 3, 8 to 4, 8 to 5,
        9 to 6, 10 to 6, 11 to 6, 12 to 6, 13 to 6, 14 to 6,
        14 to 7,
        14 to 8, 13 to 8, 12 to 8, 11 to 8, 10 to 8, 9 to 8,
        8 to 9, 8 to 10, 8 to 11, 8 to 12, 8 to 13, 8 to 14,
        7 to 14,
        6 to 14, 6 to 13, 6 to 12, 6 to 11, 6 to 10, 6 to 9,
        5 to 8, 4 to 8, 3 to 8, 2 to 8, 1 to 8, 0 to 8,
        0 to 7,
        0 to 6
    )

    /** Track indexes that cannot be captured: the four start cells and the four star cells. */
    val SAFE_TRACK_INDEXES: Set<Int> = setOf(0, 8, 13, 21, 26, 34, 39, 47)

    init {
        require(TRACK.size == 52) { "Ludo track must have 52 cells." }
    }
}

/** Result of applying one token move. */
data class LudoMoveResult(
    /** Absolute steps the token passes through (yard exit = [0]; otherwise from+1..to), for animation. */
    val pathSteps: List<Int>,
    /** (owner, tokenIndex) pairs sent back to the yard by this move. */
    val captures: List<Pair<Int, Int>>,
    val extraTurn: Boolean,
    val wonGame: Boolean
)

/** One Ludo token. Position is a "step": -1 = yard, 0..50 = main track, 51..55 = home column, 56 = home. */
class LudoToken(val owner: Int) {
    var step: Int = -1
}

/**
 * Classic 4-player Ludo rules (one human + three computer players), pure logic
 * with no Android dependencies. Rules implemented:
 *  - roll a 6 to bring a token out of the yard; a 6 grants another roll;
 *    three 6s in a row forfeit the turn;
 *  - tokens move clockwise and must land on HOME (step 56) exactly;
 *  - landing on a single opponent token on a non-safe cell sends it back to
 *    its yard (two or more tokens on a cell form a block and cannot be
 *    captured); capturing grants another roll;
 *  - the first player to bring all four tokens home wins.
 */
class LudoGame {

    data class Player(
        val index: Int,
        val name: String,
        val startIndex: Int,
        val homeColumn: List<Pair<Int, Int>>,
        val human: Boolean
    )

    /** Player 0 (red, bottom-left) is the human; the others are computer controlled. */
    val players: List<Player> = listOf(
        Player(0, "You", 39, listOf(7 to 13, 7 to 12, 7 to 11, 7 to 10, 7 to 9), true),
        Player(1, "Green", 0, listOf(1 to 7, 2 to 7, 3 to 7, 4 to 7, 5 to 7), false),
        Player(2, "Yellow", 13, listOf(7 to 1, 7 to 2, 7 to 3, 7 to 4, 7 to 5), false),
        Player(3, "Blue", 26, listOf(13 to 7, 12 to 7, 11 to 7, 10 to 7, 9 to 7), false)
    )

    val tokens: Array<Array<LudoToken>> = Array(4) { owner -> Array(4) { LudoToken(owner) } }

    var currentPlayer: Int = 0
        private set
    var lastRoll: Int = 0
        private set
    var consecutiveSixes: Int = 0
        private set
    var winner: Int = -1
        private set

    val isGameOver: Boolean
        get() = winner >= 0

    /** Main-track index for a player's token at [step], or null when off the main track. */
    fun trackIndex(playerIndex: Int, step: Int): Int? =
        if (step in 0..50) (players[playerIndex].startIndex + step) % 52 else null

    /** Physical grid cell on the main track for a player's token at [step], or null when off it. */
    fun trackCell(playerIndex: Int, step: Int): Pair<Int, Int>? =
        trackIndex(playerIndex, step)?.let { LudoBoard.TRACK[it] }

    fun tokenStep(playerIndex: Int, tokenIndex: Int): Int = tokens[playerIndex][tokenIndex].step

    /** Token indices the given roll lets [playerIndex] move. */
    fun movableTokens(playerIndex: Int, roll: Int): List<Int> {
        val result = mutableListOf<Int>()
        tokens[playerIndex].forEachIndexed { index, token ->
            if ((token.step == -1 && roll == 6) || (token.step in 0..55 && token.step + roll <= 56)) {
                result += index
            }
        }
        return result
    }

    /** Rolls the dice for the current player, tracking consecutive sixes. */
    fun rollDice(): Int {
        val roll = (1..6).random()
        lastRoll = roll
        consecutiveSixes = if (roll == 6) consecutiveSixes + 1 else 0
        return roll
    }

    /** True when three sixes in a row forfeit the current player's turn. */
    val mustForfeitTurn: Boolean
        get() = consecutiveSixes >= 3

    /** Opponent tokens standing on the main-track cell where a step lands. */
    private fun opponentsAtEndCell(playerIndex: Int, endStep: Int): List<Pair<Int, Int>> {
        val endTrackIndex = trackIndex(playerIndex, endStep) ?: return emptyList()
        val result = mutableListOf<Pair<Int, Int>>()
        for (owner in 0 until 4) {
            if (owner == playerIndex) continue
            tokens[owner].forEachIndexed { index, token ->
                if (trackIndex(owner, token.step) == endTrackIndex) {
                    result += owner to index
                }
            }
        }
        return result
    }

    /** Whether moving the token with [roll] would send an opponent token home. */
    fun wouldCapture(playerIndex: Int, tokenIndex: Int, roll: Int): Boolean {
        val current = tokens[playerIndex][tokenIndex].step
        val endStep = if (current == -1) 0 else current + roll
        val endTrackIndex = trackIndex(playerIndex, endStep) ?: return false
        if (endTrackIndex in LudoBoard.SAFE_TRACK_INDEXES) return false
        return opponentsAtEndCell(playerIndex, endStep).size == 1
    }

    /**
     * Applies the move for the current player and reports what happened so the
     * UI can animate and continue the turn order.
     */
    fun moveToken(playerIndex: Int, tokenIndex: Int, roll: Int): LudoMoveResult {
        val token = tokens[playerIndex][tokenIndex]
        val fromStep = token.step
        val toStep = if (fromStep == -1) 0 else fromStep + roll
        require(toStep in 0..56) { "Illegal move to step $toStep." }
        require(playerIndex == currentPlayer) { "Not this player's turn." }

        val pathSteps = if (fromStep == -1) listOf(0) else (fromStep + 1..toStep).toList()

        token.step = toStep

        // Capture: a single opponent on the landing cell (not a safe cell) goes home.
        val captures = mutableListOf<Pair<Int, Int>>()
        val endTrackIndex = trackIndex(playerIndex, toStep)
        if (endTrackIndex != null && endTrackIndex !in LudoBoard.SAFE_TRACK_INDEXES) {
            val opponents = opponentsAtEndCell(playerIndex, toStep)
            if (opponents.size == 1) {
                val (owner, index) = opponents.first()
                tokens[owner][index].step = -1
                captures += owner to index
            }
        }

        val won = tokens[playerIndex].all { it.step == 56 }
        if (won) winner = playerIndex

        // A six or a capture grants another roll, unless the turn is forfeited.
        val extraTurn = !won && !mustForfeitTurn && (roll == 6 || captures.isNotEmpty())

        return LudoMoveResult(pathSteps, captures, extraTurn, won)
    }

    /** Passes the turn to the next player. */
    fun nextPlayer() {
        consecutiveSixes = 0
        currentPlayer = (currentPlayer + 1) % 4
    }

    /** Number of tokens the player has brought all the way home. */
    fun homeCount(playerIndex: Int): Int = tokens[playerIndex].count { it.step == 56 }
}
