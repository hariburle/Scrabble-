package com.example.game.model

import androidx.compose.ui.graphics.Color
import java.util.UUID

/**
 * Standard Scrabble Letter Point Values
 */
fun getLetterPoints(char: Char): Int {
    return when (char.uppercaseChar()) {
        'A', 'E', 'I', 'O', 'U', 'L', 'N', 'S', 'T', 'R' -> 1
        'D', 'G' -> 2
        'B', 'C', 'M', 'P' -> 3
        'F', 'H', 'V', 'W', 'Y' -> 4
        'K' -> 5
        'J', 'X' -> 8
        'Q', 'Z' -> 10
        ' ' -> 0
        else -> 0
    }
}

/**
 * A single Scrabble tile
 */
data class Tile(
    val id: String = UUID.randomUUID().toString(),
    val letter: Char, // ' ' represents a blank tile
    val assignedLetter: Char? = null, // letter assigned to blank tile on board
    val points: Int = getLetterPoints(letter),
    val isBlank: Boolean = letter == ' '
) {
    val displayLetter: Char
        get() = if (isBlank && assignedLetter != null) assignedLetter else letter
}

/**
 * Premium multiplier squares on the 15x15 Scrabble board
 */
enum class BonusType(
    val shortLabel: String,
    val fullLabel: String,
    val letterMultiplier: Int,
    val wordMultiplier: Int
) {
    NONE("", "Normal", 1, 1),
    DOUBLE_LETTER("2L", "Double Letter Score", 2, 1),
    TRIPLE_LETTER("3L", "Triple Letter Score", 3, 1),
    DOUBLE_WORD("2W", "Double Word Score", 1, 2),
    TRIPLE_WORD("3W", "Triple Word Score", 1, 3),
    CENTER("★", "Center Start (2W)", 1, 2)
}

/**
 * Single cell in 15x15 board
 */
data class BoardCell(
    val row: Int,
    val col: Int,
    val bonusType: BonusType = BonusType.NONE,
    val tile: Tile? = null, // Committed tile from previous turns
    val isHighlighted: Boolean = false // Highlight for last move
)

/**
 * Placement of a tile this turn
 */
data class PlacedTile(
    val row: Int,
    val col: Int,
    val tile: Tile
)

/**
 * Word formed on board and its score breakdown
 */
data class FormedWord(
    val word: String,
    val points: Int,
    val startRow: Int,
    val startCol: Int,
    val isHorizontal: Boolean
)

/**
 * Result of scoring a move
 */
data class MoveScoreResult(
    val isValid: Boolean,
    val errorMessage: String? = null,
    val primaryWord: FormedWord? = null,
    val crossWords: List<FormedWord> = emptyList(),
    val totalScore: Int = 0,
    val isBingo: Boolean = false
) {
    val allWords: List<FormedWord>
        get() = listOfNotNull(primaryWord) + crossWords
}

/**
 * AI Difficulty Levels
 */
enum class Difficulty(
    val title: String,
    val botName: String,
    val avatarEmoji: String,
    val description: String,
    val maxScoreTarget: Int,
    val vocabularyRatio: Float,
    val aggressiveness: Float
) {
    RELAXED(
        title = "Relaxed",
        botName = "Zen Sage",
        avatarEmoji = "🧘",
        description = "Plays gentle, friendly words (8-22 pts). Perfect for casual relaxation.",
        maxScoreTarget = 22,
        vocabularyRatio = 0.4f,
        aggressiveness = 0.2f
    ),
    BALANCED(
        title = "Balanced",
        botName = "Scholar Oliver",
        avatarEmoji = "🦉",
        description = "Thoughtful and competent player (20-38 pts). Good standard challenge.",
        maxScoreTarget = 38,
        vocabularyRatio = 0.75f,
        aggressiveness = 0.6f
    ),
    MASTER(
        title = "Master",
        botName = "Grandmaster Nova",
        avatarEmoji = "⚡",
        description = "Maximizes multipliers, plays high-value words and bingos (35-70+ pts).",
        maxScoreTarget = 999,
        vocabularyRatio = 1.0f,
        aggressiveness = 1.0f
    ),
    CUSTOM(
        title = "Custom",
        botName = "Custom Bot",
        avatarEmoji = "⚙️",
        description = "Fully personalized AI speed, vocabulary size, and bonus aggressiveness.",
        maxScoreTarget = 50,
        vocabularyRatio = 0.6f,
        aggressiveness = 0.5f
    )
}

/**
 * Custom settings for AI
 */
data class CustomDifficultySettings(
    val vocabularyLevel: Float = 0.65f, // 0.2 to 1.0
    val aggressiveness: Float = 0.5f,   // 0.1 to 1.0
    val maxWordLength: Int = 6          // 3 to 8
)

/**
 * Display Theme Mode (System / Light / Dark)
 */
enum class ThemeMode(
    val title: String,
    val subtitle: String
) {
    SYSTEM("System Default", "Follows device dark/light theme"),
    LIGHT("Light Mode", "Bright, daytime aesthetic"),
    DARK("Dark Mode", "Sleek, eye-friendly dark background")
}

/**
 * Visual Color Themes for soothing aesthetic
 */
enum class GameThemePalette(
    val themeName: String,
    val subtitle: String,
    val isDark: Boolean,
    val primaryColor: Color,
    val secondaryColor: Color,
    val backgroundGradientTop: Color,
    val backgroundGradientBottom: Color,
    val boardBackground: Color,
    val cellBackground: Color,
    val tileColor: Color,
    val tileBorder: Color,
    val tileTextColor: Color,
    val twsColor: Color,
    val dwsColor: Color,
    val tlsColor: Color,
    val dlsColor: Color,
    val centerColor: Color
) {
    // Light Background Themes
    BOTANICAL_ZEN(
        themeName = "Botanical Zen",
        subtitle = "Forest greens & warm clay",
        isDark = false,
        primaryColor = Color(0xFF2D5A43),
        secondaryColor = Color(0xFFE29578),
        backgroundGradientTop = Color(0xFFF4F7F4),
        backgroundGradientBottom = Color(0xFFE5EDE6),
        boardBackground = Color(0xFFD3DFD5),
        cellBackground = Color(0xFFEBF1EC),
        tileColor = Color(0xFFFAF7F2),
        tileBorder = Color(0xFFCEC3B1),
        tileTextColor = Color(0xFF253526),
        twsColor = Color(0xFFE59880),
        dwsColor = Color(0xFFF2C299),
        tlsColor = Color(0xFF90B4CE),
        dlsColor = Color(0xFFC3DFE0),
        centerColor = Color(0xFF81B29A)
    ),
    PASTEL_SUNSET(
        themeName = "Pastel Sunset",
        subtitle = "Lavender skies & soft blush",
        isDark = false,
        primaryColor = Color(0xFF9368B7),
        secondaryColor = Color(0xFFF78CA0),
        backgroundGradientTop = Color(0xFFFAF3F7),
        backgroundGradientBottom = Color(0xFFF3E7ED),
        boardBackground = Color(0xFFEAD8DE),
        cellBackground = Color(0xFFFBF1F4),
        tileColor = Color(0xFFFFF9F5),
        tileBorder = Color(0xFFEAD2C6),
        tileTextColor = Color(0xFF4A3448),
        twsColor = Color(0xFFEFA7B8),
        dwsColor = Color(0xFFF8C6CB),
        tlsColor = Color(0xFFB8A9D6),
        dlsColor = Color(0xFFCCE8E8),
        centerColor = Color(0xFFEFA7B8)
    ),
    NORDIC_SLATE(
        themeName = "Nordic Slate",
        subtitle = "Ocean slate & golden oak",
        isDark = false,
        primaryColor = Color(0xFF385A64),
        secondaryColor = Color(0xFFE0A96D),
        backgroundGradientTop = Color(0xFFF0F4F6),
        backgroundGradientBottom = Color(0xFFDFE7EB),
        boardBackground = Color(0xFFCAD4DA),
        cellBackground = Color(0xFFE8EEF1),
        tileColor = Color(0xFFFAF8F5),
        tileBorder = Color(0xFFC7CBD1),
        tileTextColor = Color(0xFF1E2D33),
        twsColor = Color(0xFFE59A96),
        dwsColor = Color(0xFFF6CE98),
        tlsColor = Color(0xFF8FAEC5),
        dlsColor = Color(0xFFB8DCE5),
        centerColor = Color(0xFF8FAEC5)
    ),
    WARM_WALNUT(
        themeName = "Warm Walnut",
        subtitle = "Rich woodgrain & caramel",
        isDark = false,
        primaryColor = Color(0xFF5C3D2E),
        secondaryColor = Color(0xFFB85D19),
        backgroundGradientTop = Color(0xFFFAF7F2),
        backgroundGradientBottom = Color(0xFFEFE8DE),
        boardBackground = Color(0xFFDDCFBD),
        cellBackground = Color(0xFFF6F0E7),
        tileColor = Color(0xFFFFFDF9),
        tileBorder = Color(0xFFD6C5B3),
        tileTextColor = Color(0xFF3E2723),
        twsColor = Color(0xFFDC9292),
        dwsColor = Color(0xFFECC2A0),
        tlsColor = Color(0xFF8CAFC4),
        dlsColor = Color(0xFFBDD5DF),
        centerColor = Color(0xFFB59379)
    ),

    // Dark Background Themes
    MIDNIGHT_OBSIDIAN(
        themeName = "Midnight Obsidian",
        subtitle = "Sleek onyx & crystal ice",
        isDark = true,
        primaryColor = Color(0xFF90CAF9),
        secondaryColor = Color(0xFFFFB74D),
        backgroundGradientTop = Color(0xFF101216),
        backgroundGradientBottom = Color(0xFF181B20),
        boardBackground = Color(0xFF222730),
        cellBackground = Color(0xFF2B323E),
        tileColor = Color(0xFF3A4354),
        tileBorder = Color(0xFF556276),
        tileTextColor = Color(0xFFF0F4F8),
        twsColor = Color(0xFF9E4747),
        dwsColor = Color(0xFFA6644E),
        tlsColor = Color(0xFF345F8A),
        dlsColor = Color(0xFF2E6B75),
        centerColor = Color(0xFF3E7543)
    ),
    DEEP_FOREST_DARK(
        themeName = "Deep Forest Dark",
        subtitle = "Pine evergreen & mint glow",
        isDark = true,
        primaryColor = Color(0xFF81C784),
        secondaryColor = Color(0xFFFFB74D),
        backgroundGradientTop = Color(0xFF0D1712),
        backgroundGradientBottom = Color(0xFF15241C),
        boardBackground = Color(0xFF1E3328),
        cellBackground = Color(0xFF263F32),
        tileColor = Color(0xFF345443),
        tileBorder = Color(0xFF4E7B63),
        tileTextColor = Color(0xFFF1F8F4),
        twsColor = Color(0xFF9E4747),
        dwsColor = Color(0xFFA6644E),
        tlsColor = Color(0xFF345F8A),
        dlsColor = Color(0xFF2E6B75),
        centerColor = Color(0xFF3F6E4D)
    ),
    COZY_ESPRESSO_DARK(
        themeName = "Espresso Dark",
        subtitle = "Dark roast & warm amber",
        isDark = true,
        primaryColor = Color(0xFFFFB74D),
        secondaryColor = Color(0xFFEF9A9A),
        backgroundGradientTop = Color(0xFF16100D),
        backgroundGradientBottom = Color(0xFF221814),
        boardBackground = Color(0xFF30221C),
        cellBackground = Color(0xFF3D2C24),
        tileColor = Color(0xFF523B30),
        tileBorder = Color(0xFF725445),
        tileTextColor = Color(0xFFFFF7ED),
        twsColor = Color(0xFF9E4747),
        dwsColor = Color(0xFFA6714E),
        tlsColor = Color(0xFF3A6185),
        dlsColor = Color(0xFF336C73),
        centerColor = Color(0xFF8C5D38)
    ),
    AMETHYST_NIGHT(
        themeName = "Amethyst Night",
        subtitle = "Midnight twilight & violet",
        isDark = true,
        primaryColor = Color(0xFFCE93D8),
        secondaryColor = Color(0xFFF48FB1),
        backgroundGradientTop = Color(0xFF140F1C),
        backgroundGradientBottom = Color(0xFF20182C),
        boardBackground = Color(0xFF2B203B),
        cellBackground = Color(0xFF382A4C),
        tileColor = Color(0xFF4C3964),
        tileBorder = Color(0xFF6C528D),
        tileTextColor = Color(0xFFFAF5FF),
        twsColor = Color(0xFF964567),
        dwsColor = Color(0xFFA85978),
        tlsColor = Color(0xFF58487C),
        dlsColor = Color(0xFF386872),
        centerColor = Color(0xFF724F88)
    );

    companion object {
        val lightThemes: List<GameThemePalette>
            get() = entries.filter { !it.isDark }

        val darkThemes: List<GameThemePalette>
            get() = entries.filter { it.isDark }
    }
}

/**
 * Move history item for game log
 */
data class GameLogEntry(
    val turnNumber: Int,
    val isPlayer: Boolean,
    val word: String,
    val points: Int,
    val description: String,
    val secondaryWords: List<String> = emptyList(),
    val isPassOrSwap: Boolean = false
)

/**
 * High score and game stats
 */
data class GameStats(
    val gamesPlayed: Int = 0,
    val wins: Int = 0,
    val highestScore: Int = 0,
    val highestScoringWord: String = "",
    val highestWordPoints: Int = 0,
    val totalWordsPlayed: Int = 0
)
