package com.peakai.fitness.security

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * StrongBox Keymaster integration.
 *
 * Attempts to generate AES-256-GCM key in StrongBox (Titan M2 chip on Pixel 8+).
 * If StrongBox is unavailable, falls back to software-backed Keystore — transparent to callers.
 *
 * All health data at rest is encrypted with this key.
 * Key never leaves the secure element; only cipher operations are allowed.
 */
@Singleton
class StrongBoxKeyManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "StrongBoxKeyManager"
        private const val KEY_ALIAS = "peak_ai_health_key_v1"
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_SIZE = 256
        private const val GCM_IV_SIZE = 12
        private const val GCM_TAG_SIZE = 128
    }

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(KEYSTORE_PROVIDER).also { it.load(null) }
    }

    /**
     * Get or create the encryption key. Returns true if backed by StrongBox.
     */
    fun ensureKeyExists(): Boolean {
        if (keyStore.containsAlias(KEY_ALIAS)) return isStrongBoxBacked()

        return try {
            generateStrongBoxKey()
            Log.i(TAG, "StrongBox-backed key created ✅")
            true
        } catch (e: StrongBoxUnavailableException) {
            Log.w(TAG, "StrongBox unavailable — falling back to TEE-backed key")
            generateSoftwareKey()
            false
        } catch (e: Exception) {
            Log.e(TAG, "Key generation failed: ${e.message}")
            generateSoftwareKey()
            false
        }
    }

    /**
     * Encrypt data. Returns IV + ciphertext (IV prepended, 12 bytes).
     */
    fun encrypt(plaintext: ByteArray): ByteArray {
        ensureKeyExists()
        val key = getKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plaintext)
        return iv + ciphertext
    }

    /**
     * Decrypt data. Expects IV prepended (12 bytes).
     */
    fun decrypt(ivAndCiphertext: ByteArray): ByteArray {
        val iv = ivAndCiphertext.sliceArray(0 until GCM_IV_SIZE)
        val ciphertext = ivAndCiphertext.sliceArray(GCM_IV_SIZE until ivAndCiphertext.size)
        val key = getKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_SIZE, iv))
        return cipher.doFinal(ciphertext)
    }

    fun isStrongBoxBacked(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return false
        return try {
            val key = keyStore.getKey(KEY_ALIAS, null) as? SecretKey ?: return false
            // Inspect key info for StrongBox backing
            val keyInfo = android.security.keystore.KeyInfo::class.java
            val factory = javax.crypto.SecretKeyFactory.getInstance(key.algorithm, KEYSTORE_PROVIDER)
            val info = factory.getKeySpec(key, keyInfo) as android.security.keystore.KeyInfo
            info.securityLevel == android.security.keystore.KeyProperties.SECURITY_LEVEL_STRONGBOX
        } catch (_: Exception) {
            false
        }
    }

    private fun getKey(): SecretKey {
        ensureKeyExists()
        return keyStore.getKey(KEY_ALIAS, null) as SecretKey
    }

    private fun generateStrongBoxKey() {
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(KEY_SIZE)
            .setIsStrongBoxBacked(true)   // ← Titan M2 / StrongBox
            .setUserAuthenticationRequired(false) // So background workers can access
            .build()

        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        gen.init(spec)
        gen.generateKey()
    }

    private fun generateSoftwareKey() {
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(KEY_SIZE)
            .build()

        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        gen.init(spec)
        gen.generateKey()
    }
}
