package com.example.game.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.model.GameThemePalette
import com.example.game.model.Tile

@Composable
fun ScrabbleTileView(
    tile: Tile,
    palette: GameThemePalette,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    isSelected: Boolean = false,
    isPending: Boolean = false,
    isLastMove: Boolean = false,
    isHint: Boolean = false,
    onBoard: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val isCommittedBoardTile = onBoard && !isPending && !isHint

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.08f else if (isLastMove) 1.04f else 1.0f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 380f),
        label = "tileScale"
    )

    val elevation = if (isSelected) 8.dp else if (isPending) 4.dp else if (isLastMove) 4.dp else if (isHint) 0.dp else 2.dp

    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .shadow(elevation, RoundedCornerShape(6.dp))
            .clip(RoundedCornerShape(6.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = if (isHint) {
                        listOf(
                            palette.tileColor.copy(alpha = 0.45f),
                            palette.tileColor.copy(alpha = 0.3f)
                        )
                    } else if (isCommittedBoardTile) {
                        listOf(
                            Color(0xFFFFE082), // High-visibility bright warm amber top
                            Color(0xFFFFB300)  // Deep rich amber bottom for contrast
                        )
                    } else if (isPending) {
                        listOf(
                            palette.tileColor,
                            palette.tileColor.copy(alpha = 0.88f)
                        )
                    } else if (isLastMove) {
                        listOf(
                            palette.tileColor,
                            Color(
                                red = (palette.tileColor.red * 0.96f).coerceIn(0f, 1f),
                                green = (palette.tileColor.green * 0.94f).coerceIn(0f, 1f),
                                blue = (palette.tileColor.blue * 0.88f).coerceIn(0f, 1f),
                                alpha = 1f
                            )
                        )
                    } else {
                        listOf(
                            palette.tileColor,
                            Color(
                                red = (palette.tileColor.red * 0.94f).coerceIn(0f, 1f),
                                green = (palette.tileColor.green * 0.93f).coerceIn(0f, 1f),
                                blue = (palette.tileColor.blue * 0.90f).coerceIn(0f, 1f),
                                alpha = 1f
                            )
                        )
                    }
                )
            )
            .border(
                width = if (isSelected) 2.dp else if (isPending) 1.5.dp else if (isLastMove) 2.dp else if (isHint) 1.5.dp else 1.dp,
                color = if (isSelected) palette.secondaryColor 
                        else if (isPending) palette.primaryColor 
                        else if (isLastMove) Color(0xFFE65100) 
                        else if (isHint) Color(0xFFFFB300).copy(alpha = 0.8f)
                        else if (isCommittedBoardTile) Color(0xFF795548) // Chocolate border for coin contrast
                        else palette.tileBorder,
                shape = RoundedCornerShape(6.dp)
            )
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            )
            .testTag("scrabble_tile_${tile.displayLetter}_${tile.id}"),
        contentAlignment = Alignment.Center
    ) {
        val letterChar = tile.displayLetter
        val isBlankAssigned = tile.isBlank && tile.assignedLetter != null

        // Main Letter
        Text(
            text = if (letterChar == ' ') "" else letterChar.toString(),
            color = if (isCommittedBoardTile) Color(0xFF3E2723) // Deep rich chocolate for ultra-contrast readability
                    else if (isHint) palette.tileTextColor.copy(alpha = 0.65f)
                    else if (isBlankAssigned) palette.primaryColor 
                    else palette.tileTextColor,
            fontSize = (size.value * 0.52f).sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif,
            textAlign = TextAlign.Center,
            modifier = Modifier.offset(y = (-size.value * 0.04f).dp)
        )

        // Subscript Point Value
        if (tile.points > 0 || isBlankAssigned) {
            Text(
                text = if (tile.isBlank) "★" else tile.points.toString(),
                color = if (isCommittedBoardTile) Color(0xFF5D4037)
                        else if (isHint) palette.tileTextColor.copy(alpha = 0.5f)
                        else if (tile.isBlank) palette.secondaryColor 
                        else palette.tileTextColor.copy(alpha = 0.75f),
                fontSize = (size.value * 0.24f).sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = (size.value * 0.08f).dp, bottom = (size.value * 0.04f).dp)
            )
        }
    }
}
