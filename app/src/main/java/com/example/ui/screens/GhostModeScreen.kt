package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.PublicOff
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.GhostModePreferences
import com.example.data.model.GhostPrivacyCategory
import com.example.data.model.InterceptedPrivacyRequest
import com.example.ui.theme.AdShieldGreen
import com.example.ui.theme.GhostPurple
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GhostModeScreen(
    preferences: GhostModePreferences,
    interceptedRequests: List<InterceptedPrivacyRequest>,
    blockedCounts: Map<GhostPrivacyCategory, Int>,
    onBackClick: () -> Unit,
    onMasterToggle: (Boolean) -> Unit,
    onStorySeenToggle: (Boolean) -> Unit,
    onDmSeenToggle: (Boolean) -> Unit,
    onTypingToggle: (Boolean) -> Unit,
    onPresenceToggle: (Boolean) -> Unit,
    onLoggingToggle: (Boolean) -> Unit,
    onClearLog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalBlocked = blockedCounts.values.sum()
    val storyBlocked = blockedCounts[GhostPrivacyCategory.STORY_SEEN] ?: 0
    val dmBlocked = blockedCounts[GhostPrivacyCategory.DM_READ_RECEIPT] ?: 0
    val activityBlocked = (blockedCounts[GhostPrivacyCategory.TYPING_INDICATOR] ?: 0) +
        (blockedCounts[GhostPrivacyCategory.ONLINE_PRESENCE] ?: 0)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = {
                Column {
                    Text("Ghost Mode Shield", fontWeight = FontWeight.Bold)
                    Text(
                        text = if (preferences.isAnyActive) {
                            "${preferences.activeRuleCount} active privacy filters"
                        } else {
                            "All privacy filters paused"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (preferences.isAnyActive) GhostPurple else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            navigationIcon = {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.testTag("ghost_mode_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            ),
            modifier = Modifier.statusBarsPadding()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 680.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Hero Stealth Shield Banner
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    tonalElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = if (preferences.isAnyActive) GhostPurple.copy(alpha = 0.55f)
                            else MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(22.dp)
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        GhostPurple.copy(alpha = if (preferences.isAnyActive) 0.18f else 0.06f),
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                        MaterialTheme.colorScheme.surface
                                    )
                                )
                            )
                            .padding(18.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (preferences.isAnyActive) GhostPurple
                                                else MaterialTheme.colorScheme.surfaceVariant
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.VisibilityOff,
                                            contentDescription = "Ghost Mode Shield",
                                            tint = if (preferences.isAnyActive) Color.White
                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = if (preferences.isAnyActive) "Stealth Interception Active"
                                            else "Stealth Interception Paused",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Native Social Engine & OkHttp packets filtered locally",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Switch(
                                    checked = preferences.isAnyActive,
                                    onCheckedChange = onMasterToggle,
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = GhostPurple
                                    ),
                                    modifier = Modifier.testTag("ghost_master_switch")
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                GhostStatTile(
                                    label = "Total Blocked",
                                    value = totalBlocked.toString(),
                                    accent = GhostPurple,
                                    modifier = Modifier.weight(1f)
                                )
                                GhostStatTile(
                                    label = "Story Seen",
                                    value = storyBlocked.toString(),
                                    accent = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f)
                                )
                                GhostStatTile(
                                    label = "DM Seen",
                                    value = dmBlocked.toString(),
                                    accent = AdShieldGreen,
                                    modifier = Modifier.weight(1f)
                                )
                                GhostStatTile(
                                    label = "Presence",
                                    value = activityBlocked.toString(),
                                    accent = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // Privacy Interception Rules Card
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Security,
                                contentDescription = null,
                                tint = GhostPurple,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Network Interception Rules",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        GhostRuleToggleRow(
                            icon = Icons.Filled.AutoAwesome,
                            title = "Hide Story 'Seen' Status",
                            subtitle = "Block /stories/reel/seen, stories_seen & GraphQL PolarisStoriesV3Seen payloads",
                            checked = preferences.hideStorySeen,
                            testTag = "ghost_toggle_story_seen",
                            onCheckedChange = onStorySeenToggle
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))

                        GhostRuleToggleRow(
                            icon = Icons.Filled.ChatBubble,
                            title = "Hide DM 'Seen' Read Receipts",
                            subtitle = "Intercept /direct_v2/.../seen, mark_seen & mark_thread_seen calls",
                            checked = preferences.hideDmReadReceipts,
                            testTag = "ghost_toggle_dm_seen",
                            onCheckedChange = onDmSeenToggle
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))

                        GhostRuleToggleRow(
                            icon = Icons.Filled.Keyboard,
                            title = "Suppress Typing Indicator",
                            subtitle = "Drop indicate_activity & is_typing packets in active chats",
                            checked = preferences.hideTypingIndicator,
                            testTag = "ghost_toggle_typing",
                            onCheckedChange = onTypingToggle
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))

                        GhostRuleToggleRow(
                            icon = Icons.Filled.PublicOff,
                            title = "Mask Online Presence",
                            subtitle = "Drop /presence, get_presence & active_status heartbeats",
                            checked = preferences.hideOnlineStatus,
                            testTag = "ghost_toggle_presence",
                            onCheckedChange = onPresenceToggle
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))

                        GhostRuleToggleRow(
                            icon = Icons.Filled.History,
                            title = "Record Live Interception Log",
                            subtitle = "Display blocked Story & DM receipts below (stays on device)",
                            checked = preferences.logBlockedRequests,
                            testTag = "ghost_toggle_log_intercepts",
                            onCheckedChange = onLoggingToggle
                        )
                    }
                }

                // Live Network Interception Inspector
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Shield,
                                    contentDescription = null,
                                    tint = AdShieldGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Live Interception Inspector",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            AnimatedVisibility(visible = interceptedRequests.isNotEmpty() || totalBlocked > 0) {
                                OutlinedButton(
                                    onClick = onClearLog,
                                    modifier = Modifier.testTag("ghost_clear_log_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.DeleteSweep,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Clear")
                                }
                            }
                        }

                        if (interceptedRequests.isEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.VisibilityOff,
                                        contentDescription = null,
                                        tint = GhostPurple.copy(alpha = 0.7f),
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Text(
                                        text = "No blocked seen-receipts recorded yet",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Open Stories or Direct Messages in the feed — intercepted seen, typing, and presence requests will appear here with synthetic 200 OK responses.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                interceptedRequests.forEach { entry ->
                                    InterceptedRequestRow(entry = entry)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GhostStatTile(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = accent
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun GhostRuleToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (checked) GhostPurple.copy(alpha = 0.14f)
                    else MaterialTheme.colorScheme.surface
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (checked) GhostPurple else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = GhostPurple
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
private fun InterceptedRequestRow(entry: InterceptedPrivacyRequest) {
    val timeLabel = remember(entry.timestampMillis) {
        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(entry.timestampMillis))
    }
    val badgeColor = when (entry.category) {
        GhostPrivacyCategory.STORY_SEEN -> MaterialTheme.colorScheme.primary
        GhostPrivacyCategory.DM_READ_RECEIPT -> AdShieldGreen
        GhostPrivacyCategory.TYPING_INDICATOR -> GhostPurple
        GhostPrivacyCategory.ONLINE_PRESENCE -> MaterialTheme.colorScheme.secondary
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = badgeColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = entry.category.shortBadge,
                            style = MaterialTheme.typography.labelSmall,
                            color = badgeColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "${entry.transport} • ${entry.method}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = timeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = entry.endpoint,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${entry.reason} → 200 OK (synthetic)",
                style = MaterialTheme.typography.labelSmall,
                color = AdShieldGreen
            )
        }
    }
}
