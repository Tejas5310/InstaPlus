package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.EnhancedEncryption
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.example.data.local.LockedChatEntity
import com.example.data.model.AccentColorPreset
import com.example.data.model.ChatBackgroundPreset
import com.example.data.model.FontPreset
import com.example.data.model.InstaPlusSettings
import com.example.data.model.LauncherIconVariant
import com.example.data.model.ThemePreset
import com.example.system.BiometricAvailability
import com.example.system.BiometricHelper
import com.example.ui.theme.AdShieldGreen
import com.example.ui.theme.GhostPurple
import com.example.ui.theme.InstagramStoryBrush
import com.example.ui.theme.fontFamilyForPreset
import com.example.ui.viewmodel.CloudSyncState
import com.example.ui.viewmodel.PinSetupMode

private val modLanguages = listOf("English", "Hindi", "Spanish", "French", "Arabic", "Portuguese", "German", "Japanese")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModHubScreen(
    settings: InstaPlusSettings,
    lockedChats: List<LockedChatEntity>,
    accountCount: Int,
    vaultCount: Int,
    blockedGhostCount: Int,
    cloudSyncState: CloudSyncState,
    onSyncNow: () -> Unit,
    onSignOut: () -> Unit,
    onBackClick: () -> Unit,
    onOpenGhostMode: () -> Unit,
    onBooleanSettingChange: (String, Boolean) -> Unit,
    onDarkModeToggle: (Boolean) -> Unit,
    onThemeSelect: (ThemePreset) -> Unit,
    onAccentSelect: (AccentColorPreset) -> Unit,
    onFontSelect: (FontPreset) -> Unit,
    onChatBgSelect: (ChatBackgroundPreset) -> Unit,
    onCustomChatBgPicked: (Uri) -> Unit,
    onLauncherIconSelect: (LauncherIconVariant) -> Unit,
    onOpenPinSetup: (PinSetupMode) -> Unit,
    onAddLockedChat: (String, String) -> Unit,
    onRemoveLockedChat: (Long) -> Unit,
    onTargetLanguageSelect: (String) -> Unit,
    onFocusLimitSelect: (Int) -> Unit,
    onOpenTranslator: () -> Unit,
    onOpenVault: () -> Unit,
    onOpenAccountSwitcher: () -> Unit,
    modifier: Modifier = Modifier
) {
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> if (uri != null) onCustomChatBgPicked(uri) }
    )

    var newLockHandle by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = { Text("InstaPlus Mod Hub") },
            navigationIcon = {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.testTag("mod_hub_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Feed"
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
                // 0. InstaPlus Account & Encrypted Storage
                ModSectionCard(
                    title = "InstaPlus Account & Security",
                    subtitle = "Encrypted DataStore vault + PBKDF2-HMAC-SHA256 credentials",
                    icon = Icons.Filled.CloudDone,
                    accentColor = MaterialTheme.colorScheme.primary
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.EnhancedEncryption,
                                    contentDescription = "Hardware encryption active",
                                    tint = AdShieldGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "@${cloudSyncState.displayName.ifBlank { "instaplus_user" }}",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            if (cloudSyncState.mobileNumber.isNotBlank()) {
                                Text(
                                    text = "Mobile: ${cloudSyncState.mobileNumber}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (!cloudSyncState.email.isNullOrBlank()) {
                                Text(
                                    text = "Email ID: ${cloudSyncState.email}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "Encrypted DataStore vault • Account & PIN use 100k PBKDF2-SHA256",
                                style = MaterialTheme.typography.labelSmall,
                                color = AdShieldGreen
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (cloudSyncState.isCloudLinked) {
                            Button(
                                onClick = onSyncNow,
                                enabled = !cloudSyncState.isSyncing,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("cloud_sync_now_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Sync,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (cloudSyncState.isSyncing) "Syncing..." else "Sync Cloud")
                            }
                        } else {
                            Button(
                                onClick = onOpenAccountSwitcher,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("mod_switch_account_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AccountCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Switch Account")
                            }
                        }
                        OutlinedButton(
                            onClick = onSignOut,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("sign_out_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Log Out")
                        }
                    }
                }

                // 1. Ghost Mode & Privacy
                ModSectionCard(
                    title = "Ghost Mode & Privacy",
                    subtitle = "Browse anonymously without sending activity signals",
                    icon = Icons.Filled.VisibilityOff,
                    accentColor = GhostPurple
                ) {
                    ModSwitchRow(
                        title = "Hide Online Status",
                        subtitle = "Freeze active status & presence pings",
                        checked = settings.hideOnlineStatus,
                        testTag = "toggle_hide_online",
                        onCheckedChange = { onBooleanSettingChange("hide_online_status", it) }
                    )
                    ModSwitchRow(
                        title = "Anonymous Story Viewing",
                        subtitle = "View Stories without appearing in viewer lists",
                        checked = settings.anonymousStoryViewing,
                        testTag = "toggle_anon_stories",
                        onCheckedChange = { onBooleanSettingChange("anonymous_story_viewing", it) }
                    )
                    ModSwitchRow(
                        title = "Disable Read Receipts",
                        subtitle = "Read DMs without sending Seen status",
                        checked = settings.disableReadReceipts,
                        testTag = "toggle_disable_seen",
                        onCheckedChange = { onBooleanSettingChange("disable_read_receipts", it) }
                    )
                    ModSwitchRow(
                        title = "Hide Typing Indicator",
                        subtitle = "Prevent 'Typing...' status in chats",
                        checked = settings.hideTypingIndicator,
                        testTag = "toggle_hide_typing",
                        onCheckedChange = { onBooleanSettingChange("hide_typing_indicator", it) }
                    )
                    OutlinedButton(
                        onClick = onOpenGhostMode,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("open_ghost_mode_module_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.VisibilityOff,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ghost Mode Shield & Network Log ($blockedGhostCount blocked)")
                    }
                }

                // 2. Ad-Free Browsing
                ModSectionCard(
                    title = "Ad-Free Browsing",
                    subtitle = "Strip sponsored cards across feed, Stories & Reels",
                    icon = Icons.Filled.Block,
                    accentColor = AdShieldGreen
                ) {
                    ModSwitchRow(
                        title = "Block Feed Sponsored Posts",
                        subtitle = "Hide promoted posts & paid partnerships",
                        checked = settings.blockFeedAds,
                        testTag = "toggle_block_feed_ads",
                        onCheckedChange = { onBooleanSettingChange("block_feed_ads", it) }
                    )
                    ModSwitchRow(
                        title = "Block Story Ads",
                        subtitle = "Skip sponsored interstitial Stories",
                        checked = settings.blockStoryAds,
                        testTag = "toggle_block_story_ads",
                        onCheckedChange = { onBooleanSettingChange("block_story_ads", it) }
                    )
                    ModSwitchRow(
                        title = "Block Reels Ads",
                        subtitle = "Filter promoted Reels ads",
                        checked = settings.blockReelsAds,
                        testTag = "toggle_block_reels_ads",
                        onCheckedChange = { onBooleanSettingChange("block_reels_ads", it) }
                    )
                    ModSwitchRow(
                        title = "Hide Suggested Posts",
                        subtitle = "Clean home feed of algorithmic suggestions",
                        checked = settings.hideSuggestedPosts,
                        testTag = "toggle_hide_suggested",
                        onCheckedChange = { onBooleanSettingChange("hide_suggested_posts", it) }
                    )
                }

                // 3. Built-In Media Downloader
                ModSectionCard(
                    title = "Media Downloader",
                    subtitle = "Save Reels, Stories, photos, videos & avatars to Gallery",
                    icon = Icons.Filled.Download,
                    accentColor = MaterialTheme.colorScheme.primary
                ) {
                    ModSwitchRow(
                        title = "High-Resolution Priority",
                        subtitle = "Always resolve largest srcset & HD streams",
                        checked = settings.highResPriority,
                        testTag = "toggle_high_res",
                        onCheckedChange = { onBooleanSettingChange("high_res_priority", it) }
                    )
                    ModSwitchRow(
                        title = "Show Floating Quick Dock",
                        subtitle = "One-tap Download, Translate & Ghost pill on feed",
                        checked = settings.showFloatingActionDock,
                        testTag = "toggle_floating_dock",
                        onCheckedChange = { onBooleanSettingChange("show_floating_dock", it) }
                    )
                    OutlinedButton(
                        onClick = onOpenVault,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("mod_open_vault_button")
                    ) {
                        Icon(imageVector = Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open Downloads Vault ($vaultCount saved)")
                    }
                }

                // 4. UI & Theme Customization
                ModSectionCard(
                    title = "UI & Theme Studio",
                    subtitle = "Light/Dark mode, primary color accents, fonts, chat wallpaper & icon",
                    icon = Icons.Filled.Palette,
                    accentColor = MaterialTheme.colorScheme.primary
                ) {
                    ModSwitchRow(
                        title = "Dark Mode",
                        subtitle = "Toggle Light or AMOLED Dark surfaces (saved in DataStore)",
                        checked = settings.isDarkMode,
                        testTag = "toggle_dark_mode",
                        onCheckedChange = onDarkModeToggle
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

                    Text("Primary Accent Color", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AccentColorPreset.entries.forEach { accent ->
                            val selected = settings.accentPreset == accent
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (selected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .border(
                                        width = if (selected) 2.dp else 1.dp,
                                        color = if (selected) accent.primaryColor else MaterialTheme.colorScheme.outline,
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clickable { onAccentSelect(accent) }
                                    .testTag("accent_preset_${accent.name.lowercase()}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(accent.primaryColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (selected) {
                                            Icon(
                                                imageVector = Icons.Filled.Check,
                                                contentDescription = "Selected accent",
                                                tint = androidx.compose.ui.graphics.Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = accent.displayName,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

                    Text("App Theme Preset", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ThemePreset.entries.forEach { preset ->
                            val selected = settings.themePreset == preset
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .width(136.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .border(
                                        width = if (selected) 2.dp else 1.dp,
                                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clickable { onThemeSelect(preset) }
                                    .testTag("theme_preset_${preset.name.lowercase()}")
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(CircleShape)
                                                .background(preset.previewBackground)
                                                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(CircleShape)
                                                .background(preset.previewPrimary)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = preset.displayName,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = preset.subtitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

                    Text("App & Feed Font", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FontPreset.entries.forEach { font ->
                            FilterChip(
                                selected = settings.fontPreset == font,
                                onClick = { onFontSelect(font) },
                                label = {
                                    Text(
                                        text = font.displayName,
                                        fontFamily = fontFamilyForPreset(font)
                                    )
                                },
                                modifier = Modifier.testTag("font_preset_${font.name.lowercase()}")
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

                    Text("Chat Background", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ChatBackgroundPreset.entries.filter { it != ChatBackgroundPreset.CUSTOM_PHOTO }.forEach { bg ->
                            FilterChip(
                                selected = settings.chatBackgroundPreset == bg,
                                onClick = { onChatBgSelect(bg) },
                                label = { Text(bg.displayName) }
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                photoPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.testTag("pick_chat_wallpaper_button")
                        ) {
                            Text(
                                if (settings.chatBackgroundPreset == ChatBackgroundPreset.CUSTOM_PHOTO)
                                    "Custom Photo ✓"
                                else "Pick Photo"
                            )
                        }
                    }

                    ModSwitchRow(
                        title = "Hide Reels Tab",
                        subtitle = "Remove Reels from bottom navigation & hide Reels links",
                        checked = settings.hideReelsTab,
                        testTag = "toggle_hide_reels_tab",
                        onCheckedChange = { onBooleanSettingChange("hide_reels_tab", it) }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

                    Text("App Launcher Icon", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        LauncherIconVariant.entries.forEach { iconVariant ->
                            val selected = settings.launcherIconVariant == iconVariant
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .width(120.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .border(
                                        width = if (selected) 2.dp else 1.dp,
                                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clickable { onLauncherIconSelect(iconVariant) }
                                    .testTag("launcher_icon_${iconVariant.name.lowercase()}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(iconVariant.previewBg)
                                            .border(1.5.dp, iconVariant.previewAccent, RoundedCornerShape(12.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.CameraAlt,
                                            contentDescription = iconVariant.displayName,
                                            tint = iconVariant.previewAccent,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = iconVariant.displayName,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1
                                    )
                                    if (selected) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = "Active icon",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 5. Chat Lock & Security (InstaPro 3 Style)
                ModSectionCard(
                    title = "Chat Lock & Security",
                    subtitle = "BiometricPrompt & PIN wrapper for protected conversations",
                    icon = Icons.Filled.Lock,
                    accentColor = MaterialTheme.colorScheme.primary
                ) {
                    val context = LocalContext.current
                    val activity = context as? FragmentActivity
                    val biometricStatus = remember(context, settings.hasPinConfigured) {
                        BiometricHelper.checkAvailability(
                            context = context,
                            allowDeviceCredential = !settings.hasPinConfigured
                        )
                    }
                    var biometricTestNote by remember { mutableStateOf<String?>(null) }

                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Fingerprint,
                                    contentDescription = "Biometric sensor status",
                                    tint = if (biometricStatus == BiometricAvailability.READY) AdShieldGreen
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = when (biometricStatus) {
                                            BiometricAvailability.READY -> "BiometricPrompt Ready"
                                            BiometricAvailability.NONE_ENROLLED -> "No Biometrics Enrolled"
                                            BiometricAvailability.UNAVAILABLE -> "Biometrics Unavailable"
                                        },
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = biometricTestNote ?: when (biometricStatus) {
                                            BiometricAvailability.READY -> "Protected chats shield screenshots & auto-lock in background"
                                            BiometricAvailability.NONE_ENROLLED -> "Enroll fingerprint/face or use 4-digit PIN"
                                            BiometricAvailability.UNAVAILABLE -> "Using 4-digit PIN lock for protected chats"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (biometricStatus == BiometricAvailability.READY && activity != null) {
                                OutlinedButton(
                                    onClick = {
                                        BiometricHelper.authenticate(
                                            activity = activity,
                                            title = "Verify Chat Lock Biometrics",
                                            subtitle = "Confirm fingerprint or face authentication",
                                            allowDeviceCredential = !settings.hasPinConfigured,
                                            onSuccess = { biometricTestNote = "Biometric verification succeeded" },
                                            onFailedAttempt = { biometricTestNote = "Biometric not recognized" },
                                            onError = { err -> biometricTestNote = err }
                                        )
                                    },
                                    modifier = Modifier.testTag("test_biometric_prompt_button")
                                ) {
                                    Text("Test")
                                }
                            } else if (biometricStatus == BiometricAvailability.NONE_ENROLLED) {
                                OutlinedButton(
                                    onClick = { BiometricHelper.openBiometricEnrollment(context) },
                                    modifier = Modifier.testTag("enroll_biometric_button")
                                ) {
                                    Text("Enroll")
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            onOpenPinSetup(
                                if (settings.hasPinConfigured) PinSetupMode.CHANGE else PinSetupMode.CREATE
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("configure_pin_button")
                    ) {
                        Text(if (settings.hasPinConfigured) "Change 4-Digit PIN" else "Set Up 4-Digit PIN")
                    }
                    ModSwitchRow(
                        title = "Lock Messages Tab",
                        subtitle = "Require PIN or Biometric to open Direct Messages",
                        checked = settings.isDmTabLocked,
                        testTag = "toggle_lock_dm_tab",
                        onCheckedChange = { onBooleanSettingChange("is_dm_tab_locked", it) }
                    )
                    ModSwitchRow(
                        title = "Lock App on Launch",
                        subtitle = "Prompt security screen when InstaPlus opens",
                        checked = settings.isAppLockEnabled,
                        testTag = "toggle_lock_app_launch",
                        onCheckedChange = { onBooleanSettingChange("is_app_lock_enabled", it) }
                    )
                    ModSwitchRow(
                        title = "Use Biometric Unlock",
                        subtitle = "Fingerprint / Face unlock alongside PIN",
                        checked = settings.isBiometricEnabled,
                        testTag = "toggle_biometric",
                        onCheckedChange = { onBooleanSettingChange("is_biometric_enabled", it) }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

                    Text("Locked Specific Chats", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newLockHandle,
                            onValueChange = { newLockHandle = it },
                            label = { Text("@username or thread ID") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("add_locked_chat_input")
                        )
                        Button(
                            onClick = {
                                onAddLockedChat(newLockHandle, newLockHandle)
                                newLockHandle = ""
                            },
                            enabled = newLockHandle.isNotBlank(),
                            modifier = Modifier.testTag("add_locked_chat_button")
                        ) {
                            Text("Lock")
                        }
                    }

                    lockedChats.forEach { chat ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(chat.displayName, style = MaterialTheme.typography.bodyMedium)
                            }
                            IconButton(onClick = { onRemoveLockedChat(chat.id) }) {
                                Icon(
                                    imageVector = Icons.Filled.DeleteOutline,
                                    contentDescription = "Unlock chat"
                                )
                            }
                        }
                    }
                }

                // 6. Mindful Focus Guard (Exclusive Detox Timer)
                ModSectionCard(
                    title = "Mindful Scroll Guard",
                    subtitle = "Exclusive wellness break timer for balanced social sessions",
                    icon = Icons.Filled.Timer,
                    accentColor = MaterialTheme.colorScheme.primary
                ) {
                    ModSwitchRow(
                        title = "Enable Focus Detox Timer",
                        subtitle = "Track active session time & nudge for a breather",
                        checked = settings.focusGuardEnabled,
                        testTag = "toggle_focus_guard",
                        onCheckedChange = { onBooleanSettingChange("focus_guard_enabled", it) }
                    )
                    Text("Session Limit (minutes)", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(5, 10, 20, 45).forEach { mins ->
                            FilterChip(
                                selected = settings.focusLimitMinutes == mins,
                                onClick = { onFocusLimitSelect(mins) },
                                label = { Text("${mins}m") },
                                modifier = Modifier.testTag("focus_limit_$mins")
                            )
                        }
                    }
                }

                // 7. Translation Engine
                ModSectionCard(
                    title = "Comment & Caption Translator",
                    subtitle = "Instant AI translation & Caption Studio powered by Gemini",
                    icon = Icons.Filled.Translate,
                    accentColor = MaterialTheme.colorScheme.secondary
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        modLanguages.forEach { lang ->
                            FilterChip(
                                selected = settings.targetLanguage == lang,
                                onClick = { onTargetLanguageSelect(lang) },
                                label = { Text(lang) }
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = onOpenTranslator,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("mod_open_translator_button")
                    ) {
                        Text("Open AI Translation Sheet")
                    }
                }

                // 8. Multi-Account Switcher
                ModSectionCard(
                    title = "Multiple Accounts",
                    subtitle = "Switch between your standalone InstaPlus profiles",
                    icon = Icons.Filled.AccountCircle,
                    accentColor = MaterialTheme.colorScheme.tertiary
                ) {
                    Button(
                        onClick = onOpenAccountSwitcher,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("mod_open_accounts_button")
                    ) {
                        Text("Manage Accounts ($accountCount saved)")
                    }
                }
            }
        }
    }
}

@Composable
private fun ModSectionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: androidx.compose.ui.graphics.Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 0.75.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
                shape = RoundedCornerShape(18.dp)
            )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            content()
        }
    }
}

@Composable
private fun ModSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
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
            modifier = Modifier.testTag(testTag)
        )
    }
}
