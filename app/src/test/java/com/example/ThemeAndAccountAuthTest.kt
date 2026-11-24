package com.example

import android.app.Activity
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.InstaPlusAccountAuthRepository
import com.example.data.local.InstaPlusAuthSession
import com.example.data.local.OtpPurpose
import com.example.data.local.PreferencesRepository
import com.example.data.model.AccentColorPreset
import com.example.data.model.ThemePreset
import com.example.security.OtpDispatchOutcome
import com.example.security.OtpMessages
import com.example.security.OtpVerifyOutcome
import com.example.security.SmsOtpGateway
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import com.example.ui.screens.SignInScreen
import com.example.ui.theme.InstaPlusTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

/**
 * Test-only SMS gateway double isolated strictly in src/test.
 * Simulates server-side SMS OTP generation, delivery failures, and verification
 * without ever exposing codes on UI/repository models.
 */
private class FakeSmsOtpGateway : SmsOtpGateway {
    var shouldFailDispatch: Boolean = false
    var spoofedVerifiedPhoneOverride: String? = null
    val dispatchedPhones = mutableListOf<String>()

    private val sessionToPhone = mutableMapOf<String, String>()
    private val sessionToCode = mutableMapOf<String, String>()
    private var nextCodeCounter = 418201

    fun latestCodeForSession(phoneE164: String): String {
        val lastSession = sessionToPhone.entries.lastOrNull { it.value == phoneE164 }?.key
            ?: error("No SMS dispatched to $phoneE164")
        return sessionToCode.getValue(lastSession)
    }

    override suspend fun sendOtp(
        phoneNumberE164: String,
        activity: Activity?,
        forceResendToken: Any?
    ): OtpDispatchOutcome {
        if (shouldFailDispatch) {
            return OtpDispatchOutcome.Failure(OtpMessages.UNABLE_TO_SEND)
        }
        val sessionId = "test_session_${UUID.randomUUID()}"
        val serverOtp = (nextCodeCounter++).toString().padStart(6, '0').takeLast(6)
        dispatchedPhones += phoneNumberE164
        sessionToPhone[sessionId] = phoneNumberE164
        sessionToCode[sessionId] = serverOtp
        return OtpDispatchOutcome.CodeSent(
            verificationSessionId = sessionId,
            resendToken = "resend_token_$sessionId"
        )
    }

    override suspend fun verifyOtp(
        verificationSessionId: String,
        smsCode: String
    ): OtpVerifyOutcome {
        val expectedCode = sessionToCode[verificationSessionId]
            ?: return OtpVerifyOutcome.Expired(OtpMessages.OTP_EXPIRED)
        val phone = sessionToPhone.getValue(verificationSessionId)
        return if (smsCode == expectedCode) {
            OtpVerifyOutcome.Verified(
                verifiedPhoneE164 = spoofedVerifiedPhoneOverride ?: phone,
                providerUid = "firebase_phone_uid_${phone.filter { it.isDigit() }}"
            )
        } else {
            OtpVerifyOutcome.InvalidCode(OtpMessages.INVALID_OTP)
        }
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ThemeAndAccountAuthTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `datastore theme customization persists dark mode and accent color`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefsRepo = PreferencesRepository(context)

        prefsRepo.setThemePreset(ThemePreset.INSTAGRAM_LIGHT)
        prefsRepo.setAccentPreset(AccentColorPreset.INSTAGRAM_BLUE)

        val initial = prefsRepo.settingsFlow.first()
        assertFalse(initial.isDarkMode)
        assertEquals(AccentColorPreset.INSTAGRAM_BLUE, initial.accentPreset)

        prefsRepo.setDarkMode(true)
        prefsRepo.setAccentPreset(AccentColorPreset.PRISM_MAGENTA)

        val updatedDark = prefsRepo.settingsFlow.first()
        assertTrue(updatedDark.isDarkMode)
        assertEquals(ThemePreset.AMOLED_DARK, updatedDark.themePreset)
        assertEquals(AccentColorPreset.PRISM_MAGENTA, updatedDark.accentPreset)

        prefsRepo.setDarkMode(false)
        prefsRepo.setAccentPreset(AccentColorPreset.EMERALD_TEAL)

        val updatedLight = prefsRepo.settingsFlow.first()
        assertFalse(updatedLight.isDarkMode)
        assertEquals(ThemePreset.INSTAGRAM_LIGHT, updatedLight.themePreset)
        assertEquals(AccentColorPreset.EMERALD_TEAL, updatedLight.accentPreset)
    }

    @Test
    fun `registration and login require real SMS OTP without exposing OTP on challenge`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val fakeSms = FakeSmsOtpGateway()
        var nowMillis = 1_700_000_000_000L
        val authRepo = InstaPlusAccountAuthRepository(
            context = context,
            smsOtpGateway = fakeSms,
            clock = { nowMillis },
            firebaseSignOutAction = {}
        )
        authRepo.signOut()

        // Invalid fields rejected
        val invalidResult = authRepo.initiateRegistrationOtp(
            firstName = "A",
            lastName = "B",
            username = "a",
            mobileNumber = "123",
            email = "bad-email",
            password = "123"
        )
        assertTrue(invalidResult.isFailure)

        // Valid 6-field registration dispatches SMS privately and returns masked challenge
        val otpChallenge = authRepo.initiateRegistrationOtp(
            firstName = "Alex",
            lastName = "Mercer",
            username = "@NovaPulse",
            mobileNumber = "+1 555 234 5678",
            email = "Nova.Pulse@Example.com",
            password = "StrongPass#2026"
        ).getOrThrow()
        assertEquals(OtpPurpose.CREATE_ACCOUNT, otpChallenge.purpose)
        assertEquals(OtpMessages.OTP_SENT, otpChallenge.statusMessage)
        assertTrue(otpChallenge.maskedMobile.contains("5678"))
        assertFalse(otpChallenge.maskedMobile.contains("5552345678"))

        // Wrong OTP fails with clear message
        val wrongOtp = authRepo.verifyRegistrationOtpAndCreateAccount(
            challengeId = otpChallenge.challengeId,
            enteredOtp = "000000",
            autoSignIn = false
        )
        assertTrue(wrongOtp.isFailure)
        assertEquals(OtpMessages.INVALID_OTP, wrongOtp.exceptionOrNull()?.message)
        assertNull(authRepo.activeSessionFlow.first())

        // Server-delivered OTP activates account cleanly
        val serverSmsCode = fakeSms.latestCodeForSession("+15552345678")
        val created = authRepo.verifyRegistrationOtpAndCreateAccount(
            challengeId = otpChallenge.challengeId,
            enteredOtp = serverSmsCode,
            autoSignIn = false
        ).getOrThrow()
        assertEquals("Alex Mercer", created.fullName)
        assertEquals("novapulse", created.username)
        assertEquals("+15552345678", created.mobileNumber)
        assertEquals("nova.pulse@example.com", created.email)
        assertNull(authRepo.activeSessionFlow.first())

        // Wrong password fails before dispatching SMS
        val dispatchedBeforeBadLogin = fakeSms.dispatchedPhones.size
        assertTrue(authRepo.initiateLoginOtp("novapulse", "WrongPassword").isFailure)
        assertEquals(dispatchedBeforeBadLogin, fakeSms.dispatchedPhones.size)

        // Login across username, mobile, and E-mail requires SMS OTP to registered number
        for (identifier in listOf("@novapulse", "15552345678", "Nova.Pulse@Example.com")) {
            val loginChallenge = authRepo.initiateLoginOtp(identifier, "StrongPass#2026").getOrThrow()
            assertEquals(OtpPurpose.LOGIN, loginChallenge.purpose)
            assertEquals(OtpMessages.OTP_SENT, loginChallenge.statusMessage)
            // Session must remain unauthenticated until OTP is verified
            assertNull(authRepo.activeSessionFlow.first())

            val loginCode = fakeSms.latestCodeForSession("+15552345678")
            val loggedIn = authRepo.verifyLoginOtp(loginChallenge.challengeId, loginCode).getOrThrow()
            assertEquals("novapulse", loggedIn.username)
            assertNotNull(authRepo.activeSessionFlow.first())
            authRepo.signOut()
        }
    }

    @Test
    fun `enforces 5-minute OTP expiration, 45s resend cooldown, brute-force lockout, phone ownership, and SMS failures`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val fakeSms = FakeSmsOtpGateway()
        var nowMillis = 2_000_000_000_000L
        val authRepo = InstaPlusAccountAuthRepository(
            context = context,
            smsOtpGateway = fakeSms,
            clock = { nowMillis },
            firebaseSignOutAction = {}
        )
        authRepo.signOut()

        // 1. SMS gateway dispatch failure returns clean message
        fakeSms.shouldFailDispatch = true
        val dispatchFail = authRepo.initiateRegistrationOtp(
            firstName = "Sam",
            lastName = "Ray",
            username = "samray",
            mobileNumber = "+1 555 888 9999",
            email = "sam@example.com",
            password = "Password#99"
        )
        assertTrue(dispatchFail.isFailure)
        assertEquals(OtpMessages.UNABLE_TO_SEND, dispatchFail.exceptionOrNull()?.message)
        fakeSms.shouldFailDispatch = false

        // 2. Resend cooldown (45s) prevents immediate resend, allows after 45s
        val challenge = authRepo.initiateRegistrationOtp(
            firstName = "Sam",
            lastName = "Ray",
            username = "samray",
            mobileNumber = "+1 555 888 9999",
            email = "sam@example.com",
            password = "Password#99"
        ).getOrThrow()

        nowMillis += 20_000L
        assertTrue(authRepo.resendOtp(challenge.challengeId).isFailure)

        nowMillis += 26_000L // 46s elapsed > 45s cooldown
        val resentChallenge = authRepo.resendOtp(challenge.challengeId).getOrThrow()
        assertEquals(challenge.challengeId, resentChallenge.challengeId)

        // 3. Phone ownership mismatch (verified phone != registered phone) is rejected
        fakeSms.spoofedVerifiedPhoneOverride = "+15550001111"
        val spoofedResult = authRepo.verifyRegistrationOtpAndCreateAccount(
            challengeId = resentChallenge.challengeId,
            enteredOtp = fakeSms.latestCodeForSession("+15558889999")
        )
        assertTrue(spoofedResult.isFailure)
        assertTrue(spoofedResult.exceptionOrNull() is SecurityException)
        fakeSms.spoofedVerifiedPhoneOverride = null

        // 4. 5 wrong attempts invalidate challenge and trigger phone lockout
        val challenge2 = authRepo.initiateRegistrationOtp(
            firstName = "Sam",
            lastName = "Ray",
            username = "samray",
            mobileNumber = "+1 555 888 9999",
            email = "sam@example.com",
            password = "Password#99"
        ).getOrThrow()

        repeat(4) {
            val attemptFail = authRepo.verifyRegistrationOtpAndCreateAccount(
                challengeId = challenge2.challengeId,
                enteredOtp = "999999"
            )
            assertEquals(OtpMessages.INVALID_OTP, attemptFail.exceptionOrNull()?.message)
        }
        val fifthFail = authRepo.verifyRegistrationOtpAndCreateAccount(
            challengeId = challenge2.challengeId,
            enteredOtp = "999999"
        )
        assertEquals(OtpMessages.TOO_MANY_ATTEMPTS, fifthFail.exceptionOrNull()?.message)

        // Immediate re-initiation during lockout is blocked
        val lockedOut = authRepo.initiateRegistrationOtp(
            firstName = "Sam",
            lastName = "Ray",
            username = "samray",
            mobileNumber = "+1 555 888 9999",
            email = "sam@example.com",
            password = "Password#99"
        )
        assertEquals(OtpMessages.UNABLE_TO_SEND, lockedOut.exceptionOrNull()?.message)

        // 5. Advance past lockout (65s), initiate again, and test 5-minute OTP expiration
        nowMillis += 65_000L
        val challenge3 = authRepo.initiateRegistrationOtp(
            firstName = "Sam",
            lastName = "Ray",
            username = "samray",
            mobileNumber = "+1 555 888 9999",
            email = "sam@example.com",
            password = "Password#99"
        ).getOrThrow()

        nowMillis += InstaPlusAccountAuthRepository.OTP_TTL_MILLIS + 1_000L
        val expiredResult = authRepo.verifyRegistrationOtpAndCreateAccount(
            challengeId = challenge3.challengeId,
            enteredOtp = fakeSms.latestCodeForSession("+15558889999")
        )
        assertTrue(expiredResult.isFailure)
        assertEquals(OtpMessages.OTP_EXPIRED, expiredResult.exceptionOrNull()?.message)
    }

    @Test
    fun `multi account switching and forgot password SMS OTP reset flow`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val fakeSms = FakeSmsOtpGateway()
        var nowMillis = 3_000_000_000_000L
        val authRepo = InstaPlusAccountAuthRepository(
            context = context,
            smsOtpGateway = fakeSms,
            clock = { nowMillis },
            firebaseSignOutAction = {}
        )

        suspend fun registerVerified(
            first: String,
            last: String,
            username: String,
            mobile: String,
            email: String,
            pass: String
        ): InstaPlusAuthSession {
            val ch = authRepo.initiateRegistrationOtp(first, last, username, mobile, email, pass).getOrThrow()
            val normalized = mobile.filter { it.isDigit() || it == '+' }
            val code = fakeSms.latestCodeForSession(normalized)
            return authRepo.verifyRegistrationOtpAndCreateAccount(ch.challengeId, code, autoSignIn = true).getOrThrow()
        }

        val first = registerVerified(
            first = "Maya",
            last = "Patel",
            username = "maya_main",
            mobile = "+91 98765 11111",
            email = "maya@example.com",
            pass = "OldPassword1"
        )
        val second = registerVerified(
            first = "Maya",
            last = "Studio",
            username = "maya_studio",
            mobile = "+91 98765 22222",
            email = "studio@example.com",
            pass = "StudioPass2"
        )

        val all = authRepo.allAccountsFlow.first()
        assertTrue(all.any { it.uid == first.uid })
        assertTrue(all.any { it.uid == second.uid })
        assertEquals(second.uid, authRepo.activeSessionFlow.first()?.uid)

        authRepo.switchActiveAccount(first.uid).getOrThrow()
        assertEquals("maya_main", authRepo.activeSessionFlow.first()?.username)

        // Forgot password via mobile -> SMS OTP dispatched ONLY to Maya's registered mobile
        val resetChallenge = authRepo.initiatePasswordResetOtp("9876511111").getOrThrow()
        assertEquals(OtpPurpose.RESET_PASSWORD, resetChallenge.purpose)

        nowMillis += InstaPlusAccountAuthRepository.RESEND_COOLDOWN_MILLIS + 1_000L
        val refreshed = authRepo.resendOtp(resetChallenge.challengeId).getOrThrow()

        // Setting password before OTP verification is blocked
        assertTrue(
            authRepo.completePasswordReset(
                challengeId = refreshed.challengeId,
                newPassword = "NewPassword9",
                confirmPassword = "NewPassword9"
            ).isFailure
        )

        val resetOtp = fakeSms.latestCodeForSession("+919876511111")
        assertTrue(authRepo.verifyResetOtp(refreshed.challengeId, resetOtp).isSuccess)

        // Mismatched confirmation rejected
        assertTrue(
            authRepo.completePasswordReset(
                challengeId = refreshed.challengeId,
                newPassword = "NewPassword9",
                confirmPassword = "Mismatch9"
            ).isFailure
        )

        // Matching password succeeds
        assertTrue(
            authRepo.completePasswordReset(
                challengeId = refreshed.challengeId,
                newPassword = "NewPassword9",
                confirmPassword = "NewPassword9"
            ).isSuccess
        )

        authRepo.signOut()
        assertTrue(authRepo.initiateLoginOtp("maya@example.com", "OldPassword1").isFailure)

        val loginWithNewPass = authRepo.initiateLoginOtp("maya@example.com", "NewPassword9").getOrThrow()
        val loginOtp = fakeSms.latestCodeForSession("+919876511111")
        assertTrue(authRepo.verifyLoginOtp(loginWithNewPass.challengeId, loginOtp).isSuccess)

        authRepo.removeAccount(second.uid)
        assertFalse(authRepo.allAccountsFlow.first().any { it.uid == second.uid })
    }

    @Test
    fun `SignInScreen never renders OTP code on UI and verifies SMS OTP for signup and login`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val fakeSms = FakeSmsOtpGateway()
        val authRepo = InstaPlusAccountAuthRepository(
            context = context,
            smsOtpGateway = fakeSms,
            firebaseSignOutAction = {}
        )
        runBlocking { authRepo.signOut() }

        composeRule.setContent {
            InstaPlusTheme(isDarkMode = false) {
                SignInScreen(
                    authRepo = authRepo,
                    isDarkMode = false,
                    onToggleDarkMode = {}
                )
            }
        }

        // Switch to Create Account tab
        composeRule.onNodeWithTag("auth_tab_signup").performClick()

        // Fill all 6 required fields
        composeRule.onNodeWithTag("signup_first_name_input").performScrollTo().performTextInput("Elena")
        composeRule.onNodeWithTag("signup_last_name_input").performScrollTo().performTextInput("Vance")
        composeRule.onNodeWithTag("signup_username_input").performScrollTo().performTextInput("elena_ui")
        composeRule.onNodeWithTag("signup_mobile_input").performScrollTo().performTextInput("+1 555 777 1212")
        composeRule.onNodeWithTag("signup_email_input").performScrollTo().performTextInput("elena.ui@example.com")
        composeRule.onNodeWithTag("signup_password_input").performScrollTo().performTextInput("SecureUi#2026")
        composeRule.onNodeWithTag("instaplus_signup_button").performScrollTo().performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            fakeSms.dispatchedPhones.contains("+15557771212")
        }
        composeRule.waitForIdle()

        val signupSmsCode = fakeSms.latestCodeForSession("+15557771212")

        // Crucial security assertion: status message is visible, OTP code is NEVER rendered in UI
        composeRule.onNodeWithText(OtpMessages.OTP_SENT, substring = true).assertIsDisplayed()
        composeRule.onNodeWithTag("otp_delivered_code_text").assertDoesNotExist()
        composeRule.onNodeWithText(signupSmsCode, substring = true).assertDoesNotExist()

        // Enter wrong OTP -> clean error shown
        composeRule.onNodeWithTag("otp_code_input").performScrollTo().performTextInput("111111")
        composeRule.onNodeWithTag("verify_otp_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(OtpMessages.INVALID_OTP, substring = true).assertIsDisplayed()

        // Replace with real SMS OTP -> account created and transitions to Log In
        composeRule.onNodeWithTag("otp_code_input").performScrollTo().performTextReplacement(signupSmsCode)
        composeRule.onNodeWithTag("verify_otp_button").performScrollTo().performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            runBlocking { authRepo.getRegisteredUsernames().contains("elena_ui") }
        }
        composeRule.waitForIdle()

        // Complete Login -> SMS OTP on UI
        composeRule.onNodeWithTag("login_password_input").performScrollTo().performTextInput("SecureUi#2026")
        composeRule.onNodeWithTag("instaplus_login_button").performScrollTo().performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            fakeSms.dispatchedPhones.count { it == "+15557771212" } >= 2
        }
        composeRule.waitForIdle()

        val loginSmsCode = fakeSms.latestCodeForSession("+15557771212")
        composeRule.onNodeWithText(OtpMessages.OTP_SENT, substring = true).assertIsDisplayed()
        composeRule.onNodeWithText(loginSmsCode, substring = true).assertDoesNotExist()
        composeRule.onNodeWithTag("otp_code_input").performScrollTo().performTextInput(loginSmsCode)
        composeRule.onNodeWithTag("verify_otp_button").performScrollTo().performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            runBlocking { authRepo.activeSessionFlow.first()?.username == "elena_ui" }
        }
        assertEquals("elena_ui", runBlocking { authRepo.activeSessionFlow.first()?.username })
    }
}
