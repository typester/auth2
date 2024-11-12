package dev.typester.auth2.ui.screens

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import dev.typester.auth2.SharedContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec

private const val TAG = "EncryptKeyViewModel"

data class EncryptKeyUiState(
    val encryptKey: String = "",
    val restored: Boolean? = null,
    val saved: Boolean = false,
)

class EncryptKeyViewModel: ViewModel() {
    private val _uiState = MutableStateFlow(EncryptKeyUiState())
    val uiState = _uiState.asStateFlow()

    init {
        try {
            initializeKeyStore()
            getUserKey()?.let { key ->
                updateKey(key)
                if (key.isNotEmpty()) {
                    _uiState.update { it.copy(restored = true) }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "failed to fetch key", e)
        } finally {
            if (uiState.value.restored == null) {
                _uiState.update { it.copy(restored = false) }
            }
        }
    }

    fun updateKey(key: String) {
        _uiState.update { it.copy(encryptKey = key) }
    }

    fun saveKey() {
        try {
            storeUserKey(uiState.value.encryptKey)
            _uiState.update { it.copy(saved = true) }
        } catch (e: Exception) {
            Log.e(TAG, "failed to save key", e)
        }
    }
}

private const val ALIAS = "encryptKey"
private const val SHARED_PREFS_NAME = "encryptKey"
private const val PREF_KEY_IV = "iv"
private const val PREF_KEY_ENCRYPTED_KEY = "encrypted_key"

private fun initializeKeyStore() {
    val keyStore = KeyStore.getInstance("AndroidKeyStore")
    keyStore.load(null)

    if (!keyStore.containsAlias(ALIAS)) {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
        )
        keyGenerator.generateKey()
    }
}

private fun storeUserKey(key: String) {
    val keyStore = KeyStore.getInstance("AndroidKeyStore")
    keyStore.load(null)

    val secretKeyEntry = keyStore.getEntry(ALIAS, null) as KeyStore.SecretKeyEntry
    val secretKey = secretKeyEntry.secretKey

    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.ENCRYPT_MODE, secretKey)
    val iv = cipher.iv
    val encryptedKey = cipher.doFinal(key.toByteArray(Charsets.UTF_8))

    val base64iv = Base64.encodeToString(iv, Base64.DEFAULT)
    val base64key = Base64.encodeToString(encryptedKey, Base64.DEFAULT)

    val sharedPreferences = SharedContext.context().getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
    with (sharedPreferences.edit()) {
        putString(PREF_KEY_IV, base64iv)
        putString(PREF_KEY_ENCRYPTED_KEY, base64key)
        apply()
    }
}

private fun getUserKey(): String? {
    val keyStore = KeyStore.getInstance("AndroidKeyStore")
    keyStore.load(null)

    val secretKeyEntry = keyStore.getEntry(ALIAS, null) as KeyStore.SecretKeyEntry
    val secretKey = secretKeyEntry.secretKey

    val sharedPreferences = SharedContext.context().getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
    val base64iv = sharedPreferences.getString(PREF_KEY_IV, null)
    val base64key = sharedPreferences.getString(PREF_KEY_ENCRYPTED_KEY, null)

    if (base64iv == null || base64key == null) {
        return null
    }

    val iv = Base64.decode(base64iv, Base64.DEFAULT)
    val encryptedKey = Base64.decode(base64key, Base64.DEFAULT)

    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
    val decryptedKey = cipher.doFinal(encryptedKey)

    return String(decryptedKey, Charsets.UTF_8)
}

class AndroidKeyStore : uniffi.auth2.KeyStore {
    override fun get(): String? {
        initializeKeyStore()
        return getUserKey()
    }
}