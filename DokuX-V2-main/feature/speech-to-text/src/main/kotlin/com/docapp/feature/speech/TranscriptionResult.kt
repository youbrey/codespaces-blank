package com.docapp.feature.speech

sealed class TranscriptionResult {
    data class Success(val text: String, val confidence: Float) : TranscriptionResult()
    data class Partial(val text: String) : TranscriptionResult() // hasil sementara saat live listening
    data class Error(val message: String) : TranscriptionResult()
}
