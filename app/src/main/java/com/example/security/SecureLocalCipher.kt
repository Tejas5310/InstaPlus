package com.example.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec

/**
 * Hardware-backed Android Keystore AES-256-GCM cipher for protecting sensitive
 * device-local credentials and PBKDF2-HMAC-SHA256
 * key stretching for Chat Lock PINs.
 */
object SecureLocalCipher {

    private const val TAG = "SecureLocalCipher"
    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val KEY_ALIAS = "instaplus_aes256_gcm_master_key"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val PREFIX_V1 = "enc:v1:"
    private const val GCM_IV_BYTES = 12
    private const val GCM_TAG_BITS = 128

    private const val PIN_PREFIX_V2 = "pbkdf2v2:"
    private const val PBKDF2_ITERATIONS = 100_000
    private const val PBKDF2_KEY_BITS = 256

    @Volatile
    private var cachedKey: SecretKey? = null

    fun encrypt(plaintext: String): String {
        if (plaintext.isBlank() || plaintext.startsWith(PREFIX_V1)) return plaintext
        return runCatching {
            val key = getOrCreateSecretKey() ?: return plaintext
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val iv = cipher.iv
            val encrypted = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
            val combined = ByteArray(iv.size + encrypted.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encrypted, 0, combined, iv.size, encrypted.size)
            PREFIX_V1 + Base64.encodeToString(combined, Base64.NO_WRAP)
        }.getOrElse { error ->
            Log.w(TAG, "Keystore encrypt unavailable; falling back", error)
            plaintext
        }
    }

    fun decrypt(stored: String): String {
        if (!stored.startsWith(PREFIX_V1)) return stored
        return runCatching {
            val key = getOrCreateSecretKey() ?: return ""
            val payload = Base64.decode(stored.removePrefix(PREFIX_V1), Base64.NO_WRAP)
            if (payload.size <= GCM_IV_BYTES) return ""
            val iv = payload.copyOfRange(0, GCM_IV_BYTES)
            val cipherBytes = payload.copyOfRange(GCM_IV_BYTES, payload.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
            String(cipher.doFinal(cipherBytes), Charsets.UTF_8)
        }.getOrElse { error ->
            Log.w(TAG, "Keystore decrypt failed", error)
            ""
        }
    }

    fun isEncryptedAtRest(stored: String): Boolean = stored.startsWith(PREFIX_V1)

    fun generateSaltBase64(): String {
        val saltBytes = ByteArray(16).also { SecureRandom().nextBytes(it) }
        return Base64.encodeToString(saltBytes, Base64.NO_WRAP)
    }

    fun hashPin(pin: String, saltBase64: String): String = hashSecret(pin, saltBase64)

    fun verifyPin(pin: String, saltBase64: String, storedHash: String): Boolean =
        verifySecret(pin, saltBase64, storedHash)

    fun hashSecret(secret: String, saltBase64: String): String {
        return runCatching {
            val saltBytes = Base64.decode(saltBase64, Base64.NO_WRAP)
            val spec = PBEKeySpec(secret.toCharArray(), saltBytes, PBKDF2_ITERATIONS, PBKDF2_KEY_BITS)
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val derived = factory.generateSecret(spec).encoded
            spec.clearPassword()
            PIN_PREFIX_V2 + Base64.encodeToString(derived, Base64.NO_WRAP)
        }.getOrElse {
            hashPinLegacySha256(secret, saltBase64)
        }
    }

    fun verifySecret(secret: String, saltBase64: String, storedHash: String): Boolean {
        return if (storedHash.startsWith(PIN_PREFIX_V2)) {
            constantTimeEquals(hashSecret(secret, saltBase64), storedHash)
        } else {
            constantTimeEquals(hashPinLegacySha256(secret, saltBase64), storedHash)
        }
    }

    private fun hashPinLegacySha256(pin: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest("$salt:$pin".toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private fun constantTimeEquals(a: String, b: String): Boolean {
        val aBytes = a.toByteArray(Charsets.UTF_8)
        val bBytes = b.toByteArray(Charsets.UTF_8)
        return MessageDigest.isEqual(aBytes, bBytes)
    }

    @Synchronized
    private fun getOrCreateSecretKey(): SecretKey? {
        cachedKey?.let { return it }
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        val existing = (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey
        if (existing != null) {
            cachedKey = existing
            return existing
        }
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey().also { cachedKey = it }
    }
}
