package com.docapp.feature.speech

import android.content.Context
import android.content.Intent
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Rekam suara langsung dari mikrofon dan transkrip real-time.
 * Android SpeechRecognizer bisa berjalan offline jika model bahasa on-device
 * sudah diunduh (Pengaturan > Bahasa > Google > Pengenalan suara offline) —
 * fallback ke online otomatis jika model lokal tidak tersedia.
 *
 * Fitur dasar (durasi pendek, < batas gratis) tetap gratis; sesi panjang/berulang
 * masuk gate K7 (fitur produktivitas lanjutan), dicek di layer pemanggil (ViewModel).
 */
class MicTranscriber(private val context: Context) {

    fun listen(): Flow<TranscriptionResult> = callbackFlow {
        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "id-ID")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true) // paksa offline jika model tersedia
        }

        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: android.os.Bundle) {
                val text = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                trySend(TranscriptionResult.Success(text, confidence = 1f))
            }
            override fun onPartialResults(partial: android.os.Bundle) {
                val text = partial.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                trySend(TranscriptionResult.Partial(text))
            }
            override fun onError(error: Int) {
                trySend(TranscriptionResult.Error("Gagal transkrip (kode $error)"))
            }
            override fun onReadyForSpeech(params: android.os.Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
        })

        recognizer.startListening(intent)
        awaitClose { recognizer.destroy() }
    }
}
