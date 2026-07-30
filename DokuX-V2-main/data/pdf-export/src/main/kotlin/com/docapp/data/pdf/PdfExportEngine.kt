package com.docapp.data.pdf

import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.docapp.core.layout.LayoutEngine
import com.docapp.core.layout.LayoutPage
import com.docapp.core.model.Document
import com.docapp.feature.export.ExportPath
import java.io.File
import java.io.FileOutputStream

/**
 * Export dokumen ke PDF. Memakai LayoutEngine yang sama dengan editor
 * agar WYSIWYG akurat: apa yang tampil saat edit = apa yang keluar di PDF.
 */
class PdfExportEngine(
    private val layoutEngine: LayoutEngine = LayoutEngine(),
    private val exportPath: ExportPath = ExportPath()
) {
    fun export(document: Document, outputFile: File) {
        val pages = layoutEngine.paginate(document)
        val pdf = PdfDocument()

        pages.forEach { page ->
            val widthPt = mmToPt(document.pageSetup.widthMm)
            val heightPt = mmToPt(document.pageSetup.heightMm)
            val info = PdfDocument.PageInfo.Builder(widthPt, heightPt, page.pageIndex).create()
            val pdfPage = pdf.startPage(info)

            renderLines(page, pdfPage.canvas, document)
            exportPath.applyToCanvas(pdfPage.canvas, widthPt, heightPt) // gate watermark

            pdf.finishPage(pdfPage)
        }

        FileOutputStream(outputFile).use { pdf.writeTo(it) }
        pdf.close()
    }

    private fun renderLines(page: LayoutPage, canvas: android.graphics.Canvas, doc: Document) {
        val paint = Paint().apply { textSize = 14f; isAntiAlias = true }
        val marginTopPt = mmToPt(doc.pageSetup.margin.topMm)
        val marginLeftPt = mmToPt(doc.pageSetup.margin.leftMm)
        var y = marginTopPt.toFloat()
        page.lines.forEach { line ->
            canvas.drawText(line.text.toString(), marginLeftPt.toFloat(), y + line.baselinePx, paint)
            y += line.heightPx
        }
    }

    private fun mmToPt(mm: Float): Int = (mm * 2.8346f).toInt() // 1mm = 2.8346pt
}
