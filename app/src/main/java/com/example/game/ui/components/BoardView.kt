package com.example.game.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
    hintMove: AiCandidateMove? = null
) {
    var scale by remember { mutableFloatStateOf(1.0f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val infiniteTransition = rememberInfiniteTransition(label = "lastMoveGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

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
                                val isLastMove = lastMoveCells.contains(r to c)

                                BoardCellView(
                                    cell = cell,
                                    tile = tile,
                                    isPending = pendingTile != null,
                                    isHint = hintTile != null,
                                    isSelected = isSelected,
                                    isLastMove = isLastMove,
                                    glowAlpha = glowAlpha,
                                    palette = palette,
                                    cellSize = cellSize,
                                    onClick = { onCellClick(r, c) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // Quick Zoom Controls Overlay in bottom right
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .shadow(3.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                color = palette.cellBackground.copy(alpha = 0.88f)
            ) {
                Row(
                    modifier = Modifier.padding(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            scale = (scale + 0.4f).coerceAtMost(3.0f)
                        },
                        modifier = Modifier.size(32.dp),
                        colors = IconButtonDefaults.iconButtonColors(contentColor = palette.primaryColor)
                    ) {
                        Icon(painter = painterResource(R.drawable.ic_add), contentDescription = "Zoom In", modifier = Modifier.size(16.dp))
                    }

                    IconButton(
                        onClick = {
                            scale = 1.0f
                            offset = Offset.Zero
                        },
                        modifier = Modifier.size(32.dp),
                        colors = IconButtonDefaults.iconButtonColors(contentColor = palette.primaryColor)
                    ) {
                        Icon(painter = painterResource(R.drawable.ic_crop_free), contentDescription = "Reset Zoom", modifier = Modifier.size(16.dp))
                    }

                    IconButton(
                        onClick = {
                            scale = (scale - 0.4f).coerceAtLeast(1.0f)
                            if (scale <= 1.01f) {
                                scale = 1.0f
                                offset = Offset.Zero
                            }
                        },
                        modifier = Modifier.size(32.dp),
                        colors = IconButtonDefaults.iconButtonColors(contentColor = palette.primaryColor)
                    ) {
                        Icon(painter = painterResource(R.drawable.ic_remove), contentDescription = "Zoom Out", modifier = Modifier.size(16.dp))
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
    glowAlpha: Float,
    palette: GameThemePalette,
    cellSize: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgBonusColor = when (cell.bonusType) {
        BonusType.TRIPLE_WORD -> palette.twsColor
        BonusType.DOUBLE_WORD -> palette.dwsColor
        BonusType.TRIPLE_LETTER -> palette.tlsColor
        BonusType.DOUBLE_LETTER -> palette.dlsColor
        BonusType.CENTER -> palette.centerColor
        BonusType.NONE -> palette.cellBackground
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(0.75.dp)
            .clip(RoundedCornerShape(2.5.dp))
            .background(bgBonusColor)
            .border(
                width = if (isSelected) 2.dp else if (isLastMove) 1.5.dp else 0.5.dp,
                color = if (isSelected) palette.secondaryColor
                else if (isLastMove) palette.secondaryColor.copy(alpha = glowAlpha)
                else palette.tileBorder.copy(alpha = 0.25f),
                shape = RoundedCornerShape(2.5.dp)
            )
            .clickable(onClick = onClick)
            .testTag("cell_${cell.row}_${cell.col}"),
        contentAlignment = Alignment.Center
    ) {
        if (tile != null) {
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
