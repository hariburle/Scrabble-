package com.example.game.engine

import com.example.game.dictionary.WordDictionary
import com.example.game.model.BoardCell
import com.example.game.model.BonusType
import com.example.game.model.FormedWord
import com.example.game.model.MoveScoreResult
import com.example.game.model.PlacedTile
import com.example.game.model.Tile
import kotlin.random.Random

object ScrabbleEngine {
    const val BOARD_SIZE = 15
    const val CENTER_ROW = 7
    const val CENTER_COL = 7

    /**
     * Standard Scrabble 15x15 Bonus Multipliers Map
     */
    fun createEmptyBoard(): Array<Array<BoardCell>> {
        val board = Array(BOARD_SIZE) { r ->
            Array(BOARD_SIZE) { c ->
                BoardCell(row = r, col = c, bonusType = getBonusType(r, c))
            }
        }
        return board
    }

    fun getBonusType(r: Int, c: Int): BonusType {
        // Center
        if (r == CENTER_ROW && c == CENTER_COL) return BonusType.CENTER

        // Triple Word Score (TWS) - 8 locations
        val twsCoordinates = setOf(
            0 to 0, 0 to 7, 0 to 14,
            7 to 0, 7 to 14,
            14 to 0, 14 to 7, 14 to 14
        )
        if (twsCoordinates.contains(r to c)) return BonusType.TRIPLE_WORD

        // Double Word Score (DWS) - diagonals & 4 corners
        val dwsCoordinates = setOf(
            1 to 1, 2 to 2, 3 to 3, 4 to 4,
            1 to 13, 2 to 12, 3 to 11, 4 to 10,
            13 to 1, 12 to 2, 11 to 3, 10 to 4,
            13 to 13, 12 to 12, 11 to 11, 10 to 10
        )
        if (dwsCoordinates.contains(r to c)) return BonusType.DOUBLE_WORD

        // Triple Letter Score (TLS) - 12 locations
        val tlsCoordinates = setOf(
            1 to 5, 1 to 9,
            5 to 1, 5 to 5, 5 to 9, 5 to 13,
            9 to 1, 9 to 5, 9 to 9, 9 to 13,
            13 to 5, 13 to 9
        )
        if (tlsCoordinates.contains(r to c)) return BonusType.TRIPLE_LETTER

        // Double Letter Score (DLS) - 24 locations
        val dlsCoordinates = setOf(
            0 to 3, 0 to 11,
            2 to 6, 2 to 8,
            3 to 0, 3 to 7, 3 to 14,
            6 to 2, 6 to 6, 6 to 8, 6 to 12,
            7 to 3, 7 to 11,
            8 to 2, 8 to 6, 8 to 8, 8 to 12,
            11 to 0, 11 to 7, 11 to 14,
            12 to 6, 12 to 8,
            14 to 3, 14 to 11
        )
        if (dlsCoordinates.contains(r to c)) return BonusType.DOUBLE_LETTER

        return BonusType.NONE
    }

    /**
     * Initial 100 tiles standard distribution
     */
    fun createTileBag(): MutableList<Tile> {
        val bag = mutableListOf<Tile>()
        val distribution = mapOf(
            'A' to 9, 'B' to 2, 'C' to 2, 'D' to 4, 'E' to 12,
            'F' to 2, 'G' to 3, 'H' to 2, 'I' to 9, 'J' to 1,
            'K' to 1, 'L' to 4, 'M' to 2, 'N' to 6, 'O' to 8,
            'P' to 2, 'Q' to 1, 'R' to 6, 'S' to 4, 'T' to 6,
            'U' to 4, 'V' to 2, 'W' to 2, 'X' to 1, 'Y' to 2,
            'Z' to 1, ' ' to 2 // 2 blanks
        )

        for ((char, count) in distribution) {
            repeat(count) {
                bag.add(Tile(letter = char))
            }
        }
        bag.shuffle(Random(System.currentTimeMillis()))
        return bag
    }

    /**
     * Draws up to [count] tiles from bag
     */
    fun drawTiles(bag: MutableList<Tile>, count: Int): List<Tile> {
        val drawn = mutableListOf<Tile>()
        val toDraw = minOf(count, bag.size)
        repeat(toDraw) {
            drawn.add(bag.removeAt(0))
        }
        return drawn
    }

    /**
     * Validate and score a candidate move on the board
     */
    fun evaluateMove(
        board: Array<Array<BoardCell>>,
        placements: List<PlacedTile>,
        isFirstMove: Boolean
    ): MoveScoreResult {
        if (placements.isEmpty()) {
            return MoveScoreResult(isValid = false, errorMessage = "No tiles placed")
        }

        // Check if any blank tile has no assigned letter
        if (placements.any { it.tile.isBlank && it.tile.assignedLetter == null }) {
            return MoveScoreResult(isValid = false, errorMessage = "Assign letter to blank tile")
        }

        // Must all be in a single row or single column
        val rows = placements.map { it.row }.toSet()
        val cols = placements.map { it.col }.toSet()
        val isHorizontal = rows.size == 1
        val isVertical = cols.size == 1

        if (!isHorizontal && !isVertical) {
            return MoveScoreResult(
                isValid = false,
                errorMessage = "Tiles must be in a single straight line"
            )
        }

        val fixedRow = placements.first().row
        val fixedCol = placements.first().col

        // Check first move condition
        if (isFirstMove) {
            val coversCenter = placements.any { it.row == CENTER_ROW && it.col == CENTER_COL }
            if (!coversCenter) {
                return MoveScoreResult(
                    isValid = false,
                    errorMessage = "First word must cover the center star (★)"
                )
            }
            if (placements.size < 2) {
                return MoveScoreResult(
                    isValid = false,
                    errorMessage = "First word must be at least 2 letters"
                )
            }
        }

        // Check that placements have no gaps (all intermediate cells must have either existing tile or pending tile)
        val placementMap = placements.associateBy { it.row to it.col }

        if (isHorizontal) {
            val minCol = placements.minOf { it.col }
            val maxCol = placements.maxOf { it.col }
            for (c in minCol..maxCol) {
                val hasPlacement = placementMap.containsKey(fixedRow to c)
                val hasExisting = board[fixedRow][c].tile != null
                if (!hasPlacement && !hasExisting) {
                    return MoveScoreResult(
                        isValid = false,
                        errorMessage = "No gaps allowed between tiles"
                    )
                }
            }
        } else {
            val minRow = placements.minOf { it.row }
            val maxRow = placements.maxOf { it.row }
            for (r in minRow..maxRow) {
                val hasPlacement = placementMap.containsKey(r to fixedCol)
                val hasExisting = board[r][fixedCol].tile != null
                if (!hasPlacement && !hasExisting) {
                    return MoveScoreResult(
                        isValid = false,
                        errorMessage = "No gaps allowed between tiles"
                    )
                }
            }
        }

        // If not first move, at least one tile must connect to existing tiles
        if (!isFirstMove) {
            var isConnected = false
            for (p in placements) {
                val adjacent = listOf(
                    p.row - 1 to p.col,
                    p.row + 1 to p.col,
                    p.row to p.col - 1,
                    p.row to p.col + 1
                )
                for ((ar, ac) in adjacent) {
                    if (ar in 0 until BOARD_SIZE && ac in 0 until BOARD_SIZE) {
                        if (board[ar][ac].tile != null) {
                            isConnected = true
                            break
                        }
                    }
                }
                if (isConnected) break
            }
            if (!isConnected) {
                return MoveScoreResult(
                    isValid = false,
                    errorMessage = "Move must connect to existing words on board"
                )
            }
        }

        // Now find main word formed
        val primaryWord: FormedWord?
        val crossWords = mutableListOf<FormedWord>()

        if (isHorizontal) {
            // Find full span of horizontal word
            var startC = placements.minOf { it.col }
            while (startC > 0 && (board[fixedRow][startC - 1].tile != null || placementMap.containsKey(fixedRow to startC - 1))) {
                startC--
            }
            var endC = placements.maxOf { it.col }
            while (endC < BOARD_SIZE - 1 && (board[fixedRow][endC + 1].tile != null || placementMap.containsKey(fixedRow to endC + 1))) {
                endC++
            }

            val length = endC - startC + 1
            if (length >= 2) {
                val sb = StringBuilder()
                var wordScore = 0
                var wordMultiplier = 1

                for (c in startC..endC) {
                    val placed = placementMap[fixedRow to c]
                    val existing = board[fixedRow][c].tile
                    val tile = placed?.tile ?: existing!!
                    val letter = tile.displayLetter
                    sb.append(letter)

                    if (placed != null) {
                        val bonus = board[fixedRow][c].bonusType
                        var letterScore = tile.points * bonus.letterMultiplier
                        wordScore += letterScore
                        wordMultiplier *= bonus.wordMultiplier
                    } else {
                        // Existing tile, multipliers already used
                        wordScore += tile.points
                    }
                }
                val totalPrimary = wordScore * wordMultiplier
                val wordStr = sb.toString()
                if (!WordDictionary.isValidWord(wordStr)) {
                    return MoveScoreResult(
                        isValid = false,
                        errorMessage = "\"$wordStr\" is not in the dictionary"
                    )
                }
                primaryWord = FormedWord(wordStr, totalPrimary, fixedRow, startC, true)
            } else {
                primaryWord = null
            }

            // Check perpendicular cross words for each placed tile
            for (p in placements) {
                var startR = p.row
                while (startR > 0 && (board[startR - 1][p.col].tile != null || placementMap.containsKey(startR - 1 to p.col))) {
                    startR--
                }
                var endR = p.row
                while (endR < BOARD_SIZE - 1 && (board[endR + 1][p.col].tile != null || placementMap.containsKey(endR + 1 to p.col))) {
                    endR++
                }

                val crossLen = endR - startR + 1
                if (crossLen >= 2) {
                    val sb = StringBuilder()
                    var crossScore = 0
                    var crossMultiplier = 1

                    for (r in startR..endR) {
                        val placed = placementMap[r to p.col]
                        val existing = board[r][p.col].tile
                        val tile = placed?.tile ?: existing!!
                        sb.append(tile.displayLetter)

                        if (placed != null) {
                            val bonus = board[r][p.col].bonusType
                            crossScore += tile.points * bonus.letterMultiplier
                            crossMultiplier *= bonus.wordMultiplier
                        } else {
                            crossScore += tile.points
                        }
                    }
                    val totalCross = crossScore * crossMultiplier
                    val crossStr = sb.toString()
                    if (!WordDictionary.isValidWord(crossStr)) {
                        return MoveScoreResult(
                            isValid = false,
                            errorMessage = "\"$crossStr\" is not in the dictionary"
                        )
                    }
                    crossWords.add(FormedWord(crossStr, totalCross, startR, p.col, false))
                }
            }
        } else {
            // Vertical placement
            var startR = placements.minOf { it.row }
            while (startR > 0 && (board[startR - 1][fixedCol].tile != null || placementMap.containsKey(startR - 1 to fixedCol))) {
                startR--
            }
            var endR = placements.maxOf { it.row }
            while (endR < BOARD_SIZE - 1 && (board[endR + 1][fixedCol].tile != null || placementMap.containsKey(endR + 1 to fixedCol))) {
                endR++
            }

            val length = endR - startR + 1
            if (length >= 2) {
                val sb = StringBuilder()
                var wordScore = 0
                var wordMultiplier = 1

                for (r in startR..endR) {
                    val placed = placementMap[r to fixedCol]
                    val existing = board[r][fixedCol].tile
                    val tile = placed?.tile ?: existing!!
                    sb.append(tile.displayLetter)

                    if (placed != null) {
                        val bonus = board[r][fixedCol].bonusType
                        wordScore += tile.points * bonus.letterMultiplier
                        wordMultiplier *= bonus.wordMultiplier
                    } else {
                        wordScore += tile.points
                    }
                }
                val totalPrimary = wordScore * wordMultiplier
                val wordStr = sb.toString()
                if (!WordDictionary.isValidWord(wordStr)) {
                    return MoveScoreResult(
                        isValid = false,
                        errorMessage = "\"$wordStr\" is not in the dictionary"
                    )
                }
                primaryWord = FormedWord(wordStr, totalPrimary, startR, fixedCol, false)
            } else {
                primaryWord = null
            }

            // Check perpendicular horizontal cross words
            for (p in placements) {
                var startC = p.col
                while (startC > 0 && (board[p.row][startC - 1].tile != null || placementMap.containsKey(p.row to startC - 1))) {
                    startC--
                }
                var endC = p.col
                while (endC < BOARD_SIZE - 1 && (board[p.row][endC + 1].tile != null || placementMap.containsKey(p.row to endC + 1))) {
                    endC++
                }

                val crossLen = endC - startC + 1
                if (crossLen >= 2) {
                    val sb = StringBuilder()
                    var crossScore = 0
                    var crossMultiplier = 1

                    for (c in startC..endC) {
                        val placed = placementMap[p.row to c]
                        val existing = board[p.row][c].tile
                        val tile = placed?.tile ?: existing!!
                        sb.append(tile.displayLetter)

                        if (placed != null) {
                            val bonus = board[p.row][c].bonusType
                            crossScore += tile.points * bonus.letterMultiplier
                            crossMultiplier *= bonus.wordMultiplier
                        } else {
                            crossScore += tile.points
                        }
                    }
                    val totalCross = crossScore * crossMultiplier
                    val crossStr = sb.toString()
                    if (!WordDictionary.isValidWord(crossStr)) {
                        return MoveScoreResult(
                            isValid = false,
                            errorMessage = "\"$crossStr\" is not in the dictionary"
                        )
                    }
                    crossWords.add(FormedWord(crossStr, totalCross, p.row, startC, true))
                }
            }
        }

        val allWords = listOfNotNull(primaryWord) + crossWords
        if (allWords.isEmpty()) {
            return MoveScoreResult(
                isValid = false,
                errorMessage = "Must form at least one 2+ letter word"
            )
        }

        var totalScore = allWords.sumOf { it.points }
        val isBingo = placements.size == 7
        if (isBingo) {
            totalScore += 50 // 50 point Bingo bonus for using all 7 tiles
        }

        return MoveScoreResult(
            isValid = true,
            primaryWord = primaryWord,
            crossWords = crossWords,
            totalScore = totalScore,
            isBingo = isBingo
        )
    }

    /**
     * Checks if board has any placed tiles
     */
    fun isBoardEmpty(board: Array<Array<BoardCell>>): Boolean {
        for (r in 0 until BOARD_SIZE) {
            for (c in 0 until BOARD_SIZE) {
                if (board[r][c].tile != null) return false
            }
        }
        return true
    }
}
