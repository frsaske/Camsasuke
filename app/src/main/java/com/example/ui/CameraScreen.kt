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
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.model.CameraFlashMode
import com.example.model.CameraLensFacing
import com.example.model.CameraSettings
import com.example.model.MediaItem
import com.example.model.ResolutionOption
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkCardElevated
import com.example.ui.theme.DarkCardPrimary
import com.example.ui.theme.DarkCardSurface
import com.example.ui.theme.RecordRed
import com.example.ui.theme.SlateBlue
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
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
            delay(2400)
            successToast = null
        }
    }

    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = DarkBackground
    ) {
        if (!hasCameraPermission) {
            // Apple-style dark card permission prompt with Sasuke art
            DarkPermissionPrompt(
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

                    // 1. TOP HEADER & CAMERA CONTROLS
                    Column(modifier = Modifier.fillMaxWidth()) {
                        TopControlBar(
                            lensFacing = lensFacing,
                            flashMode = flashMode,
                            onFlashToggle = {
                                val next = when (flashMode) {
                                    CameraFlashMode.OFF -> CameraFlashMode.ON
                                    CameraFlashMode.ON -> CameraFlashMode.AUTO
                                    CameraFlashMode.AUTO -> CameraFlashMode.OFF
                                }
                                cameraManager.setFlashMode(next)
                                onSettingsChanged(settings.copy(flashMode = next))
                            },
                            onSwitchLens = {
                                previewViewRef?.let { pView ->
                                    cameraManager.switchCamera(lifecycleOwner, pView)
                                }
                            },
                            onOpenSettings = onOpenSettings
                        )
                    }

                    // 2. LIVE LOW-RESOLUTION 4:3 VIEWFINDER
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .aspectRatio(3f / 4f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(DarkCardSurface)
                            .border(1.dp, DarkBorder, RoundedCornerShape(20.dp))
                    ) {
                        // Actual CameraX SurfaceProvider targeting low resolution (640x480)
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
                                .pointerInput(Unit) {
                                    detectTapGestures { offset ->
                                        focusPoint = offset
                                        val pView = previewViewRef ?: return@detectTapGestures
                                        val factory = pView.meteringPointFactory
                                        cameraManager.tapToFocus(factory, offset.x, offset.y)
                                    }
                                }
                        )

                        // Top Viewfinder Badges
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Live low-res indicator
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xB30C0E12))
                                    .border(0.8.dp, DarkBorder, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (isVideoMode) "REC 4:3 • ${settings.videoResolution.label}" else "4:3 • ${settings.resolution.label} • RETRO",
                                    color = TextPrimary,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            // Zoom Control Buttons (- / +)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ZoomPill(label = "—", onClick = { cameraManager.zoomOut() }, testTag = "zoom_out_btn")
                                ZoomPill(label = "+", onClick = { cameraManager.zoomIn() }, testTag = "zoom_in_btn")
                            }
                        }

                        // Bottom-right permanent "ѕαѕυкє" watermark badge preview
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0x800C0E12))
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = CameraSettings.PERMANENT_WATERMARK, // "ѕαѕυкє"
                                color = TextPrimary.copy(alpha = 0.70f),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Bottom-left Date Preview if enabled
                        if (settings.dateStampEnabled) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(12.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0x800C0E12))
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "DATE ON",
                                    color = Color(0xFFFFB703),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Tap-to-focus ring
                        focusPoint?.let { pt ->
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .align(Alignment.TopStart)
                                    .padding(
                                        start = (pt.x - 28).coerceAtLeast(0f).dp,
                                        top = (pt.y - 28).coerceAtLeast(0f).dp
                                    )
                                    .border(1.5.dp, Color.White, CircleShape)
                            )
                        }

                        // Screen Flash Overlay
                        if (screenFlash) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.White)
                            )
                        }

                        // Shutter Blackout Flash
                        if (shutterAnim) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xCC000000))
                            )
                        }
                    }

                    // 3. BOTTOM DECK: RESOLUTION SELECTOR, MODE SWITCH, SHUTTER & GALLERY
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Quick 3-Choice Resolution Selector (0.3 MP, 0.4 MP Default, 1.2 MP)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(DarkCardSurface)
                                .border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ResolutionPill(
                                label = "0.3 MP",
                                selected = settings.resolution == ResolutionOption.MP_0_3,
                                onClick = { onSettingsChanged(settings.copy(resolution = ResolutionOption.MP_0_3)) },
                                modifier = Modifier.weight(1f),
                                testTag = "quick_res_0_3"
                            )
                            ResolutionPill(
                                label = "0.4 MP",
                                selected = settings.resolution == ResolutionOption.MP_0_4,
                                onClick = { onSettingsChanged(settings.copy(resolution = ResolutionOption.MP_0_4)) },
                                modifier = Modifier.weight(1f),
                                testTag = "quick_res_0_4"
                            )
                            ResolutionPill(
                                label = "1.2 MP",
                                selected = settings.resolution == ResolutionOption.MP_1_2,
                                onClick = { onSettingsChanged(settings.copy(resolution = ResolutionOption.MP_1_2)) },
                                modifier = Modifier.weight(1f),
                                testTag = "quick_res_1_2"
                            )
                        }

                        // Photo / Video Mode Selector Pill
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 40.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ModeSwitchPill(
                                text = "PHOTO",
                                active = !isVideoMode,
                                onClick = { isVideoMode = false },
                                testTag = "photo_mode_btn"
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            ModeSwitchPill(
                                text = "VIDEO",
                                active = isVideoMode,
                                onClick = { isVideoMode = true },
                                testTag = "video_mode_btn"
                            )
                        }

                        // Toast banner if photo captured
                        AnimatedVisibility(
                            visible = successToast != null,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(DarkCardPrimary)
                                    .padding(vertical = 6.dp, horizontal = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = successToast ?: "",
                                    color = TextPrimary,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Main Shutter Deck: [ GALLERY ] [ SHUTTER / RECORD ] [ IMPORT ]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left: Gallery Thumbnail
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(DarkCardSurface)
                                    .border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
                                    .clickable(onClick = onOpenGallery)
                                    .testTag("gallery_thumbnail_btn")
                            ) {
                                if (latestMedia != null) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(latestMedia.uri)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "Latest capture",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_sasuke_mark),
                                        contentDescription = "Gallery",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(4.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                    )
                                }
                            }

                            // Center: Shutter or Video Record Button
                            if (isVideoMode) {
                                // Video Record Button
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(76.dp)
                                        .clip(CircleShape)
                                        .background(DarkCardSurface)
                                        .border(2.5.dp, if (isRecording) RecordRed else TextPrimary, CircleShape)
                                        .clickable {
                                            if (isRecording) {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                cameraManager.stopRecording()
                                            } else {
                                                if (!hasAudioPermission) {
                                                    audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                                }
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                cameraManager.startRecording(
                                                    hasAudioPermission = hasAudioPermission,
                                                    settings = settings,
                                                    onRecordingStarted = {
                                                        successToast = "Recording clip..."
                                                    },
                                                    onRecordingSaved = { uri, filename ->
                                                        successToast = "Saved $filename"
                                                    },
                                                    onError = { err ->
                                                        Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                                    }
                                                )
                                            }
                                        }
                                        .testTag("record_video_button")
                                ) {
                                    if (isRecording) {
                                        // Red square
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(RecordRed)
                                        )
                                    } else {
                                        // Red circle
                                        Box(
                                            modifier = Modifier
                                                .size(54.dp)
                                                .clip(CircleShape)
                                                .background(RecordRed)
                                        )
                                    }
                                }
                            } else {
                                // Photo Shutter Button (Apple-style dual circle)
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(76.dp)
                                        .clip(CircleShape)
                                        .background(DarkCardSurface)
                                        .border(3.dp, Color.White, CircleShape)
                                        .clickable {
                                            if (!isCapturingPhoto) {
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
                                        .testTag("shutter_button")
                                ) {
                                    if (isCapturingPhoto) {
                                        CircularProgressIndicator(
                                            color = SlateBlue,
                                            modifier = Modifier.size(36.dp),
                                            strokeWidth = 3.dp
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(60.dp)
                                                .clip(CircleShape)
                                                .background(Color.White)
                                        )
                                    }
                                }
                            }

                            // Right: Import Image/Video
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(DarkCardSurface)
                                    .border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
                                    .clickable {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                        )
                                    }
                                    .testTag("import_media_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddPhotoAlternate,
                                    contentDescription = "Import",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        // Live recording timer if recording
                        if (isRecording) {
                            val mins = recordingSeconds / 60
                            val secs = recordingSeconds % 60
                            Text(
                                text = String.format("REC %02d:%02d", mins, secs),
                                color = RecordRed,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopControlBar(
    lensFacing: CameraLensFacing,
    flashMode: CameraFlashMode,
    onFlashToggle: () -> Unit,
    onSwitchLens: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App Identity with Sasuke Mark
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_sasuke_mark),
                contentDescription = "Sasuke Mark",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .border(1.2.dp, SlateBlue, CircleShape)
            )
            Column {
                Text(
                    text = "SASUKE",
                    color = TextPrimary,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "RETROCAM",
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    letterSpacing = 1.sp
                )
            }
        }

        // Action icons: Flash, Camera flip, Settings
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Flash toggle
            IconButton(
                onClick = onFlashToggle,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(DarkCardSurface)
                    .border(1.dp, DarkBorder, CircleShape)
                    .testTag("flash_toggle_btn")
            ) {
                val icon = when (flashMode) {
                    CameraFlashMode.OFF -> Icons.Default.FlashOff
                    CameraFlashMode.ON -> Icons.Default.FlashOn
                    CameraFlashMode.AUTO -> Icons.Default.FlashAuto
                }
                Icon(
                    imageVector = icon,
                    contentDescription = "Flash",
                    tint = if (flashMode != CameraFlashMode.OFF) Color(0xFFFFB703) else TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Flip lens
            IconButton(
                onClick = onSwitchLens,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(DarkCardSurface)
                    .border(1.dp, DarkBorder, CircleShape)
                    .testTag("flip_camera_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Cameraswitch,
                    contentDescription = "Switch Camera",
                    tint = TextPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Settings
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(DarkCardSurface)
                    .border(1.dp, DarkBorder, CircleShape)
                    .testTag("settings_btn")
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
}

@Composable
private fun ZoomPill(
    label: String,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(Color(0xB30C0E12))
            .border(0.8.dp, DarkBorder, CircleShape)
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Text(
            text = label,
            color = TextPrimary,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun ResolutionPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) DarkCardPrimary else Color.Transparent)
            .border(
                1.dp,
                if (selected) SlateBlue else Color.Transparent,
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Text(
            text = label,
            color = if (selected) TextPrimary else TextSecondary,
            fontFamily = FontFamily.Monospace,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun ModeSwitchPill(
    text: String,
    active: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Text(
        text = text,
        color = if (active) Color.White else TextMuted,
        fontFamily = FontFamily.Monospace,
        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
        fontSize = 13.sp,
        letterSpacing = 1.2.sp,
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag(testTag)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

@Composable
private fun DarkPermissionPrompt(
    onGrantClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(DarkCardSurface)
                .border(1.dp, DarkBorder, RoundedCornerShape(24.dp))
                .padding(28.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_sasuke_mark),
                contentDescription = "Sasuke",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .border(2.dp, SlateBlue, CircleShape)
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "SASUKE RETROCAM",
                color = TextPrimary,
                fontSize = 18.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "AUTHENTIC NOSTALGIC CAMERA",
                color = TextSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "To capture vintage low-resolution photos and retro videos with the permanent Sasuke signature, enable camera permission.",
                color = TextMuted,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(DarkCardPrimary)
                    .border(1.dp, SlateBlue, RoundedCornerShape(14.dp))
                    .clickable(onClick = onGrantClick)
                    .testTag("grant_camera_permission_button")
            ) {
                Text(
                    text = "ALLOW CAMERA ACCESS",
                    color = TextPrimary,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}
