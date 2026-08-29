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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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

    val snackbarHostState = remember { SnackbarHostState() }
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    LaunchedEffect(state.messageToast) {
        state.messageToast?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearToast()
        }
    }

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
                        onCellClick = { r, c -> viewModel.onBoardCellClick(r, c) }
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
                            .verticalScroll(rememberScrollState()),
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
                            onRestartGame = { viewModel.startNewGame() }
                        )

                        Spacer(modifier = Modifier.height(3.dp))

                        // Score Cards
                        ScrabbleScoreCards(
                            playerScore = state.playerScore,
                            aiScore = state.aiScore,
                            isPlayerTurn = state.isPlayerTurn,
                            isAiThinking = state.isAiThinking,
                            difficulty = state.difficulty,
                            palette = palette
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

                        // Wooden Tile Rack
                        RackTileTray(
                            rack = state.playerRack,
                            selectedTileIndex = state.selectedRackIndex,
                            isPlayerTurn = state.isPlayerTurn && !state.isAiThinking,
                            palette = palette,
                            onTileClick = { idx -> viewModel.onRackTileClick(idx) },
                            tileSize = 38.dp,
                            spacing = 4.dp
                        )

                        Spacer(modifier = Modifier.height(5.dp))

                        // Action Buttons
                        RackActionButtons(
                            rack = state.playerRack,
                            hasPendingPlacements = state.pendingPlacements.isNotEmpty(),
                            isPlayerTurn = state.isPlayerTurn && !state.isAiThinking,
                            validationResult = state.validationResult,
                            palette = palette,
                            onShuffle = { viewModel.onShuffleRack() },
                            onRecall = { viewModel.onRecallAll() },
                            onSwap = { viewModel.setDialogVisible(swap = true) },
                            onHint = { viewModel.onGetHint() },
                            onPlayMove = { viewModel.onPlayMove() },
                            onPass = { viewModel.onPassTurn() }
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
                    onOpenSettings = { viewModel.setDialogVisible(settings = true) },
                    onOpenThemes = { viewModel.setDialogVisible(theme = true) },
                    onOpenHistory = { viewModel.setDialogVisible(history = true) },
                    onOpenStats = { viewModel.setDialogVisible(stats = true) },
                    onRestartGame = { viewModel.startNewGame() }
                )

                // Scrabble 15x15 Interactive Board (Takes available middle space)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ScrabbleBoardView(
                        board = state.board,
                        pendingPlacements = state.pendingPlacements,
                        lastMoveCells = state.lastMoveCells,
                        selectedCell = state.selectedBoardCell,
                        palette = palette,
                        onCellClick = { r, c -> viewModel.onBoardCellClick(r, c) }
                    )
                }

                // Player Tile Rack & Action Toolbar
                PlayerRackView(
                    rack = state.playerRack,
                    selectedTileIndex = state.selectedRackIndex,
                    validationResult = state.validationResult,
                    hasPendingPlacements = state.pendingPlacements.isNotEmpty(),
                    isPlayerTurn = state.isPlayerTurn && !state.isAiThinking,
                    isAiThinking = state.isAiThinking,
                    statusMessage = state.messageToast,
                    palette = palette,
                    onTileClick = { idx -> viewModel.onRackTileClick(idx) },
                    onShuffle = { viewModel.onShuffleRack() },
                    onRecall = { viewModel.onRecallAll() },
                    onSwap = { viewModel.setDialogVisible(swap = true) },
                    onPlayMove = { viewModel.onPlayMove() },
                    onHint = { viewModel.onGetHint() },
                    onPass = { viewModel.onPassTurn() }
                )
            }
        }

        // Floating Toast / Snackbar Host
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = if (isLandscape) 16.dp else 80.dp)
        ) { snackbarData ->
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = palette.primaryColor,
                shadowElevation = 6.dp
            ) {
                Text(
                    text = snackbarData.visuals.message,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
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
                onWordClick = { word -> viewModel.setDialogVisible(definitionWord = word) },
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
    }
}
