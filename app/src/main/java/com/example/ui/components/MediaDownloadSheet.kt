package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.media.DownloadState
import com.example.data.model.DetectedMediaItem
import com.example.data.model.MediaType

@Composable
fun StandaloneMediaArtwork(
    mediaKey: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    when {
        mediaKey.startsWith("drawable:") -> {
            val resId = when (mediaKey.removePrefix("drawable:").trim().lowercase()) {
                "img_post_neon_tokyo" -> R.drawable.img_post_neon_tokyo
                "img_post_alpine_sunrise" -> R.drawable.img_post_alpine_sunrise
                "img_post_studio_desk" -> R.drawable.img_post_studio_desk
                "img_reel_cosmic_prism" -> R.drawable.img_reel_cosmic_prism
                "img_chat_bg_prism" -> R.drawable.img_chat_bg_prism
                else -> R.drawable.instaplus_app_icon
            }
            Image(
                painter = painterResource(id = resId),
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = modifier
            )
        }
        mediaKey.startsWith("gradient:") -> {
            val colors = when (mediaKey.removePrefix("gradient:").lowercase()) {
                "ocean" -> listOf(Color(0xFF0095F6), Color(0xFF10B981))
                "violet" -> listOf(Color(0xFF7E22CE), Color(0xFFDD2A7B))
                "midnight" -> listOf(Color(0xFF110A1E), Color(0xFF515BD4))
                else -> listOf(Color(0xFFF58529), Color(0xFFDD2A7B))
            }
            Box(
                modifier = modifier.background(Brush.linearGradient(colors)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Image,
                    contentDescription = contentDescription,
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        else -> {
            AsyncImage(
                model = mediaKey,
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = modifier
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaDownloadBottomSheet(
    detectedMedia: List<DetectedMediaItem>,
    downloadState: DownloadState,
    onDownloadItem: (DetectedMediaItem) -> Unit,
    onDownloadManualUrl: (String, MediaType) -> Unit,
    onDismiss: () -> Unit
) {
    var manualUrl by remember { mutableStateOf("") }
    var selectedManualType by remember { mutableStateOf(MediaType.REEL) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "High-Res Media Vault Saver",
                style = MaterialTheme.typography.titleLarge
            )

            when (downloadState) {
                is DownloadState.Downloading -> {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text(
                        text = downloadState.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                is DownloadState.Success -> {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Saved ${downloadState.entity.fileName} to Gallery & Vault",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
                is DownloadState.Error -> {
                    Surface(
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = downloadState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
                DownloadState.Idle -> Unit
            }

            if (detectedMedia.isNotEmpty()) {
                Text(
                    text = "Feed & Reels Media Ready (${detectedMedia.size})",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(detectedMedia) { item ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StandaloneMediaArtwork(
                                    mediaKey = item.url,
                                    contentDescription = item.mediaType.label,
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${item.mediaType.label} • @${item.authorHandle}",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (item.caption.isNotBlank()) {
                                        Text(
                                            text = item.caption,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = { onDownloadItem(item) },
                                    enabled = downloadState !is DownloadState.Downloading,
                                    modifier = Modifier.testTag("download_detected_item_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Download,
                                        contentDescription = "Save",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Save")
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Save External Media URL",
                style = MaterialTheme.typography.labelLarge
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MediaType.entries.forEach { type ->
                    FilterChip(
                        selected = selectedManualType == type,
                        onClick = { selectedManualType = type },
                        label = { Text(type.label) }
                    )
                }
            }
            OutlinedTextField(
                value = manualUrl,
                onValueChange = { manualUrl = it },
                label = { Text("Direct image or video URL") },
                placeholder = { Text("https://...") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("manual_download_url_input")
            )
            Button(
                onClick = { onDownloadManualUrl(manualUrl, selectedManualType) },
                enabled = manualUrl.isNotBlank() && downloadState !is DownloadState.Downloading,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("manual_download_submit_button")
            ) {
                Icon(imageVector = Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save to Device Gallery")
            }
        }
    }
}
