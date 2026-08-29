package com.example.game.engine

import com.example.game.dictionary.WordDictionary
import com.example.game.model.BoardCell
import com.example.game.model.CustomDifficultySettings
import com.example.game.model.Difficulty
import com.example.game.model.MoveScoreResult
import com.example.game.model.PlacedTile
import com.example.game.model.Tile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.Random

data class AiCandidateMove(
    val placements: List<PlacedTile>,
    val scoreResult: MoveScoreResult
)

object AiEngine {

    /**
     * Finds the best or appropriately tailored move for the AI opponent
     */
    suspend fun findAiMove(
        board: Array<Array<BoardCell>>,
        aiRack: List<Tile>,
        difficulty: Difficulty,
        customSettings: CustomDifficultySettings? = null
    ): AiCandidateMove? = withContext(Dispatchers.Default) {
        val isFirstMove = ScrabbleEngine.isBoardEmpty(board)
        val candidateMoves = generateCandidateMoves(board, aiRack, isFirstMove, limit = 50)

        if (candidateMoves.isEmpty()) return@withContext null

        when (difficulty) {
            Difficulty.RELAXED -> {
                // Pick friendly word with score between 8 and 22 if possible, or lowest scoring valid word
                val inTargetRange = candidateMoves.filter { it.scoreResult.totalScore in 8..24 }
                if (inTargetRange.isNotEmpty()) {
                    inTargetRange.random()
                } else {
                    candidateMoves.minByOrNull { it.scoreResult.totalScore }
                }
            }
            Difficulty.BALANCED -> {
                // Pick a medium-strength move (top 30-70th percentile)
                val sorted = candidateMoves.sortedByDescending { it.scoreResult.totalScore }
                val targetIndex = (sorted.size * 0.35f).toInt().coerceIn(0, sorted.size - 1)
                // Pick among nearby candidates for variety
                val windowSize = minOf(3, sorted.size)
                val startIndex = (targetIndex - windowSize / 2).coerceIn(0, sorted.size - windowSize)
                sorted.subList(startIndex, startIndex + windowSize).random()
            }
            Difficulty.MASTER -> {
                // Pick highest scoring move
                candidateMoves.maxByOrNull { it.scoreResult.totalScore }
            }
            Difficulty.CUSTOM -> {
                val agg = customSettings?.aggressiveness ?: 0.5f
                val maxLen = customSettings?.maxWordLength ?: 6
                val filtered = candidateMoves.filter { 
                    (it.scoreResult.primaryWord?.word?.length ?: 0) <= maxLen 
                }.ifEmpty { candidateMoves }

                val sorted = filtered.sortedByDescending { it.scoreResult.totalScore }
                val index = ((1.0f - agg) * (sorted.size - 1)).toInt().coerceIn(0, sorted.size - 1)
                sorted[index]
            }
        }
    }

    /**
     * Generates a hint for the human player
     */
    suspend fun findHint(
        board: Array<Array<BoardCell>>,
        playerRack: List<Tile>
    ): AiCandidateMove? = withContext(Dispatchers.Default) {
        val isFirstMove = ScrabbleEngine.isBoardEmpty(board)
        val candidates = generateCandidateMoves(board, playerRack, isFirstMove, limit = 30)
        candidates.maxByOrNull { it.scoreResult.totalScore }
    }

    private fun generateCandidateMoves(
        board: Array<Array<BoardCell>>,
        rack: List<Tile>,
        isFirstMove: Boolean,
        limit: Int
    ): List<AiCandidateMove> {
        val candidates = mutableListOf<AiCandidateMove>()
        val rackLetters = rack.map { if (it.isBlank) ' ' else it.letter }

        if (isFirstMove) {
            // Must go through (7,7)
            // Try horizontal and vertical words placed across center
            generateFirstMoves(board, rack, candidates)
            return candidates.take(limit)
        }

        // Find all anchor cells (empty cells adjacent to at least one placed tile)
        val anchors = mutableListOf<Pair<Int, Int>>()
        for (r in 0 until ScrabbleEngine.BOARD_SIZE) {
            for (c in 0 until ScrabbleEngine.BOARD_SIZE) {
                if (board[r][c].tile == null) {
                    val hasAdjacentTile = (r > 0 && board[r - 1][c].tile != null) ||
                            (r < ScrabbleEngine.BOARD_SIZE - 1 && board[r + 1][c].tile != null) ||
                            (c > 0 && board[r][c - 1].tile != null) ||
                            (c < ScrabbleEngine.BOARD_SIZE - 1 && board[r][c + 1].tile != null)
                    if (hasAdjacentTile) {
                        anchors.add(r to c)
                    }
                }
            }
        }

        anchors.shuffle()

        // Sample dictionary words to test on anchors
        val wordSample = WordDictionary.allWordsList.shuffled().take(400)

        for ((ar, ac) in anchors) {
            if (candidates.size >= limit * 2) break

            // Try horizontal plays around anchor
            for (word in wordSample) {
                if (word.length > 8) continue

                // Check if word can fit horizontally across (ar, ac)
                for (offset in 0 until word.length) {
                    val startC = ac - offset
                    val endC = startC + word.length - 1
                    if (startC < 0 || endC >= ScrabbleEngine.BOARD_SIZE) continue

                    val placements = matchWordToBoardHorizontal(board, rack, word, ar, startC)
                    if (placements != null && placements.isNotEmpty()) {
                        val eval = ScrabbleEngine.evaluateMove(board, placements, isFirstMove = false)
                        if (eval.isValid) {
                            candidates.add(AiCandidateMove(placements, eval))
                            if (candidates.size >= limit * 2) break
                        }
                    }
                }

                // Check if word can fit vertically across (ar, ac)
                for (offset in 0 until word.length) {
                    val startR = ar - offset
                    val endR = startR + word.length - 1
                    if (startR < 0 || endR >= ScrabbleEngine.BOARD_SIZE) continue

                    val placements = matchWordToBoardVertical(board, rack, word, startR, ac)
                    if (placements != null && placements.isNotEmpty()) {
                        val eval = ScrabbleEngine.evaluateMove(board, placements, isFirstMove = false)
                        if (eval.isValid) {
                            candidates.add(AiCandidateMove(placements, eval))
                            if (candidates.size >= limit * 2) break
                        }
                    }
                }
            }
        }

        return candidates.distinctBy { move ->
            move.placements.map { "${it.row},${it.col},${it.tile.displayLetter}" }.sorted().joinToString(";")
        }.take(limit)
    }

    private fun generateFirstMoves(
        board: Array<Array<BoardCell>>,
        rack: List<Tile>,
        results: MutableList<AiCandidateMove>
    ) {
        val sampleWords = WordDictionary.allWordsList.shuffled().take(600)
        val center = ScrabbleEngine.CENTER_ROW

        for (word in sampleWords) {
            if (word.length !in 2..rack.size) continue

            // Try placing horizontally covering (7,7)
            for (offset in 0 until word.length) {
                val startC = center - offset
                val endC = startC + word.length - 1
                if (startC < 0 || endC >= ScrabbleEngine.BOARD_SIZE) continue

                val placements = matchWordToEmpty(rack, word, center, startC, isHorizontal = true)
                if (placements != null) {
                    val eval = ScrabbleEngine.evaluateMove(board, placements, isFirstMove = true)
                    if (eval.isValid) {
                        results.add(AiCandidateMove(placements, eval))
                    }
                }
            }
        }
    }

    private fun matchWordToEmpty(
        rack: List<Tile>,
        word: String,
        startR: Int,
        startC: Int,
        isHorizontal: Boolean
    ): List<PlacedTile>? {
        val availableRack = rack.toMutableList()
        val placements = mutableListOf<PlacedTile>()

        for (i in word.indices) {
            val char = word[i]
            val r = if (isHorizontal) startR else startR + i
            val c = if (isHorizontal) startC + i else startC

            val directTileIdx = availableRack.indexOfFirst { !it.isBlank && it.letter == char }
            if (directTileIdx >= 0) {
                val tile = availableRack.removeAt(directTileIdx)
                placements.add(PlacedTile(r, c, tile))
            } else {
                val blankIdx = availableRack.indexOfFirst { it.isBlank }
                if (blankIdx >= 0) {
                    val blankTile = availableRack.removeAt(blankIdx)
                    placements.add(PlacedTile(r, c, blankTile.copy(assignedLetter = char)))
                } else {
                    return null // Cannot form word with current rack
                }
            }
        }
        return placements
    }

    private fun matchWordToBoardHorizontal(
        board: Array<Array<BoardCell>>,
        rack: List<Tile>,
        word: String,
        row: Int,
        startC: Int
    ): List<PlacedTile>? {
        val availableRack = rack.toMutableList()
        val placements = mutableListOf<PlacedTile>()
        var usedAtLeastOneRackTile = false
        var usedAtLeastOneBoardTile = false

        for (i in word.indices) {
            val char = word[i]
            val c = startC + i
            val existing = board[row][c].tile

            if (existing != null) {
                if (existing.displayLetter != char) return null
                usedAtLeastOneBoardTile = true
            } else {
                // Must use tile from rack
                val directTileIdx = availableRack.indexOfFirst { !it.isBlank && it.letter == char }
                if (directTileIdx >= 0) {
                    val tile = availableRack.removeAt(directTileIdx)
                    placements.add(PlacedTile(row, c, tile))
                    usedAtLeastOneRackTile = true
                } else {
                    val blankIdx = availableRack.indexOfFirst { it.isBlank }
                    if (blankIdx >= 0) {
                        val blankTile = availableRack.removeAt(blankIdx)
                        placements.add(PlacedTile(row, c, blankTile.copy(assignedLetter = char)))
                        usedAtLeastOneRackTile = true
                    } else {
                        return null
                    }
                }
            }
        }

        return if (usedAtLeastOneRackTile && usedAtLeastOneBoardTile) placements else null
    }

    private fun matchWordToBoardVertical(
        board: Array<Array<BoardCell>>,
        rack: List<Tile>,
        word: String,
        startR: Int,
        col: Int
    ): List<PlacedTile>? {
        val availableRack = rack.toMutableList()
        val placements = mutableListOf<PlacedTile>()
        var usedAtLeastOneRackTile = false
        var usedAtLeastOneBoardTile = false

        for (i in word.indices) {
            val char = word[i]
            val r = startR + i
            val existing = board[r][col].tile

            if (existing != null) {
                if (existing.displayLetter != char) return null
                usedAtLeastOneBoardTile = true
            } else {
                // Must use tile from rack
                val directTileIdx = availableRack.indexOfFirst { !it.isBlank && it.letter == char }
                if (directTileIdx >= 0) {
                    val tile = availableRack.removeAt(directTileIdx)
                    placements.add(PlacedTile(r, col, tile))
                    usedAtLeastOneRackTile = true
                } else {
                    val blankIdx = availableRack.indexOfFirst { it.isBlank }
                    if (blankIdx >= 0) {
                        val blankTile = availableRack.removeAt(blankIdx)
                        placements.add(PlacedTile(r, col, blankTile.copy(assignedLetter = char)))
                        usedAtLeastOneRackTile = true
                    } else {
                        return null
                    }
                }
            }
        }

        return if (usedAtLeastOneRackTile && usedAtLeastOneBoardTile) placements else null
    }
}
