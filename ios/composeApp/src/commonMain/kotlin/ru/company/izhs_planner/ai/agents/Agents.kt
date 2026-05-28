package ru.company.izhs_planner.ai.agents

import ru.company.izhs_planner.ai.LLMInference

class PlannerAgent(private val llm: LLMInference) {
    fun generate(context: String): String {
        return llm.generate(context, maxTokens = 2048)
    }
}

class CalculatorAgent(private val llm: LLMInference) {
    fun generate(context: String): String {
        return llm.generate(context, maxTokens = 2048)
    }
}

class CodesCheckerAgent(private val llm: LLMInference) {
    fun generate(context: String): String {
        return llm.generate(context, maxTokens = 2048)
    }
}
