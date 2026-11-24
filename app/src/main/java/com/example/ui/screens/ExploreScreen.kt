package com.example.ui.screens

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.SocialPostEntity
import com.example.ui.components.StandaloneMediaArtwork

private val exploreCategories = listOf("All", "Travel", "Nature", "Design", "Tech", "Lifestyle")

@Composable
fun ExploreScreen(
    posts: List<SocialPostEntity>,
    onToggleLike: (SocialPostEntity) -> Unit,
    onSaveToVault: (SocialPostEntity) -> Unit,
    onCopyCaption: (SocialPostEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedPost by remember { mutableStateOf<SocialPostEntity?>(null) }

    val filtered = remember(posts, searchQuery, selectedCategory) {
        posts.filter { post ->
            val catMatch = selectedCategory == "All" ||
                post.categoryTag.equals(selectedCategory, ignoreCase = true)
            val q = searchQuery.trim().lowercase()
            val queryMatch = q.isBlank() ||
                post.authorUsername.lowercase().contains(q) ||
                post.caption.lowercase().contains(q) ||
                post.hashtags.lowercase().contains(q) ||
                post.categoryTag.lowercase().contains(q)
            catMatch && queryMatch
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search creators, captions, or #tags...") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .testTag("explore_search_input")
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            exploreCategories.forEach { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = cat },
                    label = { Text(cat) },
                    modifier = Modifier.testTag("explore_cat_${cat.lowercase()}")
                )
            }
        }

        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No matching posts found for \"$searchQuery\".",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val columns = if (maxWidth >= 650.dp) 3 else 2
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filtered, key = { it.id }) { post ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(0.95f)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { selectedPost = post }
                                .testTag("explore_grid_item_${post.id}")
                        ) {
                            StandaloneMediaArtwork(
                                mediaKey = post.mediaKey,
                                contentDescription = post.caption,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                Color.Transparent,
                                                Color.Black.copy(alpha = 0.65f)
                                            )
                                        )
                                    )
                            )
                            if (post.pollQuestion.isNotBlank()) {
                                Surface(
                                    color = Color.Black.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.HowToVote,
                                        contentDescription = "Includes Poll",
                                        tint = Color.White,
                                        modifier = Modifier.padding(5.dp).size(14.dp)
                                    )
                                }
                            }
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = "@${post.authorUsername}",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Favorite,
                                        contentDescription = null,
                                        tint = Color(0xFFED4956),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "${post.likesCount} • ${post.categoryTag}",
                                        color = Color.White.copy(alpha = 0.9f),
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    selectedPost?.let { post ->
        AlertDialog(
            onDismissRequest = { selectedPost = null },
            title = { Text("@${post.authorUsername} • ${post.categoryTag}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    StandaloneMediaArtwork(
                        mediaKey = post.mediaKey,
                        contentDescription = post.caption,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp)
                            .clip(RoundedCornerShape(14.dp))
                    )
                    Text(
                        text = post.caption,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (post.hashtags.isNotBlank()) {
                        Text(
                            text = post.hashtags,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    onSaveToVault(post)
                    selectedPost = null
                }) {
                    Text("Save HD")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(onClick = {
                        onToggleLike(post)
                        selectedPost = post.copy(
                            isLiked = !post.isLiked,
                            likesCount = post.likesCount + if (!post.isLiked) 1 else -1
                        )
                    }) {
                        Text(if (post.isLiked) "Liked ♥" else "Like")
                    }
                    TextButton(onClick = {
                        onCopyCaption(post)
                    }) {
                        Text("Copy")
                    }
                    TextButton(onClick = { selectedPost = null }) {
                        Text("Close")
                    }
                }
            }
        )
    }
}
