package com.docapp.core.security

object NativeBridge {
    @Volatile private var cachedMask: Long = 0L

    fun chk(featureId: Int): Boolean {
        return ((cachedMask shr featureId) and 1L) != 0L
    }

    fun expectedSig(): ByteArray {
        return ByteArray(32)
    }

    fun setCachedMask(mask: Long) {
        cachedMask = mask
    }
}
