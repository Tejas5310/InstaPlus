package com.example.security

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

private const val TAG = "GoogleAuthManager"

sealed interface GoogleAuthResult {
    data class Success(val user: FirebaseUser) : GoogleAuthResult
    data object Cancelled : GoogleAuthResult
    data object NoSavedCredential : GoogleAuthResult
    data class Error(val message: String) : GoogleAuthResult
}

fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        trySend(firebaseAuth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

class GoogleAuthManager(private val activityContext: Context) {

    private val credentialManager: CredentialManager = CredentialManager.create(activityContext)
    private val auth: FirebaseAuth = Firebase.auth

    suspend fun attemptSilentSignIn(): GoogleAuthResult {
        val serverClientId = activityContext.getString(R.string.default_web_client_id)
        if (serverClientId.isBlank()) {
            return GoogleAuthResult.Error("Missing web client configuration")
        }

        return try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(true)
                .setServerClientId(serverClientId)
                .setAutoSelectEnabled(true)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val response = credentialManager.getCredential(
                request = request,
                context = activityContext
            )
            exchangeGoogleCredential(response.credential)
        } catch (_: NoCredentialException) {
            GoogleAuthResult.NoSavedCredential
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "Silent Google sign-in cancelled by user", e)
            GoogleAuthResult.Cancelled
        } catch (e: GetCredentialException) {
            Log.w(TAG, "Silent Google sign-in skipped: ${e.type}", e)
            GoogleAuthResult.NoSavedCredential
        } catch (e: Exception) {
            Log.w(TAG, "Silent Google sign-in error", e)
            GoogleAuthResult.NoSavedCredential
        }
    }

    suspend fun signInInteractive(): GoogleAuthResult {
        val serverClientId = activityContext.getString(R.string.default_web_client_id)
        if (serverClientId.isBlank()) {
            return GoogleAuthResult.Error("Google Sign-In client configuration is unavailable.")
        }

        return try {
            val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(serverClientId)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(signInWithGoogleOption)
                .build()

            val response = credentialManager.getCredential(
                request = request,
                context = activityContext
            )
            exchangeGoogleCredential(response.credential)
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "Interactive Google Sign-In cancelled (check SHA-1 or account selection)", e)
            GoogleAuthResult.Cancelled
        } catch (e: NoCredentialException) {
            Log.w(TAG, "No Google accounts available on device", e)
            GoogleAuthResult.Error("No Google account found on this device. Add a Google account in Settings to continue.")
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Credential Manager error during Google Sign-In: ${e.type}", e)
            GoogleAuthResult.Error(e.errorMessage?.toString() ?: "Unable to complete Google Sign-In.")
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during Google Sign-In", e)
            GoogleAuthResult.Error(e.localizedMessage ?: "Sign-in failed. Please try again.")
        }
    }

    private suspend fun exchangeGoogleCredential(credential: androidx.credentials.Credential): GoogleAuthResult {
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleIdCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val firebaseCredential = GoogleAuthProvider.getCredential(googleIdCredential.idToken, null)
            val authResult = auth.signInWithCredential(firebaseCredential).await()
            val user = authResult.user
                ?: return GoogleAuthResult.Error("Firebase sign-in returned an empty user session.")
            return GoogleAuthResult.Success(user)
        }
        return GoogleAuthResult.Error("Unsupported credential type returned.")
    }

    suspend fun signOut() {
        runCatching {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        }.onFailure { e ->
            Log.w(TAG, "Failed to clear credential state on sign-out", e)
        }
        auth.signOut()
    }
}
