package com.example.nativeinsight.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.nativeinsight.viewmodel.DashboardViewModel

@Composable
fun EditScreen(viewModel: DashboardViewModel, onNavigateBack: () -> Unit) {
    val card by viewModel.cardInEditMode.collectAsState()
    val scrollState = rememberScrollState()

    card?.let { currentCard ->
        val style = getCategoryStyle(currentCard.category)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Edit Flashcard", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

            OutlinedTextField(
                value = currentCard.category,
                onValueChange = { viewModel.updateCardField(currentCard.copy(category = it)) },
                label = { Text("Category") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = currentCard.concept,
                onValueChange = { viewModel.updateCardField(currentCard.copy(concept = it)) },
                label = { Text("Concept") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = currentCard.frontPt,
                onValueChange = { viewModel.updateCardField(currentCard.copy(frontPt = it)) },
                label = { Text("Front (PT)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = currentCard.backEn,
                onValueChange = { viewModel.updateCardField(currentCard.copy(backEn = it)) },
                label = { Text("Back (EN)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = currentCard.literalLogic,
                onValueChange = { viewModel.updateCardField(currentCard.copy(literalLogic = it)) },
                label = { Text("Literal Logic") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { viewModel.saveEditedCard(onComplete = onNavigateBack) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = style.color)
            ) {
                Text("SAVE CHANGES", color = MaterialTheme.colorScheme.surface, fontWeight = FontWeight.Bold)
            }
        }
    } ?: Box(Modifier.fillMaxSize()) { Text("No card selected", Modifier.padding(16.dp)) }
}