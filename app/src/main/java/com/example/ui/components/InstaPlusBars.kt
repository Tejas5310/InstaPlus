package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.InstaPlusSection
import com.example.data.model.InstaPlusSettings
import com.example.ui.theme.AdShieldGreen
import com.example.ui.theme.GhostPurple
import com.example.ui.theme.InstagramStoryBrush

@Composable
fun InstaPlusTopBar(
    activeUsername: String?,
    settings: InstaPlusSettings,
    vaultCount: Int,
    focusSecondsElapsed: Int,
    onAccountSwitchClick: () -> Unit,
    onCreatePostClick: () -> Unit,
    onToggleThemeClick: () -> Unit,
    onGhostClick: () -> Unit,
    onVaultClick: () -> Unit,
    onModHubClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onAccountSwitchClick)
                        .minimumInteractiveComponentSize()
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                        .testTag("account_switcher_button")
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.instaplus_app_icon),
                        contentDescription = stringResource(R.string.app_name),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.action_switch_account),
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(18.dp)
                    )
                    if (!activeUsername.isNullOrBlank()) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "@$activeUsername",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (settings.focusGuardEnabled) {
                        val remSecs = ((settings.focusLimitMinutes * 60) - focusSecondsElapsed).coerceAtLeast(0)
                        val mins = remSecs / 60
                        val secs = remSecs % 60
                        StatusChipPill(
                            label = "%d:%02d".format(mins, secs),
                            color = MaterialTheme.colorScheme.primary,
                            icon = Icons.Filled.Timer,
                            onClick = onModHubClick,
                            testTag = "top_bar_focus_timer"
                        )
                    } else if (settings.isGhostModeAnyActive) {
                        StatusChipPill(
                            label = stringResource(R.string.ghost_mode_active),
                            color = GhostPurple,
                            icon = Icons.Filled.VisibilityOff,
                            onClick = onGhostClick,
                            testTag = "top_bar_ghost_chip"
                        )
                    }
                    if (!settings.focusGuardEnabled && settings.isAdBlockAnyActive) {
                        StatusChipPill(
                            label = stringResource(R.string.ad_free_active),
                            color = AdShieldGreen,
                            icon = Icons.Filled.Shield
                        )
                    }

                    IconButton(
                        onClick = onCreatePostClick,
                        modifier = Modifier.testTag("top_bar_create_post_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AddBox,
                            contentDescription = "Create post, story, or reel",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = onToggleThemeClick,
                        modifier = Modifier.testTag("quick_theme_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (settings.isDarkMode) Icons.Filled.LightMode
                            else Icons.Filled.DarkMode,
                            contentDescription = if (settings.isDarkMode) "Switch to Light Mode" else "Switch to Dark Mode",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    IconButton(
                        onClick = onVaultClick,
                        modifier = Modifier.testTag("open_vault_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (vaultCount > 0) {
                                    Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                        Text(if (vaultCount > 99) "99+" else vaultCount.toString())
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PhotoLibrary,
                                contentDescription = stringResource(R.string.nav_vault),
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }

                    IconButton(
                        onClick = onModHubClick,
                        modifier = Modifier.testTag("open_mod_hub_button")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, InstagramStoryBrush, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Tune,
                                contentDescription = stringResource(R.string.nav_mod_hub),
                                tint = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }
            }
            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun StatusChipPill(
    label: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: (() -> Unit)? = null,
    testTag: String? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.14f))
            .let { mod -> if (onClick != null) mod.clickable(onClick = onClick) else mod }
            .let { mod -> if (testTag != null) mod.testTag(testTag) else mod }
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun InstaPlusBottomBar(
    activeSection: InstaPlusSection,
    hideReelsTab: Boolean,
    isDmLocked: Boolean,
    onSelectSection: (InstaPlusSection) -> Unit,
    modifier: Modifier = Modifier
) {
    val visibleSections = buildList {
        add(InstaPlusSection.HOME)
        add(InstaPlusSection.SEARCH)
        if (!hideReelsTab) add(InstaPlusSection.REELS)
        add(InstaPlusSection.MESSAGES)
        add(InstaPlusSection.PROFILE)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
        )
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.background,
            windowInsets = WindowInsets.navigationBars,
            tonalElevation = 0.dp
        ) {
            visibleSections.forEach { section ->
                val selected = section == activeSection
                NavigationBarItem(
                    selected = selected,
                    onClick = { onSelectSection(section) },
                    modifier = Modifier.testTag("nav_tab_${section.name.lowercase()}"),
                    icon = {
                        SectionNavIcon(
                            section = section,
                            selected = selected,
                            showLockBadge = section == InstaPlusSection.MESSAGES && isDmLocked
                        )
                    },
                    label = {
                        Text(
                            text = section.label,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        }
    }
}

@Composable
fun InstaPlusNavigationRail(
    activeSection: InstaPlusSection,
    hideReelsTab: Boolean,
    isDmLocked: Boolean,
    onSelectSection: (InstaPlusSection) -> Unit,
    modifier: Modifier = Modifier
) {
    val visibleSections = buildList {
        add(InstaPlusSection.HOME)
        add(InstaPlusSection.SEARCH)
        if (!hideReelsTab) add(InstaPlusSection.REELS)
        add(InstaPlusSection.MESSAGES)
        add(InstaPlusSection.PROFILE)
    }

    NavigationRail(
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        visibleSections.forEach { section ->
            val selected = section == activeSection
            NavigationRailItem(
                selected = selected,
                onClick = { onSelectSection(section) },
                modifier = Modifier.testTag("rail_tab_${section.name.lowercase()}"),
                icon = {
                    SectionNavIcon(
                        section = section,
                        selected = selected,
                        showLockBadge = section == InstaPlusSection.MESSAGES && isDmLocked
                    )
                },
                label = { Text(section.label) }
            )
        }
    }
}

@Composable
private fun SectionNavIcon(
    section: InstaPlusSection,
    selected: Boolean,
    showLockBadge: Boolean
) {
    val icon = when (section) {
        InstaPlusSection.HOME -> if (selected) Icons.Filled.Home else Icons.Outlined.Home
        InstaPlusSection.SEARCH -> if (selected) Icons.Filled.Search else Icons.Outlined.Search
        InstaPlusSection.REELS -> if (selected) Icons.Filled.Movie else Icons.Outlined.Movie
        InstaPlusSection.MESSAGES -> if (selected) Icons.Filled.ChatBubble else Icons.Outlined.ChatBubbleOutline
        InstaPlusSection.PROFILE -> if (selected) Icons.Filled.Person else Icons.Outlined.PersonOutline
    }
    if (showLockBadge) {
        BadgedBox(
            badge = {
                Badge(containerColor = MaterialTheme.colorScheme.primary) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = "Protected chats",
                        modifier = Modifier.size(9.dp)
                    )
                }
            }
        ) {
            Icon(imageVector = icon, contentDescription = section.label)
        }
    } else {
        Icon(imageVector = icon, contentDescription = section.label)
    }
}

@Composable
fun InstaPlusQuickDock(
    visible: Boolean,
    isGhostActive: Boolean,
    onCreateClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onTranslateClick: () -> Unit,
    onGhostToggleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            shadowElevation = 8.dp,
            tonalElevation = 4.dp,
            modifier = Modifier.border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                shape = RoundedCornerShape(28.dp)
            )
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp)
            ) {
                IconButton(
                    onClick = onCreateClick,
                    modifier = Modifier.testTag("quick_dock_create")
                ) {
                    Icon(
                        imageVector = Icons.Filled.AddBox,
                        contentDescription = "Create post",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(
                    onClick = onDownloadClick,
                    modifier = Modifier.testTag("quick_dock_download")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Download,
                        contentDescription = stringResource(R.string.action_download),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(
                    onClick = onTranslateClick,
                    modifier = Modifier.testTag("quick_dock_translate")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Translate,
                        contentDescription = stringResource(R.string.action_translate),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
                IconButton(
                    onClick = onGhostToggleClick,
                    modifier = Modifier.testTag("quick_dock_ghost")
                ) {
                    Icon(
                        imageVector = if (isGhostActive) Icons.Filled.VisibilityOff else Icons.Outlined.VisibilityOff,
                        contentDescription = "Toggle Ghost Mode",
                        tint = if (isGhostActive) GhostPurple else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
