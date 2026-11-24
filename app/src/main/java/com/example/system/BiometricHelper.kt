package com.example.system

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

enum class BiometricAvailability {
    READY,
    NONE_ENROLLED,
    UNAVAILABLE
}

object BiometricHelper {

    private fun authenticatorsFor(allowDeviceCredential: Boolean): Int {
        return if (allowDeviceCredential) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            } else {
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            }
        } else {
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK
        }
    }

    fun checkAvailability(
        context: Context,
        allowDeviceCredential: Boolean = false
    ): BiometricAvailability {
        val manager = BiometricManager.from(context)
        return when (manager.canAuthenticate(authenticatorsFor(allowDeviceCredential))) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricAvailability.READY
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAvailability.NONE_ENROLLED
            else -> BiometricAvailability.UNAVAILABLE
        }
    }

    fun isBiometricAvailable(
        context: Context,
        allowDeviceCredential: Boolean = false
    ): Boolean = checkAvailability(context, allowDeviceCredential) == BiometricAvailability.READY

    fun openBiometricEnrollment(context: Context) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                putExtra(
                    Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
                )
            }
        } else {
            Intent(Settings.ACTION_SECURITY_SETTINGS)
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    fun authenticate(
        activity: FragmentActivity,
        title: String = "Unlock Protected Chat",
        subtitle: String = "Verify your identity to view this conversation",
        allowDeviceCredential: Boolean = false,
        onSuccess: () -> Unit,
        onFailedAttempt: () -> Unit = {},
        onError: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onFailedAttempt()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                        errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                        errorCode != BiometricPrompt.ERROR_CANCELED
                    ) {
                        onError(errString.toString())
                    }
                }
            }
        )

        val authenticators = authenticatorsFor(allowDeviceCredential)
        val builder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(authenticators)
            .setConfirmationRequired(false)

        if (!allowDeviceCredential) {
            builder.setNegativeButtonText("Use PIN")
        }

        runCatching {
            prompt.authenticate(builder.build())
        }.onFailure { err ->
            onError(err.localizedMessage ?: "Biometric authentication unavailable.")
        }
    }
}
