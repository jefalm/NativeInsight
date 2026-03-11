package com.example.nativeinsight

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.KeyEvent
import android.widget.Toast
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.example.nativeinsight.data.Flashcard
import com.example.nativeinsight.logic.SpeechComparator
import com.example.nativeinsight.ui.AnalyticsScreen
import com.example.nativeinsight.ui.EditScreen
import com.example.nativeinsight.ui.FloatingIcon
import com.example.nativeinsight.ui.getCategoryStyle
import com.example.nativeinsight.ui.theme.NativeInsightTheme
import com.example.nativeinsight.viewmodel.DashboardViewModel
import java.util.Locale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import com.example.nativeinsight.logic.SmartChunker
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.runtime.mutableStateMapOf

class MainActivity : ComponentActivity(),TextToSpeech.OnInitListener {
    private lateinit var tts: TextToSpeech
    private lateinit var viewModel: DashboardViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        setContent {
            NativeInsightTheme {
                viewModel = viewModel()
                MainScreen(onSpeak = { text ->
                    tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
                })
            }
        }
    }
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.US
        }}
        override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
            when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_DOWN -> {

                    // Read the English (Back) side of the active card
                    val card = viewModel.activeDiscoveryCard.value
                    Log.d(TAG, card.toString())
                    card?.let {
                        tts.speak(it.backEn, TextToSpeech.QUEUE_FLUSH, null, null)
                    }
                    return true // Consume the event so the volume doesn't actually change
                }
                KeyEvent.KEYCODE_MEDIA_PLAY,
                KeyEvent.KEYCODE_MEDIA_PAUSE,
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                KeyEvent.KEYCODE_HEADSETHOOK -> {
                    viewModel.triggerMicFromHardware()
                    return true
                }
            }
            return super.onKeyDown(keyCode, event)
        }

        override fun onDestroy() {
            tts.stop()
            tts.shutdown()
            super.onDestroy()
        }
    }


// This Composable manages the Bottom Navigation and switching screens
@Composable
fun MainScreen(onSpeak: (String) -> Unit) {
    // Create the ViewModel ONCE here to share it between screens
    val viewModel: DashboardViewModel = viewModel()

    val flashcards = viewModel.flashcardPager.collectAsLazyPagingItems()

    // State to track which tab is active ("dashboard" or "analytics")
    var currentScreen by remember { mutableStateOf("dashboard") }

    LaunchedEffect(flashcards.loadState.refresh) {
        if (flashcards.loadState.refresh is LoadState.NotLoading) {
            if (flashcards.itemCount > 0) {
                val nextCard = flashcards[0]
                viewModel.updateActiveCard(nextCard)
            }
        }
    }



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
                            val activeCard = flashcards[0]
                            viewModel.setCardForEdit(activeCard)
                        }
                        currentScreen = "editor"
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.ViewCarousel, contentDescription = "Clusters") },
                    label = { Text("Clusters") },
                    selected = currentScreen == "clusters",
                    onClick = { currentScreen = "clusters" }
                )
            }
        }
    ) { innerPadding ->
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
                "clusters" -> ClusterScreen(viewModel = viewModel, onSpeak = onSpeak)
            }
        }
    }
}

@Composable
fun DashboardScreen(viewModel: DashboardViewModel = viewModel()) {
    val flashcards = viewModel.flashcardPager.collectAsLazyPagingItems()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val globalIsFlipped by viewModel.isFlipped.collectAsState()

    // Context for Toasts and Intent
    val context = LocalContext.current

    // State for speech matching
    var smartMaskedText by remember { mutableStateOf<String?>(null) }

    val focusManager = LocalFocusManager.current
    var isSearchFocused by remember { mutableStateOf(false) } // Track focus state

    // Speech Launcher
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data = result.data
            val spokenText = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.get(0) ?: ""

            // Validate against the current card (index 0 in discovery mode)
            if (flashcards.itemCount > 0) {
                val currentCard = flashcards[0]
                if (currentCard != null) {
                    val comparison = SpeechComparator.evaluate(currentCard.backEn, spokenText)

                    // Calculate percentage for display
                    val accuracyPct = (comparison.score * 100).toInt()

                    if (comparison.isSuccess) {
                        // --- MATCH SUCCESS LOGIC ---
                        if (comparison.score >= 0.99f) {
                            // 100% Accuracy: Show original text (Capitalized/Punctuated)
                            smartMaskedText = currentCard.backEn
                            Toast.makeText(context, "Perfect! 100% 💯", Toast.LENGTH_SHORT).show()
                        } else {
                            // 50% - 99%: Show masked text (with blanks)
                            smartMaskedText = comparison.maskedText
                            Toast.makeText(context, "Good match! ($accuracyPct%) 🎯", Toast.LENGTH_SHORT).show()
                        }

                        // Auto-flip to Back if currently on Front
                        if (!viewModel.isFlipped.value) {
                            viewModel.toggleFlip()
                        }

                    } else {
                        // < 50%: Failure
                        Toast.makeText(context, "Try again ($accuracyPct%)", Toast.LENGTH_SHORT).show()
                        // [FIX] Do NOT clear the mask on failure.
                        // We leave smartMaskedText as it is, so if the user had a mask, they keep it.
                        // If they were on the front, they stay on the front.
                    }
                }
            }
        }

    }
    // Define this new launcher specifically for search
    val searchSpeechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data = result.data
            val spokenText = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.get(0) ?: ""
            // Directly populate the search box
            viewModel.onSearchQueryChanged(spokenText)
        }
    }

    // Listen for hardware mic triggers as events
    LaunchedEffect(Unit) {
        viewModel.micTriggerSignal.collect {
            if (flashcards.itemCount > 0) {
                val currentCard = flashcards[0]
                if (currentCard != null) {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                    }
                    speechLauncher.launch(intent)
                }
            }
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri -> uri?.let { viewModel.importFile(it) } }
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        // Floating Mic Button (Only in Discovery Mode)
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            if (searchQuery.isBlank() && flashcards.itemCount > 0) {
                FloatingActionButton(
                    onClick = {
                        // Launch speech recognition
                        val currentCard = flashcards[0]
                        if (currentCard != null) {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                            }
                            speechLauncher.launch(intent)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(bottom = 8.dp) // Slight padding to separate from Nav Bar area
                ) {
                    Icon(Icons.Default.Mic, contentDescription = "Speak")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier
            .padding(padding)
            .padding(16.dp)) {

            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
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
                    IconButton(onClick = {
                        val currentCard = if (flashcards.itemCount > 0) flashcards[0] else null
                        // Reset mask on refresh
                        smartMaskedText = null
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
                onValueChange = {
                    viewModel.onSearchQueryChanged(it)
                    // Reset mask if user starts searching
                    smartMaskedText = null
                },
                label = { Text("Search logic or phrase") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 55.dp)
                    .onFocusChanged { isSearchFocused = it.isFocused },
                trailingIcon = {
                    // Logic: Show button only if focused
                    if (isSearchFocused) {
                        if (searchQuery.isEmpty()) {
                            // MIC: Only shows when focused and empty
                            IconButton(onClick = {
                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                                }
                                searchSpeechLauncher.launch(intent) // Uses the new specific launcher
                            }) {
                                Icon(Icons.Default.Mic, contentDescription = "Voice Search")
                            }
                        } else {
                            // Focus + Text = Clear Button
                            IconButton(onClick = {
                                viewModel.onSearchQueryChanged("")
                                focusManager.clearFocus()
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear Search")
                            }
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                singleLine = true
            )

            // Content Area
            if (searchQuery.isBlank()) {
                // --- DISCOVERY MODE (Single Centered Card) ---
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    if (flashcards.itemCount > 0) {
                        val card = flashcards[0]
                        if (card != null) {
                            FlashcardFlipItem(
                                card = card,
                                isFlipped = globalIsFlipped,
                                onToggleFlip = { viewModel.toggleFlip() },
                                smartMaskedText = smartMaskedText, // Pass the mask state
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
                                smartMaskedText = null, // No masking in list view
                                modifier = Modifier.fillMaxWidth().wrapContentHeight()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FlashcardFlipItem(
    card: Flashcard,
    isFlipped: Boolean,
    onToggleFlip: () -> Unit,
    smartMaskedText: String? = null,
    modifier: Modifier = Modifier
) {
    val style = getCategoryStyle(card.category)

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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        // Enforce a minimum height (300.dp) so the AutoResizingText has space to work with
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (rotation <= 90f) {
                // --- FRONT SIDE (Portuguese) ---
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = style.label.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = style.color
                        )
                        FloatingIcon(
                            icon = style.icon,
                            tint = style.color,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Main Content (Auto Resizing)
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        AutoResizingText(
                            text = SmartChunker.colorize(card.frontPt, isEnglish = false),
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Footer
                    Text(
                        text = SmartChunker.colorize(card.literalLogic, isEnglish = false),
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // --- BACK SIDE (English) ---
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { rotationY = 180f },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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

                    Text(
                        text = card.concept.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    // Main Content (Auto Resizing)
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        AutoResizingText(
                            text = SmartChunker.colorize(smartMaskedText ?: card.backEn, isEnglish = true),
                            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                            color = if (smartMaskedText != null) style.color.copy(alpha = 0.8f) else style.color,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Footer Area (Hint + Literal Logic)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (smartMaskedText != null) {
                            Text(
                                text = "(Tap card to reset)",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }

                        // Literal Logic on Back Side
                        Text(
                            text = SmartChunker.colorize(card.literalLogic, isEnglish = false),
                            style = MaterialTheme.typography.bodyMedium,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.secondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

// Helper for auto-sizing text
@Composable
fun AutoResizingText(
    text: AnnotatedString,
    style: androidx.compose.ui.text.TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    textAlign: TextAlign = TextAlign.Center
) {
    // Remember the current size so we don't reset unnecessarily,
    // but reset if the text or base style changes.
    var resizedTextStyle by remember(text, style) { mutableStateOf(style) }
    var readyToDraw by remember(text, style) { mutableStateOf(false) }

    Text(
        text = text,
        color = color,
        textAlign = textAlign,
        modifier = modifier.drawWithContent {
            if (readyToDraw) drawContent()
        },
        style = resizedTextStyle,
        softWrap = true,
        onTextLayout = { result ->
            // If the text overflows the container height or width, shrink it
            if (result.didOverflowHeight || result.didOverflowWidth) {
                // Reduce font size by 5% and retry
                resizedTextStyle = resizedTextStyle.copy(
                    fontSize = resizedTextStyle.fontSize * 0.95
                )
            } else {
                // Fits! Show it.
                readyToDraw = true
            }
        }
    )
}

@Composable
fun ClusterScreen(
    viewModel: DashboardViewModel,
    onSpeak: (String) -> Unit // <--- Add this parameter to pass to TTS
) {
    val clusterCards by viewModel.activeCluster.collectAsState()
    val targetCard by viewModel.targetClusterCard.collectAsState()
    val context = LocalContext.current

    // Tracks the "revealed text" or "fill-in-the-blanks" state for each card by ID
    val revealedTexts = remember { mutableStateMapOf<Int, String>() }

    val clusterSpeechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.get(0) ?: ""

            targetCard?.let { card ->
                val comparison = SpeechComparator.evaluate(card.backEn, spokenText)
                val accuracyPct = (comparison.score * 100).toInt()

                // Generate a full blank string (e.g., "_____ _____ _____") if the score is < 50%
                val fallbackMask = card.backEn.split(" ").joinToString(" ") { "_____" }

                if (comparison.isSuccess) {
                    if (comparison.score >= 0.99f) {
                        revealedTexts[card.id] = card.backEn // 100% -> Show full text
                        Toast.makeText(context, "Perfect! 100% 💯", Toast.LENGTH_SHORT).show()
                    } else {
                        // Success (>= 50%), maskedText exists, but ?: keeps the compiler happy
                        revealedTexts[card.id] = comparison.maskedText ?: fallbackMask
                        Toast.makeText(context, "Good match! ($accuracyPct%) 🎯", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // Failed (< 50%). maskedText is null, so we use the fallback blanks
                    revealedTexts[card.id] = comparison.maskedText ?: fallbackMask
                    Toast.makeText(context, "Try again ($accuracyPct%)", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (clusterCards.isEmpty()) {
            viewModel.loadSemanticCluster()
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Semantic Cluster Practice", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(clusterCards, key = { it.id }) { card ->
                ClusterCardRow(
                    card = card,
                    revealedText = revealedTexts[card.id], // Pass the state down
                    onMicClicked = {
                        viewModel.setTargetClusterCard(card)
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                        }
                        clusterSpeechLauncher.launch(intent)
                    },
                    onSpeakClicked = {
                        onSpeak(card.backEn) // Trigger TTS for this specific card
                    }
                )
            }
        }

        Button(
            onClick = {
                revealedTexts.clear() // Hide everything again for the next batch
                viewModel.submitClusterAndRefresh()
            },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) {
            Text("Shuffle Next Cluster (Mark 4x Reviewed)")
        }
    }
}

// Reusable UI for the 4 individual items
@Composable
fun ClusterCardRow(
    card: Flashcard,
    revealedText: String?,
    onMicClicked: () -> Unit,
    onSpeakClicked: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Front (Portuguese) always visible
                Text(text = card.frontPt, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(8.dp))

                // Back (English) Logic
                val isHidden = revealedText == null
                val displayText = revealedText ?: "[ Tap Mic to Guess ]"
                val textColor = when {
                    revealedText == card.backEn -> MaterialTheme.colorScheme.primary // Perfect
                    !isHidden -> MaterialTheme.colorScheme.secondary // Masked / Blanks
                    else -> Color.Gray // Hidden state
                }

                Text(
                    text = displayText,
                    color = textColor,
                    fontStyle = if (isHidden) FontStyle.Italic else FontStyle.Normal,
                    fontSize = 16.sp
                )
            }

            // Vertical Stack for Action Buttons
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onMicClicked) {
                    Icon(Icons.Default.Mic, contentDescription = "Practice Phrase", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onSpeakClicked) {
                    Icon(Icons.Default.VolumeUp, contentDescription = "Listen to Phrase", tint = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
}
