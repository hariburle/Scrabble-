package com.example.game.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.engine.ScrabbleEngine
import com.example.game.model.BoardCell
import com.example.game.model.BonusType
import com.example.game.model.GameThemePalette
import com.example.game.model.Tile

import com.example.game.engine.AiCandidateMove

@Composable
fun ScrabbleBoardView(
    board: Array<Array<BoardCell>>,
    pendingPlacements: Map<Pair<Int, Int>, Tile>,
    lastMoveCells: Set<Pair<Int, Int>>,
    selectedCell: Pair<Int, Int>?,
    palette: GameThemePalette,
    onCellClick: (row: Int, col: Int) -> Unit,
    modifier: Modifier = Modifier,
    hintMove: AiCandidateMove? = null,
    wordSpanCells: Set<Pair<Int, Int>> = emptySet(),
    boardScale: Float = 1.0f,
    onScaleChanged: (Float) -> Unit = {},
    boardOffset: Offset = Offset.Zero,
    onOffsetChanged: (Offset) -> Unit = {},
    onCellPositioned: (row: Int, col: Int, bounds: androidx.compose.ui.geometry.Rect) -> Unit = { _, _, _ -> },
    hoveredCell: Pair<Int, Int>? = null,
    draggedPendingTileId: String? = null,
    onPendingDragStart: (row: Int, col: Int, com.example.game.model.Tile, androidx.compose.ui.geometry.Offset) -> Unit = { _, _, _, _ -> },
    onPendingDrag: (androidx.compose.ui.geometry.Offset) -> Unit = {},
    onPendingDragEnd: () -> Unit = {}
) {
    var scale by remember { mutableFloatStateOf(boardScale) }
    var offset by remember { mutableStateOf(boardOffset) }

    LaunchedEffect(boardScale) {
        scale = boardScale
    }
    LaunchedEffect(boardOffset) {
        offset = boardOffset
    }

    LaunchedEffect(scale) {
        onScaleChanged(scale)
    }
    LaunchedEffect(offset) {
        onOffsetChanged(offset)
    }

    // The last-move highlight pulses briefly, then fades out completely.
    // It used to pulse forever (infiniteTransition), then it held a steady
    // glow — both read as the word staying "lit".
    val glowAlphaAnim = remember { Animatable(0f) }

    val combinedHighlights = remember(lastMoveCells, wordSpanCells) {
        if (wordSpanCells.isNotEmpty()) wordSpanCells else lastMoveCells
    }

    // Brief pulse whenever the highlighted word changes, then fade to
    // nothing — no steady light left on the board.
    LaunchedEffect(combinedHighlights) {
        if (combinedHighlights.isNotEmpty()) {
            glowAlphaAnim.snapTo(0.35f)
            repeat(2) {
                glowAlphaAnim.animateTo(0.95f, animationSpec = tween(900))
                glowAlphaAnim.animateTo(0.35f, animationSpec = tween(900))
            }
            // Settle at full strength and STAY there until the next move:
            // the just-played word must remain clearly highlighted against
            // the board, not fade back to invisible.
            glowAlphaAnim.animateTo(1f, animationSpec = tween(600))
        }
    }
    val glowAlpha = glowAlphaAnim.value

    val hintPlacementsMap = remember(hintMove) {
        hintMove?.placements?.associate { (it.row to it.col) to it.tile } ?: emptyMap()
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val boardDimension = minOf(maxWidth, maxHeight) - 4.dp
        val cellSize = (boardDimension - 12.dp) / ScrabbleEngine.BOARD_SIZE

        Box(
            modifier = Modifier
                .size(boardDimension)
                .shadow(4.dp, RoundedCornerShape(10.dp))
                .clip(RoundedCornerShape(10.dp))
                .background(palette.boardBackground)
                .border(1.5.dp, palette.primaryColor.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                .pointerInput(Unit) {
                    awaitEachGesture {
                        var zoom = 1f
                        var pan = Offset.Zero
                        var pastTouchSlop = false
                        val touchSlop = viewConfiguration.touchSlop

                        awaitFirstDown(requireUnconsumed = false)
                        do {
                            val event = awaitPointerEvent()
                            val canceled = event.changes.any { it.isConsumed }
                            if (!canceled) {
                                val pointerCount = event.changes.size
                                if (pointerCount >= 2) {
                                    val zoomChange = event.calculateZoom()
                                    val panChange = event.calculatePan()

                                    if (!pastTouchSlop) {
                                        zoom *= zoomChange
                                        pan += panChange
                                        val centroid = event.calculateCentroid(useCurrent = false)
                                        val zoomMotion = kotlin.math.abs(1 - zoom) * centroid.getDistance()
                                        val panMotion = pan.getDistance()
                                        if (zoomMotion > touchSlop || panMotion > touchSlop) {
                                            pastTouchSlop = true
                                        }
                                    }

                                    if (pastTouchSlop) {
                                        scale = (scale * zoomChange).coerceIn(1.0f, 3.0f)
                                        if (scale <= 1.01f) {
                                            scale = 1.0f
                                            offset = Offset.Zero
                                        } else {
                                            val boardPx = size.width.toFloat()
                                            val maxPan = (boardPx * (scale - 1f)) / 2f + (36f * (scale - 1f))
                                            offset = Offset(
                                                x = (offset.x + panChange.x).coerceIn(-maxPan, maxPan),
                                                y = (offset.y + panChange.y).coerceIn(-maxPan, maxPan)
                                            )
                                        }
                                        event.changes.forEach { it.consume() }
                                    }
                                } else if (pointerCount == 1 && scale > 1.0f) {
                                    val change = event.changes[0]
                                    val panChange = change.position - change.previousPosition

                                    if (!pastTouchSlop) {
                                        pan += panChange
                                        if (pan.getDistance() > touchSlop) {
                                            pastTouchSlop = true
                                        }
                                    }

                                    if (pastTouchSlop) {
                                        val boardPx = size.width.toFloat()
                                        val maxPan = (boardPx * (scale - 1f)) / 2f + (36f * (scale - 1f))
                                        offset = Offset(
                                            x = (offset.x + panChange.x).coerceIn(-maxPan, maxPan),
                                            y = (offset.y + panChange.y).coerceIn(-maxPan, maxPan)
                                        )
                                        change.consume()
                                    }
                                }
                            }
                        } while (event.changes.any { it.pressed })
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // Main 15x15 Grid with Zoom/Pan transform
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    )
                    .padding(3.dp)
                    .testTag("scrabble_board_grid")
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    for (r in 0 until ScrabbleEngine.BOARD_SIZE) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            for (c in 0 until ScrabbleEngine.BOARD_SIZE) {
                                val cell = board[r][c]
                                val pendingTile = pendingPlacements[r to c]
                                val hintTile = hintPlacementsMap[r to c]
                                val tile = pendingTile ?: cell.tile ?: hintTile
                                val isSelected = selectedCell == (r to c)
                                val isLastMove = combinedHighlights.contains(r to c)

                                BoardCellView(
                                    cell = cell,
                                    tile = tile,
                                    isPending = pendingTile != null,
                                    // Hint ghost ONLY when the displayed tile
                                    // actually IS the hint tile. Previously
                                    // any hint-covered cell rendered as a
                                    // ghost even when it held a real
                                    // committed tile — so after accepting a
                                    // word, committed tiles on hint cells
                                    // turned translucent ("became light").
                                    isHint = hintTile != null && pendingTile == null && cell.tile == null,
                                    isSelected = isSelected,
                                    isLastMove = isLastMove,
                                    isHovered = hoveredCell == (r to c),
                                    // Null-safe: pendingTile?.id is null for
                                    // committed tiles, and draggedPendingTileId
                                    // is null when no drag is active, so the
                                    // old `pendingTile?.id == draggedPendingTileId`
                                    // was true (null == null) for EVERY committed
                                    // tile — ghosting them all to 0.2 alpha.
                                    isBeingDragged = pendingTile != null && pendingTile.id == draggedPendingTileId,
                                    glowAlpha = glowAlpha,
                                    palette = palette,
                                    cellSize = cellSize,
                                    onClick = { onCellClick(r, c) },
                                    onDoubleTap = {
                                        if (scale > 1.05f) {
                                            scale = 1.0f
                                            offset = Offset.Zero
                                        } else {
                                            scale = 2.0f
                                            offset = Offset.Zero
                                        }
                                    },
                                    onDragStart = { initialOffset ->
                                        if (pendingTile != null) {
                                            onPendingDragStart(r, c, pendingTile, initialOffset)
                                        }
                                    },
                                    onDrag = onPendingDrag,
                                    onDragEnd = onPendingDragEnd,
                                    modifier = Modifier
                                        .weight(1f)
                                        .onGloballyPositioned { coords ->
                                            if (coords.isAttached) {
                                                onCellPositioned(r, c, coords.boundsInRoot())
                                            }
                                        }
                                )
                            }
                        }
                    }
                }
            }

            // Floating Reset Zoom Pill ONLY when actively zoomed in (so it never blocks tiles during normal 1.0x play)
            AnimatedVisibility(
                visible = scale > 1.01f,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = palette.primaryColor.copy(alpha = 0.92f),
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .clickable {
                            scale = 1.0f
                            offset = Offset.Zero
                        }
                        .testTag("reset_zoom_chip")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_crop_free),
                            contentDescription = "Reset Zoom (1.0x)",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Reset 1.0x",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BoardCellView(
    cell: BoardCell,
    tile: Tile?,
    isPending: Boolean,
    isHint: Boolean,
    isSelected: Boolean,
    isLastMove: Boolean,
    isHovered: Boolean = false,
    isBeingDragged: Boolean = false,
    glowAlpha: Float,
    palette: GameThemePalette,
    cellSize: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onDoubleTap: () -> Unit = {},
    onDragStart: (androidx.compose.ui.geometry.Offset) -> Unit = {},
    onDrag: (androidx.compose.ui.geometry.Offset) -> Unit = {},
    onDragEnd: () -> Unit = {}
) {
    val bgBonusColor = when (cell.bonusType) {
        BonusType.TRIPLE_WORD -> palette.twsColor
        BonusType.DOUBLE_WORD -> palette.dwsColor
        BonusType.TRIPLE_LETTER -> palette.tlsColor
        BonusType.DOUBLE_LETTER -> palette.dlsColor
        BonusType.CENTER -> palette.centerColor
        BonusType.NONE -> palette.cellBackground
    }

    val scaleFactor by animateFloatAsState(
        targetValue = if (isHovered) 1.15f else 1.0f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 300f),
        label = "cellHoverScale"
    )

    val dragModifier = if (isPending && tile != null) {
        Modifier.pointerInput(tile.id) {
            var totalDragOffset = Offset.Zero
            var isGloballyDragging = false
            var touchOffset = Offset.Zero
            detectDragGestures(
                onDragStart = { offset ->
                    totalDragOffset = Offset.Zero
                    isGloballyDragging = false
                    touchOffset = offset
                },
                onDragEnd = {
                    if (isGloballyDragging) {
                        onDragEnd()
                    }
                },
                onDragCancel = {
                    if (isGloballyDragging) {
                        onDragEnd()
                    }
                },
                onDrag = { change, dragAmount ->
                    change.consume()
                    totalDragOffset += dragAmount
                    if (!isGloballyDragging) {
                        if (totalDragOffset.getDistance() > 15f) {
                            isGloballyDragging = true
                            // Anchor = exact finger position at activation: the
                            // grab point within the cell + the distance moved
                            // since touch-down. (Passing only the distance, as
                            // before, made the floating tile jump away from
                            // the finger the moment the drag started.)
                            onDragStart(touchOffset + totalDragOffset)
                        }
                    } else {
                        onDrag(dragAmount)
                    }
                }
            )
        }
    } else Modifier

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scaleFactor
                scaleY = scaleFactor
            }
            .then(dragModifier)
            .aspectRatio(1f)
            .padding(0.75.dp)
            .clip(RoundedCornerShape(2.5.dp))
            .background(bgBonusColor)
            .border(
                width = if (isHovered) 2.5.dp else if (isSelected) 2.dp else if (isLastMove) 1.5.dp else 0.5.dp,
                color = if (isHovered) Color(0xFFFFD54F)
                else if (isSelected) palette.secondaryColor
                else if (isLastMove) Color(0xFFE65100).copy(alpha = glowAlpha)
                else palette.tileBorder.copy(alpha = 0.25f),
                shape = RoundedCornerShape(2.5.dp)
            )
            .pointerInput(onClick, onDoubleTap) {
                detectTapGestures(
                    onTap = { onClick() },
                    onDoubleTap = { onDoubleTap() }
                )
            }
            .testTag("cell_${cell.row}_${cell.col}"),
        contentAlignment = Alignment.Center
    ) {
        if (isHovered) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFFFD54F).copy(alpha = 0.3f))
            )
        }
        if (tile != null) {
            // The tile being dragged fades in its cell (same 0.2 ghost as
            // tray drags) so the floating tile is the only full-strength one.
            Box(
                modifier = Modifier.graphicsLayer {
                    alpha = if (isBeingDragged) 0.2f else 1f
                }
            ) {
                ScrabbleTileView(
                    tile = tile,
                    palette = palette,
                    size = cellSize - 1.5.dp,
                    isSelected = isSelected,
                    isPending = isPending,
                    isHint = isHint,
                    isLastMove = isLastMove,
                    onBoard = true
                )
            }
        } else {
            // Display multiplier text with gentle, muted contrast that never overpowers the board
            if (cell.bonusType != BonusType.NONE) {
                Text(
                    text = cell.bonusType.shortLabel,
                    color = if (palette.isDark) Color.White.copy(alpha = 0.78f) else Color(0xFF334E68).copy(alpha = 0.70f),
                    fontSize = (cellSize.value * 0.27f).coerceIn(6f, 10f).sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    lineHeight = (cellSize.value * 0.28f).coerceIn(6.5f, 11f).sp
                )
            }
        }
    }
}
