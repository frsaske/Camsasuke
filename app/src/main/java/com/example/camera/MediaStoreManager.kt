package com.example.camera

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.example.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.regex.Pattern

class MediaStoreManager(private val context: Context) {

    companion object {
        const val ALBUM_NAME = "Sasuke RetroCam"
        private const val PREFS_NAME = "sasuke_media_prefs"
        private const val KEY_PHOTO_INDEX = "photo_index"
        private const val KEY_VIDEO_INDEX = "video_index"
        private const val TAG = "MediaStoreManager"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Finds the next sequential photo filename, e.g. "Sasuke-001.jpg", avoiding collisions.
     */
    suspend fun getNextPhotoFilename(): String = withContext(Dispatchers.IO) {
        val maxExisting = queryMaxIndex(isVideo = false)
        val storedIndex = prefs.getInt(KEY_PHOTO_INDEX, 0)
        val nextIndex = maxOf(maxExisting, storedIndex) + 1
        prefs.edit().putInt(KEY_PHOTO_INDEX, nextIndex).apply()
        String.format("Sasuke-%03d.jpg", nextIndex)
    }

    /**
     * Finds the next sequential video filename, e.g. "Sasuke-Video-001.mp4", avoiding collisions.
     */
    suspend fun getNextVideoFilename(): String = withContext(Dispatchers.IO) {
        val maxExisting = queryMaxIndex(isVideo = true)
        val storedIndex = prefs.getInt(KEY_VIDEO_INDEX, 0)
        val nextIndex = maxOf(maxExisting, storedIndex) + 1
        prefs.edit().putInt(KEY_VIDEO_INDEX, nextIndex).apply()
        String.format("Sasuke-Video-%03d.mp4", nextIndex)
    }

    private fun queryMaxIndex(isVideo: Boolean): Int {
        val collection = if (isVideo) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }
        }

        val projection = arrayOf(MediaStore.MediaColumns.DISPLAY_NAME)
        val prefix = if (isVideo) "Sasuke-Video-" else "Sasuke-"
        val regex = if (isVideo) {
            Pattern.compile("Sasuke-Video-(\\d+)\\..*", Pattern.CASE_INSENSITIVE)
        } else {
            Pattern.compile("Sasuke-(\\d+)\\..*", Pattern.CASE_INSENSITIVE)
        }

        var maxNum = 0
        try {
            val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?"
            val selectionArgs = arrayOf("$prefix%")
            context.contentResolver.query(collection, projection, selection, selectionArgs, null)?.use { cursor ->
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                while (cursor.moveToNext()) {
                    val name = cursor.getString(nameCol) ?: continue
                    val matcher = regex.matcher(name)
                    if (matcher.matches()) {
                        val numStr = matcher.group(1)
                        val num = numStr?.toIntOrNull() ?: 0
                        if (num > maxNum) maxNum = num
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying max index", e)
        }
        return maxNum
    }

    /**
     * Saves a retro processed bitmap directly to MediaStore in album "Sasuke RetroCam".
     */
    suspend fun saveBitmap(
        bitmap: Bitmap,
        quality: Int = 90,
        customFilename: String? = null
    ): Result<Uri> = withContext(Dispatchers.IO) {
        val filename = customFilename ?: getNextPhotoFilename()
        val resolver = context.contentResolver
        val relativePath = "${Environment.DIRECTORY_DCIM}/$ALBUM_NAME"

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.WIDTH, bitmap.width)
            put(MediaStore.Images.Media.HEIGHT, bitmap.height)
            val nowSeconds = System.currentTimeMillis() / 1000
            put(MediaStore.Images.Media.DATE_ADDED, nowSeconds)
            put(MediaStore.Images.Media.DATE_MODIFIED, nowSeconds)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, relativePath)
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val uri = resolver.insert(collection, contentValues)
            ?: return@withContext Result.failure(Exception("Could not create MediaStore entry"))

        try {
            resolver.openOutputStream(uri)?.use { outStream ->
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outStream)) {
                    throw Exception("Bitmap compression failed")
                }
                outStream.flush()
            } ?: throw Exception("Failed to open output stream")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }

            Result.success(uri)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save photo", e)
            resolver.delete(uri, null, null)
            Result.failure(e)
        }
    }

    /**
     * Prepares ContentValues and URI for direct video recording into MediaStore.
     */
    suspend fun createVideoContentValues(filename: String): Pair<Uri, ContentValues> = withContext(Dispatchers.IO) {
        val relativePath = "${Environment.DIRECTORY_DCIM}/$ALBUM_NAME"
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, filename)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            val nowSeconds = System.currentTimeMillis() / 1000
            put(MediaStore.Video.Media.DATE_ADDED, nowSeconds)
            put(MediaStore.Video.Media.DATE_MODIFIED, nowSeconds)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, relativePath)
            }
        }
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }
        Pair(collection, values)
    }

    /**
     * Copies a recorded video file to MediaStore in album "Sasuke RetroCam".
     */
    suspend fun saveVideoFile(videoFile: File): Result<Uri> = withContext(Dispatchers.IO) {
        val filename = getNextVideoFilename()
        val resolver = context.contentResolver
        val relativePath = "${Environment.DIRECTORY_DCIM}/$ALBUM_NAME"

        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, filename)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            val nowSeconds = System.currentTimeMillis() / 1000
            put(MediaStore.Video.Media.DATE_ADDED, nowSeconds)
            put(MediaStore.Video.Media.DATE_MODIFIED, nowSeconds)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, relativePath)
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val uri = resolver.insert(collection, values)
            ?: return@withContext Result.failure(Exception("Unable to create MediaStore entry for video"))

        try {
            resolver.openOutputStream(uri)?.use { out ->
                videoFile.inputStream().use { input ->
                    input.copyTo(out)
                }
                out.flush()
            } ?: throw Exception("Failed to open output stream for video")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Video.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }

            // Cleanup temp file
            videoFile.delete()
            Result.success(uri)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save video to MediaStore", e)
            resolver.delete(uri, null, null)
            Result.failure(e)
        }
    }

    /**
     * Loads all media items in the Sasuke RetroCam album, sorted latest first.
     */
    suspend fun queryAlbumMedia(): List<MediaItem> = withContext(Dispatchers.IO) {
        val items = mutableListOf<MediaItem>()
        val resolver = context.contentResolver

        // 1. Query Images
        val imageCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val imageProjection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_MODIFIED,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.Images.Media.RELATIVE_PATH else MediaStore.Images.Media.DATA
        )

        try {
            val selection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ? OR ${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?"
            } else {
                "${MediaStore.Images.Media.DATA} LIKE ? OR ${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?"
            }
            val selectionArgs = arrayOf("%$ALBUM_NAME%", "Sasuke-%")

            resolver.query(
                imageCollection,
                imageProjection,
                selection,
                selectionArgs,
                "${MediaStore.Images.Media.DATE_MODIFIED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
                val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
                val pathCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.Images.Media.RELATIVE_PATH)
                } else {
                    cursor.getColumnIndex(MediaStore.Images.Media.DATA)
                }

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val uri = ContentUris.withAppendedId(imageCollection, id)
                    val name = cursor.getString(nameCol) ?: "Sasuke-Photo.jpg"
                    val width = cursor.getInt(widthCol)
                    val height = cursor.getInt(heightCol)
                    val size = cursor.getLong(sizeCol)
                    val date = cursor.getLong(dateCol) * 1000
                    val path = if (pathCol != -1) cursor.getString(pathCol) ?: "" else ""

                    items.add(
                        MediaItem(
                            id = id,
                            uri = uri,
                            displayName = name,
                            width = width,
                            height = height,
                            sizeBytes = size,
                            dateModified = date,
                            isVideo = false,
                            relativePath = path
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying photos", e)
        }

        // 2. Query Videos
        val videoCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val videoProjection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.DURATION,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.Video.Media.RELATIVE_PATH else MediaStore.Video.Media.DATA
        )

        try {
            val selection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                "${MediaStore.Video.Media.RELATIVE_PATH} LIKE ? OR ${MediaStore.Video.Media.DISPLAY_NAME} LIKE ?"
            } else {
                "${MediaStore.Video.Media.DATA} LIKE ? OR ${MediaStore.Video.Media.DISPLAY_NAME} LIKE ?"
            }
            val selectionArgs = arrayOf("%$ALBUM_NAME%", "Sasuke-Video-%")

            resolver.query(
                videoCollection,
                videoProjection,
                selection,
                selectionArgs,
                "${MediaStore.Video.Media.DATE_MODIFIED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
                val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val pathCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.Video.Media.RELATIVE_PATH)
                } else {
                    cursor.getColumnIndex(MediaStore.Video.Media.DATA)
                }

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val uri = ContentUris.withAppendedId(videoCollection, id)
                    val name = cursor.getString(nameCol) ?: "Sasuke-Video.mp4"
                    val width = cursor.getInt(widthCol)
                    val height = cursor.getInt(heightCol)
                    val size = cursor.getLong(sizeCol)
                    val date = cursor.getLong(dateCol) * 1000
                    val duration = cursor.getLong(durationCol)
                    val path = if (pathCol != -1) cursor.getString(pathCol) ?: "" else ""

                    items.add(
                        MediaItem(
                            id = id,
                            uri = uri,
                            displayName = name,
                            width = width,
                            height = height,
                            sizeBytes = size,
                            dateModified = date,
                            isVideo = true,
                            durationMs = duration,
                            relativePath = path
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying videos", e)
        }

        // Sort descending by date
        items.sortedByDescending { it.dateModified }
    }

    /**
     * Deletes a media item from MediaStore.
     */
    suspend fun deleteMedia(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val rows = context.contentResolver.delete(uri, null, null)
            rows > 0
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete media: $uri", e)
            false
        }
    }
}
