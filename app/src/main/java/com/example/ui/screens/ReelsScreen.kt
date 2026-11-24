package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
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
import androidx.compose.ui.unit.dp
import com.example.data.local.PostCommentEntity
import com.example.data.local.SocialPostEntity
import com.example.ui.components.CommentsBottomSheet
import com.example.ui.components.StandaloneMediaArtwork
import kotlinx.coroutines.delay

@Composable
fun ReelsScreen(
    posts: List<SocialPostEntity>,
    comments: List<PostCommentEntity>,
    onToggleLike: (SocialPostEntity) -> Unit,
    onToggleBookmark: (SocialPostEntity) -> Unit,
    onAddComment: (Long, String) -> Unit,
    onCopyText: (String, String) -> Unit,
    onTranslateText: (String) -> Unit,
    onSaveToVault: (SocialPostEntity) -> Unit,
    onSharePost: (SocialPostEntity) -> Unit,
    onCreateReelClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val reelList = remember(posts) {
        val reelsFirst = posts.sortedByDescending { it.isReel }
        reelsFirst
    }
    var commentTargetId by remember { mutableStateOf<Long?>(null) }

    if (reelList.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("No Reels published yet", style = MaterialTheme.typography.titleMedium)
                Button(onClick = onCreateReelClick) {
                    Icon(Icons.Filled.AddBox, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Create Reel")
                }
            }
        }
        return
    }

    val pagerState = rememberPagerState(pageCount = { reelList.size })
    val commentsByPost = remember(comments) { comments.groupBy { it.postId } }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        VerticalPager(
            state = pagerState,
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.fillMaxSize()
        ) { pageIndex ->
            val reel = reelList[pageIndex]
            var isPlaying by remember(reel.id) { mutableStateOf(true) }
            var isMuted by remember(reel.id) { mutableStateOf(false) }
            var playbackProgress by remember(reel.id) { mutableStateOf(0f) }

            LaunchedEffect(reel.id, isPlaying) {
                while (isPlaying) {
                    delay(120L)
                    playbackProgress = (playbackProgress + 0.015f).let { if (it > 1f) 0f else it }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { isPlaying = !isPlaying }
                    .testTag("reel_page_${reel.id}")
            ) {
                StandaloneMediaArtwork(
                    mediaKey = reel.mediaKey,
                    contentDescription = reel.caption,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.35f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.8f)
                                )
                            )
                        )
                )

                // Top Header
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = if (reel.isReel) "InstaPlus Reels • HD" else "Featured Motion Post",
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(
                            onClick = { isMuted = !isMuted },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.45f))
                        ) {
                            Icon(
                                imageVector = if (isMuted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                                contentDescription = if (isMuted) "Unmute audio" else "Mute audio",
                                tint = Color.White
                            )
                        }
                        IconButton(
                            onClick = onCreateReelClick,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.45f))
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AddBox,
                                contentDescription = "Create Reel",
                                tint = Color.White
                            )
                        }
                    }
                }

                if (!isPlaying) {
                    Icon(
                        imageVector = Icons.Filled.PlayCircle,
                        contentDescription = "Paused",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(64.dp)
                    )
                }

                // Bottom Caption & Right Action Column
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 10.dp, bottom = 20.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "@${reel.authorUsername}",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = reel.caption,
                            color = Color.White.copy(alpha = 0.94f),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 3
                        )
                        if (reel.hashtags.isNotBlank()) {
                            Text(
                                text = reel.hashtags,
                                color = Color(0xFF93C5FD),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                        Surface(
                            color = Color.Black.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(50)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.MusicNote,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = reel.audioTrack.ifBlank { "@${reel.authorUsername} • Original Audio" },
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ReelActionIcon(
                            icon = if (reel.isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            label = reel.likesCount.toString(),
                            tint = if (reel.isLiked) Color(0xFFED4956) else Color.White,
                            onClick = { onToggleLike(reel) }
                        )
                        ReelActionIcon(
                            icon = Icons.Outlined.ChatBubbleOutline,
                            label = (commentsByPost[reel.id]?.size ?: 0).toString(),
                            tint = Color.White,
                            onClick = { commentTargetId = reel.id }
                        )
                        ReelActionIcon(
                            icon = Icons.Filled.Download,
                            label = "Save HD",
                            tint = Color(0xFF38BDF8),
                            onClick = { onSaveToVault(reel) }
                        )
                        ReelActionIcon(
                            icon = Icons.Filled.Translate,
                            label = "Translate",
                            tint = Color.White,
                            onClick = { onTranslateText(reel.caption) }
                        )
                        ReelActionIcon(
                            icon = if (reel.isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            label = "Bookmark",
                            tint = if (reel.isBookmarked) Color(0xFFFBBF24) else Color.White,
                            onClick = { onToggleBookmark(reel) }
                        )
                        ReelActionIcon(
                            icon = Icons.Filled.Share,
                            label = "Share",
                            tint = Color.White,
                            onClick = { onSharePost(reel) }
                        )
                    }
                }

                LinearProgressIndicator(
                    progress = { playbackProgress },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(3.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.White.copy(alpha = 0.2f)
                )
            }
        }
    }

    commentTargetId?.let { targetId ->
        val post = reelList.firstOrNull { it.id == targetId }
        if (post != null) {
            CommentsBottomSheet(
                post = post,
                comments = commentsByPost[targetId].orEmpty(),
                onAddComment = { text -> onAddComment(targetId, text) },
                onCopyText = { txt -> onCopyText("Comment", txt) },
                onTranslateText = { txt ->
                    commentTargetId = null
                    onTranslateText(txt)
                },
                onDismiss = { commentTargetId = null }
            )
        }
    }
}

@Composable
private fun ReelActionIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onClick) {
            Icon(imageVector = icon, contentDescription = label, tint = tint, modifier = Modifier.size(28.dp))
        }
        Text(text = label, color = Color.White, style = MaterialTheme.typography.labelSmall)
    }
}
