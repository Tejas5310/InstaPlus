package com.example.data.local

import com.example.security.SecureLocalCipher
import kotlinx.coroutines.flow.Flow

class VaultAndAccountRepository(
    private val mediaDao: DownloadedMediaDao,
    private val accountDao: AccountSessionDao,
    private val lockedChatDao: LockedChatDao
) {
    val downloadedMedia: Flow<List<DownloadedMediaEntity>> = mediaDao.observeAll()
    val accountSessions: Flow<List<AccountSessionEntity>> = accountDao.observeAll()
    val lockedChats: Flow<List<LockedChatEntity>> = lockedChatDao.observeAll()

    suspend fun saveDownloadedMedia(entity: DownloadedMediaEntity): Long = mediaDao.insert(entity)

    suspend fun deleteDownloadedMedia(id: Long) = mediaDao.deleteById(id)

    suspend fun clearVault() = mediaDao.clearAll()

    suspend fun getActiveAccount(): AccountSessionEntity? = accountDao.getActiveSession()

    suspend fun getAccountById(id: Long): AccountSessionEntity? = accountDao.getById(id)

    fun decryptAccountCookies(account: AccountSessionEntity): String {
        return SecureLocalCipher.decrypt(account.cookiesJson)
    }

    suspend fun upsertAndActivateAccount(
        username: String,
        displayName: String,
        cookiesJson: String
    ): Long {
        accountDao.clearActiveFlags()
        val encryptedCookies = SecureLocalCipher.encrypt(cookiesJson)
        return accountDao.insert(
            AccountSessionEntity(
                username = username,
                displayName = displayName,
                cookiesJson = encryptedCookies,
                isActive = true,
                lastActiveAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun switchActiveAccount(targetId: Long, currentCookiesJson: String) {
        val current = accountDao.getActiveSession()
        if (current != null && current.id != targetId) {
            val encryptedCookies = SecureLocalCipher.encrypt(currentCookiesJson)
            accountDao.updateCookies(current.id, encryptedCookies)
        }
        accountDao.clearActiveFlags()
        accountDao.markActive(targetId)
    }

    suspend fun updateActiveCookies(cookiesJson: String) {
        val current = accountDao.getActiveSession() ?: return
        val encryptedCookies = SecureLocalCipher.encrypt(cookiesJson)
        accountDao.updateCookies(current.id, encryptedCookies)
    }

    suspend fun deleteAccount(id: Long) = accountDao.deleteById(id)

    suspend fun addLockedChat(threadKey: String, displayName: String) {
        val cleanKey = threadKey.trim().lowercase()
        if (cleanKey.isBlank()) return
        lockedChatDao.insert(
            LockedChatEntity(
                threadKey = cleanKey,
                displayName = displayName.trim().ifBlank { cleanKey }
            )
        )
    }

    suspend fun removeLockedChat(id: Long) = lockedChatDao.deleteById(id)
}
