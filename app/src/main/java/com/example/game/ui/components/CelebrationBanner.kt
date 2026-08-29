package com.example.game.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.model.GameThemePalette

@Composable
fun CelebrationBanner(
    word: String?,
    points: Int,
    isBingo: Boolean,
    palette: GameThemePalette,
    modifier: Modifier = Modifier
) {
    val isActive = word != null

    AnimatedVisibility(
        visible = isActive,
        enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
        exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top),
        modifier = modifier.fillMaxWidth()
    ) {
        if (word != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = if (isBingo || points >= 40) {
                                listOf(
                                    Color(0xFFFFD54F), // Amber
                                    Color(0xFFFF7043), // Deep coral orange
                                    Color(0xFFAB47BC)  // Purple royal
                                )
                            } else {
                                listOf(
                                    palette.primaryColor,
                                    palette.secondaryColor
                                )
                            }
                        )
                    )
                    .border(
                        width = 2.dp,
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                Color(0xFFFFD700), // Pure gold
                                Color(0xFFFFA500), // Orange
                                Color(0xFFFFD700)
                            )
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                // Background animated particles
                CelebrationParticles()

                // Main Content
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse_label")
                    val pulseScale by infiniteTransition.animateFloat(
                        initialValue = 0.95f,
                        targetValue = 1.05f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(600, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "scale"
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.15f),
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = pulseScale
                                scaleY = pulseScale
                            }
                    ) {
                        Text(
                            text = if (isBingo) "🎉 BINGO SPECIAL! 🎉" else "✨ AMAZING WORD! ✨",
                            color = Color(0xFFFFFFD0),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = word.uppercase(),
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.SansSerif,
                            letterSpacing = 1.5.sp
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // Points badge
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0xFFFFD700))
                                .border(1.5.dp, Color.White, CircleShape)
                                .size(34.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+$points",
                                color = Color(0xFF3E2723),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "Nice playing! Keep racking up those points!",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun CelebrationParticles() {
    val infiniteTransition = rememberInfiniteTransition(label = "particles")
    
    // Spawn 5 independent animated particles drifting across the background
    val p1Offset by infiniteTransition.animateFloat(
        initialValue = -50f,
        targetValue = 50f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "p1"
    )
    val p1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "p1a"
    )

    val p2Offset by infiniteTransition.animateFloat(
        initialValue = 60f,
        targetValue = -60f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "p2"
    )

    val rotateAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rot"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Star Left
        Text(
            text = "✨",
            color = Color.White,
            fontSize = 18.sp,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = p1Offset.dp, y = (p1Offset / 2).dp)
                .alpha(p1Alpha)
                .graphicsLayer { rotationZ = rotateAngle }
        )

        // Star Right
        Text(
            text = "🌟",
            color = Color(0xFFFFD700),
            fontSize = 20.sp,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = p2Offset.dp, y = -(p1Offset / 3).dp)
                .alpha(p1Alpha + 0.1f)
                .graphicsLayer { rotationZ = -rotateAngle }
        )

        // Party Popper Top Left
        Text(
            text = "🎉",
            fontSize = 16.sp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = (p1Offset / 3).dp, y = p2Offset.dp / 4)
                .alpha(0.6f)
        )

        // Trophy Bottom Right
        Text(
            text = "🏆",
            fontSize = 16.sp,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = -(p2Offset / 2).dp, y = -p1Offset.dp / 5)
                .alpha(0.5f)
        )
    }
}
