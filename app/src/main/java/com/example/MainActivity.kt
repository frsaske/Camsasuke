package com.example

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.camera.CameraManager
import com.example.camera.MediaStoreManager
import com.example.model.CameraSettings
import com.example.model.MediaItem
import com.example.ui.CameraScreen
import com.example.ui.GalleryScreen
import com.example.ui.ImportEditScreen
import com.example.ui.MediaDetailScreen
import com.example.ui.SettingsScreen
import com.example.ui.theme.SasukeBlack
import com.example.ui.theme.SasukeRetroCamTheme
import kotlinx.coroutines.launch

sealed interface Screen {
    object Camera : Screen
    object Gallery : Screen
    data class MediaDetail(val item: MediaItem) : Screen
    data class ImportEdit(val uri: Uri, val isVideo: Boolean) : Screen
    object Settings : Screen
}

class MainActivity : ComponentActivity() {

    private lateinit var mediaStoreManager: MediaStoreManager
    private lateinit var cameraManager: CameraManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        mediaStoreManager = MediaStoreManager(applicationContext)
        cameraManager = CameraManager(applicationContext, mediaStoreManager, lifecycleScope)

        setContent {
            SasukeRetroCamTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = SasukeBlack
                ) {
                    RetroCamApp(
                        cameraManager = cameraManager,
                        mediaStoreManager = mediaStoreManager
                    )
                }
            }
        }
    }
}

@Composable
fun RetroCamApp(
    cameraManager: CameraManager,
    mediaStoreManager: MediaStoreManager
) {
    val coroutineScope = rememberCoroutineScope()
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Camera) }
    var settings by remember { mutableStateOf(CameraSettings()) }
    var mediaList by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var isLoadingMedia by remember { mutableStateOf(true) }
    var nextFilenamePreview by remember { mutableStateOf("Sasuke-001.jpg") }

    fun refreshMedia() {
        coroutineScope.launch {
            isLoadingMedia = true
            mediaList = mediaStoreManager.queryAlbumMedia()
            nextFilenamePreview = mediaStoreManager.getNextPhotoFilename()
            isLoadingMedia = false
        }
    }

    LaunchedEffect(Unit) {
        refreshMedia()
    }

    val latestMedia = mediaList.firstOrNull()

    when (val screen = currentScreen) {
        is Screen.Camera -> {
            CameraScreen(
                cameraManager = cameraManager,
                mediaStoreManager = mediaStoreManager,
                settings = settings,
                latestMedia = latestMedia,
                onSettingsChanged = { newSettings ->
                    settings = newSettings
                },
                onOpenGallery = {
                    refreshMedia()
                    currentScreen = Screen.Gallery
                },
                onOpenSettings = {
                    refreshMedia()
                    currentScreen = Screen.Settings
                },
                onOpenImport = { uri, isVideo ->
                    currentScreen = Screen.ImportEdit(uri, isVideo)
                }
            )
        }

        is Screen.Gallery -> {
            GalleryScreen(
                mediaList = mediaList,
                isLoading = isLoadingMedia,
                onItemClick = { item ->
                    currentScreen = Screen.MediaDetail(item)
                },
                onBack = {
                    currentScreen = Screen.Camera
                }
            )
        }

        is Screen.MediaDetail -> {
            MediaDetailScreen(
                item = screen.item,
                onBack = {
                    refreshMedia()
                    currentScreen = Screen.Gallery
                },
                onDelete = { deletedItem ->
                    coroutineScope.launch {
                        mediaStoreManager.deleteMedia(deletedItem.uri)
                        refreshMedia()
                        currentScreen = Screen.Gallery
                    }
                }
            )
        }

        is Screen.ImportEdit -> {
            ImportEditScreen(
                mediaUri = screen.uri,
                isVideo = screen.isVideo,
                currentSettings = settings,
                mediaStoreManager = mediaStoreManager,
                onSaveSuccess = {
                    refreshMedia()
                    currentScreen = Screen.Gallery
                },
                onBack = {
                    currentScreen = Screen.Camera
                }
            )
        }

        is Screen.Settings -> {
            SettingsScreen(
                settings = settings,
                totalAlbumCount = mediaList.size,
                nextFilenamePreview = nextFilenamePreview,
                onSettingsChanged = { newSettings ->
                    settings = newSettings
                },
                onBack = {
                    currentScreen = Screen.Camera
                }
            )
        }
    }
}
