package ru.company.izhs_planner.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import ru.company.izhs_planner.domain.model.Project

@Composable
fun SyncProjectsScreen(
    localProjects: List<Project>,
    cloudProjects: List<Project>,
    isSyncing: Boolean,
    lastSyncTime: Long,
    onSyncProject: (Project) -> Unit,
    onDeleteProject: (Project) -> Unit,
    onUploadProject: (Project) -> Unit,
    onDownloadProject: (Project) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }
    
    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Мои проекты") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                }
            },
            actions = {
                if (isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    IconButton(onClick = { /* Sync all */ }) {
                        Icon(Icons.Default.Sync, contentDescription = "Синхронизировать")
                    }
                }
            }
        )
        
        if (lastSyncTime > 0) {
            Text(
                text = "Последняя синхронизация: ${formatSyncTime(lastSyncTime)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
        
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("На устройстве (${localProjects.size})") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("В облаке (${cloudProjects.size})") }
            )
        }
        
        when (selectedTab) {
            0 -> {
                if (localProjects.isEmpty()) {
                    EmptyProjectsMessage()
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(localProjects) { project ->
                            ProjectSyncCard(
                                project = project,
                                onSync = { onSyncProject(project) },
                                onDelete = { onDeleteProject(project) },
                                showCloudStatus = true
                            )
                        }
                    }
                }
            }
            1 -> {
                if (cloudProjects.isEmpty()) {
                    EmptyProjectsMessage(message = "Нет проектов в облаке")
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(cloudProjects) { project ->
                            ProjectSyncCard(
                                project = project,
                                onSync = { onDownloadProject(project) },
                                onDelete = { onDeleteProject(project) },
                                showCloudStatus = false
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProjectSyncCard(
    project: Project,
    onSync: () -> Unit,
    onDelete: () -> Unit,
    showCloudStatus: Boolean,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = project.name.ifEmpty { "Без названия" },
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
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
            
            Row {
                IconButton(onClick = onSync) {
                    Icon(
                        if (showCloudStatus) Icons.Default.CloudUpload else Icons.Default.CloudDownload,
                        contentDescription = if (showCloudStatus) "В облако" else "На устройство"
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
}

@Composable
fun EmptyProjectsMessage(
    message: String = "Нет сохранённых проектов",
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.FolderOff,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatSyncTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60_000 -> "только что"
        diff < 3600_000 -> "${diff / 60_000} мин. назад"
        diff < 86400_000 -> "${diff / 3600_000} ч. назад"
        else -> "${diff / 86400_000} дн. назад"
    }
}

@Composable
fun PremiumPurchaseScreen(
    isPremium: Boolean,
    isLoading: Boolean,
    onPurchase: () -> Unit,
    onRestore: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.WorkspacePremium,
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = if (isPremium) MaterialTheme.colorScheme.primary 
                   else MaterialTheme.colorScheme.tertiary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = if (isPremium) "Премиум" else "Премиум",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = if (isPremium) "Активен" else "Разблокируйте все возможности",
            style = MaterialTheme.typography.bodyLarge,
            color = if (isPremium) MaterialTheme.colorScheme.primary 
                    else MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Features list
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                PremiumFeatureItem(icon = Icons.Default.Cloud, text = "Синхронизация проектов")
                PremiumFeatureItem(icon = Icons.Default.Chat, text = "Безлимитный ИИ")
                PremiumFeatureItem(icon = Icons.Default.WaterDrop, text = "Экспорт без водяного знака")
                PremiumFeatureItem(icon = Icons.Default.Block, text = "Без рекламы")
                PremiumFeatureItem(icon = Icons.Default.Inventory, text = "Неограниченное количество проектов")
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        if (isPremium) {
            Text(
                text = "Срок действия: бессрочно",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            Button(
                onClick = onPurchase,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Купить за 399 ₽")
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            TextButton(
                onClick = onRestore,
                enabled = !isLoading
            ) {
                Text("Восстановить покупки")
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        TextButton(onClick = onBack) {
            Text("Назад")
        }
    }
}

@Composable
private fun PremiumFeatureItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}