package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.MediaController
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.camera.MediaStoreManager
import com.example.camera.RetroImageProcessor
import com.example.model.AspectRatioOption
import com.example.model.CameraSettings
import com.example.model.ResolutionOption
import com.example.model.RetroPreset
import com.example.ui.theme.GlassBackground
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.SasukeAmber
import com.example.ui.theme.SasukeBlack
import com.example.ui.theme.SasukeBorder
import com.example.ui.theme.SasukeCharcoal
import com.example.ui.theme.SasukeMuted
import com.example.ui.theme.SasukeSilver
import com.example.ui.theme.SasukeSurface
import com.example.ui.theme.SasukeSurfaceVariant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

@Composable
fun ImportEditScreen(
    mediaUri: Uri,
    isVideo: Boolean,
    currentSettings: CameraSettings,
    mediaStoreManager: MediaStoreManager,
    onSaveSuccess: (Uri) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }

    // Adjustable retro parameters
    var selectedPreset by remember { mutableStateOf(currentSettings.preset) }
    var filterIntensity by remember { mutableFloatStateOf(currentSettings.filterIntensity) }
    var grainAmount by remember { mutableFloatStateOf(currentSettings.grainAmount) }
    var selectedResolution by remember { mutableStateOf(currentSettings.resolution) }
    var selectedAspect by remember { mutableStateOf(currentSettings.aspectRatio) }
    var dateStampEnabled by remember { mutableStateOf(false) }

    // Load original bitmap if photo
    LaunchedEffect(mediaUri) {
        if (!isVideo) {
            withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(mediaUri)?.use { input ->
                        val bmp = BitmapFactory.decodeStream(input)
                        originalBitmap = bmp
                    }
                } catch (e: Exception) {
                    // Ignore
                } finally {
                    isLoading = false
                }
            }
        } else {
            isLoading = false
        }
    }

    // Recompute preview bitmap whenever filters change
    LaunchedEffect(originalBitmap, selectedPreset, filterIntensity, grainAmount, selectedResolution, selectedAspect, dateStampEnabled) {
        val src = originalBitmap ?: return@LaunchedEffect
        withContext(Dispatchers.Default) {
            val settings = currentSettings.copy(
                preset = selectedPreset,
                filterIntensity = filterIntensity,
                grainAmount = grainAmount,
                resolution = selectedResolution,
                aspectRatio = selectedAspect,
                dateStampEnabled = dateStampEnabled
            )
            val processed = RetroImageProcessor.processImage(src, settings)
            withContext(Dispatchers.Main) {
                previewBitmap = processed
            }
        }
    }

    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = SasukeBlack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = topPadding, bottom = bottomPadding)
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .testTag("import_back_button")
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(GlassBackground)
                            .border(1.dp, GlassBorder, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = SasukeSilver
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isVideo) "IMPORT VIDEO" else "RETRO PHOTO LAB",
                            color = SasukeSilver,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                        if (!isVideo && previewBitmap != null) {
                            val bmp = previewBitmap!!
                            Text(
                                text = "${bmp.width} × ${bmp.height} • ${selectedPreset.title}",
                                color = SasukeAmber,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Save button
                Button(
                    onClick = {
                        isSaving = true
                        coroutineScope.launch {
                            if (!isVideo) {
                                val bmp = previewBitmap ?: originalBitmap
                                if (bmp != null) {
                                    val filename = mediaStoreManager.getNextPhotoFilename()
                                    val result = mediaStoreManager.saveBitmap(
                                        bitmap = bmp,
                                        quality = currentSettings.jpegQuality,
                                        customFilename = filename
                                    )
                                    isSaving = false
                                    result.onSuccess { uri ->
                                        Toast.makeText(context, "Saved $filename to Sasuke RetroCam", Toast.LENGTH_SHORT).show()
                                        onSaveSuccess(uri)
                                    }.onFailure { err ->
                                        Toast.makeText(context, "Save error: ${err.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                // Save imported video copy to Sasuke RetroCam
                                withContext(Dispatchers.IO) {
                                    try {
                                        val tempFile = File(context.cacheDir, "import_video_${System.currentTimeMillis()}.mp4")
                                        context.contentResolver.openInputStream(mediaUri)?.use { input ->
                                            tempFile.outputStream().use { out ->
                                                input.copyTo(out)
                                            }
                                        }
                                        val result = mediaStoreManager.saveVideoFile(tempFile)
                                        withContext(Dispatchers.Main) {
                                            isSaving = false
                                            result.onSuccess { uri ->
                                                Toast.makeText(context, "Saved video copy to Sasuke RetroCam", Toast.LENGTH_SHORT).show()
                                                onSaveSuccess(uri)
                                            }.onFailure { err ->
                                                Toast.makeText(context, "Save error: ${err.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) {
                                            isSaving = false
                                            Toast.makeText(context, "Video import failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        }
                    },
                    enabled = !isLoading && !isSaving,
                    colors = ButtonDefaults.buttonColors(containerColor = SasukeAmber),
                    modifier = Modifier.testTag("save_retro_copy_button")
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            color = SasukeBlack,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = null,
                            tint = SasukeBlack,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Save Copy",
                            color = SasukeBlack,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Media Preview Center Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SasukeCharcoal),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = SasukeAmber)
                } else if (isVideo) {
                    AndroidView(
                        factory = { ctx ->
                            VideoView(ctx).apply {
                                setVideoURI(mediaUri)
                                val controller = MediaController(ctx)
                                controller.setAnchorView(this)
                                setMediaController(controller)
                                setOnPreparedListener { mp ->
                                    mp.isLooping = true
                                    start()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    val bmp = previewBitmap ?: originalBitmap
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Retro Edited Preview",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(text = "Failed to load image", color = SasukeMuted)
                    }
                }
            }

            // Controls for photo filtering
            if (!isVideo) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SasukeSurface)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    // Presets Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RetroPreset.values().forEach { preset ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (selectedPreset == preset) SasukeAmber else SasukeSurfaceVariant)
                                    .clickable { selectedPreset = preset }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = preset.title,
                                    color = if (selectedPreset == preset) SasukeBlack else SasukeSilver,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick toggles: Resolution, Aspect Ratio, Date Stamp
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Resolution chip
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(SasukeSurfaceVariant)
                                .clickable {
                                    selectedResolution = when (selectedResolution) {
                                        ResolutionOption.RETRO_0_4_MP -> ResolutionOption.LOW_1_2_MP
                                        ResolutionOption.LOW_1_2_MP -> ResolutionOption.STANDARD
                                        ResolutionOption.STANDARD -> ResolutionOption.RETRO_0_4_MP
                                    }
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = selectedResolution.label,
                                color = SasukeAmber,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Aspect Ratio chip
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(SasukeSurfaceVariant)
                                .clickable {
                                    selectedAspect = when (selectedAspect) {
                                        AspectRatioOption.RATIO_4_3 -> AspectRatioOption.RATIO_16_9
                                        AspectRatioOption.RATIO_16_9 -> AspectRatioOption.RATIO_1_1
                                        AspectRatioOption.RATIO_1_1 -> AspectRatioOption.RATIO_4_3
                                    }
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = selectedAspect.label,
                                color = SasukeSilver,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Date stamp toggle
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Date Stamp",
                                color = SasukeSilver,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Switch(
                                checked = dateStampEnabled,
                                onCheckedChange = { dateStampEnabled = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = SasukeBlack,
                                    checkedTrackColor = SasukeAmber
                                ),
                                modifier = Modifier.size(width = 38.dp, height = 24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Intensity slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Effect",
                            color = SasukeMuted,
                            fontSize = 11.sp,
                            modifier = Modifier.width(42.dp)
                        )
                        Slider(
                            value = filterIntensity,
                            onValueChange = { filterIntensity = it },
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(
                                thumbColor = SasukeAmber,
                                activeTrackColor = SasukeAmber
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${(filterIntensity * 100).toInt()}%",
                            color = SasukeAmber,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.width(36.dp)
                        )
                    }
                }
            } else {
                // Video info card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SasukeSurface)
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Import Video to Sasuke RetroCam",
                        color = SasukeSilver,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Tap 'Save Copy' above to add this video to your Sasuke RetroCam album as Sasuke-Video-XXX.mp4.",
                        color = SasukeMuted,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
