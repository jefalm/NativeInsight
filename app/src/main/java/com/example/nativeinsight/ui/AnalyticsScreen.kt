package com.example.nativeinsight.ui

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.nativeinsight.MainActivity
import com.example.nativeinsight.data.RevisionStat
import com.example.nativeinsight.viewmodel.DashboardViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AnalyticsScreen(viewModel: DashboardViewModel) {
    val stats by viewModel.analyticsData.collectAsState()
    val totalCards by viewModel.totalCardsCount.collectAsState()
    val context = LocalContext.current

    val maxCount = remember(stats) { stats.maxOfOrNull { it.cardCount } ?: 1 }

    // --- Backup Launcher ---
    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/x-sqlite3")
    ) { uri ->
        uri?.let { viewModel.backupDatabase(it) }
    }

    // --- Restore Launcher ---
    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            viewModel.restoreDatabase(
                sourceUri = it,
                onSuccess = {
                    Toast.makeText(context, "Database Restored! Restarting...", Toast.LENGTH_LONG).show()
                    val intent = Intent(context, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    context.startActivity(intent)
                    Runtime.getRuntime().exit(0)
                },
                onError = {
                    Toast.makeText(context, "Failed to restore database", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Knowledge Distribution", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("$totalCards total cards tracked", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(stats) { stat ->
                RevisionBarRow(stat, maxCount)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Backup Button ---
        Button(
            onClick = {
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val fileName = "native_$timeStamp.db"
                backupLauncher.launch(fileName)
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1E1E1E), // Dark Gray/Black
                contentColor = Color.White          // White Text
            )
        ) {
            Text("Backup Database to Drive")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- Restore Button ---
        Button(
            onClick = {
                restoreLauncher.launch(arrayOf("*/*"))
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1E1E1E), // Matches the Backup Button
                contentColor = Color.White
            )
        ) {
            Text("Load Backup Database")
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
    return when (level) {
        0 -> Brush.horizontalGradient(listOf(Color(0xFF90A4AE), Color(0xFF78909C)))
        1 -> Brush.horizontalGradient(listOf(Color(0xFF64B5F6), Color(0xFF42A5F5)))
        2 -> Brush.horizontalGradient(listOf(Color(0xFF7986CB), Color(0xFF5C6BC0)))
        3 -> Brush.horizontalGradient(listOf(Color(0xFF9575CD), Color(0xFF7E57C2)))
        4 -> Brush.horizontalGradient(listOf(Color(0xFFBA68C8), Color(0xFFAB47BC)))
        5 -> Brush.horizontalGradient(listOf(Color(0xFFF06292), Color(0xFFEC407A)))
        6 -> Brush.horizontalGradient(listOf(Color(0xFFFF8A65), Color(0xFFFF7043)))
        7 -> Brush.horizontalGradient(listOf(Color(0xFFFFB74D), Color(0xFFFFA726)))
        8 -> Brush.horizontalGradient(listOf(Color(0xFFFFD54F), Color(0xFFFFCA28)))
        9 -> Brush.horizontalGradient(listOf(Color(0xFFAED581), Color(0xFF9CCC65)))
        10 -> Brush.horizontalGradient(listOf(Color(0xFF81C784), Color(0xFF66BB6A)))
        else -> Brush.horizontalGradient(listOf(Color(0xFF4DB6AC), Color(0xFF26A69A)))
    }
}