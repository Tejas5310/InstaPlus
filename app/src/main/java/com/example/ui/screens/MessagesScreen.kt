package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.local.DirectMessageEntity
import com.example.data.local.DirectThreadEntity
import com.example.data.local.LockedChatEntity
import com.example.data.model.ChatBackgroundPreset
import com.example.data.model.InstaPlusSettings
import com.example.ui.theme.AdShieldGreen
import com.example.ui.theme.GhostPurple

@Composable
fun MessagesScreen(
    settings: InstaPlusSettings,
    threads: List<DirectThreadEntity>,
    messages: List<DirectMessageEntity>,
    lockedChats: List<LockedChatEntity>,
    activeThreadKey: String?,
    onOpenThread: (DirectThreadEntity) -> Unit,
    onCloseThread: () -> Unit,
    onToggleThreadLock: (DirectThreadEntity) -> Unit,
    onSendMessage: (threadKey: String, text: String, burnAfterSeconds: Int) -> Unit,
    onTypingChanged: (threadKey: String) -> Unit,
    onCreateThread: (handle: String, displayName: String) -> Unit,
    onCopyText: (String, String) -> Unit,
    onTranslateText: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeThread = remember(threads, activeThreadKey) {
        activeThreadKey?.let { key -> threads.firstOrNull { it.threadKey.equals(key, ignoreCase = true) } }
    }

    if (activeThread != null) {
        BackHandler { onCloseThread() }
        val threadMessages = remember(messages, activeThread.threadKey) {
            messages.filter { it.threadKey.equals(activeThread.threadKey, ignoreCase = true) }
        }
        val isLocked = lockedChats.any { it.threadKey.equals(activeThread.threadKey, ignoreCase = true) }

        ConversationThreadView(
            settings = settings,
            thread = activeThread,
            messages = threadMessages,
            isLocked = isLocked,
            onBackClick = onCloseThread,
            onToggleLock = { onToggleThreadLock(activeThread) },
            onSendMessage = { text, burnSecs -> onSendMessage(activeThread.threadKey, text, burnSecs) },
            onTypingChanged = { onTypingChanged(activeThread.threadKey) },
            onCopyText = onCopyText,
            onTranslateText = onTranslateText,
            modifier = modifier
        )
    } else {
        DirectInboxView(
            settings = settings,
            threads = threads,
            lockedChats = lockedChats,
            onOpenThread = onOpenThread,
            onToggleThreadLock = onToggleThreadLock,
            onCreateThread = onCreateThread,
            modifier = modifier
        )
    }
}

@Composable
private fun DirectInboxView(
    settings: InstaPlusSettings,
    threads: List<DirectThreadEntity>,
    lockedChats: List<LockedChatEntity>,
    onOpenThread: (DirectThreadEntity) -> Unit,
    onToggleThreadLock: (DirectThreadEntity) -> Unit,
    onCreateThread: (handle: String, displayName: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var search by remember { mutableStateOf("") }
    var showNewChatDialog by remember { mutableStateOf(false) }

    val filtered = remember(threads, search) {
        val q = search.trim().lowercase()
        if (q.isBlank()) threads
        else threads.filter {
            it.displayName.lowercase().contains(q) ||
                it.threadKey.lowercase().contains(q) ||
                it.lastMessagePreview.lowercase().contains(q)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Direct Messages",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (settings.hideOnlineStatus) "Ghost Presence: Your active status is hidden"
                    else "Encrypted Local Threads • Tap lock icon to shield chats",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (settings.hideOnlineStatus) GhostPurple else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = { showNewChatDialog = true },
                modifier = Modifier.testTag("new_chat_thread_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.AddComment,
                    contentDescription = "New Conversation",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            placeholder = { Text("Search messages & handles...") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .testTag("dm_search_input")
        )

        // Active Friends Strip
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(threads, key = { it.threadKey }) { thread ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(68.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onOpenThread(thread) }
                ) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(Color(thread.avatarColorHex).copy(alpha = 0.22f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = thread.displayName.take(1).uppercase(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(thread.avatarColorHex)
                            )
                        }
                        if (thread.isOnline && !settings.hideOnlineStatus) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(AdShieldGreen)
                                    .border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = thread.displayName.substringBefore(" "),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(filtered, key = { it.threadKey }) { thread ->
                val isLocked = lockedChats.any { it.threadKey.equals(thread.threadKey, ignoreCase = true) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenThread(thread) }
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                        .testTag("dm_thread_row_${thread.threadKey}"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(thread.avatarColorHex).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = thread.displayName.take(1).uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(thread.avatarColorHex)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = thread.displayName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            if (isLocked) {
                                Icon(
                                    imageVector = Icons.Filled.Lock,
                                    contentDescription = "Locked chat",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Text(
                            text = if (isLocked) "Protected conversation • Tap to unlock"
                            else thread.lastMessagePreview,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (thread.unreadCount > 0) {
                        Badge(containerColor = MaterialTheme.colorScheme.primary) {
                            Text(thread.unreadCount.toString())
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    IconButton(
                        onClick = { onToggleThreadLock(thread) },
                        modifier = Modifier.testTag("toggle_thread_lock_${thread.threadKey}")
                    ) {
                        Icon(
                            imageVector = if (isLocked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                            contentDescription = if (isLocked) "Unlock thread" else "Lock thread with Biometric/PIN",
                            tint = if (isLocked) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (showNewChatDialog) {
        var handle by remember { mutableStateOf("") }
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewChatDialog = false },
            title = { Text("Start New Conversation") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = handle,
                        onValueChange = { handle = it },
                        label = { Text("Username (e.g. @alex)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Display Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (handle.isNotBlank()) {
                            onCreateThread(handle, name)
                            showNewChatDialog = false
                        }
                    },
                    enabled = handle.isNotBlank()
                ) {
                    Text("Open Chat")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewChatDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ConversationThreadView(
    settings: InstaPlusSettings,
    thread: DirectThreadEntity,
    messages: List<DirectMessageEntity>,
    isLocked: Boolean,
    onBackClick: () -> Unit,
    onToggleLock: () -> Unit,
    onSendMessage: (String, Int) -> Unit,
    onTypingChanged: () -> Unit,
    onCopyText: (String, String) -> Unit,
    onTranslateText: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var draft by remember { mutableStateOf("") }
    var burnSeconds by remember { mutableStateOf(0) }

    Box(modifier = modifier.fillMaxSize()) {
        ChatWallpaperLayer(settings = settings)

        Column(modifier = Modifier.fillMaxSize()) {
            // Thread Header
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick, modifier = Modifier.testTag("chat_thread_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Inbox")
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = thread.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (settings.disableReadReceipts) {
                                Icon(
                                    imageVector = Icons.Filled.VisibilityOff,
                                    contentDescription = "Ghost Read Receipts Hidden",
                                    tint = GhostPurple,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "Ghost DM • Read & Typing Masked",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = GhostPurple
                                )
                            } else {
                                Text(
                                    text = thread.threadKey,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    IconButton(onClick = onToggleLock) {
                        Icon(
                            imageVector = if (isLocked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                            contentDescription = if (isLocked) "Remove lock" else "Lock chat",
                            tint = if (isLocked) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Message Bubbles
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    val alignEnd = msg.isOutgoing
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (alignEnd) Arrangement.End else Arrangement.Start
                    ) {
                        Surface(
                            color = if (alignEnd) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                            shape = RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = if (alignEnd) 16.dp else 4.dp,
                                bottomEnd = if (alignEnd) 4.dp else 16.dp
                            ),
                            tonalElevation = 1.dp,
                            modifier = Modifier.padding(
                                start = if (alignEnd) 48.dp else 0.dp,
                                end = if (alignEnd) 0.dp else 48.dp
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = msg.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (alignEnd) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurface
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (msg.burnAfterSeconds > 0) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Filled.LocalFireDepartment,
                                                contentDescription = "Self-destructing",
                                                tint = Color(0xFFF58529),
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(Modifier.width(2.dp))
                                            Text(
                                                text = "Auto-burns ${msg.burnAfterSeconds}s",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (alignEnd) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                                                else Color(0xFFF58529)
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.Filled.ContentCopy,
                                        contentDescription = "Copy message",
                                        tint = if (alignEnd) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                                        else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clickable { onCopyText("Message", msg.text) }
                                    )
                                    Icon(
                                        imageVector = Icons.Filled.Translate,
                                        contentDescription = "Translate message",
                                        tint = if (alignEnd) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                                        else MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clickable { onTranslateText(msg.text) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Self-Destruct Timer Picker + Input Row
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                tonalElevation = 4.dp
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Self-Destruct:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        listOf(0 to "Off", 10 to "10s Burn", 60 to "60s Burn").forEach { (secs, label) ->
                            FilterChip(
                                selected = burnSeconds == secs,
                                onClick = { burnSeconds = secs },
                                label = { Text(label) },
                                modifier = Modifier.testTag("burn_timer_$secs")
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = draft,
                            onValueChange = {
                                if (draft.isEmpty() && it.isNotEmpty()) onTypingChanged()
                                draft = it
                            },
                            placeholder = {
                                Text(
                                    if (burnSeconds > 0) "Send secret (${burnSeconds}s) message..."
                                    else "Message ${thread.displayName}..."
                                )
                            },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("dm_message_input")
                        )
                        Button(
                            onClick = {
                                onSendMessage(draft, burnSeconds)
                                draft = ""
                            },
                            enabled = draft.isNotBlank(),
                            modifier = Modifier.testTag("dm_send_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatWallpaperLayer(settings: InstaPlusSettings) {
    Box(modifier = Modifier.fillMaxSize()) {
        when (settings.chatBackgroundPreset) {
            ChatBackgroundPreset.PRISM_SILK -> Image(
                painter = painterResource(id = R.drawable.img_chat_bg_prism),
                contentDescription = "Chat background",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            ChatBackgroundPreset.CUSTOM_PHOTO -> {
                val uri = settings.customChatBackgroundUri?.let { runCatching { Uri.parse(it) }.getOrNull() }
                if (uri != null) {
                    AsyncImage(
                        model = uri,
                        contentDescription = "Custom chat wallpaper",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            ChatBackgroundPreset.AMOLED_PITCH -> Box(Modifier.fillMaxSize().background(Color.Black))
            ChatBackgroundPreset.SOFT_ROSE -> Box(Modifier.fillMaxSize().background(Color(0xFFFFF0F6)))
            ChatBackgroundPreset.DEFAULT -> Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        }
    }
}
