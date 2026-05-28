package ru.company.izhs_planner.domain.model.chat

data class ChatMessage(
    val id: String = "",
    val role: ChatRole,
    val content: String,
    val timestamp: Long = 0L
)

enum class ChatRole {
    USER, ASSISTANT, SYSTEM
}

data class ChatSession(
    val id: String = "",
    val agentType: AgentType,
    val messages: List<ChatMessage> = emptyList(),
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

enum class AgentType {
    PLANNER, CALCULATOR, CODES_CHECKER
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
