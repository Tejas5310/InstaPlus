package com.example.security

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

object OtpMessages {
    const val OTP_SENT = "OTP sent successfully to your mobile number."
    const val INVALID_OTP = "Invalid OTP. Please try again."
    const val OTP_EXPIRED = "OTP expired. Request a new OTP."
    const val UNABLE_TO_SEND = "Unable to send OTP. Please try again later."
    const val TOO_MANY_ATTEMPTS = "Too many incorrect OTP attempts. Request a new OTP."
}

sealed interface OtpDispatchOutcome {
    data class CodeSent(
        val verificationSessionId: String,
        val resendToken: Any? = null
    ) : OtpDispatchOutcome

    data class AutoVerified(
        val verifiedPhoneE164: String,
        val providerUid: String? = null
    ) : OtpDispatchOutcome

    data class Failure(
        val userMessage: String,
        val isRateLimited: Boolean = false
    ) : OtpDispatchOutcome
}

sealed interface OtpVerifyOutcome {
    data class Verified(
        val verifiedPhoneE164: String,
        val providerUid: String? = null
    ) : OtpVerifyOutcome

    data class InvalidCode(val userMessage: String = OtpMessages.INVALID_OTP) : OtpVerifyOutcome
    data class Expired(val userMessage: String = OtpMessages.OTP_EXPIRED) : OtpVerifyOutcome
    data class Failed(val userMessage: String) : OtpVerifyOutcome
}

interface SmsOtpGateway {
    suspend fun sendOtp(
        phoneNumberE164: String,
        activity: Activity? = null,
        forceResendToken: Any? = null
    ): OtpDispatchOutcome

    suspend fun verifyOtp(
        verificationSessionId: String,
        smsCode: String
    ): OtpVerifyOutcome
}

tailrec fun Context.findActivityOrNull(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivityOrNull()
    else -> null
}

/**
 * Production SMS OTP gateway backed by Firebase Phone Authentication.
 *
 * All OTP generation, SMS dispatch, expiration, and verification execute on
 * Firebase's HTTPS authentication backend. No OTP code is generated, persisted,
 * displayed, or logged on the client.
 */
class FirebasePhoneAuthOtpGateway(
    private val firebaseAuthProvider: () -> FirebaseAuth = { Firebase.auth }
) : SmsOtpGateway {

    private val gatewayScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override suspend fun sendOtp(
        phoneNumberE164: String,
        activity: Activity?,
        forceResendToken: Any?
    ): OtpDispatchOutcome {
        val hostActivity = activity
        if (hostActivity == null || hostActivity.isFinishing || hostActivity.isDestroyed) {
            Log.w(TAG, "Cannot start SMS verification without an active foreground Activity.")
            return OtpDispatchOutcome.Failure(OtpMessages.UNABLE_TO_SEND)
        }

        val auth = runCatching { firebaseAuthProvider() }.getOrElse {
            Log.w(TAG, "FirebaseAuth unavailable during OTP dispatch.")
            return OtpDispatchOutcome.Failure(OtpMessages.UNABLE_TO_SEND)
        }

        return suspendCancellableCoroutine { continuation ->
            val resumed = AtomicBoolean(false)

            val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    resumeOnce(
                        continuation,
                        resumed,
                        OtpDispatchOutcome.CodeSent(
                            verificationSessionId = verificationId,
                            resendToken = token
                        )
                    )
                }

                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    // Instant or Play Services carrier auto-retrieval without ever exposing an OTP.
                    gatewayScope.launch {
                        val outcome = signInWithPhoneCredential(auth, credential, expectedOnAuto = true)
                        when (outcome) {
                            is OtpVerifyOutcome.Verified -> {
                                resumeOnce(
                                    continuation,
                                    resumed,
                                    OtpDispatchOutcome.AutoVerified(
                                        verifiedPhoneE164 = outcome.verifiedPhoneE164,
                                        providerUid = outcome.providerUid
                                    )
                                )
                            }
                            is OtpVerifyOutcome.InvalidCode -> {
                                resumeOnce(
                                    continuation,
                                    resumed,
                                    OtpDispatchOutcome.Failure(outcome.userMessage)
                                )
                            }
                            is OtpVerifyOutcome.Expired -> {
                                resumeOnce(
                                    continuation,
                                    resumed,
                                    OtpDispatchOutcome.Failure(outcome.userMessage)
                                )
                            }
                            is OtpVerifyOutcome.Failed -> {
                                resumeOnce(
                                    continuation,
                                    resumed,
                                    OtpDispatchOutcome.Failure(outcome.userMessage)
                                )
                            }
                        }
                    }
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    val failure = mapDispatchFailure(e)
                    resumeOnce(continuation, resumed, failure)
                }
            }

            try {
                val builder = PhoneAuthOptions.newBuilder(auth)
                    .setPhoneNumber(phoneNumberE164)
                    .setTimeout(FIREBASE_SMS_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .setActivity(hostActivity)
                    .setCallbacks(callbacks)

                val resendToken = forceResendToken as? PhoneAuthProvider.ForceResendingToken
                if (resendToken != null) {
                    builder.setForceResendingToken(resendToken)
                }

                PhoneAuthProvider.verifyPhoneNumber(builder.build())
            } catch (e: Exception) {
                Log.w(TAG, "PhoneAuthProvider.verifyPhoneNumber failed: ${e::class.java.simpleName}")
                resumeOnce(continuation, resumed, OtpDispatchOutcome.Failure(OtpMessages.UNABLE_TO_SEND))
            }
        }
    }

    override suspend fun verifyOtp(
        verificationSessionId: String,
        smsCode: String
    ): OtpVerifyOutcome {
        if (verificationSessionId.isBlank()) {
            return OtpVerifyOutcome.Expired(OtpMessages.OTP_EXPIRED)
        }
        val cleanCode = smsCode.trim()
        if (cleanCode.length != 6 || !cleanCode.all { it.isDigit() }) {
            return OtpVerifyOutcome.InvalidCode(OtpMessages.INVALID_OTP)
        }

        val auth = runCatching { firebaseAuthProvider() }.getOrElse {
            return OtpVerifyOutcome.Failed(OtpMessages.UNABLE_TO_SEND)
        }

        val credential = try {
            PhoneAuthProvider.getCredential(verificationSessionId, cleanCode)
        } catch (_: Exception) {
            return OtpVerifyOutcome.InvalidCode(OtpMessages.INVALID_OTP)
        }

        return signInWithPhoneCredential(auth, credential, expectedOnAuto = false)
    }

    private suspend fun signInWithPhoneCredential(
        auth: FirebaseAuth,
        credential: PhoneAuthCredential,
        expectedOnAuto: Boolean
    ): OtpVerifyOutcome {
        return try {
            val authResult = auth.signInWithCredential(credential).await()
            val user = authResult.user
            val verifiedPhone = user?.phoneNumber
            if (user == null || verifiedPhone.isNullOrBlank()) {
                OtpVerifyOutcome.Failed(
                    if (expectedOnAuto) OtpMessages.UNABLE_TO_SEND else OtpMessages.INVALID_OTP
                )
            } else {
                OtpVerifyOutcome.Verified(
                    verifiedPhoneE164 = verifiedPhone,
                    providerUid = user.uid
                )
            }
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            val code = e.errorCode
            when (code) {
                "ERROR_SESSION_EXPIRED" -> OtpVerifyOutcome.Expired(OtpMessages.OTP_EXPIRED)
                else -> OtpVerifyOutcome.InvalidCode(OtpMessages.INVALID_OTP)
            }
        } catch (e: FirebaseAuthException) {
            when (e.errorCode) {
                "ERROR_SESSION_EXPIRED" -> OtpVerifyOutcome.Expired(OtpMessages.OTP_EXPIRED)
                "ERROR_TOO_MANY_REQUESTS" -> OtpVerifyOutcome.Failed(OtpMessages.UNABLE_TO_SEND)
                "ERROR_INVALID_VERIFICATION_CODE",
                "ERROR_INVALID_VERIFICATION_ID" -> OtpVerifyOutcome.InvalidCode(OtpMessages.INVALID_OTP)
                else -> OtpVerifyOutcome.Failed(OtpMessages.UNABLE_TO_SEND)
            }
        } catch (_: FirebaseTooManyRequestsException) {
            OtpVerifyOutcome.Failed(OtpMessages.UNABLE_TO_SEND)
        } catch (_: FirebaseNetworkException) {
            OtpVerifyOutcome.Failed(OtpMessages.UNABLE_TO_SEND)
        } catch (e: Exception) {
            Log.w(TAG, "OTP server verification failed: ${e::class.java.simpleName}")
            OtpVerifyOutcome.Failed(OtpMessages.UNABLE_TO_SEND)
        }
    }

    private fun mapDispatchFailure(e: FirebaseException): OtpDispatchOutcome.Failure {
        Log.w(TAG, "SMS OTP dispatch error: ${e::class.java.simpleName}")
        return when (e) {
            is FirebaseTooManyRequestsException -> OtpDispatchOutcome.Failure(
                userMessage = OtpMessages.UNABLE_TO_SEND,
                isRateLimited = true
            )
            is FirebaseAuthInvalidCredentialsException -> OtpDispatchOutcome.Failure(
                userMessage = "Enter a valid mobile number with country code (e.g. +1 555 234 5678)."
            )
            is FirebaseNetworkException -> OtpDispatchOutcome.Failure(
                userMessage = OtpMessages.UNABLE_TO_SEND
            )
            else -> OtpDispatchOutcome.Failure(userMessage = OtpMessages.UNABLE_TO_SEND)
        }
    }

    private fun <T> resumeOnce(
        continuation: CancellableContinuation<T>,
        resumed: AtomicBoolean,
        value: T
    ) {
        if (resumed.compareAndSet(false, true) && continuation.isActive) {
            continuation.resume(value)
        }
    }

    companion object {
        private const val TAG = "FirebasePhoneAuthGateway"
        private const val FIREBASE_SMS_TIMEOUT_SECONDS = 60L
    }
}
