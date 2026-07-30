package com.docapp.feature.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docapp.core.command.*
import com.docapp.core.model.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** section index selalu 0 untuk MVP (multi-section belum didukung UI). */
private const val SECTION = 0

class EditorViewModel(initialDocument: Document) : ViewModel() {

    private val commandStack = CommandStack()
    private val _document = MutableStateFlow(initialDocument)
    val document: StateFlow<Document> = _document.asStateFlow()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()
    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    private val _focusedParagraph = MutableStateFlow(0)
    val focusedParagraph: StateFlow<Int> = _focusedParagraph.asStateFlow()

    private val _dirty = MutableStateFlow(false)
    val dirty: StateFlow<Boolean> = _dirty.asStateFlow()

    private var autosaveJob: Job? = null
    var onAutosave: ((Document) -> Unit)? = null

    private fun apply(cmd: EditCommand) {
        _document.value = commandStack.run(cmd, _document.value)
        _canUndo.value = commandStack.canUndo()
        _canRedo.value = commandStack.canRedo()
        _dirty.value = true
        scheduleAutosave()
    }

    private fun scheduleAutosave() {
        autosaveJob?.cancel()
        autosaveJob = viewModelScope.launch {
            delay(15_000) // autosave setelah 15 detik idle
            onAutosave?.invoke(_document.value)
            _dirty.value = false
        }
    }

    fun undo() {
        commandStack.undo(_document.value)?.let { _document.value = it }
        _canUndo.value = commandStack.canUndo(); _canRedo.value = commandStack.canRedo()
    }

    fun redo() {
        commandStack.redo(_document.value)?.let { _document.value = it }
        _canUndo.value = commandStack.canUndo(); _canRedo.value = commandStack.canRedo()
    }

    fun setFocusedParagraph(index: Int) { _focusedParagraph.value = index }

    /** Dipanggil dari BasicTextField onValueChange — ganti teks paragraf, pertahankan format run pertama. */
    fun onParagraphTextChanged(paragraphIndex: Int, newText: String) {
        val section = _document.value.sections[SECTION]
        val old = section.paragraphs[paragraphIndex].runs
        val template = old.firstOrNull() ?: TextRun(text = "")
        apply(EditParagraphCommand(SECTION, paragraphIndex, listOf(template.copy(text = newText))))
    }

    fun insertParagraphAfter(index: Int) {
        apply(InsertParagraphCommand(SECTION, index, Paragraph(id = "p_${System.nanoTime()}", runs = listOf(TextRun(text = "")))))
        _focusedParagraph.value = index + 1
    }

    fun removeParagraph(index: Int) {
        if (_document.value.sections[SECTION].paragraphs.size <= 1) return
        apply(RemoveParagraphCommand(SECTION, index))
        _focusedParagraph.value = (index - 1).coerceAtLeast(0)
    }

    fun toggleBold(paragraphIndex: Int) = apply(ToggleParagraphFormatCommand(
        SECTION, paragraphIndex, { r, v -> r.copy(bold = v) }, { it.bold }
    ))

    fun toggleItalic(paragraphIndex: Int) = apply(ToggleParagraphFormatCommand(
        SECTION, paragraphIndex, { r, v -> r.copy(italic = v) }, { it.italic }
    ))

    fun toggleUnderline(paragraphIndex: Int) = apply(ToggleParagraphFormatCommand(
        SECTION, paragraphIndex,
        { r, v -> r.copy(underline = if (v) UnderlineStyle.SINGLE else UnderlineStyle.NONE) },
        { it.underline != UnderlineStyle.NONE }
    ))

    fun toggleStrikethrough(paragraphIndex: Int) = apply(ToggleParagraphFormatCommand(
        SECTION, paragraphIndex, { r, v -> r.copy(strikethrough = v) }, { it.strikethrough }
    ))

    fun toggleHighlight(paragraphIndex: Int, colorArgb: Int = 0xFFFFFF00.toInt()) = apply(ToggleParagraphFormatCommand(
        SECTION, paragraphIndex,
        { r, v -> r.copy(highlightArgb = if (v) colorArgb else null) },
        { it.highlightArgb != null }
    ))

    fun setFontColor(paragraphIndex: Int, colorArgb: Int) = apply(ToggleParagraphFormatCommand(
        SECTION, paragraphIndex, { r, _ -> r.copy(colorArgb = colorArgb) }, { it.colorArgb == colorArgb }
    ))

    fun setFontFamily(paragraphIndex: Int, family: String) = apply(ToggleParagraphFormatCommand(
        SECTION, paragraphIndex, { r, _ -> r.copy(fontFamily = family) }, { it.fontFamily == family }
    ))

    fun setFontSize(paragraphIndex: Int, sizePt: Float) = apply(ToggleParagraphFormatCommand(
        SECTION, paragraphIndex, { r, _ -> r.copy(fontSizePt = sizePt) }, { it.fontSizePt == sizePt }
    ))

    fun clearFormatting(paragraphIndex: Int) {
        apply(ApplyParagraphStyleCommand(SECTION, paragraphIndex) { para ->
            para.copy(runs = para.runs.map { TextRun(text = it.text) })
        })
    }

    fun setAlignment(paragraphIndex: Int, alignment: Alignment) = apply(
        ApplyParagraphStyleCommand(SECTION, paragraphIndex) { it.copy(alignment = alignment) }
    )

    fun setLineSpacing(paragraphIndex: Int, spacing: LineSpacing) = apply(
        ApplyParagraphStyleCommand(SECTION, paragraphIndex) { it.copy(lineSpacing = spacing) }
    )

    fun setListInfo(paragraphIndex: Int, listInfo: ListInfo?) = apply(
        ApplyParagraphStyleCommand(SECTION, paragraphIndex) { it.copy(listInfo = listInfo) }
    )

    fun insertPageBreak(paragraphIndex: Int) =
        apply(InsertInlineObjectCommand(SECTION, paragraphIndex, InlineObject.PageBreak))

    fun insertImage(paragraphIndex: Int, path: String, wMm: Float = 100f, hMm: Float = 75f) =
        apply(InsertInlineObjectCommand(SECTION, paragraphIndex, InlineObject.Image(path, wMm, hMm)))

    fun insertTable(paragraphIndex: Int, rows: Int, cols: Int) {
        val data = List(rows) { List(cols) { "" } }
        val widths = List(cols) { 40f }
        apply(InsertInlineObjectCommand(SECTION, paragraphIndex, InlineObject.Table(data, widths)))
    }

    fun insertHyperlink(paragraphIndex: Int, text: String, url: String) =
        apply(InsertInlineObjectCommand(SECTION, paragraphIndex, InlineObject.Hyperlink(text, url)))

    fun setPageSetup(setup: PageSetup) = apply(SetPageSetupCommand(setup))

    fun setMargin(margin: Margin) = setPageSetup(_document.value.pageSetup.copy(margin = margin))
    fun setPaperSize(size: PaperSize) = setPageSetup(_document.value.pageSetup.copy(size = size))
    fun setCustomPaperSize(widthMm: Float, heightMm: Float) =
        setPageSetup(_document.value.pageSetup.copy(size = PaperSize.CUSTOM, customWidthMm = widthMm, customHeightMm = heightMm))
    fun setOrientation(orientation: Orientation) = setPageSetup(_document.value.pageSetup.copy(orientation = orientation))

    fun setIndentDelta(paragraphIndex: Int, deltaMm: Float) {
        apply(ApplyParagraphStyleCommand(SECTION, paragraphIndex) { p ->
            val currentLeft = p.indent.leftMm
            val newLeft = (currentLeft + deltaMm).coerceAtLeast(0f)
            p.copy(indent = p.indent.copy(leftMm = newLeft))
        })
    }

    fun findReplace(query: String, replacement: String, regexMode: Boolean = false) {
        if (query.isEmpty()) return
        val pattern = if (regexMode) Regex(query) else Regex(Regex.escape(query))
        apply(FindReplaceCommand(pattern, replacement))
    }

    fun wordCount(): Pair<Int, Int> {
        val text = _document.value.sections.flatMap { it.paragraphs }
            .joinToString(" ") { p -> p.runs.joinToString("") { it.text } }
        val words = text.trim().split(Regex("\\s+")).count { it.isNotBlank() }
        return words to text.length
    }

    /** Insert hasil transkripsi suara sebagai paragraf baru setelah index. */
    fun insertTranscribedText(paragraphIndex: Int, text: String) {
        insertParagraphAfter(paragraphIndex)
        onParagraphTextChanged(paragraphIndex + 1, text)
    }

    /** Ganti dokumen aktif sepenuhnya (buka file baru) — reset undo/redo karena histori lama tidak relevan. */
    fun loadDocument(doc: Document) {
        _document.value = doc
        _focusedParagraph.value = 0
        _dirty.value = false
        autosaveJob?.cancel()
    }

    fun currentSnapshot(): Document = _document.value
    fun markSaved() { _dirty.value = false }
}
