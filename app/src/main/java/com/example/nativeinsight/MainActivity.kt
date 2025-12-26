package com.example.nativeinsight

import androidx.compose.ui.tooling.preview.Preview
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nativeinsight.data.Flashcard
import com.example.nativeinsight.viewmodel.DashboardViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NativeInsightTheme {
                DashboardScreen()
            }
        }
    }
}

@Composable
fun DashboardScreen(viewModel: DashboardViewModel = viewModel()) {
    val flashcards by viewModel.flashcards.collectAsState()

    // File Picker Launcher
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importFile(it) }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFF121212) // Dark Background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Header & Import Button
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Native Insight",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Button(onClick = { launcher.launch(arrayOf("text/plain")) }) {
                        Text("Import Data")
                    }
                }
            }

            // 2. The Glassmorphism Chart
            if (flashcards.isNotEmpty()) {
                item {
                    ContextInsightsCard(flashcards)
                }
            }

            // 3. List of recent cards
            items(flashcards) { card ->
                FlashcardItem(card)
            }
        }
    }
}

@Composable
fun FlashcardItem(card: Flashcard) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp) // Spacing between sections
        ) {
            // 1. CONCEPT (The Header)
            Column {
                Text(
                    text = "Concept",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = card.concept,
                    color = Color(0xFFBB86FC), // Purple Accent
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Divider for structure
            HorizontalDivider(thickness = 1.dp, color = Color.Gray.copy(alpha = 0.3f))

            // 2. FRONT (PT)
            Column {
                Text(text = "Front (PT)", color = Color.Gray, fontSize = 12.sp)
                Text(
                    text = card.frontPt,
                    color = Color.White,
                    fontSize = 16.sp
                )
            }

            // 3. BACK (EN)
            Column {
                Text(text = "Back (EN)", color = Color.Gray, fontSize = 12.sp)
                Text(
                    text = card.backEn,
                    color = Color(0xFF03DAC5), // Teal Accent for English
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // 4. LITERAL (The Logic)
            Column(
                modifier = Modifier
                    .background(Color(0xFF2D2D2D), shape = RoundedCornerShape(8.dp))
                    .padding(8.dp)
                    .fillMaxWidth()
            ) {
                Text(text = "Literal Logic", color = Color.Gray, fontSize = 11.sp)
                Text(
                    text = card.literal,
                    color = Color(0xFFFFB74D), // Orange for "Logic"
                    fontSize = 14.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        }
    }
}

// --- Glassmorphism Component ---
fun Modifier.glassEffect(): Modifier = this
    .background(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFFFFFFFF).copy(alpha = 0.15f),
                Color(0xFFFFFFFF).copy(alpha = 0.05f)
            )
        ),
        shape = RoundedCornerShape(16.dp)
    )
    .clip(RoundedCornerShape(16.dp))

@Composable
fun ContextInsightsCard(flashcards: List<Flashcard>) {
    val categoryCounts = flashcards.groupingBy { it.category }.eachCount()
    val total = flashcards.size.toFloat()

    // Quick colors
    val chartColors = listOf(Color(0xFF00C853), Color(0xFF2979FF), Color(0xFFFF4081), Color(0xFFFFD600))

    Column(modifier = Modifier.fillMaxWidth().glassEffect().padding(24.dp)) {
        Text("Native Context Mix", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(100.dp)) {
                Canvas(modifier = Modifier.size(100.dp)) {
                    var startAngle = -90f
                    val stroke = 25f
                    categoryCounts.entries.forEachIndexed { index, entry ->
                        val sweep = (entry.value / total) * 360f
                        drawArc(
                            color = chartColors.getOrElse(index) { Color.Gray },
                            startAngle = startAngle, sweepAngle = sweep, useCenter = false,
                            style = Stroke(width = stroke)
                        )
                        startAngle += sweep
                    }
                }
                Text("${flashcards.size}", color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(24.dp))
            Column {
                categoryCounts.keys.forEachIndexed { i, cat ->
                    Text("• $cat", color = chartColors.getOrElse(i) { Color.Gray })
                }
            }
        }
    }
}

// Quick Theme wrapper if you don't have one
@Composable
fun NativeInsightTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme(), content = content)
}




@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
fun DashboardPreview() {
    NativeInsightTheme {
        // 1. Create Mock Data (Simulating your "Havaianas" file)
        val mockCards = listOf(
            Flashcard(
                concept = "Business Recovery",
                category = "Havaianas Case",
                idiomDensity = 1.2f,
                frontPt = "A empresa deu a volta por cima.",
                backEn = "The company turned things around.",
                literal = "A empresa virou as coisas ao redor."
            ),
            Flashcard(
                concept = "Social Media Backlash",
                category = "Havaianas Case",
                idiomDensity = 1.1f,
                frontPt = "A campanha deu o que falar.",
                backEn = "The campaign caused a stir.",
                literal = "A campanha causou um mexer."
            ),
            Flashcard(
                concept = "Lead someone on",
                category = "Dating",
                idiomDensity = 1.5f,
                frontPt = "Não queria iludir ela.",
                backEn = "Didn't want to lead her on.",
                literal = "Liderar ela para cima."
            )
        )

        // 2. Render the actual UI with this fake data
        androidx.compose.material3.Scaffold(
            containerColor = Color(0xFF121212)
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
            ) {
                // The Title
                Text(
                    text = "Native Insight",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // The Glassmorphism Chart (Visualizing the Contexts)
                ContextInsightsCard(flashcards = mockCards)

                Spacer(modifier = Modifier.height(16.dp))

                // The List Items
                mockCards.forEach { card ->
                    FlashcardItem(card)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}