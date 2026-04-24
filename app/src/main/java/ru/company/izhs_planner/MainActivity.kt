package ru.company.izhs_planner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ru.company.izhs_planner.data.local.datastore.PreferencesManager
import ru.company.izhs_planner.data.local.datastore.ThemeMode
import ru.company.izhs_planner.data.repository.ChatRepository
import ru.company.izhs_planner.data.repository.ProjectRepository
import ru.company.izhs_planner.mobile_ads.MobileAdsManager
import ru.company.izhs_planner.premium.PremiumManagerImpl
import ru.company.izhs_planner.premium.RuStoreBillingHelper
import ru.company.izhs_planner.ui.screens.*
import ru.company.izhs_planner.ui.theme.IzhsPlannerTheme
import ru.company.izhs_planner.ui.theme.ThemeModePreference
import ru.company.izhs_planner.ai.AIManager
import ru.company.izhs_planner.generator3d.ParametricGenerator
import ru.company.izhs_planner.export.ExportService

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: MainViewModel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val database = ru.company.izhs_planner.data.local.IzhsDatabase.getDatabase(applicationContext)
        val preferencesManager = PreferencesManager(applicationContext)
        val projectRepository = ProjectRepository(database)
        val chatRepository = ChatRepository(database)
        val aiManager = AIManager(applicationContext)
        val generator = ParametricGenerator()
        val exportService = ExportService(applicationContext)
        val adsManager = MobileAdsManager(applicationContext)
        val billingHelper = RuStoreBillingHelper(applicationContext)
        val premiumManager = PremiumManagerImpl(applicationContext, billingHelper)
        
        viewModel = MainViewModel(
            projectRepository = projectRepository,
            chatRepository = chatRepository,
            preferencesManager = preferencesManager,
            aiManager = aiManager,
            generator = generator,
            exportService = exportService,
            adsManager = adsManager,
            premiumManager = premiumManager
        )
        
        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val isDarkTheme by viewModel.isDarkTheme.collectAsState()
            val isAiDownloaded by viewModel.isAiDownloaded.collectAsState()
            val aiDownloadState by viewModel.aiDownloadState.collectAsState()
            val aiDownloadProgress by viewModel.aiDownloadProgress.collectAsState()
            
            IzhsPlannerTheme(
                darkTheme = isDarkTheme,
                themeMode = when (themeMode) {
                    ThemeMode.LIGHT -> ThemeModePreference.LIGHT
                    ThemeMode.DARK -> ThemeModePreference.DARK
                    ThemeMode.SYSTEM -> ThemeModePreference.SYSTEM
                }
            ) {
                val showDisclaimer by viewModel.showDisclaimer.collectAsState()
                
                if (showDisclaimer) {
                    DisclaimerDialog(
                        onAccept = { viewModel.acceptDisclaimer() },
                        onDecline = { finish() }
                    )
                } else if (!isAiDownloaded && aiDownloadState == ru.company.izhs_planner.ai.DownloadState.NOT_STARTED) {
                    AiDownloadPrompt(
                        downloadState = aiDownloadState,
                        progress = aiDownloadProgress,
                        isDownloaded = isAiDownloaded,
                        onDownload = { viewModel.downloadAiModel() },
                        onSkip = { }
                    )
                } else {
                    MainScreen(
                        viewModel = viewModel,
                        onPrivacyPolicy = { openPrivacyPolicy() }
                    )
                }
            }
        }
    }
    
    private fun openPrivacyPolicy() {
        try {
            val intent = android.content.Intent(this, PrivacyPolicyActivity::class.java)
            startActivity(intent)
        } catch (e: Exception) {
            android.widget.Toast.makeText(this, "Не удалось открыть политику", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        viewModel.onDestroy()
    }
}