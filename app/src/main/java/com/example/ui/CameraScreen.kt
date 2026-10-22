package com.example.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaActionSound
import android.net.Uri
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.camera.CameraManager
import com.example.camera.MediaStoreManager
import com.example.model.AspectRatioOption
import com.example.model.CameraFlashMode
import com.example.model.CameraLensFacing
import com.example.model.CameraSettings
import com.example.model.MediaItem
import com.example.model.ResolutionOption
import kotlinx.coroutines.delay

@Composable
fun CameraScreen(
    cameraManager: CameraManager,
    mediaStoreManager: MediaStoreManager,
    settings: CameraSettings,
    latestMedia: MediaItem?,
    onSettingsChanged: (CameraSettings) -> Unit,
    onOpenGallery: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenImport: (Uri, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptic = LocalHapticFeedback.current
    val actionSound = remember { MediaActionSound().apply { load(MediaActionSound.SHUTTER_CLICK) } }

    DisposableEffect(Unit) {
        onDispose {
            actionSound.release()
        }
    }

    // Camera permissions
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
    }

    // Photo picker for media import
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val type = context.contentResolver.getType(uri) ?: ""
            val isVideo = type.startsWith("video")
            onOpenImport(uri, isVideo)
        }
    }

    // Reactive camera states
    val lensFacing by cameraManager.lensFacing.collectAsState()
    val flashMode by cameraManager.flashMode.collectAsState()
    val isRecording by cameraManager.isRecording.collectAsState()
    val recordingSeconds by cameraManager.recordingDurationSeconds.collectAsState()
    val shutterAnim by cameraManager.shutterAnimTrigger.collectAsState()
    val screenFlash by cameraManager.screenFlashTrigger.collectAsState()

    var isVideoMode by remember { mutableStateOf(false) }
    var isCapturingPhoto by remember { mutableStateOf(false) }
    var focusPoint by remember { mutableStateOf<Offset?>(null) }
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }
    var successToast by remember { mutableStateOf<String?>(null) }

    // Focus point timeout
    LaunchedEffect(focusPoint) {
        if (focusPoint != null) {
            delay(1800)
            focusPoint = null
        }
    }

    // Shutter flash animation timeout
    LaunchedEffect(shutterAnim) {
        if (shutterAnim) {
            delay(120)
            cameraManager.dismissFlashAnim()
        }
    }

    // Notification toast timeout
    LaunchedEffect(successToast) {
        if (successToast != null) {
            delay(2600)
            successToast = null
        }
    }

    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = DarkCanvas
    ) {
        if (!hasCameraPermission) {
            // Elegant, human-crafted glassmorphism permission prompt
            LuxuryPermissionPrompt(
                onGrantClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }
            )
        } else {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = topPadding, bottom = bottomPadding)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {

                    // 1. TOP HEADER & CONTROLS DOCK
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Top status bar
                        LuxuryRetroHeader()

                        Spacer(modifier = Modifier.height(4.dp))

                        // Controls Row: [ IMPORT ] [ ⚡ FLASH ] | [ ↺ REAR/FRONT ] [ ⚙ ]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left actions
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                LuxuryGlassButton(
                                    text = "IMPORT",
                                    onClick = {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                        )
                                    },
                                    testTag = "import_button"
                                )

                                val flashLabel = when (flashMode) {
                                    CameraFlashMode.OFF -> "FLASH OFF"
                                    CameraFlashMode.ON -> "FLASH ON"
                                    CameraFlashMode.AUTO -> "FLASH AUTO"
                                }
                                LuxuryGlassButton(
                                    text = flashLabel,
                                    active = flashMode != CameraFlashMode.OFF,
                                    accentColor = AccentAmber,
                                    onClick = {
                                        val next = when (flashMode) {
                                            CameraFlashMode.OFF -> CameraFlashMode.ON
                                            CameraFlashMode.ON -> CameraFlashMode.AUTO
                                            CameraFlashMode.AUTO -> CameraFlashMode.OFF
                                        }
                                        cameraManager.setFlashMode(next)
                                        onSettingsChanged(settings.copy(flashMode = next))
                                    },
                                    testTag = "flash_button"
                                )
                            }

                            // Right actions
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val lensLabel = if (lensFacing == CameraLensFacing.BACK) "FRONT" else "REAR"
                                LuxuryGlassButton(
                                    text = lensLabel,
                                    onClick = {
                                        previewViewRef?.let { pView ->
                                            cameraManager.switchCamera(lifecycleOwner, pView)
                                        }
                                    },
                                    testTag = "camera_switch_btn"
                                )

                                // Settings button
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0x66222230))
                                        .border(1.dp, GlassBorderRefined, CircleShape)
                                        .clickable(onClick = onOpenSettings)
                                        .testTag("settings_icon_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Settings",
                                        tint = TextPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // TACTILE MP SELECTOR BAR (0.3 • 0.4 • 0.5 • 0.9 • 1 • 2 • 3)
                        LuxuryMpSelectorStrip(
                            selectedResolution = settings.resolution,
                            onSelect = { res ->
                                onSettingsChanged(settings.copy(resolution = res))
                            },
                            modifier = Modifier.padding(horizontal = 14.dp)
                        )
                    }

                    // 2. LIVE CAMERA VIEWFINDER (Curved, Floating, Authentic 4:3 letterboxing)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF040406))
                            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        // CameraX Preview
                        AndroidView(
                            factory = { ctx ->
                                PreviewView(ctx).apply {
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                    scaleType = PreviewView.ScaleType.FILL_CENTER
                                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                                    previewViewRef = this
                                    cameraManager.initializeCamera(lifecycleOwner, this)
                                }
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(previewViewRef) {
                                    detectTapGestures { offset ->
                                        focusPoint = offset
                                        previewViewRef?.let { pView ->
                                            cameraManager.tapToFocus(
                                                factory = pView.meteringPointFactory,
                                                x = offset.x,
                                                y = offset.y
                                            )
                                        }
                                    }
                                }
                        )

                        // Focus indicator
                        if (focusPoint != null) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .align(Alignment.Center)
                                    .border(1.5.dp, AccentAmber, RoundedCornerShape(4.dp))
                            )
                        }

                        // Live recording indicator
                        if (isRecording) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(12.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xDD000000))
                                    .border(1.dp, AccentRed, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                val min = recordingSeconds / 60
                                val sec = recordingSeconds % 60
                                Text(
                                    text = String.format("● REC %02d:%02d [SD LOW]", min, sec),
                                    color = AccentRed,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Floating Zoom Pill on the right edge
                        LuxuryZoomControl(
                            onZoomIn = { cameraManager.zoomIn() },
                            onZoomOut = { cameraManager.zoomOut() },
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 10.dp)
                        )

                        // Bottom-Left Date Stamp watermark preview
                        if (settings.dateStampEnabled) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(12.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0x99000000))
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "23.10.2026",
                                    color = AccentAmber,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Bottom-Right Sasuke signature watermark preview
                        if (settings.sasukeWatermarkEnabled) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(12.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0x99000000))
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "⚡ Sasuke",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Screen flash overlay for front camera
                        if (screenFlash) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFFFFFAF0))
                            )
                        }

                        // Shutter flash animation
                        ShutterFlashOverlay(visible = shutterAnim)

                        // Loading spinner
                        if (isCapturingPhoto) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0x55000000)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = AccentAmber, strokeWidth = 3.dp)
                            }
                        }
                    }

                    // Notification banner
                    AnimatedVisibility(
                        visible = successToast != null,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        successToast?.let { msg ->
                            Box(
                                modifier = Modifier
                                    .padding(bottom = 6.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xEE1A1A24))
                                    .border(1.dp, AccentAmber, RoundedCornerShape(16.dp))
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = msg,
                                    color = AccentAmber,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // 3. BOTTOM CONTROL DECK
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Mode Switcher Pill [ PHOTO | VIDEO ]
                        LuxuryModeSwitcher(
                            isVideoMode = isVideoMode,
                            onModeChanged = { videoMode ->
                                if (videoMode && !hasAudioPermission && settings.recordAudio) {
                                    audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                                isVideoMode = videoMode
                            }
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Main Shutter Row: [ GALLERY ] [ SHUTTER ] [ DATE TOGGLE ]
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left: Latest photo thumbnail
                            Box(
                                modifier = Modifier
                                    .testTag("gallery_thumbnail_btn")
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0x66222230))
                                    .border(1.dp, GlassBorderRefined, RoundedCornerShape(16.dp))
                                    .clickable(onClick = onOpenGallery),
                                contentAlignment = Alignment.Center
                            ) {
                                if (latestMedia != null) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(latestMedia.uri)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "Gallery",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.AddPhotoAlternate,
                                        contentDescription = "Gallery",
                                        tint = TextMuted,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            // Center: Machined Luxury Shutter Button
                            LuxuryShutterButton(
                                isVideoMode = isVideoMode,
                                isRecording = isRecording,
                                enabled = !isCapturingPhoto,
                                onClick = {
                                    if (isVideoMode) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        if (isRecording) {
                                            cameraManager.stopRecording()
                                        } else {
                                            cameraManager.startRecording(
                                                hasAudioPermission = hasAudioPermission,
                                                settings = settings,
                                                onRecordingStarted = {},
                                                onRecordingSaved = { uri, filename ->
                                                    successToast = "Saved $filename"
                                                },
                                                onError = { err ->
                                                    Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                        }
                                    } else {
                                        if (settings.soundEnabled) {
                                            actionSound.play(MediaActionSound.SHUTTER_CLICK)
                                        }
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        isCapturingPhoto = true
                                        cameraManager.takePhoto(
                                            settings = settings,
                                            onCaptureSuccess = { uri, filename, width, height ->
                                                isCapturingPhoto = false
                                                successToast = "$filename • $width×$height"
                                            },
                                            onError = { err ->
                                                isCapturingPhoto = false
                                                Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    }
                                }
                            )

                            // Right: Instant Date Toggle Pill
                            LuxuryGlassButton(
                                text = if (settings.dateStampEnabled) "DATE ON" else "DATE OFF",
                                active = settings.dateStampEnabled,
                                accentColor = AccentAmber,
                                onClick = {
                                    onSettingsChanged(settings.copy(dateStampEnabled = !settings.dateStampEnabled))
                                },
                                testTag = "date_quick_toggle"
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Beautiful, human-designed dark glass permission prompt.
 */
@Composable
private fun LuxuryPermissionPrompt(
    onGrantClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkCanvas)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF1E1E28), Color(0xFF12121A))
                    )
                )
                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(24.dp))
                .padding(28.dp)
        ) {
            // Sasuke Avatar with luminous halo
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(92.dp)
                    .clip(CircleShape)
                    .background(Color(0x3300E5FF))
                    .padding(4.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_sasuke_mark),
                    contentDescription = "Sasuke",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .border(2.dp, AccentAmber, CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "SASUKE RETROCAM",
                color = TextPrimary,
                fontSize = 19.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "VINTAGE CCD CAMERA ENGINE",
                color = AccentAmber,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "To capture authentic, low-resolution 0.4 MP nostalgic photos and retro videos with the Sasuke signature, grant camera permission.",
                color = TextMuted,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(26.dp))

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(RoundedCornerShape(23.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFFFFC107), AccentAmber)
                        )
                    )
                    .clickable(onClick = onGrantClick)
                    .testTag("grant_camera_permission_button")
            ) {
                Text(
                    text = "ENABLE CAMERA",
                    color = Color.Black,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}
