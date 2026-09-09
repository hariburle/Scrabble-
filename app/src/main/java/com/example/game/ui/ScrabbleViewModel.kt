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
import com.example.game.model.WinConditionType
import com.example.game.model.InspectedWordInfo
import com.example.game.model.TrayWordInfo
import com.example.game.ui.components.GameAudioSynthesizer
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
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
    val lastWordSpanCells: Set<Pair<Int, Int>> = emptySet(),
    val lastPlayedWord: String? = null,
    val lastPlayedScore: Int = 0,
    val lastPlayedIsPlayer: Boolean = true,
    val validationResult: MoveScoreResult? = null,
    val difficulty: Difficulty = Difficulty.BALANCED,
    val customSettings: CustomDifficultySettings = CustomDifficultySettings(),
    val winConditionType: WinConditionType = WinConditionType.CLASSIC,
    val targetScore: Int = 150,
    val maxTurnsPerPlayer: Int = 12,
    val playerTurnCount: Int = 0,
    val aiTurnCount: Int = 0,
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
    val inspectedWordInfo: InspectedWordInfo? = null,
    val trayWordInfo: TrayWordInfo? = null,
    val hintMove: AiCandidateMove? = null,
    val messageToast: String? = null,
    val hintsLeft: Int = 3,
    val swapsLeft: Int = 3,
    val peeksLeft: Int = 3,
    val isPeeking: Boolean = false,
    val celebrationWord: String? = null,
    val celebrationPoints: Int = 0,
    val celebrationIsBingo: Boolean = false,
    val isSnapModeActive: Boolean = false,
    val snapSelectedCell: Pair<Int, Int>? = null,
    val snapPreviewIsHorizontal: Boolean = true,
    val snapWordTray: List<Tile> = emptyList(),
    val fullRackBeforeSnap: List<Tile> = emptyList(),
    val isSoundEnabled: Boolean = true,
    val isTimedModeEnabled: Boolean = false,
    val turnTimeLimitSeconds: Int = 120,
    val playerTimeLeftSeconds: Int = 120,
    val showDebugDialog: Boolean = false,
    val lastErrorLog: String? = null,
    val debugLogs: List<String> = emptyList()
)

class ScrabbleViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ScrabbleUiState())
    val uiState: StateFlow<ScrabbleUiState> = _uiState.asStateFlow()

    private var tileBag = mutableListOf<Tile>()
    private var timerJob: kotlinx.coroutines.Job? = null
    private var wordInspectionDismissJob: kotlinx.coroutines.Job? = null

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
                trayWordInfo = computeTrayWordInfo(playerInitialRack),
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
                isSnapModeActive = false,
                snapSelectedCell = null,
                fullRackBeforeSnap = emptyList(),
                messageToast = "Game started! Place your first word over the center star (★)"
            )
        }
        startTurnTimer()
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

        if (state.isSnapModeActive) {
            updateSnapPreview(row, col, state.snapPreviewIsHorizontal)
            return
        }

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
                    selectedBoardCell = null,
                    trayWordInfo = computeTrayWordInfo(newRack, newPending.isNotEmpty())
                )
            }
            return
        }

        // If cell is already occupied by a permanently played tile, inspect the word on board
        if (cell.tile != null) {
            inspectTileAt(row, col)
            return
        }

        // If an empty cell is tapped and there was an active word inspection, dismiss inspection
        if (state.inspectedWordInfo != null) {
            _uiState.update { it.copy(inspectedWordInfo = null) }
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
                        validationResult = validation,
                        trayWordInfo = computeTrayWordInfo(newRack, newPending.isNotEmpty())
                    )
                }
            }
        }
    }

    fun onDropTileOnBoard(rackIndex: Int, row: Int, col: Int) {
        val state = _uiState.value
        if (!state.isPlayerTurn || state.isAiThinking) return

        val cell = state.board[row][col]
        if (cell.tile != null || state.pendingPlacements.containsKey(row to col)) return

        if (rackIndex in state.playerRack.indices) {
            val tileToPlace = state.playerRack[rackIndex]
            val newRack = state.playerRack.toMutableList()
            newRack.removeAt(rackIndex)

            if (tileToPlace.isBlank) {
                val newPending = state.pendingPlacements.toMutableMap()
                newPending[row to col] = tileToPlace
                _uiState.update {
                    it.copy(
                        playerRack = newRack,
                        pendingPlacements = newPending,
                        selectedRackIndex = null,
                        blankPickerCell = row to col,
                        trayWordInfo = computeTrayWordInfo(newRack, newPending.isNotEmpty())
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
                        validationResult = validation,
                        trayWordInfo = computeTrayWordInfo(newRack, newPending.isNotEmpty())
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
        if (state.isSnapModeActive) {
            cancelSnapMode()
            return
        }
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
                validationResult = null,
                trayWordInfo = computeTrayWordInfo(newRack)
            )
        }
    }

    fun onShuffleRack() {
        _uiState.update {
            val shuffled = it.playerRack.shuffled()
            it.copy(
                playerRack = shuffled,
                trayWordInfo = computeTrayWordInfo(shuffled)
            )
        }
    }

    fun onRearrangePlayerRack(fromIndex: Int, toIndex: Int) {
        val current = _uiState.value.playerRack
        if (fromIndex in current.indices && toIndex in current.indices) {
            val updated = current.toMutableList()
            val tile = updated.removeAt(fromIndex)
            updated.add(toIndex, tile)
            _uiState.update {
                // If the selected index was one of the rearranged, keep selection with the tile
                val selectedIdx = it.selectedRackIndex
                val nextSelected = if (selectedIdx == null) {
                    null
                } else {
                    when (selectedIdx) {
                        fromIndex -> toIndex
                        in minOf(fromIndex, toIndex)..maxOf(fromIndex, toIndex) -> {
                            // Shift other indexes
                            val dir = if (toIndex > fromIndex) -1 else 1
                            selectedIdx + dir
                        }
                        else -> selectedIdx
                    }
                }
                it.copy(
                    playerRack = updated,
                    selectedRackIndex = nextSelected,
                    trayWordInfo = null // Suppress dictionary validation during motion
                )
            }
        }
    }

    fun onRackDragFinished() {
        _uiState.update {
            it.copy(
                trayWordInfo = computeTrayWordInfo(it.playerRack, it.pendingPlacements.isNotEmpty())
            )
        }
    }

    fun computeTrayWordInfo(rack: List<Tile>, hasPendingPlacements: Boolean = false): TrayWordInfo? {
        if (hasPendingPlacements || rack.size < 2 || !WordDictionary.isInitialized) return null

        var bestWord: TrayWordInfo? = null

        // Search from longest sub-sequence down to 2
        for (len in rack.size downTo 2) {
            for (i in 0..(rack.size - len)) {
                val j = i + len - 1
                val subList = rack.subList(i, j + 1)

                val unassignedBlankCount = subList.count { it.isBlank && it.assignedLetter == null }
                if (unassignedBlankCount == 0) {
                    val str = subList.map { it.displayLetter }.joinToString("").uppercase()
                    if (WordDictionary.isValidWord(str)) {
                        val pts = subList.sumOf { it.points }
                        val info = TrayWordInfo(word = str, totalScore = pts, startIndex = i, endIndex = j)
                        if (bestWord == null || info.word.length > bestWord.word.length || (info.word.length == bestWord.word.length && info.totalScore > bestWord.totalScore)) {
                            bestWord = info
                        }
                    }
                } else if (unassignedBlankCount == 1) {
                    val blankIndex = subList.indexOfFirst { it.isBlank && it.assignedLetter == null }
                    for (ch in 'A'..'Z') {
                        val candidateStr = subList.mapIndexed { idx, tile ->
                            if (idx == blankIndex) ch else tile.displayLetter
                        }.joinToString("").uppercase()

                        if (WordDictionary.isValidWord(candidateStr)) {
                            val pts = subList.sumOf { it.points }
                            val info = TrayWordInfo(word = candidateStr, totalScore = pts, startIndex = i, endIndex = j)
                            if (bestWord == null || info.word.length > bestWord.word.length || (info.word.length == bestWord.word.length && info.totalScore > bestWord.totalScore)) {
                                bestWord = info
                                break
                            }
                        }
                    }
                }
            }
            if (bestWord != null) {
                return bestWord
            }
        }

        return bestWord
    }

    private fun computeWordSpanCells(formedWords: List<com.example.game.model.FormedWord>): Set<Pair<Int, Int>> {
        val cells = mutableSetOf<Pair<Int, Int>>()
        for (fw in formedWords) {
            for (i in fw.word.indices) {
                val r = if (fw.isHorizontal) fw.startRow else fw.startRow + i
                val c = if (fw.isHorizontal) fw.startCol + i else fw.startCol
                cells.add(r to c)
            }
        }
        return cells
    }

    fun onPlayMove() {
        val state = _uiState.value
        val validation = state.validationResult
        if (!state.isPlayerTurn || validation == null || !validation.isValid) return

        try {
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

            val allWords = validation.allWords
            val wordPlayed = validation.primaryWord?.word
                ?: validation.crossWords.maxByOrNull { it.points }?.word
                ?: validation.crossWords.firstOrNull()?.word
                ?: "WORD"
            val secondaryWords = allWords.map { it.word }.filter { it != wordPlayed }
            val allFormedWords = allWords.map { it.word }
            val pointsEarned = validation.totalScore
            val newPlayerScore = state.playerScore + pointsEarned
            val newPlayerTurnCount = state.playerTurnCount + 1

            // Safely resolve the remaining rack regardless of whether move was submitted via Snap Mode or Drag/Drop
            val placedTileIds = state.pendingPlacements.values.map { it.id }.toSet()
            val fullRack = if (state.fullRackBeforeSnap.isNotEmpty()) state.fullRackBeforeSnap else state.playerRack
            val remainingRack = fullRack.filter { it.id !in placedTileIds }
            val tilesNeeded = (7 - remainingRack.size).coerceIn(0, 7)
            val drawn = ScrabbleEngine.drawTiles(tileBag, tilesNeeded)
            val replenishedRack = remainingRack + drawn

            val lastCells = state.pendingPlacements.keys.toSet()
            val fullSpanCells = computeWordSpanCells(allWords).ifEmpty { lastCells }

            // Record Move Log
            val logEntry = GameLogEntry(
                turnNumber = state.moveLogs.size + 1,
                isPlayer = true,
                word = wordPlayed,
                points = pointsEarned,
                description = "You played \"$wordPlayed\" for $pointsEarned pts" + (if (validation.isBingo) " (Bingo +50!)" else ""),
                secondaryWords = secondaryWords,
                placedCoordinates = lastCells,
                allFormedWords = allFormedWords
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

            val isBingo = validation.isBingo
            val isMega = pointsEarned >= 40 || isBingo
            val isHighScoreWord = pointsEarned >= 15

            try {
                if (isMega) {
                    com.example.game.ui.components.GameAudioSynthesizer.playMegaScorePlay()
                } else if (pointsEarned >= 20) {
                    com.example.game.ui.components.GameAudioSynthesizer.playHighScorePlay()
                } else {
                    com.example.game.ui.components.GameAudioSynthesizer.playNormalPlay()
                }
            } catch (audioErr: Throwable) {
                // Audio synthesis should never interrupt game logic
            }

            if (isHighScoreWord) {
                viewModelScope.launch {
                    delay(6000)
                    clearCelebration()
                }
            }

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
                    lastWordSpanCells = fullSpanCells,
                    lastPlayedWord = wordPlayed,
                    lastPlayedScore = pointsEarned,
                    lastPlayedIsPlayer = true,
                    playerTurnCount = newPlayerTurnCount,
                    moveLogs = listOf(logEntry) + it.moveLogs,
                    stats = updatedStats,
                    bestWord = newBestWord,
                    bestWordPoints = newBestWordPts,
                    isPlayerTurn = false,
                    consecutivePasses = 0,
                    isSnapModeActive = false,
                    fullRackBeforeSnap = emptyList(),
                    snapWordTray = emptyList(),
                    snapSelectedCell = null,
                    messageToast = "You played \"$wordPlayed\" (+$pointsEarned pts)!",
                    celebrationWord = if (isHighScoreWord) wordPlayed else null,
                    celebrationPoints = if (isHighScoreWord) pointsEarned else 0,
                    celebrationIsBingo = if (isHighScoreWord) isBingo else false
                )
            }

            // Check for game end condition
            if (checkWinConditions(newPlayerScore, state.aiScore, newPlayerTurnCount, state.aiTurnCount, replenishedRack.isEmpty())) {
                return
            }

            // Trigger AI Turn
            triggerAiTurn()
        } catch (e: Throwable) {
            logDebugError("Crash in onPlayMove: ${e.message}", e)
            _uiState.update {
                it.copy(
                    messageToast = "⚠️ Recovered from move error: ${e.message ?: "State safely restored"}",
                    isPlayerTurn = true,
                    isAiThinking = false
                )
            }
        }
    }

    private fun checkWinConditions(
        playerScore: Int,
        aiScore: Int,
        playerTurns: Int,
        aiTurns: Int,
        playerRackEmpty: Boolean
    ): Boolean {
        val state = _uiState.value
        when (state.winConditionType) {
            WinConditionType.TARGET_SCORE -> {
                if (playerScore >= state.targetScore || aiScore >= state.targetScore) {
                    endGame()
                    return true
                }
            }
            WinConditionType.TURN_LIMIT -> {
                if (playerTurns >= state.maxTurnsPerPlayer && aiTurns >= state.maxTurnsPerPlayer) {
                    endGame()
                    return true
                }
            }
            WinConditionType.CLASSIC -> {
                if (tileBag.isEmpty() && (playerRackEmpty || state.aiRack.isEmpty())) {
                    endGame()
                    return true
                }
            }
        }
        return false
    }

    private fun logDebugError(message: String, throwable: Throwable? = null) {
        android.util.Log.e("ScrabbleViewModel", message, throwable)
        val stackTraceStr = throwable?.stackTraceToString() ?: ""
        val fullLog = "$message\n$stackTraceStr"
        _uiState.update {
            it.copy(
                lastErrorLog = fullLog,
                debugLogs = (it.debugLogs + message).takeLast(50)
            )
        }
    }

    fun onPassTurn() {
        val state = _uiState.value
        if (!state.isPlayerTurn) return

        onRecallAll()

        val newPasses = state.consecutivePasses + 1
        val newPlayerTurnCount = state.playerTurnCount + 1
        val logEntry = GameLogEntry(
            turnNumber = state.moveLogs.size + 1,
            isPlayer = true,
            word = "",
            points = 0,
            description = "You passed your turn",
            isPassOrSwap = true
        )

        _uiState.update {
            it.copy(
                isPlayerTurn = false,
                playerTurnCount = newPlayerTurnCount,
                consecutivePasses = newPasses,
                moveLogs = listOf(logEntry) + it.moveLogs,
                messageToast = "You passed your turn"
            )
        }

        if (newPasses >= 4 || checkWinConditions(state.playerScore, state.aiScore, newPlayerTurnCount, state.aiTurnCount, false)) {
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

        val newPlayerTurnCount = state.playerTurnCount + 1
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
                playerTurnCount = newPlayerTurnCount,
                isPlayerTurn = false,
                showSwapDialog = false,
                consecutivePasses = 0,
                swapsLeft = it.swapsLeft - 1,
                moveLogs = listOf(logEntry) + it.moveLogs,
                messageToast = "Exchanged ${tilesToSwap.size} tiles"
            )
        }

        if (checkWinConditions(state.playerScore, state.aiScore, newPlayerTurnCount, state.aiTurnCount, false)) {
            endGame()
        } else {
            triggerAiTurn()
        }
    }

    fun onGetHint() {
        val state = _uiState.value
        if (!state.isPlayerTurn || state.hintsLeft <= 0) return

        viewModelScope.launch {
            val hint = AiEngine.findHint(state.board, state.playerRack)
            if (hint != null && hint.scoreResult.isValid) {
                val word = hint.scoreResult.primaryWord?.word
                    ?: hint.scoreResult.crossWords.firstOrNull()?.word
                    ?: "word"
                val pts = hint.scoreResult.totalScore
                _uiState.update {
                    it.copy(
                        hintMove = hint,
                        hintsLeft = it.hintsLeft - 1,
                        messageToast = "💡 Hint: Try playing \"$word\" for ~$pts pts!"
                    )
                }
                // Brief placement duration: 6 seconds before clearing from board
                kotlinx.coroutines.delay(6000)
                _uiState.update {
                    if (it.hintMove == hint) {
                        it.copy(hintMove = null)
                    } else {
                        it
                    }
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
        stopTurnTimer()
        val currentDifficulty = _uiState.value.difficulty
        _uiState.update {
            it.copy(
                isAiThinking = true,
                messageToast = "${currentDifficulty.botName} is thinking..."
            )
        }

        viewModelScope.launch {
            try {
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
                    val allWords = aiMove.scoreResult.allWords
                    val wordPlayed = aiMove.scoreResult.primaryWord?.word
                        ?: aiMove.scoreResult.crossWords.maxByOrNull { it.points }?.word
                        ?: aiMove.scoreResult.crossWords.firstOrNull()?.word
                        ?: "WORD"
                    val secondaryWords = allWords.map { it.word }.filter { it != wordPlayed }
                    val allFormedWords = allWords.map { it.word }
                    val pointsEarned = aiMove.scoreResult.totalScore
                    val newAiScore = state.aiScore + pointsEarned
                    val newAiTurnCount = state.aiTurnCount + 1

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

                    val allAiCells = aiMove.placements.map { it.row to it.col }.toSet()
                    val fullSpanCells = computeWordSpanCells(allWords).ifEmpty { allAiCells }

                    val logEntry = GameLogEntry(
                        turnNumber = state.moveLogs.size + 1,
                        isPlayer = false,
                        word = wordPlayed,
                        points = pointsEarned,
                        description = "${state.difficulty.botName} played \"$wordPlayed\" for $pointsEarned pts",
                        secondaryWords = secondaryWords,
                        placedCoordinates = allAiCells,
                        allFormedWords = allFormedWords
                    )

                    // Highlight full word completion before handing turn back
                    _uiState.update {
                        it.copy(
                            lastMoveCells = allAiCells,
                            lastWordSpanCells = fullSpanCells,
                            lastPlayedWord = wordPlayed,
                            lastPlayedScore = pointsEarned,
                            lastPlayedIsPlayer = false,
                            aiTurnCount = newAiTurnCount,
                            messageToast = "✨ ${state.difficulty.botName} completed \"$wordPlayed\" (+$pointsEarned pts)!"
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
                            lastWordSpanCells = fullSpanCells,
                            moveLogs = listOf(logEntry) + it.moveLogs,
                            consecutivePasses = 0,
                            messageToast = "${state.difficulty.botName} played \"$wordPlayed\" (+$pointsEarned pts)"
                        )
                    }

                    if (checkWinConditions(state.playerScore, newAiScore, state.playerTurnCount, newAiTurnCount, state.playerRack.isEmpty())) {
                        return@launch
                    }
                } else {
                    // AI Passes or Swaps
                    val newAiTurnCount = state.aiTurnCount + 1
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
                                aiTurnCount = newAiTurnCount,
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
                                aiTurnCount = newAiTurnCount,
                                consecutivePasses = newPasses,
                                moveLogs = listOf(logEntry) + it.moveLogs,
                                messageToast = "${state.difficulty.botName} passed turn"
                            )
                        }

                        if (newPasses >= 4 || checkWinConditions(state.playerScore, state.aiScore, state.playerTurnCount, newAiTurnCount, false)) {
                            endGame()
                            return@launch
                        }
                    }
                }
                onTurnTransitionToPlayer()
            } catch (e: Throwable) {
                logDebugError("AI turn crashed: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        isAiThinking = false,
                        isPlayerTurn = true,
                        messageToast = "⚠️ Bot encountered a temporary issue. Your turn."
                    )
                }
            }
        }
    }

    fun inspectTileAt(row: Int, col: Int) {
        wordInspectionDismissJob?.cancel()
        val state = _uiState.value
        val cell = state.board[row][col]
        if (cell.tile == null) {
            _uiState.update { it.copy(inspectedWordInfo = null) }
            return
        }

        // Find horizontal word passing through (row, col)
        var hStart = col
        while (hStart > 0 && state.board[row][hStart - 1].tile != null) {
            hStart--
        }
        var hEnd = col
        while (hEnd < ScrabbleEngine.BOARD_SIZE - 1 && state.board[row][hEnd + 1].tile != null) {
            hEnd++
        }
        val hWord = if (hEnd > hStart) {
            (hStart..hEnd).map { state.board[row][it].tile!!.displayLetter }.joinToString("")
        } else null

        // Find vertical word passing through (row, col)
        var vStart = row
        while (vStart > 0 && state.board[vStart - 1][col].tile != null) {
            vStart--
        }
        var vEnd = row
        while (vEnd < ScrabbleEngine.BOARD_SIZE - 1 && state.board[vEnd + 1][col].tile != null) {
            vEnd++
        }
        val vWord = if (vEnd > vStart) {
            (vStart..vEnd).map { state.board[it][col].tile!!.displayLetter }.joinToString("")
        } else null

        val chosenWord = hWord ?: vWord
        if (chosenWord != null) {
            val coords = if (chosenWord == hWord) {
                (hStart..hEnd).map { row to it }
            } else {
                (vStart..vEnd).map { it to col }
            }
            var rawPts = 0
            for (ch in chosenWord) {
                rawPts += com.example.game.model.getLetterPoints(ch)
            }
            val def = WordDictionary.getDefinition(chosenWord)
            _uiState.update {
                it.copy(
                    inspectedWordInfo = InspectedWordInfo(
                        word = chosenWord,
                        totalScore = rawPts,
                        definition = def,
                        coordinates = coords
                    ),
                    lastMoveCells = coords.toSet(),
                    lastWordSpanCells = coords.toSet()
                )
            }

            // Fetch actual dictionary definition from open Free Dictionary API in the background
            viewModelScope.launch(Dispatchers.IO) {
                val onlineDef = fetchOnlineDefinition(chosenWord)
                if (onlineDef != null) {
                    val formattedDef = "$onlineDef (Definition retrieved in real-time)."
                    _uiState.update { current ->
                        if (current.inspectedWordInfo?.word == chosenWord) {
                            current.copy(
                                inspectedWordInfo = current.inspectedWordInfo.copy(definition = formattedDef)
                            )
                        } else current
                    }
                }
            }
        } else {
            // Single standalone tile or no full word
            val letter = cell.tile.displayLetter
            val pts = cell.tile.points
            _uiState.update {
                it.copy(
                    inspectedWordInfo = InspectedWordInfo(
                        word = letter.toString(),
                        totalScore = pts,
                        definition = "Tile '$letter' ($pts pts)",
                        coordinates = listOf(row to col)
                    ),
                    lastMoveCells = setOf(row to col),
                    lastWordSpanCells = setOf(row to col)
                )
            }
        }
        wordInspectionDismissJob = viewModelScope.launch {
            delay(6000)
            dismissWordInspection()
        }
    }

    private fun fetchOnlineDefinition(word: String): String? {
        return try {
            val url = java.net.URL("https://api.dictionaryapi.dev/api/v2/entries/en/${word.lowercase()}")
            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 3000
            connection.readTimeout = 3000
            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val regex = "\"definition\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"".toRegex()
                val match = regex.find(response)
                if (match != null) {
                    match.groupValues[1].replace("\\\"", "\"").replace("\\'", "'")
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Throwable) {
            null
        }
    }

    fun clearInspectedWord() {
        _uiState.update { it.copy(inspectedWordInfo = null) }
    }

    fun dismissWordInspection() {
        clearInspectedWord()
    }

    fun highlightLogMove(entry: GameLogEntry) {
        if (entry.placedCoordinates.isNotEmpty()) {
            _uiState.update {
                it.copy(
                    lastMoveCells = entry.placedCoordinates,
                    lastWordSpanCells = entry.placedCoordinates,
                    showHistoryDialog = false,
                    messageToast = "📍 Showing ${if (entry.isPlayer) "Your" else "AI's"} play: ${entry.word} (+${entry.points} pts)"
                )
            }
        }
    }

    fun highlightMoveFromLog(entry: GameLogEntry) {
        highlightLogMove(entry)
    }

    fun concedeOrEndGame() {
        endGame()
    }

    fun setWinCondition(type: WinConditionType, target: Int = 150, maxTurns: Int = 12) {
        _uiState.update {
            it.copy(
                winConditionType = type,
                targetScore = target,
                maxTurnsPerPlayer = maxTurns,
                messageToast = "Mode: ${type.title}"
            )
        }
    }

    private fun endGame() {
        stopTurnTimer()
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

    fun clearCelebration() {
        _uiState.update {
            it.copy(
                celebrationWord = null,
                celebrationPoints = 0,
                celebrationIsBingo = false
            )
        }
    }

    // --- Turn Timer Logic ---
    private fun startTurnTimer() {
        timerJob?.cancel()
        val state = _uiState.value
        if (!state.isTimedModeEnabled || !state.isPlayerTurn || state.isGameOver) {
            return
        }

        _uiState.update { it.copy(playerTimeLeftSeconds = it.turnTimeLimitSeconds) }

        timerJob = viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(1000)
                val current = _uiState.value
                if (current.playerTimeLeftSeconds > 1) {
                    _uiState.update { it.copy(playerTimeLeftSeconds = it.playerTimeLeftSeconds - 1) }
                } else {
                    _uiState.update { it.copy(playerTimeLeftSeconds = 0, messageToast = "⏳ Time's up! Your turn passed.") }
                    onPassTurn()
                    break
                }
            }
        }
    }

    private fun stopTurnTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    private fun onTurnTransitionToPlayer() {
        startTurnTimer()
    }

    // --- Snap Word Mode Logic ---
    fun toggleSnapMode() {
        val state = _uiState.value
        if (!state.isPlayerTurn || state.isAiThinking) return

        if (state.isSnapModeActive) {
            cancelSnapMode()
        } else {
            val fullPlayerRack = if (state.pendingPlacements.isNotEmpty()) {
                val restored = state.playerRack.toMutableList()
                state.pendingPlacements.values.forEach { tile ->
                    restored.add(tile.copy(assignedLetter = null))
                }
                restored
            } else {
                state.playerRack
            }

            _uiState.update {
                it.copy(
                    isSnapModeActive = true,
                    fullRackBeforeSnap = fullPlayerRack,
                    playerRack = fullPlayerRack,
                    snapWordTray = fullPlayerRack,
                    snapSelectedCell = 7 to 7,
                    snapPreviewIsHorizontal = true,
                    messageToast = "🧩 Snap Word: Tap tiles to pick only letters you want, then tap starting cell on board!"
                )
            }
            updateSnapPreview(7, 7, true)
        }
    }

    fun cancelSnapMode() {
        val state = _uiState.value
        val fullRack = if (state.fullRackBeforeSnap.isNotEmpty()) state.fullRackBeforeSnap else state.playerRack
        _uiState.update {
            it.copy(
                isSnapModeActive = false,
                playerRack = fullRack,
                fullRackBeforeSnap = emptyList(),
                snapWordTray = emptyList(),
                pendingPlacements = emptyMap(),
                snapSelectedCell = null,
                validationResult = null,
                messageToast = "Snap mode cancelled"
            )
        }
    }

    fun onSnapTileToggle(tile: Tile) {
        val state = _uiState.value
        if (!state.isSnapModeActive) return

        val currentTray = state.snapWordTray.toMutableList()
        val existingIndex = currentTray.indexOfFirst { it.id == tile.id }
        if (existingIndex != -1) {
            currentTray.removeAt(existingIndex)
        } else {
            currentTray.add(tile)
        }
        _uiState.update { it.copy(snapWordTray = currentTray) }
        val cell = state.snapSelectedCell ?: (7 to 7)
        updateSnapPreview(cell.first, cell.second, state.snapPreviewIsHorizontal)
    }

    fun snapSelectAll() {
        val state = _uiState.value
        if (!state.isSnapModeActive) return
        val fullRack = if (state.fullRackBeforeSnap.isNotEmpty()) state.fullRackBeforeSnap else state.playerRack
        _uiState.update { it.copy(snapWordTray = fullRack) }
        val cell = state.snapSelectedCell ?: (7 to 7)
        updateSnapPreview(cell.first, cell.second, state.snapPreviewIsHorizontal)
    }

    fun snapClearSelection() {
        val state = _uiState.value
        if (!state.isSnapModeActive) return
        val fullRack = if (state.fullRackBeforeSnap.isNotEmpty()) state.fullRackBeforeSnap else state.playerRack
        _uiState.update {
            it.copy(
                snapWordTray = emptyList(),
                pendingPlacements = emptyMap(),
                playerRack = fullRack,
                validationResult = null
            )
        }
    }

    fun nudgeSnap(dRow: Int, dCol: Int) {
        val state = _uiState.value
        if (!state.isSnapModeActive) return
        val currentCell = state.snapSelectedCell ?: (7 to 7)
        val nextRow = (currentCell.first + dRow).coerceIn(0, ScrabbleEngine.BOARD_SIZE - 1)
        val nextCol = (currentCell.second + dCol).coerceIn(0, ScrabbleEngine.BOARD_SIZE - 1)
        updateSnapPreview(nextRow, nextCol, state.snapPreviewIsHorizontal)
    }

    fun updateSnapPreview(row: Int, col: Int, isHorizontal: Boolean) {
        val state = _uiState.value
        if (!state.isPlayerTurn || state.isAiThinking || !state.isSnapModeActive) return

        val fullRack = if (state.fullRackBeforeSnap.isNotEmpty()) state.fullRackBeforeSnap else state.playerRack
        val wordTiles = state.snapWordTray

        if (wordTiles.isEmpty()) {
            _uiState.update {
                it.copy(
                    snapSelectedCell = row to col,
                    snapPreviewIsHorizontal = isHorizontal,
                    pendingPlacements = emptyMap(),
                    playerRack = fullRack,
                    validationResult = null
                )
            }
            return
        }

        val tempPending = mutableMapOf<Pair<Int, Int>, Tile>()
        val tilesToPlace = wordTiles.toMutableList()

        var curRow = row
        var curCol = col
        var tileIdx = 0

        while (tileIdx < tilesToPlace.size && curRow in 0 until ScrabbleEngine.BOARD_SIZE && curCol in 0 until ScrabbleEngine.BOARD_SIZE) {
            val cell = state.board[curRow][curCol]
            if (cell.tile != null) {
                // Existing tile on board, pass over it (word bridges through it)
                if (isHorizontal) curCol++ else curRow++
            } else {
                val tile = tilesToPlace[tileIdx]
                tempPending[curRow to curCol] = tile
                tileIdx++
                if (isHorizontal) curCol++ else curRow++
            }
        }

        val placedTileIds = tempPending.values.map { it.id }.toSet()
        val remainingRack = fullRack.filter { it.id !in placedTileIds }
        val validation = evaluatePendingPlacements(state.board, tempPending)

        _uiState.update {
            it.copy(
                snapSelectedCell = row to col,
                snapPreviewIsHorizontal = isHorizontal,
                pendingPlacements = tempPending,
                playerRack = remainingRack,
                validationResult = validation
            )
        }
    }

    fun confirmSnapPlay() {
        val state = _uiState.value
        if (!state.isPlayerTurn || state.isAiThinking || !state.isSnapModeActive) return

        val fullRack = if (state.fullRackBeforeSnap.isNotEmpty()) state.fullRackBeforeSnap else state.playerRack
        val placedTileIds = state.pendingPlacements.values.map { it.id }.toSet()
        val remainingRack = fullRack.filter { it.id !in placedTileIds }

        val blankCellWithoutLetter = state.pendingPlacements.entries.firstOrNull {
            it.value.isBlank && it.value.assignedLetter == null
        }?.key

        _uiState.update {
            it.copy(
                isSnapModeActive = false,
                playerRack = remainingRack,
                snapSelectedCell = null,
                snapWordTray = emptyList(),
                fullRackBeforeSnap = emptyList(),
                blankPickerCell = blankCellWithoutLetter,
                messageToast = if (state.pendingPlacements.isNotEmpty()) "✨ Word placed! Tap PLAY to submit." else "Snap mode closed"
            )
        }
    }

    fun toggleSnapDirection() {
        val state = _uiState.value
        val cell = state.snapSelectedCell ?: (7 to 7)
        updateSnapPreview(cell.first, cell.second, !state.snapPreviewIsHorizontal)
    }

    fun toggleDebugDialog(visible: Boolean) {
        _uiState.update { it.copy(showDebugDialog = visible) }
    }

    // --- Settings / Sound Config ---
    fun toggleSoundEffects(enabled: Boolean) {
        GameAudioSynthesizer.isSoundEnabled = enabled
        _uiState.update { it.copy(isSoundEnabled = enabled) }
    }

    fun toggleTimedMode(enabled: Boolean, timeLimitSeconds: Int = 120) {
        _uiState.update {
            it.copy(
                isTimedModeEnabled = enabled,
                turnTimeLimitSeconds = timeLimitSeconds
            )
        }
        if (enabled && _uiState.value.isPlayerTurn) {
            startTurnTimer()
        } else {
            stopTurnTimer()
        }
    }
}
