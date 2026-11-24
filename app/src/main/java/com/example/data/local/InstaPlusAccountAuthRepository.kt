package com.example.data.local

import android.app.Activity
import android.content.Context
import android.util.Patterns
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.security.FirebasePhoneAuthOtpGateway
import com.example.security.OtpDispatchOutcome
import com.example.security.OtpMessages
import com.example.security.OtpVerifyOutcome
import com.example.security.SecureLocalCipher
import com.example.security.SmsOtpGateway
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

private val Context.authDataStore: DataStore<Preferences> by preferencesDataStore(name = "instaplus_accounts_store")

@Serializable
data class StoredInstaPlusAccount(
    val uid: String,
    val firstName: String = "",
    val lastName: String = "",
    val username: String,
    val mobileNumber: String,
    val email: String,
    val passwordSalt: String,
    val passwordHash: String,
    val isPhoneVerified: Boolean = true,
    val providerPhoneUid: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis()
)

data class InstaPlusAuthSession(
    val uid: String,
    val firstName: String,
    val lastName: String,
    val username: String,
    val mobileNumber: String,
    val email: String,
    val createdAtMillis: Long
) {
    val fullName: String
        get() = listOf(firstName.trim(), lastName.trim())
            .filter { it.isNotEmpty() }
            .joinToString(" ")
            .ifBlank { username }
}

enum class OtpPurpose {
    CREATE_ACCOUNT,
    LOGIN,
    RESET_PASSWORD
}

/**
 * Public UI representation of an active SMS OTP verification challenge.
 *
 * Intentionally excludes any OTP code, verification ID, or secret resend token.
 */
data class OtpChallenge(
    val challengeId: String,
    val purpose: OtpPurpose,
    val targetUsername: String,
    val maskedEmail: String,
    val maskedMobile: String,
    val expiresAtMillis: Long,
    val resendAvailableAtMillis: Long,
    val attemptsRemaining: Int,
    val statusMessage: String = OtpMessages.OTP_SENT,
    val autoVerifiedSession: InstaPlusAuthSession? = null,
    val autoVerifiedReset: Boolean = false
)

private data class InternalOtpState(
    val challengeId: String,
    val purpose: OtpPurpose,
    val verificationSessionId: String,
    val resendToken: Any?,
    val targetUsername: String,
    val targetEmail: String,
    val targetMobileE164: String,
    val expiresAtMillis: Long,
    val resendAvailableAtMillis: Long,
    val attemptsLeft: Int,
    val resendsUsed: Int = 0,
    val isVerified: Boolean = false,
    val pendingFirstName: String = "",
    val pendingLastName: String = "",
    val pendingPassword: String = "",
    val targetAccountUid: String? = null
)

class InstaPlusAccountAuthRepository(
    private val context: Context,
    private val smsOtpGateway: SmsOtpGateway = FirebasePhoneAuthOtpGateway(),
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val firebaseSignOutAction: () -> Unit = { runCatching { Firebase.auth.signOut() } }
) {

    private val json = Json { ignoreUnknownKeys = true }
    private val pendingChallenges = ConcurrentHashMap<String, InternalOtpState>()
    private val phoneLockoutsUntil = ConcurrentHashMap<String, Long>()

    private object Keys {
        val ENCRYPTED_ACCOUNTS = stringPreferencesKey("encrypted_accounts_blob")
        val ACTIVE_UID = stringPreferencesKey("active_account_uid")
    }

    val allAccountsFlow: Flow<List<InstaPlusAuthSession>> = context.authDataStore.data.map { prefs ->
        decodeAccounts(prefs[Keys.ENCRYPTED_ACCOUNTS]).map { it.toPublicSession() }
    }

    val activeSessionFlow: Flow<InstaPlusAuthSession?> = context.authDataStore.data.map { prefs ->
        val activeUid = prefs[Keys.ACTIVE_UID] ?: return@map null
        val accounts = decodeAccounts(prefs[Keys.ENCRYPTED_ACCOUNTS])
        accounts.firstOrNull { it.uid == activeUid }?.toPublicSession()
    }

    suspend fun getRegisteredUsernames(): List<String> {
        val prefs = context.authDataStore.data.first()
        return decodeAccounts(prefs[Keys.ENCRYPTED_ACCOUNTS]).map { it.username }
    }

    suspend fun initiateRegistrationOtp(
        firstName: String,
        lastName: String,
        username: String,
        mobileNumber: String,
        email: String,
        password: String,
        activity: Activity? = null
    ): Result<OtpChallenge> {
        val cleanFirst = firstName.trim()
        val cleanLast = lastName.trim()
        val cleanUsername = username.trim().removePrefix("@").lowercase()
        val cleanEmail = email.trim().lowercase()

        if (cleanFirst.length < 2) {
            return Result.failure(IllegalArgumentException("Enter your first name (at least 2 characters)."))
        }
        if (cleanLast.length < 2) {
            return Result.failure(IllegalArgumentException("Enter your last name (at least 2 characters)."))
        }
        if (cleanUsername.length < 3 || !cleanUsername.matches(Regex("^[a-z0-9._]+$"))) {
            return Result.failure(IllegalArgumentException("Username must be at least 3 characters (letters, numbers, . or _)."))
        }
        val mobileE164 = normalizeToE164(mobileNumber)
            ?: return Result.failure(
                IllegalArgumentException("Enter a valid mobile number with country code (e.g. +1 555 234 5678).")
            )
        if (!isValidEmail(cleanEmail)) {
            return Result.failure(IllegalArgumentException("Enter a valid E-mail address."))
        }
        if (password.length < 6) {
            return Result.failure(IllegalArgumentException("Password must be at least 6 characters."))
        }
        if (isPhoneLockedOut(mobileE164)) {
            return Result.failure(IllegalStateException(OtpMessages.UNABLE_TO_SEND))
        }

        val prefs = context.authDataStore.data.first()
        val existing = decodeAccounts(prefs[Keys.ENCRYPTED_ACCOUNTS])

        if (existing.any { it.username.equals(cleanUsername, ignoreCase = true) }) {
            return Result.failure(IllegalArgumentException("Username @$cleanUsername is already registered."))
        }
        if (existing.any { phoneNumbersMatchForLookup(it.mobileNumber, mobileE164) }) {
            return Result.failure(IllegalArgumentException("This mobile number is already linked to an account."))
        }
        if (existing.any { it.email.equals(cleanEmail, ignoreCase = true) }) {
            return Result.failure(IllegalArgumentException("This E-mail address is already registered."))
        }

        return when (val dispatch = smsOtpGateway.sendOtp(mobileE164, activity)) {
            is OtpDispatchOutcome.Failure -> {
                Result.failure(IllegalStateException(dispatch.userMessage))
            }
            is OtpDispatchOutcome.AutoVerified -> {
                if (dispatch.verifiedPhoneE164 != mobileE164) {
                    return Result.failure(SecurityException("Verified mobile number does not match the entered mobile number."))
                }
                val created = persistNewVerifiedAccount(
                    firstName = cleanFirst,
                    lastName = cleanLast,
                    username = cleanUsername,
                    mobileE164 = mobileE164,
                    email = cleanEmail,
                    rawPassword = password,
                    providerUid = dispatch.providerUid,
                    autoSignIn = false
                )
                if (created.isFailure) return Result.failure(created.exceptionOrNull()!!)
                val now = clock()
                Result.success(
                    OtpChallenge(
                        challengeId = UUID.randomUUID().toString(),
                        purpose = OtpPurpose.CREATE_ACCOUNT,
                        targetUsername = cleanUsername,
                        maskedEmail = maskEmail(cleanEmail),
                        maskedMobile = maskPhoneNumber(mobileE164),
                        expiresAtMillis = now + OTP_TTL_MILLIS,
                        resendAvailableAtMillis = now + RESEND_COOLDOWN_MILLIS,
                        attemptsRemaining = MAX_OTP_ATTEMPTS,
                        statusMessage = OtpMessages.OTP_SENT,
                        autoVerifiedSession = created.getOrThrow()
                    )
                )
            }
            is OtpDispatchOutcome.CodeSent -> {
                val now = clock()
                val challengeId = UUID.randomUUID().toString()
                val state = InternalOtpState(
                    challengeId = challengeId,
                    purpose = OtpPurpose.CREATE_ACCOUNT,
                    verificationSessionId = dispatch.verificationSessionId,
                    resendToken = dispatch.resendToken,
                    targetUsername = cleanUsername,
                    targetEmail = cleanEmail,
                    targetMobileE164 = mobileE164,
                    expiresAtMillis = now + OTP_TTL_MILLIS,
                    resendAvailableAtMillis = now + RESEND_COOLDOWN_MILLIS,
                    attemptsLeft = MAX_OTP_ATTEMPTS,
                    pendingFirstName = cleanFirst,
                    pendingLastName = cleanLast,
                    pendingPassword = password
                )
                pendingChallenges[challengeId] = state
                Result.success(state.toPublicChallenge())
            }
        }
    }

    suspend fun verifyRegistrationOtpAndCreateAccount(
        challengeId: String,
        enteredOtp: String,
        autoSignIn: Boolean = false
    ): Result<InstaPlusAuthSession> {
        val state = pendingChallenges[challengeId]
            ?: return Result.failure(IllegalArgumentException(OtpMessages.OTP_EXPIRED))
        if (state.purpose != OtpPurpose.CREATE_ACCOUNT) {
            return Result.failure(IllegalArgumentException("Invalid verification session."))
        }

        val verificationResult = verifyChallengeCodeOnBackend(challengeId, state, enteredOtp)
        if (verificationResult.isFailure) {
            return Result.failure(verificationResult.exceptionOrNull()!!)
        }
        val verified = verificationResult.getOrThrow()

        val created = persistNewVerifiedAccount(
            firstName = state.pendingFirstName,
            lastName = state.pendingLastName,
            username = state.targetUsername,
            mobileE164 = state.targetMobileE164,
            email = state.targetEmail,
            rawPassword = state.pendingPassword,
            providerUid = verified.providerUid,
            autoSignIn = autoSignIn
        )
        if (created.isSuccess) {
            pendingChallenges.remove(challengeId)
            phoneLockoutsUntil.remove(state.targetMobileE164)
        } else {
            pendingChallenges.remove(challengeId)
        }
        return created
    }

    suspend fun initiateLoginOtp(
        identifier: String,
        password: String,
        activity: Activity? = null
    ): Result<OtpChallenge> {
        val rawId = identifier.trim()
        if (rawId.isBlank() || password.isBlank()) {
            return Result.failure(IllegalArgumentException("Enter your username, mobile number, or E-mail and password."))
        }

        val prefs = context.authDataStore.data.first()
        val accounts = decodeAccounts(prefs[Keys.ENCRYPTED_ACCOUNTS])
        if (accounts.isEmpty()) {
            return Result.failure(IllegalArgumentException("No InstaPlus account found. Create an account first."))
        }

        val matched = findAccountByIdentifier(accounts, rawId)
            ?: return Result.failure(IllegalArgumentException("Invalid credentials or account not found."))

        val validPassword = SecureLocalCipher.verifySecret(password, matched.passwordSalt, matched.passwordHash)
        if (!validPassword) {
            return Result.failure(IllegalArgumentException("Incorrect password. Please try again."))
        }

        // Security Requirement #13: Always dispatch login OTP strictly to the account's own registered number.
        val registeredMobileE164 = normalizeToE164(matched.mobileNumber)
            ?: return Result.failure(IllegalStateException(OtpMessages.UNABLE_TO_SEND))

        if (isPhoneLockedOut(registeredMobileE164)) {
            return Result.failure(IllegalStateException(OtpMessages.UNABLE_TO_SEND))
        }

        return when (val dispatch = smsOtpGateway.sendOtp(registeredMobileE164, activity)) {
            is OtpDispatchOutcome.Failure -> {
                Result.failure(IllegalStateException(dispatch.userMessage))
            }
            is OtpDispatchOutcome.AutoVerified -> {
                if (dispatch.verifiedPhoneE164 != registeredMobileE164) {
                    return Result.failure(SecurityException("Verified mobile number does not belong to this account."))
                }
                context.authDataStore.edit { store ->
                    store[Keys.ACTIVE_UID] = matched.uid
                }
                val now = clock()
                Result.success(
                    OtpChallenge(
                        challengeId = UUID.randomUUID().toString(),
                        purpose = OtpPurpose.LOGIN,
                        targetUsername = matched.username,
                        maskedEmail = maskEmail(matched.email),
                        maskedMobile = maskPhoneNumber(registeredMobileE164),
                        expiresAtMillis = now + OTP_TTL_MILLIS,
                        resendAvailableAtMillis = now + RESEND_COOLDOWN_MILLIS,
                        attemptsRemaining = MAX_OTP_ATTEMPTS,
                        statusMessage = OtpMessages.OTP_SENT,
                        autoVerifiedSession = matched.toPublicSession()
                    )
                )
            }
            is OtpDispatchOutcome.CodeSent -> {
                val now = clock()
                val challengeId = UUID.randomUUID().toString()
                val state = InternalOtpState(
                    challengeId = challengeId,
                    purpose = OtpPurpose.LOGIN,
                    verificationSessionId = dispatch.verificationSessionId,
                    resendToken = dispatch.resendToken,
                    targetUsername = matched.username,
                    targetEmail = matched.email,
                    targetMobileE164 = registeredMobileE164,
                    expiresAtMillis = now + OTP_TTL_MILLIS,
                    resendAvailableAtMillis = now + RESEND_COOLDOWN_MILLIS,
                    attemptsLeft = MAX_OTP_ATTEMPTS,
                    targetAccountUid = matched.uid
                )
                pendingChallenges[challengeId] = state
                Result.success(state.toPublicChallenge())
            }
        }
    }

    suspend fun verifyLoginOtp(
        challengeId: String,
        enteredOtp: String
    ): Result<InstaPlusAuthSession> {
        val state = pendingChallenges[challengeId]
            ?: return Result.failure(IllegalArgumentException(OtpMessages.OTP_EXPIRED))
        if (state.purpose != OtpPurpose.LOGIN) {
            return Result.failure(IllegalArgumentException("Invalid verification session."))
        }

        val verificationResult = verifyChallengeCodeOnBackend(challengeId, state, enteredOtp)
        if (verificationResult.isFailure) {
            return Result.failure(verificationResult.exceptionOrNull()!!)
        }

        val targetUid = state.targetAccountUid
            ?: return Result.failure(IllegalArgumentException("Account missing for verification session."))
        val prefs = context.authDataStore.data.first()
        val accounts = decodeAccounts(prefs[Keys.ENCRYPTED_ACCOUNTS])
        val matched = accounts.firstOrNull { it.uid == targetUid }
            ?: run {
                pendingChallenges.remove(challengeId)
                return Result.failure(IllegalArgumentException("Account no longer exists."))
            }

        val storedMobileE164 = normalizeToE164(matched.mobileNumber)
        if (storedMobileE164 != state.targetMobileE164) {
            pendingChallenges.remove(challengeId)
            return Result.failure(SecurityException("Verified mobile number does not match the registered account."))
        }

        context.authDataStore.edit { store ->
            store[Keys.ACTIVE_UID] = matched.uid
        }
        pendingChallenges.remove(challengeId)
        phoneLockoutsUntil.remove(state.targetMobileE164)
        return Result.success(matched.toPublicSession())
    }

    suspend fun initiatePasswordResetOtp(
        identifier: String,
        activity: Activity? = null
    ): Result<OtpChallenge> {
        val rawId = identifier.trim()
        if (rawId.isBlank()) {
            return Result.failure(IllegalArgumentException("Enter your username, mobile number, or E-mail."))
        }
        val prefs = context.authDataStore.data.first()
        val accounts = decodeAccounts(prefs[Keys.ENCRYPTED_ACCOUNTS])
        val matched = findAccountByIdentifier(accounts, rawId)
            ?: return Result.failure(IllegalArgumentException("No InstaPlus account matches \"$rawId\"."))

        val registeredMobileE164 = normalizeToE164(matched.mobileNumber)
            ?: return Result.failure(IllegalStateException(OtpMessages.UNABLE_TO_SEND))

        if (isPhoneLockedOut(registeredMobileE164)) {
            return Result.failure(IllegalStateException(OtpMessages.UNABLE_TO_SEND))
        }

        return when (val dispatch = smsOtpGateway.sendOtp(registeredMobileE164, activity)) {
            is OtpDispatchOutcome.Failure -> {
                Result.failure(IllegalStateException(dispatch.userMessage))
            }
            is OtpDispatchOutcome.AutoVerified -> {
                if (dispatch.verifiedPhoneE164 != registeredMobileE164) {
                    return Result.failure(SecurityException("Verified mobile number does not belong to this account."))
                }
                val now = clock()
                val challengeId = UUID.randomUUID().toString()
                val state = InternalOtpState(
                    challengeId = challengeId,
                    purpose = OtpPurpose.RESET_PASSWORD,
                    verificationSessionId = "",
                    resendToken = null,
                    targetUsername = matched.username,
                    targetEmail = matched.email,
                    targetMobileE164 = registeredMobileE164,
                    expiresAtMillis = now + OTP_TTL_MILLIS,
                    resendAvailableAtMillis = now + RESEND_COOLDOWN_MILLIS,
                    attemptsLeft = MAX_OTP_ATTEMPTS,
                    isVerified = true,
                    targetAccountUid = matched.uid
                )
                pendingChallenges[challengeId] = state
                Result.success(state.toPublicChallenge(autoVerifiedReset = true))
            }
            is OtpDispatchOutcome.CodeSent -> {
                val now = clock()
                val challengeId = UUID.randomUUID().toString()
                val state = InternalOtpState(
                    challengeId = challengeId,
                    purpose = OtpPurpose.RESET_PASSWORD,
                    verificationSessionId = dispatch.verificationSessionId,
                    resendToken = dispatch.resendToken,
                    targetUsername = matched.username,
                    targetEmail = matched.email,
                    targetMobileE164 = registeredMobileE164,
                    expiresAtMillis = now + OTP_TTL_MILLIS,
                    resendAvailableAtMillis = now + RESEND_COOLDOWN_MILLIS,
                    attemptsLeft = MAX_OTP_ATTEMPTS,
                    targetAccountUid = matched.uid
                )
                pendingChallenges[challengeId] = state
                Result.success(state.toPublicChallenge())
            }
        }
    }

    suspend fun verifyResetOtp(challengeId: String, enteredOtp: String): Result<Unit> {
        val state = pendingChallenges[challengeId]
            ?: return Result.failure(IllegalArgumentException(OtpMessages.OTP_EXPIRED))
        if (state.purpose != OtpPurpose.RESET_PASSWORD) {
            return Result.failure(IllegalArgumentException("Invalid password reset session."))
        }

        val verification = verifyChallengeCodeOnBackend(challengeId, state, enteredOtp)
        if (verification.isFailure) return Result.failure(verification.exceptionOrNull()!!)

        pendingChallenges[challengeId] = state.copy(isVerified = true)
        phoneLockoutsUntil.remove(state.targetMobileE164)
        return Result.success(Unit)
    }

    suspend fun completePasswordReset(
        challengeId: String,
        newPassword: String,
        confirmPassword: String
    ): Result<InstaPlusAuthSession> {
        val state = pendingChallenges[challengeId]
            ?: return Result.failure(IllegalArgumentException(OtpMessages.OTP_EXPIRED))
        if (clock() > state.expiresAtMillis) {
            pendingChallenges.remove(challengeId)
            return Result.failure(IllegalArgumentException(OtpMessages.OTP_EXPIRED))
        }
        if (state.purpose != OtpPurpose.RESET_PASSWORD || !state.isVerified) {
            return Result.failure(IllegalArgumentException("Verify the SMS OTP before setting a new password."))
        }
        if (newPassword.length < 6) {
            return Result.failure(IllegalArgumentException("New password must be at least 6 characters."))
        }
        if (newPassword != confirmPassword) {
            return Result.failure(IllegalArgumentException("Passwords do not match."))
        }

        val targetUid = state.targetAccountUid
            ?: return Result.failure(IllegalArgumentException("Target account missing."))

        val prefs = context.authDataStore.data.first()
        val existing = decodeAccounts(prefs[Keys.ENCRYPTED_ACCOUNTS])
        val targetIndex = existing.indexOfFirst { it.uid == targetUid }
        if (targetIndex < 0) {
            pendingChallenges.remove(challengeId)
            return Result.failure(IllegalArgumentException("Account no longer exists."))
        }

        val newSalt = SecureLocalCipher.generateSaltBase64()
        val newHash = SecureLocalCipher.hashSecret(newPassword, newSalt)
        val updatedAccount = existing[targetIndex].copy(
            passwordSalt = newSalt,
            passwordHash = newHash
        )
        val updatedList = existing.toMutableList().apply { set(targetIndex, updatedAccount) }
        val encryptedBlob = SecureLocalCipher.encrypt(json.encodeToString(updatedList))

        context.authDataStore.edit { store ->
            store[Keys.ENCRYPTED_ACCOUNTS] = encryptedBlob
        }
        pendingChallenges.remove(challengeId)
        return Result.success(updatedAccount.toPublicSession())
    }

    suspend fun resendOtp(
        challengeId: String,
        activity: Activity? = null
    ): Result<OtpChallenge> {
        val current = pendingChallenges[challengeId]
            ?: return Result.failure(IllegalArgumentException(OtpMessages.OTP_EXPIRED))
        val now = clock()
        if (now < current.resendAvailableAtMillis) {
            val remainingSeconds = ((current.resendAvailableAtMillis - now + 999L) / 1000L).coerceAtLeast(1L)
            return Result.failure(IllegalStateException("Please wait ${remainingSeconds}s before requesting a new OTP."))
        }
        if (isPhoneLockedOut(current.targetMobileE164) || current.resendsUsed >= MAX_RESENDS_PER_SESSION) {
            return Result.failure(IllegalStateException(OtpMessages.UNABLE_TO_SEND))
        }

        return when (
            val dispatch = smsOtpGateway.sendOtp(
                phoneNumberE164 = current.targetMobileE164,
                activity = activity,
                forceResendToken = current.resendToken
            )
        ) {
            is OtpDispatchOutcome.Failure -> {
                Result.failure(IllegalStateException(dispatch.userMessage))
            }
            is OtpDispatchOutcome.AutoVerified -> {
                if (dispatch.verifiedPhoneE164 != current.targetMobileE164) {
                    return Result.failure(SecurityException("Verified mobile number does not match the registered account."))
                }
                val updated = current.copy(
                    isVerified = true,
                    attemptsLeft = MAX_OTP_ATTEMPTS,
                    expiresAtMillis = now + OTP_TTL_MILLIS,
                    resendAvailableAtMillis = now + RESEND_COOLDOWN_MILLIS
                )
                pendingChallenges[challengeId] = updated
                when (current.purpose) {
                    OtpPurpose.CREATE_ACCOUNT -> {
                        val created = persistNewVerifiedAccount(
                            firstName = current.pendingFirstName,
                            lastName = current.pendingLastName,
                            username = current.targetUsername,
                            mobileE164 = current.targetMobileE164,
                            email = current.targetEmail,
                            rawPassword = current.pendingPassword,
                            providerUid = dispatch.providerUid,
                            autoSignIn = false
                        )
                        if (created.isFailure) return Result.failure(created.exceptionOrNull()!!)
                        pendingChallenges.remove(challengeId)
                        Result.success(updated.toPublicChallenge(autoVerifiedSession = created.getOrThrow()))
                    }
                    OtpPurpose.LOGIN -> {
                        val uid = current.targetAccountUid
                            ?: return Result.failure(IllegalStateException(OtpMessages.UNABLE_TO_SEND))
                        val session = switchActiveAccount(uid).getOrElse {
                            return Result.failure(it)
                        }
                        pendingChallenges.remove(challengeId)
                        Result.success(updated.toPublicChallenge(autoVerifiedSession = session))
                    }
                    OtpPurpose.RESET_PASSWORD -> {
                        Result.success(updated.toPublicChallenge(autoVerifiedReset = true))
                    }
                }
            }
            is OtpDispatchOutcome.CodeSent -> {
                val refreshed = current.copy(
                    verificationSessionId = dispatch.verificationSessionId,
                    resendToken = dispatch.resendToken ?: current.resendToken,
                    expiresAtMillis = now + OTP_TTL_MILLIS,
                    resendAvailableAtMillis = now + RESEND_COOLDOWN_MILLIS,
                    attemptsLeft = MAX_OTP_ATTEMPTS,
                    resendsUsed = current.resendsUsed + 1,
                    isVerified = false
                )
                pendingChallenges[challengeId] = refreshed
                Result.success(refreshed.toPublicChallenge())
            }
        }
    }

    suspend fun cancelChallenge(challengeId: String) {
        pendingChallenges.remove(challengeId)
    }

    suspend fun switchActiveAccount(uid: String): Result<InstaPlusAuthSession> {
        val prefs = context.authDataStore.data.first()
        val accounts = decodeAccounts(prefs[Keys.ENCRYPTED_ACCOUNTS])
        val matched = accounts.firstOrNull { it.uid == uid }
            ?: return Result.failure(IllegalArgumentException("Account not found."))
        context.authDataStore.edit { store ->
            store[Keys.ACTIVE_UID] = matched.uid
        }
        return Result.success(matched.toPublicSession())
    }

    suspend fun removeAccount(uid: String) {
        val prefs = context.authDataStore.data.first()
        val accounts = decodeAccounts(prefs[Keys.ENCRYPTED_ACCOUNTS])
        val remaining = accounts.filterNot { it.uid == uid }
        val encryptedBlob = if (remaining.isEmpty()) "" else SecureLocalCipher.encrypt(json.encodeToString(remaining))
        context.authDataStore.edit { store ->
            if (encryptedBlob.isEmpty()) {
                store.remove(Keys.ENCRYPTED_ACCOUNTS)
            } else {
                store[Keys.ENCRYPTED_ACCOUNTS] = encryptedBlob
            }
            if (store[Keys.ACTIVE_UID] == uid) {
                val fallback = remaining.firstOrNull()?.uid
                if (fallback != null) {
                    store[Keys.ACTIVE_UID] = fallback
                } else {
                    store.remove(Keys.ACTIVE_UID)
                }
            }
        }
    }

    suspend fun signOut() {
        context.authDataStore.edit { store ->
            store.remove(Keys.ACTIVE_UID)
        }
        firebaseSignOutAction()
    }

    private suspend fun verifyChallengeCodeOnBackend(
        challengeId: String,
        state: InternalOtpState,
        enteredOtp: String
    ): Result<OtpVerifyOutcome.Verified> {
        val now = clock()
        if (now > state.expiresAtMillis) {
            pendingChallenges.remove(challengeId)
            return Result.failure(IllegalArgumentException(OtpMessages.OTP_EXPIRED))
        }
        if (state.attemptsLeft <= 0) {
            pendingChallenges.remove(challengeId)
            return Result.failure(IllegalStateException(OtpMessages.TOO_MANY_ATTEMPTS))
        }

        val cleanOtp = enteredOtp.trim()
        if (cleanOtp.length != 6 || !cleanOtp.all { it.isDigit() }) {
            return recordFailedAttempt(challengeId, state)
        }

        return when (val outcome = smsOtpGateway.verifyOtp(state.verificationSessionId, cleanOtp)) {
            is OtpVerifyOutcome.Verified -> {
                // Security Requirement #13: Verified phone number must exactly match the challenged phone number.
                val verifiedNormalized = normalizeToE164(outcome.verifiedPhoneE164)
                if (verifiedNormalized != state.targetMobileE164) {
                    pendingChallenges.remove(challengeId)
                    return Result.failure(
                        SecurityException("Verified mobile number does not match the registered account phone number.")
                    )
                }
                Result.success(outcome)
            }
            is OtpVerifyOutcome.Expired -> {
                pendingChallenges.remove(challengeId)
                Result.failure(IllegalArgumentException(OtpMessages.OTP_EXPIRED))
            }
            is OtpVerifyOutcome.InvalidCode -> {
                recordFailedAttempt(challengeId, state)
            }
            is OtpVerifyOutcome.Failed -> {
                Result.failure(IllegalStateException(outcome.userMessage))
            }
        }
    }

    private fun recordFailedAttempt(
        challengeId: String,
        state: InternalOtpState
    ): Result<OtpVerifyOutcome.Verified> {
        val remaining = state.attemptsLeft - 1
        if (remaining <= 0) {
            pendingChallenges.remove(challengeId)
            phoneLockoutsUntil[state.targetMobileE164] = clock() + BRUTE_FORCE_LOCKOUT_MILLIS
            return Result.failure(IllegalStateException(OtpMessages.TOO_MANY_ATTEMPTS))
        }
        pendingChallenges[challengeId] = state.copy(attemptsLeft = remaining)
        return Result.failure(IllegalArgumentException(OtpMessages.INVALID_OTP))
    }

    private suspend fun persistNewVerifiedAccount(
        firstName: String,
        lastName: String,
        username: String,
        mobileE164: String,
        email: String,
        rawPassword: String,
        providerUid: String?,
        autoSignIn: Boolean
    ): Result<InstaPlusAuthSession> {
        val prefs = context.authDataStore.data.first()
        val existing = decodeAccounts(prefs[Keys.ENCRYPTED_ACCOUNTS])
        if (existing.any { it.username.equals(username, ignoreCase = true) }) {
            return Result.failure(IllegalArgumentException("Username @$username is already registered."))
        }
        if (existing.any { phoneNumbersMatchForLookup(it.mobileNumber, mobileE164) }) {
            return Result.failure(IllegalArgumentException("This mobile number is already linked to an account."))
        }
        if (existing.any { it.email.equals(email, ignoreCase = true) }) {
            return Result.failure(IllegalArgumentException("This E-mail address is already registered."))
        }

        val salt = SecureLocalCipher.generateSaltBase64()
        val hash = SecureLocalCipher.hashSecret(rawPassword, salt)
        val newAccount = StoredInstaPlusAccount(
            uid = "igp_${UUID.randomUUID().toString().replace("-", "").take(20)}",
            firstName = firstName,
            lastName = lastName,
            username = username,
            mobileNumber = mobileE164,
            email = email,
            passwordSalt = salt,
            passwordHash = hash,
            isPhoneVerified = true,
            providerPhoneUid = providerUid,
            createdAtMillis = clock()
        )

        val updatedList = existing + newAccount
        val encryptedBlob = SecureLocalCipher.encrypt(json.encodeToString(updatedList))

        context.authDataStore.edit { store ->
            store[Keys.ENCRYPTED_ACCOUNTS] = encryptedBlob
            if (autoSignIn) {
                store[Keys.ACTIVE_UID] = newAccount.uid
            }
        }
        return Result.success(newAccount.toPublicSession())
    }

    private fun isPhoneLockedOut(phoneE164: String): Boolean {
        val lockoutUntil = phoneLockoutsUntil[phoneE164] ?: return false
        if (clock() >= lockoutUntil) {
            phoneLockoutsUntil.remove(phoneE164)
            return false
        }
        return true
    }

    private fun findAccountByIdentifier(
        accounts: List<StoredInstaPlusAccount>,
        rawId: String
    ): StoredInstaPlusAccount? {
        val cleanUsernameOrEmail = rawId.removePrefix("@").lowercase()
        return accounts.firstOrNull { acc ->
            acc.username.equals(cleanUsernameOrEmail, ignoreCase = true) ||
                acc.email.equals(cleanUsernameOrEmail, ignoreCase = true) ||
                phoneNumbersMatchForLookup(acc.mobileNumber, rawId)
        }
    }

    private fun phoneNumbersMatchForLookup(storedMobile: String, query: String): Boolean {
        val storedE164 = normalizeToE164(storedMobile)
        val queryE164 = normalizeToE164(query)
        if (storedE164 != null && queryE164 != null) {
            return storedE164 == queryE164
        }
        val storedDigits = normalizeDigits(storedMobile)
        val queryDigits = normalizeDigits(query)
        if (storedDigits.isEmpty() || queryDigits.length < 7) return false
        return storedDigits == queryDigits ||
            (storedDigits.length >= 10 && queryDigits.length >= 10 &&
                storedDigits.takeLast(10) == queryDigits.takeLast(10))
    }

    private fun normalizeToE164(raw: String): String? {
        val trimmed = raw.trim()
        if (!trimmed.startsWith("+")) return null
        val digits = trimmed.drop(1).filter { it.isDigit() }
        if (digits.length !in 8..15 || digits.startsWith("0")) return null
        // Reject unexpected letters in input
        if (trimmed.any { it.isLetter() }) return null
        return "+$digits"
    }

    private fun InternalOtpState.toPublicChallenge(
        autoVerifiedSession: InstaPlusAuthSession? = null,
        autoVerifiedReset: Boolean = false
    ): OtpChallenge = OtpChallenge(
        challengeId = challengeId,
        purpose = purpose,
        targetUsername = targetUsername,
        maskedEmail = maskEmail(targetEmail),
        maskedMobile = maskPhoneNumber(targetMobileE164),
        expiresAtMillis = expiresAtMillis,
        resendAvailableAtMillis = resendAvailableAtMillis,
        attemptsRemaining = attemptsLeft,
        statusMessage = OtpMessages.OTP_SENT,
        autoVerifiedSession = autoVerifiedSession,
        autoVerifiedReset = autoVerifiedReset
    )

    private fun maskPhoneNumber(e164: String): String {
        val digits = e164.filter { it.isDigit() }
        if (digits.length <= 4) return "••••"
        val prefix = if (digits.length > 6) "+${digits.take(2)}" else "+"
        val suffix = digits.takeLast(4)
        val hiddenCount = (digits.length - (if (digits.length > 6) 2 else 0) - 4).coerceAtLeast(3)
        return "$prefix${"•".repeat(hiddenCount)}$suffix"
    }

    private fun maskEmail(email: String): String {
        val parts = email.split("@")
        if (parts.size != 2) return "***"
        val local = parts[0]
        val maskedLocal = if (local.length <= 2) "${local.firstOrNull() ?: '*'}***" else "${local.take(2)}***"
        return "$maskedLocal@${parts[1]}"
    }

    private fun decodeAccounts(storedBlob: String?): List<StoredInstaPlusAccount> {
        if (storedBlob.isNullOrBlank()) return emptyList()
        val decrypted = SecureLocalCipher.decrypt(storedBlob)
        if (decrypted.isBlank()) return emptyList()
        return runCatching {
            json.decodeFromString<List<StoredInstaPlusAccount>>(decrypted)
        }.getOrDefault(emptyList())
    }

    private fun StoredInstaPlusAccount.toPublicSession(): InstaPlusAuthSession = InstaPlusAuthSession(
        uid = uid,
        firstName = firstName,
        lastName = lastName,
        username = username,
        mobileNumber = mobileNumber,
        email = email,
        createdAtMillis = createdAtMillis
    )

    private fun normalizeDigits(input: String): String = input.filter { it.isDigit() }

    private fun isValidEmail(email: String): Boolean {
        return email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    companion object {
        const val OTP_TTL_MILLIS = 5 * 60 * 1000L
        const val RESEND_COOLDOWN_MILLIS = 45 * 1000L
        const val BRUTE_FORCE_LOCKOUT_MILLIS = 60 * 1000L
        const val MAX_OTP_ATTEMPTS = 5
        const val MAX_RESENDS_PER_SESSION = 5
    }
}
