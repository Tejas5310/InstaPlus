package com.example.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.local.OtpChallenge
import com.example.data.local.OtpPurpose
import kotlinx.coroutines.delay

enum class ForgotPasswordStep {
    ENTER_IDENTIFIER,
    VERIFY_OTP,
    SET_NEW_PASSWORD
}

@Composable
fun CreateAccountFieldsSection(
    firstName: String,
    onFirstNameChange: (String) -> Unit,
    lastName: String,
    onLastNameChange: (String) -> Unit,
    username: String,
    onUsernameChange: (String) -> Unit,
    mobileNumber: String,
    onMobileNumberChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    passwordVisible: Boolean,
    onTogglePasswordVisible: () -> Unit,
    isLoading: Boolean,
    onSubmitClick: () -> Unit
) {
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        cursorColor = MaterialTheme.colorScheme.primary
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = firstName,
                onValueChange = onFirstNameChange,
                label = { Text("First name") },
                leadingIcon = {
                    Icon(imageVector = Icons.Filled.Badge, contentDescription = null)
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                colors = fieldColors,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("signup_first_name_input")
            )
            OutlinedTextField(
                value = lastName,
                onValueChange = onLastNameChange,
                label = { Text("Last name") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                colors = fieldColors,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("signup_last_name_input")
            )
        }

        OutlinedTextField(
            value = username,
            onValueChange = onUsernameChange,
            label = { Text("Username") },
            placeholder = { Text("e.g. alex_media") },
            leadingIcon = {
                Icon(imageVector = Icons.Filled.AlternateEmail, contentDescription = null)
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            colors = fieldColors,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("signup_username_input")
        )

        OutlinedTextField(
            value = mobileNumber,
            onValueChange = onMobileNumberChange,
            label = { Text("Mobile number (with country code)") },
            placeholder = { Text("+1 555 234 5678") },
            supportingText = {
                Text("SMS verification code will be sent privately to this number.")
            },
            leadingIcon = {
                Icon(imageVector = Icons.Filled.Phone, contentDescription = null)
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Next
            ),
            colors = fieldColors,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("signup_mobile_input")
        )

        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text("E-mail") },
            placeholder = { Text("you@example.com") },
            leadingIcon = {
                Icon(imageVector = Icons.Filled.Email, contentDescription = null)
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            colors = fieldColors,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("signup_email_input")
        )

        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Password") },
            placeholder = { Text("At least 6 characters") },
            leadingIcon = {
                Icon(imageVector = Icons.Filled.Lock, contentDescription = null)
            },
            trailingIcon = {
                IconButton(onClick = onTogglePasswordVisible) {
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
                .testTag("signup_password_input")
        )

        Button(
            onClick = onSubmitClick,
            enabled = !isLoading,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 50.dp)
                .testTag("instaplus_signup_button")
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Continue & Send SMS OTP", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun OtpVerificationSection(
    challenge: OtpChallenge,
    otpInput: String,
    onOtpInputChange: (String) -> Unit,
    isLoading: Boolean,
    onVerifyClick: () -> Unit,
    onResendClick: () -> Unit,
    onBackClick: () -> Unit
) {
    var resendSecondsRemaining by remember(challenge.challengeId, challenge.resendAvailableAtMillis) {
        mutableIntStateOf(
            ((challenge.resendAvailableAtMillis - System.currentTimeMillis() + 999L) / 1000L)
                .toInt()
                .coerceAtLeast(0)
        )
    }

    LaunchedEffect(challenge.challengeId, challenge.resendAvailableAtMillis) {
        while (true) {
            val remaining = ((challenge.resendAvailableAtMillis - System.currentTimeMillis() + 999L) / 1000L)
                .toInt()
                .coerceAtLeast(0)
            resendSecondsRemaining = remaining
            if (remaining <= 0) break
            delay(1000L)
        }
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        cursorColor = MaterialTheme.colorScheme.primary
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Secure SMS status card — never displays or exposes any OTP code.
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(14.dp)
                )
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
                        imageVector = Icons.Filled.Sms,
                        contentDescription = "SMS verification",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = when (challenge.purpose) {
                            OtpPurpose.CREATE_ACCOUNT -> "Verify Account Mobile Number"
                            OtpPurpose.LOGIN -> "Two-Step SMS Verification"
                            OtpPurpose.RESET_PASSWORD -> "Verify Recovery Mobile Number"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Text(
                    text = challenge.statusMessage,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Check your SMS inbox on ${challenge.maskedMobile}. This code expires in 5 minutes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                )
            }
        }

        OutlinedTextField(
            value = otpInput,
            onValueChange = { raw ->
                val digitsOnly = raw.filter { it.isDigit() }.take(6)
                onOtpInputChange(digitsOnly)
            },
            label = { Text("Enter 6-digit SMS OTP") },
            placeholder = { Text("Enter SMS code") },
            leadingIcon = {
                Icon(imageVector = Icons.Filled.Pin, contentDescription = null)
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            colors = fieldColors,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("otp_code_input")
        )

        Button(
            onClick = onVerifyClick,
            enabled = !isLoading && otpInput.length == 6,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 50.dp)
                .testTag("verify_otp_button")
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when (challenge.purpose) {
                        OtpPurpose.CREATE_ACCOUNT -> "Verify OTP & Create Account"
                        OtpPurpose.LOGIN -> "Verify OTP & Log In"
                        OtpPurpose.RESET_PASSWORD -> "Verify OTP"
                    },
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onBackClick,
                enabled = !isLoading,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("otp_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (challenge.purpose == OtpPurpose.LOGIN) "Back" else "Edit Details")
            }

            OutlinedButton(
                onClick = onResendClick,
                enabled = !isLoading && resendSecondsRemaining == 0,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("resend_otp_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    if (resendSecondsRemaining > 0) "Resend (${resendSecondsRemaining}s)"
                    else "Resend OTP"
                )
            }
        }
    }
}

@Composable
fun ForgotPasswordContent(
    step: ForgotPasswordStep,
    identifier: String,
    onIdentifierChange: (String) -> Unit,
    challenge: OtpChallenge?,
    otpInput: String,
    onOtpInputChange: (String) -> Unit,
    newPassword: String,
    onNewPasswordChange: (String) -> Unit,
    confirmPassword: String,
    onConfirmPasswordChange: (String) -> Unit,
    isLoading: Boolean,
    onSendResetOtp: () -> Unit,
    onVerifyOtp: () -> Unit,
    onResendOtp: () -> Unit,
    onSubmitNewPassword: () -> Unit,
    onBackToStepOne: () -> Unit,
    onCancelToLogin: () -> Unit
) {
    var passwordVisible by remember { mutableStateOf(false) }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        cursorColor = MaterialTheme.colorScheme.primary
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when (step) {
            ForgotPasswordStep.ENTER_IDENTIFIER -> {
                Text(
                    text = "Reset your InstaPlus password",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Enter your username, mobile number, or E-mail ID. We will send an SMS OTP to your registered mobile number.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = identifier,
                    onValueChange = onIdentifierChange,
                    label = { Text("Username, mobile number, or E-mail") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Filled.Person, contentDescription = null)
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    colors = fieldColors,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("forgot_identifier_input")
                )

                Button(
                    onClick = onSendResetOtp,
                    enabled = !isLoading,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp)
                        .testTag("forgot_send_otp_button")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Send SMS OTP", fontWeight = FontWeight.Bold)
                    }
                }

                TextButton(
                    onClick = onCancelToLogin,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .heightIn(min = 48.dp)
                        .testTag("forgot_back_to_login_button")
                ) {
                    Text("Back to Log In")
                }
            }

            ForgotPasswordStep.VERIFY_OTP -> {
                if (challenge != null) {
                    OtpVerificationSection(
                        challenge = challenge,
                        otpInput = otpInput,
                        onOtpInputChange = onOtpInputChange,
                        isLoading = isLoading,
                        onVerifyClick = onVerifyOtp,
                        onResendClick = onResendOtp,
                        onBackClick = onBackToStepOne
                    )
                }
            }

            ForgotPasswordStep.SET_NEW_PASSWORD -> {
                Text(
                    text = "SMS Verified — Set New Password",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Choose a strong new password (minimum 6 characters) for @${challenge?.targetUsername.orEmpty()}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = onNewPasswordChange,
                    label = { Text("New password") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Filled.Lock, contentDescription = null)
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                    colors = fieldColors,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_password_input")
                )

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = onConfirmPasswordChange,
                    label = { Text("Confirm new password") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Filled.Lock, contentDescription = null)
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    colors = fieldColors,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("confirm_new_password_input")
                )

                Button(
                    onClick = onSubmitNewPassword,
                    enabled = !isLoading,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp)
                        .testTag("update_password_button")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Set New Password", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
