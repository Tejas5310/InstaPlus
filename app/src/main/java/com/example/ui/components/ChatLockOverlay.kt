package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.example.system.BiometricAvailability
import com.example.system.BiometricHelper
import com.example.ui.theme.InstagramStoryBrush
import com.example.ui.viewmodel.LockChallenge
import com.example.ui.viewmodel.PinSetupMode
import kotlin.math.roundToInt

@Composable
fun ChatLockOverlay(
    challenge: LockChallenge,
    hasPinConfigured: Boolean,
    isBiometricEnabled: Boolean,
    errorMessage: String?,
    onPinSubmitted: (String) -> Unit,
    onBiometricSuccess: () -> Unit,
    onCancelToHome: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val allowDeviceCredential = !hasPinConfigured
    val availability = remember(context, allowDeviceCredential) {
        BiometricHelper.checkAvailability(context, allowDeviceCredential)
    }

    var enteredPin by remember { mutableStateOf("") }
    var biometricStatusMessage by remember { mutableStateOf<String?>(null) }
    val shakeOffset = remember { Animatable(0f) }

    fun triggerBiometricPrompt() {
        if (activity == null) {
            biometricStatusMessage = "Biometric prompt requires an active activity."
            return
        }
        biometricStatusMessage = null
        BiometricHelper.authenticate(
            activity = activity,
            title = when (challenge) {
                LockChallenge.AppLaunch -> "Unlock InstaPlus"
                LockChallenge.MessagesTab -> "Unlock Direct Messages"
                is LockChallenge.SpecificThread -> "Unlock ${challenge.displayName}"
            },
            subtitle = "Authenticate with biometrics to open protected conversation",
            allowDeviceCredential = allowDeviceCredential,
            onSuccess = onBiometricSuccess,
            onFailedAttempt = {
                biometricStatusMessage = "Biometric not recognized. Try again or use PIN."
            },
            onError = { err ->
                biometricStatusMessage = err
            }
        )
    }

    LaunchedEffect(errorMessage) {
        if (!errorMessage.isNullOrBlank()) {
            enteredPin = ""
            repeat(4) { i ->
                shakeOffset.animateTo(if (i % 2 == 0) 18f else -18f, tween(55))
            }
            shakeOffset.animateTo(0f, tween(55))
        }
    }

    LaunchedEffect(challenge, isBiometricEnabled, availability) {
        if (isBiometricEnabled && availability == BiometricAvailability.READY && activity != null) {
            triggerBiometricPrompt()
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.99f),
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .border(2.dp, InstagramStoryBrush, CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = "Protected Chat Lock",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = when (challenge) {
                    LockChallenge.AppLaunch -> "InstaPlus Locked"
                    LockChallenge.MessagesTab -> "Messages Protected"
                    is LockChallenge.SpecificThread -> "Locked Chat: ${challenge.displayName}"
                },
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (hasPinConfigured) {
                    "Verify with biometrics or enter your 4-digit PIN"
                } else {
                    "Verify with your device biometrics or screen lock"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            val activeError = errorMessage ?: biometricStatusMessage
            if (!activeError.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = activeError,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }

            if (hasPinConfigured) {
                Spacer(modifier = Modifier.height(22.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
                ) {
                    repeat(4) { index ->
                        val filled = index < enteredPin.length
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(
                                    if (filled) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .border(
                                    1.5.dp,
                                    if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    CircleShape
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(26.dp))

                val rows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9")
                )
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    rows.forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                            row.forEach { digit ->
                                KeypadCircleButton(
                                    label = digit,
                                    onClick = {
                                        if (enteredPin.length < 4) {
                                            val next = enteredPin + digit
                                            enteredPin = next
                                            if (next.length == 4) {
                                                onPinSubmitted(next)
                                            }
                                        }
                                    },
                                    modifier = Modifier.testTag("pin_key_$digit")
                                )
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .clickable(
                                    enabled = isBiometricEnabled &&
                                        availability == BiometricAvailability.READY &&
                                        activity != null
                                ) {
                                    triggerBiometricPrompt()
                                }
                                .testTag("pin_biometric_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isBiometricEnabled && availability == BiometricAvailability.READY) {
                                Icon(
                                    imageVector = Icons.Filled.Fingerprint,
                                    contentDescription = "Unlock with Biometrics",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }

                        KeypadCircleButton(
                            label = "0",
                            onClick = {
                                if (enteredPin.length < 4) {
                                    val next = enteredPin + "0"
                                    enteredPin = next
                                    if (next.length == 4) {
                                        onPinSubmitted(next)
                                    }
                                }
                            },
                            modifier = Modifier.testTag("pin_key_0")
                        )

                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .clickable {
                                    if (enteredPin.isNotEmpty()) {
                                        enteredPin = enteredPin.dropLast(1)
                                    }
                                }
                                .testTag("pin_backspace_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Backspace,
                                contentDescription = "Backspace",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                // Biometric / Device Credential primary gate when no separate PIN is configured
                Spacer(modifier = Modifier.height(28.dp))
                when (availability) {
                    BiometricAvailability.READY -> {
                        Button(
                            onClick = { triggerBiometricPrompt() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("biometric_unlock_primary_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Fingerprint,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Verify Identity")
                        }
                    }
                    BiometricAvailability.NONE_ENROLLED -> {
                        Text(
                            text = "No biometrics or screen lock enrolled on this device yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { BiometricHelper.openBiometricEnrollment(context) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Set Up Biometric Lock")
                        }
                    }
                    BiometricAvailability.UNAVAILABLE -> {
                        OutlinedButton(
                            onClick = onBiometricSuccess,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Biometric Hardware Unavailable — Continue")
                        }
                    }
                }
            }

            if (onCancelToHome != null && challenge != LockChallenge.AppLaunch) {
                Spacer(modifier = Modifier.height(20.dp))
                TextButton(
                    onClick = onCancelToHome,
                    modifier = Modifier.testTag("pin_cancel_home")
                ) {
                    Text("Return to Feed")
                }
            }
        }
    }
}

@Composable
private fun KeypadCircleButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
            .size(68.dp)
            .minimumInteractiveComponentSize()
            .clip(CircleShape)
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun PinSetupDialog(
    mode: PinSetupMode,
    onSavePin: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (mode == PinSetupMode.CREATE) "Create Chat Lock PIN"
                else "Change Chat Lock PIN"
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Set a 4-digit PIN to lock Messages, specific chats, or app launch alongside BiometricPrompt.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = pin,
                    onValueChange = { value ->
                        if (value.length <= 4 && value.all { it.isDigit() }) {
                            pin = value
                            errorText = null
                        }
                    },
                    label = { Text("4-digit PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("setup_pin_input")
                )
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { value ->
                        if (value.length <= 4 && value.all { it.isDigit() }) {
                            confirmPin = value
                            errorText = null
                        }
                    },
                    label = { Text("Confirm PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("setup_pin_confirm_input")
                )
                if (!errorText.isNullOrBlank()) {
                    Text(
                        text = errorText!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when {
                        pin.length != 4 -> errorText = "PIN must be exactly 4 digits."
                        pin != confirmPin -> errorText = "PINs do not match."
                        else -> onSavePin(pin)
                    }
                },
                modifier = Modifier.testTag("save_pin_button")
            ) {
                Text("Save PIN")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
