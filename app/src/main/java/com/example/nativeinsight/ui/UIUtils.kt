package com.example.nativeinsight.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

// 1. Data class to hold the visual theme for a card
data class CategoryStyle(
    val icon: ImageVector,
    val color: Color,
    val label: String
)

// 2. Logic to map your "FlashcardParser" categories to Icons/Colors
fun getCategoryStyle(categoryRaw: String): CategoryStyle {
    return when {
        categoryRaw.contains("Music", ignoreCase = true) || categoryRaw.contains("Urban", ignoreCase = true) -> CategoryStyle(
            icon = Icons.Default.PlayArrow, // Represents Music/Media
            color = Color(0xFFBD93F9),      // Purple
            label = "Music & Vibe"
        )
        categoryRaw.contains("Workplace", ignoreCase = true) || categoryRaw.contains("Data", ignoreCase = true) -> CategoryStyle(
            icon = Icons.Default.Settings,  // Represents Work/Tools
            color = Color(0xFFFFB86C),      // Orange
            label = "Work & Data"
        )
        categoryRaw.contains("Health", ignoreCase = true) || categoryRaw.contains("Wellbeing", ignoreCase = true) -> CategoryStyle(
            icon = Icons.Default.Favorite,  // Represents Health
            color = Color(0xFF50FA7B),      // Green
            label = "Health"
        )
        categoryRaw.contains("Social", ignoreCase = true) -> CategoryStyle(
            icon = Icons.Default.Face,      // Represents Social
            color = Color(0xFF8BE9FD),      // Cyan
            label = "Social"
        )
        else -> CategoryStyle(
            icon = Icons.Default.Star,      // Default/General
            color = Color(0xFFF8F8F2),      // White/Grey
            label = categoryRaw.ifBlank { "General" }
        )
    }
}

// 3. The Composable that makes the icon "float" up and down
@Composable
fun FloatingIcon(
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "floatAnim")

    // Animate Y position slightly to create a breathing/hovering effect
    val dy by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -8f, // Move up 8dp
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dy"
    )

    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = tint,
        modifier = modifier.offset(y = dy.dp)
    )
}