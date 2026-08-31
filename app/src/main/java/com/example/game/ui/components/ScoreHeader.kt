package com.example.game.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.model.Difficulty
import com.example.game.model.GameThemePalette
import com.example.game.model.Tile

@Composable
fun ScoreHeader(
    playerScore: Int,
    aiScore: Int,
    isPlayerTurn: Boolean,
    isAiThinking: Boolean,
    difficulty: Difficulty,
    remainingTilesCount: Int,
    palette: GameThemePalette,
    peeksLeft: Int,
    isPeeking: Boolean,
    aiRack: List<Tile>,
    onPeekClick: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenThemes: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenStats: () -> Unit,
    onRestartGame: () -> Unit,
    modifier: Modifier = Modifier,
    isTimedModeEnabled: Boolean = false,
    playerTimeLeftSeconds: Int = 120,
    onOpenDebug: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        ScrabbleTopAppBar(
            remainingTilesCount = remainingTilesCount,
            palette = palette,
            onOpenSettings = onOpenSettings,
            onOpenThemes = onOpenThemes,
            onOpenHistory = onOpenHistory,
            onOpenStats = onOpenStats,
            onRestartGame = onRestartGame,
            onOpenDebug = onOpenDebug
        )

        Spacer(modifier = Modifier.height(2.dp))

        ScrabbleScoreCards(
            playerScore = playerScore,
            aiScore = aiScore,
            isPlayerTurn = isPlayerTurn,
            isAiThinking = isAiThinking,
            difficulty = difficulty,
            palette = palette,
            peeksLeft = peeksLeft,
            isPeeking = isPeeking,
            aiRack = aiRack,
            onPeekClick = onPeekClick,
            isTimedModeEnabled = isTimedModeEnabled,
            playerTimeLeftSeconds = playerTimeLeftSeconds
        )
    }
}

@Composable
fun ScrabbleTopAppBar(
    remainingTilesCount: Int,
    palette: GameThemePalette,
    onOpenSettings: () -> Unit,
    onOpenThemes: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenStats: () -> Unit,
    onRestartGame: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenDebug: () -> Unit = {}
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App Title & Bag Count
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = palette.primaryColor,
                shadowElevation = 2.dp,
                modifier = Modifier.clickable { onOpenDebug() }.testTag("debug_title_trigger")
            ) {
                Text(
                    text = "SCRABBLE",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            // Tile Bag Badge
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = palette.cellBackground,
                border = androidx.compose.foundation.BorderStroke(1.dp, palette.tileBorder)
            ) {
                Text(
                    text = "👝 $remainingTilesCount left",
                    color = palette.tileTextColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        // Quick Actions
        Row(
            horizontalArrangement = Arrangement.spacedBy(1.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onOpenThemes,
                modifier = Modifier.size(34.dp).testTag("theme_button")
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_palette),
                    contentDescription = "Themes",
                    tint = palette.primaryColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier.size(34.dp).testTag("settings_button")
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Difficulty & AI",
                    tint = palette.primaryColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = onOpenHistory,
                modifier = Modifier.size(34.dp).testTag("history_button")
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_history),
                    contentDescription = "Move History",
                    tint = palette.primaryColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = onOpenStats,
                modifier = Modifier.size(34.dp).testTag("stats_button")
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_bar_chart),
                    contentDescription = "Statistics",
                    tint = palette.primaryColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = onRestartGame,
                modifier = Modifier.size(34.dp).testTag("restart_button")
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "New Game",
                    tint = palette.primaryColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun ScrabbleScoreCards(
    playerScore: Int,
    aiScore: Int,
    isPlayerTurn: Boolean,
    isAiThinking: Boolean,
    difficulty: Difficulty,
    palette: GameThemePalette,
    peeksLeft: Int,
    isPeeking: Boolean,
    aiRack: List<Tile>,
    onPeekClick: () -> Unit,
    modifier: Modifier = Modifier,
    isTimedModeEnabled: Boolean = false,
    playerTimeLeftSeconds: Int = 120
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Player Card
        Surface(
            modifier = Modifier
                .weight(1f)
                .shadow(if (isPlayerTurn) 3.dp else 1.dp, RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            color = if (isPlayerTurn) palette.cellBackground else palette.cellBackground.copy(alpha = 0.7f),
            border = androidx.compose.foundation.BorderStroke(
                width = if (isPlayerTurn) 2.dp else 1.dp,
                color = if (isPlayerTurn) palette.primaryColor else palette.tileBorder.copy(alpha = 0.5f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "👤",
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "YOU",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = if (isPlayerTurn) palette.primaryColor else palette.tileTextColor
                            )
                            if (isPlayerTurn) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(if (isTimedModeEnabled && playerTimeLeftSeconds < 30) Color.Red else Color(0xFF2E7D32), CircleShape)
                                )
                            }
                        }
                        
                        val timerLabel = if (isPlayerTurn && isTimedModeEnabled) {
                            val mins = playerTimeLeftSeconds / 60
                            val secs = playerTimeLeftSeconds % 60
                            "Your Turn (${String.format("%02d:%02d", mins, secs)})"
                        } else if (isPlayerTurn) {
                            "Your Turn"
                        } else {
                            "Waiting..."
                        }
                        
                        val timerColor = if (isPlayerTurn) {
                            if (isTimedModeEnabled && playerTimeLeftSeconds < 30) Color.Red else Color(0xFF2E7D32)
                        } else {
                            palette.tileTextColor.copy(alpha = 0.6f)
                        }

                        Text(
                            text = timerLabel,
                            fontSize = 9.sp,
                            fontWeight = if (isPlayerTurn) FontWeight.SemiBold else FontWeight.Normal,
                            color = timerColor
                        )
                    }
                }

                Text(
                    text = "$playerScore",
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    color = palette.tileTextColor,
                    modifier = Modifier.testTag("player_score_text")
                )
            }
        }

        // AI Opponent Card
        Surface(
            modifier = Modifier
                .weight(1f)
                .shadow(if (!isPlayerTurn) 3.dp else 1.dp, RoundedCornerShape(12.dp))
                .clickable(
                    enabled = isPlayerTurn && peeksLeft > 0 && !isPeeking,
                    onClick = onPeekClick
                ),
            shape = RoundedCornerShape(12.dp),
            color = if (isPeeking) palette.secondaryColor.copy(alpha = 0.15f) else if (!isPlayerTurn) palette.cellBackground else palette.cellBackground.copy(alpha = 0.7f),
            border = androidx.compose.foundation.BorderStroke(
                width = if (isPeeking) 2.dp else if (!isPlayerTurn) 2.dp else 1.dp,
                color = if (isPeeking) Color(0xFFE65100) else if (!isPlayerTurn) palette.secondaryColor else palette.tileBorder.copy(alpha = 0.5f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = difficulty.avatarEmoji,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = difficulty.botName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = if (!isPlayerTurn) palette.secondaryColor else palette.tileTextColor,
                                maxLines = 1
                            )
                            if (!isPlayerTurn) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(palette.secondaryColor, CircleShape)
                                )
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .padding(start = 4.dp, end = 2.dp)
                                    .alpha(if (peeksLeft > 0) 1.0f else 0.4f)
                            ) {
                                Text(
                                    text = "👁️",
                                    fontSize = 10.sp
                                )
                                Spacer(modifier = Modifier.width(1.dp))
                                Text(
                                    text = "$peeksLeft",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.tileTextColor.copy(alpha = 0.8f)
                                )
                            }
                        }
                        if (isPeeking) {
                            Text(
                                text = aiRack.joinToString(" ") { it.letter.toString() },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp,
                                color = Color(0xFFE65100)
                            )
                        } else if (isAiThinking) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(8.dp),
                                    strokeWidth = 1.5.dp,
                                    color = palette.secondaryColor
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Thinking...",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = palette.secondaryColor
                                )
                            }
                        } else {
                            Text(
                                text = if (!isPlayerTurn) "Bot's Turn" else "Tap to peek",
                                fontSize = 9.sp,
                                fontWeight = if (!isPlayerTurn) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (!isPlayerTurn) palette.secondaryColor else palette.tileTextColor.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                Text(
                    text = "$aiScore",
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    color = palette.tileTextColor,
                    modifier = Modifier.testTag("ai_score_text")
                )
            }
        }
    }
}
