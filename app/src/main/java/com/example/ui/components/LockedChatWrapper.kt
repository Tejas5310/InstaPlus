package com.example.ui.components

import android.app.Activity
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.theme.AdShieldGreen
import com.example.ui.theme.GhostPurple
import com.example.ui.viewmodel.LockChallenge

/**
 * Secure wrapper for protected conversations and Direct Messages.
 * - Applies FLAG_SECURE when inside protected chats or locked states to prevent screenshots/recents leaks.
 * - Automatically re-locks unlocked conversations when the app moves to background (ON_STOP).
 * - Masks underlying chat content completely when a LockChallenge is active.
 */
@Composable
fun LockedChatWrapper(
    isInDirectMessages: Boolean,
    activeThreadLabel: String?,
    isCurrentChatProtected: Boolean,
    activeLockChallenge: LockChallenge?,
    hasPinConfigured: Boolean,
    isBiometricEnabled: Boolean,
    pinError: String?,
    onRequestLockCurrentChat: () -> Unit,
    onRelockNow: () -> Unit,
    onAutoRelockOnBackground: () -> Unit,
    onPinSubmitted: (String) -> Unit,
    onBiometricSuccess: () -> Unit,
    onCancelToHome: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isSensitiveContext = activeLockChallenge != null || isInDirectMessages || isCurrentChatProtected

    // Prevent screenshots & app-switcher thumbnails while viewing protected conversations
    DisposableEffect(context, isSensitiveContext) {
        val window = (context as? Activity)?.window
        if (isSensitiveContext) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    // Auto re-lock protected conversations whenever the user leaves the app
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                onAutoRelockOnBackground()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Conversation Protection Banner when inside Direct Messages
            AnimatedVisibility(
                visible = isInDirectMessages && activeLockChallenge == null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .border(
                            width = 1.dp,
                            color = if (isCurrentChatProtected) AdShieldGreen.copy(alpha = 0.6f)
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(50)
                        )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (isCurrentChatProtected) Icons.Filled.VerifiedUser else Icons.Outlined.Lock,
                                contentDescription = null,
                                tint = if (isCurrentChatProtected) AdShieldGreen else GhostPurple,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isCurrentChatProtected) {
                                    "Protected${activeThreadLabel?.let { " • $it" } ?: ""} (Screen Shielded)"
                                } else {
                                    activeThreadLabel?.let { "Chat: $it" } ?: "Direct Messages"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        if (isCurrentChatProtected) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
                                    .clickable(onClick = onRelockNow)
                                    .minimumInteractiveComponentSize()
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                                    .testTag("relock_chat_now_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Lock,
                                    contentDescription = "Re-lock chat",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Lock Now",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(GhostPurple.copy(alpha = 0.14f))
                                    .clickable(onClick = onRequestLockCurrentChat)
                                    .minimumInteractiveComponentSize()
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                                    .testTag("lock_current_chat_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Lock,
                                    contentDescription = "Lock this chat",
                                    tint = GhostPurple,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Lock Chat",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = GhostPurple,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Underlying conversation surface (always hidden/blocked when lock challenge is active)
            if (activeLockChallenge == null) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    content()
                }
            } else {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                )
            }
        }

        // Full-screen Biometric + PIN Lock Gate
        activeLockChallenge?.let { challenge ->
            ChatLockOverlay(
                challenge = challenge,
                hasPinConfigured = hasPinConfigured,
                isBiometricEnabled = isBiometricEnabled,
                errorMessage = pinError,
                onPinSubmitted = onPinSubmitted,
                onBiometricSuccess = onBiometricSuccess,
                onCancelToHome = onCancelToHome
            )
        }
    }
}
