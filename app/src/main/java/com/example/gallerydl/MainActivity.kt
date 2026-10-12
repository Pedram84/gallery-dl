package com.example.gallerydl

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.gallerydl.data.repository.ConfigRepository
import com.example.gallerydl.data.repository.JobRepository
import com.example.gallerydl.engine.GalleryDlEngine
import com.example.gallerydl.ui.components.AppBottomBar
import com.example.gallerydl.ui.components.AppTab
import com.example.gallerydl.ui.components.AppTopBar
import com.example.gallerydl.ui.screens.*
import com.example.gallerydl.ui.theme.DarkBackground
import com.example.gallerydl.ui.theme.GalleryDlTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var jobRepository: JobRepository
    private lateinit var configRepository: ConfigRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        GalleryDlEngine.registerContext(this)
        jobRepository = JobRepository(this)
        configRepository = ConfigRepository(this)

        // Handle shared URL if opened from browser or share sheet
        var sharedUrl: String? = null
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            sharedUrl = intent.getStringExtra(Intent.EXTRA_TEXT)?.trim()
        }

        setContent {
            GalleryDlTheme {
                MainApp(
                    jobRepository = jobRepository,
                    configRepository = configRepository,
                    initialSharedUrl = sharedUrl
                )
            }
        }
    }
}

@Composable
fun MainApp(
    jobRepository: JobRepository,
    configRepository: ConfigRepository,
    initialSharedUrl: String?
) {
    var currentTab by remember { mutableStateOf(AppTab.DOWNLOADER) }
    var selectedUrlForDownloader by remember { mutableStateOf(initialSharedUrl ?: "") }
    var versionText by remember { mutableStateOf("1.32.16") }
    val currentRunningJobId by jobRepository.currentRunningJobId.collectAsState()
    val isRunning = currentRunningJobId != null

    LaunchedEffect(Unit) {
        try {
            versionText = GalleryDlEngine.getVersion()
        } catch (e: Exception) {
            versionText = "Error: ${e.message}"
        }
    }

    // Handle back button: return to Downloader tab if currently on other tabs
    BackHandler(enabled = currentTab != AppTab.DOWNLOADER) {
        currentTab = AppTab.DOWNLOADER
    }

    Scaffold(
        topBar = {
            AppTopBar(
                version = versionText,
                isRunning = isRunning,
                onCancelJob = { jobRepository.cancelCurrentJob() }
            )
        },
        bottomBar = {
            AppBottomBar(
                currentTab = currentTab,
                onTabSelected = { currentTab = it }
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = DarkBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .padding(innerPadding)
        ) {
            when (currentTab) {
                AppTab.DOWNLOADER -> {
                    DownloaderScreen(
                        jobRepository = jobRepository,
                        initialUrl = selectedUrlForDownloader,
                        onNavigateToJobs = { currentTab = AppTab.JOBS }
                    )
                }
                AppTab.JOBS -> {
                    DownloadsScreen(
                        jobRepository = jobRepository,
                        onRerunJob = { rerunUrl ->
                            selectedUrlForDownloader = rerunUrl
                            currentTab = AppTab.DOWNLOADER
                        }
                    )
                }
                AppTab.EXTRACTORS -> {
                    ExtractorsScreen(
                        onSelectExampleUrl = { exampleUrl ->
                            selectedUrlForDownloader = exampleUrl
                            currentTab = AppTab.DOWNLOADER
                        }
                    )
                }
                AppTab.CONFIG -> {
                    ConfigScreen(
                        configRepository = configRepository
                    )
                }
                AppTab.TERMINAL -> {
                    TerminalScreen()
                }
            }
        }
    }
}
