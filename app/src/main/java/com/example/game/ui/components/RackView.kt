package com.example.game.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.model.GameThemePalette
import com.example.game.model.MoveScoreResult
import com.example.game.model.Tile

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
    palette: GameThemePalette,
    onTileClick: (index: Int) -> Unit,
    onShuffle: () -> Unit,
    onRecall: () -> Unit,
    onSwap: () -> Unit,
    onPlayMove: () -> Unit,
    onHint: () -> Unit,
    onPass: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        RackTileTray(
            rack = rack,
            selectedTileIndex = selectedTileIndex,
            isPlayerTurn = isPlayerTurn,
            palette = palette,
            onTileClick = onTileClick
        )

        Spacer(modifier = Modifier.height(6.dp))

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
            onPass = onPass
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
    modifier: Modifier = Modifier
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
    spacing: Dp = 5.dp
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        // Compute responsive large tile size based on available width (46dp to 52dp)
        val calculatedTileSize = tileSize ?: ((maxWidth - 20.dp - (spacing * 6)) / 7.1f).coerceIn(44.dp, 52.dp)

        Surface(
            modifier = Modifier
                .shadow(8.dp, RoundedCornerShape(14.dp))
                .clip(RoundedCornerShape(14.dp))
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
                    )
                )
                .border(2.dp, palette.tileBorder, RoundedCornerShape(14.dp))
                .padding(horizontal = 8.dp, vertical = 7.dp),
            color = Color.Transparent
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until 7) {
                    val tile = rack.getOrNull(i)
                    if (tile != null) {
                        ScrabbleTileView(
                            tile = tile,
                            palette = palette,
                            size = calculatedTileSize,
                            isSelected = selectedTileIndex == i,
                            isPending = false,
                            onClick = if (isPlayerTurn) { { onTileClick(i) } } else null
                        )
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
    modifier: Modifier = Modifier
) {
    val isPlayEnabled = isPlayerTurn && hasPendingPlacements && (validationResult?.isValid == true)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Shuffle
        IconButton(
            onClick = onShuffle,
            enabled = isPlayerTurn && rack.isNotEmpty(),
            modifier = Modifier
                .size(46.dp)
                .shadow(3.dp, CircleShape)
                .background(palette.cellBackground, CircleShape)
                .border(1.dp, palette.tileBorder.copy(alpha = 0.6f), CircleShape)
                .testTag("shuffle_button")
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_shuffle),
                contentDescription = "Shuffle Rack",
                tint = if (isPlayerTurn && rack.isNotEmpty()) palette.primaryColor else palette.primaryColor.copy(alpha = 0.35f),
                modifier = Modifier.size(22.dp)
            )
        }

        // Recall / Undo
        IconButton(
            onClick = onRecall,
            enabled = isPlayerTurn && hasPendingPlacements,
            modifier = Modifier
                .size(46.dp)
                .shadow(3.dp, CircleShape)
                .background(palette.cellBackground, CircleShape)
                .border(1.dp, palette.tileBorder.copy(alpha = 0.6f), CircleShape)
                .testTag("recall_button")
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_undo),
                contentDescription = "Recall Placed Tiles",
                tint = if (hasPendingPlacements) palette.secondaryColor else palette.primaryColor.copy(alpha = 0.3f),
                modifier = Modifier.size(22.dp)
            )
        }

        // Swap
        Box(contentAlignment = Alignment.Center) {
            IconButton(
                onClick = onSwap,
                enabled = isPlayerTurn && rack.isNotEmpty() && !hasPendingPlacements && swapsLeft > 0,
                modifier = Modifier
                    .size(46.dp)
                    .shadow(3.dp, CircleShape)
                    .background(palette.cellBackground, CircleShape)
                    .border(1.dp, palette.tileBorder.copy(alpha = 0.6f), CircleShape)
                    .testTag("swap_button")
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_swap_horiz),
                    contentDescription = "Exchange Tiles",
                    tint = if (isPlayerTurn && rack.isNotEmpty() && !hasPendingPlacements && swapsLeft > 0) palette.primaryColor else palette.primaryColor.copy(alpha = 0.3f),
                    modifier = Modifier.size(22.dp)
                )
            }
            // Swap count badge
            Box(
                modifier = Modifier
                    .size(15.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 1.dp, y = (-1).dp)
                    .background(palette.primaryColor, CircleShape)
                    .border(0.8.dp, Color.White, CircleShape),
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

        // Hint
        Box(contentAlignment = Alignment.Center) {
            IconButton(
                onClick = onHint,
                enabled = isPlayerTurn && !hasPendingPlacements && hintsLeft > 0,
                modifier = Modifier
                    .size(46.dp)
                    .shadow(3.dp, CircleShape)
                    .background(palette.cellBackground, CircleShape)
                    .border(1.dp, palette.tileBorder.copy(alpha = 0.6f), CircleShape)
                    .testTag("hint_button")
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_lightbulb),
                    contentDescription = "Get Hint",
                    tint = if (isPlayerTurn && !hasPendingPlacements && hintsLeft > 0) Color(0xFFE65100) else Color(0xFFE65100).copy(alpha = 0.35f),
                    modifier = Modifier.size(22.dp)
                )
            }
            // Hint count badge
            Box(
                modifier = Modifier
                    .size(15.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 1.dp, y = (-1).dp)
                    .background(Color(0xFFE65100), CircleShape)
                    .border(0.8.dp, Color.White, CircleShape),
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
            shape = RoundedCornerShape(24.dp),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 4.dp,
                pressedElevation = 2.dp,
                disabledElevation = 0.dp
            ),
            border = if (isPlayEnabled) {
                androidx.compose.foundation.BorderStroke(1.8.dp, Color(0xFF81C784))
            } else {
                androidx.compose.foundation.BorderStroke(1.dp, palette.tileBorder.copy(alpha = 0.45f))
            },
            modifier = Modifier
                .height(46.dp)
                .testTag("play_move_button")
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.ic_send),
                    contentDescription = null,
                    tint = if (isPlayEnabled) Color.White else palette.tileTextColor.copy(alpha = 0.35f),
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isPlayEnabled && validationResult?.isValid == true) {
                        "PLAY (+${validationResult.totalScore})"
                    } else {
                        "PLAY"
                    },
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = if (isPlayEnabled) Color.White else palette.tileTextColor.copy(alpha = 0.35f)
                )
            }
        }

        // Pass Turn
        OutlinedButton(
            onClick = onPass,
            enabled = isPlayerTurn && !hasPendingPlacements,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .height(46.dp)
                .testTag("pass_button")
        ) {
            Text(
                text = "Pass",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isPlayerTurn && !hasPendingPlacements) palette.primaryColor else palette.tileTextColor.copy(alpha = 0.35f)
            )
        }
    }
}
