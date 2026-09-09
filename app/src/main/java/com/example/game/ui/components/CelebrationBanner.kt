package com.example.game.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.game.model.GameThemePalette

@Composable
fun CelebrationBanner(
    word: String?,
    points: Int,
    isBingo: Boolean,
    palette: GameThemePalette,
    modifier: Modifier = Modifier
) {
    // Completely disabled celebratory banner to prevent high-score word play crashes on mobile devices
}

@Composable
fun CelebrationParticles() {
    // Disabled particles
}

