package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.ai.AiCaptionUiState
import com.example.ui.components.StandaloneMediaArtwork

private val mediaOptions = listOf(
    "gradient:sunset" to "Sunset Card",
    "gradient:ocean" to "Ocean Card",
    "gradient:violet" to "Prism Violet",
    "drawable:img_post_neon_tokyo" to "Neon City",
    "drawable:img_post_alpine_sunrise" to "Alpine Peak",
    "drawable:img_post_studio_desk" to "Creator Desk",
    "drawable:img_reel_cosmic_prism" to "Cosmic Prism"
)

private val moodOptions = listOf("", "Focused", "Wanderlust", "Vibrant", "Chill", "Grateful")
private val categoryOptions = listOf("Lifestyle", "Travel", "Nature", "Design", "Tech")
private val ttlOptions = listOf(null to "Permanent", 1 to "1h Capsule", 24 to "24h Capsule")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostComposerSheet(
    aiCaptionState: AiCaptionUiState,
    onGenerateAiCaption: (topic: String, tone: String) -> Unit,
    onPublish: (
        asStory: Boolean,
        asReel: Boolean,
        mediaKey: String,
        caption: String,
        hashtags: String,
        categoryTag: String,
        moodTag: String,
        pollQuestion: String,
        pollOptionA: String,
        pollOptionB: String,
        ttlHours: Int?
    ) -> Unit,
    onDismiss: () -> Unit
) {
    var publishKind by remember { mutableStateOf(0) } // 0 = Post, 1 = Story, 2 = Reel
    var selectedMediaKey by remember { mutableStateOf("gradient:sunset") }
    var caption by remember { mutableStateOf("") }
    var hashtags by remember { mutableStateOf("#InstaPlus") }
    var categoryTag by remember { mutableStateOf("Lifestyle") }
    var moodTag by remember { mutableStateOf("Focused") }
    var ttlHours by remember { mutableStateOf<Int?>(null) }

    var includePoll by remember { mutableStateOf(false) }
    var pollQuestion by remember { mutableStateOf("") }
    var pollOptionA by remember { mutableStateOf("") }
    var pollOptionB by remember { mutableStateOf("") }

    var aiTopic by remember { mutableStateOf("") }
    var aiTone by remember { mutableStateOf("Aesthetic") }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> if (uri != null) selectedMediaKey = uri.toString() }
    )

    LaunchedEffect(aiCaptionState) {
        if (aiCaptionState is AiCaptionUiState.Success) {
            caption = aiCaptionState.caption
            hashtags = aiCaptionState.hashtags
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Create on InstaPlus",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0 to "Feed Post", 1 to "24h Story", 2 to "Reel").forEach { (idx, label) ->
                    FilterChip(
                        selected = publishKind == idx,
                        onClick = { publishKind = idx },
                        label = { Text(label) },
                        modifier = Modifier.testTag("composer_kind_$idx")
                    )
                }
            }

            // Media Picker & Artwork Presets
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StandaloneMediaArtwork(
                    mediaKey = selectedMediaKey,
                    contentDescription = "Selected post artwork",
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(14.dp))
                )
                OutlinedButton(
                    onClick = {
                        photoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Choose Photo")
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                mediaOptions.forEach { (key, label) ->
                    val selected = selectedMediaKey == key
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .border(
                                width = if (selected) 1.5.dp else 0.5.dp,
                                color = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { selectedMediaKey = key }
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            // Exclusive AI Caption & Hashtag Studio
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
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
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "AI Caption & Hashtag Studio",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = aiTopic,
                            onValueChange = { aiTopic = it },
                            placeholder = { Text("Describe topic (e.g. evening coffee)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = { onGenerateAiCaption(aiTopic, aiTone) },
                            enabled = aiTopic.isNotBlank() && aiCaptionState !is AiCaptionUiState.Loading
                        ) {
                            if (aiCaptionState is AiCaptionUiState.Loading) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Text("Generate")
                            }
                        }
                    }
                    if (aiCaptionState is AiCaptionUiState.Error) {
                        Text(
                            text = aiCaptionState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            OutlinedTextField(
                value = caption,
                onValueChange = { caption = it },
                label = { Text("Caption") },
                minLines = 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("composer_caption_input")
            )

            if (publishKind != 1) {
                OutlinedTextField(
                    value = hashtags,
                    onValueChange = { hashtags = it },
                    label = { Text("Hashtags") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Mood Tag Picker
            Text("Mood Check-In", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                moodOptions.forEach { m ->
                    FilterChip(
                        selected = moodTag == m,
                        onClick = { moodTag = m },
                        label = { Text(m.ifBlank { "None" }) }
                    )
                }
            }

            if (publishKind != 1) {
                Text("Category", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categoryOptions.forEach { c ->
                        FilterChip(
                            selected = categoryTag == c,
                            onClick = { categoryTag = c },
                            label = { Text(c) }
                        )
                    }
                }

                // Exclusive Time Capsule Auto-Expire TTL
                Text("Time Capsule Auto-Expire (Exclusive)", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ttlOptions.forEach { (hours, label) ->
                        FilterChip(
                            selected = ttlHours == hours,
                            onClick = { ttlHours = hours },
                            label = { Text(label) }
                        )
                    }
                }

                // Exclusive Feed Poll Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Attach Interactive Poll", style = MaterialTheme.typography.labelLarge)
                        Text(
                            "Let followers vote directly inside your feed card",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = includePoll, onCheckedChange = { includePoll = it })
                }

                AnimatedVisibility(visible = includePoll) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = pollQuestion,
                            onValueChange = { pollQuestion = it },
                            label = { Text("Poll Question") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = pollOptionA,
                                onValueChange = { pollOptionA = it },
                                label = { Text("Option A") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = pollOptionB,
                                onValueChange = { pollOptionB = it },
                                label = { Text("Option B") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            Button(
                onClick = {
                    onPublish(
                        publishKind == 1,
                        publishKind == 2,
                        selectedMediaKey,
                        caption,
                        hashtags,
                        categoryTag,
                        moodTag,
                        if (includePoll) pollQuestion else "",
                        if (includePoll) pollOptionA else "",
                        if (includePoll) pollOptionB else "",
                        ttlHours
                    )
                },
                enabled = caption.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("composer_publish_button")
            ) {
                Text(
                    when (publishKind) {
                        1 -> "Share Story"
                        2 -> "Publish Reel"
                        else -> if (ttlHours != null) "Publish Time Capsule (${ttlHours}h)" else "Publish Post"
                    }
                )
            }
        }
    }
}
