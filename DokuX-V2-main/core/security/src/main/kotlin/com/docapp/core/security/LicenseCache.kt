package com.docapp.core.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.docapp.core.gate.StateVault

/**
 * Menyimpan hasil verifikasi server secara terenkripsi (Jetpack Security),
 * lalu menyalakan StateVault + NativeBridge. Refresh berkala saat online,
 * dipakai offline di antara periode refresh (selaras dengan app offline-first).
 */
class LicenseCache(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context, "license_store", masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val REFRESH_MARGIN_MS = 24 * 60 * 60 * 1000L // re-check 1 hari sebelum expiry
    }

    fun store(mask: Long, expiresAt: Long, purchaseToken: String, productId: String) {
        prefs.edit()
            .putLong("mask", mask)
            .putLong("expiresAt", expiresAt)
            .putString("token", purchaseToken)
            .putString("productId", productId)
            .apply()
        StateVault.sync(mask)
        NativeBridge.setCachedMask(mask)
    }

    fun loadIntoMemory() {
        val mask = prefs.getLong("mask", 0L)
        StateVault.sync(mask)
        NativeBridge.setCachedMask(mask)
    }

    fun needsRefresh(): Boolean =
        System.currentTimeMillis() > prefs.getLong("expiresAt", 0L) - REFRESH_MARGIN_MS

    fun lastToken(): Pair<String, String>? {
        val token = prefs.getString("token", null) ?: return null
        val productId = prefs.getString("productId", null) ?: return null
        return token to productId
    }

    fun revoke() {
        prefs.edit().clear().apply()
        StateVault.sync(0L)
        NativeBridge.setCachedMask(0L)
    }
}
