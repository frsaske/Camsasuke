package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.ResolutionOption
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// High-end Stealth Cyber Retro Palette
val DarkCanvas = Color(0xFF09090C)
val SurfaceCard = Color(0xFF14141A)
val SurfaceElevated = Color(0xFF1E1E28)
val GlassFrosted = Color(0x80181824)
val GlassBorderRefined = Color(0x2EFFFFFF)
val GlassHighlight = Color(0x40FFFFFF)

val AccentAmber = Color(0xFFFFB703)
val AccentCyan = Color(0xFF00E5FF)
val AccentRed = Color(0xFFFF3B30)
val TextPrimary = Color(0xFFF3F4F6)
val TextMuted = Color(0xFF9CA3AF)

/**
 * Top Cyber-Retro Status Header: live time, Sasuke avatar, antenna & battery
 */
@Composable
fun LuxuryRetroHeader(
    modifier: Modifier = Modifier
) {
    var currentTime by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        while (true) {
            currentTime = sdf.format(Date())
            delay(1000)
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(38.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left: Digital Clock & Vintage Envelope
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x33000000))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = currentTime.ifEmpty { "00:35" },
                color = TextPrimary,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "✉",
                color = AccentAmber,
                fontSize = 11.sp
            )
        }

        // Center: Sasuke Signature Badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0x40000000))
                .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(14.dp))
                .padding(horizontal = 10.dp, vertical = 3.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_sasuke_mark),
                contentDescription = "Sasuke Icon",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .border(1.dp, AccentAmber, CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "SASUKE",
                color = TextPrimary,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 1.5.sp
            )
        }

        // Right: Telemetry (Signal & Battery)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x33000000))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "Ψ ıllı",
                color = TextPrimary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.width(6.dp))
            // Battery icon
            Box(
                modifier = Modifier
                    .width(18.dp)
                    .height(9.dp)
                    .border(1.dp, TextPrimary, RoundedCornerShape(2.dp))
                    .padding(1.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF34D399), RoundedCornerShape(1.dp))
                )
            }
        }
    }
}

/**
 * Tactile, human-crafted glass capsule button.
 */
@Composable
fun LuxuryGlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    accentColor: Color = AccentAmber,
    testTag: String = ""
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "press_scale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .testTag(testTag)
            .scale(scale)
            .height(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (active) Brush.linearGradient(
                    colors = listOf(accentColor.copy(alpha = 0.25f), accentColor.copy(alpha = 0.12f))
                ) else Brush.linearGradient(
                    colors = listOf(Color(0x66222230), Color(0x40161622))
                )
            )
            .border(
                1.dp,
                if (active) accentColor.copy(alpha = 0.8f) else GlassBorderRefined,
                RoundedCornerShape(18.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = accentColor),
                onClick = onClick
            )
            .padding(horizontal = 14.dp)
    ) {
        Text(
            text = text,
            color = if (active) accentColor else TextPrimary,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
        )
    }
}

/**
 * Interactive Horizontal MP Selector Strip (0.3 • 0.4 • 0.5 • 0.9 • 1 • 2 • 3 MP).
 */
@Composable
fun LuxuryMpSelectorStrip(
    selectedResolution: ResolutionOption,
    onSelect: (ResolutionOption) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0x730F0F16))
            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(18.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ResolutionOption.values().forEach { res ->
            val isSelected = selectedResolution == res
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .height(30.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(
                        if (isSelected) AccentAmber else Color.Transparent
                    )
                    .clickable { onSelect(res) }
            ) {
                Text(
                    text = res.label.replace(" MP", ""),
                    color = if (isSelected) Color.Black else TextMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                )
            }
        }
    }
}

/**
 * High-end tactile Circular Shutter Button with multi-stage metal machining rings.
 */
@Composable
fun LuxuryShutterButton(
    isVideoMode: Boolean,
    isRecording: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = tween(durationMillis = 90),
        label = "shutter_press"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .testTag("shutter_button")
            .scale(scale)
            .size(80.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF2E2E3C), Color(0xFF14141C), Color(0xFF0C0C12))
                )
            )
            .border(
                2.5.dp,
                if (isVideoMode) AccentRed.copy(alpha = 0.9f) else Color(0x66FFFFFF),
                CircleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, radius = 40.dp, color = if (isVideoMode) AccentRed else Color.White),
                enabled = enabled,
                onClick = onClick
            )
    ) {
        // Inner tactile disc
        if (isVideoMode) {
            if (isRecording) {
                // Recording stop square
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(AccentRed)
                )
            } else {
                // Video record red pill
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color(0xFFFF5252), AccentRed, Color(0xFFB71C1C))
                            )
                        )
                        .border(1.5.dp, Color(0x44FFFFFF), CircleShape)
                )
            }
        } else {
            // Photo shutter disc
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFFFFFFFF), Color(0xFFE2E4EC), Color(0xFFA5A8B6))
                        )
                    )
                    .border(1.dp, Color(0x33000000), CircleShape)
            )
        }
    }
}

/**
 * Sleek Zoom Pill (+ / -) for the side of the viewfinder.
 */
@Composable
fun LuxuryZoomControl(
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x9912121A))
            .border(1.dp, Color(0x2EFFFFFF), RoundedCornerShape(20.dp))
            .padding(vertical = 4.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .clickable(onClick = onZoomIn)
        ) {
            Text(
                text = "+",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Box(
            modifier = Modifier
                .width(16.dp)
                .height(1.dp)
                .background(Color(0x22FFFFFF))
        )

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .clickable(onClick = onZoomOut)
        ) {
            Text(
                text = "−",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Clean mode selector switch (PHOTO / VIDEO).
 */
@Composable
fun LuxuryModeSwitcher(
    isVideoMode: Boolean,
    onModeChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(34.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(Color(0x80181824))
            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(17.dp))
            .padding(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(15.dp))
                .background(if (!isVideoMode) Color(0xFFF3F4F6) else Color.Transparent)
                .clickable { onModeChanged(false) }
                .padding(horizontal = 14.dp, vertical = 5.dp)
        ) {
            Text(
                text = "PHOTO",
                color = if (!isVideoMode) Color.Black else TextMuted,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(15.dp))
                .background(if (isVideoMode) AccentRed else Color.Transparent)
                .clickable { onModeChanged(true) }
                .padding(horizontal = 14.dp, vertical = 5.dp)
        ) {
            Text(
                text = "VIDEO",
                color = if (isVideoMode) Color.White else TextMuted,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}

/**
 * Screen Shutter Flash Overlay.
 */
@Composable
fun ShutterFlashOverlay(
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        )
    }
}
