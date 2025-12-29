package com.example.nativeinsight.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.nativeinsight.data.RevisionStat
import com.example.nativeinsight.viewmodel.DashboardViewModel

@Composable
fun AnalyticsScreen(viewModel: DashboardViewModel) {
    val stats by viewModel.analyticsData.collectAsState()
    val totalCards by viewModel.totalCardsCount.collectAsState()

    val maxCount = remember(stats) { stats.maxOfOrNull { it.cardCount } ?: 1 }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Knowledge Distribution", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("$totalCards total cards tracked", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(stats) { stat ->
                RevisionBarRow(stat, maxCount)
            }
        }
    }
}

@Composable
fun RevisionBarRow(stat: RevisionStat, maxCount: Int) {
    val barColor = getBarColor(stat.revisionLevel)
    val fillFraction = stat.cardCount.toFloat() / maxCount.toFloat()
    val animatedWidth by animateFloatAsState(
        targetValue = fillFraction,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "width"
    )

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text("${stat.revisionLevel}", modifier = Modifier.width(30.dp), fontWeight = FontWeight.Bold)
        Box(modifier = Modifier.weight(1f)) {
            Box(modifier = Modifier
                .height(24.dp)
                .fillMaxWidth(animatedWidth)
                .clip(RoundedCornerShape(4.dp))
                .background(barColor))
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text("${stat.cardCount}", fontWeight = FontWeight.Bold)
    }
}

fun getBarColor(level: Int): Brush {
    return when {
        level == 0 -> Brush.horizontalGradient(listOf(Color(0xFF90A4AE), Color(0xFF78909C)))
        level == 1 -> Brush.horizontalGradient(listOf(Color(0xFF64B5F6), Color(0xFF42A5F5)))
        level == 2 -> Brush.horizontalGradient(listOf(Color(0xFF7986CB), Color(0xFF5C6BC0)))
        else -> Brush.horizontalGradient(listOf(Color(0xFF81C784), Color(0xFF66BB6A)))
    }
}