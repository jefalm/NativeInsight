package com.example.nativeinsight

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home // [NEW] Icon
import androidx.compose.material.icons.filled.Info // [NEW] Icon for Stats
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar // [NEW]
import androidx.compose.material3.NavigationBarItem // [NEW]
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface // [NEW]
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.example.nativeinsight.data.Flashcard
import com.example.nativeinsight.ui.AnalyticsScreen // [NEW] Import your new screen
import com.example.nativeinsight.ui.theme.NativeInsightTheme
import com.example.nativeinsight.viewmodel.DashboardViewModel
import com.example.nativeinsight.ui.FloatingIcon
import com.example.nativeinsight.ui.getCategoryStyle
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.Edit
import com.example.nativeinsight.ui.EditScreen
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NativeInsightTheme {
                // [UPDATE] Call MainScreen instead of DashboardScreen directly
                MainScreen()
            }
        }
    }
}

// [NEW] This Composable manages the Bottom Navigation and switching screens
@Composable
fun MainScreen() {
    // Create the ViewModel ONCE here to share it between screens
    val viewModel: DashboardViewModel = viewModel()

    val flashcards = viewModel.flashcardPager.collectAsLazyPagingItems() // Collect here to share stat

    // State to track which tab is active ("dashboard" or "analytics")
    var currentScreen by remember { mutableStateOf("dashboard") }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Practice") },
                    label = { Text("Practice") },
                    selected = currentScreen == "dashboard",
                    onClick = { currentScreen = "dashboard" }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Info, contentDescription = "Stats") },
                    label = { Text("Stats") },
                    selected = currentScreen == "analytics",
                    onClick = { currentScreen = "analytics" }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Edit, contentDescription = "Editor") },
                    label = { Text("Editor") },
                    selected = currentScreen == "editor",
                    onClick = {
                        // Logic: Identify the card based on the current UI state
                        if (flashcards.itemCount > 0) {
                            // In Discovery Mode (query is blank), index 0 is always the active card.
                            // In Search Mode, this defaults to the top-most visible card in the list.
                            val activeCard = flashcards[0]
                            viewModel.setCardForEdit(activeCard)
                        }
                        currentScreen = "editor"
                    }
                )
            }
        }
    ) { innerPadding ->
        // Surface handles the background and the padding from the bottom bar
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            when (currentScreen) {
                "dashboard" -> DashboardScreen(viewModel = viewModel)
                "analytics" -> AnalyticsScreen(viewModel = viewModel)
                "editor" -> EditScreen(
                    viewModel = viewModel,
                    onNavigateBack = { currentScreen = "dashboard" }
                )
            }
        }
    }
}

// [EXISTING] Your DashboardScreen logic remains the same
@Composable
fun DashboardScreen(viewModel: DashboardViewModel = viewModel()) {
    val flashcards = viewModel.flashcardPager.collectAsLazyPagingItems()
    val searchQuery by viewModel.searchQuery.collectAsState()

    val globalIsFlipped by viewModel.isFlipped.collectAsState()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri -> uri?.let { viewModel.importFile(it) } }
    )

    var currentScreen by remember { mutableStateOf("dashboard") }

    // Note: We don't need another Scaffold here strictly, but it's fine to keep
    // for the internal padding logic of this specific screen.
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {

            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Native Insight",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Discovery Refresh Button
                    IconButton(onClick = {
                        // [CRITICAL] Pass the current card if available to increment stats
                        val currentCard = if (flashcards.itemCount > 0) flashcards[0] else null
                        viewModel.refreshDiscovery(currentCard)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Discovery",
                            tint = MaterialTheme.colorScheme.primary

                        )
                    }
                    Spacer(modifier = Modifier.padding(4.dp))
                    Button(
                        onClick = { launcher.launch(arrayOf("text/plain")) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Import", color = MaterialTheme.colorScheme.onSecondary)
                    }
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                label = { Text("Search logic or phrase (Clear for Discovery)") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                singleLine = true
            )

            // Dynamic Layout Strategy
            if (searchQuery.isBlank()) {
                // --- DISCOVERY MODE (Single Centered Card) ---
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (flashcards.itemCount > 0) {
                        val card = flashcards[0]
                        if (card != null) {
                            FlashcardFlipItem(
                                card = card,
                                isFlipped = globalIsFlipped,
                                onToggleFlip = { viewModel.toggleFlip() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight()
                            )
                        }
                    } else {
                        Text("No flashcards found. Import some!")
                    }
                }
            } else {
                // --- SEARCH MODE (Scrollable List) ---
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        count = flashcards.itemCount,
                        key = flashcards.itemKey { it.concept + it.frontPt },
                        contentType = flashcards.itemContentType { "Flashcard" }
                    ) { index ->
                        val card = flashcards[index]
                        if (card != null) {
                            var localFlip by remember(card) { mutableStateOf(false) }
                            FlashcardFlipItem(
                                card = card,
                                isFlipped = localFlip,
                                onToggleFlip = { localFlip = !localFlip },
                                modifier = Modifier.fillMaxWidth().wrapContentHeight()
                            )
                        }
                    }
                }
            }
        }
    }
}

// ... (Keep MainScreen and DashboardScreen exactly as they are)


// [UPDATE] Replace the entire FlashcardFlipItem function with this:
@Composable
fun FlashcardFlipItem(
    card: Flashcard,
    isFlipped: Boolean,
    onToggleFlip: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 1. Get the visual style based on the category
    val style = getCategoryStyle(card.category)

    // Animation State
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "FlipAnimation"
    )

    Card(
        modifier = modifier
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            }
            .clickable { onToggleFlip() },
        // Use a dark container color to make the neon colors pop (like the game)
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (rotation <= 90f) {
                // --- FRONT SIDE (Portuguese) ---
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header: Category Label + Floating Icon
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = style.label.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = style.color // Neon color
                        )
                        FloatingIcon(
                            icon = style.icon,
                            tint = style.color,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = card.frontPt,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "\"${card.literalLogic}\"",
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            } else {
                // --- BACK SIDE (English) ---
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer { rotationY = 180f }, // Correct text mirroring
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header: Category Label + Floating Icon (Mirrored for back)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween, // Swap alignment if desired, or keep uniform
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Note: Because the whole Column is flipped 180, this Row layout
                        // physically appears correctly left-to-right to the user.
                        Text(
                            text = style.label.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = style.color.copy(alpha = 0.7f)
                        )
                        FloatingIcon(
                            icon = style.icon,
                            tint = style.color.copy(alpha = 0.7f),
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = card.concept.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = card.backEn,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = style.color, // Use the category color for the answer!
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}