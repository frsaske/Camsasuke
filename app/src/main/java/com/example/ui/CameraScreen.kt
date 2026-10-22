package com.example.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.camera.CameraManager
import com.example.camera.MediaStoreManager
import com.example.model.AspectRatioOption
import com.example.model.CameraFlashMode
import com.example.model.CameraLensFacing
import com.example.model.CameraSettings
import com.example.model.MediaItem
import com.example.model.ResolutionOption
import com.example.model.RetroPreset
import com.example.ui.theme.GlassBackground
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.SasukeAmber
import com.example.ui.theme.SasukeBlack
import com.example.ui.theme.SasukeBorder
import com.example.ui.theme.SasukeCharcoal
import com.example.ui.theme.SasukeMuted
import com.example.ui.theme.SasukeRed
import com.example.ui.theme.SasukeSilver
import com.example.ui.theme.SasukeSurface
import com.example.ui.theme.SasukeSurfaceVariant
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    val coroutineScope = rememberCoroutineScope()

    // Permissions
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

    // Photo picker for import
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val type = context.contentResolver.getType(uri) ?: ""
            val isVideo = type.startsWith("video")
            onOpenImport(uri, isVideo)
        }
    }

    // Camera states
    val lensFacing by cameraManager.lensFacing.collectAsState()
    val flashMode by cameraManager.flashMode.collectAsState()
    val isRecording by cameraManager.isRecording.collectAsState()
    val recordingSeconds by cameraManager.recordingDurationSeconds.collectAsState()
    val shutterAnim by cameraManager.shutterAnimTrigger.collectAsState()
    val screenFlash by cameraManager.screenFlashTrigger.collectAsState()

    var isVideoMode by remember { mutableStateOf(false) }
    var focusPoint by remember { mutableStateOf<Offset?>(null) }
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }
    var nextFilename by remember { mutableStateOf("Sasuke-001") }
    var captureSuccessMessage by remember { mutableStateOf<String?>(null) }

    // Fetch next filename for HUD
    LaunchedEffect(latestMedia, isVideoMode) {
        nextFilename = if (isVideoMode) {
            mediaStoreManager.getNextVideoFilename()
        } else {
            mediaStoreManager.getNextPhotoFilename()
        }
    }

    // Focus point timeout
    LaunchedEffect(focusPoint) {
        if (focusPoint != null) {
            delay(2500)
            focusPoint = null
        }
    }

    // Success banner timeout
    LaunchedEffect(captureSuccessMessage) {
        if (captureSuccessMessage != null) {
            delay(3000)
            captureSuccessMessage = null
        }
    }

    // Shutter flash timeout
    LaunchedEffect(shutterAnim) {
        if (shutterAnim) {
            delay(120)
            cameraManager.dismissFlashAnim()
        }
    }

    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = SasukeBlack
    ) {
        if (!hasCameraPermission) {
            // Permission Request Card
            CameraPermissionPrompt(
                onGrantClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }
            )
        } else {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val availableWidth = maxWidth
                val availableHeight = maxHeight

                // 1. Live Camera Preview Box with Aspect Ratio Framing
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(SasukeBlack),
                    contentAlignment = Alignment.Center
                ) {
                    val frameModifier = when (settings.aspectRatio) {
                        AspectRatioOption.RATIO_4_3 -> Modifier.fillMaxWidth().aspectRatio(3f / 4f)
                        AspectRatioOption.RATIO_16_9 -> Modifier.fillMaxWidth().aspectRatio(9f / 16f)
                        AspectRatioOption.RATIO_1_1 -> Modifier.fillMaxWidth().aspectRatio(1f)
                    }

                    Box(
                        modifier = frameModifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black)
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
                    ) {
                        // CameraX Preview View
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
                            modifier = Modifier.fillMaxSize()
                        )

                        // Viewfinder Overlay (brackets, crosshairs, telemetry)
                        ViewfinderOverlay(
                            settings = settings,
                            isRecording = isRecording,
                            recordingSeconds = recordingSeconds,
                            focusPoint = focusPoint,
                            nextFilename = nextFilename,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // 2. Screen Flash Simulation Overlay (for front selfie flash)
                if (screenFlash) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFFFFAF0))
                    )
                }

                // 3. Shutter Flash Overlay
                ShutterFlashOverlay(visible = shutterAnim)

                // 4. Capture Success Banner
                AnimatedVisibility(
                    visible = captureSuccessMessage != null,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = topPadding + 60.dp)
                ) {
                    captureSuccessMessage?.let { msg ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xDD121214))
                                .border(1.dp, SasukeAmber, RoundedCornerShape(20.dp))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = msg,
                                color = SasukeAmber,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // 5. TOP BAR: Sasuke wordmark, Flash, Camera switch, Settings
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(top = topPadding)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Sasuke Brand Wordmark
                    Column {
                        Text(
                            text = "SASUKE",
                            color = SasukeSilver,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 3.sp
                        )
                        Text(
                            text = "RETROCAM 0.4MP",
                            color = SasukeAmber,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                    }

                    // Top Action Icons
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Flash Toggle
                        val flashIcon = when (flashMode) {
                            CameraFlashMode.AUTO -> Icons.Default.FlashAuto
                            CameraFlashMode.ON -> Icons.Default.FlashOn
                            CameraFlashMode.OFF -> Icons.Default.FlashOff
                        }
                        GlassIconButton(
                            icon = flashIcon,
                            contentDescription = "Flash mode",
                            badgeText = if (flashMode == CameraFlashMode.AUTO) "A" else null,
                            tint = if (flashMode != CameraFlashMode.OFF) SasukeAmber else SasukeSilver,
                            onClick = {
                                val nextFlash = when (flashMode) {
                                    CameraFlashMode.AUTO -> CameraFlashMode.ON
                                    CameraFlashMode.ON -> CameraFlashMode.OFF
                                    CameraFlashMode.OFF -> CameraFlashMode.AUTO
                                }
                                cameraManager.setFlashMode(nextFlash)
                            }
                        )

                        // Camera Flip Switch
                        GlassIconButton(
                            icon = Icons.Default.Cameraswitch,
                            contentDescription = "Switch camera",
                            onClick = {
                                previewViewRef?.let { pView ->
                                    cameraManager.switchCamera(lifecycleOwner, pView)
                                }
                            }
                        )

                        // Settings Icon
                        GlassIconButton(
                            icon = Icons.Default.Settings,
                            contentDescription = "Settings",
                            onClick = onOpenSettings
                        )
                    }
                }

                // 6. BOTTOM CONTROL PANEL
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = bottomPadding)
                        .padding(bottom = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Quick Settings Chips (Preset, Resolution, Aspect Ratio)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Preset selector chip (cycles through presets)
                        RetroStatusBadge(
                            text = settings.preset.title.uppercase(),
                            highlight = settings.preset != RetroPreset.NONE,
                            onClick = {
                                val presets = RetroPreset.values()
                                val nextIndex = (settings.preset.ordinal + 1) % presets.size
                                onSettingsChanged(settings.copy(preset = presets[nextIndex]))
                            }
                        )

                        // Resolution chip (cycles: 0.4 MP -> 1.2 MP -> STD)
                        RetroStatusBadge(
                            text = settings.resolution.label,
                            highlight = settings.resolution == ResolutionOption.RETRO_0_4_MP,
                            onClick = {
                                val nextRes = when (settings.resolution) {
                                    ResolutionOption.RETRO_0_4_MP -> ResolutionOption.LOW_1_2_MP
                                    ResolutionOption.LOW_1_2_MP -> ResolutionOption.STANDARD
                                    ResolutionOption.STANDARD -> ResolutionOption.RETRO_0_4_MP
                                }
                                onSettingsChanged(settings.copy(resolution = nextRes))
                            }
                        )

                        // Aspect ratio chip (cycles: 4:3 -> 16:9 -> 1:1)
                        RetroStatusBadge(
                            text = settings.aspectRatio.label,
                            highlight = true,
                            onClick = {
                                val nextAspect = when (settings.aspectRatio) {
                                    AspectRatioOption.RATIO_4_3 -> AspectRatioOption.RATIO_16_9
                                    AspectRatioOption.RATIO_16_9 -> AspectRatioOption.RATIO_1_1
                                    AspectRatioOption.RATIO_1_1 -> AspectRatioOption.RATIO_4_3
                                }
                                onSettingsChanged(settings.copy(aspectRatio = nextAspect))
                            }
                        )

                        // Date stamp badge
                        RetroStatusBadge(
                            text = if (settings.dateStampEnabled) "DATE ON" else "DATE OFF",
                            highlight = settings.dateStampEnabled,
                            onClick = {
                                onSettingsChanged(settings.copy(dateStampEnabled = !settings.dateStampEnabled))
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Mode Switcher [ PHOTO | VIDEO ]
                    ModeSwitcher(
                        isVideoMode = isVideoMode,
                        onModeChanged = { videoMode ->
                            if (videoMode && !hasAudioPermission && settings.recordAudio) {
                                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                            isVideoMode = videoMode
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Primary Shutter & Gallery Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Gallery Thumbnail
                        Box(
                            modifier = Modifier
                                .testTag("gallery_thumbnail_button")
                                .size(54.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SasukeSurface)
                                .border(1.5.dp, SasukeSilver, RoundedCornerShape(12.dp))
                                .clickable(onClick = onOpenGallery),
                            contentAlignment = Alignment.Center
                        ) {
                            if (latestMedia != null) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(latestMedia.uri)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Latest photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.AddPhotoAlternate,
                                    contentDescription = "Open Gallery",
                                    tint = SasukeMuted,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        // Center: Large Shutter Button / Video Record Button
                        if (isVideoMode) {
                            VideoRecordButton(
                                isRecording = isRecording,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    if (isRecording) {
                                        cameraManager.stopRecording()
                                    } else {
                                        cameraManager.startRecording(
                                            hasAudioPermission = hasAudioPermission,
                                            settings = settings,
                                            onRecordingStarted = {
                                                // Started
                                            },
                                            onRecordingSaved = { uri, filename ->
                                                captureSuccessMessage = "Saved $filename"
                                            },
                                            onError = { err ->
                                                Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    }
                                }
                            )
                        } else {
                            ShutterButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    cameraManager.takePhoto(
                                        settings = settings,
                                        onCaptureSuccess = { uri, filename, width, height ->
                                            captureSuccessMessage = "Saved $filename ($width × $height)"
                                        },
                                        onError = { err ->
                                            Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            )
                        }

                        // Right: Import media button
                        Box(
                            modifier = Modifier
                                .testTag("import_media_button")
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(GlassBackground)
                                .border(1.dp, GlassBorder, CircleShape)
                                .clickable {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = "Import Photo/Video",
                                tint = SasukeAmber,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraPermissionPrompt(
    onGrantClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(SasukeSurface)
                .border(1.dp, SasukeBorder, RoundedCornerShape(16.dp))
                .padding(24.dp)
        ) {
            Text(
                text = "SASUKE RETROCAM",
                color = SasukeAmber,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Camera Access Required",
                color = SasukeSilver,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "To capture authentic 0.4 MP retro photos and nostalgic digital videos, Sasuke RetroCam needs access to your device camera.",
                color = SasukeMuted,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onGrantClick,
                colors = ButtonDefaults.buttonColors(containerColor = SasukeAmber),
                modifier = Modifier.testTag("grant_camera_permission_button")
            ) {
                Text(
                    text = "Grant Camera Access",
                    color = SasukeBlack,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
