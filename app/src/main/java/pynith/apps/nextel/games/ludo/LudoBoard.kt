package pynith.apps.nextel.games.ludo

/**
 * Ludo board geometry ported 1:1 from the Flutter module
 * (flutter-game-module/ludo/constants.dart). Coordinates are [x, y] with
 * x = column from the left and y = row from the top of a 15x15 board.
 *
 * Each player path holds 57 steps: 51 shared ring cells followed by the
 * 6 colored home-column cells; step 56 is the final cell. Step -1 means
 * the pawn is parked in its yard.
 */
object LudoBoard {

    val greenPath = arrayOf(
        intArrayOf(1, 6), intArrayOf(2, 6), intArrayOf(3, 6), intArrayOf(4, 6), intArrayOf(5, 6),
        intArrayOf(6, 5), intArrayOf(6, 4), intArrayOf(6, 3), intArrayOf(6, 2), intArrayOf(6, 1), intArrayOf(6, 0),
        intArrayOf(7, 0),
        intArrayOf(8, 0), intArrayOf(8, 1), intArrayOf(8, 2), intArrayOf(8, 3), intArrayOf(8, 4), intArrayOf(8, 5),
        intArrayOf(9, 6), intArrayOf(10, 6), intArrayOf(11, 6), intArrayOf(12, 6), intArrayOf(13, 6), intArrayOf(14, 6),
        intArrayOf(14, 7),
        intArrayOf(14, 8), intArrayOf(13, 8), intArrayOf(12, 8), intArrayOf(11, 8), intArrayOf(10, 8), intArrayOf(9, 8),
        intArrayOf(8, 9), intArrayOf(8, 10), intArrayOf(8, 11), intArrayOf(8, 12), intArrayOf(8, 13), intArrayOf(8, 14),
        intArrayOf(7, 14),
        intArrayOf(6, 14), intArrayOf(6, 13), intArrayOf(6, 12), intArrayOf(6, 11), intArrayOf(6, 10), intArrayOf(6, 9),
        intArrayOf(5, 8), intArrayOf(4, 8), intArrayOf(3, 8), intArrayOf(2, 8), intArrayOf(1, 8), intArrayOf(0, 8),
        intArrayOf(0, 7),
        intArrayOf(1, 7), intArrayOf(2, 7), intArrayOf(3, 7), intArrayOf(4, 7), intArrayOf(5, 7), intArrayOf(6, 7)
    )

    val yellowPath = arrayOf(
        intArrayOf(8, 1), intArrayOf(8, 2), intArrayOf(8, 3), intArrayOf(8, 4), intArrayOf(8, 5),
        intArrayOf(9, 6), intArrayOf(10, 6), intArrayOf(11, 6), intArrayOf(12, 6), intArrayOf(13, 6), intArrayOf(14, 6),
        intArrayOf(14, 7),
        intArrayOf(14, 8), intArrayOf(13, 8), intArrayOf(12, 8), intArrayOf(11, 8), intArrayOf(10, 8), intArrayOf(9, 8),
        intArrayOf(8, 9), intArrayOf(8, 10), intArrayOf(8, 11), intArrayOf(8, 12), intArrayOf(8, 13), intArrayOf(8, 14),
        intArrayOf(7, 14),
        intArrayOf(6, 14), intArrayOf(6, 13), intArrayOf(6, 12), intArrayOf(6, 11), intArrayOf(6, 10), intArrayOf(6, 9),
        intArrayOf(5, 8), intArrayOf(4, 8), intArrayOf(3, 8), intArrayOf(2, 8), intArrayOf(1, 8), intArrayOf(0, 8),
        intArrayOf(0, 7),
        intArrayOf(0, 6), intArrayOf(1, 6), intArrayOf(2, 6), intArrayOf(3, 6), intArrayOf(4, 6), intArrayOf(5, 6),
        intArrayOf(6, 5), intArrayOf(6, 4), intArrayOf(6, 3), intArrayOf(6, 2), intArrayOf(6, 1), intArrayOf(6, 0),
        intArrayOf(7, 0),
        intArrayOf(7, 1), intArrayOf(7, 2), intArrayOf(7, 3), intArrayOf(7, 4), intArrayOf(7, 5), intArrayOf(7, 6)
    )

    val bluePath = arrayOf(
        intArrayOf(13, 8), intArrayOf(12, 8), intArrayOf(11, 8), intArrayOf(10, 8), intArrayOf(9, 8),
        intArrayOf(8, 9), intArrayOf(8, 10), intArrayOf(8, 11), intArrayOf(8, 12), intArrayOf(8, 13), intArrayOf(8, 14),
        intArrayOf(7, 14),
        intArrayOf(6, 14), intArrayOf(6, 13), intArrayOf(6, 12), intArrayOf(6, 11), intArrayOf(6, 10), intArrayOf(6, 9),
        intArrayOf(5, 8), intArrayOf(4, 8), intArrayOf(3, 8), intArrayOf(2, 8), intArrayOf(1, 8), intArrayOf(0, 8),
        intArrayOf(0, 7),
        intArrayOf(0, 6), intArrayOf(1, 6), intArrayOf(2, 6), intArrayOf(3, 6), intArrayOf(4, 6), intArrayOf(5, 6),
        intArrayOf(6, 5), intArrayOf(6, 4), intArrayOf(6, 3), intArrayOf(6, 2), intArrayOf(6, 1), intArrayOf(6, 0),
        intArrayOf(7, 0),
        intArrayOf(8, 0), intArrayOf(8, 1), intArrayOf(8, 2), intArrayOf(8, 3), intArrayOf(8, 4), intArrayOf(8, 5),
        intArrayOf(9, 6), intArrayOf(10, 6), intArrayOf(11, 6), intArrayOf(12, 6), intArrayOf(13, 6), intArrayOf(14, 6),
        intArrayOf(14, 7),
        intArrayOf(13, 7), intArrayOf(12, 7), intArrayOf(11, 7), intArrayOf(10, 7), intArrayOf(9, 7), intArrayOf(8, 7)
    )

    val redPath = arrayOf(
        intArrayOf(6, 13), intArrayOf(6, 12), intArrayOf(6, 11), intArrayOf(6, 10), intArrayOf(6, 9),
        intArrayOf(5, 8), intArrayOf(4, 8), intArrayOf(3, 8), intArrayOf(2, 8), intArrayOf(1, 8), intArrayOf(0, 8),
        intArrayOf(0, 7),
        intArrayOf(0, 6), intArrayOf(1, 6), intArrayOf(2, 6), intArrayOf(3, 6), intArrayOf(4, 6), intArrayOf(5, 6),
        intArrayOf(6, 5), intArrayOf(6, 4), intArrayOf(6, 3), intArrayOf(6, 2), intArrayOf(6, 1), intArrayOf(6, 0),
        intArrayOf(7, 0),
        intArrayOf(8, 0), intArrayOf(8, 1), intArrayOf(8, 2), intArrayOf(8, 3), intArrayOf(8, 4), intArrayOf(8, 5),
        intArrayOf(9, 6), intArrayOf(10, 6), intArrayOf(11, 6), intArrayOf(12, 6), intArrayOf(13, 6), intArrayOf(14, 6),
        intArrayOf(14, 7),
        intArrayOf(14, 8), intArrayOf(13, 8), intArrayOf(12, 8), intArrayOf(11, 8), intArrayOf(10, 8), intArrayOf(9, 8),
        intArrayOf(8, 9), intArrayOf(8, 10), intArrayOf(8, 11), intArrayOf(8, 12), intArrayOf(8, 13), intArrayOf(8, 14),
        intArrayOf(7, 14),
        intArrayOf(7, 13), intArrayOf(7, 12), intArrayOf(7, 11), intArrayOf(7, 10), intArrayOf(7, 9), intArrayOf(7, 8)
    )

    /** Cells (the four start cells plus the four star cells) where pawns cannot be captured. */
    val safeCells: Set<Long> = setOf(
        cell(6, 2), cell(12, 6), cell(8, 12), cell(2, 8),
        cell(8, 1), cell(13, 8), cell(6, 13), cell(1, 6)
    )

    /** Yard parking slots [x, y] (centers, in cell units), one per pawn. */
    val greenYard = arrayOf(
        floatArrayOf(1.5f, 1.5f), floatArrayOf(1.5f, 3.5f),
        floatArrayOf(3.5f, 1.5f), floatArrayOf(3.5f, 3.5f)
    )
    val yellowYard = arrayOf(
        floatArrayOf(10.5f, 1.5f), floatArrayOf(10.5f, 3.5f),
        floatArrayOf(12.5f, 1.5f), floatArrayOf(12.5f, 3.5f)
    )
    val blueYard = arrayOf(
        floatArrayOf(10.5f, 10.5f), floatArrayOf(10.5f, 12.5f),
        floatArrayOf(12.5f, 10.5f), floatArrayOf(12.5f, 12.5f)
    )
    val redYard = arrayOf(
        floatArrayOf(1.5f, 10.5f), floatArrayOf(1.5f, 12.5f),
        floatArrayOf(3.5f, 10.5f), floatArrayOf(3.5f, 12.5f)
    )

    fun path(type: LudoPlayerType): Array<IntArray> = when (type) {
        LudoPlayerType.GREEN -> greenPath
        LudoPlayerType.YELLOW -> yellowPath
        LudoPlayerType.BLUE -> bluePath
        LudoPlayerType.RED -> redPath
    }

    fun yard(type: LudoPlayerType): Array<FloatArray> = when (type) {
        LudoPlayerType.GREEN -> greenYard
        LudoPlayerType.YELLOW -> yellowYard
        LudoPlayerType.BLUE -> blueYard
        LudoPlayerType.RED -> redYard
    }

    /** Packs a cell into a comparable key. */
    fun cell(x: Int, y: Int): Long = (x.toLong() shl 32) or y.toLong()

    /** The shared ring, in draw order (any player's first 51 cells). */
    val ring: Array<IntArray> = greenPath.copyOfRange(0, 51)
}
