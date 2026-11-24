package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.EnhancedEncryption
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.InstaPlusAccountAuthRepository
import com.example.data.local.InstaPlusAuthSession
import com.example.data.local.OtpChallenge
import com.example.security.OtpMessages
import com.example.security.findActivityOrNull
import com.example.ui.theme.AdShieldGreen
import com.example.ui.theme.InstagramStoryBrush
import kotlinx.coroutines.launch

enum class AuthScreenMode {
    LOG_IN,
    CREATE_ACCOUNT,
    FORGOT_PASSWORD
}

@Composable
fun SignInScreen(
    authRepo: InstaPlusAccountAuthRepository,
    isDarkMode: Boolean,
    onToggleDarkMode: (Boolean) -> Unit,
    savedAccounts: List<InstaPlusAuthSession> = emptyList(),
    initialMode: AuthScreenMode = AuthScreenMode.LOG_IN,
    onQuickSwitchAccount: ((String) -> Unit)? = null,
    onCancelToApp: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val hostActivity = LocalContext.current.findActivityOrNull()

    var screenMode by remember(initialMode) { mutableStateOf(initialMode) }

    // Login state (identifier + password -> SMS OTP verification)
    var loginIdentifier by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }
    var loginOtpChallenge by remember { mutableStateOf<OtpChallenge?>(null) }
    var loginOtpInput by remember { mutableStateOf("") }

    // Create account state (6 fields + SMS OTP verification)
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var signupUsername by remember { mutableStateOf("") }
    var signupMobile by remember { mutableStateOf("") }
    var signupEmail by remember { mutableStateOf("") }
    var signupPassword by remember { mutableStateOf("") }
    var signupOtpChallenge by remember { mutableStateOf<OtpChallenge?>(null) }
    var signupOtpInput by remember { mutableStateOf("") }

    // Forgot password state (identifier -> SMS OTP -> new password)
    var forgotStep by remember { mutableStateOf(ForgotPasswordStep.ENTER_IDENTIFIER) }
    var forgotIdentifier by remember { mutableStateOf("") }
    var forgotOtpChallenge by remember { mutableStateOf<OtpChallenge?>(null) }
    var forgotOtpInput by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmNewPassword by remember { mutableStateOf("") }

    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var infoMessage by remember { mutableStateOf<String?>(null) }

    if (onCancelToApp != null) {
        BackHandler(onBack = onCancelToApp)
    }

    val bgGradient = if (isDarkMode) {
        Brush.verticalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.background,
                MaterialTheme.colorScheme.surface,
                MaterialTheme.colorScheme.background
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.background,
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                MaterialTheme.colorScheme.background
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgGradient)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        if (onCancelToApp != null) {
            IconButton(
                onClick = onCancelToApp,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
                    .testTag("auth_back_to_app_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to InstaPlus",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        IconButton(
            onClick = { onToggleDarkMode(!isDarkMode) },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                .testTag("auth_theme_toggle_button")
        ) {
            Icon(
                imageVector = if (isDarkMode) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                contentDescription = if (isDarkMode) "Switch to Light Mode" else "Switch to Dark Mode",
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .widthIn(max = 460.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                color = Color.Black,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .height(116.dp)
                    .widthIn(max = 220.dp)
                    .border(1.5.dp, InstagramStoryBrush, RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.instaplus_brand_logo),
                    contentDescription = "Insta Plus Logo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }

            Text(
                text = when (screenMode) {
                    AuthScreenMode.LOG_IN -> if (loginOtpChallenge == null) {
                        "Log in with Username, Mobile No., or E-mail"
                    } else {
                        "Verify SMS OTP sent to your registered mobile number"
                    }
                    AuthScreenMode.CREATE_ACCOUNT -> if (signupOtpChallenge == null) {
                        "Create a verified InstaPlus account"
                    } else {
                        "Verify your SMS OTP to activate account"
                    }
                    AuthScreenMode.FORGOT_PASSWORD -> "Recover access to your InstaPlus account"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            if (screenMode != AuthScreenMode.FORGOT_PASSWORD) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        AuthModePill(
                            label = "Log In",
                            selected = screenMode == AuthScreenMode.LOG_IN,
                            onClick = {
                                screenMode = AuthScreenMode.LOG_IN
                                errorMessage = null
                            },
                            testTag = "auth_tab_login",
                            modifier = Modifier.weight(1f)
                        )
                        AuthModePill(
                            label = "Create Account",
                            selected = screenMode == AuthScreenMode.CREATE_ACCOUNT,
                            onClick = {
                                screenMode = AuthScreenMode.CREATE_ACCOUNT
                                errorMessage = null
                                infoMessage = null
                            },
                            testTag = "auth_tab_signup",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(20.dp),
                tonalElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(20.dp)
                    )
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AnimatedVisibility(visible = !infoMessage.isNullOrBlank()) {
                        Surface(
                            color = AdShieldGreen.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = AdShieldGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = infoMessage.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AdShieldGreen,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    val fieldColors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        cursorColor = MaterialTheme.colorScheme.primary
                    )

                    when (screenMode) {
                        AuthScreenMode.LOG_IN -> {
                            val activeLoginChallenge = loginOtpChallenge
                            if (activeLoginChallenge == null) {
                                OutlinedTextField(
                                    value = loginIdentifier,
                                    onValueChange = {
                                        loginIdentifier = it
                                        errorMessage = null
                                    },
                                    label = { Text("Username, mobile number, or E-mail") },
                                    leadingIcon = {
                                        Icon(imageVector = Icons.Filled.Person, contentDescription = null)
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                    colors = fieldColors,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("login_identifier_input")
                                )

                                OutlinedTextField(
                                    value = loginPassword,
                                    onValueChange = {
                                        loginPassword = it
                                        errorMessage = null
                                    },
                                    label = { Text("Password") },
                                    leadingIcon = {
                                        Icon(imageVector = Icons.Filled.Lock, contentDescription = null)
                                    },
                                    trailingIcon = {
                                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                            Icon(
                                                imageVector = if (passwordVisible) Icons.Filled.VisibilityOff
                                                else Icons.Filled.Visibility,
                                                contentDescription = if (passwordVisible) "Hide password" else "Show password"
                                            )
                                        }
                                    },
                                    visualTransformation = if (passwordVisible) VisualTransformation.None
                                    else PasswordVisualTransformation(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Password,
                                        imeAction = ImeAction.Done
                                    ),
                                    colors = fieldColors,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("login_password_input")
                                )

                                Box(modifier = Modifier.fillMaxWidth()) {
                                    TextButton(
                                        onClick = {
                                            screenMode = AuthScreenMode.FORGOT_PASSWORD
                                            forgotStep = ForgotPasswordStep.ENTER_IDENTIFIER
                                            forgotIdentifier = loginIdentifier
                                            forgotOtpInput = ""
                                            newPassword = ""
                                            confirmNewPassword = ""
                                            errorMessage = null
                                            infoMessage = null
                                        },
                                        modifier = Modifier
                                            .align(Alignment.CenterEnd)
                                            .heightIn(min = 48.dp)
                                            .testTag("forgot_password_button")
                                    ) {
                                        Text("Forgot password?")
                                    }
                                }

                                Button(
                                    onClick = {
                                        if (isLoading) return@Button
                                        scope.launch {
                                            isLoading = true
                                            errorMessage = null
                                            infoMessage = null
                                            val res = authRepo.initiateLoginOtp(
                                                identifier = loginIdentifier,
                                                password = loginPassword,
                                                activity = hostActivity
                                            )
                                            isLoading = false
                                            res.onSuccess { challenge ->
                                                if (challenge.autoVerifiedSession != null) {
                                                    loginOtpChallenge = null
                                                    loginOtpInput = ""
                                                    onCancelToApp?.invoke()
                                                } else {
                                                    loginOtpChallenge = challenge
                                                    loginOtpInput = ""
                                                    infoMessage = null
                                                }
                                            }.onFailure { err ->
                                                errorMessage = err.localizedMessage ?: OtpMessages.UNABLE_TO_SEND
                                            }
                                        }
                                    },
                                    enabled = !isLoading,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 50.dp)
                                        .testTag("instaplus_login_button")
                                ) {
                                    if (isLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    } else {
                                        Text("Continue with SMS Verification", fontWeight = FontWeight.Bold)
                                    }
                                }
                            } else {
                                OtpVerificationSection(
                                    challenge = activeLoginChallenge,
                                    otpInput = loginOtpInput,
                                    onOtpInputChange = {
                                        loginOtpInput = it
                                        errorMessage = null
                                    },
                                    isLoading = isLoading,
                                    onVerifyClick = {
                                        if (isLoading) return@OtpVerificationSection
                                        scope.launch {
                                            isLoading = true
                                            errorMessage = null
                                            val verified = authRepo.verifyLoginOtp(
                                                challengeId = activeLoginChallenge.challengeId,
                                                enteredOtp = loginOtpInput
                                            )
                                            isLoading = false
                                            verified.onSuccess {
                                                loginOtpChallenge = null
                                                loginOtpInput = ""
                                                loginPassword = ""
                                                onCancelToApp?.invoke()
                                            }.onFailure { err ->
                                                errorMessage = err.localizedMessage ?: OtpMessages.INVALID_OTP
                                            }
                                        }
                                    },
                                    onResendClick = {
                                        if (isLoading) return@OtpVerificationSection
                                        scope.launch {
                                            isLoading = true
                                            errorMessage = null
                                            val res = authRepo.resendOtp(activeLoginChallenge.challengeId, hostActivity)
                                            isLoading = false
                                            res.onSuccess { refreshed ->
                                                if (refreshed.autoVerifiedSession != null) {
                                                    loginOtpChallenge = null
                                                    loginOtpInput = ""
                                                    onCancelToApp?.invoke()
                                                } else {
                                                    loginOtpChallenge = refreshed
                                                    infoMessage = null
                                                }
                                            }.onFailure { err ->
                                                errorMessage = err.localizedMessage ?: OtpMessages.UNABLE_TO_SEND
                                            }
                                        }
                                    },
                                    onBackClick = {
                                        scope.launch { authRepo.cancelChallenge(activeLoginChallenge.challengeId) }
                                        loginOtpChallenge = null
                                        loginOtpInput = ""
                                        errorMessage = null
                                        infoMessage = null
                                    }
                                )
                            }
                        }

                        AuthScreenMode.CREATE_ACCOUNT -> {
                            val activeChallenge = signupOtpChallenge
                            if (activeChallenge == null) {
                                CreateAccountFieldsSection(
                                    firstName = firstName,
                                    onFirstNameChange = { firstName = it; errorMessage = null },
                                    lastName = lastName,
                                    onLastNameChange = { lastName = it; errorMessage = null },
                                    username = signupUsername,
                                    onUsernameChange = { signupUsername = it; errorMessage = null },
                                    mobileNumber = signupMobile,
                                    onMobileNumberChange = { signupMobile = it; errorMessage = null },
                                    email = signupEmail,
                                    onEmailChange = { signupEmail = it; errorMessage = null },
                                    password = signupPassword,
                                    onPasswordChange = { signupPassword = it; errorMessage = null },
                                    passwordVisible = passwordVisible,
                                    onTogglePasswordVisible = { passwordVisible = !passwordVisible },
                                    isLoading = isLoading,
                                    onSubmitClick = {
                                        if (isLoading) return@CreateAccountFieldsSection
                                        scope.launch {
                                            isLoading = true
                                            errorMessage = null
                                            infoMessage = null
                                            val otpResult = authRepo.initiateRegistrationOtp(
                                                firstName = firstName,
                                                lastName = lastName,
                                                username = signupUsername,
                                                mobileNumber = signupMobile,
                                                email = signupEmail,
                                                password = signupPassword,
                                                activity = hostActivity
                                            )
                                            isLoading = false
                                            otpResult.onSuccess { challenge ->
                                                val created = challenge.autoVerifiedSession
                                                if (created != null) {
                                                    signupOtpChallenge = null
                                                    signupOtpInput = ""
                                                    firstName = ""; lastName = ""; signupUsername = ""
                                                    signupMobile = ""; signupEmail = ""; signupPassword = ""
                                                    loginIdentifier = created.username
                                                    loginPassword = ""
                                                    screenMode = AuthScreenMode.LOG_IN
                                                    infoMessage = "Account @${created.username} created! Log in with your credentials."
                                                } else {
                                                    signupOtpChallenge = challenge
                                                    signupOtpInput = ""
                                                    infoMessage = null
                                                }
                                            }.onFailure { err ->
                                                errorMessage = err.localizedMessage ?: OtpMessages.UNABLE_TO_SEND
                                            }
                                        }
                                    }
                                )
                            } else {
                                OtpVerificationSection(
                                    challenge = activeChallenge,
                                    otpInput = signupOtpInput,
                                    onOtpInputChange = {
                                        signupOtpInput = it
                                        errorMessage = null
                                    },
                                    isLoading = isLoading,
                                    onVerifyClick = {
                                        if (isLoading) return@OtpVerificationSection
                                        scope.launch {
                                            isLoading = true
                                            errorMessage = null
                                            val created = authRepo.verifyRegistrationOtpAndCreateAccount(
                                                challengeId = activeChallenge.challengeId,
                                                enteredOtp = signupOtpInput,
                                                autoSignIn = false
                                            )
                                            isLoading = false
                                            created.onSuccess { session ->
                                                signupOtpChallenge = null
                                                signupOtpInput = ""
                                                firstName = ""; lastName = ""; signupUsername = ""
                                                signupMobile = ""; signupEmail = ""; signupPassword = ""
                                                loginIdentifier = session.username
                                                loginPassword = ""
                                                screenMode = AuthScreenMode.LOG_IN
                                                infoMessage = "Account @${session.username} created! Log in with your username, mobile number, or E-mail and password."
                                            }.onFailure { err ->
                                                errorMessage = err.localizedMessage ?: OtpMessages.INVALID_OTP
                                            }
                                        }
                                    },
                                    onResendClick = {
                                        if (isLoading) return@OtpVerificationSection
                                        scope.launch {
                                            isLoading = true
                                            errorMessage = null
                                            val res = authRepo.resendOtp(activeChallenge.challengeId, hostActivity)
                                            isLoading = false
                                            res.onSuccess { refreshed ->
                                                val created = refreshed.autoVerifiedSession
                                                if (created != null) {
                                                    signupOtpChallenge = null
                                                    signupOtpInput = ""
                                                    firstName = ""; lastName = ""; signupUsername = ""
                                                    signupMobile = ""; signupEmail = ""; signupPassword = ""
                                                    loginIdentifier = created.username
                                                    loginPassword = ""
                                                    screenMode = AuthScreenMode.LOG_IN
                                                    infoMessage = "Account @${created.username} created! Log in with your credentials."
                                                } else {
                                                    signupOtpChallenge = refreshed
                                                    infoMessage = null
                                                }
                                            }.onFailure { err ->
                                                errorMessage = err.localizedMessage ?: OtpMessages.UNABLE_TO_SEND
                                            }
                                        }
                                    },
                                    onBackClick = {
                                        scope.launch { authRepo.cancelChallenge(activeChallenge.challengeId) }
                                        signupOtpChallenge = null
                                        signupOtpInput = ""
                                        errorMessage = null
                                        infoMessage = null
                                    }
                                )
                            }
                        }

                        AuthScreenMode.FORGOT_PASSWORD -> {
                            ForgotPasswordContent(
                                step = forgotStep,
                                identifier = forgotIdentifier,
                                onIdentifierChange = { forgotIdentifier = it; errorMessage = null },
                                challenge = forgotOtpChallenge,
                                otpInput = forgotOtpInput,
                                onOtpInputChange = { forgotOtpInput = it; errorMessage = null },
                                newPassword = newPassword,
                                onNewPasswordChange = { newPassword = it; errorMessage = null },
                                confirmPassword = confirmNewPassword,
                                onConfirmPasswordChange = { confirmNewPassword = it; errorMessage = null },
                                isLoading = isLoading,
                                onSendResetOtp = {
                                    if (isLoading) return@ForgotPasswordContent
                                    scope.launch {
                                        isLoading = true
                                        errorMessage = null
                                        infoMessage = null
                                        val res = authRepo.initiatePasswordResetOtp(forgotIdentifier, hostActivity)
                                        isLoading = false
                                        res.onSuccess { ch ->
                                            forgotOtpChallenge = ch
                                            forgotOtpInput = ""
                                            forgotStep = if (ch.autoVerifiedReset) {
                                                ForgotPasswordStep.SET_NEW_PASSWORD
                                            } else {
                                                infoMessage = null
                                                ForgotPasswordStep.VERIFY_OTP
                                            }
                                        }.onFailure { err ->
                                            errorMessage = err.localizedMessage ?: OtpMessages.UNABLE_TO_SEND
                                        }
                                    }
                                },
                                onVerifyOtp = {
                                    val ch = forgotOtpChallenge ?: return@ForgotPasswordContent
                                    if (isLoading) return@ForgotPasswordContent
                                    scope.launch {
                                        isLoading = true
                                        errorMessage = null
                                        val res = authRepo.verifyResetOtp(ch.challengeId, forgotOtpInput)
                                        isLoading = false
                                        res.onSuccess {
                                            infoMessage = null
                                            forgotStep = ForgotPasswordStep.SET_NEW_PASSWORD
                                        }.onFailure { err ->
                                            errorMessage = err.localizedMessage ?: OtpMessages.INVALID_OTP
                                        }
                                    }
                                },
                                onResendOtp = {
                                    val ch = forgotOtpChallenge ?: return@ForgotPasswordContent
                                    if (isLoading) return@ForgotPasswordContent
                                    scope.launch {
                                        isLoading = true
                                        errorMessage = null
                                        val res = authRepo.resendOtp(ch.challengeId, hostActivity)
                                        isLoading = false
                                        res.onSuccess { refreshed ->
                                            forgotOtpChallenge = refreshed
                                            if (refreshed.autoVerifiedReset) {
                                                infoMessage = null
                                                forgotStep = ForgotPasswordStep.SET_NEW_PASSWORD
                                            } else {
                                                infoMessage = null
                                            }
                                        }.onFailure { err ->
                                            errorMessage = err.localizedMessage ?: OtpMessages.UNABLE_TO_SEND
                                        }
                                    }
                                },
                                onSubmitNewPassword = {
                                    val ch = forgotOtpChallenge ?: return@ForgotPasswordContent
                                    if (isLoading) return@ForgotPasswordContent
                                    scope.launch {
                                        isLoading = true
                                        errorMessage = null
                                        val res = authRepo.completePasswordReset(
                                            challengeId = ch.challengeId,
                                            newPassword = newPassword,
                                            confirmPassword = confirmNewPassword
                                        )
                                        isLoading = false
                                        res.onSuccess { updatedSession ->
                                            forgotOtpChallenge = null
                                            forgotOtpInput = ""
                                            newPassword = ""
                                            confirmNewPassword = ""
                                            loginIdentifier = updatedSession.username
                                            loginPassword = ""
                                            screenMode = AuthScreenMode.LOG_IN
                                            infoMessage = "Password updated for @${updatedSession.username}. Log in with your new password."
                                        }.onFailure { err ->
                                            errorMessage = err.localizedMessage ?: "Could not update password."
                                        }
                                    }
                                },
                                onBackToStepOne = {
                                    forgotOtpChallenge?.let { ch ->
                                        scope.launch { authRepo.cancelChallenge(ch.challengeId) }
                                    }
                                    forgotStep = ForgotPasswordStep.ENTER_IDENTIFIER
                                    errorMessage = null
                                    infoMessage = null
                                },
                                onCancelToLogin = {
                                    forgotOtpChallenge?.let { ch ->
                                        scope.launch { authRepo.cancelChallenge(ch.challengeId) }
                                    }
                                    screenMode = AuthScreenMode.LOG_IN
                                    errorMessage = null
                                    infoMessage = null
                                }
                            )
                        }
                    }

                    AnimatedVisibility(visible = !errorMessage.isNullOrBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = errorMessage.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            if (savedAccounts.isNotEmpty() && onQuickSwitchAccount != null) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (onCancelToApp != null) "Switch Verified InstaPlus Account" else "Saved InstaPlus Accounts",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        savedAccounts.forEach { acc ->
                            AssistChip(
                                onClick = {
                                    if (onCancelToApp != null) {
                                        onQuickSwitchAccount(acc.uid)
                                        onCancelToApp.invoke()
                                    } else {
                                        // Signed out: require password + SMS OTP verification, never bypass.
                                        screenMode = AuthScreenMode.LOG_IN
                                        loginIdentifier = acc.username
                                        loginPassword = ""
                                        loginOtpChallenge = null
                                        errorMessage = null
                                        infoMessage = "Enter password and verify SMS OTP to log into @${acc.username}."
                                    }
                                },
                                label = { Text("@${acc.username}") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("quick_switch_chip_${acc.username}")
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.EnhancedEncryption,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "Hardware AES-256-GCM, PBKDF2-SHA256 & Firebase SMS Auth",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AuthModePill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
