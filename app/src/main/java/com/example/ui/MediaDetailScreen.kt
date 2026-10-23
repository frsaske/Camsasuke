package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.MediaItem
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkCardElevated
import com.example.ui.theme.DarkCardSurface
import com.example.ui.theme.RecordRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MediaDetailScreen(
    items: List<MediaItem>,
    initialIndex: Int,
    onBack: () -> Unit,
    onDelete: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val context = LocalContext.current

    if (items.isEmpty()) {
        onBack()
        return
    }

    val safeInitialIndex = initialIndex.coerceIn(0, items.size - 1)
    val pagerState = rememberPagerState(initialPage = safeInitialIndex) { items.size }

    val currentItem = items.getOrNull(pagerState.currentPage) ?: items[0]

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }

    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = DarkBackground
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            // Sliding Horizontal Pager for swiping between photos and videos
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val media = items[page]
                if (media.isVideo) {
                    // Video View
                    AndroidView(
                        factory = { ctx ->
                            VideoView(ctx).apply {
                                setVideoURI(media.uri)
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
                    // Photo View with pinch-to-zoom
                    var scale by remember { mutableFloatStateOf(1f) }
                    var offset by remember { mutableStateOf(Offset.Zero) }
                    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
                        scale = (scale * zoomChange).coerceIn(1f, 5f)
                        offset = if (scale > 1f) offset + offsetChange else Offset.Zero
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .transformable(state = transformState),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(media.uri)
                                .crossfade(true)
                                .build(),
                            contentDescription = media.displayName,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offset.x,
                                    translationY = offset.y
                                )
                        )
                    }
                }
            }

            // Top Header: Back button + Index Indicator + Name
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(top = topPadding)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .testTag("detail_back_button")
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(DarkCardSurface.copy(alpha = 0.85f))
                        .border(1.dp, DarkBorder, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }

                // Index Pill (e.g., 3 of 12)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(DarkCardSurface.copy(alpha = 0.85f))
                        .border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${pagerState.currentPage + 1} / ${items.size}",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Quick Info Icon
                IconButton(
                    onClick = { showInfoDialog = true },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(DarkCardSurface.copy(alpha = 0.85f))
                        .border(1.dp, DarkBorder, CircleShape)
                        .testTag("detail_info_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Details",
                        tint = TextPrimary
                    )
                }
            }

            // Bottom Action Dock: Native Gallery Tools (Share, Open In, Delete)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = bottomPadding + 8.dp)
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(DarkCardSurface.copy(alpha = 0.90f))
                    .border(1.dp, DarkBorder, RoundedCornerShape(24.dp))
                    .padding(vertical = 8.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Share
                IconButton(
                    onClick = { shareMedia(context, currentItem) },
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("action_share_button")
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text("Share", color = TextSecondary, fontSize = 10.sp)
                    }
                }

                // Open In
                IconButton(
                    onClick = { openExternal(context, currentItem) },
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("action_open_in_button")
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = "Open In",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text("Open", color = TextSecondary, fontSize = 10.sp)
                    }
                }

                // Info
                IconButton(
                    onClick = { showInfoDialog = true },
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("action_info_button")
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Info",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text("Info", color = TextSecondary, fontSize = 10.sp)
                    }
                }

                // Delete
                IconButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("action_delete_button")
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = RecordRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Text("Delete", color = RecordRed, fontSize = 10.sp)
                    }
                }
            }
        }
    }

    // Info Dialog
    if (showInfoDialog) {
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US)
        val formattedDate = sdf.format(Date(currentItem.dateModified * 1000L))
        val sizeText = currentItem.fileSizeText

        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            containerColor = DarkCardSurface,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = {
                Text(
                    text = "ITEM INFORMATION",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    InfoRow(label = "Filename", value = currentItem.displayName)
                    InfoRow(label = "Type", value = if (currentItem.isVideo) "Retro Video (MP4)" else "Retro Photo (JPEG)")
                    if (currentItem.dimensionsText.isNotEmpty()) {
                        InfoRow(label = "Resolution", value = "${currentItem.dimensionsText} (${currentItem.megapixelsText})")
                    }
                    InfoRow(label = "Aspect Ratio", value = "4:3")
                    InfoRow(label = "Watermark", value = "Permanent \"ѕαѕυкє\"")
                    if (sizeText.isNotEmpty()) {
                        InfoRow(label = "Size", value = sizeText)
                    }
                    InfoRow(label = "Captured", value = formattedDate)
                }
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) {
                    Text("CLOSE", color = TextPrimary, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = DarkCardSurface,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = {
                Text(
                    text = "Delete File?",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Do you want to permanently delete \"${currentItem.displayName}\" from Sasuke RetroCam gallery?",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        onDelete(currentItem)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RecordRed)
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = TextMuted, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        Text(text = value, color = TextPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium)
    }
}

private fun shareMedia(context: Context, item: MediaItem) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = if (item.isVideo) "video/mp4" else "image/jpeg"
        putExtra(Intent.EXTRA_STREAM, item.uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share Sasuke Retro Capture"))
}

private fun openExternal(context: Context, item: MediaItem) {
    val viewIntent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(item.uri, if (item.isVideo) "video/mp4" else "image/jpeg")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(viewIntent, "Open Media"))
}
