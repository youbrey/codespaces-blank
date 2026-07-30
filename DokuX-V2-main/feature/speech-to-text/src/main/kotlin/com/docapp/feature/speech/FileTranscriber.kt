package com.docapp.feature.speech

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File

/**
 * Transkrip file audio (mp3/m4a/wav) yang diimpor user, murni on-device.
 * Decode via AudioDecoder (MediaCodec bawaan Android), lalu feed ke Vosk.
 * Bagian dari fitur produktivitas lanjutan (K7) — lihat SpeechAccessGate.
 */
class FileTranscriber(private val modelManager: VoskModelManager) {

    suspend fun transcribe(audioFile: File, languageModel: String = "id"): TranscriptionResult =
        withContext(Dispatchers.Default) {
            try {
                require(audioFile.exists()) { "File audio tidak ditemukan" }
                if (!modelManager.isDownloaded(languageModel)) {
                    return@withContext TranscriptionResult.Error(
                        "Model bahasa belum diunduh. Unduh dulu lewat pengaturan Suara ke Teks."
                    )
                }

                val pcm = AudioDecoder.decodeToPcm16kMono(audioFile)
                val model = Model(modelManager.modelDir(languageModel).absolutePath)
                val recognizer = Recognizer(model, 16000f)

                val chunkSize = 4096
                val fullText = StringBuilder()
                var offset = 0
                while (offset < pcm.size) {
                    val end = minOf(offset + chunkSize, pcm.size)
                    val chunk = pcm.copyOfRange(offset, end)
                    if (recognizer.acceptWaveForm(chunk, chunk.size)) {
                        val partial = JSONObject(recognizer.result).optString("text", "")
                        if (partial.isNotBlank()) fullText.append(partial).append(" ")
                    }
                    offset = end
                }
                val finalText = JSONObject(recognizer.finalResult).optString("text", "")
                fullText.append(finalText)

                recognizer.close(); model.close()

                TranscriptionResult.Success(text = fullText.toString().trim(), confidence = 0.85f)
            } catch (e: Exception) {
                TranscriptionResult.Error(e.message ?: "Gagal memproses file audio")
            }
        }
}
