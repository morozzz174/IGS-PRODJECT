package ru.company.izhs_planner.ai

expect class LLMInference {
    fun init(modelPath: String): Boolean
    fun generate(prompt: String, maxTokens: Int): String
    fun reset()
    fun release()
    fun isInitialized(): Boolean
}
