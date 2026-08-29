package com.example.game.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.game.dictionary.WordDictionary
import com.example.game.model.CustomDifficultySettings
import com.example.game.model.Difficulty
import com.example.game.model.GameLogEntry
import com.example.game.model.GameStats
import com.example.game.model.GameThemePalette
import com.example.game.model.ThemeMode
import com.example.game.model.Tile

/**
 * Letter picker for blank tile
 */
@Composable
fun BlankTilePickerDialog(
    palette: GameThemePalette,
    onLetterSelected: (Char) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = palette.cellBackground,
            border = BorderStroke(2.dp, palette.primaryColor),
            shadowElevation = 12.dp,
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🌟 Assign Blank Tile",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = palette.primaryColor
                )
                Text(
                    text = "Choose which letter this blank tile represents:",
                    fontSize = 13.sp,
                    color = palette.tileTextColor.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                val alphabet = ('A'..'Z').toList()
                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth().height(260.dp)
                ) {
                    items(alphabet) { char ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = palette.tileColor,
                            border = BorderStroke(1.dp, palette.tileBorder),
                            shadowElevation = 2.dp,
                            modifier = Modifier
                                .size(42.dp)
                                .clickable { onLetterSelected(char) }
                                .testTag("blank_letter_$char")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = char.toString(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = palette.tileTextColor
                                )
                            }
                        }
                    }
                }

                DialogCreditsSubtext(palette)
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = palette.secondaryColor)
                }
            }
        }
    }
}

/**
 * Tile exchange / swap dialog
 */
@Composable
fun TileSwapDialog(
    rack: List<Tile>,
    bagCount: Int,
    palette: GameThemePalette,
    onConfirmSwap: (List<Tile>) -> Unit,
    onDismiss: () -> Unit
) {
    val selectedTiles = remember { mutableStateListOf<Tile>() }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = palette.cellBackground,
            border = BorderStroke(1.5.dp, palette.primaryColor),
            shadowElevation = 12.dp,
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🔁 Swap Tiles",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = palette.primaryColor
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = palette.tileTextColor)
                    }
                }

                Text(
                    text = "Select tiles from your rack to return to the bag ($bagCount available). Swapping ends your turn.",
                    fontSize = 13.sp,
                    color = palette.tileTextColor.copy(alpha = 0.8f),
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                // Rack selection row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(vertical = 12.dp)
                ) {
                    rack.forEach { tile ->
                        val isSelected = selectedTiles.contains(tile)
                        ScrabbleTileView(
                            tile = tile,
                            palette = palette,
                            size = 40.dp,
                            isSelected = isSelected,
                            onClick = {
                                if (isSelected) selectedTiles.remove(tile)
                                else selectedTiles.add(tile)
                            }
                        )
                    }
                }

                Text(
                    text = "${selectedTiles.size} tile(s) selected",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.secondaryColor
                )

                DialogCreditsSubtext(palette)

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = palette.tileTextColor)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirmSwap(selectedTiles.toList()) },
                        enabled = selectedTiles.isNotEmpty() && bagCount >= selectedTiles.size,
                        colors = ButtonDefaults.buttonColors(containerColor = palette.primaryColor)
                    ) {
                        Text("Exchange Tiles")
                    }
                }
            }
        }
    }
}

/**
 * Customizable Difficulty & AI Configuration Dialog
 */
@Composable
fun DifficultySettingsDialog(
    currentDifficulty: Difficulty,
    customSettings: CustomDifficultySettings,
    palette: GameThemePalette,
    onSaveDifficulty: (Difficulty, CustomDifficultySettings) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedDiff by remember { mutableStateOf(currentDifficulty) }
    var vocabLevel by remember { mutableFloatStateOf(customSettings.vocabularyLevel) }
    var aggressiveness by remember { mutableFloatStateOf(customSettings.aggressiveness) }
    var maxWordLen by remember { mutableIntStateOf(customSettings.maxWordLength) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = palette.cellBackground,
            border = BorderStroke(1.5.dp, palette.primaryColor),
            shadowElevation = 12.dp,
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "⚙️ AI Difficulty & Personality",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = palette.primaryColor
                )
                Text(
                    text = "Tailor your opponent's skill to your playstyle:",
                    fontSize = 12.sp,
                    color = palette.tileTextColor.copy(alpha = 0.7f),
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Difficulty options
                Difficulty.values().forEach { diff ->
                    val isSelected = selectedDiff == diff
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { selectedDiff = diff },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) palette.primaryColor.copy(alpha = 0.12f) else palette.tileColor
                        ),
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) palette.primaryColor else palette.tileBorder
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = diff.avatarEmoji, fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${diff.title} (${diff.botName})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = palette.tileTextColor
                                )
                                Text(
                                    text = diff.description,
                                    fontSize = 11.sp,
                                    color = palette.tileTextColor.copy(alpha = 0.75f)
                                )
                            }
                            RadioButton(
                                selected = isSelected,
                                onClick = { selectedDiff = diff },
                                colors = RadioButtonDefaults.colors(selectedColor = palette.primaryColor)
                            )
                        }
                    }
                }

                // If Custom is selected, show customizable sliders
                if (selectedDiff == Difficulty.CUSTOM) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        Text(
                            text = "Vocabulary Depth: ${(vocabLevel * 100).toInt()}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.tileTextColor
                        )
                        Slider(
                            value = vocabLevel,
                            onValueChange = { vocabLevel = it },
                            valueRange = 0.2f..1.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = palette.primaryColor,
                                activeTrackColor = palette.primaryColor
                            )
                        )

                        Text(
                            text = "Aggressiveness / Multipliers: ${(aggressiveness * 100).toInt()}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.tileTextColor
                        )
                        Slider(
                            value = aggressiveness,
                            onValueChange = { aggressiveness = it },
                            valueRange = 0.1f..1.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = palette.secondaryColor,
                                activeTrackColor = palette.secondaryColor
                            )
                        )

                        Text(
                            text = "Max AI Word Length: $maxWordLen letters",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.tileTextColor
                        )
                        Slider(
                            value = maxWordLen.toFloat(),
                            onValueChange = { maxWordLen = it.toInt() },
                            valueRange = 3f..8f,
                            steps = 4,
                            colors = SliderDefaults.colors(
                                thumbColor = palette.primaryColor,
                                activeTrackColor = palette.primaryColor
                            )
                        )
                    }
                }

                DialogCreditsSubtext(palette)

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = palette.tileTextColor)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val newCustom = CustomDifficultySettings(
                                vocabularyLevel = vocabLevel,
                                aggressiveness = aggressiveness,
                                maxWordLength = maxWordLen
                            )
                            onSaveDifficulty(selectedDiff, newCustom)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = palette.primaryColor)
                    ) {
                        Text("Apply Settings")
                    }
                }
            }
        }
    }
}

/**
 * Visual Theme & Mode Selector Dialog
 */
@Composable
fun ThemePickerDialog(
    currentTheme: GameThemePalette,
    currentThemeMode: ThemeMode,
    palette: GameThemePalette,
    onSelectTheme: (GameThemePalette) -> Unit,
    onSelectThemeMode: (ThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = palette.cellBackground,
            border = BorderStroke(1.5.dp, palette.primaryColor),
            shadowElevation = 14.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "🎨 Appearance & Themes",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = palette.primaryColor
                        )
                        Text(
                            text = "Choose display mode and board color palette",
                            fontSize = 12.sp,
                            color = palette.tileTextColor.copy(alpha = 0.75f)
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = palette.tileTextColor.copy(alpha = 0.8f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section 1: Display Mode (System / Light / Dark)
                Text(
                    text = "DISPLAY MODE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                    color = palette.primaryColor
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(palette.boardBackground.copy(alpha = 0.5f))
                        .border(1.dp, palette.tileBorder.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ThemeMode.values().forEach { mode ->
                        val isModeSelected = currentThemeMode == mode
                        val (icon, label) = when (mode) {
                            ThemeMode.SYSTEM -> "📱" to "System"
                            ThemeMode.LIGHT -> "☀️" to "Light"
                            ThemeMode.DARK -> "🌙" to "Dark"
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isModeSelected) palette.primaryColor else Color.Transparent,
                            shadowElevation = if (isModeSelected) 2.dp else 0.dp,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onSelectThemeMode(mode) }
                                .testTag("theme_mode_${mode.name.lowercase()}")
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = icon,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = label,
                                    fontWeight = if (isModeSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.sp,
                                    color = if (isModeSelected) Color.White else palette.tileTextColor.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }

                // Subtitle explanation for current mode
                Text(
                    text = currentThemeMode.subtitle,
                    fontSize = 11.sp,
                    color = palette.tileTextColor.copy(alpha = 0.65f),
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Section 2: Dark Background Palettes
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "🌙 DARK BACKGROUND PALETTES",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp,
                        color = palette.primaryColor
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF263238),
                        border = BorderStroke(1.dp, Color(0xFF455A64))
                    ) {
                        Text(
                            text = "Eye Comfort",
                            color = Color(0xFF81D4FA),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                GameThemePalette.darkThemes.forEach { theme ->
                    ThemeCard(
                        theme = theme,
                        isSelected = currentTheme == theme,
                        onSelect = {
                            onSelectTheme(theme)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section 3: Light Background Palettes
                Text(
                    text = "☀️ LIGHT BACKGROUND PALETTES",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                    color = palette.primaryColor
                )

                Spacer(modifier = Modifier.height(6.dp))

                GameThemePalette.lightThemes.forEach { theme ->
                    ThemeCard(
                        theme = theme,
                        isSelected = currentTheme == theme,
                        onSelect = {
                            onSelectTheme(theme)
                        }
                    )
                }

                DialogCreditsSubtext(palette)

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = palette.primaryColor),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Done", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeCard(
    theme: GameThemePalette,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onSelect)
            .testTag("theme_card_${theme.name.lowercase()}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = theme.backgroundGradientTop),
        border = BorderStroke(
            width = if (isSelected) 2.5.dp else 1.dp,
            color = if (isSelected) theme.primaryColor else theme.tileBorder.copy(alpha = 0.7f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 3.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Theme Details & Swatches
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = theme.themeName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (theme.isDark) Color(0xFFECEFF1) else theme.primaryColor
                    )
                    if (theme.isDark) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "🌙", fontSize = 11.sp)
                    }
                }

                Text(
                    text = theme.subtitle,
                    fontSize = 11.sp,
                    color = if (theme.isDark) Color(0xFFB0BEC5) else theme.tileTextColor.copy(alpha = 0.7f)
                )

                // Swatch Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 6.dp)
                ) {
                    Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(theme.primaryColor).border(0.5.dp, Color.White.copy(alpha = 0.3f), CircleShape))
                    Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(theme.secondaryColor).border(0.5.dp, Color.White.copy(alpha = 0.3f), CircleShape))
                    Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(theme.twsColor).border(0.5.dp, Color.White.copy(alpha = 0.3f), CircleShape))
                    Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(theme.tlsColor).border(0.5.dp, Color.White.copy(alpha = 0.3f), CircleShape))
                    Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(theme.boardBackground).border(0.5.dp, Color.White.copy(alpha = 0.3f), CircleShape))
                }
            }

            // Live Mini Scrabble Tile Preview in this theme's style
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = theme.tileColor,
                    border = BorderStroke(1.dp, theme.tileBorder),
                    shadowElevation = 2.dp,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "A",
                            color = theme.tileTextColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "1",
                            color = theme.tileTextColor.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.sp,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 2.dp, bottom = 1.dp)
                        )
                    }
                }

                if (isSelected) {
                    Surface(
                        shape = CircleShape,
                        color = theme.primaryColor,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .border(1.5.dp, theme.tileBorder.copy(alpha = 0.5f), CircleShape)
                    )
                }
            }
        }
    }
}

/**
 * Move History & Game Replay Sheet
 */
@Composable
fun MoveHistoryDialog(
    logs: List<GameLogEntry>,
    palette: GameThemePalette,
    onWordClick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var showChart by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = palette.cellBackground,
            border = BorderStroke(1.5.dp, palette.primaryColor),
            shadowElevation = 12.dp,
            modifier = Modifier.fillMaxWidth().height(460.dp).padding(8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📜 Move History",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = palette.primaryColor
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = palette.tileTextColor)
                    }
                }

                // Sleek Segmented Switch Tab Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .background(palette.tileColor.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // List Log Tab Button
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showChart = false },
                        shape = RoundedCornerShape(8.dp),
                        color = if (!showChart) palette.primaryColor else Color.Transparent
                    ) {
                        Text(
                            text = "📜 Turn Log",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (!showChart) Color.White else palette.tileTextColor.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }

                    // Chart Progression Tab Button
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showChart = true },
                        shape = RoundedCornerShape(8.dp),
                        color = if (showChart) palette.primaryColor else Color.Transparent
                    ) {
                        Text(
                            text = "📊 Score Chart",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (showChart) Color.White else palette.tileTextColor.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                }

                if (logs.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No moves played yet. Make your first play!", color = palette.tileTextColor.copy(alpha = 0.6f))
                    }
                } else if (showChart) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        ScoreProgressionChart(logs = logs, palette = palette)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(logs) { log ->
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (log.isPlayer) palette.primaryColor.copy(alpha = 0.08f) else palette.secondaryColor.copy(alpha = 0.08f)
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (log.isPlayer) palette.primaryColor.copy(alpha = 0.3f) else palette.secondaryColor.copy(alpha = 0.3f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (log.isPlayer) "👤" else "🤖",
                                            fontSize = 18.sp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = if (log.isPassOrSwap) log.description else log.word,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = palette.tileTextColor
                                            )
                                            if (!log.isPassOrSwap) {
                                                Text(
                                                    text = "Tap to view definition",
                                                    fontSize = 10.sp,
                                                    color = palette.primaryColor
                                                )
                                            }
                                        }
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (log.points > 0) "+${log.points} pts" else "0 pts",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = if (log.isPlayer) palette.primaryColor else palette.secondaryColor
                                        )
                                        if (!log.isPassOrSwap) {
                                            IconButton(
                                                onClick = { onWordClick(log.word) },
                                                modifier = Modifier.size(24.dp).padding(start = 4.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Info,
                                                    contentDescription = "Definition",
                                                    tint = palette.primaryColor,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                DialogCreditsSubtext(palette)
            }
        }
    }
}

/**
 * Word Definition and Meaning Pop-up
 */
@Composable
fun WordDefinitionDialog(
    word: String,
    palette: GameThemePalette,
    onDismiss: () -> Unit
) {
    val definition = WordDictionary.getDefinition(word)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(painter = painterResource(R.drawable.ic_auto_awesome), contentDescription = null, tint = palette.secondaryColor)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = word.uppercase(),
                    fontWeight = FontWeight.Bold,
                    color = palette.primaryColor
                )
            }
        },
        text = {
            Column {
                Text(
                    text = "Scrabble Word Definition:",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = palette.tileTextColor.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = definition,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = palette.tileTextColor
                )
                DialogCreditsSubtext(palette)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got it", color = palette.primaryColor)
            }
        }
    )
}

/**
 * Game Over Summary Modal
 */
@Composable
fun GameOverDialog(
    playerScore: Int,
    aiScore: Int,
    bestWord: String,
    bestWordPoints: Int,
    difficulty: Difficulty,
    palette: GameThemePalette,
    onNewGame: () -> Unit,
    onDismiss: () -> Unit
) {
    val isWin = playerScore > aiScore
    val isTie = playerScore == aiScore

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = palette.cellBackground,
            border = BorderStroke(2.dp, if (isWin) palette.primaryColor else palette.secondaryColor),
            shadowElevation = 16.dp,
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isWin) "🎉 VICTORY!" else if (isTie) "🤝 IT'S A TIE!" else "👏 GOOD GAME!",
                    fontWeight = FontWeight.Black,
                    fontSize = 24.sp,
                    color = if (isWin) palette.primaryColor else palette.secondaryColor
                )

                Text(
                    text = if (isWin) "You defeated ${difficulty.botName}!" else "${difficulty.botName} won this match.",
                    fontSize = 14.sp,
                    color = palette.tileTextColor.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                // Score banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = palette.tileColor),
                    border = BorderStroke(1.dp, palette.tileBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("YOU", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = palette.primaryColor)
                            Text("$playerScore", fontWeight = FontWeight.Black, fontSize = 28.sp, color = palette.tileTextColor)
                        }
                        Text("vs", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = palette.tileBorder)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(difficulty.botName.uppercase(), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = palette.secondaryColor)
                            Text("$aiScore", fontWeight = FontWeight.Black, fontSize = 28.sp, color = palette.tileTextColor)
                        }
                    }
                }

                if (bestWord.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = palette.primaryColor.copy(alpha = 0.08f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🌟 Best Word: $bestWord",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = palette.primaryColor
                            )
                            Text(
                                text = "+$bestWordPoints pts",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = palette.secondaryColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onNewGame,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = palette.primaryColor),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Play Again", fontWeight = FontWeight.Bold)
                }
                DialogCreditsSubtext(palette)
            }
        }
    }
}

/**
 * Statistics Dialog
 */
@Composable
fun GameStatsDialog(
    stats: GameStats,
    palette: GameThemePalette,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = palette.cellBackground,
            border = BorderStroke(1.5.dp, palette.primaryColor),
            shadowElevation = 12.dp,
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(painter = painterResource(R.drawable.ic_emoji_events), contentDescription = null, tint = palette.secondaryColor)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Game Statistics",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = palette.primaryColor
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = palette.tileTextColor)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                StatRow("Games Played", "${stats.gamesPlayed}", palette)
                StatRow("Victories Won", "${stats.wins}", palette)
                StatRow("Win Rate", if (stats.gamesPlayed > 0) "${(stats.wins * 100) / stats.gamesPlayed}%" else "0%", palette)
                StatRow("Highest Game Score", "${stats.highestScore} pts", palette)
                StatRow("Best Word Played", if (stats.highestScoringWord.isNotEmpty()) "${stats.highestScoringWord} (${stats.highestWordPoints} pts)" else "-", palette)
                StatRow("Total Words Formed", "${stats.totalWordsPlayed}", palette)

                DialogCreditsSubtext(palette)

                Spacer(modifier = Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = palette.primaryColor)
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

@Composable
private fun StatRow(title: String, value: String, palette: GameThemePalette) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = title, fontSize = 13.sp, color = palette.tileTextColor.copy(alpha = 0.8f))
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = palette.primaryColor)
    }
}

@Composable
fun ScoreProgressionChart(
    logs: List<GameLogEntry>,
    palette: GameThemePalette
) {
    // Reconstruct chronological score progression
    val chronological = logs.reversed()
    data class TurnScore(val turnIndex: Int, val playerSum: Int, val aiSum: Int)
    
    val pointsList = mutableListOf<TurnScore>()
    var pSum = 0
    var aSum = 0
    var turnCounter = 1
    
    for (entry in chronological) {
        if (entry.isPlayer) {
            pSum += entry.points
        } else {
            aSum += entry.points
        }
        pointsList.add(TurnScore(turnCounter++, pSum, aSum))
    }

    if (pointsList.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No score progression data available yet.",
                color = palette.tileTextColor.copy(alpha = 0.5f),
                fontSize = 13.sp
            )
        }
        return
    }

    val maxScore = maxOf(pSum, aSum, 10).toFloat()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        // Legend
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(palette.primaryColor, RoundedCornerShape(2.dp))
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Player", fontSize = 11.sp, color = palette.tileTextColor, fontWeight = FontWeight.Medium)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(palette.secondaryColor, RoundedCornerShape(2.dp))
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Computer", fontSize = 11.sp, color = palette.tileTextColor, fontWeight = FontWeight.Medium)
            }
        }

        // Horizontal scrollable Container
        androidx.compose.foundation.lazy.LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(palette.tileColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                .border(1.dp, palette.tileBorder.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            items(pointsList) { turn ->
                val playerBarHeightRatio = turn.playerSum / maxScore
                val aiBarHeightRatio = turn.aiSum / maxScore

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(52.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .height(180.dp)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            // Player Bar
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "${turn.playerSum}",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.primaryColor,
                                    modifier = Modifier.padding(bottom = 2.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height((160 * playerBarHeightRatio).dp)
                                        .background(
                                            palette.primaryColor,
                                            RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)
                                        )
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            // AI Bar
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "${turn.aiSum}",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.secondaryColor,
                                    modifier = Modifier.padding(bottom = 2.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height((160 * aiBarHeightRatio).dp)
                                        .background(
                                            palette.secondaryColor,
                                            RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)
                                        )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Turn ${turn.turnIndex}",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.tileTextColor.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
fun DialogCreditsSubtext(palette: GameThemePalette) {
    Spacer(modifier = Modifier.height(12.dp))
    Text(
        text = "Created by Hari Burle with Google AI Studio",
        color = palette.tileTextColor.copy(alpha = 0.45f),
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
    )
}

