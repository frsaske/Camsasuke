package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.CameraSettings
import com.example.model.ResolutionOption
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkCardElevated
import com.example.ui.theme.DarkCardPrimary
import com.example.ui.theme.DarkCardSurface
import com.example.ui.theme.SlateBlue
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    settings: CameraSettings,
    onSettingsChanged: (CameraSettings) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val scrollState = rememberScrollState()

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
            // Top Navigation Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .testTag("settings_back_btn")
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

                Spacer(modifier = Modifier.width(14.dp))

                Text(
                    text = "SETTINGS",
                    color = TextPrimary,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    letterSpacing = 1.2.sp
                )
            }

            HorizontalDivider(
                color = DarkBorder,
                thickness = 1.dp
            )

            // Settings Body with Apple-like grouped cards
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // CARD 1: TARGET RESOLUTION (Strictly 3 options: 0.4 MP default, 0.3 MP, 1.2 MP)
                SectionTitle("PHOTO RESOLUTION (4:3)")
                GroupedCard {
                    ResolutionChoiceRow(
                        title = "0.4 MP (Default)",
                        subtitle = "730×548 • Authentic early 2000s compact camera look",
                        isSelected = settings.resolution == ResolutionOption.MP_0_4,
                        onClick = { onSettingsChanged(settings.copy(resolution = ResolutionOption.MP_0_4)) },
                        testTag = "res_0_4_option"
                    )

                    DividerLine()

                    ResolutionChoiceRow(
                        title = "0.3 MP (Ultra Vintage)",
                        subtitle = "640×480 • Lo-fi VGA flip phone & early webcam sensor",
                        isSelected = settings.resolution == ResolutionOption.MP_0_3,
                        onClick = { onSettingsChanged(settings.copy(resolution = ResolutionOption.MP_0_3)) },
                        testTag = "res_0_3_option"
                    )

                    DividerLine()

                    ResolutionChoiceRow(
                        title = "1.2 MP (Hi-Fi Digital)",
                        subtitle = "1264×948 • Crisp nostalgic 2004 CCD digital camera",
                        isSelected = settings.resolution == ResolutionOption.MP_1_2,
                        onClick = { onSettingsChanged(settings.copy(resolution = ResolutionOption.MP_1_2)) },
                        testTag = "res_1_2_option"
                    )
                }

                // CARD 2: PHOTO PREFERENCES & WATERMARK
                SectionTitle("PHOTO PREFERENCES")
                GroupedCard {
                    // Date Stamp
                    SwitchRow(
                        title = "DATE STAMP",
                        subtitle = "Burn vintage amber LCD date (dd.MM.yyyy) in bottom-left corner",
                        checked = settings.dateStampEnabled,
                        onCheckedChange = { onSettingsChanged(settings.copy(dateStampEnabled = it)) },
                        testTag = "date_stamp_switch"
                    )

                    DividerLine()

                    // Watermark info (Permanent, no disable option as requested)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp, horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                text = "WATERMARK",
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Permanent signature burned in bottom-right corner",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkCardPrimary)
                                .border(1.dp, SlateBlue, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = CameraSettings.PERMANENT_WATERMARK, // "ѕαѕυкє"
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    DividerLine()

                    // Shutter Sound
                    SwitchRow(
                        title = "SHUTTER SOUND",
                        subtitle = "Play retro mechanical click and haptic feedback on capture",
                        checked = settings.soundEnabled,
                        onCheckedChange = { onSettingsChanged(settings.copy(soundEnabled = it)) },
                        testTag = "sound_switch"
                    )
                }

                // CARD 3: VIDEO RECORDING
                SectionTitle("VIDEO RECORDING")
                GroupedCard {
                    // Video Resolution: 0.4 MP (default) and 1.2 MP
                    ResolutionChoiceRow(
                        title = "0.4 MP Video (SD Default)",
                        subtitle = "Authentic nostalgic low-resolution video recording",
                        isSelected = settings.videoResolution == ResolutionOption.MP_0_4,
                        onClick = { onSettingsChanged(settings.copy(videoResolution = ResolutionOption.MP_0_4)) },
                        testTag = "video_res_0_4"
                    )

                    DividerLine()

                    ResolutionChoiceRow(
                        title = "1.2 MP Video (HD)",
                        subtitle = "Slightly higher definition nostalgic video",
                        isSelected = settings.videoResolution == ResolutionOption.MP_1_2,
                        onClick = { onSettingsChanged(settings.copy(videoResolution = ResolutionOption.MP_1_2)) },
                        testTag = "video_res_1_2"
                    )

                    DividerLine()

                    // Audio Recording
                    SwitchRow(
                        title = "RECORD AUDIO",
                        subtitle = "Capture microphone sound with vintage video recordings",
                        checked = settings.recordAudio,
                        onCheckedChange = { onSettingsChanged(settings.copy(recordAudio = it)) },
                        testTag = "audio_record_switch"
                    )
                }

                // CARD 4: FEEDBACK BUTTON (Directly redirects to Gmail)
                SectionTitle("FEEDBACK")
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkCardPrimary)
                        .border(1.dp, SlateBlue, RoundedCornerShape(16.dp))
                        .clickable { openFeedbackMail(context) }
                        .testTag("feedback_button")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Send Feedback",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "SEND FEEDBACK (GMAIL)",
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            letterSpacing = 1.sp
                        )
                    }
                }

                // CARD 5: ABOUT SASUKE RETROCAM
                SectionTitle("ABOUT")
                GroupedCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_sasuke_mark),
                            contentDescription = "Sasuke",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, SlateBlue, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Sasuke RetroCam",
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Developer: Sasuke",
                                color = TextSecondary,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "Sasuke.winx@gmail.com",
                                color = SlateBlue,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                modifier = Modifier.clickable { openFeedbackMail(context) }
                            )
                        }
                    }

                    DividerLine()

                    // Privacy Guarantee
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Privacy",
                            tint = SlateBlue,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Privacy Guarantee",
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "100% on-device processing. No photos, videos, or personal data ever leave your device.",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        color = TextMuted,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
    )
}

@Composable
private fun GroupedCard(
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkCardSurface)
            .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
    ) {
        content()
    }
}

@Composable
private fun DividerLine() {
    HorizontalDivider(
        color = DarkBorder,
        thickness = 0.8.dp
    )
}

@Composable
private fun ResolutionChoiceRow(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                color = if (isSelected) TextPrimary else TextSecondary,
                fontFamily = FontFamily.Monospace,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 13.sp
            )
            Text(
                text = subtitle,
                color = TextMuted,
                fontSize = 11.sp
            )
        }
        if (isSelected) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(SlateBlue)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                color = TextPrimary,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 11.sp
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = SlateBlue,
                uncheckedThumbColor = Color(0xFF64748B),
                uncheckedTrackColor = Color(0xFF1E232E)
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}

private fun openFeedbackMail(context: Context) {
    try {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:Sasuke.winx@gmail.com?subject=Contact%20for%20RetroCamSasuke")
        }
        context.startActivity(Intent.createChooser(intent, "Contact Sasuke via Gmail"))
    } catch (e: Exception) {
        Toast.makeText(context, "Contact: Sasuke.winx@gmail.com", Toast.LENGTH_LONG).show()
    }
}
