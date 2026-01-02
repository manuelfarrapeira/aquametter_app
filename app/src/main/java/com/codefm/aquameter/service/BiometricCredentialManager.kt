package com.codefm.aquameter.service

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gestor de credenciales encriptadas para autenticación biométrica
 */
@Singleton
class BiometricCredentialManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val sharedPrefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "biometric_credentials"
        private const val KEY_ENCRYPTED_CREDENTIALS = "encrypted_credentials"
        private const val KEY_IV = "encryption_iv"
        private const val KEY_HAS_BIOMETRIC = "has_biometric"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "aquameter_biometric_key"
    }

    /**
     * Verifica si hay credenciales guardadas con biometría
     */
    fun hasBiometricCredentials(): Boolean {
        return sharedPrefs.getBoolean(KEY_HAS_BIOMETRIC, false)
    }

    /**
     * Guarda las credenciales encriptadas
     */
    fun saveCredentials(username: String, password: String): Boolean {
        return try {
            val credentials = "$username:$password"
            val cipher = getEncryptCipher()
            val encrypted = cipher.doFinal(credentials.toByteArray(Charsets.UTF_8))
            val iv = cipher.iv

            sharedPrefs.edit()
                .putString(KEY_ENCRYPTED_CREDENTIALS, Base64.encodeToString(encrypted, Base64.DEFAULT))
                .putString(KEY_IV, Base64.encodeToString(iv, Base64.DEFAULT))
                .putBoolean(KEY_HAS_BIOMETRIC, true)
                .apply()

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Recupera las credenciales desencriptadas
     */
    fun getCredentials(): Pair<String, String>? {
        return try {
            val encryptedString = sharedPrefs.getString(KEY_ENCRYPTED_CREDENTIALS, null) ?: return null
            val ivString = sharedPrefs.getString(KEY_IV, null) ?: return null

            val encrypted = Base64.decode(encryptedString, Base64.DEFAULT)
            val iv = Base64.decode(ivString, Base64.DEFAULT)

            val cipher = getDecryptCipher(iv)
            val decrypted = cipher.doFinal(encrypted)
            val credentials = String(decrypted, Charsets.UTF_8)

            val parts = credentials.split(":")
            if (parts.size == 2) {
                Pair(parts[0], parts[1])
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Obtiene o crea la clave secreta en el KeyStore
     */
    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)

        keyStore.getKey(KEY_ALIAS, null)?.let { return it as SecretKey }

        // Crear nueva clave
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )

        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
            .setUserAuthenticationRequired(false) // No requerimos auth para cada uso
            .build()

        keyGenerator.init(keyGenParameterSpec)
        return keyGenerator.generateKey()
    }

    /**
     * Obtiene cipher para encriptar
     */
    private fun getEncryptCipher(): Cipher {
        val cipher = Cipher.getInstance(
            "${KeyProperties.KEY_ALGORITHM_AES}/" +
            "${KeyProperties.BLOCK_MODE_CBC}/" +
            KeyProperties.ENCRYPTION_PADDING_PKCS7
        )
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        return cipher
    }

    /**
     * Obtiene cipher para desencriptar
     */
    private fun getDecryptCipher(iv: ByteArray): Cipher {
        val cipher = Cipher.getInstance(
            "${KeyProperties.KEY_ALGORITHM_AES}/" +
            "${KeyProperties.BLOCK_MODE_CBC}/" +
            KeyProperties.ENCRYPTION_PADDING_PKCS7
        )
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), IvParameterSpec(iv))
        return cipher
    }
}

