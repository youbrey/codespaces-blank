package com.docapp.core.model

data class Document(
    val id: String,
    val title: String,
    val pageSetup: PageSetup,
    val sections: List<Section>,
    val styles: StyleSheet = StyleSheet.default(),
    val createdAt: Long,
    val modifiedAt: Long
)

data class PageSetup(
    val size: PaperSize = PaperSize.A4,
    val customWidthMm: Float? = null,
    val customHeightMm: Float? = null,
    val orientation: Orientation = Orientation.PORTRAIT,
    val margin: Margin = Margin.default()
) {
    val widthMm: Float get() = if (size == PaperSize.CUSTOM) (customWidthMm ?: 210f) else size.widthMm
    val heightMm: Float get() = if (size == PaperSize.CUSTOM) (customHeightMm ?: 297f) else size.heightMm
}

enum class PaperSize(val widthMm: Float, val heightMm: Float, val label: String) {
    A3(297f, 420f, "A3 (297 x 420 mm)"),
    A4(210f, 297f, "A4 (210 x 297 mm)"),
    A5(148f, 210f, "A5 (148 x 210 mm)"),
    B5(176f, 250f, "B5 (176 x 250 mm)"),
    LETTER(215.9f, 279.4f, "Letter (215.9 x 279.4 mm)"),
    LEGAL(215.9f, 355.6f, "Legal (215.9 x 355.6 mm)"),
    TABLOID(279.4f, 431.8f, "Tabloid (279.4 x 431.8 mm)"),
    EXECUTIVE(184.1f, 266.7f, "Executive (184.1 x 266.7 mm)"),
    F4(210f, 330f, "F4 / Folio (210 x 330 mm)"),
    CUSTOM(0f, 0f, "Kustom / Custom")
}

enum class Orientation { PORTRAIT, LANDSCAPE }

data class Margin(val topMm: Float, val bottomMm: Float, val leftMm: Float, val rightMm: Float) {
    companion object {
        fun default() = Margin(25.4f, 25.4f, 25.4f, 25.4f)
        fun narrow() = Margin(12.7f, 12.7f, 12.7f, 12.7f)
        fun moderate() = Margin(25.4f, 25.4f, 19.1f, 19.1f)
        fun wide() = Margin(25.4f, 25.4f, 50.8f, 50.8f)
        fun mirrored() = Margin(25.4f, 25.4f, 25.4f, 19.1f)
        fun office() = Margin(20f, 20f, 20f, 20f)
    }
}

data class Section(
    val paragraphs: List<Paragraph>,
    val header: HeaderFooter? = null,
    val footer: HeaderFooter? = null
)

data class HeaderFooter(val runs: List<TextRun>)

data class Paragraph(
    val id: String,
    val runs: List<TextRun>,
    val alignment: Alignment = Alignment.LEFT,
    val lineSpacing: LineSpacing = LineSpacing.Single,
    val spacingBeforePt: Float = 0f,
    val spacingAfterPt: Float = 8f,
    val indent: Indent = Indent(0f, 0f, 0f),
    val listInfo: ListInfo? = null,
    val inlineObjects: List<InlineObject> = emptyList()
)

enum class Alignment { LEFT, CENTER, RIGHT, JUSTIFY }

sealed class LineSpacing {
    data object Single : LineSpacing()
    data object OnePointFive : LineSpacing()
    data object Double : LineSpacing()
    data class Exact(val pt: Float) : LineSpacing()
    data class Multiple(val factor: Float) : LineSpacing()
}

data class Indent(val leftMm: Float, val rightMm: Float, val firstLineMm: Float)

data class ListInfo(val level: Int, val isOrdered: Boolean)

data class TextRun(
    val text: String,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: UnderlineStyle = UnderlineStyle.NONE,
    val strikethrough: Boolean = false,
    val fontFamily: String = "Liberation Sans",
    val fontSizePt: Float = 11f,
    val colorArgb: Int = 0xFF000000.toInt(),
    val highlightArgb: Int? = null
)

enum class UnderlineStyle { NONE, SINGLE, DOUBLE }

sealed class InlineObject {
    data class Image(val localPath: String, val widthMm: Float, val heightMm: Float) : InlineObject()
    data class Table(val rows: List<List<String>>, val columnWidthsMm: List<Float>) : InlineObject()
    data object PageBreak : InlineObject()
    data class Hyperlink(val text: String, val url: String) : InlineObject()
}

data class StyleSheet(val namedStyles: Map<String, TextRun>) {
    companion object { fun default() = StyleSheet(emptyMap()) }
}
