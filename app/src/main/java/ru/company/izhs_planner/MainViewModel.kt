package ru.company.izhs_planner

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.company.izhs_planner.data.local.datastore.PreferencesManager
import ru.company.izhs_planner.data.local.datastore.ThemeMode
import ru.company.izhs_planner.data.repository.ChatRepository
import ru.company.izhs_planner.data.repository.ProjectRepository
import ru.company.izhs_planner.domain.model.chat.*
import ru.company.izhs_planner.domain.model.*
import ru.company.izhs_planner.mobile_ads.MobileAdsManager
import ru.company.izhs_planner.premium.PremiumManagerImpl
import ru.company.izhs_planner.ai.AIManager
import ru.company.izhs_planner.ai.DownloadState
import ru.company.izhs_planner.generator3d.ParametricGenerator
import ru.company.izhs_planner.export.ExportService

class MainViewModel(
    private val projectRepository: ProjectRepository,
    private val chatRepository: ChatRepository,
    private val preferencesManager: PreferencesManager,
    private val aiManager: AIManager,
    private val generator: ParametricGenerator,
    private val exportService: ExportService,
    private val adsManager: MobileAdsManager,
    private val premiumManager: PremiumManagerImpl
) : AndroidViewModel(Application()) {
    
    val projects = projectRepository.getAllProjects()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    val isLoading = MutableStateFlow(false)
    
    val selectedProject = MutableStateFlow<Project?>(null)
    
    val themeMode = preferencesManager.themeMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)
    
    val isDisclaimerAccepted = preferencesManager.isDisclaimerAccepted
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)
    
    val showDisclaimer = isDisclaimerAccepted.map { accepted -> !accepted }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)
    
    val messagesToday = preferencesManager.messagesToday
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)
    
    val isPremium = preferencesManager.isPremiumActive
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)
    
    private val _currentAgentType = MutableStateFlow(AgentType.PLANNER)
    val currentAgentType: StateFlow<AgentType> = _currentAgentType
    
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages
    
    private val _chatLoading = MutableStateFlow(false)
    val chatLoading: StateFlow<Boolean> = _chatLoading
    
    val isDarkTheme = combine(themeMode, isLoading) { theme, _ ->
        theme == ThemeMode.DARK
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    
    private val _estimate = MutableStateFlow<Estimate?>(null)
    val estimate: StateFlow<Estimate?> = _estimate
    
    private val _codeCheckResults = MutableStateFlow<List<CodeCheckResult>>(emptyList())
    val codeCheckResults: StateFlow<List<CodeCheckResult>> = _codeCheckResults
    
    val aiDownloadState = aiManager.downloadState
    val aiDownloadProgress = aiManager.downloadProgress
    val isAiDownloaded = MutableStateFlow(false)
    
    init {
        viewModelScope.launch {
            adsManager.initialize()
            loadDailyLimit()
        }
    }
    
    private fun loadDailyLimit() {
        viewModelScope.launch {
            premiumManager.checkPremiumStatus()
            val downloaded = aiManager.isDownloaded()
            isAiDownloaded.value = downloaded
        }
    }
    
    fun downloadAiModel() {
        viewModelScope.launch {
            isAiDownloaded.value = false
            val success = aiManager.downloadModel()
            isAiDownloaded.value = success
        }
    }
    
    fun acceptDisclaimer() {
        viewModelScope.launch {
            preferencesManager.setDisclaimerAccepted(true)
        }
    }
    
    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            preferencesManager.setThemeMode(mode)
        }
    }
    
    fun createProject(name: String, description: String) {
        viewModelScope.launch {
            isLoading.value = true
            val projectCount = projectRepository.getProjectCount()
            
            if (!premiumManager.isPremiumActive() && projectCount >= premiumManager.maxProjectsFree) {
                isLoading.value = false
                return@launch
            }
            
            val project = Project(
                name = name,
                description = description,
                plot = Plot(),
                house = House(),
                floors = listOf(Floor(1), Floor(2))
            )
            
            projectRepository.saveProject(project)
            selectedProject.value = project
            isLoading.value = false
        }
    }
    
    fun selectProject(project: Project) {
        selectedProject.value = project
        updateProjectRooms(project)
    }
    
    fun updateProject(plot: Plot, house: House) {
        val current = selectedProject.value ?: return
        selectedProject.value = current.copy(
            plot = plot,
            house = house,
            updatedAt = System.currentTimeMillis()
        )
    }
    
    fun saveCurrentProject() {
        viewModelScope.launch {
            selectedProject.value?.let { project ->
                projectRepository.saveProject(project)
            }
        }
    }
    
    fun deleteProject(project: Project) {
        viewModelScope.launch {
            projectRepository.deleteProject(project.id)
            if (selectedProject.value?.id == project.id) {
                selectedProject.value = null
            }
        }
    }
    
    fun generateFloorPlan(rooms: List<RoomType>) {
        val project = selectedProject.value ?: return
        val floorRooms = generator.generateFloorPlan(
            project.house, 
            rooms, 
            project.house.width * project.house.length
        )
        
        val floors = floorRooms.groupBy { it.floorNumber }
            .map { (floorNum, roomList) -> Floor(floorNum, roomList) }
            .sortedBy { it.number }
        
        selectedProject.value = project.copy(
            floors = floors,
            updatedAt = System.currentTimeMillis()
        )
    }
    
    private fun updateProjectRooms(project: Project) {
        val roomTypes = listOf(
            RoomType.KITCHEN, RoomType.LIVING_ROOM,
            RoomType.BEDROOM, RoomType.BEDROOM,
            RoomType.BATHROOM, RoomType.HALLWAY
        )
        generateFloorPlan(roomTypes)
    }
    
    fun calculateMaterials() {
        val project = selectedProject.value ?: return
        _estimate.value = generator.calculateMaterials(project.house)
    }
    
    fun checkCodes() {
        val project = selectedProject.value ?: return
        viewModelScope.launch {
            _codeCheckResults.value = run {
                val house = project.house
                val plot = project.plot
                val results = mutableListOf<CodeCheckResult>()
                
                val plotPercent = plot.width * plot.length / 10000f * 100
                results.add(CodeCheckResult(
                    checkName = "Площадь застройки",
                    status = if (plotPercent <= 50) CheckStatus.PASS else CheckStatus.ERROR,
                    message = if (plotPercent <= 50) "50% от площади участка" else "Превышает 50%"
                ))
                
                results.add(CodeCheckResult(
                    checkName = "Отступ от границ",
                    status = CheckStatus.WARNING,
                    message = "Рекомендуется не менее 3 метров"
                ))
                
                results.add(CodeCheckResult(
                    checkName = "Этажность",
                    status = if (house.floors <= 3) CheckStatus.PASS else CheckStatus.ERROR,
                    message = if (house.floors <= 3) "$house.floors этажа" else "Превышает 3 этажа"
                ))
                
                results.add(CodeCheckResult(
                    checkName = "Уклон участка",
                    status = if (plot.slope <= 10) CheckStatus.PASS else CheckStatus.WARNING,
                    message = "${plot.slope}% (при >10% - террасирование)"
                ))
                
                results.add(CodeCheckResult(
                    checkName = "Высота этажа",
                    status = if (house.floorHeight >= 2.5f) CheckStatus.PASS else CheckStatus.ERROR,
                    message = "${house.floorHeight} м (минимум 2.5 м)"
                ))
                
                results
            }
        }
    }
    
    fun setAgentType(agentType: AgentType) {
        _currentAgentType.value = agentType
    }
    
    fun sendMessage(content: String) {
        if (content.isBlank()) return
        
        val project = selectedProject.value
        val projectInfo = buildProjectInfo(project)
        val isPrem = isPremium.value
        val usedToday = messagesToday.value
        
        if (!premiumManager.canUseAI(isPrem, usedToday)) {
            return
        }
        
        viewModelScope.launch {
            _chatLoading.value = true
            
            val userMessage = ChatMessage(
                role = ChatRole.USER,
                content = content
            )
            
            _chatMessages.value = _chatMessages.value + userMessage
            
            val systemPrompt = getSystemPrompt(_currentAgentType.value)
            val response = aiManager.generateResponse(
                userMessage = content,
                agentType = _currentAgentType.value,
                chatHistory = _chatMessages.value,
                projectInfo = projectInfo
            )
            
            val assistantMessage = ChatMessage(
                role = ChatRole.ASSISTANT,
                content = response.content
            )
            
            _chatMessages.value = _chatMessages.value + assistantMessage
            
            if (!isPrem) {
                preferencesManager.incrementMessagesToday()
            }
            
            val session = ChatSession(
                agentType = _currentAgentType.value,
                messages = _chatMessages.value
            )
            chatRepository.saveSession(session)
            
            _chatLoading.value = false
        }
    }
    
    fun clearChatHistory() {
        _chatMessages.value = emptyList()
        viewModelScope.launch {
            chatRepository.clearAllSessions()
        }
    }
    
    private fun buildProjectInfo(project: Project?): String {
        if (project == null) return "Проект не выбран"
        
        return """
            |Участок: ${project.plot.width}×${project.plot.length} м (${project.plot.area} сот.)
            |Уклон: ${project.plot.slope}%
            |Дом: ${project.house.width}×${project.house.length} м
            |Этажность: ${project.house.floors}
            |Высота этажа: ${project.house.floorHeight} м
            |Кровля: ${project.house.roofType}
            |Фундамент: ${project.house.foundationType}
            |Стены: ${project.house.wallMaterial.displayName}
        """.trimMargin()
    }
    
    private fun getSystemPrompt(agentType: AgentType): String {
        return when (agentType) {
            AgentType.PLANNER -> "Ты — инженер-проектировщик ИЖС, специалист по планировке."
            AgentType.CALCULATOR -> "Ты — инженер-сметчик ИЖС, специалист по расчёту материалов."
            AgentType.CODES_CHECKER -> "Ты — инженер нормоконтроля ИЖС, специалист по проверке норм."
        }
    }
    
    fun setPlotDimensions(width: Float, length: Float) {
        val project = selectedProject.value ?: return
        val area = (width * length) / 100f
        
        selectedProject.value = project.copy(
            plot = project.plot.copy(
                width = width,
                length = length,
                area = area
            ),
            updatedAt = System.currentTimeMillis()
        )
    }
    
    fun setHouseDimensions(width: Float, length: Float, floors: Int, floorHeight: Float) {
        val project = selectedProject.value ?: return
        
        selectedProject.value = project.copy(
            house = project.house.copy(
                width = width,
                length = length,
                floors = floors,
                floorHeight = floorHeight
            )
        ),
        updatedAt = System.currentTimeMillis()
    }
    
    fun setRoofType(roofType: RoofType) {
        val project = selectedProject.value ?: return
        selectedProject.value = project.copy(
            house = project.house.copy(roofType = roofType),
            updatedAt = System.currentTimeMillis()
        )
    }
    
    fun setFoundationType(foundationType: FoundationType) {
        val project = selectedProject.value ?: return
        selectedProject.value = project.copy(
            house = project.house.copy(foundationType = foundationType),
            updatedAt = System.currentTimeMillis()
        )
    }
    
    fun setWallMaterial(material: WallMaterial) {
        val project = selectedProject.value ?: return
        selectedProject.value = project.copy(
            house = project.house.copy(wallMaterial = material),
            updatedAt = System.currentTimeMillis()
        )
    }
    
    fun exportToPdf(includeWatermark: Boolean = true): Boolean {
        val project = selectedProject.value ?: return false
        
        val useWatermark = !premiumManager.canExportWithoutWatermark(isPremium.value) || includeWatermark
        
        return exportService.exportToPdf(project, useWatermark) != null
    }
    
    fun exportToObj(): Boolean {
        val project = selectedProject.value ?: return false
        return exportService.exportToObj(project) != null
    }
    
    fun onDestroy() {
        adsManager.destroy()
    }
}