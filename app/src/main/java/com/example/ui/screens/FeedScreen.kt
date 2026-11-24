package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.PostCommentEntity
import com.example.data.local.SocialPostEntity
import com.example.data.local.StoryEntity
import com.example.data.model.InstaPlusSettings
import com.example.ui.components.CommentsBottomSheet
import com.example.ui.components.InstaPlusQuickDock
import com.example.ui.components.SocialPostCard
import com.example.ui.components.StandaloneMediaArtwork
import com.example.ui.theme.AdShieldGreen
import com.example.ui.theme.GhostPurple
import com.example.ui.theme.InstagramStoryBrush
import kotlinx.coroutines.delay

private enum class FeedFilter(val label: String) {
    ALL("All Feed"),
    FOLLOWING("Following"),
    POLLS("Polls"),
    TIME_CAPSULES("Time Capsules")
}

@Composable
fun FeedScreen(
    settings: InstaPlusSettings,
    currentUsername: String,
    posts: List<SocialPostEntity>,
    comments: List<PostCommentEntity>,
    stories: List<StoryEntity>,
    onToggleLike: (SocialPostEntity) -> Unit,
    onToggleBookmark: (SocialPostEntity) -> Unit,
    onToggleFollow: (SocialPostEntity) -> Unit,
    onVotePoll: (SocialPostEntity, Int) -> Unit,
    onAddComment: (Long, String) -> Unit,
    onStoryViewed: (StoryEntity) -> Unit,
    onSavePostToVault: (SocialPostEntity) -> Unit,
    onSaveStoryToVault: (StoryEntity) -> Unit,
    onCopyText: (String, String) -> Unit,
    onTranslateText: (String) -> Unit,
    onSharePost: (SocialPostEntity) -> Unit,
    onOpenCreateComposer: () -> Unit,
    onOpenDownloadSheet: () -> Unit,
    onToggleGhostClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf(FeedFilter.ALL) }
    var activeCommentsPostId by remember { mutableStateOf<Long?>(null) }
    var activeStory by remember { mutableStateOf<StoryEntity?>(null) }

    val commentsByPost = remember(comments) { comments.groupBy { it.postId } }
    val filteredPosts = remember(posts, selectedFilter, settings.hideSuggestedPosts) {
        val base = if (settings.hideSuggestedPosts) {
            posts.filter { it.isFollowingAuthor || it.authorUsername.equals(currentUsername, ignoreCase = true) }
        } else {
            posts
        }
        when (selectedFilter) {
            FeedFilter.ALL -> base
            FeedFilter.FOLLOWING -> base.filter { it.isFollowingAuthor || it.authorUsername.equals(currentUsername, ignoreCase = true) }
            FeedFilter.POLLS -> base.filter { it.pollQuestion.isNotBlank() }
            FeedFilter.TIME_CAPSULES -> base.filter { it.expiresAtMillis != null }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("standalone_home_feed_list"),
            contentPadding = PaddingValues(bottom = 84.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Stories Tray
            item {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(72.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable(onClick = onOpenCreateComposer)
                                .testTag("create_story_ring_button")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = "Add Story or Post",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "New Story",
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    items(stories, key = { it.id }) { story ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(72.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    activeStory = story
                                    onStoryViewed(story)
                                }
                                .testTag("story_ring_${story.authorUsername}")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(66.dp)
                                    .clip(CircleShape)
                                    .border(
                                        width = if (story.isViewed) 1.5.dp else 2.5.dp,
                                        brush = if (story.isViewed) {
                                            Brush.linearGradient(
                                                listOf(
                                                    MaterialTheme.colorScheme.outline,
                                                    MaterialTheme.colorScheme.outline
                                                )
                                            )
                                        } else InstagramStoryBrush,
                                        shape = CircleShape
                                    )
                                    .padding(4.dp)
                            ) {
                                StandaloneMediaArtwork(
                                    mediaKey = story.mediaKey,
                                    contentDescription = story.authorFullName,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                )
                            }
                            Spacer(modifier = Modifier.height(5.dp))
                            Text(
                                text = story.authorUsername,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Filter Pills + Ad-Free Shield Pill
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 640.dp)
                        .padding(horizontal = 14.dp)
                ) {
                    if (settings.isAdBlockAnyActive) {
                        Surface(
                            color = AdShieldGreen.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Shield,
                                    contentDescription = null,
                                    tint = AdShieldGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "100% Standalone Ad-Free Feed • Zero external trackers",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = AdShieldGreen,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FeedFilter.entries.forEach { filter ->
                            FilterChip(
                                selected = selectedFilter == filter,
                                onClick = { selectedFilter = filter },
                                label = { Text(filter.label) },
                                modifier = Modifier.testTag("feed_filter_${filter.name.lowercase()}")
                            )
                        }
                    }
                }
            }

            if (filteredPosts.isEmpty()) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .padding(20.dp)
                            .widthIn(max = 500.dp)
                            .fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Nothing in this filter yet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Tap + Create to publish a post, live poll, or Time Capsule!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filteredPosts, key = { it.id }) { post ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 640.dp)
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        SocialPostCard(
                            post = post,
                            comments = commentsByPost[post.id].orEmpty(),
                            currentUsername = currentUsername,
                            onLikeClick = { onToggleLike(post) },
                            onBookmarkClick = { onToggleBookmark(post) },
                            onFollowClick = { onToggleFollow(post) },
                            onVotePoll = { choice -> onVotePoll(post, choice) },
                            onCopyCaption = {
                                val combined = if (post.hashtags.isNotBlank()) {
                                    "${post.caption}\n\n${post.hashtags}"
                                } else post.caption
                                onCopyText("Caption", combined)
                            },
                            onTranslateCaption = { onTranslateText(post.caption) },
                            onSaveToVault = { onSavePostToVault(post) },
                            onShareClick = { onSharePost(post) },
                            onOpenComments = { activeCommentsPostId = post.id }
                        )
                    }
                }
            }
        }

        InstaPlusQuickDock(
            visible = settings.showFloatingActionDock,
            isGhostActive = settings.isGhostModeAnyActive,
            onCreateClick = onOpenCreateComposer,
            onDownloadClick = onOpenDownloadSheet,
            onTranslateClick = { onTranslateText("") },
            onGhostToggleClick = onToggleGhostClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 14.dp, bottom = 18.dp)
        )
    }

    activeCommentsPostId?.let { targetId ->
        val post = posts.firstOrNull { it.id == targetId }
        if (post != null) {
            CommentsBottomSheet(
                post = post,
                comments = commentsByPost[targetId].orEmpty(),
                onAddComment = { text -> onAddComment(targetId, text) },
                onCopyText = { txt -> onCopyText("Comment", txt) },
                onTranslateText = { txt ->
                    activeCommentsPostId = null
                    onTranslateText(txt)
                },
                onDismiss = { activeCommentsPostId = null }
            )
        }
    }

    activeStory?.let { story ->
        StoryViewerDialog(
            story = story,
            isGhostStoryActive = settings.anonymousStoryViewing,
            onSaveStory = { onSaveStoryToVault(story) },
            onDismiss = { activeStory = null }
        )
    }
}

@Composable
private fun StoryViewerDialog(
    story: StoryEntity,
    isGhostStoryActive: Boolean,
    onSaveStory: () -> Unit,
    onDismiss: () -> Unit
) {
    var progress by remember(story.id) { mutableStateOf(0f) }

    LaunchedEffect(story.id) {
        val steps = 60
        for (i in 1..steps) {
            delay(85L)
            progress = i / steps.toFloat()
        }
        onDismiss()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(onClick = onDismiss)
        ) {
            StandaloneMediaArtwork(
                mediaKey = story.mediaKey,
                contentDescription = story.caption,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.65f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.72f)
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 22.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(3.dp),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.3f)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "@${story.authorUsername}",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (isGhostStoryActive) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.VisibilityOff,
                                    contentDescription = "Ghost Story Shield Active",
                                    tint = GhostPurple,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Anonymous Viewing (No 'Seen' Sent)",
                                    color = Color(0xFFD8B4FE),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                    Row {
                        IconButton(onClick = onSaveStory) {
                            Icon(
                                imageVector = Icons.Filled.Download,
                                contentDescription = "Save Story to Vault",
                                tint = Color.White
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Close Story",
                                tint = Color.White
                            )
                        }
                    }
                }
            }

            if (story.caption.isNotBlank()) {
                Surface(
                    color = Color.Black.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 32.dp)
                ) {
                    Text(
                        text = story.caption,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }
    }
}
