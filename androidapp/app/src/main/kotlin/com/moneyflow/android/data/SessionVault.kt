package com.moneyflow.android.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@Serializable
internal class Session(val userName: String, val token: String)

internal interface SessionVault {
    suspend fun read(): Session?
    suspend fun save(session: Session)
    suspend fun clear()
}

internal class KeystoreVault(context: Context) : SessionVault {
    private val file = AtomicFile(File(context.noBackupFilesDir, "moneyflow-session"))
    private val alias = "com.moneyflow.android.session.v1"

    override suspend fun read(): Session? = withContext(Dispatchers.IO) {
        val bytes = try {
            file.readFully()
        } catch (_: java.io.FileNotFoundException) {
            return@withContext null
        }
        require(bytes.size > IV_LENGTH)
        val key = keyStore().getKey(alias, null) as? SecretKey
        if (key == null) {
            file.delete()
            return@withContext null
        }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, bytes.copyOfRange(0, IV_LENGTH)))
        val plain = cipher.doFinal(bytes, IV_LENGTH, bytes.size - IV_LENGTH)
        wireJson.decodeFromString<Session>(plain.toString(Charsets.UTF_8))
    }

    override suspend fun save(session: Session) = withContext(Dispatchers.IO) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, existingOrNewKey())
        val bytes = cipher.iv + cipher.doFinal(wireJson.encodeToString(Session.serializer(), session).toByteArray())
        val stream = file.startWrite()
        try {
            stream.write(bytes)
            file.finishWrite(stream)
        } catch (error: Exception) {
            file.failWrite(stream)
            throw error
        }
    }

    override suspend fun clear() = withContext(Dispatchers.IO) {
        // Removing the key first renders any residual ciphertext unreadable.
        keyStore().deleteEntry(alias)
        file.delete()
    }

    private fun keyStore() = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    private fun existingOrNewKey(): SecretKey {
        return keyStore().getKey(alias, null) as? SecretKey ?: KeyGenerator
            .getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            .apply {
                init(
                    KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setKeySize(256)
                        .build(),
                )
            }
            .generateKey()
    }

    private companion object {
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_LENGTH = 12
    }
}
