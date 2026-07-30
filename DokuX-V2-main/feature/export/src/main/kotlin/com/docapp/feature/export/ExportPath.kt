package com.docapp.feature.export

import android.graphics.Canvas
import android.graphics.Paint
import com.docapp.core.gate.FeatureIds
import com.docapp.core.security.NativeBridge

/**
 * Menentukan apakah watermark ditambahkan ke hasil export.
 * Pengecekan lewat NativeBridge (JNI), bukan Kotlin boolean biasa,
 * supaya logika tidak langsung terlihat saat decompile bytecode.
 */
class ExportPath(private val native: NativeBridge = NativeBridge) {

    fun applyToCanvas(canvas: Canvas, pageWidthPx: Int, pageHeightPx: Int) {
        if (native.chk(FeatureIds.R4)) return // fitur aktif, tidak perlu watermark
        drawWatermark(canvas, pageWidthPx, pageHeightPx)
    }

    private fun drawWatermark(canvas: Canvas, w: Int, h: Int) {
        val paint = Paint().apply {
            color = 0x33000000
            textSize = 28f
            isAntiAlias = true
        }
        canvas.drawText("Dibuat dengan DocEditor", (w - 260).toFloat(), (h - 24).toFloat(), paint)
    }
}
