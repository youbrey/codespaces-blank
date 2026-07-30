package com.docapp.data.filesystem

import android.content.Context
import android.net.Uri
import com.docapp.core.model.Document
import com.docapp.data.docx.DocxReader
import com.docapp.data.docx.DocxWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Wrapper akses file via Storage Access Framework (ACTION_OPEN_DOCUMENT / ACTION_CREATE_DOCUMENT).
 * Tidak minta izin storage broad — sesuai kebijakan Scoped Storage.
 * SAF tidak dukung rename atomic langsung ke content:// Uri, jadi strategi:
 * tulis ke cache lokal dulu (atomic via DocxWriter), lalu salin byte ke OutputStream target.
 */
class SafFileRepository(private val context: Context) {

    private val reader = DocxReader()
    private val writer = DocxWriter()

    /** Buka dokumen .docx dari Uri yang dipilih user lewat ACTION_OPEN_DOCUMENT. */
    suspend fun openFromUri(uri: Uri): Document = withContext(Dispatchers.IO) {
        val tmp = File(context.cacheDir, "open_${System.nanoTime()}.docx")
        context.contentResolver.openInputStream(uri)?.use { input ->
            tmp.outputStream().use { output -> input.copyTo(output) }
        } ?: error("Tidak bisa membuka Uri: $uri")
        try {
            reader.read(tmp)
        } finally {
            tmp.delete()
        }
    }

    /** Simpan dokumen ke Uri hasil ACTION_CREATE_DOCUMENT (save-as) atau Uri tersimpan sebelumnya. */
    suspend fun saveToUri(doc: Document, uri: Uri): Unit = withContext(Dispatchers.IO) {
        val tmp = File(context.cacheDir, "save_${System.nanoTime()}.docx")
        writer.write(doc, tmp) // atomic write + verifikasi di local cache dulu
        context.contentResolver.openOutputStream(uri, "wt")?.use { output ->
            tmp.inputStream().use { input -> input.copyTo(output) }
        } ?: error("Tidak bisa menulis ke Uri: $uri")
        tmp.delete()
    }

    /** Simpan ke app-scoped storage internal (dokumen default / autosave), tanpa perlu SAF. */
    suspend fun saveInternal(doc: Document, fileName: String): File = withContext(Dispatchers.IO) {
        val dir = File(context.getExternalFilesDir(null), "Documents").apply { mkdirs() }
        val target = File(dir, if (fileName.endsWith(".docx")) fileName else "$fileName.docx")
        writer.write(doc, target)
        target
    }

    /** Salin snapshot revisi ke folder .versions/{documentId}/ (dipakai revision history). */
    suspend fun saveRevisionSnapshot(doc: Document, documentId: String): File = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, ".versions/$documentId").apply { mkdirs() }
        val target = File(dir, "rev_${System.currentTimeMillis()}.docx")
        writer.write(doc, target)
        target
    }

    fun openInternal(file: File): Document = reader.read(file)
}
