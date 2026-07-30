package com.docapp.data.billing

import android.content.Context
import android.provider.Settings
import com.android.billingclient.api.*
import com.docapp.core.security.LicenseCache
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class PurchaseRepository(
    private val context: Context,
    private val licenseCache: LicenseCache,
    private val api: LicenseApi = LicenseApi()
) {
    private val deviceId: String
        get() = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)

    private lateinit var billingClient: BillingClient

    fun initialize(onReady: () -> Unit) {
        billingClient = BillingClient.newBuilder(context)
            .setListener { result, purchases ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    purchases?.forEach { handlePurchase(it) }
                }
            }
            .enablePendingPurchases()
            .build()

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) onReady()
            }
            override fun onBillingServiceDisconnected() { /* retry via WorkManager jika perlu */ }
        })
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        val productId = purchase.products.firstOrNull() ?: return

        val response = api.verify(
            VerifyRequest(purchase.purchaseToken, productId, context.packageName, deviceId)
        )

        if (response.valid) {
            licenseCache.store(response.mask, response.expiresAt, purchase.purchaseToken, productId)
            acknowledgeIfNeeded(purchase)
        }
    }

    private fun acknowledgeIfNeeded(purchase: Purchase) {
        if (purchase.isAcknowledged) return
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken).build()
        billingClient.acknowledgePurchase(params) { /* no-op, log jika gagal */ }
    }

    /** Dipanggil berkala (mis. WorkManager tiap 24 jam saat online) untuk re-cek status. */
    fun periodicRevalidate() {
        if (!licenseCache.needsRefresh()) return
        val (token, productId) = licenseCache.lastToken() ?: return
        val response = api.verify(VerifyRequest(token, productId, context.packageName, deviceId))
        if (response.valid) {
            licenseCache.store(response.mask, response.expiresAt, token, productId)
        } else {
            licenseCache.revoke() // refund/cancel terdeteksi -> fitur terkunci lagi
        }
    }

    fun launchPurchase(activity: android.app.Activity, productId: String) {
        billingClient.queryProductDetailsAsync(
            QueryProductDetailsParams.newBuilder()
                .setProductList(listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productId)
                        .setProductType(
                            if (productId == ProductIds.ESSENTIAL_SUB) BillingClient.ProductType.SUBS
                            else BillingClient.ProductType.INAPP
                        ).build()
                )).build()
        ) { result, productDetailsList ->
            val details = productDetailsList.firstOrNull() ?: return@queryProductDetailsAsync
            val flowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details).build()
                )).build()
            billingClient.launchBillingFlow(activity, flowParams)
        }
    }
}
