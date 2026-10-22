package com.example.model

import android.net.Uri

data class MediaItem(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val width: Int,
    val height: Int,
    val sizeBytes: Long,
    val dateModified: Long,
    val isVideo: Boolean,
    val durationMs: Long = 0L,
    val relativePath: String = ""
) {
    val megapixelsText: String
        get() {
            if (width <= 0 || height <= 0) return ""
            val mp = (width.toLong() * height.toLong()).toDouble() / 1_000_000.0
            return String.format("%.2f MP", mp)
        }

    val dimensionsText: String
        get() = if (width > 0 && height > 0) "${width} × ${height}" else ""

    val fileSizeText: String
        get() {
            if (sizeBytes <= 0) return ""
            val kb = sizeBytes / 1024.0
            return if (kb < 1024) {
                String.format("%.1f KB", kb)
            } else {
                String.format("%.2f MB", kb / 1024.0)
            }
        }
}
