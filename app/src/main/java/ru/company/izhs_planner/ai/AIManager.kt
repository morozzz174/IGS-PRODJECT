package ru.company.izhs_planner.ai

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import ru.company.izhs_planner.ai.agents.CalculatorAgent
import ru.company.izhs_planner.ai.agents.CodesCheckerAgent
import ru.company.izhs_planner.ai.agents.PlannerAgent
import ru.company.izhs_planner.domain.model.chat.*
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.net.HttpURLConnection

class AIManager(private val context: Context) {
    private var isModelLoaded = false
    private var isModelDownloaded = false
    private val plannerAgent = PlannerAgent()
    private val calculatorAgent = CalculatorAgent()
    private val codesCheckerAgent = CodesCheckerAgent()

    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress

    private val _downloadState = MutableStateFlow(DownloadState.NOT_STARTED)
    val downloadState: StateFlow<DownloadState> = _downloadState

    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            val modelDir = File(context.filesDir, "models")
            val modelFile = File(modelDir, "qwen2.5-3b-instruct-q4_0.bin")
            
            if (modelFile.exists() && modelFile.length() > 1_000_000_000L) {
                isModelDownloaded = true
                isModelLoaded = true
                _downloadState.value = DownloadState.COMPLETED
                _downloadProgress.value = 1f
            } else {
                isModelDownloaded = false
                _downloadState.value = DownloadState.NOT_STARTED
            }
            true
        } catch (e: Exception) {
            isModelLoaded = false
            isModelDownloaded = false
            _downloadState.value = DownloadState.NOT_STARTED
            true
        }
    }

    fun isReady(): Boolean = isModelLoaded

    fun isDownloaded(): Boolean = isModelDownloaded

    suspend fun downloadModel(): Boolean = withContext(Dispatchers.IO) {
        _downloadState.value = DownloadState.DOWNLOADING
        
        try {
            val modelDir = File(context.filesDir, "models")
            if (!modelDir.exists()) {
                modelDir.mkdirs()
            }
            
            val modelFile = File(modelDir, "qwen2.5-3b-instruct-q4_0.bin")
            
            val modelUrl = MODEL_DOWNLOAD_URL
            
            val url = URL(modelUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 30000
            
            val totalBytes = connection.contentLength.toLong()
            
            connection.inputStream.use { input ->
                FileOutputStream(modelFile).use { output ->
                    val buffer = ByteArray(8192)
                    var downloadedBytes = 0L
                    var bytesRead: Int
                    
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        
                        if (totalBytes > 0) {
                            _downloadProgress.value = downloadedBytes.toFloat() / totalBytes
                        }
                    }
                }
            }
            
            connection.disconnect()
            
            if (modelFile.exists() && modelFile.length() > 1_000_000_000L) {
                isModelDownloaded = true
                isModelLoaded = true
                _downloadState.value = DownloadState.COMPLETED
                _downloadProgress.value = 1f
                true
            } else {
                modelFile.delete()
                _downloadState.value = DownloadState.FAILED
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _downloadState.value = DownloadState.FAILED
            false
        }
    }

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
            
            Не давай рекомендаций, требующих лицензированного проектирования.
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
            Приводи конкретные цифры с обоснованием.
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
            
            |От��ет:
        """.trimMargin()
    }

    fun getMaxContextTokens(): Int = 4096

    fun getModelSizeMB(): Long = 1800

    companion object {
        const val MODEL_DOWNLOAD_URL = "https://huggingface.co/Qwen/Qwen2.5-3B-Instruct-GGUF/resolve/main/qwen2.5-3b-instruct-q4_0.bin"
    }
}

enum class DownloadState {
    NOT_STARTED,
    DOWNLOADING,
    COMPLETED,
    FAILED,
    SKIPPED
}