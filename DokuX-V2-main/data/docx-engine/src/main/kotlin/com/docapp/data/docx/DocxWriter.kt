package com.docapp.data.docx

import android.util.Xml
import com.docapp.core.model.*
import org.xmlpull.v1.XmlSerializer
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * Menulis model Document menjadi file .docx (ZIP + OOXML) dengan atomic write:
 * tulis ke .tmp, verifikasi, baru rename. Mencegah file korup jika app di-kill
 * di tengah proses save.
 * Cakupan: run formatting lengkap, w:sectPr, w:tbl (tabel), w:drawing (gambar dasar via media rels).
 */
class DocxWriter {

    fun write(doc: Document, target: File) {
        val tmp = File(target.parentFile, "${target.name}.tmp")
        val images = collectImages(doc)
        ZipOutputStream(FileOutputStream(tmp)).use { zip ->
            writeEntry(zip, "[Content_Types].xml", contentTypesXml(images.isNotEmpty()))
            writeEntry(zip, "_rels/.rels", relsXml())
            writeEntry(zip, "word/_rels/document.xml.rels", documentRelsXml(images))
            writeEntry(zip, "word/document.xml", serializeDocument(doc, images))
            writeEntry(zip, "word/styles.xml", stylesXml())
            writeEntry(zip, "word/numbering.xml", numberingXml())
            images.forEachIndexed { i, img ->
                val bytes = runCatching { File(img.localPath).readBytes() }.getOrNull()
                if (bytes != null) writeBinaryEntry(zip, "word/media/image${i + 1}.png", bytes)
            }
        }
        verifyZip(tmp)
        if (target.exists()) target.delete()
        check(tmp.renameTo(target)) { "Gagal commit file: rename atomic tidak berhasil" }
    }

    private fun collectImages(doc: Document): List<InlineObject.Image> =
        doc.sections.flatMap { it.paragraphs }.flatMap { it.inlineObjects }.filterIsInstance<InlineObject.Image>()

    private fun writeEntry(zip: ZipOutputStream, name: String, content: String) {
        zip.putNextEntry(ZipEntry(name)); zip.write(content.toByteArray(Charsets.UTF_8)); zip.closeEntry()
    }

    private fun writeBinaryEntry(zip: ZipOutputStream, name: String, bytes: ByteArray) {
        zip.putNextEntry(ZipEntry(name)); zip.write(bytes); zip.closeEntry()
    }

    private fun serializeDocument(doc: Document, images: List<InlineObject.Image>): String {
        val serializer = Xml.newSerializer()
        val writer = java.io.StringWriter()
        serializer.setOutput(writer)
        serializer.startDocument("UTF-8", true)
        serializer.startTag(null, "w:document")
        serializer.attribute(null, "xmlns:w", "http://schemas.openxmlformats.org/wordprocessingml/2006/main")
        serializer.attribute(null, "xmlns:r", "http://schemas.openxmlformats.org/officeDocument/2006/relationships")
        serializer.startTag(null, "w:body")

        doc.sections.forEach { section ->
            section.paragraphs.forEach { p -> writeParagraph(serializer, p, images) }
            writeSectionProperties(serializer, doc.pageSetup)
        }

        serializer.endTag(null, "w:body")
        serializer.endTag(null, "w:document")
        serializer.endDocument()
        return writer.toString()
    }

    private fun writeParagraph(s: XmlSerializer, paragraph: Paragraph, images: List<InlineObject.Image>) {
        val table = paragraph.inlineObjects.filterIsInstance<InlineObject.Table>().firstOrNull()
        if (table != null) { writeTable(s, table); return }

        s.startTag(null, "w:p")
        s.startTag(null, "w:pPr")
        s.startTag(null, "w:jc"); s.attribute(null, "w:val", alignmentToXml(paragraph.alignment)); s.endTag(null, "w:jc")
        s.startTag(null, "w:spacing")
        s.attribute(null, "w:before", ptToTwips(paragraph.spacingBeforePt).toString())
        s.attribute(null, "w:after", ptToTwips(paragraph.spacingAfterPt).toString())
        s.endTag(null, "w:spacing")
        paragraph.listInfo?.let {
            s.startTag(null, "w:numPr")
            s.startTag(null, "w:ilvl"); s.attribute(null, "w:val", it.level.toString()); s.endTag(null, "w:ilvl")
            s.startTag(null, "w:numId"); s.attribute(null, "w:val", if (it.isOrdered) "2" else "1"); s.endTag(null, "w:numId")
            s.endTag(null, "w:numPr")
        }
        s.endTag(null, "w:pPr")

        paragraph.runs.forEach { run -> writeRun(s, run) }

        paragraph.inlineObjects.forEach { obj ->
            when (obj) {
                is InlineObject.PageBreak -> {
                    s.startTag(null, "w:r"); s.startTag(null, "w:br"); s.attribute(null, "w:type", "page"); s.endTag(null, "w:br"); s.endTag(null, "w:r")
                }
                is InlineObject.Image -> writeImage(s, obj, images.indexOf(obj) + 1)
                is InlineObject.Hyperlink -> {
                    s.startTag(null, "w:hyperlink"); s.attribute(null, "r:id", "")
                    s.startTag(null, "w:r"); s.startTag(null, "w:t"); s.text(obj.text); s.endTag(null, "w:t"); s.endTag(null, "w:r")
                    s.endTag(null, "w:hyperlink")
                }
                else -> Unit
            }
        }
        s.endTag(null, "w:p")
    }

    private fun writeRun(s: XmlSerializer, run: TextRun) {
        s.startTag(null, "w:r")
        s.startTag(null, "w:rPr")
        if (run.bold) s.startTag(null, "w:b").endTag(null, "w:b")
        if (run.italic) s.startTag(null, "w:i").endTag(null, "w:i")
        if (run.strikethrough) s.startTag(null, "w:strike").endTag(null, "w:strike")
        if (run.underline != UnderlineStyle.NONE) {
            s.startTag(null, "w:u"); s.attribute(null, "w:val", "single"); s.endTag(null, "w:u")
        }
        if (run.colorArgb != 0xFF000000.toInt()) {
            s.startTag(null, "w:color"); s.attribute(null, "w:val", colorToHex(run.colorArgb)); s.endTag(null, "w:color")
        }
        run.highlightArgb?.let {
            s.startTag(null, "w:highlight"); s.attribute(null, "w:val", highlightToName(it)); s.endTag(null, "w:highlight")
        }
        s.startTag(null, "w:sz"); s.attribute(null, "w:val", (run.fontSizePt * 2).toInt().toString()); s.endTag(null, "w:sz")
        s.endTag(null, "w:rPr")
        s.startTag(null, "w:t")
        s.attribute("http://www.w3.org/XML/1998/namespace", "space", "preserve")
        s.text(run.text)
        s.endTag(null, "w:t")
        s.endTag(null, "w:r")
    }

    private fun writeTable(s: XmlSerializer, table: InlineObject.Table) {
        s.startTag(null, "w:tbl")
        s.startTag(null, "w:tblPr")
        s.startTag(null, "w:tblBorders")
        listOf("top", "left", "bottom", "right", "insideH", "insideV").forEach { side ->
            s.startTag(null, "w:$side")
            s.attribute(null, "w:val", "single"); s.attribute(null, "w:sz", "4"); s.attribute(null, "w:color", "000000")
            s.endTag(null, "w:$side")
        }
        s.endTag(null, "w:tblBorders")
        s.endTag(null, "w:tblPr")
        s.startTag(null, "w:tblGrid")
        table.columnWidthsMm.forEach { w ->
            s.startTag(null, "w:gridCol"); s.attribute(null, "w:w", mmToTwips(w).toString()); s.endTag(null, "w:gridCol")
        }
        s.endTag(null, "w:tblGrid")
        table.rows.forEach { row ->
            s.startTag(null, "w:tr")
            row.forEach { cell ->
                s.startTag(null, "w:tc")
                s.startTag(null, "w:p")
                s.startTag(null, "w:r"); s.startTag(null, "w:t"); s.text(cell); s.endTag(null, "w:t"); s.endTag(null, "w:r")
                s.endTag(null, "w:p")
                s.endTag(null, "w:tc")
            }
            s.endTag(null, "w:tr")
        }
        s.endTag(null, "w:tbl")
    }

    private fun writeImage(s: XmlSerializer, image: InlineObject.Image, relIndex: Int) {
        val cx = mmToEmu(image.widthMm); val cy = mmToEmu(image.heightMm)
        s.startTag(null, "w:r")
        s.startTag(null, "w:drawing")
        s.startTag(null, "wp:inline")
        s.startTag(null, "wp:extent"); s.attribute(null, "cx", cx.toString()); s.attribute(null, "cy", cy.toString()); s.endTag(null, "wp:extent")
        s.startTag(null, "a:graphic")
        s.startTag(null, "a:graphicData"); s.attribute(null, "uri", "http://schemas.openxmlformats.org/drawingml/2006/picture")
        s.startTag(null, "pic:pic")
        s.startTag(null, "pic:blipFill")
        s.startTag(null, "a:blip"); s.attribute(null, "r:embed", "rIdImg$relIndex"); s.endTag(null, "a:blip")
        s.endTag(null, "pic:blipFill")
        s.endTag(null, "pic:pic")
        s.endTag(null, "a:graphicData")
        s.endTag(null, "a:graphic")
        s.endTag(null, "wp:inline")
        s.endTag(null, "w:drawing")
        s.endTag(null, "w:r")
    }

    private fun writeSectionProperties(s: XmlSerializer, pageSetup: PageSetup) {
        val w = if (pageSetup.orientation == Orientation.LANDSCAPE) pageSetup.heightMm else pageSetup.widthMm
        val h = if (pageSetup.orientation == Orientation.LANDSCAPE) pageSetup.widthMm else pageSetup.heightMm
        s.startTag(null, "w:sectPr")
        s.startTag(null, "w:pgSz")
        s.attribute(null, "w:w", mmToTwips(w).toString()); s.attribute(null, "w:h", mmToTwips(h).toString())
        if (pageSetup.orientation == Orientation.LANDSCAPE) s.attribute(null, "w:orient", "landscape")
        s.endTag(null, "w:pgSz")
        s.startTag(null, "w:pgMar")
        s.attribute(null, "w:top", mmToTwips(pageSetup.margin.topMm).toString())
        s.attribute(null, "w:bottom", mmToTwips(pageSetup.margin.bottomMm).toString())
        s.attribute(null, "w:left", mmToTwips(pageSetup.margin.leftMm).toString())
        s.attribute(null, "w:right", mmToTwips(pageSetup.margin.rightMm).toString())
        s.endTag(null, "w:pgMar")
        s.endTag(null, "w:sectPr")
    }

    private fun alignmentToXml(a: Alignment) = when (a) {
        Alignment.CENTER -> "center"; Alignment.RIGHT -> "right"
        Alignment.JUSTIFY -> "both"; Alignment.LEFT -> "left"
    }

    private fun colorToHex(argb: Int): String = String.format("%06X", argb and 0xFFFFFF)
    private fun highlightToName(argb: Int): String = when (argb) {
        0xFFFFFF00.toInt() -> "yellow"; 0xFF00FF00.toInt() -> "green"; 0xFF00FFFF.toInt() -> "cyan"; else -> "yellow"
    }

    private fun mmToTwips(mm: Float): Int = (mm * 56.6929f).toInt()
    private fun ptToTwips(pt: Float): Int = (pt * 20).toInt()
    private fun mmToEmu(mm: Float): Long = (mm * 36000).toLong()

    private fun verifyZip(file: File) {
        ZipFile(file).use { zip ->
            check(zip.getEntry("word/document.xml") != null) { "Verifikasi gagal: document.xml hilang" }
        }
    }

    private fun contentTypesXml(hasImages: Boolean) = """<?xml version="1.0" encoding="UTF-8"?>
        |<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
        |<Default Extension="xml" ContentType="application/xml"/>
        |${if (hasImages) "<Default Extension=\"png\" ContentType=\"image/png\"/>" else ""}
        |<Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
        |<Override PartName="/word/numbering.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.numbering+xml"/>
        |</Types>""".trimMargin()

    private fun relsXml() = """<?xml version="1.0" encoding="UTF-8"?>
        |<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
        |<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
        |</Relationships>""".trimMargin()

    private fun documentRelsXml(images: List<InlineObject.Image>) = buildString {
        append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        append("<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">\n")
        append("<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>\n")
        append("<Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/numbering\" Target=\"numbering.xml\"/>\n")
        images.forEachIndexed { i, _ ->
            append("<Relationship Id=\"rIdImg${i + 1}\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/image\" Target=\"media/image${i + 1}.png\"/>\n")
        }
        append("</Relationships>")
    }

    private fun stylesXml() = """<?xml version="1.0" encoding="UTF-8"?>
        |<w:styles xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"/>""".trimMargin()

    /** numId=1 -> bullet, numId=2 -> numbered — minimal, cukup untuk render dasar Word. */
    private fun numberingXml() = """<?xml version="1.0" encoding="UTF-8"?>
        |<w:numbering xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
        |<w:abstractNum w:abstractNumId="0"><w:lvl w:ilvl="0"><w:numFmt w:val="bullet"/><w:lvlText w:val="&#8226;"/></w:lvl></w:abstractNum>
        |<w:abstractNum w:abstractNumId="1"><w:lvl w:ilvl="0"><w:numFmt w:val="decimal"/><w:lvlText w:val="%1."/></w:lvl></w:abstractNum>
        |<w:num w:numId="1"><w:abstractNumId w:val="0"/></w:num>
        |<w:num w:numId="2"><w:abstractNumId w:val="1"/></w:num>
        |</w:numbering>""".trimMargin()
}
