package ru.company.izhs_planner.domain.model.chat

import java.util.UUID

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: ChatRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class ChatRole {
    USER,
    ASSISTANT,
    SYSTEM
}

data class ChatSession(
    val id: String = UUID.randomUUID().toString(),
    val agentType: AgentType,
    val messages: List<ChatMessage> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class AgentType {
    PLANNER,
    CALCULATOR,
    CODES_CHECKER
}

data class AIResponse(
    val content: String,
    val isComplete: Boolean = true,
    val errorMessage: String? = null
)

data class DailyLimitInfo(
    val used: Int,
    val limit: Int,
    val isPremium: Boolean = false
) {
    val remaining: Int get() = limit - used
    val isReached: Boolean get() = used >= limit
}