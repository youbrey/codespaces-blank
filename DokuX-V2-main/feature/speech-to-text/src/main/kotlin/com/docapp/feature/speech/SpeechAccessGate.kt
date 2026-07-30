package com.docapp.feature.speech

import com.docapp.core.gate.FeatureIds
import com.docapp.core.security.NativeBridge

/**
 * Kebijakan: rekam mikrofon durasi pendek (< MAX_FREE_SECONDS) tetap gratis
 * untuk semua user. Sesi lebih panjang dan transkrip file audio (butuh model
 * Vosk lebih besar) masuk fitur produktivitas lanjutan (K7).
 */
class SpeechAccessGate(private val native: NativeBridge = NativeBridge) {

    companion object { const val MAX_FREE_SECONDS = 60 }

    fun canUseFileTranscription(): Boolean = native.chk(FeatureIds.K7)

    fun canRecordBeyondFreeLimit(): Boolean = native.chk(FeatureIds.K7)
}
