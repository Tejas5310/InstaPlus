package com.example.data.remote

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.InstaPlusSettings
import com.example.data.model.ThemePreset
import com.example.security.SecureLocalCipher
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class FirestoreCloudRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun ensureUserProfile_and_syncSettings_succeedsForOwner() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repo = FirestoreCloudRepository(firestore)

        val initialSettings = InstaPlusSettings(themePreset = ThemePreset.AMOLED_DARK)
        val profileResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.ensureUserProfile(aliceUid, "Alice", ALICE_EMAIL, initialSettings)
        }
        assertTrue("Profile create should succeed: ${profileResult.exceptionOrNull()}", profileResult.isSuccess)
        assertEquals(aliceUid, profileResult.getOrThrow().userId)

        val updateResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.syncSettingsToCloud(
                userId = aliceUid,
                displayName = "Alice Updated",
                email = ALICE_EMAIL,
                settings = initialSettings.copy(hideReelsTab = true, themePreset = ThemePreset.MIDNIGHT_VIOLET)
            )
        }
        assertTrue("Settings merge update should succeed", updateResult.isSuccess)

        val observed = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeUserProfile(aliceUid).first { it?.themePreset == ThemePreset.MIDNIGHT_VIOLET.name }
        }
        assertNotNull(observed)
        assertEquals(true, observed?.hideReelsTab)
    }

    @Test
    fun saveVaultItem_and_observeVaultItems_emitsSavedRecord() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repo = FirestoreCloudRepository(firestore)
        val itemId = "vault_${UUID.randomUUID().toString().take(8)}"

        val saveResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.saveVaultItem(
                userId = aliceUid,
                item = CloudVaultItem(
                    id = itemId,
                    userId = aliceUid,
                    mediaType = "REEL",
                    authorHandle = "creator_handle",
                    captionSnippet = "Saved reel snippet",
                    sourceUrl = "https://cdn.example.com/reel.mp4",
                    fileName = "reel_1.mp4",
                    mimeType = "video/mp4",
                    fileSizeBytes = 102400L
                )
            )
        }
        assertTrue("Vault item write should succeed: ${saveResult.exceptionOrNull()}", saveResult.isSuccess)

        val items = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeVaultItems(aliceUid).first { list -> list.any { it.id == itemId } }
        }
        assertTrue(items.any { it.id == itemId && it.authorHandle == "creator_handle" })

        val deleteResult = withTimeout(DEFAULT_TIMEOUT_MS) { repo.deleteVaultItem(aliceUid, itemId) }
        assertTrue(deleteResult.isSuccess)
    }

    @Test
    fun crossUserAccess_lockedChatsAndProfiles_rejectedWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = FirestoreCloudRepository(firestore)
        val lockResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.upsertLockedChat(aliceUid, "thread_secret_1", "@private_chat")
        }
        assertTrue(lockResult.isSuccess)

        // Sign in as Bob and attempt to read Alice's locked chats
        signInTestUser(BOB_EMAIL)
        val bobRepo = FirestoreCloudRepository(firestore)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                bobRepo.observeLockedChats(aliceUid).first()
            }
            fail("Expected PERMISSION_DENIED when Bob reads Alice's lockedChats")
        } catch (e: Exception) {
            val fsException = e.unwrapFirestoreException()
            assertNotNull("Expected nested FirebaseFirestoreException, got $e", fsException)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsException?.code)
        }
    }

    @Test
    fun unauthenticatedCaller_rejectedWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = FirestoreCloudRepository(firestore)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeVaultItems("some_uid_123").first()
            }
            fail("Expected PERMISSION_DENIED for unauthenticated Flow listener")
        } catch (e: Exception) {
            val fsException = e.unwrapFirestoreException()
            assertNotNull("Expected nested FirebaseFirestoreException, got $e", fsException)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsException?.code)
        }
    }

    @Test
    fun pbkdf2PinHashing_verifiesCorrectPin_andRejectsWrongPin() {
        val salt = SecureLocalCipher.generateSaltBase64()
        val hash = SecureLocalCipher.hashPin("4829", salt)
        assertTrue(hash.startsWith("pbkdf2v2:"))
        assertTrue(SecureLocalCipher.verifyPin("4829", salt, hash))
        assertFalse(SecureLocalCipher.verifyPin("0000", salt, hash))
    }

    private companion object {
        const val ALICE_EMAIL = "alice_instaplus@test.com"
        const val BOB_EMAIL = "bob_instaplus@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
