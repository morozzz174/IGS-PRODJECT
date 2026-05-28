package ru.company.izhs_planner.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_sessions")
data class ChatSessionEntity(
    @PrimaryKey
    val id: String,
    val agentType: String,
    val messagesJson: String,
    val createdAt: Long,
    val updatedAt: Long
)