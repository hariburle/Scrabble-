package com.example.game.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.game.model.GameThemePalette
import com.example.game.model.ThemeMode
import com.example.game.ui.components.BlankTilePickerDialog
import com.example.game.ui.components.DifficultySettingsDialog
import com.example.game.ui.components.GameOverDialog
import com.example.game.ui.components.GameStatsDialog
import com.example.game.ui.components.MoveHistoryDialog
import com.example.game.ui.components.PlayerRackView
import com.example.game.ui.components.RackActionButtons
import com.example.game.ui.components.RackStatusPill
import com.example.game.ui.components.RackTileTray
import com.example.game.ui.components.ScrabbleBoardView
import com.example.game.ui.components.ScrabbleScoreCards
import com.example.game.ui.components.ScrabbleTopAppBar
import com.example.game.ui.components.ScoreHeader
import com.example.game.ui.components.ThemePickerDialog
import com.example.game.ui.components.TileSwapDialog
import com.example.game.ui.components.WordDefinitionDialog
import com.example.game.ui.components.WordInspectionOverlayCard
import com.example.game.ui.components.CelebrationBanner

@Composable
fun ScrabbleScreen(
    viewModel: ScrabbleViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val systemInDark = isSystemInDarkTheme()

    // Determine active palette based on user selected ThemeMode and Palette
    val palette = remember(state.currentTheme, state.themeMode, systemInDark) {
        if (state.themeMode == ThemeMode.SYSTEM) {
            if (systemInDark && !state.currentTheme.isDark) {
                when (state.currentTheme) {
                    GameThemePalette.BOTANICAL_ZEN -> GameThemePalette.DEEP_FOREST_DARK
                    GameThemePalette.PASTEL_SUNSET -> GameThemePalette.AMETHYST_NIGHT
                    GameThemePalette.WARM_WALNUT -> GameThemePalette.COZY_ESPRESSO_DARK
                    else -> GameThemePalette.MIDNIGHT_OBSIDIAN
                }
            } else if (!systemInDark && state.currentTheme.isDark) {
                when (state.currentTheme) {
                    GameThemePalette.DEEP_FOREST_DARK -> GameThemePalette.BOTANICAL_ZEN
                    GameThemePalette.AMETHYST_NIGHT -> GameThemePalette.PASTEL_SUNSET
                    GameThemePalette.COZY_ESPRESSO_DARK -> GameThemePalette.WARM_WALNUT
                    else -> GameThemePalette.NORDIC_SLATE
                }
            } else {
                state.currentTheme
            }
        } else {
            state.currentTheme
        }
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var isDraggingTile by remember { mutableStateOf(false) }
    var draggedTileInfo by remember { mutableStateOf<Triple<Int, com.example.game.model.Tile, Offset>?>(null) }
    val cellBoundsMap = remember { mutableStateMapOf<Pair<Int, Int>, Rect>() }

    var boardScale by remember { mutableStateOf(1.0f) }
    var boardOffset by remember { mutableStateOf(Offset.Zero) }

    val hoveredCell = draggedTileInfo?.let { (_, _, pos) ->
        val halfSizePx = with(LocalDensity.current) { 22.dp.toPx() }
        val tileCenter = pos + Offset(halfSizePx, halfSizePx)
        cellBoundsMap.entries.firstOrNull { (_, bounds) ->
            bounds.contains(tileCenter)
        }?.key
    }

    val isDragging = draggedTileInfo != null
    val dragScale by animateFloatAsState(
        targetValue = if (isDragging) 1.25f else 1.0f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 320f),
        label = "dragScale"
    )
    val dragRotation by animateFloatAsState(
        targetValue = if (isDragging) 6f else 0f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 250f),
        label = "dragRotation"
    )
    val dragElevation by animateFloatAsState(
        targetValue = if (isDragging) 16f else 0f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 300f),
        label = "dragElevation"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        palette.backgroundGradientTop,
                        palette.backgroundGradientBottom
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("scrabble_screen_root")
    ) {
        if (isLandscape) {
            // Landscape Layout: 15x15 Board Hero on Left, Full Game Controls & Rack Sidebar on Right
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Side: Scrabble Board (Maximized square board)
                Box(
                    modifier = Modifier
                        .weight(1.15f)
                        .fillMaxHeight()
                        .padding(end = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ScrabbleBoardView(
                        board = state.board,
                        pendingPlacements = state.pendingPlacements,
                        lastMoveCells = state.lastMoveCells,
                        selectedCell = state.selectedBoardCell,
                        palette = palette,
                        onCellClick = { r, c -> viewModel.onBoardCellClick(r, c) },
                        hintMove = state.hintMove,
                        wordSpanCells = state.lastWordSpanCells,
                        boardScale = boardScale,
                        onScaleChanged = { boardScale = it },
                        boardOffset = boardOffset,
                        onOffsetChanged = { boardOffset = it },
                        onCellPositioned = { r, c, bounds ->
                            cellBoundsMap[r to c] = bounds
                        },
                        hoveredCell = hoveredCell
                    )
                }

                // Right Side: Sidebar Container with Scores, Controls, Rack & Actions
                Surface(
                    modifier = Modifier
                        .weight(0.95f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(16.dp),
                    color = palette.cellBackground.copy(alpha = 0.55f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, palette.tileBorder.copy(alpha = 0.45f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                            .verticalScroll(rememberScrollState(), enabled = !isDraggingTile),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Top Bar
                        ScrabbleTopAppBar(
                            remainingTilesCount = state.tileBagCount,
                            palette = palette,
                            onOpenSettings = { viewModel.setDialogVisible(settings = true) },
                            onOpenThemes = { viewModel.setDialogVisible(theme = true) },
                            onOpenHistory = { viewModel.setDialogVisible(history = true) },
                            onOpenStats = { viewModel.setDialogVisible(stats = true) },
                            onRestartGame = { viewModel.startNewGame() },
                            onOpenDebug = { viewModel.toggleDebugDialog(true) }
                        )

                        Spacer(modifier = Modifier.height(3.dp))

                        // Score Cards
                        ScrabbleScoreCards(
                            playerScore = state.playerScore,
                            aiScore = state.aiScore,
                            isPlayerTurn = state.isPlayerTurn,
                            isAiThinking = state.isAiThinking,
                            difficulty = state.difficulty,
                            palette = palette,
                            peeksLeft = state.peeksLeft,
                            isPeeking = state.isPeeking,
                            aiRack = state.aiRack,
                            onPeekClick = { viewModel.onPeekOpponent() },
                            isTimedModeEnabled = state.isTimedModeEnabled,
                            playerTimeLeftSeconds = state.playerTimeLeftSeconds
                        )

                        Spacer(modifier = Modifier.height(3.dp))

                        // Live Word Status / AI Thinking Pill
                        RackStatusPill(
                            validationResult = state.validationResult,
                            hasPendingPlacements = state.pendingPlacements.isNotEmpty(),
                            isPlayerTurn = state.isPlayerTurn && !state.isAiThinking,
                            isAiThinking = state.isAiThinking,
                            statusMessage = state.messageToast,
                            palette = palette
                        )

                        Spacer(modifier = Modifier.height(3.dp))

                        // Wooden Tile Rack & Snap Controls
                        val density = LocalDensity.current
                        PlayerRackView(
                            modifier = Modifier.fillMaxWidth(),
                            rack = if (state.isSnapModeActive && state.fullRackBeforeSnap.isNotEmpty()) state.fullRackBeforeSnap else state.playerRack,
                            selectedTileIndex = state.selectedRackIndex,
                            validationResult = state.validationResult,
                            hasPendingPlacements = state.pendingPlacements.isNotEmpty(),
                            isPlayerTurn = state.isPlayerTurn && !state.isAiThinking,
                            hintsLeft = state.hintsLeft,
                            swapsLeft = state.swapsLeft,
                            isAiThinking = state.isAiThinking,
                            statusMessage = state.messageToast,
                            palette = palette,
                            onTileClick = { idx -> viewModel.onRackTileClick(idx) },
                            onShuffle = { viewModel.onShuffleRack() },
                            onRecall = { viewModel.onRecallAll() },
                            onSwap = { viewModel.setDialogVisible(swap = true) },
                            onPlayMove = { viewModel.onPlayMove() },
                            onHint = { viewModel.onGetHint() },
                            onPass = { viewModel.onPassTurn() },
                            onRearrange = { from, to -> viewModel.onRearrangePlayerRack(from, to) },
                            isSnapModeActive = state.isSnapModeActive,
                            snapSelectedCell = state.snapSelectedCell,
                            snapPreviewIsHorizontal = state.snapPreviewIsHorizontal,
                            snapWordTray = state.snapWordTray,
                            onSnapToggle = { viewModel.toggleSnapMode() },
                            onSnapDirectionToggle = { viewModel.toggleSnapDirection() },
                            onSnapTileToggle = { tile -> viewModel.onSnapTileToggle(tile) },
                            onSnapSelectAll = { viewModel.snapSelectAll() },
                            onSnapClearSelection = { viewModel.snapClearSelection() },
                            onNudgeSnap = { dRow, dCol -> viewModel.nudgeSnap(dRow, dCol) },
                            onSnapConfirm = { viewModel.confirmSnapPlay() },
                            onSnapCancel = { viewModel.cancelSnapMode() },
                            onDragStateChanged = { isDraggingTile = it },
                            onDragStart = { idx, tile, startPos ->
                                draggedTileInfo = Triple(idx, tile, startPos)
                            },
                            onDrag = { amount ->
                                draggedTileInfo = draggedTileInfo?.let {
                                    it.copy(third = it.third + amount)
                                }
                            },
                            onDragEnd = {
                                val info = draggedTileInfo
                                if (info != null) {
                                    val (idx, tile, pos) = info
                                    val halfSizePx = with(density) { 19.dp.toPx() }
                                    val tileCenter = pos + Offset(halfSizePx, halfSizePx)
                                    val droppedCell = cellBoundsMap.entries.firstOrNull { (_, bounds) ->
                                        bounds.contains(tileCenter)
                                    }?.key
                                    if (droppedCell != null) {
                                        viewModel.onDropTileOnBoard(idx, droppedCell.first, droppedCell.second)
                                    }
                                }
                                draggedTileInfo = null
                            },
                            tileSize = 38.dp,
                            spacing = 4.dp
                        )
                    }
                }
            }
        } else {
            // Portrait Layout: Top Header -> Maximized Board -> Bottom Rack & Actions
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header with scores and options
                ScoreHeader(
                    playerScore = state.playerScore,
                    aiScore = state.aiScore,
                    isPlayerTurn = state.isPlayerTurn,
                    isAiThinking = state.isAiThinking,
                    difficulty = state.difficulty,
                    remainingTilesCount = state.tileBagCount,
                    palette = palette,
                    peeksLeft = state.peeksLeft,
                    isPeeking = state.isPeeking,
                    aiRack = state.aiRack,
                    onPeekClick = { viewModel.onPeekOpponent() },
                    onOpenSettings = { viewModel.setDialogVisible(settings = true) },
                    onOpenThemes = { viewModel.setDialogVisible(theme = true) },
                    onOpenHistory = { viewModel.setDialogVisible(history = true) },
                    onOpenStats = { viewModel.setDialogVisible(stats = true) },
                    onRestartGame = { viewModel.startNewGame() },
                    isTimedModeEnabled = state.isTimedModeEnabled,
                    playerTimeLeftSeconds = state.playerTimeLeftSeconds,
                    onOpenDebug = { viewModel.toggleDebugDialog(true) }
                )

                // Fancy animated banner above the board for high score and nice words
                CelebrationBanner(
                    word = state.celebrationWord,
                    points = state.celebrationPoints,
                    isBingo = state.celebrationIsBingo,
                    palette = palette
                )

                // Scrabble 15x15 Interactive Board (Takes available middle space)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ScrabbleBoardView(
                        board = state.board,
                        pendingPlacements = state.pendingPlacements,
                        lastMoveCells = state.lastMoveCells,
                        selectedCell = state.selectedBoardCell,
                        palette = palette,
                        onCellClick = { r, c -> viewModel.onBoardCellClick(r, c) },
                        hintMove = state.hintMove,
                        wordSpanCells = state.lastWordSpanCells,
                        boardScale = boardScale,
                        onScaleChanged = { boardScale = it },
                        boardOffset = boardOffset,
                        onOffsetChanged = { boardOffset = it },
                        onCellPositioned = { r, c, bounds ->
                            cellBoundsMap[r to c] = bounds
                        },
                        hoveredCell = hoveredCell
                    )
                }

                // Generous vertical spacer to distribute excess screen space nicely on tall devices
                Spacer(modifier = Modifier.weight(0.12f))

                // Elegant Status Pill centered in the vertical space between the board and the letter tray
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    RackStatusPill(
                        validationResult = state.validationResult,
                        hasPendingPlacements = state.pendingPlacements.isNotEmpty(),
                        isPlayerTurn = state.isPlayerTurn && !state.isAiThinking,
                        isAiThinking = state.isAiThinking,
                        statusMessage = state.messageToast,
                        palette = palette
                    )
                }

                // Generous vertical spacer to distribute excess screen space nicely on tall devices
                Spacer(modifier = Modifier.weight(0.12f))

                // Player Tile Rack & Action Toolbar
                val density = LocalDensity.current
                PlayerRackView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    rack = if (state.isSnapModeActive && state.fullRackBeforeSnap.isNotEmpty()) state.fullRackBeforeSnap else state.playerRack,
                    selectedTileIndex = state.selectedRackIndex,
                    validationResult = state.validationResult,
                    hasPendingPlacements = state.pendingPlacements.isNotEmpty(),
                    isPlayerTurn = state.isPlayerTurn && !state.isAiThinking,
                    hintsLeft = state.hintsLeft,
                    swapsLeft = state.swapsLeft,
                    isAiThinking = state.isAiThinking,
                    statusMessage = state.messageToast,
                    palette = palette,
                    onTileClick = { idx -> viewModel.onRackTileClick(idx) },
                    onShuffle = { viewModel.onShuffleRack() },
                    onRecall = { viewModel.onRecallAll() },
                    onSwap = { viewModel.setDialogVisible(swap = true) },
                    onPlayMove = { viewModel.onPlayMove() },
                    onHint = { viewModel.onGetHint() },
                    onPass = { viewModel.onPassTurn() },
                    onRearrange = { from, to -> viewModel.onRearrangePlayerRack(from, to) },
                    isSnapModeActive = state.isSnapModeActive,
                    snapSelectedCell = state.snapSelectedCell,
                    snapPreviewIsHorizontal = state.snapPreviewIsHorizontal,
                    snapWordTray = state.snapWordTray,
                    onSnapToggle = { viewModel.toggleSnapMode() },
                    onSnapDirectionToggle = { viewModel.toggleSnapDirection() },
                    onSnapTileToggle = { tile -> viewModel.onSnapTileToggle(tile) },
                    onSnapSelectAll = { viewModel.snapSelectAll() },
                    onSnapClearSelection = { viewModel.snapClearSelection() },
                    onNudgeSnap = { dRow, dCol -> viewModel.nudgeSnap(dRow, dCol) },
                    onSnapConfirm = { viewModel.confirmSnapPlay() },
                    onSnapCancel = { viewModel.cancelSnapMode() },
                    onDragStateChanged = { isDraggingTile = it },
                    onDragStart = { idx, tile, startPos ->
                        draggedTileInfo = Triple(idx, tile, startPos)
                    },
                    onDrag = { amount ->
                        draggedTileInfo = draggedTileInfo?.let {
                            it.copy(third = it.third + amount)
                        }
                    },
                    onDragEnd = {
                        val info = draggedTileInfo
                        if (info != null) {
                            val (idx, tile, pos) = info
                            val halfSizePx = with(density) { 24.dp.toPx() }
                            val tileCenter = pos + Offset(halfSizePx, halfSizePx)
                            val droppedCell = cellBoundsMap.entries.firstOrNull { (_, bounds) ->
                                bounds.contains(tileCenter)
                            }?.key
                            if (droppedCell != null) {
                                viewModel.onDropTileOnBoard(idx, droppedCell.first, droppedCell.second)
                            }
                        }
                        draggedTileInfo = null
                    }
                )
            }
        }

        // Dialogs
        if (state.blankPickerCell != null) {
            BlankTilePickerDialog(
                palette = palette,
                onLetterSelected = { char -> viewModel.onBlankLetterSelected(char) },
                onDismiss = { viewModel.dismissBlankPicker() }
            )
        }

        if (state.showSwapDialog) {
            TileSwapDialog(
                rack = state.playerRack,
                bagCount = state.tileBagCount,
                palette = palette,
                onConfirmSwap = { tiles -> viewModel.onSwapTiles(tiles) },
                onDismiss = { viewModel.setDialogVisible(swap = false) }
            )
        }

        if (state.showSettingsDialog) {
            DifficultySettingsDialog(
                currentDifficulty = state.difficulty,
                customSettings = state.customSettings,
                palette = palette,
                isSoundEnabled = state.isSoundEnabled,
                isTimedModeEnabled = state.isTimedModeEnabled,
                turnTimeLimitSeconds = state.turnTimeLimitSeconds,
                winConditionType = state.winConditionType,
                targetScore = state.targetScore,
                maxTurnsPerPlayer = state.maxTurnsPerPlayer,
                onSoundToggle = { enabled -> viewModel.toggleSoundEffects(enabled) },
                onTimedModeToggle = { enabled, secs -> viewModel.toggleTimedMode(enabled, secs) },
                onWinConditionChange = { type, target, turns -> viewModel.setWinCondition(type, target, turns) },
                onSaveDifficulty = { diff, custom -> viewModel.setDifficulty(diff, custom) },
                onDismiss = { viewModel.setDialogVisible(settings = false) }
            )
        }

        if (state.showThemeDialog) {
            ThemePickerDialog(
                currentTheme = state.currentTheme,
                currentThemeMode = state.themeMode,
                palette = palette,
                onSelectTheme = { theme -> viewModel.setTheme(theme) },
                onSelectThemeMode = { mode -> viewModel.setThemeMode(mode) },
                onDismiss = { viewModel.setDialogVisible(theme = false) }
            )
        }

        if (state.showHistoryDialog) {
            MoveHistoryDialog(
                logs = state.moveLogs,
                palette = palette,
                onHighlightMove = { log -> viewModel.highlightMoveFromLog(log) },
                onDismiss = { viewModel.setDialogVisible(history = false) }
            )
        }

        if (state.showStatsDialog) {
            GameStatsDialog(
                stats = state.stats,
                palette = palette,
                onDismiss = { viewModel.setDialogVisible(stats = false) }
            )
        }

        if (state.definitionWord != null) {
            WordDefinitionDialog(
                word = state.definitionWord!!,
                palette = palette,
                onDismiss = { viewModel.setDialogVisible(definitionWord = null) }
            )
        }

        if (state.showGameOverDialog) {
            GameOverDialog(
                playerScore = state.playerScore,
                aiScore = state.aiScore,
                bestWord = state.bestWord,
                bestWordPoints = state.bestWordPoints,
                difficulty = state.difficulty,
                palette = palette,
                onNewGame = { viewModel.startNewGame() },
                onDismiss = { viewModel.setDialogVisible(gameOver = false) }
            )
        }

        if (state.showDebugDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.toggleDebugDialog(false) },
                title = { Text("🛠️ Scrabble Debug Info", fontWeight = FontWeight.Bold) },
                text = {
                    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                        Text("Last Error Log:", fontWeight = FontWeight.Bold, color = Color.Red)
                        Spacer(modifier = Modifier.height(4.dp))
                        SelectionContainer {
                            Text(
                                text = state.lastErrorLog ?: "No errors recorded.",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                modifier = Modifier
                                    .background(Color.Black.copy(alpha = 0.05f))
                                    .padding(8.dp)
                                    .fillMaxWidth()
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Recent Events:", fontWeight = FontWeight.Bold)
                        state.debugLogs.reversed().forEach { log ->
                            Text("• $log", fontSize = 12.sp, modifier = Modifier.padding(vertical = 2.dp))
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.toggleDebugDialog(false) }) {
                        Text("Dismiss")
                    }
                }
            )
        }

        // Floating Word Definition Overlay Card that floats on top of the entire screen without displacing any view
        if (state.inspectedWordInfo != null) {
            val coords = state.inspectedWordInfo!!.coordinates
            val averageRow = if (coords.isNotEmpty()) coords.map { it.first }.average() else 7.5
            val isTopHalf = averageRow < 7.5
            
            // Put it on the opposite half of where the word resides to prevent blocking it
            val alignment = if (isTopHalf) Alignment.BottomCenter else Alignment.TopCenter
            
            // Distribute paddings so it stays neat and clear of headers/racks
            val topPadding = if (isLandscape) 12.dp else 125.dp
            val bottomPadding = if (isLandscape) 16.dp else 135.dp
            
            val boxPadding = if (isTopHalf) {
                PaddingValues(bottom = bottomPadding, start = 16.dp, end = 16.dp)
            } else {
                PaddingValues(top = topPadding, start = 16.dp, end = 16.dp)
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(boxPadding),
                contentAlignment = alignment
            ) {
                WordInspectionOverlayCard(
                    wordInfo = state.inspectedWordInfo!!,
                    palette = palette,
                    onDismiss = { viewModel.dismissWordInspection() },
                    modifier = Modifier.widthIn(max = 500.dp)
                )
            }
        }

        // Floating drag-and-drop overlay tile
        draggedTileInfo?.let { (rackIndex, tile, position) ->
            val density = LocalDensity.current
            val cellWidthPx = cellBoundsMap.values.firstOrNull()?.width ?: with(density) { 44.dp.toPx() }
            val dragTileSizeDp = with(density) { cellWidthPx.toDp() }

            Box(
                modifier = Modifier
                    .offset {
                        androidx.compose.ui.unit.IntOffset(
                            position.x.coerceAtLeast(0f).toInt(),
                            position.y.coerceAtLeast(0f).toInt()
                        )
                    }
                    .size(dragTileSizeDp)
                    .graphicsLayer {
                        scaleX = dragScale
                        scaleY = dragScale
                        rotationZ = dragRotation
                        shadowElevation = dragElevation
                    }
            ) {
                com.example.game.ui.components.ScrabbleTileView(
                    tile = tile,
                    palette = palette,
                    size = dragTileSizeDp,
                    isSelected = false,
                    isPending = false,
                    onClick = null
                )
            }
        }
    }
}
