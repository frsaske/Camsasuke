package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AspectRatioOption
import com.example.model.CameraSettings
import com.example.model.ResolutionOption
import com.example.ui.theme.SasukeAmber
import com.example.ui.theme.SasukeGreen
import com.example.ui.theme.SasukeRed
import com.example.ui.theme.SasukeSilver
import kotlin.math.roundToInt

@Composable
fun ViewfinderOverlay(
    settings: CameraSettings,
    isRecording: Boolean,
    recordingSeconds: Long,
    focusPoint: Offset?,
    nextFilename: String,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "rec_blink")
    val recAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rec_alpha"
    )

    Box(modifier = modifier.fillMaxSize()) {
        // Grid lines and retro corner brackets
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Rule of thirds grid lines if enabled
            if (settings.showGrid) {
                val gridColor = Color(0x33FFFFFF)
                val strokeWidth = 1.dp.toPx()

                // Vertical lines
                drawLine(gridColor, Offset(w / 3f, 0f), Offset(w / 3f, h), strokeWidth)
                drawLine(gridColor, Offset(2f * w / 3f, 0f), Offset(2f * w / 3f, h), strokeWidth)

                // Horizontal lines
                drawLine(gridColor, Offset(0f, h / 3f), Offset(w, h / 3f), strokeWidth)
                drawLine(gridColor, Offset(0f, 2f * h / 3f), Offset(w, 2f * h / 3f), strokeWidth)
            }

            // Vintage Viewfinder HUD brackets
            if (settings.showVintageHud) {
                val bracketColor = Color(0x66FFFFFF)
                val bracketStroke = 1.5.dp.toPx()
                val bracketLen = 22.dp.toPx()
                val margin = 16.dp.toPx()

                // Top-Left
                drawLine(bracketColor, Offset(margin, margin), Offset(margin + bracketLen, margin), bracketStroke)
                drawLine(bracketColor, Offset(margin, margin), Offset(margin, margin + bracketLen), bracketStroke)

                // Top-Right
                drawLine(bracketColor, Offset(w - margin, margin), Offset(w - margin - bracketLen, margin), bracketStroke)
                drawLine(bracketColor, Offset(w - margin, margin), Offset(w - margin, margin + bracketLen), bracketStroke)

                // Bottom-Left
                drawLine(bracketColor, Offset(margin, h - margin), Offset(margin + bracketLen, h - margin), bracketStroke)
                drawLine(bracketColor, Offset(margin, h - margin), Offset(margin, h - margin - bracketLen), bracketStroke)

                // Bottom-Right
                drawLine(bracketColor, Offset(w - margin, h - margin), Offset(w - margin - bracketLen, h - margin), bracketStroke)
                drawLine(bracketColor, Offset(w - margin, h - margin), Offset(w - margin, h - margin - bracketLen), bracketStroke)

                // Center crosshair
                val cx = w / 2f
                val cy = h / 2f
                val chLen = 8.dp.toPx()
                val chGap = 5.dp.toPx()
                val chColor = Color(0x44FFFFFF)
                drawLine(chColor, Offset(cx - chLen - chGap, cy), Offset(cx - chGap, cy), 1.dp.toPx())
                drawLine(chColor, Offset(cx + chGap, cy), Offset(cx + chLen + chGap, cy), 1.dp.toPx())
                drawLine(chColor, Offset(cx, cy - chLen - chGap), Offset(cx, cy - chGap), 1.dp.toPx())
                drawLine(chColor, Offset(cx, cy + chGap), Offset(cx, cy + chLen + chGap), 1.dp.toPx())
            }
        }

        // Tap-to-focus indicator box
        if (focusPoint != null) {
            val boxSize = 56.dp
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (focusPoint.x - (boxSize.toPx() / 2)).roundToInt(),
                            (focusPoint.y - (boxSize.toPx() / 2)).roundToInt()
                        )
                    }
                    .size(boxSize)
                    .border(1.5.dp, SasukeGreen, RoundedCornerShape(4.dp))
            )
        }

        // Top vintage status row
        if (settings.showVintageHud) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Rec/Standby status
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isRecording) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(SasukeRed.copy(alpha = recAlpha))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        val minutes = recordingSeconds / 60
                        val seconds = recordingSeconds % 60
                        Text(
                            text = String.format("REC %02d:%02d", minutes, seconds),
                            color = SasukeRed,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(SasukeGreen)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "STBY",
                            color = SasukeGreen,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Center: Preset indicator
                Text(
                    text = "[ ${settings.preset.title.uppercase()} ]",
                    color = SasukeAmber,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold
                )

                // Right: Resolution & Aspect ratio
                Text(
                    text = "${settings.resolution.label} • ${settings.aspectRatio.label}",
                    color = SasukeSilver,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Bottom camera telemetry (aperture, speed, battery, next file)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "F/2.8 • 1/125s • ISO AUTO",
                    color = Color(0xAAFFFFFF),
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )

                Text(
                    text = nextFilename,
                    color = SasukeAmber,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
