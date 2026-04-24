package ru.company.izhs_planner.ai

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.company.izhs_planner.ai.agents.CalculatorAgent
import ru.company.izhs_planner.ai.agents.CodesCheckerAgent
import ru.company.izhs_planner.ai.agents.PlannerAgent
import ru.company.izhs_planner.domain.model.chat.*
import java.io.BufferedReader
import java.io.InputStreamReader

class AIManager(private val context: Context) {
    private var isModelLoaded = false
    private val plannerAgent = PlannerAgent()
    private val calculatorAgent = CalculatorAgent()
    private val codesCheckerAgent = CodesCheckerAgent()

    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            val modelFile = context.assets.open("models/qwen2.5-3b-instruct-q4_0.bin")
            modelFile.close()
            isModelLoaded = true
            true
        } catch (e: Exception) {
            isModelLoaded = false
            true
        }
    }

    fun isReady(): Boolean = isModelLoaded

    fun getAgents(): List<AgentType> = AgentType.entries

    suspend fun generateResponse(
        userMessage: String,
        agentType: AgentType,
        chatHistory: List<ChatMessage>,
        projectInfo: String
    ): AIResponse = withContext(Dispatchers.Default) {
        try {
            val systemPrompt = getSystemPrompt(agentType)
            val context = buildContext(systemPrompt, userMessage, chatHistory, projectInfo)

            val response = when (agentType) {
                AgentType.PLANNER -> plannerAgent.generate(context)
                AgentType.CALCULATOR -> calculatorAgent.generate(context)
                AgentType.CODES_CHECKER -> codesCheckerAgent.generate(context)
            }

            AIResponse(content = response, isComplete = true)
        } catch (e: Exception) {
            AIResponse(
                content = "Извините, произошла ошибка при обработке запроса. Попробуйте ещё раз.",
                isComplete = false,
                errorMessage = e.message
            )
        }
    }

    private fun getSystemPrompt(agentType: AgentType): String = when (agentType) {
        AgentType.PLANNER -> """
            Ты — инженер-проектировщик ИЖС (Индивидуальное жилищное строительство), специалист по планировке.
            
            Твоя задача — помогать с планировкой помещений, зонированием, инсоляцией (естественным освещением).
            Отвечай строго в рамках СП 11-101-95 (Порядок разработки, согласования и утверждения проектной документации), 
            СП 55.13330 (Жилые здания), СП 42.13330 (Планировка и застройка городов).
            
            Основные принципы:
            - Кухня должна быть не менее 5 м², предпочтительно рядом с гостиной
            - Минимальная площадь спальни — 10 м²
            - Инсоляция жилых комнат — не менее 2 часов в день
            - Санузлы не должны примыкать к кухне и жилым комнатам (по санправилам)
            - Вход в санузел — из коридора или прихожей
            
            Не давай рекомендаций, требующих лицензирова��ного проектирования.
            Предупреждай о необходимости проверки у архитектора.
            Отвечай кратко и по существу на русском языке.
        """.trimIndent()

        AgentType.CALCULATOR -> """
            Ты — инженер-сметчик ИЖС, специалист по расчёту объёмов материалов.
            
            Твоя задача — рассчитывать примерные объёмы материалов и ориентировочную смету.
            Используй Сметные нормативы и среднерыночные цены 2025-2026 года.
            
            Основные данные для расчёта:
            - Фундамент: ленточный — бетон М200, объём = периметр × ширина × глубина
            - Стены: объём = площадь стен × толщина
            - Кровля: площадь скатов (для двускатной: ширина × длина × 1.4)
            - Перекрытия: площадь этажа × 0.3 (балки) + бетон М200
            
            Примерная стоимость материалов (2025-2026):
            - Бетон М200: ~4500 ₽/м³
            - Газобетон D400: ~3500 ₽/м³
            - Кирпич: ~15 ₽/шт
            - Металлочерепица: ~550 ₽/м²
            - Доска обрезная: ~12000 ₽/м³
            - Утеплитель: ~1500 ₽/м³
            
            Всегда указывай, что смета ориентировочная и требует уточнения.
            Приводи具体的 цифры с обоснованием.
            Отвечай на русском языке.
        """.trimIndent()

        AgentType.CODES_CHECKER -> """
            Ты — инженер нормоконтроля ИЖС, специалист по проверке соответствия нормам.
            
            Твоя задача — проверять отступы, этажность, уклон участка на соответствие СНиП.
            Отвечай в рамках СП 11-101-95, СП 55.13330, СП 42.13330, СП 31-102-99.
            
            Основные нормы:
            - Отступ от границы участка — не менее 3 метров до дома
            - Отступ от соседнего дома — не менее 6 метров
            - Максимальная этажность ИЖС — 3 этажа
            - Высота этажа — не менее 2.5 м, рекомендуется 2.7-3.0 м
            - Площадь застройки — не более 50% от площади участка
            - Инсоляция — минимум 2 часа в день для жилых комнат
            - Уклон участка — для фундамента допустим до 10%, иначе — террасирование
            
            Проверяй по пунктам и давай заключение:
            - ✅ Соответствует
            - ⚠️ Требует проверки
            - ❌ Не соответствует
            
            Если требуется лицензированное проектирование — указывай это явно.
            Предупреждай о необходимости проверки у архитектора.
            Отвечай структурированно на русском языке.
        """.trimIndent()
    }

    private fun buildContext(
        systemPrompt: String,
        userMessage: String,
        chatHistory: List<ChatMessage>,
        projectInfo: String
    ): String {
        val historyText = chatHistory.takeLast(6).joinToString("\n") { msg ->
            "${if (msg.role == ChatRole.USER) "Пользователь" else "Ассистент"}: ${msg.content}"
        }

        return """
            |Системный промпт: $systemPrompt
            
            |Информация о проекте:
            |$projectInfo
            
            |История диалога:
            |$historyText
            
            |Текущий вопрос: $userMessage
            
            |Ответ:
        """.trimMargin()
    }

    fun getMaxContextTokens(): Int = 4096
}