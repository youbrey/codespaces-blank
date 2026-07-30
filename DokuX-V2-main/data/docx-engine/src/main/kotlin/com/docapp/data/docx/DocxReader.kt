package com.docapp.data.docx

import android.util.Xml
import com.docapp.core.model.*
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.util.zip.ZipFile

/**
 * Membaca file .docx (ZIP + OOXML) menjadi model Document.
 * Batasan keamanan: cegah zip bomb dengan limit entry & ukuran ekstraksi.
 * Cakupan: teks+run formatting (b/i/u/strike/color/highlight/size), alignment,
 * w:sectPr (ukuran halaman+margin), w:numPr (heuristik bullet/numbering),
 * w:drawing (gambar -> disalin ke cache lokal), w:tbl (tabel dasar).
 */
class DocxReader {

    companion object {
        private const val MAX_ENTRIES = 5000
        private const val MAX_UNCOMPRESSED_BYTES = 200L * 1024 * 1024 // 200MB
        private const val TWIPS_PER_MM = 56.6929f
    }

    fun read(file: File): Document {
        ZipFile(file).use { zip ->
            require(zip.size() <= MAX_ENTRIES) { "Jumlah entry ZIP melebihi batas aman" }
            var totalUncompressed = 0L
            zip.entries().asSequence().forEach { entry ->
                totalUncompressed += entry.size
                require(totalUncompressed <= MAX_UNCOMPRESSED_BYTES) { "Ukuran ekstraksi melebihi batas aman" }
            }

            val documentEntry = zip.getEntry("word/document.xml")
                ?: error("File .docx tidak valid: word/document.xml tidak ditemukan")

            val parser: XmlPullParser = Xml.newPullParser().apply {
                setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                setInput(zip.getInputStream(documentEntry), "UTF-8")
            }
            return parseDocument(parser, file.nameWithoutExtension)
        }
    }

    private fun parseDocument(parser: XmlPullParser, title: String): Document {
        val paragraphs = mutableListOf<Paragraph>()
        var currentRuns = mutableListOf<TextRun>()
        var currentAlignment = Alignment.LEFT
        var currentListInfo: ListInfo? = null
        var currentInline = mutableListOf<InlineObject>()

        // run pending state
        var runBold = false; var runItalic = false; var runStrike = false
        var runUnderline = UnderlineStyle.NONE; var runColor = 0xFF000000.toInt()
        var runHighlight: Int? = null; var runSizePt = 11f

        var pageSetup = PageSetup()
        var tableRows: MutableList<List<String>>? = null
        var tableRowCells: MutableList<String>? = null
        var cellTextBuf = StringBuilder()
        var inTable = false

        var event = parser.eventType
        var paragraphCounter = 0

        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "p" -> {
                        currentRuns = mutableListOf(); currentAlignment = Alignment.LEFT
                        currentListInfo = null; currentInline = mutableListOf()
                    }
                    "jc" -> currentAlignment = mapAlignment(parser.getAttributeValue(null, "val"))
                    "numPr" -> currentListInfo = ListInfo(level = 0, isOrdered = false) // heuristik: detail numId/ilvl di numbering.xml (belum di-resolve)
                    "ilvl" -> currentListInfo = currentListInfo?.copy(level = parser.getAttributeValue(null, "val")?.toIntOrNull() ?: 0)
                    "r" -> { runBold = false; runItalic = false; runStrike = false; runUnderline = UnderlineStyle.NONE; runColor = 0xFF000000.toInt(); runHighlight = null; runSizePt = 11f }
                    "b" -> runBold = parser.getAttributeValue(null, "val") != "false"
                    "i" -> runItalic = parser.getAttributeValue(null, "val") != "false"
                    "strike" -> runStrike = parser.getAttributeValue(null, "val") != "false"
                    "u" -> runUnderline = if (parser.getAttributeValue(null, "val") in listOf(null, "none")) UnderlineStyle.NONE else UnderlineStyle.SINGLE
                    "color" -> parser.getAttributeValue(null, "val")?.let { runColor = parseHexColor(it) }
                    "highlight" -> parser.getAttributeValue(null, "val")?.let { runHighlight = parseNamedHighlight(it) }
                    "sz" -> parser.getAttributeValue(null, "val")?.toFloatOrNull()?.let { runSizePt = it / 2f } // half-points -> pt
                    "t" -> {
                        val text = if (parser.next() == XmlPullParser.TEXT) parser.text else ""
                        if (inTable) cellTextBuf.append(text)
                        else currentRuns.add(TextRun(text, runBold, runItalic, runUnderline, runStrike, "Liberation Sans", runSizePt, runColor, runHighlight))
                    }
                    "tbl" -> { inTable = true; tableRows = mutableListOf() }
                    "tr" -> tableRowCells = mutableListOf()
                    "tc" -> cellTextBuf = StringBuilder()
                    "blip" -> {
                        // r:embed berisi relationship id gambar; path fisik diselesaikan di layer caller (butuh akses ZIP rels)
                        currentInline.add(InlineObject.Image(localPath = parser.getAttributeValue(null, "embed") ?: "", widthMm = 100f, heightMm = 75f))
                    }
                    "pgSz" -> {
                        val w = parser.getAttributeValue(null, "w")?.toFloatOrNull()
                        val h = parser.getAttributeValue(null, "h")?.toFloatOrNull()
                        if (w != null && h != null) {
                            val size = matchPaperSize(w / TWIPS_PER_MM, h / TWIPS_PER_MM)
                            pageSetup = pageSetup.copy(size = size, orientation = if (w > h) Orientation.LANDSCAPE else Orientation.PORTRAIT)
                        }
                    }
                    "pgMar" -> {
                        val top = parser.getAttributeValue(null, "top")?.toFloatOrNull()
                        val bottom = parser.getAttributeValue(null, "bottom")?.toFloatOrNull()
                        val left = parser.getAttributeValue(null, "left")?.toFloatOrNull()
                        val right = parser.getAttributeValue(null, "right")?.toFloatOrNull()
                        if (top != null && bottom != null && left != null && right != null) {
                            pageSetup = pageSetup.copy(margin = Margin(top / TWIPS_PER_MM, bottom / TWIPS_PER_MM, left / TWIPS_PER_MM, right / TWIPS_PER_MM))
                        }
                    }
                }
                XmlPullParser.END_TAG -> when (parser.name) {
                    "p" -> {
                        paragraphs += Paragraph(
                            id = "p${paragraphCounter++}", runs = currentRuns.toList(),
                            alignment = currentAlignment, listInfo = currentListInfo, inlineObjects = currentInline.toList()
                        )
                    }
                    "tc" -> tableRowCells?.add(cellTextBuf.toString())
                    "tr" -> tableRowCells?.let { tableRows?.add(it.toList()) }
                    "tbl" -> {
                        inTable = false
                        val rows = tableRows ?: emptyList()
                        val cols = rows.maxOfOrNull { it.size } ?: 0
                        paragraphs += Paragraph(
                            id = "tbl${paragraphCounter++}", runs = emptyList(), alignment = Alignment.LEFT,
                            inlineObjects = listOf(InlineObject.Table(rows, List(cols) { 40f }))
                        )
                        tableRows = null
                    }
                }
            }
            event = parser.next()
        }

        val now = System.currentTimeMillis()
        return Document(
            id = title, title = title, pageSetup = pageSetup,
            sections = listOf(Section(paragraphs = paragraphs)),
            createdAt = now, modifiedAt = now
        )
    }

    private fun mapAlignment(value: String?): Alignment = when (value) {
        "center" -> Alignment.CENTER; "right" -> Alignment.RIGHT
        "both" -> Alignment.JUSTIFY; else -> Alignment.LEFT
    }

    private fun parseHexColor(hex: String): Int = try {
        (0xFF000000.toInt()) or hex.removePrefix("#").toInt(16)
    } catch (e: NumberFormatException) { 0xFF000000.toInt() }

    private fun parseNamedHighlight(name: String): Int? = when (name) {
        "yellow" -> 0xFFFFFF00.toInt(); "green" -> 0xFF00FF00.toInt()
        "cyan" -> 0xFF00FFFF.toInt(); "none" -> null; else -> null
    }

    private fun matchPaperSize(wMm: Float, hMm: Float): PaperSize {
        val candidates = PaperSize.values()
        return candidates.minByOrNull { p ->
            val (pw, ph) = if (wMm > hMm) maxOf(p.widthMm, p.heightMm) to minOf(p.widthMm, p.heightMm) else minOf(p.widthMm, p.heightMm) to maxOf(p.widthMm, p.heightMm)
            kotlin.math.abs(pw - maxOf(wMm, hMm)) + kotlin.math.abs(ph - minOf(wMm, hMm))
        } ?: PaperSize.A4
    }
}
