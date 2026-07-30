package com.docapp.feature.speech

import android.content.Context
import org.vosk.android.StorageService
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.zip.ZipInputStream

/**
 * Model Vosk tidak dibundel di APK (ukuran ~50MB/bahasa akan membengkakkan
 * ukuran install) — diunduh sekali saat user pertama pakai fitur file-transcription,
 * lalu dipakai offline sepenuhnya sesudahnya (selaras prinsip offline-first:
 * hanya butuh koneksi sekali di awal, bukan setiap transkrip).
 */
class VoskModelManager(private val context: Context) {

    private val modelUrls = mapOf(
        "id" to "https://alphacephei.com/vosk/models/vosk-model-small-id-0.22.zip",
        "en" to "https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip"
    )

    fun modelDir(lang: String): File = File(context.filesDir, "vosk-model-$lang")

    fun isDownloaded(lang: String): Boolean = modelDir(lang).exists() && modelDir(lang).listFiles()?.isNotEmpty() == true

    /**
     * Download + ekstrak model. Dipanggil dari background thread (WorkManager/coroutine IO).
     * Progress callback untuk ditampilkan di UI (mis. "Mengunduh model bahasa... 42%").
     */
    fun download(lang: String, onProgress: (Int) -> Unit): Result<Unit> = try {
        val url = modelUrls[lang] ?: error("Bahasa '$lang' tidak didukung")
        val connection = URL(url).openConnection()
        val totalSize = connection.contentLength
        var downloaded = 0

        val tempZip = File(context.cacheDir, "vosk-$lang.zip")
        connection.getInputStream().use { input ->
            FileOutputStream(tempZip).use { output ->
                val buffer = ByteArray(8192)
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    output.write(buffer, 0, read)
                    downloaded += read
                    if (totalSize > 0) onProgress((downloaded * 100) / totalSize)
                }
            }
        }

        extractZip(tempZip, modelDir(lang))
        tempZip.delete()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    private fun extractZip(zipFile: File, targetDir: File) {
        targetDir.mkdirs()
        ZipInputStream(zipFile.inputStream()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val outFile = File(targetDir, entry.name)
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { zip.copyTo(it) }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
    }

    fun deleteModel(lang: String) {
        modelDir(lang).deleteRecursively()
    }
}
