package com.docapp.feature.speech

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer

/**
 * Decode file audio (mp3/m4a/aac/wav) menjadi PCM 16-bit mono 16kHz,
 * format yang dibutuhkan Vosk. Pakai MediaCodec bawaan Android (tanpa
 * dependency eksternal tambahan), konsisten dengan prinsip offline-first.
 */
object AudioDecoder {

    fun decodeToPcm16kMono(file: File): ByteArray {
        val extractor = MediaExtractor().apply { setDataSource(file.absolutePath) }
        val trackIndex = selectAudioTrack(extractor)
        require(trackIndex >= 0) { "Tidak ada track audio ditemukan di file" }
        extractor.selectTrack(trackIndex)

        val format = extractor.getTrackFormat(trackIndex)
        val mime = format.getString(MediaFormat.KEY_MIME) ?: error("Format audio tidak dikenali")
        val sourceSampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val sourceChannels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

        val codec = MediaCodec.createDecoderByType(mime).apply {
            configure(format, null, null, 0)
            start()
        }

        val rawPcm = ByteArrayOutputStream()
        val bufferInfo = MediaCodec.BufferInfo()
        var inputDone = false
        var outputDone = false

        while (!outputDone) {
            if (!inputDone) {
                val inputIndex = codec.dequeueInputBuffer(10_000)
                if (inputIndex >= 0) {
                    val inputBuffer = codec.getInputBuffer(inputIndex) ?: ByteBuffer.allocate(0)
                    val sampleSize = extractor.readSampleData(inputBuffer, 0)
                    if (sampleSize < 0) {
                        codec.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        inputDone = true
                    } else {
                        codec.queueInputBuffer(inputIndex, 0, sampleSize, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }
            }

            val outputIndex = codec.dequeueOutputBuffer(bufferInfo, 10_000)
            if (outputIndex >= 0) {
                val outputBuffer = codec.getOutputBuffer(outputIndex)
                val chunk = ByteArray(bufferInfo.size)
                outputBuffer?.get(chunk)
                rawPcm.write(chunk)
                codec.releaseOutputBuffer(outputIndex, false)
                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
            }
        }

        codec.stop(); codec.release(); extractor.release()

        val decoded = rawPcm.toByteArray()
        return resampleTo16kMono(decoded, sourceSampleRate, sourceChannels)
    }

    private fun selectAudioTrack(extractor: MediaExtractor): Int {
        for (i in 0 until extractor.trackCount) {
            val mime = extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith("audio/")) return i
        }
        return -1
    }

    /** Downmix ke mono + resample sederhana (nearest-neighbor) ke 16kHz — cukup untuk speech recognition. */
    private fun resampleTo16kMono(pcm: ByteArray, sourceSampleRate: Int, channels: Int): ByteArray {
        val samples = ShortArray(pcm.size / 2)
        ByteBuffer.wrap(pcm).asShortBuffer().get(samples)

        val mono = if (channels == 1) samples else downmixToMono(samples, channels)
        if (sourceSampleRate == 16000) return shortsToBytes(mono)

        val ratio = sourceSampleRate.toDouble() / 16000.0
        val outLength = (mono.size / ratio).toInt()
        val resampled = ShortArray(outLength) { i -> mono[(i * ratio).toInt().coerceAtMost(mono.size - 1)] }
        return shortsToBytes(resampled)
    }

    private fun downmixToMono(samples: ShortArray, channels: Int): ShortArray {
        val frames = samples.size / channels
        return ShortArray(frames) { i ->
            var sum = 0
            for (c in 0 until channels) sum += samples[i * channels + c]
            (sum / channels).toShort()
        }
    }

    private fun shortsToBytes(shorts: ShortArray): ByteArray {
        val bytes = ByteArray(shorts.size * 2)
        ByteBuffer.wrap(bytes).asShortBuffer().put(shorts)
        return bytes
    }
}
