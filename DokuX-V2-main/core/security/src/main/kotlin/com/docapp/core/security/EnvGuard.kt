package com.docapp.core.security

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Debug
import java.io.File
import java.security.MessageDigest

/**
 * Sinyal risiko lingkungan (root, debugger, emulator, signature mismatch).
 * Dipakai untuk menaikkan frekuensi re-verifikasi server, BUKAN untuk
 * blokir otomatis — banyak user legit root device untuk alasan lain.
 */
object EnvGuard {

    fun signatureValid(context: Context): Boolean = try {
        @Suppress("DEPRECATION")
        val info = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
        val actual = MessageDigest.getInstance("SHA-256").digest(info.signatures[0].toByteArray())
        actual.contentEquals(NativeBridge.expectedSig())
    } catch (e: Exception) { false }

    fun riskScore(): Int = listOf(
        isProbablyRooted(), Debug.isDebuggerConnected(), isProbablyEmulator()
    ).count { it }

    private fun isProbablyRooted(): Boolean =
        listOf("/system/bin/su", "/system/xbin/su", "/sbin/su").any { File(it).exists() }

    private fun isProbablyEmulator(): Boolean =
        Build.FINGERPRINT.startsWith("generic") || Build.MODEL.contains("Emulator") || Build.MODEL.contains("sdk_gphone")
}
