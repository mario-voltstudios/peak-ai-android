package com.peakai.fitness.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.peakai.fitness.domain.model.CoachMessage
import com.peakai.fitness.ui.theme.AmberDark
import com.peakai.fitness.ui.theme.AmberPrimary
import com.peakai.fitness.ui.theme.BackgroundDark
import com.peakai.fitness.ui.theme.OnSurface
import com.peakai.fitness.ui.theme.OnSurfaceVariant
import com.peakai.fitness.ui.theme.SurfaceDark
import com.peakai.fitness.ui.theme.SurfaceVariantDark
import com.peakai.fitness.ui.viewmodel.CoachViewModel

@Composable
fun CoachScreen(viewModel: CoachViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .imePadding()
    ) {
        // Header
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                "ASK COACH",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = AmberPrimary,
                    letterSpacing = androidx.compose.ui.unit.TextUnit(4f, androidx.compose.ui.unit.TextUnitType.Sp)
                )
            )
            Text(
                "Powered by ${if (uiState.usingGeminiNano) "Gemini Nano" else "Rule Engine"}",
                style = MaterialTheme.typography.labelSmall.copy(color = OnSurfaceVariant)
            )
        }

        // Chat messages
        LazyColumn(
            modifier = Modifier.weight(1f),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
        ) {
            if (uiState.messages.isEmpty()) {
                item {
                    SuggestedQueries(onQueryClick = { viewModel.sendMessage(it) })
                }
            }

            items(uiState.messages) { msg ->
                ChatBubble(msg)
            }

            if (uiState.isLoading) {
                item {
                    Row(Modifier.padding(start = 8.dp)) {
                        TypingIndicator()
                    }
                }
            }
        }

        // Input field
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceDark)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text("Ask your coach...", color = OnSurfaceVariant)
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AmberPrimary,
                    unfocusedBorderColor = SurfaceVariantDark,
                    focusedTextColor = OnSurface,
                    unfocusedTextColor = OnSurface,
                    cursorColor = AmberPrimary,
                    focusedContainerColor = SurfaceVariantDark,
                    unfocusedContainerColor = SurfaceVariantDark
                ),
                shape = RoundedCornerShape(24.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (inputText.isNotBlank()) {
                        viewModel.sendMessage(inputText)
                        inputText = ""
                    }
                }),
                maxLines = 4
            )

            IconButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        viewModel.sendMessage(inputText)
                        inputText = ""
                    }
                },
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(AmberPrimary)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = Color.Black
                )
            }
        }
    }
}

@Composable
private fun ChatBubble(message: CoachMessage) {
    val isUser = message.role == CoachMessage.Role.USER

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .background(if (isUser) AmberDark else SurfaceDark)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                message.content,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = if (isUser) Color.Black else OnSurface
                )
            )
        }
    }
}

@Composable
private fun TypingIndicator() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceDark)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            color = AmberPrimary,
            strokeWidth = 2.dp
        )
    }
}

@Composable
private fun SuggestedQueries(onQueryClick: (String) -> Unit) {
    val suggestions = listOf(
        "What's my readiness today?",
        "Should I train today?",
        "How's my HRV looking?",
        "How much caffeine do I have left?",
        "What does my sleep say?"
    )

    Column {
        Text(
            "Suggested",
            style = MaterialTheme.typography.labelSmall.copy(color = OnSurfaceVariant),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        suggestions.forEach { query ->
            androidx.compose.material3.OutlinedButton(
                onClick = { onQueryClick(query) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                    contentColor = AmberPrimary
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceVariantDark)
            ) {
                Text(query, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
