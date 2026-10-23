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
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.example.model.CameraSettings
import com.example.model.ResolutionOption
import com.example.model.RetroPreset
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkCardElevated
import com.example.ui.theme.DarkCardPrimary
import com.example.ui.theme.DarkCardSurface
import com.example.ui.theme.SlateBlue
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

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
    var selectedResolution by remember { mutableStateOf(currentSettings.resolution) }
    var dateStampEnabled by remember { mutableStateOf(currentSettings.dateStampEnabled) }

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
    LaunchedEffect(originalBitmap, selectedPreset, selectedResolution, dateStampEnabled) {
        val src = originalBitmap ?: return@LaunchedEffect
        withContext(Dispatchers.Default) {
            val settings = currentSettings.copy(
                preset = selectedPreset,
                resolution = selectedResolution,
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
        color = DarkBackground
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
                    .padding(horizontal = 14.dp, vertical = 10.dp),
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
                            .background(DarkCardSurface)
                            .border(1.dp, DarkBorder, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isVideo) "IMPORT VIDEO" else "RETRO PHOTO LAB",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                        if (!isVideo && previewBitmap != null) {
                            val bmp = previewBitmap!!
                            Text(
                                text = "${bmp.width} × ${bmp.height} • ${selectedPreset.title}",
                                color = TextSecondary,
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
                                        Toast.makeText(context, "Saved $filename", Toast.LENGTH_SHORT).show()
                                        onSaveSuccess(uri)
                                    }.onFailure { err ->
                                        Toast.makeText(context, "Save error: ${err.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                // Save imported video copy
                                withContext(Dispatchers.IO) {
                                    try {
                                        val tempFile = File(context.cacheDir, "imported_${System.currentTimeMillis()}.mp4")
                                        context.contentResolver.openInputStream(mediaUri)?.use { input ->
                                            tempFile.outputStream().use { output ->
                                                input.copyTo(output)
                                            }
                                        }
                                        val saveResult = mediaStoreManager.saveVideoFile(tempFile)
                                        isSaving = false
                                        saveResult.onSuccess { uri ->
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(context, "Imported video saved", Toast.LENGTH_SHORT).show()
                                                onSaveSuccess(uri)
                                            }
                                        }.onFailure { err ->
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(context, "Import failed: ${err.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    } catch (e: Exception) {
                                        isSaving = false
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        }
                    },
                    enabled = !isLoading && !isSaving,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkCardPrimary,
                        contentColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.border(1.dp, SlateBlue, RoundedCornerShape(12.dp))
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            color = TextPrimary,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("SAVE", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Preview Area (Fixed 4:3)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkCardSurface)
                    .border(1.dp, DarkBorder, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = SlateBlue)
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
                            contentDescription = "Retro Preview",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text("Failed to load image", color = TextMuted)
                    }
                }
            }

            // Bottom Filter & Setting Controls (for photos)
            if (!isVideo) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Preset Selector
                    Text(
                        text = "RETRO PRESET",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RetroPreset.values().forEach { preset ->
                            val isSelected = selectedPreset == preset
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) DarkCardPrimary else DarkCardSurface)
                                    .border(1.dp, if (isSelected) SlateBlue else DarkBorder, RoundedCornerShape(10.dp))
                                    .clickable { selectedPreset = preset }
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = preset.title,
                                    color = if (isSelected) TextPrimary else TextSecondary,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Resolution Pills (0.3, 0.4, 1.2)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkCardSurface)
                            .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ResolutionOption.values().forEach { res ->
                            val isSelected = selectedResolution == res
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) DarkCardPrimary else Color.Transparent)
                                    .border(1.dp, if (isSelected) SlateBlue else Color.Transparent, RoundedCornerShape(8.dp))
                                    .clickable { selectedResolution = res }
                            ) {
                                Text(
                                    text = res.label,
                                    color = if (isSelected) TextPrimary else TextSecondary,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Quick Date Stamp Toggle Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DATE STAMP",
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Switch(
                            checked = dateStampEnabled,
                            onCheckedChange = { dateStampEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = SlateBlue,
                                uncheckedThumbColor = Color(0xFF64748B),
                                uncheckedTrackColor = Color(0xFF1E232E)
                            )
                        )
                    }
                }
            }
        }
    }
}
