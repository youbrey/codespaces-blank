package com.docapp.core.layout

import android.graphics.Paint
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.docapp.core.model.Document
import com.docapp.core.model.LineSpacing
import com.docapp.core.model.Paragraph
import com.docapp.core.model.PaperSize
import kotlin.math.roundToInt

data class VisualLine(val text: CharSequence, val heightPx: Float, val baselinePx: Float)
data class LayoutPage(val pageIndex: Int, val lines: List<VisualLine>)

private const val MM_TO_PX = 3.7795f // asumsi ~96dpi; disesuaikan actual density saat render

/**
 * Menghitung pagination dokumen. Dipakai bersama oleh editor view dan PDF exporter
 * agar hasil WYSIWYG konsisten (satu sumber kebenaran layout).
 */
class LayoutEngine {

    fun paginate(doc: Document): List<LayoutPage> {
        val pageWidthPx = mmToPx(doc.pageSetup.widthMm) -
            mmToPx(doc.pageSetup.margin.leftMm + doc.pageSetup.margin.rightMm)
        val pageHeightPx = mmToPx(doc.pageSetup.heightMm) -
            mmToPx(doc.pageSetup.margin.topMm + doc.pageSetup.margin.bottomMm)

        val pages = mutableListOf<LayoutPage>()
        var currentLines = mutableListOf<VisualLine>()
        var heightUsed = 0f
        var pageIndex = 0

        doc.sections.flatMap { it.paragraphs }.forEach { paragraph ->
            shapeParagraph(paragraph, pageWidthPx).forEach { line ->
                if (heightUsed + line.heightPx > pageHeightPx && currentLines.isNotEmpty()) {
                    pages += LayoutPage(pageIndex++, currentLines)
                    currentLines = mutableListOf()
                    heightUsed = 0f
                }
                currentLines += line
                heightUsed += line.heightPx
            }
        }
        if (currentLines.isNotEmpty()) pages += LayoutPage(pageIndex, currentLines)
        return pages
    }

    /** Ubah satu paragraf jadi baris-baris visual via StaticLayout (line-breaking asli Android). */
    private fun shapeParagraph(paragraph: Paragraph, maxWidthPx: Int): List<VisualLine> {
        if (paragraph.runs.isEmpty()) return listOf(VisualLine("", defaultLineHeight(), 0f))

        val text = buildSpannedText(paragraph)
        val paint = TextPaint().apply {
            textSize = (paragraph.runs.first().fontSizePt * 1.333f) // pt -> px approx
            isAntiAlias = true
        }
        val multiplier = spacingMultiplier(paragraph.lineSpacing)

        val staticLayout = StaticLayout.Builder
            .obtain(text, 0, text.length, paint, maxWidthPx)
            .setLineSpacing(0f, multiplier)
            .setBreakStrategy(Layout.BREAK_STRATEGY_HIGH_QUALITY)
            .setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NORMAL)
            .build()

        return (0 until staticLayout.lineCount).map { i ->
            val start = staticLayout.getLineStart(i)
            val end = staticLayout.getLineEnd(i)
            val top = staticLayout.getLineTop(i)
            val bottom = staticLayout.getLineBottom(i)
            VisualLine(text.subSequence(start, end), (bottom - top).toFloat(), staticLayout.getLineBaseline(i).toFloat())
        }
    }

    private fun buildSpannedText(paragraph: Paragraph): CharSequence =
        paragraph.runs.joinToString(separator = "") { it.text } // spans formatting ditangani di layer render

    private fun spacingMultiplier(spacing: LineSpacing): Float = when (spacing) {
        is LineSpacing.Single -> 1.0f
        is LineSpacing.OnePointFive -> 1.5f
        is LineSpacing.Double -> 2.0f
        is LineSpacing.Multiple -> spacing.factor
        is LineSpacing.Exact -> 1.0f // ditangani terpisah via fixed line height
    }

    private fun defaultLineHeight(): Float = 11f * 1.333f * 1.2f

    private fun mmToPx(mm: Float): Int = (mm * MM_TO_PX).roundToInt()
}
