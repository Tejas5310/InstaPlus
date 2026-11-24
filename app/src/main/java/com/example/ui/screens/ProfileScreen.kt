package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.InstaPlusAuthSession
import com.example.data.local.SocialPostEntity
import com.example.ui.components.StandaloneMediaArtwork
import com.example.ui.theme.AdShieldGreen
import com.example.ui.theme.GhostPurple
import com.example.ui.theme.InstagramStoryBrush

@Composable
fun ProfileScreen(
    session: InstaPlusAuthSession,
    userBio: String,
    allPosts: List<SocialPostEntity>,
    vaultCount: Int,
    blockedGhostCount: Int,
    onUpdateBio: (String) -> Unit,
    onCopyText: (String, String) -> Unit,
    onCreatePostClick: () -> Unit,
    onOpenVault: () -> Unit,
    onOpenModHub: () -> Unit,
    onDeletePost: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) } // 0 = My Posts, 1 = Bookmarks
    var showEditBioDialog by remember { mutableStateOf(false) }
    var inspectPost by remember { mutableStateOf<SocialPostEntity?>(null) }

    val ownPosts = remember(allPosts, session.username) {
        allPosts.filter { it.authorUsername.equals(session.username, ignoreCase = true) }
    }
    val bookmarkedPosts = remember(allPosts) {
        allPosts.filter { it.isBookmarked }
    }
    val totalLikesReceived = remember(ownPosts) { ownPosts.sumOf { it.likesCount } }
    val followingCount = remember(allPosts) {
        allPosts.filter { it.isFollowingAuthor }.map { it.authorUsername }.distinct().size
    }

    val displayedPosts = if (selectedTab == 0) ownPosts else bookmarkedPosts

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("standalone_profile_screen")
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .border(2.5.dp, InstagramStoryBrush, CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = session.fullName.take(1).uppercase(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ProfileStatCol(ownPosts.size.toString(), "Posts")
                        ProfileStatCol(followingCount.toString(), "Following")
                        ProfileStatCol(bookmarkedPosts.size.toString(), "Saved")
                        ProfileStatCol(vaultCount.toString(), "Vault")
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = session.fullName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "@${session.username} • ${session.email}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = { onCopyText("Profile bio & handle", "@${session.username}\n$userBio") }) {
                        Icon(
                            imageVector = Icons.Filled.ContentCopy,
                            contentDescription = "Copy Bio & Handle",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = { showEditBioDialog = true }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit Bio")
                    }
                }

                Text(text = userBio, style = MaterialTheme.typography.bodyMedium)

                // Exclusive Account & Privacy Insights Radar
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Analytics,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Creator & Privacy Insights",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Likes earned: $totalLikesReceived", style = MaterialTheme.typography.bodySmall)
                            Text(
                                "Ghost blocks: $blockedGhostCount",
                                style = MaterialTheme.typography.bodySmall,
                                color = GhostPurple,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "0 Ads",
                                style = MaterialTheme.typography.bodySmall,
                                color = AdShieldGreen,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(onClick = onCreatePostClick, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.AddBox, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Create")
                    }
                    OutlinedButton(onClick = onOpenVault, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Vault")
                    }
                    OutlinedButton(onClick = onOpenModHub, modifier = Modifier.weight(1f)) {
                        Text("Settings")
                    }
                }

                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Posts (${ownPosts.size})") },
                        icon = { Icon(Icons.Filled.GridOn, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Saved (${bookmarkedPosts.size})") },
                        icon = { Icon(Icons.Filled.Bookmark, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                }
            }
        }

        if (displayedPosts.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (selectedTab == 0) "Tap Create to publish your first InstaPlus post!"
                        else "Bookmark any feed post or Reel to collect it here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(displayedPosts, key = { it.id }) { post ->
                StandaloneMediaArtwork(
                    mediaKey = post.mediaKey,
                    contentDescription = post.caption,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { inspectPost = post }
                )
            }
        }
    }

    if (showEditBioDialog) {
        var bioDraft by remember { mutableStateOf(userBio) }
        AlertDialog(
            onDismissRequest = { showEditBioDialog = false },
            title = { Text("Edit Profile Bio") },
            text = {
                OutlinedTextField(
                    value = bioDraft,
                    onValueChange = { bioDraft = it },
                    label = { Text("Bio") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    onUpdateBio(bioDraft)
                    showEditBioDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showEditBioDialog = false }) { Text("Cancel") }
            }
        )
    }

    inspectPost?.let { post ->
        AlertDialog(
            onDismissRequest = { inspectPost = null },
            title = { Text("@${post.authorUsername}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    StandaloneMediaArtwork(
                        mediaKey = post.mediaKey,
                        contentDescription = post.caption,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    Text(text = post.caption, style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = {
                if (post.authorUsername.equals(session.username, ignoreCase = true)) {
                    OutlinedButton(onClick = {
                        onDeletePost(post.id)
                        inspectPost = null
                    }) {
                        Icon(Icons.Filled.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Delete")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { inspectPost = null }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun ProfileStatCol(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
