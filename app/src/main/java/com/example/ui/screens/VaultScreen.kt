package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.local.DownloadedMediaEntity
import com.example.data.model.MediaType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(
    items: List<DownloadedMediaEntity>,
    onBackClick: () -> Unit,
    onDeleteItem: (Long) -> Unit,
    onOpenManualDownloader: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedFilter by remember { mutableStateOf<MediaType?>(null) }
    var previewItem by remember { mutableStateOf<DownloadedMediaEntity?>(null) }

    val filteredItems = remember(items, selectedFilter) {
        if (selectedFilter == null) items
        else items.filter { it.mediaType == selectedFilter?.name }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = { Text("Downloads Vault (${items.size})") },
            navigationIcon = {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.testTag("vault_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Feed"
                    )
                }
            },
            actions = {
                IconButton(
                    onClick = onOpenManualDownloader,
                    modifier = Modifier.testTag("vault_manual_download_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.AddLink,
                        contentDescription = "Download from URL"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            ),
            modifier = Modifier.statusBarsPadding()
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedFilter == null,
                onClick = { selectedFilter = null },
                label = { Text("All") }
            )
            MediaType.entries.forEach { type ->
                FilterChip(
                    selected = selectedFilter == type,
                    onClick = { selectedFilter = type },
                    label = { Text(type.label) }
                )
            }
        }

        if (filteredItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(28.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_vault_empty),
                        contentDescription = "Empty media vault illustration",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(width = 240.dp, height = 180.dp)
                            .clip(RoundedCornerShape(20.dp))
                    )
                    Text(
                        text = stringResource(R.string.vault_empty_title),
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = stringResource(R.string.vault_empty_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = onOpenManualDownloader,
                        modifier = Modifier.testTag("vault_empty_download_cta")
                    ) {
                        Text("Download via URL")
                    }
                }
            }
        } else {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val columns = if (maxWidth >= 600.dp) 3 else 2
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredItems, key = { it.id }) { item ->
                        VaultMediaCard(
                            item = item,
                            onClick = { previewItem = item }
                        )
                    }
                }
            }
        }
    }

    previewItem?.let { item ->
        val isVideo = item.mimeType.startsWith("video/")
        AlertDialog(
            onDismissRequest = { previewItem = null },
            title = {
                Text(
                    text = "${item.mediaType} • @${item.authorHandle}",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (!isVideo) {
                        AsyncImage(
                            model = runCatching { Uri.parse(item.galleryUri) }.getOrNull(),
                            contentDescription = item.fileName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(14.dp))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Movie,
                                contentDescription = "Saved Video",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                    Text(
                        text = item.fileName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (item.captionSnippet.isNotBlank()) {
                        Text(
                            text = item.captionSnippet,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = item.mimeType
                        putExtra(Intent.EXTRA_STREAM, Uri.parse(item.galleryUri))
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share ${item.mediaType}"))
                }) {
                    Icon(imageVector = Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share")
                }
            },
            dismissButton = {
                Row {
                    OutlinedButton(onClick = {
                        onDeleteItem(item.id)
                        previewItem = null
                    }) {
                        Icon(imageVector = Icons.Filled.DeleteOutline, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    TextButton(onClick = { previewItem = null }) {
                        Text("Close")
                    }
                }
            }
        )
    }
}

@Composable
private fun VaultMediaCard(
    item: DownloadedMediaEntity,
    onClick: () -> Unit
) {
    val isVideo = item.mimeType.startsWith("video/")
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("vault_item_${item.id}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            ) {
                if (isVideo) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Movie,
                            contentDescription = item.mediaType,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                } else {
                    AsyncImage(
                        model = runCatching { Uri.parse(item.galleryUri) }.getOrNull(),
                        contentDescription = item.fileName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Surface(
                    color = Color.Black.copy(alpha = 0.68f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Text(
                        text = item.mediaType,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = "@${item.authorHandle}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${(item.fileSizeBytes / 1024).coerceAtLeast(1)} KB",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
