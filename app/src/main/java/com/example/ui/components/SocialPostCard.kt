package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.PostCommentEntity
import com.example.data.local.SocialPostEntity
import com.example.ui.theme.InstagramStoryBrush
import kotlinx.coroutines.delay

@Composable
fun SocialPostCard(
    post: SocialPostEntity,
    comments: List<PostCommentEntity>,
    currentUsername: String,
    onLikeClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    onFollowClick: () -> Unit,
    onVotePoll: (Int) -> Unit,
    onCopyCaption: () -> Unit,
    onTranslateCaption: () -> Unit,
    onSaveToVault: () -> Unit,
    onShareClick: () -> Unit,
    onOpenComments: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showHeartOverlay by remember { mutableStateOf(false) }

    LaunchedEffect(showHeartOverlay) {
        if (showHeartOverlay) {
            delay(650L)
            showHeartOverlay = false
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 1.dp,
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 0.75.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                shape = RoundedCornerShape(18.dp)
            )
            .testTag("social_post_card_${post.id}")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Post Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .border(2.dp, InstagramStoryBrush, CircleShape)
                        .background(Color(post.authorAvatarColorHex).copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = post.authorUsername.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(post.authorAvatarColorHex)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "@${post.authorUsername}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (post.moodTag.isNotBlank()) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(50)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Mood,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = post.moodTag,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        text = "${post.authorFullName} • ${post.categoryTag}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }

                if (post.expiresAtMillis != null) {
                    val remainingHours = ((post.expiresAtMillis - System.currentTimeMillis()) / 3_600_000L)
                        .coerceAtLeast(1L)
                    Surface(
                        color = Color(0xFFF58529).copy(alpha = 0.16f),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.HourglassBottom,
                                contentDescription = "Time Capsule expires soon",
                                tint = Color(0xFFF58529),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "${remainingHours}h TTL",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFF58529),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (!post.authorUsername.equals(currentUsername, ignoreCase = true)) {
                    TextButton(
                        onClick = onFollowClick,
                        modifier = Modifier.testTag("follow_author_${post.id}")
                    ) {
                        Text(
                            text = if (post.isFollowingAuthor) "Following" else "Follow",
                            fontWeight = FontWeight.SemiBold,
                            color = if (post.isFollowingAuthor) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Media Surface with Double-Tap Like
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(if (post.isReel) 0.9f else 1.25f)
                    .pointerInput(post.id, post.isLiked) {
                        detectTapGestures(
                            onDoubleTap = {
                                showHeartOverlay = true
                                if (!post.isLiked) onLikeClick()
                            }
                        )
                    }
            ) {
                StandaloneMediaArtwork(
                    mediaKey = post.mediaKey,
                    contentDescription = post.caption,
                    modifier = Modifier.matchParentSize()
                )
                this@Column.AnimatedVisibility(
                    visible = showHeartOverlay,
                    enter = fadeIn() + scaleIn(spring()),
                    exit = fadeOut() + scaleOut(),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = null,
                        tint = Color(0xFFED4956),
                        modifier = Modifier.size(84.dp)
                    )
                }
            }

            // Action Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onLikeClick,
                        modifier = Modifier.testTag("like_post_${post.id}")
                    ) {
                        Icon(
                            imageVector = if (post.isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Like post",
                            tint = if (post.isLiked) Color(0xFFED4956) else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = onOpenComments,
                        modifier = Modifier.testTag("comment_post_${post.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ChatBubbleOutline,
                            contentDescription = "Comments (${comments.size})"
                        )
                    }
                    IconButton(
                        onClick = onShareClick,
                        modifier = Modifier.testTag("share_post_${post.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = "Share post"
                        )
                    }
                    IconButton(
                        onClick = onCopyCaption,
                        modifier = Modifier.testTag("copy_caption_${post.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ContentCopy,
                            contentDescription = "Copy caption & hashtags",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onTranslateCaption,
                        modifier = Modifier.testTag("translate_post_${post.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Translate,
                            contentDescription = "AI Translate caption",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onSaveToVault,
                        modifier = Modifier.testTag("download_post_${post.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Download,
                            contentDescription = "Save HD to Gallery & Vault",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onBookmarkClick,
                        modifier = Modifier.testTag("bookmark_post_${post.id}")
                    ) {
                        Icon(
                            imageVector = if (post.isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "Bookmark post",
                            tint = if (post.isBookmarked) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Likes & Caption
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .padding(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "${post.likesCount} likes • ${comments.size} comments",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "@${post.authorUsername}  ${post.caption}",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (post.hashtags.isNotBlank()) {
                    Text(
                        text = post.hashtags,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Exclusive Live Poll Card
                if (post.pollQuestion.isNotBlank() && post.pollOptionA.isNotBlank() && post.pollOptionB.isNotBlank()) {
                    FeedPollWidget(
                        question = post.pollQuestion,
                        optionA = post.pollOptionA,
                        optionB = post.pollOptionB,
                        votesA = post.pollVotesA,
                        votesB = post.pollVotesB,
                        userVote = post.userPollVote,
                        onVote = onVotePoll
                    )
                }

                if (comments.isNotEmpty()) {
                    val latest = comments.last()
                    Text(
                        text = "View all ${comments.size} comments • @${latest.authorUsername}: ${latest.text}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onOpenComments)
                            .padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FeedPollWidget(
    question: String,
    optionA: String,
    optionB: String,
    votesA: Int,
    votesB: Int,
    userVote: Int,
    onVote: (Int) -> Unit
) {
    val totalVotes = (votesA + votesB).coerceAtLeast(1)
    val fracA by animateFloatAsState(targetValue = votesA.toFloat() / totalVotes, label = "pollA")
    val fracB by animateFloatAsState(targetValue = votesB.toFloat() / totalVotes, label = "pollB")

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.HowToVote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = question,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
            PollOptionBar(
                label = optionA,
                percent = ((votesA * 100f) / totalVotes).toInt(),
                fraction = if (userVote != 0) fracA else 0f,
                isSelected = userVote == 1,
                showPercentages = userVote != 0,
                onClick = { onVote(1) }
            )
            PollOptionBar(
                label = optionB,
                percent = ((votesB * 100f) / totalVotes).toInt(),
                fraction = if (userVote != 0) fracB else 0f,
                isSelected = userVote == 2,
                showPercentages = userVote != 0,
                onClick = { onVote(2) }
            )
            Text(
                text = if (userVote == 0) "Tap an option to vote" else "${votesA + votesB} total votes",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PollOptionBar(
    label: String,
    percent: Int,
    fraction: Float,
    isSelected: Boolean,
    showPercentages: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 42.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
    ) {
        if (showPercentages) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .matchParentSize()
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    )
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (isSelected) "$label ✓" else label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
            if (showPercentages) {
                Text(
                    text = "$percent%",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsBottomSheet(
    post: SocialPostEntity,
    comments: List<PostCommentEntity>,
    onAddComment: (String) -> Unit,
    onCopyText: (String) -> Unit,
    onTranslateText: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var draft by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Comments on @${post.authorUsername}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (comments.isEmpty()) {
                    item {
                        Text(
                            text = "No comments yet. Start the conversation below!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                } else {
                    items(comments, key = { it.id }) { comment ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "@${comment.authorUsername}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = comment.text,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                IconButton(onClick = { onCopyText(comment.text) }) {
                                    Icon(
                                        imageVector = Icons.Filled.ContentCopy,
                                        contentDescription = "Copy comment",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(onClick = { onTranslateText(comment.text) }) {
                                    Icon(
                                        imageVector = Icons.Filled.Translate,
                                        contentDescription = "Translate comment",
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    placeholder = { Text("Add a comment...") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("comment_draft_input")
                )
                Button(
                    onClick = {
                        onAddComment(draft)
                        draft = ""
                    },
                    enabled = draft.isNotBlank(),
                    modifier = Modifier.testTag("submit_comment_button")
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "Post comment")
                }
            }
        }
    }
}
