package com.example.data.remote

import android.content.Context
import com.example.R
import com.example.data.model.InstaPlusSettings
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirestoreCloudRepository(
    private val db: FirebaseFirestore
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    fun observeUserProfile(userId: String): Flow<UserCloudProfile?> {
        return db.collection("users").document(userId)
            .snapshots()
            .map { snapshot ->
                if (!snapshot.exists()) null
                else snapshot.toObject(
                    UserCloudProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
    }

    fun observeVaultItems(userId: String): Flow<List<CloudVaultItem>> {
        return db.collection("users").document(userId)
            .collection("vaultItems")
            .snapshots()
            .map { querySnapshot ->
                querySnapshot.toObjects(
                    CloudVaultItem::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
    }

    fun observeLockedChats(userId: String): Flow<List<CloudLockedChat>> {
        return db.collection("users").document(userId)
            .collection("lockedChats")
            .snapshots()
            .map { querySnapshot ->
                querySnapshot.toObjects(
                    CloudLockedChat::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
    }

    fun observeAccountProfiles(userId: String): Flow<List<CloudAccountProfile>> {
        return db.collection("users").document(userId)
            .collection("accountProfiles")
            .snapshots()
            .map { querySnapshot ->
                querySnapshot.toObjects(
                    CloudAccountProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
    }

    suspend fun ensureUserProfile(
        userId: String,
        displayName: String,
        email: String?,
        settings: InstaPlusSettings
    ): Result<UserCloudProfile> {
        val docRef = db.collection("users").document(userId)
        return try {
            val existing = docRef.get().await()
            if (existing.exists()) {
                val parsed = existing.toObject(
                    UserCloudProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                if (parsed != null) return Result.success(parsed)
            }
            val model = UserCloudProfile.fromSettings(
                userId = userId,
                displayName = displayName,
                email = email,
                settings = settings
            )
            docRef.set(model.toCreateMap()).await()
            val saved = docRef.get().await().toObject(
                UserCloudProfile::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: model
            Result.success(saved)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun syncSettingsToCloud(
        userId: String,
        displayName: String,
        email: String?,
        settings: InstaPlusSettings
    ): Result<Unit> {
        val docRef = db.collection("users").document(userId)
        return try {
            val existing = docRef.get().await()
            val model = UserCloudProfile.fromSettings(
                userId = userId,
                displayName = displayName,
                email = email,
                settings = settings
            )
            if (existing.exists()) {
                docRef.set(model.toUpdateMap(), SetOptions.merge()).await()
            } else {
                docRef.set(model.toCreateMap()).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun saveVaultItem(userId: String, item: CloudVaultItem): Result<String> {
        val safeId = sanitizeCloudId(item.id.ifBlank { "${item.fileName}_${System.currentTimeMillis()}" })
        val docRef = db.collection("users").document(userId)
            .collection("vaultItems").document(safeId)
        return try {
            docRef.set(item.copy(id = safeId, userId = userId).toCreateMap()).await()
            Result.success(safeId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteVaultItem(userId: String, itemId: String): Result<Unit> {
        val safeId = sanitizeCloudId(itemId)
        val docRef = db.collection("users").document(userId)
            .collection("vaultItems").document(safeId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun upsertLockedChat(
        userId: String,
        threadKey: String,
        displayName: String
    ): Result<String> {
        val cleanKey = threadKey.trim().lowercase()
        val safeId = sanitizeCloudId(cleanKey)
        val docRef = db.collection("users").document(userId)
            .collection("lockedChats").document(safeId)
        return try {
            val existing = docRef.get().await()
            val model = CloudLockedChat(
                id = safeId,
                userId = userId,
                threadKey = cleanKey.take(128),
                displayName = displayName.trim().ifBlank { cleanKey }.take(80)
            )
            if (existing.exists()) {
                docRef.set(model.toUpdateMap(), SetOptions.merge()).await()
            } else {
                docRef.set(model.toCreateMap()).await()
            }
            Result.success(safeId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteLockedChat(userId: String, threadKeyOrId: String): Result<Unit> {
        val safeId = sanitizeCloudId(threadKeyOrId)
        val docRef = db.collection("users").document(userId)
            .collection("lockedChats").document(safeId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun upsertAccountProfile(
        userId: String,
        username: String,
        displayName: String,
        isActive: Boolean
    ): Result<String> {
        val cleanUsername = username.trim().removePrefix("@").lowercase()
        val safeId = sanitizeCloudId(cleanUsername)
        val docRef = db.collection("users").document(userId)
            .collection("accountProfiles").document(safeId)
        return try {
            val existing = docRef.get().await()
            val model = CloudAccountProfile(
                id = safeId,
                userId = userId,
                username = cleanUsername.take(64),
                displayName = displayName.trim().ifBlank { cleanUsername }.take(80),
                isActive = isActive
            )
            if (existing.exists()) {
                docRef.set(model.toUpdateMap(), SetOptions.merge()).await()
            } else {
                docRef.set(model.toCreateMap()).await()
            }
            Result.success(safeId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteAccountProfile(userId: String, usernameOrId: String): Result<Unit> {
        val safeId = sanitizeCloudId(usernameOrId)
        val docRef = db.collection("users").document(userId)
            .collection("accountProfiles").document(safeId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }
}
