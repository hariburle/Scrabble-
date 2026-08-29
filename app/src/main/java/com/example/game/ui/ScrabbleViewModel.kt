package com.example.game.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.game.dictionary.WordDictionary
import com.example.game.engine.AiCandidateMove
import com.example.game.engine.AiEngine
import com.example.game.engine.ScrabbleEngine
import com.example.game.model.BoardCell
import com.example.game.model.CustomDifficultySettings
import com.example.game.model.Difficulty
import com.example.game.model.GameLogEntry
import com.example.game.model.GameStats
import com.example.game.model.GameThemePalette
import com.example.game.model.MoveScoreResult
import com.example.game.model.PlacedTile
import com.example.game.model.ThemeMode
import com.example.game.model.Tile
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ScrabbleUiState(
    val board: Array<Array<BoardCell>> = ScrabbleEngine.createEmptyBoard(),
    val playerRack: List<Tile> = emptyList(),
    val aiRack: List<Tile> = emptyList(),
    val tileBagCount: Int = 100,
    val playerScore: Int = 0,
    val aiScore: Int = 0,
    val isPlayerTurn: Boolean = true,
    val isAiThinking: Boolean = false,
    val pendingPlacements: Map<Pair<Int, Int>, Tile> = emptyMap(),
    val selectedRackIndex: Int? = null,
    val selectedBoardCell: Pair<Int, Int>? = null,
    val lastMoveCells: Set<Pair<Int, Int>> = emptySet(),
    val validationResult: MoveScoreResult? = null,
    val difficulty: Difficulty = Difficulty.BALANCED,
    val customSettings: CustomDifficultySettings = CustomDifficultySettings(),
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val currentTheme: GameThemePalette = GameThemePalette.BOTANICAL_ZEN,
    val moveLogs: List<GameLogEntry> = emptyList(),
    val stats: GameStats = GameStats(),
    val isGameOver: Boolean = false,
    val bestWord: String = "",
    val bestWordPoints: Int = 0,
    val consecutivePasses: Int = 0,
    val blankPickerCell: Pair<Int, Int>? = null,
    val showSwapDialog: Boolean = false,
    val showSettingsDialog: Boolean = false,
    val showThemeDialog: Boolean = false,
    val showHistoryDialog: Boolean = false,
    val showStatsDialog: Boolean = false,
    val showGameOverDialog: Boolean = false,
    val definitionWord: String? = null,
    val hintMove: AiCandidateMove? = null,
    val messageToast: String? = null,
    val hintsLeft: Int = 3,
    val swapsLeft: Int = 3,
    val peeksLeft: Int = 3,
    val isPeeking: Boolean = false
)

class ScrabbleViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ScrabbleUiState())
    val uiState: StateFlow<ScrabbleUiState> = _uiState.asStateFlow()

    private var tileBag = mutableListOf<Tile>()

    init {
        viewModelScope.launch {
            WordDictionary.init()
            startNewGame()
        }
    }

    fun startNewGame() {
        tileBag = ScrabbleEngine.createTileBag()
        val emptyBoard = ScrabbleEngine.createEmptyBoard()
        val playerInitialRack = ScrabbleEngine.drawTiles(tileBag, 7)
        val aiInitialRack = ScrabbleEngine.drawTiles(tileBag, 7)

        _uiState.update { current ->
            current.copy(
                board = emptyBoard,
                playerRack = playerInitialRack,
                aiRack = aiInitialRack,
                tileBagCount = tileBag.size,
                playerScore = 0,
                aiScore = 0,
                isPlayerTurn = true,
                isAiThinking = false,
                pendingPlacements = emptyMap(),
                selectedRackIndex = null,
                selectedBoardCell = null,
                lastMoveCells = emptySet(),
                validationResult = null,
                isGameOver = false,
                bestWord = "",
                bestWordPoints = 0,
                consecutivePasses = 0,
                blankPickerCell = null,
                showSwapDialog = false,
                showSettingsDialog = false,
                showThemeDialog = false,
                showHistoryDialog = false,
                showStatsDialog = false,
                showGameOverDialog = false,
                definitionWord = null,
                hintMove = null,
                hintsLeft = 3,
                swapsLeft = 3,
                peeksLeft = 3,
                isPeeking = false,
                messageToast = "Game started! Place your first word over the center star (★)"
            )
        }
    }

    fun onRackTileClick(index: Int) {
        val current = _uiState.value
        if (!current.isPlayerTurn || current.isAiThinking) return

        if (current.selectedRackIndex == index) {
            // Deselect
            _uiState.update { it.copy(selectedRackIndex = null) }
        } else {
            _uiState.update { it.copy(selectedRackIndex = index) }
        }
    }

    fun onBoardCellClick(row: Int, col: Int) {
        val state = _uiState.value
        if (!state.isPlayerTurn || state.isAiThinking) return

        val cell = state.board[row][col]
        val pendingTile = state.pendingPlacements[row to col]

        if (pendingTile != null) {
            // Tile was placed this turn -> return it to player's rack
            val newPending = state.pendingPlacements.toMutableMap()
            newPending.remove(row to col)
            val newRack = state.playerRack.toMutableList()
            newRack.add(pendingTile.copy(assignedLetter = null))

            val validation = evaluatePendingPlacements(state.board, newPending)
            _uiState.update {
                it.copy(
                    playerRack = newRack,
                    pendingPlacements = newPending,
                    validationResult = validation,
                    selectedBoardCell = null
                )
            }
            return
        }

        // If cell is already occupied by a permanently played tile, cannot place
        if (cell.tile != null) {
            return
        }

        // If a rack tile is selected, place it here!
        val selectedIdx = state.selectedRackIndex
        if (selectedIdx != null && selectedIdx in state.playerRack.indices) {
            val tileToPlace = state.playerRack[selectedIdx]
            val newRack = state.playerRack.toMutableList()
            newRack.removeAt(selectedIdx)

            if (tileToPlace.isBlank) {
                // Show blank letter picker
                val newPending = state.pendingPlacements.toMutableMap()
                newPending[row to col] = tileToPlace
                _uiState.update {
                    it.copy(
                        playerRack = newRack,
                        pendingPlacements = newPending,
                        selectedRackIndex = null,
                        blankPickerCell = row to col
                    )
                }
            } else {
                val newPending = state.pendingPlacements.toMutableMap()
                newPending[row to col] = tileToPlace
                val validation = evaluatePendingPlacements(state.board, newPending)
                _uiState.update {
                    it.copy(
                        playerRack = newRack,
                        pendingPlacements = newPending,
                        selectedRackIndex = null,
                        validationResult = validation
                    )
                }
            }
        }
    }

    fun onBlankLetterSelected(char: Char) {
        val state = _uiState.value
        val targetCell = state.blankPickerCell ?: return
        val currentTile = state.pendingPlacements[targetCell] ?: return

        val updatedTile = currentTile.copy(assignedLetter = char.uppercaseChar())
        val newPending = state.pendingPlacements.toMutableMap()
        newPending[targetCell] = updatedTile

        val validation = evaluatePendingPlacements(state.board, newPending)
        _uiState.update {
            it.copy(
                pendingPlacements = newPending,
                blankPickerCell = null,
                validationResult = validation
            )
        }
    }

    fun onRecallAll() {
        val state = _uiState.value
        if (state.pendingPlacements.isEmpty()) return

        val newRack = state.playerRack.toMutableList()
        state.pendingPlacements.values.forEach { tile ->
            newRack.add(tile.copy(assignedLetter = null))
        }

        _uiState.update {
            it.copy(
                playerRack = newRack,
                pendingPlacements = emptyMap(),
                selectedRackIndex = null,
                validationResult = null
            )
        }
    }

    fun onShuffleRack() {
        _uiState.update {
            it.copy(playerRack = it.playerRack.shuffled())
        }
    }

    fun onPlayMove() {
        val state = _uiState.value
        val validation = state.validationResult
        if (!state.isPlayerTurn || validation == null || !validation.isValid) return

        val placements = state.pendingPlacements.map { PlacedTile(it.key.first, it.key.second, it.value) }
        val newBoard = Array(ScrabbleEngine.BOARD_SIZE) { r ->
            Array(ScrabbleEngine.BOARD_SIZE) { c ->
                val existing = state.board[r][c]
                val newlyPlaced = state.pendingPlacements[r to c]
                if (newlyPlaced != null) {
                    existing.copy(tile = newlyPlaced)
                } else {
                    existing
                }
            }
        }

        val wordPlayed = validation.primaryWord?.word ?: "WORD"
        val pointsEarned = validation.totalScore
        val newPlayerScore = state.playerScore + pointsEarned

        // Draw new tiles to replenish rack to 7
        val drawn = ScrabbleEngine.drawTiles(tileBag, 7 - state.playerRack.size)
        val replenishedRack = state.playerRack + drawn

        // Record Move Log
        val logEntry = GameLogEntry(
            turnNumber = state.moveLogs.size + 1,
            isPlayer = true,
            word = wordPlayed,
            points = pointsEarned,
            description = "You played \"$wordPlayed\" for $pointsEarned pts" + (if (validation.isBingo) " (Bingo +50!)" else "")
        )

        // Update stats
        val newBestWord = if (pointsEarned > state.bestWordPoints) wordPlayed else state.bestWord
        val newBestWordPts = maxOf(pointsEarned, state.bestWordPoints)
        val updatedStats = state.stats.copy(
            highestScore = maxOf(newPlayerScore, state.stats.highestScore),
            highestScoringWord = if (pointsEarned > state.stats.highestWordPoints) wordPlayed else state.stats.highestScoringWord,
            highestWordPoints = maxOf(pointsEarned, state.stats.highestWordPoints),
            totalWordsPlayed = state.stats.totalWordsPlayed + 1
        )

        val lastCells = state.pendingPlacements.keys

        _uiState.update {
            it.copy(
                board = newBoard,
                playerRack = replenishedRack,
                tileBagCount = tileBag.size,
                playerScore = newPlayerScore,
                pendingPlacements = emptyMap(),
                selectedRackIndex = null,
                validationResult = null,
                lastMoveCells = lastCells,
                moveLogs = listOf(logEntry) + it.moveLogs,
                stats = updatedStats,
                bestWord = newBestWord,
                bestWordPoints = newBestWordPts,
                isPlayerTurn = false,
                consecutivePasses = 0,
                messageToast = "You played $wordPlayed (+$pointsEarned pts)!"
            )
        }

        // Check for game end condition
        if (replenishedRack.isEmpty() && tileBag.isEmpty()) {
            endGame()
            return
        }

        // Trigger AI Turn
        triggerAiTurn()
    }

    fun onPassTurn() {
        val state = _uiState.value
        if (!state.isPlayerTurn) return

        onRecallAll()

        val logEntry = GameLogEntry(
            turnNumber = state.moveLogs.size + 1,
            isPlayer = true,
            word = "",
            points = 0,
            description = "You passed your turn",
            isPassOrSwap = true
        )

        val newPasses = state.consecutivePasses + 1
        _uiState.update {
            it.copy(
                isPlayerTurn = false,
                consecutivePasses = newPasses,
                moveLogs = listOf(logEntry) + it.moveLogs,
                messageToast = "You passed your turn"
            )
        }

        if (newPasses >= 4) {
            endGame()
        } else {
            triggerAiTurn()
        }
    }

    fun onSwapTiles(tilesToSwap: List<Tile>) {
        val state = _uiState.value
        if (!state.isPlayerTurn || tilesToSwap.isEmpty() || tileBag.size < tilesToSwap.size || state.swapsLeft <= 0) return

        onRecallAll()

        val newRack = state.playerRack.toMutableList()
        tilesToSwap.forEach { newRack.remove(it) }
        tileBag.addAll(tilesToSwap)
        tileBag.shuffle()

        val drawn = ScrabbleEngine.drawTiles(tileBag, tilesToSwap.size)
        newRack.addAll(drawn)

        val logEntry = GameLogEntry(
            turnNumber = state.moveLogs.size + 1,
            isPlayer = true,
            word = "",
            points = 0,
            description = "You exchanged ${tilesToSwap.size} tiles",
            isPassOrSwap = true
        )

        _uiState.update {
            it.copy(
                playerRack = newRack,
                tileBagCount = tileBag.size,
                isPlayerTurn = false,
                showSwapDialog = false,
                consecutivePasses = 0,
                swapsLeft = it.swapsLeft - 1,
                moveLogs = listOf(logEntry) + it.moveLogs,
                messageToast = "Exchanged ${tilesToSwap.size} tiles"
            )
        }

        triggerAiTurn()
    }

    fun onGetHint() {
        val state = _uiState.value
        if (!state.isPlayerTurn || state.hintsLeft <= 0) return

        viewModelScope.launch {
            val hint = AiEngine.findHint(state.board, state.playerRack)
            if (hint != null && hint.scoreResult.isValid) {
                val word = hint.scoreResult.primaryWord?.word ?: "word"
                val pts = hint.scoreResult.totalScore
                _uiState.update {
                    it.copy(
                        hintMove = hint,
                        hintsLeft = it.hintsLeft - 1,
                        messageToast = "💡 Hint: Try playing \"$word\" for ~$pts pts!"
                    )
                }
            } else {
                _uiState.update {
                    it.copy(messageToast = "No clear high-scoring play found. Try shuffling or swapping tiles!")
                }
            }
        }
    }

    fun onPeekOpponent() {
        val state = _uiState.value
        if (!state.isPlayerTurn || state.peeksLeft <= 0 || state.isPeeking) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    peeksLeft = it.peeksLeft - 1,
                    isPeeking = true,
                    messageToast = "👁️ Peeking at opponent's tiles..."
                )
            }
            delay(3000) // Keep peeked tiles visible for 3 seconds
            _uiState.update {
                it.copy(isPeeking = false)
            }
        }
    }

    private fun triggerAiTurn() {
        val currentDifficulty = _uiState.value.difficulty
        _uiState.update {
            it.copy(
                isAiThinking = true,
                messageToast = "${currentDifficulty.botName} is thinking..."
            )
        }

        viewModelScope.launch {
            // Natural thinking delay
            delay(1100)

            val state = _uiState.value
            val aiMove = AiEngine.findAiMove(
                board = state.board,
                aiRack = state.aiRack,
                difficulty = state.difficulty,
                customSettings = state.customSettings
            )

            if (aiMove != null && aiMove.scoreResult.isValid) {
                val wordPlayed = aiMove.scoreResult.primaryWord?.word ?: "WORD"
                val pointsEarned = aiMove.scoreResult.totalScore
                val newAiScore = state.aiScore + pointsEarned

                // Sort placements in natural reading order (top-to-bottom, left-to-right)
                val sortedPlacements = aiMove.placements.sortedWith(compareBy({ it.row }, { it.col }))
                val accumulatedAiCells = mutableSetOf<Pair<Int, Int>>()

                // Animate placing tiles one by one with a slow, visually pleasing cadence
                for (placed in sortedPlacements) {
                    accumulatedAiCells.add(placed.row to placed.col)

                    val currentBoard = _uiState.value.board
                    val stepBoard = Array(ScrabbleEngine.BOARD_SIZE) { r ->
                        Array(ScrabbleEngine.BOARD_SIZE) { c ->
                            if (r == placed.row && c == placed.col) {
                                currentBoard[r][c].copy(tile = placed.tile)
                            } else {
                                currentBoard[r][c]
                            }
                        }
                    }

                    _uiState.update {
                        it.copy(
                            board = stepBoard,
                            lastMoveCells = accumulatedAiCells.toSet(),
                            messageToast = "${state.difficulty.botName} placing '${placed.tile.displayLetter}' in $wordPlayed..."
                        )
                    }

                    // Pacing delay per letter placement
                    delay(520)
                }

                // Replenish AI rack
                val newAiRack = state.aiRack.toMutableList()
                aiMove.placements.forEach { placed ->
                    val idx = newAiRack.indexOfFirst {
                        (placed.tile.isBlank && it.isBlank) || (!placed.tile.isBlank && it.letter == placed.tile.letter)
                    }
                    if (idx >= 0) newAiRack.removeAt(idx)
                }
                val drawn = ScrabbleEngine.drawTiles(tileBag, 7 - newAiRack.size)
                newAiRack.addAll(drawn)

                val logEntry = GameLogEntry(
                    turnNumber = state.moveLogs.size + 1,
                    isPlayer = false,
                    word = wordPlayed,
                    points = pointsEarned,
                    description = "${state.difficulty.botName} played \"$wordPlayed\" for $pointsEarned pts"
                )

                val allAiCells = aiMove.placements.map { it.row to it.col }.toSet()

                // Highlight full word completion before handing turn back
                _uiState.update {
                    it.copy(
                        lastMoveCells = allAiCells,
                        messageToast = "✨ ${state.difficulty.botName} completed $wordPlayed (+$pointsEarned pts)!"
                    )
                }

                // Visual appreciation pause for completed word and score
                delay(850)

                _uiState.update {
                    it.copy(
                        aiRack = newAiRack,
                        tileBagCount = tileBag.size,
                        aiScore = newAiScore,
                        isAiThinking = false,
                        isPlayerTurn = true,
                        lastMoveCells = allAiCells,
                        moveLogs = listOf(logEntry) + it.moveLogs,
                        consecutivePasses = 0,
                        messageToast = "${state.difficulty.botName} played $wordPlayed (+$pointsEarned pts)"
                    )
                }

                if (newAiRack.isEmpty() && tileBag.isEmpty()) {
                    endGame()
                }
            } else {
                // AI Passes or Swaps
                if (tileBag.size >= 3) {
                    // AI Swaps
                    _uiState.update {
                        it.copy(messageToast = "${state.difficulty.botName} is exchanging tiles...")
                    }
                    delay(800)

                    val toSwap = state.aiRack.take(3)
                    val newAiRack = state.aiRack.drop(3).toMutableList()
                    tileBag.addAll(toSwap)
                    tileBag.shuffle()
                    val drawn = ScrabbleEngine.drawTiles(tileBag, 3)
                    newAiRack.addAll(drawn)

                    val logEntry = GameLogEntry(
                        turnNumber = state.moveLogs.size + 1,
                        isPlayer = false,
                        word = "",
                        points = 0,
                        description = "${state.difficulty.botName} swapped 3 tiles",
                        isPassOrSwap = true
                    )

                    _uiState.update {
                        it.copy(
                            aiRack = newAiRack,
                            tileBagCount = tileBag.size,
                            isAiThinking = false,
                            isPlayerTurn = true,
                            consecutivePasses = 0,
                            moveLogs = listOf(logEntry) + it.moveLogs,
                            messageToast = "${state.difficulty.botName} swapped 3 tiles"
                        )
                    }
                } else {
                    // AI Passes
                    _uiState.update {
                        it.copy(messageToast = "${state.difficulty.botName} is considering options...")
                    }
                    delay(800)

                    val newPasses = state.consecutivePasses + 1
                    val logEntry = GameLogEntry(
                        turnNumber = state.moveLogs.size + 1,
                        isPlayer = false,
                        word = "",
                        points = 0,
                        description = "${state.difficulty.botName} passed turn",
                        isPassOrSwap = true
                    )

                    _uiState.update {
                        it.copy(
                            isAiThinking = false,
                            isPlayerTurn = true,
                            consecutivePasses = newPasses,
                            moveLogs = listOf(logEntry) + it.moveLogs,
                            messageToast = "${state.difficulty.botName} passed turn"
                        )
                    }

                    if (newPasses >= 4) {
                        endGame()
                    }
                }
            }
        }
    }

    private fun endGame() {
        val state = _uiState.value
        val isWin = state.playerScore > state.aiScore
        val updatedStats = state.stats.copy(
            gamesPlayed = state.stats.gamesPlayed + 1,
            wins = if (isWin) state.stats.wins + 1 else state.stats.wins
        )

        _uiState.update {
            it.copy(
                isGameOver = true,
                showGameOverDialog = true,
                stats = updatedStats
            )
        }
    }

    private fun evaluatePendingPlacements(
        board: Array<Array<BoardCell>>,
        pending: Map<Pair<Int, Int>, Tile>
    ): MoveScoreResult? {
        if (pending.isEmpty()) return null
        val placements = pending.map { PlacedTile(it.key.first, it.key.second, it.value) }
        val isFirst = ScrabbleEngine.isBoardEmpty(board)
        return ScrabbleEngine.evaluateMove(board, placements, isFirst)
    }

    fun setDifficulty(diff: Difficulty, custom: CustomDifficultySettings) {
        _uiState.update {
            it.copy(
                difficulty = diff,
                customSettings = custom,
                showSettingsDialog = false,
                messageToast = "AI set to ${diff.botName}"
            )
        }
    }

    fun setTheme(theme: GameThemePalette) {
        _uiState.update { current ->
            // If user explicitly picks a theme, synchronize themeMode if they were in pure light/dark mode
            val updatedMode = when {
                theme.isDark && current.themeMode == ThemeMode.LIGHT -> ThemeMode.DARK
                !theme.isDark && current.themeMode == ThemeMode.DARK -> ThemeMode.LIGHT
                else -> current.themeMode
            }
            current.copy(
                currentTheme = theme,
                themeMode = updatedMode,
                showThemeDialog = false,
                messageToast = "${theme.themeName} theme applied"
            )
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        _uiState.update { current ->
            val updatedTheme = when (mode) {
                ThemeMode.DARK -> if (!current.currentTheme.isDark) GameThemePalette.MIDNIGHT_OBSIDIAN else current.currentTheme
                ThemeMode.LIGHT -> if (current.currentTheme.isDark) GameThemePalette.BOTANICAL_ZEN else current.currentTheme
                ThemeMode.SYSTEM -> current.currentTheme
            }
            current.copy(
                themeMode = mode,
                currentTheme = updatedTheme,
                messageToast = "Display mode: ${mode.title}"
            )
        }
    }

    fun setDialogVisible(
        swap: Boolean? = null,
        settings: Boolean? = null,
        theme: Boolean? = null,
        history: Boolean? = null,
        stats: Boolean? = null,
        gameOver: Boolean? = null,
        definitionWord: String? = null
    ) {
        _uiState.update {
            it.copy(
                showSwapDialog = swap ?: it.showSwapDialog,
                showSettingsDialog = settings ?: it.showSettingsDialog,
                showThemeDialog = theme ?: it.showThemeDialog,
                showHistoryDialog = history ?: it.showHistoryDialog,
                showStatsDialog = stats ?: it.showStatsDialog,
                showGameOverDialog = gameOver ?: it.showGameOverDialog,
                definitionWord = definitionWord
            )
        }
    }

    fun dismissBlankPicker() {
        // Return blank tile to rack
        val state = _uiState.value
        val targetCell = state.blankPickerCell ?: return
        val tile = state.pendingPlacements[targetCell] ?: return

        val newPending = state.pendingPlacements.toMutableMap()
        newPending.remove(targetCell)
        val newRack = state.playerRack.toMutableList()
        newRack.add(tile.copy(assignedLetter = null))

        _uiState.update {
            it.copy(
                playerRack = newRack,
                pendingPlacements = newPending,
                blankPickerCell = null,
                validationResult = null
            )
        }
    }

    fun clearToast() {
        _uiState.update { it.copy(messageToast = null) }
    }
}
