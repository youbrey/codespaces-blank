package com.docapp.data.billing

data class VerifyRequest(
    val purchaseToken: String,
    val productId: String,
    val packageName: String,
    val deviceId: String
)

data class VerifyResponse(
    val valid: Boolean,
    val mask: Long,
    val expiresAt: Long
)

/** Produk yang terdaftar di Play Console — dua SKU sesuai keputusan harga. */
object ProductIds {
    const val PRO_BUNDLE = "pro_bundle_onetime"       // $1.99, one-time IAP
    const val ESSENTIAL_SUB = "essential_monthly"      // Rp 3.000/bulan, subscription
}
