package ru.company.izhs_planner.ai.agents

import ru.company.izhs_planner.ai.LLMInference

class PlannerAgent {
    fun generate(context: String): String {
        return LLMInference.generate(context, maxTokens = 2048)
    }
}

class CalculatorAgent {
    fun generate(context: String): String {
        return LLMInference.generate(context, maxTokens = 2048)
    }
}

class CodesCheckerAgent {
    fun generate(context: String): String {
        return LLMInference.generate(context, maxTokens = 2048)
    }
}
