package ru.company.izhs_planner.ui.screens

import android.widget.FrameLayout
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.collectLatest
import ru.company.izhs_planner.MainViewModel
import ru.company.izhs_planner.R
import ru.company.izhs_planner.domain.model.chat.*
import ru.company.izhs_planner.ai.DownloadState
import ru.company.izhs_planner.premium.PremiumManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onPrivacyPolicy: () -> Unit
) {
    val navController = androidx.navigation.compose.rememberNavController()
    val selectedProject by viewModel.selectedProject.collectAsState()
    val projectCount by viewModel.projects.collectLatest { it.size }
    val isPremium by viewModel.isPremium.collectAsState()
    val messagesToday by viewModel.messagesToday.collectAsState()
    
    var showCreateDialog by remember { mutableStateOf(false) }
    var showPremiumDialog by remember { mutableStateOf(false) }
    var showExportMenu by remember { mutableStateOf(false) }
    
    val items = listOf(
        androidx.navigation.compose.NavDestination(
            icon = { Icon(Icons.Default.Home, contentDescription = null) },
            label = "Проекты",
            route = "projects"
        ),
        androidx.navigation.compose.NavDestination(
            icon = { Icon(Icons.Default.Edit, contentDescription = null) },
            label = "Параметры",
            route = "params"
        ),
        androidx.navigation.compose.NavDestination(
            icon = { Icon(Icons.Default.Chat, contentDescription = null) },
            label = "ИИ",
            route = "chat"
        ),
        androidx.navigation.compose.NavDestination(
            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
            label = "Настройки",
            route = "settings"
        )
    )
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ИЖС-Проектировщик") },
                actions = {
                    IconButton(onClick = { showExportMenu = true }) {
                        Icon(Icons.Default.Share, contentDescription = "Экспорт")
                    }
                    DropdownMenu(
                        expanded = showExportMenu,
                        onDismissRequest = { showExportMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Экспорт в PDF") },
                            onClick = { viewModel.exportToPdf(); showExportMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Экспорт в OBJ") },
                            onClick = { viewModel.exportToObj(); showExportMenu = false }
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                items.forEach { destination ->
                    NavigationBarItem(
                        icon = { destination.icon },
                        label = { destination.label },
                        selected = navController.currentBackStackEntryAsState().value?.destination?.route == destination.route,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo("projects") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "projects",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("projects") {
                ProjectsScreen(
                    viewModel = viewModel,
                    onCreateProject = { showCreateDialog = true }
                )
            }
            composable("params") {
                ParamsScreen(viewModel = viewModel)
            }
            composable("chat") {
                ChatScreen(viewModel = viewModel)
            }
            composable("settings") {
                SettingsScreen(
                    viewModel = viewModel,
                    onPrivacyPolicy = onPrivacyPolicy
                )
            }
        }
    }
    
    if (showCreateDialog) {
        CreateProjectDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, desc ->
                viewModel.createProject(name, desc)
                showCreateDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(
    viewModel: MainViewModel,
    onCreateProject: () -> Unit
) {
    val projects by viewModel.projects.collectAsState()
    val selectedProject by viewModel.selectedProject.collectAsState()
    val isPremium by viewModel.isPremium.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Мои проекты",
                style = MaterialTheme.typography.headlineSmall
            )
            FilledTonalButton(onClick = onCreateProject) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Создать")
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        if (projects.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Architecture,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Нет сохранённых проектов",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Создайте свой первый проект",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(projects) { project ->
                    ProjectCard(
                        project = project,
                        isSelected = project.id == selectedProject?.id,
                        onClick = { viewModel.selectProject(project) },
                        onDelete = { viewModel.deleteProject(project) }
                    )
                }
            }
        }
    }
}

@Composable
fun ProjectCard(
    project: Project,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = if (isSelected) CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ) else CardDefaults.cardColors()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = project.name.ifEmpty { "Проект без названия" },
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${project.house.width}×${project.house.length} м, ${project.house.floors} эт.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Общая пл.: ${project.totalArea} м²",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Удалить",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun CreateProjectDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Создание проекта") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название проекта") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Описание") },
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(name, description) },
                enabled = name.isNotEmpty()
            ) {
                Text("Создать")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParamsScreen(viewModel: MainViewModel) {
    val project by viewModel.selectedProject.collectAsState()
    
    var plotWidth by remember { mutableStateOf("20") }
    var plotLength by remember { mutableStateOf("30") }
    var plotSlope by remember { mutableStateOf("0") }
    var houseWidth by remember { mutableStateOf("10") }
    var houseLength by remember { mutableStateOf("12") }
    var floors by remember { mutableStateOf("2") }
    var floorHeight by remember { mutableStateOf("3") }
    
    LaunchedEffect(project) {
        project?.let {
            plotWidth = it.plot.width.toInt().toString()
            plotLength = it.plot.length.toInt().toString()
            plotSlope = it.plot.slope.toInt().toString()
            houseWidth = it.house.width.toInt().toString()
            houseLength = it.house.length.toInt().toString()
            floors = it.house.floors.toString()
            floorHeight = it.house.floorHeight.toString()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Параметры") },
                actions = {
                    TextButton(onClick = { viewModel.saveCurrentProject() }) {
                        Text("Сохранить")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Параметры участка",
                style = MaterialTheme.typography.titleMedium
            )
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = plotWidth,
                    onValueChange = {
                        plotWidth = it
                        (it.toFloatOrNull() ?: 0f).let { w -> 
                            viewModel.setPlotDimensions(w, plotLength.toFloatOrNull() ?: 30f) 
                        }
                    },
                    label = { Text("Ширина, м") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = plotLength,
                    onValueChange = {
                        plotLength = it
                        (it.toFloatOrNull() ?: 30f).let { l -> 
                            viewModel.setPlotDimensions(plotWidth.toFloatOrNull() ?: 20f, l) 
                        }
                    },
                    label = { Text("Длина, м") },
                    modifier = Modifier.weight(1f)
                )
            }
            
            OutlinedTextField(
                value = plotSlope,
                onValueChange = { plotSlope = it },
                label = { Text("Уклон, %") },
                modifier = Modifier.fillMaxWidth()
            )
            
            HorizontalDivider()
            
            Text(
                text = "Параметры дома",
                style = MaterialTheme.typography.titleMedium
            )
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = houseWidth,
                    onValueChange = {
                        houseWidth = it
                        it.toFloatOrNull()?.let { w ->
                            viewModel.setHouseDimensions(
                                w, 
                                houseLength.toFloatOrNull() ?: 12f, 
                                floors.toIntOrNull() ?: 2, 
                                floorHeight.toFloatOrNull() ?: 3f
                            )
                        }
                    },
                    label = { Text("Ширина, м") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = houseLength,
                    onValueChange = {
                        houseLength = it
                        it.toFloatOrNull()?.let { l ->
                            viewModel.setHouseDimensions(
                                houseWidth.toFloatOrNull() ?: 10f, 
                                l, 
                                floors.toIntOrNull() ?: 2, 
                                floorHeight.toFloatOrNull() ?: 3f
                            )
                        }
                    },
                    label = { Text("Длина, м") },
                    modifier = Modifier.weight(1f)
                )
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = floors,
                    onValueChange = {
                        floors = it
                        it.toIntOrNull()?.let { f ->
                            viewModel.setHouseDimensions(
                                houseWidth.toFloatOrNull() ?: 10f, 
                                houseLength.toFloatOrNull() ?: 12f, 
                                f, 
                                floorHeight.toFloatOrNull() ?: 3f
                            )
                        }
                    },
                    label = { Text("Этажи") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = floorHeight,
                    onValueChange = { floorHeight = it },
                    label = { Text("Высота, м") },
                    modifier = Modifier.weight(1f)
                )
            }
            
            HorizontalDivider()
            
            Text(
                text = "Конструктивные элементы",
                style = MaterialTheme.typography.titleMedium
            )
            
            var selectedRoof by remember { mutableStateOf(RoofType.GABLED) }
            var selectedFoundation by remember { mutableStateOf(FoundationType.STRIP) }
            var selectedWall by remember { mutableStateOf(WallMaterial.GAS_BLOCK) }
            
            Text(
                text = "Тип кровли",
                style = MaterialTheme.typography.bodyMedium
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RoofType.entries.forEach { roof ->
                    FilterChip(
                        selected = selectedRoof == roof,
                        onClick = { 
                            selectedRoof = roof
                            viewModel.setRoofType(roof)
                        },
                        label = { Text(roof.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Фундамент",
                style = MaterialTheme.typography.bodyMedium
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FoundationType.entries.forEach { foundation ->
                    FilterChip(
                        selected = selectedFoundation == foundation,
                        onClick = { 
                            selectedFoundation = foundation
                            viewModel.setFoundationType(foundation)
                        },
                        label = { Text(foundation.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Материал стен",
                style = MaterialTheme.typography.bodyMedium
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WallMaterial.entries.forEach { wall ->
                    FilterChip(
                        selected = selectedWall == wall,
                        onClick = { 
                            selectedWall = wall
                            viewModel.setWallMaterial(wall)
                        },
                        label = { Text(wall.displayName) }
                    )
                }
            }
            
            HorizontalDivider()
            
            Button(
                onClick = { viewModel.calculateMaterials() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Calculate, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Рассчитать материалы")
            }
            
            Button(
                onClick = { viewModel.checkCodes() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Проверить нормы")
            }
        }
    }
}

@Composable
fun ChatScreen(viewModel: MainViewModel) {
    val messages by viewModel.chatMessages.collectAsState()
    val loading by viewModel.chatLoading.collectAsState()
    val agentType by viewModel.currentAgentType.collectAsState()
    val isPremium by viewModel.isPremium.collectAsState()
    val messagesToday by viewModel.messagesToday.collectAsState()
    
    var inputText by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()
    
    LaunchedEffect(messages.size) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AgentType.entries.forEach { type ->
                FilterChip(
                    selected = agentType == type,
                    onClick = { viewModel.setAgentType(type) },
                    label = { Text(when (type) {
                        AgentType.PLANNER -> "Планировщик"
                        AgentType.CALCULATOR -> "Калькулятор"
                        AgentType.CODES_CHECKER -> "Нормы"
                    }) }
                )
            }
        }
        
        Spacer(Modifier.height(8.dp))
        
        val limitText = if (isPremium) "Безлимит" else "Осталось: ${50 - messagesToday}/50"
        Text(
            text = limitText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(Modifier.height(8.dp))
        
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
                if (messages.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Задайте вопрос по проектированию",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(messages) { message ->
                            val isUser = message.role == ChatRole.USER
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                            ) {
                                Card(
                                    colors = if (isUser) CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                    ) else CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Text(
                                        text = message.content,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                
                if (loading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        }
        
        Spacer(Modifier.height(8.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("Ваш вопрос...") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = { 
                    viewModel.sendMessage(inputText)
                    inputText = ""
                },
                enabled = inputText.isNotBlank() && !loading
            ) {
                Icon(Icons.Default.Send, contentDescription = "Отправить")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onPrivacyPolicy: () -> Unit
) {
    val themeMode by viewModel.themeMode.collectAsState()
    val isPremium by viewModel.isPremium.collectAsState()
    
    Scaffold(
        topBar = { TopAppBar(title = { Text("Настройки") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text("Тема", style = MaterialTheme.typography.titleMedium)
            
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = themeMode == mode,
                        onClick = { viewModel.setThemeMode(mode) },
                        label = { Text(when (mode) {
                            ThemeMode.LIGHT -> "Светлая"
                            ThemeMode.DARK -> "Тёмная"
                            ThemeMode.SYSTEM -> "Системная"
                        }) }
                    )
                }
            }
            
            HorizontalDivider(Modifier.padding(vertical = 16.dp))
            
            Text("Премиум", style = MaterialTheme.typography.titleMedium)
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isPremium) "Премиум активен" else "Премиум не активен",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Безлимитный ИИ, экспорт без водяного знака",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (!isPremium) {
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { /* purchase */ }) {
                            Text("Купить за 399 ₽")
                        }
                    }
                }
            }
            
            HorizontalDivider(Modifier.padding(vertical = 16.dp))
            
            Text("О приложении", style = MaterialTheme.typography.titleMedium))
            
            TextButton(onClick = onPrivacyPolicy) {
                Text("Политика конфиденциальности")
            }
        }
    }
}