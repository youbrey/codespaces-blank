package com.docapp.data.billing

import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

/**
 * Client HTTP sederhana ke endpoint verifikasi (tanpa Retrofit agar modul ringan).
 * BASE_URL diisi domain Cloud Function setelah deploy (lihat server/functions).
 */
class LicenseApi(private val baseUrl: String = BuildConfigUrls.VERIFY_ENDPOINT) {

    fun verify(request: VerifyRequest): VerifyResponse {
        val conn = (URL("$baseUrl/verify-purchase").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            doOutput = true
            connectTimeout = 10_000
            readTimeout = 10_000
        }
        val body = JSONObject().apply {
            put("purchaseToken", request.purchaseToken)
            put("productId", request.productId)
            put("packageName", request.packageName)
            put("deviceId", request.deviceId)
        }
        conn.outputStream.use { it.write(body.toString().toByteArray()) }

        return conn.inputStream.bufferedReader().use { reader ->
            val json = JSONObject(reader.readText())
            VerifyResponse(
                valid = json.getBoolean("valid"),
                mask = json.getLong("mask"),
                expiresAt = json.getLong("expiresAt")
            )
        }
    }
}

object BuildConfigUrls {
    // Ganti setelah deploy: https://REGION-PROJECT_ID.cloudfunctions.net
    const val VERIFY_ENDPOINT = "https://asia-southeast1-docapp-prod.cloudfunctions.net"
}
