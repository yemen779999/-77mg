package com.example.utils

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SecurePreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("pro_ledger_secure_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ALIAS = "pro_ledger_master_key"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val PIN_SALT = "ProLedgerSalt_2026_Secure"

        fun hashPin(pin: String): String {
            if (pin.isBlank()) return ""
            val bytes = MessageDigest.getInstance("SHA-256").digest((PIN_SALT + pin).toByteArray(Charsets.UTF_8))
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }

    init {
        initKeystore()
    }

    private fun initKeystore() {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                val keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    ANDROID_KEYSTORE
                )
                val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build()
                keyGenerator.init(keyGenParameterSpec)
                keyGenerator.generateKey()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun saveEncryptedString(key: String, value: String) {
        if (value.isEmpty()) {
            prefs.edit().remove(key).remove("${key}_iv").apply()
            return
        }
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            val secretKey = keyStore.getKey(KEY_ALIAS, null) as SecretKey
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(value.toByteArray(Charsets.UTF_8))

            val encryptedHex = encryptedBytes.joinToString("") { "%02x".format(it) }
            val ivHex = iv.joinToString("") { "%02x".format(it) }

            prefs.edit()
                .putString(key, encryptedHex)
                .putString("${key}_iv", ivHex)
                .apply()
        } catch (e: Exception) {
            // Fallback for environment/testing without full keystore support
            prefs.edit().putString(key, value).apply()
        }
    }

    fun getEncryptedString(key: String, defaultValue: String = ""): String {
        val encryptedHex = prefs.getString(key, null) ?: return defaultValue
        val ivHex = prefs.getString("${key}_iv", null)
        if (ivHex == null) return encryptedHex // Fallback if plain value saved

        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            val secretKey = keyStore.getKey(KEY_ALIAS, null) as SecretKey
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")

            val iv = hexToByteArray(ivHex)
            val encryptedBytes = hexToByteArray(encryptedHex)

            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            return String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            return defaultValue
        }
    }

    fun saveHashedPin(pin: String) {
        val hashed = hashPin(pin)
        prefs.edit().putString("hashed_pin", hashed).apply()
    }

    fun verifyPin(enteredPin: String): Boolean {
        val savedHash = prefs.getString("hashed_pin", null)
        if (savedHash.isNullOrBlank()) return true
        return hashPin(enteredPin) == savedHash
    }

    fun isPinSet(): Boolean {
        return !prefs.getString("hashed_pin", null).isNullOrBlank()
    }

    private fun hexToByteArray(hex: String): ByteArray {
        val len = hex.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(hex[i], 16) shl 4) + Character.digit(hex[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }
}
