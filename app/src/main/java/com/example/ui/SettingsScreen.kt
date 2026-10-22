package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AspectRatioOption
import com.example.model.CameraFlashMode
import com.example.model.CameraLensFacing
import com.example.model.CameraSettings
import com.example.model.DateStampFormat
import com.example.model.DateStampStyle
import com.example.model.ResolutionOption
import com.example.model.RetroPreset
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
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    settings: CameraSettings,
    totalAlbumCount: Int,
    nextFilenamePreview: String,
    onSettingsChanged: (CameraSettings) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

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
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("settings_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = SasukeSilver
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text(
                        text = "SETTINGS",
                        color = SasukeSilver,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "Sasuke RetroCam Configuration",
                        color = SasukeMuted,
                        fontSize = 12.sp
                    )
                }
            }

            HorizontalDivider(color = SasukeBorder)

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { Spacer(modifier = Modifier.height(4.dp)) }

                // SECTION: CAMERA
                item {
                    SettingsSectionHeader(title = "CAMERA", icon = Icons.Default.CameraAlt)
                }

                item {
                    SettingsCard {
                        // Default Resolution
                        Text(
                            text = "Target Photo Resolution",
                            color = SasukeSilver,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Retro 0.4 MP creates authentic vintage pixel dimensions (~730 × 548).",
                            color = SasukeMuted,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ResolutionOption.values().forEach { res ->
                                SelectableChip(
                                    label = res.label,
                                    selected = settings.resolution == res,
                                    onClick = { onSettingsChanged(settings.copy(resolution = res)) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Aspect Ratio
                        Text(
                            text = "Default Aspect Ratio",
                            color = SasukeSilver,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AspectRatioOption.values().forEach { aspect ->
                                SelectableChip(
                                    label = aspect.label,
                                    selected = settings.aspectRatio == aspect,
                                    onClick = { onSettingsChanged(settings.copy(aspectRatio = aspect)) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Default Camera Lens
                        Text(
                            text = "Default Camera",
                            color = SasukeSilver,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SelectableChip(
                                label = "Rear Camera",
                                selected = settings.defaultLens == CameraLensFacing.BACK,
                                onClick = { onSettingsChanged(settings.copy(defaultLens = CameraLensFacing.BACK)) },
                                modifier = Modifier.weight(1f)
                            )
                            SelectableChip(
                                label = "Front Selfie",
                                selected = settings.defaultLens == CameraLensFacing.FRONT,
                                onClick = { onSettingsChanged(settings.copy(defaultLens = CameraLensFacing.FRONT)) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Selfie Mirroring Options
                        SettingToggleRow(
                            title = "Save Selfie Mirrored",
                            subtitle = "Mirror photos taken with the front camera",
                            checked = settings.saveSelfieMirrored,
                            onCheckedChange = { onSettingsChanged(settings.copy(saveSelfieMirrored = it)) }
                        )

                        SettingToggleRow(
                            title = "Grid Lines",
                            subtitle = "Show rule-of-thirds composition grid",
                            checked = settings.showGrid,
                            onCheckedChange = { onSettingsChanged(settings.copy(showGrid = it)) }
                        )

                        SettingToggleRow(
                            title = "Vintage Viewfinder HUD",
                            subtitle = "Display vintage brackets and camera telemetry",
                            checked = settings.showVintageHud,
                            onCheckedChange = { onSettingsChanged(settings.copy(showVintageHud = it)) }
                        )
                    }
                }

                // SECTION: PHOTO EFFECTS & PRESETS
                item {
                    SettingsSectionHeader(title = "RETRO PHOTO EFFECTS", icon = Icons.Default.Photo)
                }

                item {
                    SettingsCard {
                        // Default Preset Selection
                        Text(
                            text = "Retro Preset",
                            color = SasukeSilver,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = settings.preset.description,
                            color = SasukeAmber,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            RetroPreset.values().forEach { preset ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (settings.preset == preset) SasukeSurfaceVariant else Color.Transparent)
                                        .border(1.dp, if (settings.preset == preset) SasukeAmber else SasukeBorder, RoundedCornerShape(8.dp))
                                        .clickable { onSettingsChanged(settings.copy(preset = preset)) }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = preset.title,
                                            color = if (settings.preset == preset) SasukeAmber else SasukeSilver,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    if (settings.preset == preset) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = SasukeAmber,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Filter Intensity Slider
                        Text(
                            text = "Filter Intensity: ${(settings.filterIntensity * 100).roundToInt()}%",
                            color = SasukeSilver,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Slider(
                            value = settings.filterIntensity,
                            onValueChange = { onSettingsChanged(settings.copy(filterIntensity = it)) },
                            valueRange = 0.1f..1.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = SasukeAmber,
                                activeTrackColor = SasukeAmber,
                                inactiveTrackColor = SasukeBorder
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Grain Slider
                        Text(
                            text = "Digital Sensor Grain: ${(settings.grainAmount * 100).roundToInt()}%",
                            color = SasukeSilver,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Slider(
                            value = settings.grainAmount,
                            onValueChange = { onSettingsChanged(settings.copy(grainAmount = it)) },
                            valueRange = 0.0f..0.8f,
                            colors = SliderDefaults.colors(
                                thumbColor = SasukeAmber,
                                activeTrackColor = SasukeAmber,
                                inactiveTrackColor = SasukeBorder
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Date Stamp Section
                        SettingToggleRow(
                            title = "Date Stamp",
                            subtitle = "Burn vintage digital timestamp into photo bottom-left corner",
                            checked = settings.dateStampEnabled,
                            onCheckedChange = { onSettingsChanged(settings.copy(dateStampEnabled = it)) }
                        )

                        if (settings.dateStampEnabled) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Date Stamp Color",
                                color = SasukeSilver,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                DateStampStyle.values().forEach { style ->
                                    SelectableChip(
                                        label = style.label,
                                        selected = settings.dateStampStyle == style,
                                        onClick = { onSettingsChanged(settings.copy(dateStampStyle = style)) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Date Format",
                                color = SasukeSilver,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                DateStampFormat.values().forEach { format ->
                                    SelectableChip(
                                        label = "${format.label} (${format.pattern})",
                                        selected = settings.dateStampFormat == format,
                                        onClick = { onSettingsChanged(settings.copy(dateStampFormat = format)) }
                                    )
                                }
                            }
                        }
                    }
                }

                // SECTION: VIDEO
                item {
                    SettingsSectionHeader(title = "VIDEO RECORDING", icon = Icons.Default.Videocam)
                }

                item {
                    SettingsCard {
                        SettingToggleRow(
                            title = "Record Audio",
                            subtitle = "Include nostalgic microphone audio with video clips",
                            checked = settings.recordAudio,
                            onCheckedChange = { onSettingsChanged(settings.copy(recordAudio = it)) }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Video Resolution",
                            color = SasukeSilver,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Standard supported camera hardware video resolutions",
                            color = SasukeMuted,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("HD 720p", "SD 480p").forEach { quality ->
                                SelectableChip(
                                    label = quality,
                                    selected = settings.videoQuality == quality,
                                    onClick = { onSettingsChanged(settings.copy(videoQuality = quality)) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // SECTION: STORAGE
                item {
                    SettingsSectionHeader(title = "STORAGE & GALLERY", icon = Icons.Default.Storage)
                }

                item {
                    SettingsCard {
                        InfoRow(label = "Destination Album", value = "DCIM/Sasuke RetroCam")
                        InfoRow(label = "Next Photo Name", value = nextFilenamePreview)
                        InfoRow(label = "Next Video Name", value = "Sasuke-Video-XXX.mp4")
                        InfoRow(label = "Album Media Count", value = "$totalAlbumCount items")
                        InfoRow(label = "Storage Access", value = "Direct MediaStore (Scoped)")
                    }
                }

                // SECTION: ABOUT
                item {
                    SettingsSectionHeader(title = "ABOUT SASUKE RETROCAM", icon = Icons.Default.Info)
                }

                item {
                    SettingsCard {
                        InfoRow(label = "App Name", value = "Sasuke RetroCam")
                        InfoRow(label = "Version", value = "1.0 (MVP)")
                        InfoRow(label = "Brand", value = "Sasuke")
                        InfoRow(label = "Developer", value = "Sasuke")
                        InfoRow(label = "Contact", value = "Sasuke.winx@gmail.com")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Privacy Guarantee:\nThis is a private personal-use camera application. No user accounts, no logins, no cloud tracking, and no telemetry. All photos and videos stay strictly local on your device.",
                            color = SasukeMuted,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = SasukeAmber,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            color = SasukeAmber,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SasukeSurface)
            .border(1.dp, SasukeBorder, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        content()
    }
}

@Composable
private fun SelectableChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) SasukeAmber else SasukeSurfaceVariant)
            .border(1.dp, if (selected) SasukeAmber else SasukeBorder, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) SasukeBlack else SasukeSilver,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(
                text = title,
                color = SasukeSilver,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                color = SasukeMuted,
                fontSize = 12.sp
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = SasukeBlack,
                checkedTrackColor = SasukeAmber,
                uncheckedThumbColor = SasukeMuted,
                uncheckedTrackColor = SasukeSurfaceVariant
            )
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = SasukeMuted, fontSize = 12.sp)
        Text(
            text = value,
            color = SasukeSilver,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold
        )
    }
}
