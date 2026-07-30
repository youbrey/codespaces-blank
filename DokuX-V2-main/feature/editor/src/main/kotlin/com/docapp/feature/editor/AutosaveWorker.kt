package com.docapp.feature.editor

import android.content.Context
import androidx.work.*
import com.docapp.data.db.AppDatabase
import com.docapp.data.db.DocumentEntity
import com.docapp.data.db.RevisionEntity
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Worker autosave: dijalankan setelah dokumen ditulis ke file lokal (lihat EditorViewModel.onAutosave
 * -> SafFileRepository.saveInternal). Tugas worker: update index Room + simpan snapshot revisi +
 * pangkas revisi lama (maks 10). Berjalan via WorkManager supaya tetap selesai walau app di-kill
 * setelah enqueue (guaranteed execution, sesuai dokumentasi kebijakan retensi revisi).
 */
class AutosaveWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val documentId = inputData.getString(KEY_DOC_ID) ?: return Result.failure()
        val title = inputData.getString(KEY_TITLE) ?: "Untitled"
        val filePath = inputData.getString(KEY_FILE_PATH) ?: return Result.failure()
        val file = File(filePath)
        if (!file.exists()) return Result.failure()

        return try {
            val db = AppDatabase.instance(applicationContext)
            db.documentDao().upsert(
                DocumentEntity(
                    id = documentId, title = title, filePath = filePath,
                    createdAt = file.lastModified(), modifiedAt = System.currentTimeMillis(),
                    sizeBytes = file.length()
                )
            )
            val snapshot = File(applicationContext.filesDir, ".versions/$documentId/rev_${System.currentTimeMillis()}.docx")
            snapshot.parentFile?.mkdirs()
            file.copyTo(snapshot, overwrite = true)
            db.revisionDao().insert(
                RevisionEntity(id = "rev_${System.nanoTime()}", documentId = documentId,
                    timestamp = System.currentTimeMillis(), snapshotPath = snapshot.path, label = "Autosave")
            )
            db.revisionDao().pruneOld(documentId, keepCount = 10)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val KEY_DOC_ID = "documentId"
        const val KEY_TITLE = "title"
        const val KEY_FILE_PATH = "filePath"
    }
}

object AutosaveScheduler {
    /** Enqueue autosave sekali, unik per dokumen (REPLACE mencegah tumpukan job saat mengetik cepat). */
    fun schedule(context: Context, documentId: String, title: String, filePath: String) {
        val request = OneTimeWorkRequestBuilder<AutosaveWorker>()
            .setInputData(workDataOf(
                AutosaveWorker.KEY_DOC_ID to documentId,
                AutosaveWorker.KEY_TITLE to title,
                AutosaveWorker.KEY_FILE_PATH to filePath
            ))
            .setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "autosave_$documentId", ExistingWorkPolicy.REPLACE, request
        )
    }
}
