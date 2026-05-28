package ru.company.izhs_planner

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.company.izhs_planner.ai.LLMInference
import ru.company.izhs_planner.ai.AIManager
import ru.company.izhs_planner.domain.model.chat.AgentType
import ru.company.izhs_planner.domain.model.chat.ChatMessage
import ru.company.izhs_planner.domain.model.chat.ChatRole

@Composable
fun App() {
    MaterialTheme(
        colorScheme = darkColorScheme()
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            MainScreen()
        }
    }
}

@Composable
private fun MainScreen() {
    var selectedAgent by remember { mutableStateOf(AgentType.PLANNER) }
    var chatMessages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var inputText by remember { mutableStateOf("") }
    var outputText by remember { mutableStateOf("") }
    var modelStatus by remember { mutableStateOf("Модель не загружена") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "ИЖС-Проектировщик",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Платформа: ${getPlatformName()}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = modelStatus,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Agent selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AgentType.entries.forEach { agent ->
                FilterChip(
                    selected = selectedAgent == agent,
                    onClick = { selectedAgent = agent },
                    label = {
                        Text(
                            when (agent) {
                                AgentType.PLANNER -> "Планировщик"
                                AgentType.CALCULATOR -> "Калькулятор"
                                AgentType.CODES_CHECKER -> "Нормоконтроль"
                            }
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Chat area
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                if (chatMessages.isEmpty()) {
                    Text(
                        text = "Выберите агента и задайте вопрос",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = outputText,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Input area
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Введите вопрос...") },
                maxLines = 3
            )

            Button(
                onClick = {
                    if (inputText.isNotBlank()) {
                        val userMsg = ChatMessage(
                            role = ChatRole.USER,
                            content = inputText
                        )
                        chatMessages = chatMessages + userMsg
                        outputText = "Обработка..."
                        inputText = ""
                    }
                },
                enabled = inputText.isNotBlank()
            ) {
                Text("→")
            }
        }
    }
}
