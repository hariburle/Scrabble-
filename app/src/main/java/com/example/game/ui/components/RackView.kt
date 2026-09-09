package com.example.game.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.geometry.Offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.model.GameThemePalette
import com.example.game.model.MoveScoreResult
import com.example.game.model.Tile
import com.example.game.model.TrayWordInfo

@Composable
fun PlayerRackView(
    rack: List<Tile>,
    selectedTileIndex: Int?,
    validationResult: MoveScoreResult?,
    hasPendingPlacements: Boolean,
    isPlayerTurn: Boolean,
    hintsLeft: Int,
    swapsLeft: Int,
    isAiThinking: Boolean = false,
    statusMessage: String? = null,
    trayWordInfo: TrayWordInfo? = null,
    palette: GameThemePalette,
    onTileClick: (index: Int) -> Unit,
    onShuffle: () -> Unit,
    onRecall: () -> Unit,
    onSwap: () -> Unit,
    onPlayMove: () -> Unit,
    onHint: () -> Unit,
    onPass: () -> Unit,
    modifier: Modifier = Modifier,
    onRearrange: (fromIndex: Int, toIndex: Int) -> Unit = { _, _ -> },
    onRearrangeFinished: () -> Unit = {},
    isSnapModeActive: Boolean = false,
    snapSelectedCell: Pair<Int, Int>? = null,
    snapPreviewIsHorizontal: Boolean = true,
    snapWordTray: List<Tile> = emptyList(),
    onSnapToggle: () -> Unit = {},
    onSnapDirectionToggle: () -> Unit = {},
    onSnapTileToggle: (Tile) -> Unit = {},
    onSnapSelectAll: () -> Unit = {},
    onSnapClearSelection: () -> Unit = {},
    onNudgeSnap: (dRow: Int, dCol: Int) -> Unit = { _, _ -> },
    onSnapConfirm: () -> Unit = {},
    onSnapCancel: () -> Unit = {},
    onDragStart: (index: Int, tile: Tile, startPosition: Offset) -> Unit = { _, _, _ -> },
    onDrag: (amount: Offset) -> Unit = {},
    onDragEnd: () -> Unit = {},
    onDragStateChanged: (isDragging: Boolean) -> Unit = {},
    tileSize: androidx.compose.ui.unit.Dp? = null,
    spacing: androidx.compose.ui.unit.Dp = 5.dp
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        RackTileTray(
            rack = rack,
            selectedTileIndex = selectedTileIndex,
            isPlayerTurn = isPlayerTurn,
            palette = palette,
            trayWordInfo = trayWordInfo,
            onTileClick = if (isSnapModeActive) {
                { idx ->
                    val tile = rack.getOrNull(idx)
                    if (tile != null) onSnapTileToggle(tile)
                }
            } else {
                onTileClick
            },
            onRearrange = onRearrange,
            onRearrangeFinished = onRearrangeFinished,
            isSnapModeActive = isSnapModeActive,
            snapWordTray = snapWordTray,
            onDragStart = onDragStart,
            onDrag = onDrag,
            onDragEnd = onDragEnd,
            onDragStateChanged = onDragStateChanged,
            tileSize = tileSize,
            spacing = spacing
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (isSnapModeActive) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                color = palette.cellBackground,
                border = BorderStroke(1.5.dp, palette.primaryColor),
                shadowElevation = 4.dp
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("🧩", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "SNAP WORD MODE",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp,
                                    color = palette.primaryColor,
                                    letterSpacing = 0.8.sp
                                )
                                Text(
                                    text = if (snapWordTray.isNotEmpty()) {
                                        "Word: \"${snapWordTray.joinToString("") { it.effectiveLetter.toString() }}\" (${snapWordTray.size} tiles selected)"
                                    } else {
                                        "Tap rack tiles below to pick only letters you want"
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = if (snapWordTray.isNotEmpty()) FontWeight.Bold else FontWeight.Normal,
                                    color = if (snapWordTray.isNotEmpty()) palette.primaryColor else palette.tileTextColor.copy(alpha = 0.8f)
                                )
                            }
                        }

                        // Quick selection & close actions
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier
                                    .clickable { onSnapClearSelection() }
                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(6.dp),
                                color = palette.tileColor,
                                border = BorderStroke(1.dp, palette.tileBorder.copy(alpha = 0.6f))
                            ) {
                                Text(
                                    "Clear",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.tileTextColor,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }

                            Surface(
                                modifier = Modifier
                                    .clickable { onSnapSelectAll() }
                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(6.dp),
                                color = palette.tileColor,
                                border = BorderStroke(1.dp, palette.tileBorder.copy(alpha = 0.6f))
                            ) {
                                Text(
                                    "All",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.tileTextColor,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }

                            IconButton(
                                onClick = onSnapCancel,
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(Color.Red.copy(alpha = 0.12f), CircleShape)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel Snap", tint = Color.Red, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    if (snapSelectedCell != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Direction toggle button
                            OutlinedButton(
                                onClick = onSnapDirectionToggle,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(34.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Text(
                                    text = if (snapPreviewIsHorizontal) "↔ Across" else "↕ Down",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.primaryColor
                                )
                            }

                            // Nudge Arrow D-Pad
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    modifier = Modifier.size(32.dp).clickable { onNudgeSnap(0, -1) },
                                    shape = CircleShape,
                                    color = palette.tileColor,
                                    border = BorderStroke(1.dp, palette.tileBorder)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("◀", fontSize = 12.sp, color = palette.tileTextColor)
                                    }
                                }
                                Surface(
                                    modifier = Modifier.size(32.dp).clickable { onNudgeSnap(-1, 0) },
                                    shape = CircleShape,
                                    color = palette.tileColor,
                                    border = BorderStroke(1.dp, palette.tileBorder)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("▲", fontSize = 12.sp, color = palette.tileTextColor)
                                    }
                                }
                                Surface(
                                    modifier = Modifier.size(32.dp).clickable { onNudgeSnap(1, 0) },
                                    shape = CircleShape,
                                    color = palette.tileColor,
                                    border = BorderStroke(1.dp, palette.tileBorder)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("▼", fontSize = 12.sp, color = palette.tileTextColor)
                                    }
                                }
                                Surface(
                                    modifier = Modifier.size(32.dp).clickable { onNudgeSnap(0, 1) },
                                    shape = CircleShape,
                                    color = palette.tileColor,
                                    border = BorderStroke(1.dp, palette.tileBorder)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("▶", fontSize = 12.sp, color = palette.tileTextColor)
                                    }
                                }
                            }

                            // Snap Confirm
                            Button(
                                onClick = onSnapConfirm,
                                enabled = snapWordTray.isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF2E7D32),
                                    disabledContainerColor = Color.Gray.copy(alpha = 0.4f)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(34.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = "Confirm", tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "Snap (${snapWordTray.size})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "👉 Tap any starting cell on the board to position the word",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = palette.secondaryColor
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        RackActionButtons(
            rack = rack,
            hasPendingPlacements = hasPendingPlacements,
            isPlayerTurn = isPlayerTurn,
            validationResult = validationResult,
            palette = palette,
            hintsLeft = hintsLeft,
            swapsLeft = swapsLeft,
            onShuffle = onShuffle,
            onRecall = onRecall,
            onSwap = onSwap,
            onHint = onHint,
            onPlayMove = onPlayMove,
            onPass = onPass,
            isSnapModeActive = isSnapModeActive,
            onSnapToggle = onSnapToggle
        )
    }
}

@Composable
fun RackStatusPill(
    validationResult: MoveScoreResult?,
    hasPendingPlacements: Boolean,
    isPlayerTurn: Boolean,
    isAiThinking: Boolean,
    statusMessage: String?,
    palette: GameThemePalette,
    modifier: Modifier = Modifier,
    trayWordInfo: TrayWordInfo? = null
) {
    Box(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        if (hasPendingPlacements && validationResult != null) {
            val isValid = validationResult.isValid
            val wordText = validationResult.primaryWord?.word ?: ""
            val pts = validationResult.totalScore

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isValid) Color(0xFF1B5E20) else Color(0xFFB71C1C),
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (isValid) Icons.Default.Check else Icons.Default.Info,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isValid) {
                            "Valid: $wordText (+$pts pts)" + (if (validationResult.isBingo) " ★ BINGO!" else "")
                        } else {
                            validationResult.errorMessage ?: "Invalid Placement"
                        },
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        style = androidx.compose.ui.text.TextStyle(
                            platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)
                        )
                    )
                }
            }
        } else if (isAiThinking || (statusMessage != null && isAiThinking)) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = palette.secondaryColor,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        strokeWidth = 1.8.dp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = statusMessage ?: "AI is thinking...",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        style = androidx.compose.ui.text.TextStyle(
                            platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)
                        )
                    )
                }
            }
        } else if (statusMessage != null) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = palette.cellBackground,
                border = androidx.compose.foundation.BorderStroke(1.dp, palette.primaryColor.copy(alpha = 0.35f)),
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(palette.primaryColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = statusMessage,
                        color = palette.primaryColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        style = TextStyle(
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        )
                    )
                }
            }
        } else if (isPlayerTurn && trayWordInfo != null) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF1B5E20),
                border = BorderStroke(1.dp, Color(0xFF81C784)),
                shadowElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "💡",
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Tray Spells: \"${trayWordInfo.word}\" (+${trayWordInfo.totalScore} pts)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        style = androidx.compose.ui.text.TextStyle(
                            platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)
                        )
                    )
                }
            }
        } else {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = palette.cellBackground.copy(alpha = 0.75f),
                border = androidx.compose.foundation.BorderStroke(0.8.dp, palette.tileBorder.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(
                                if (isPlayerTurn) Color(0xFF2E7D32) else palette.tileTextColor.copy(alpha = 0.4f),
                                CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isPlayerTurn) "Your Turn • Place tiles on the board" else "Opponent's turn...",
                        color = palette.tileTextColor.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        style = androidx.compose.ui.text.TextStyle(
                            platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun RackTileTray(
    rack: List<Tile>,
    selectedTileIndex: Int?,
    isPlayerTurn: Boolean,
    palette: GameThemePalette,
    onTileClick: (index: Int) -> Unit,
    modifier: Modifier = Modifier,
    tileSize: Dp? = null,
    spacing: Dp = 5.dp,
    onRearrange: (fromIndex: Int, toIndex: Int) -> Unit = { _, _ -> },
    onRearrangeFinished: () -> Unit = {},
    isSnapModeActive: Boolean = false,
    snapWordTray: List<Tile> = emptyList(),
    trayWordInfo: TrayWordInfo? = null,
    onDragStart: (index: Int, tile: Tile, startPosition: Offset) -> Unit = { _, _, _ -> },
    onDrag: (amount: Offset) -> Unit = {},
    onDragEnd: () -> Unit = {},
    onDragStateChanged: (isDragging: Boolean) -> Unit = {}
) {
    val currentRack by rememberUpdatedState(rack)
    val currentOnRearrange by rememberUpdatedState(onRearrange)
    val currentOnRearrangeFinished by rememberUpdatedState(onRearrangeFinished)
    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDrag by rememberUpdatedState(onDrag)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)
    val currentOnDragStateChanged by rememberUpdatedState(onDragStateChanged)

    var draggedTileId by remember { mutableStateOf<String?>(null) }
    var touchStartInRoot by remember { mutableStateOf(Offset.Zero) }
    var cumulativeDrag by remember { mutableStateOf(Offset.Zero) }
    var initialSlotIndex by remember { mutableIntStateOf(-1) }
    var currentSlotIndex by remember { mutableIntStateOf(-1) }
    var isDraggingGlobally by remember { mutableStateOf(false) }
    val tilePositions = remember { Array(7) { Offset.Zero } }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        // Compute responsive large tile size based on available width (46dp to 52dp)
        val calculatedTileSize = tileSize ?: ((maxWidth - 20.dp - (spacing * 6)) / 7.1f).coerceIn(44.dp, 52.dp)

        val density = LocalDensity.current
        val tileWidthPx = with(density) { calculatedTileSize.toPx() }
        val tileHeightPx = tileWidthPx
        val spacingPx = with(density) { spacing.toPx() }
        val slotDistance = tileWidthPx + spacingPx

        Surface(
            modifier = Modifier
                .shadow(8.dp, RoundedCornerShape(14.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            palette.boardBackground,
                            Color(
                                red = (palette.boardBackground.red * 0.82f).coerceIn(0f, 1f),
                                green = (palette.boardBackground.green * 0.82f).coerceIn(0f, 1f),
                                blue = (palette.boardBackground.blue * 0.82f).coerceIn(0f, 1f),
                                alpha = 1f
                            )
                        )
                    ),
                    shape = RoundedCornerShape(14.dp)
                )
                .border(2.dp, palette.tileBorder, RoundedCornerShape(14.dp))
                .padding(horizontal = 10.dp, vertical = 9.dp),
            color = Color.Transparent,
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until 7) {
                    val tile = rack.getOrNull(i)
                    if (tile != null) {
                        key(tile.id) {
                            val index = i
                            val isBeingDragged = (draggedTileId == tile.id)
                            val isCurrentGloballyDragged = isDraggingGlobally && isBeingDragged
                            val displayOffset = if (isBeingDragged && !isDraggingGlobally && initialSlotIndex != -1 && currentSlotIndex != -1) {
                                cumulativeDrag.x - (currentSlotIndex - initialSlotIndex) * slotDistance
                            } else {
                                0f
                            }
                            val snapOrderIndex = if (isSnapModeActive) snapWordTray.indexOfFirst { it.id == tile.id } else -1
                            val isSnapSelected = snapOrderIndex != -1
                            val isTrayWordTile = trayWordInfo != null && selectedTileIndex == null && !isSnapModeActive && index in trayWordInfo.startIndex..trayWordInfo.endIndex

                            Box(
                                modifier = Modifier
                                    .zIndex(if (isBeingDragged) 10f else 1f)
                                    .onGloballyPositioned { coordinates ->
                                        if (index in tilePositions.indices) {
                                            tilePositions[index] = coordinates.positionInRoot()
                                        }
                                    }
                                    .graphicsLayer {
                                        translationX = displayOffset
                                        if (isBeingDragged && !isDraggingGlobally) {
                                            scaleX = 1.15f
                                            scaleY = 1.15f
                                        }
                                        alpha = if (isCurrentGloballyDragged) 0.2f else 1.0f
                                    }
                                    .pointerInput(tile.id, isPlayerTurn, isSnapModeActive) {
                                        detectDragGestures(
                                            onDragStart = { localOffset ->
                                                if (isPlayerTurn && !isSnapModeActive) {
                                                    val currentIdx = currentRack.indexOfFirst { it.id == tile.id }
                                                    val startIdx = if (currentIdx != -1) currentIdx else index
                                                    draggedTileId = tile.id
                                                    initialSlotIndex = startIdx
                                                    currentSlotIndex = startIdx
                                                    val slotRootPos = tilePositions.getOrElse(startIdx) { Offset.Zero }
                                                    touchStartInRoot = slotRootPos + localOffset
                                                    cumulativeDrag = Offset.Zero
                                                    isDraggingGlobally = false
                                                    currentOnDragStateChanged(true)
                                                }
                                            },
                                            onDragEnd = {
                                                if (isDraggingGlobally) {
                                                    currentOnDragEnd()
                                                } else if (draggedTileId != null) {
                                                    currentOnRearrangeFinished()
                                                }
                                                draggedTileId = null
                                                initialSlotIndex = -1
                                                currentSlotIndex = -1
                                                cumulativeDrag = Offset.Zero
                                                touchStartInRoot = Offset.Zero
                                                isDraggingGlobally = false
                                                currentOnDragStateChanged(false)
                                            },
                                            onDragCancel = {
                                                if (isDraggingGlobally) {
                                                    currentOnDragEnd()
                                                } else if (draggedTileId != null) {
                                                    currentOnRearrangeFinished()
                                                }
                                                draggedTileId = null
                                                initialSlotIndex = -1
                                                currentSlotIndex = -1
                                                cumulativeDrag = Offset.Zero
                                                touchStartInRoot = Offset.Zero
                                                isDraggingGlobally = false
                                                currentOnDragStateChanged(false)
                                            },
                                            onDrag = { change, dragAmount ->
                                                if (isPlayerTurn && !isSnapModeActive && draggedTileId == tile.id) {
                                                    change.consume()
                                                    cumulativeDrag += dragAmount

                                                    if (!isDraggingGlobally) {
                                                        val slotShift = kotlin.math.round(cumulativeDrag.x / slotDistance).toInt()
                                                        val isDraggingOffTray = if (isLandscape) {
                                                            cumulativeDrag.y < -tileHeightPx * 0.35f || (initialSlotIndex + slotShift < 0 && cumulativeDrag.x < -slotDistance * 0.4f)
                                                        } else {
                                                            cumulativeDrag.y < -tileHeightPx * 0.35f
                                                        }

                                                        if (isDraggingOffTray) {
                                                            isDraggingGlobally = true
                                                            val currentTouchPoint = touchStartInRoot + cumulativeDrag
                                                            currentOnDragStart(currentSlotIndex, tile, currentTouchPoint)
                                                        } else {
                                                            val targetSlot = (initialSlotIndex + slotShift).coerceIn(0, currentRack.lastIndex)
                                                            if (targetSlot != currentSlotIndex && targetSlot in currentRack.indices) {
                                                                currentOnRearrange(currentSlotIndex, targetSlot)
                                                                currentSlotIndex = targetSlot
                                                            }
                                                        }
                                                    } else {
                                                        currentOnDrag(dragAmount)
                                                    }
                                                }
                                            }
                                        )
                                    }
                            ) {
                                ScrabbleTileView(
                                    tile = tile,
                                    palette = palette,
                                    size = calculatedTileSize,
                                    isSelected = if (isSnapModeActive) isSnapSelected else (selectedTileIndex == index),
                                    isPending = false,
                                    isTrayWordTile = isTrayWordTile,
                                    onClick = if (isPlayerTurn && draggedTileId == null) { { onTileClick(index) } } else null
                                )

                                // In Snap Mode, show order badge on selected tiles
                                if (isSnapModeActive && isSnapSelected) {
                                    Surface(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = 3.dp, y = (-3).dp)
                                            .size(16.dp),
                                        shape = CircleShape,
                                        color = palette.primaryColor,
                                        border = BorderStroke(1.dp, Color.White),
                                        shadowElevation = 2.dp
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "${snapOrderIndex + 1}",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(calculatedTileSize)
                                .clip(RoundedCornerShape(8.dp))
                                .background(palette.cellBackground.copy(alpha = 0.45f))
                                .border(1.dp, palette.tileBorder.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RackActionButtons(
    rack: List<Tile>,
    hasPendingPlacements: Boolean,
    isPlayerTurn: Boolean,
    validationResult: MoveScoreResult?,
    palette: GameThemePalette,
    hintsLeft: Int,
    swapsLeft: Int,
    onShuffle: () -> Unit,
    onRecall: () -> Unit,
    onSwap: () -> Unit,
    onHint: () -> Unit,
    onPlayMove: () -> Unit,
    onPass: () -> Unit,
    modifier: Modifier = Modifier,
    isSnapModeActive: Boolean = false,
    onSnapToggle: () -> Unit = {}
) {
    val isPlayEnabled = isPlayerTurn && hasPendingPlacements && (validationResult?.isValid == true)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Row 1: Shuffle, Undo, Snap and Play
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Shuffle
            IconButton(
                onClick = onShuffle,
                enabled = isPlayerTurn && rack.isNotEmpty(),
                modifier = Modifier
                    .size(52.dp)
                    .shadow(3.dp, CircleShape)
                    .background(palette.cellBackground, CircleShape)
                    .border(1.dp, palette.tileBorder.copy(alpha = 0.6f), CircleShape)
                    .testTag("shuffle_button")
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_shuffle),
                    contentDescription = "Shuffle Rack",
                    tint = if (isPlayerTurn && rack.isNotEmpty()) palette.primaryColor else palette.primaryColor.copy(alpha = 0.35f),
                    modifier = Modifier.size(24.dp)
                )
            }

            // Recall / Undo
            IconButton(
                onClick = onRecall,
                enabled = isPlayerTurn && hasPendingPlacements,
                modifier = Modifier
                    .size(52.dp)
                    .shadow(3.dp, CircleShape)
                    .background(palette.cellBackground, CircleShape)
                    .border(1.dp, palette.tileBorder.copy(alpha = 0.6f), CircleShape)
                    .testTag("recall_button")
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_undo),
                    contentDescription = "Recall Placed Tiles",
                    tint = if (hasPendingPlacements) palette.secondaryColor else palette.primaryColor.copy(alpha = 0.3f),
                    modifier = Modifier.size(24.dp)
                )
            }

            // Snap Mode Toggle
            IconButton(
                onClick = onSnapToggle,
                enabled = isPlayerTurn && rack.isNotEmpty(),
                modifier = Modifier
                    .size(52.dp)
                    .shadow(3.dp, CircleShape)
                    .background(if (isSnapModeActive) palette.primaryColor else palette.cellBackground, CircleShape)
                    .border(
                        width = 1.5.dp,
                        color = if (isSnapModeActive) palette.primaryColor else palette.tileBorder.copy(alpha = 0.6f),
                        shape = CircleShape
                    )
                    .testTag("snap_mode_button")
            ) {
                Text(
                    text = "🧩",
                    fontSize = 20.sp,
                    modifier = Modifier.alpha(if (isPlayerTurn) 1f else 0.4f)
                )
            }

            // Play Move Primary Button (High contrast, prominent pill)
            Button(
                onClick = onPlayMove,
                enabled = isPlayEnabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1B5E20),
                    contentColor = Color.White,
                    disabledContainerColor = palette.cellBackground.copy(alpha = 0.65f),
                    disabledContentColor = palette.tileTextColor.copy(alpha = 0.35f)
                ),
                shape = RoundedCornerShape(26.dp),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 2.dp,
                    disabledElevation = 0.dp
                ),
                border = if (isPlayEnabled) {
                    androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF81C784))
                } else {
                    androidx.compose.foundation.BorderStroke(1.dp, palette.tileBorder.copy(alpha = 0.45f))
                },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .testTag("play_move_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_send),
                        contentDescription = null,
                        tint = if (isPlayEnabled) Color.White else palette.tileTextColor.copy(alpha = 0.35f),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isPlayEnabled && validationResult?.isValid == true) {
                            "PLAY (+${validationResult.totalScore})"
                        } else {
                            "PLAY"
                        },
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        color = if (isPlayEnabled) Color.White else palette.tileTextColor.copy(alpha = 0.35f)
                    )
                }
            }
        }

        // Row 2: Swap, Hint, and Pass with uniform heights
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Swap Button (with Badge overlay inside)
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                val isSwapEnabled = isPlayerTurn && rack.isNotEmpty() && !hasPendingPlacements && swapsLeft > 0
                OutlinedButton(
                    onClick = onSwap,
                    enabled = isSwapEnabled,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = palette.primaryColor,
                        disabledContentColor = palette.primaryColor.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(24.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = if (isSwapEnabled) palette.primaryColor.copy(alpha = 0.6f) else palette.tileBorder.copy(alpha = 0.25f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("swap_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_swap_horiz),
                            contentDescription = null,
                            tint = if (isSwapEnabled) palette.primaryColor else palette.primaryColor.copy(alpha = 0.3f),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Swap",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                // Swap count badge
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = (-4).dp, y = (-2).dp)
                        .background(palette.primaryColor, CircleShape)
                        .border(1.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$swapsLeft",
                        color = Color.White,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        style = androidx.compose.ui.text.TextStyle(
                            platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)
                        )
                    )
                }
            }

            // Hint Button
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                val isHintEnabled = isPlayerTurn && !hasPendingPlacements && hintsLeft > 0
                OutlinedButton(
                    onClick = onHint,
                    enabled = isHintEnabled,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFE65100),
                        disabledContentColor = Color(0xFFE65100).copy(alpha = 0.35f)
                    ),
                    shape = RoundedCornerShape(24.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = if (isHintEnabled) Color(0xFFE65100).copy(alpha = 0.6f) else palette.tileBorder.copy(alpha = 0.25f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("hint_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_lightbulb),
                            contentDescription = null,
                            tint = if (isHintEnabled) Color(0xFFE65100) else Color(0xFFE65100).copy(alpha = 0.35f),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Hint",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                // Hint count badge
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = (-4).dp, y = (-2).dp)
                        .background(Color(0xFFE65100), CircleShape)
                        .border(1.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$hintsLeft",
                        color = Color.White,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        style = androidx.compose.ui.text.TextStyle(
                            platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)
                        )
                    )
                }
            }

            // Pass Button
            val isPassEnabled = isPlayerTurn && !hasPendingPlacements
            OutlinedButton(
                onClick = onPass,
                enabled = isPassEnabled,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = palette.primaryColor,
                    disabledContentColor = palette.primaryColor.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = if (isPassEnabled) palette.primaryColor.copy(alpha = 0.6f) else palette.tileBorder.copy(alpha = 0.25f)
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .testTag("pass_button")
            ) {
                Text(
                    text = "Pass",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isPassEnabled) palette.primaryColor else palette.tileTextColor.copy(alpha = 0.35f)
                )
            }
        }
    }
}
