package com.docapp.feature.ai

import com.docapp.core.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

sealed class AiGenerateResult {
    data class Success(val document: Document) : AiGenerateResult()
    data class Error(val message: String) : AiGenerateResult()
}

/**
 * Generate dokumen dari prompt via Gemini API. Dipanggil hanya setelah GoogleAuthGate
 * memastikan user login. API key server-side direkomendasikan (proxy lewat Cloud Function)
 * agar key tidak tertanam di APK — lihat server/functions/src/index.ts (generateDocument).
 */
class GeminiDocumentGenerator(private val proxyEndpoint: String) {

    suspend fun generate(prompt: String, idToken: String): AiGenerateResult = withContext(Dispatchers.IO) {
        try {
            val conn = (URL("$proxyEndpoint/generate-document").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer $idToken")
                doOutput = true
                connectTimeout = 20_000
                readTimeout = 30_000
            }
            val body = JSONObject().put("prompt", prompt)
            conn.outputStream.use { it.write(body.toString().toByteArray()) }

            if (conn.responseCode != 200) {
                return@withContext AiGenerateResult.Error("Gagal generate (kode ${conn.responseCode})")
            }

            val json = JSONObject(conn.inputStream.bufferedReader().readText())
            AiGenerateResult.Success(parseToDocument(json))
        } catch (e: Exception) {
            AiGenerateResult.Error(e.message ?: "Gagal terhubung ke layanan AI")
        }
    }

    /** Respons proxy: { "title": "...", "paragraphs": ["...", "..."] } */
    private fun parseToDocument(json: JSONObject): Document {
        val now = System.currentTimeMillis()
        val paragraphsJson: JSONArray = json.optJSONArray("paragraphs") ?: JSONArray()
        val paragraphs = (0 until paragraphsJson.length()).map { i ->
            Paragraph(id = "p$i", runs = listOf(TextRun(text = paragraphsJson.getString(i))))
        }.ifEmpty { listOf(Paragraph(id = "p0", runs = listOf(TextRun(text = "")))) }

        return Document(
            id = "doc_ai_$now",
            title = json.optString("title", "Dokumen AI"),
            pageSetup = PageSetup(),
            sections = listOf(Section(paragraphs = paragraphs)),
            createdAt = now,
            modifiedAt = now
        )
    }
}
