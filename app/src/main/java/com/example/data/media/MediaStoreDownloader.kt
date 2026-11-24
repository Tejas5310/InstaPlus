package com.example.data.media

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.local.DownloadedMediaEntity
import com.example.data.model.DetectedMediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

private const val TAG = "MediaStoreDownloader"

sealed interface DownloadState {
    data object Idle : DownloadState
    data class Downloading(val label: String) : DownloadState
    data class Success(val entity: DownloadedMediaEntity) : DownloadState
    data class Error(val message: String) : DownloadState
}

class MediaStoreDownloader(
    private val context: Context,
    private val ghostInterceptor: Interceptor? = null
) {

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .followRedirects(true)
            .apply {
                if (ghostInterceptor != null) {
                    addInterceptor(ghostInterceptor)
                }
            }
            .build()
    }

    suspend fun downloadToGallery(item: DetectedMediaItem): Result<DownloadedMediaEntity> =
        withContext(Dispatchers.IO) {
            runCatching {
                val cleanUrl = item.url.trim()
                val safeAuthor = item.authorHandle
                    .replace(Regex("[^a-zA-Z0-9._-]"), "_")
                    .ifBlank { "instaplus" }
                val timestamp = System.currentTimeMillis()

                when {
                    cleanUrl.startsWith("drawable:") -> {
                        val resId = resolveDrawableResId(cleanUrl.removePrefix("drawable:"))
                        val bmp = BitmapFactory.decodeResource(context.resources, resId)
                            ?: error("Unable to decode standalone artwork resource")
                        val bytes = compressBitmapToJpeg(bmp)
                        val fileName = "InstaPlus_${safeAuthor}_${item.mediaType.name.lowercase()}_$timestamp.jpg"
                        saveStreamAsVaultEntity(
                            isVideo = false,
                            fileName = fileName,
                            mimeType = "image/jpeg",
                            sourceUrl = cleanUrl,
                            item = item,
                            timestamp = timestamp,
                            stream = ByteArrayInputStream(bytes)
                        )
                    }

                    cleanUrl.startsWith("gradient:") -> {
                        val bmp = renderGradientCardBitmap(
                            gradientKey = cleanUrl.removePrefix("gradient:"),
                            author = safeAuthor,
                            caption = item.caption
                        )
                        val bytes = compressBitmapToJpeg(bmp)
                        val fileName = "InstaPlus_${safeAuthor}_${item.mediaType.name.lowercase()}_$timestamp.jpg"
                        saveStreamAsVaultEntity(
                            isVideo = false,
                            fileName = fileName,
                            mimeType = "image/jpeg",
                            sourceUrl = cleanUrl,
                            item = item,
                            timestamp = timestamp,
                            stream = ByteArrayInputStream(bytes)
                        )
                    }

                    cleanUrl.startsWith("content://") || cleanUrl.startsWith("file://") -> {
                        val parsedUri = Uri.parse(cleanUrl)
                        val detectedMime = context.contentResolver.getType(parsedUri)
                            ?: if (item.isVideo) "video/mp4" else "image/jpeg"
                        val isVideo = item.isVideo || detectedMime.startsWith("video/")
                        val ext = when {
                            detectedMime.contains("mp4") -> "mp4"
                            detectedMime.contains("png") -> "png"
                            detectedMime.contains("webp") -> "webp"
                            else -> "jpg"
                        }
                        val fileName = "InstaPlus_${safeAuthor}_${item.mediaType.name.lowercase()}_$timestamp.$ext"
                        val input = context.contentResolver.openInputStream(parsedUri)
                            ?: error("Unable to read selected media file")
                        input.use { stream ->
                            saveStreamAsVaultEntity(
                                isVideo = isVideo,
                                fileName = fileName,
                                mimeType = detectedMime,
                                sourceUrl = cleanUrl,
                                item = item,
                                timestamp = timestamp,
                                stream = stream
                            )
                        }
                    }

                    cleanUrl.startsWith("http://") || cleanUrl.startsWith("https://") -> {
                        val request = Request.Builder()
                            .url(cleanUrl)
                            .header("User-Agent", "InstaPlus-Android/2.0")
                            .build()

                        client.newCall(request).execute().use { response ->
                            if (!response.isSuccessful) {
                                error("Download failed with HTTP ${response.code}")
                            }
                            val body = response.body ?: error("Empty response body")
                            val contentType = response.header("Content-Type")
                                ?.substringBefore(";")
                                ?.trim()
                                ?.lowercase()
                            val isVideo = item.isVideo ||
                                (contentType?.startsWith("video/") == true) ||
                                cleanUrl.contains(".mp4", ignoreCase = true)

                            val mimeType = when {
                                !contentType.isNullOrBlank() &&
                                    (contentType.startsWith("image/") || contentType.startsWith("video/")) -> contentType
                                isVideo -> "video/mp4"
                                else -> "image/jpeg"
                            }

                            val ext = when {
                                mimeType.contains("mp4") -> "mp4"
                                mimeType.contains("png") -> "png"
                                mimeType.contains("webp") -> "webp"
                                else -> "jpg"
                            }
                            val fileName = "InstaPlus_${safeAuthor}_${item.mediaType.name.lowercase()}_$timestamp.$ext"
                            saveStreamAsVaultEntity(
                                isVideo = isVideo,
                                fileName = fileName,
                                mimeType = mimeType,
                                sourceUrl = cleanUrl,
                                item = item,
                                timestamp = timestamp,
                                stream = body.byteStream()
                            )
                        }
                    }

                    else -> error("Unsupported media source. Select a post, Story, Reel, or paste a valid https:// URL.")
                }
            }.onFailure { err ->
                Log.e(TAG, "Failed saving media", err)
            }
        }

    private fun saveStreamAsVaultEntity(
        isVideo: Boolean,
        fileName: String,
        mimeType: String,
        sourceUrl: String,
        item: DetectedMediaItem,
        timestamp: Long,
        stream: InputStream
    ): DownloadedMediaEntity {
        val savedResult = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveViaMediaStoreQ(
                isVideo = isVideo,
                fileName = fileName,
                mimeType = mimeType,
                stream = stream
            )
        } else {
            saveToAppScopedGalleryLegacy(
                isVideo = isVideo,
                fileName = fileName,
                stream = stream
            )
        }

        return DownloadedMediaEntity(
            mediaType = item.mediaType.name,
            authorHandle = item.authorHandle,
            captionSnippet = item.caption.take(180),
            sourceUrl = sourceUrl,
            galleryUri = savedResult.first.toString(),
            fileName = fileName,
            mimeType = mimeType,
            fileSizeBytes = savedResult.second,
            savedAt = timestamp
        )
    }

    private fun resolveDrawableResId(key: String): Int = when (key.trim().lowercase()) {
        "img_post_neon_tokyo" -> R.drawable.img_post_neon_tokyo
        "img_post_alpine_sunrise" -> R.drawable.img_post_alpine_sunrise
        "img_post_studio_desk" -> R.drawable.img_post_studio_desk
        "img_reel_cosmic_prism" -> R.drawable.img_reel_cosmic_prism
        "img_chat_bg_prism" -> R.drawable.img_chat_bg_prism
        else -> R.drawable.instaplus_app_icon
    }

    private fun renderGradientCardBitmap(
        gradientKey: String,
        author: String,
        caption: String
    ): Bitmap {
        val width = 1080
        val height = 1080
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val (c1, c2) = when (gradientKey.lowercase()) {
            "ocean" -> 0xFF0095F6.toInt() to 0xFF10B981.toInt()
            "violet" -> 0xFF7E22CE.toInt() to 0xFFDD2A7B.toInt()
            "midnight" -> 0xFF110A1E.toInt() to 0xFF515BD4.toInt()
            else -> 0xFFF58529.toInt() to 0xFFDD2A7B.toInt()
        }
        val bgPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                c1, c2, Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = 52f
            isFakeBoldText = true
        }
        val lineText = caption.ifBlank { "Shared via InstaPlus" }.take(44)
        canvas.drawText(lineText, 84f, height * 0.48f, textPaint)

        val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xCCFFFFFF.toInt()
            textSize = 38f
        }
        canvas.drawText("@$author • InstaPlus", 84f, height * 0.58f, handlePaint)
        return bmp
    }

    private fun compressBitmapToJpeg(bitmap: Bitmap): ByteArray {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 92, baos)
        return baos.toByteArray()
    }

    private fun saveViaMediaStoreQ(
        isVideo: Boolean,
        fileName: String,
        mimeType: String,
        stream: InputStream
    ): Pair<Uri, Long> {
        val resolver = context.contentResolver
        val collection = if (isVideo) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        }
        val relativeDir = if (isVideo) {
            "${Environment.DIRECTORY_MOVIES}/InstaPlus"
        } else {
            "${Environment.DIRECTORY_PICTURES}/InstaPlus"
        }

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativeDir)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }

        val uri = resolver.insert(collection, values)
            ?: error("Unable to create MediaStore entry")

        var totalBytes = 0L
        try {
            resolver.openOutputStream(uri)?.use { out ->
                val buffer = ByteArray(16 * 1024)
                while (true) {
                    val read = stream.read(buffer)
                    if (read <= 0) break
                    out.write(buffer, 0, read)
                    totalBytes += read
                }
                out.flush()
            } ?: error("Unable to open MediaStore output stream")

            val publishValues = ContentValues().apply {
                put(MediaStore.MediaColumns.IS_PENDING, 0)
            }
            resolver.update(uri, publishValues, null, null)
            return uri to totalBytes
        } catch (t: Throwable) {
            runCatching { resolver.delete(uri, null, null) }
            throw t
        }
    }

    private fun saveToAppScopedGalleryLegacy(
        isVideo: Boolean,
        fileName: String,
        stream: InputStream
    ): Pair<Uri, Long> {
        val dirType = if (isVideo) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES
        val parent = File(context.getExternalFilesDir(dirType), "InstaPlus").apply { mkdirs() }
        val outFile = File(parent, fileName)
        var totalBytes = 0L
        FileOutputStream(outFile).use { out ->
            val buffer = ByteArray(16 * 1024)
            while (true) {
                val read = stream.read(buffer)
                if (read <= 0) break
                out.write(buffer, 0, read)
                totalBytes += read
            }
            out.flush()
        }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            outFile
        )
        return uri to totalBytes
    }
}
