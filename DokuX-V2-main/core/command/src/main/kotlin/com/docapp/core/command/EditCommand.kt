package com.docapp.core.command

import com.docapp.core.model.*

interface EditCommand {
    fun execute(doc: Document): Document
    fun undo(doc: Document): Document
}

private fun Document.mutateSection(sIdx: Int, f: (Section) -> Section): Document {
    val sections = sections.toMutableList()
    sections[sIdx] = f(sections[sIdx])
    return copy(sections = sections, modifiedAt = System.currentTimeMillis())
}

private fun Section.mutateParagraphs(f: (MutableList<Paragraph>) -> Unit): Section {
    val list = paragraphs.toMutableList(); f(list); return copy(paragraphs = list)
}

/** Ganti isi satu paragraf (insert/delete teks via TextField). */
class EditParagraphCommand(
    private val sectionIndex: Int,
    private val paragraphIndex: Int,
    private val newRuns: List<TextRun>
) : EditCommand {
    private lateinit var previousRuns: List<TextRun>

    override fun execute(doc: Document): Document {
        previousRuns = doc.sections[sectionIndex].paragraphs[paragraphIndex].runs
        return doc.mutateSection(sectionIndex) { s -> s.mutateParagraphs { it[paragraphIndex] = it[paragraphIndex].copy(runs = newRuns) } }
    }

    override fun undo(doc: Document): Document =
        doc.mutateSection(sectionIndex) { s -> s.mutateParagraphs { it[paragraphIndex] = it[paragraphIndex].copy(runs = previousRuns) } }
}

/** Terapkan format (bold/italic/dll) ke satu run tertentu. */
class ApplyFormatCommand(
    private val sectionIndex: Int,
    private val paragraphIndex: Int,
    private val runIndex: Int,
    private val formatChange: (TextRun) -> TextRun
) : EditCommand {
    private lateinit var previousRun: TextRun

    override fun execute(doc: Document): Document {
        previousRun = doc.sections[sectionIndex].paragraphs[paragraphIndex].runs[runIndex]
        return replace(doc, formatChange(previousRun))
    }
    override fun undo(doc: Document): Document = replace(doc, previousRun)

    private fun replace(doc: Document, run: TextRun) = doc.mutateSection(sectionIndex) { s ->
        s.mutateParagraphs { list ->
            val p = list[paragraphIndex]
            val runs = p.runs.toMutableList(); runs[runIndex] = run
            list[paragraphIndex] = p.copy(runs = runs)
        }
    }
}

/** Toggle format ke SEMUA run dalam satu paragraf (dipakai toolbar saat tidak ada seleksi karakter granular). */
class ToggleParagraphFormatCommand(
    private val sectionIndex: Int,
    private val paragraphIndex: Int,
    private val toggle: (TextRun, Boolean) -> TextRun,
    private val isActive: (TextRun) -> Boolean
) : EditCommand {
    private lateinit var previousRuns: List<TextRun>

    override fun execute(doc: Document): Document {
        val runs = doc.sections[sectionIndex].paragraphs[paragraphIndex].runs
        previousRuns = runs
        val target = !(runs.isNotEmpty() && runs.all(isActive))
        val newRuns = runs.map { toggle(it, target) }
        return replace(doc, newRuns)
    }
    override fun undo(doc: Document): Document = replace(doc, previousRuns)

    private fun replace(doc: Document, runs: List<TextRun>) = doc.mutateSection(sectionIndex) { s ->
        s.mutateParagraphs { it[paragraphIndex] = it[paragraphIndex].copy(runs = runs) }
    }
}

/** Ganti alignment/lineSpacing/indent paragraf (layout-level, bukan karakter). */
class ApplyParagraphStyleCommand(
    private val sectionIndex: Int,
    private val paragraphIndex: Int,
    private val transform: (Paragraph) -> Paragraph
) : EditCommand {
    private lateinit var previous: Paragraph
    override fun execute(doc: Document): Document {
        previous = doc.sections[sectionIndex].paragraphs[paragraphIndex]
        return replace(doc, transform(previous))
    }
    override fun undo(doc: Document): Document = replace(doc, previous)
    private fun replace(doc: Document, p: Paragraph) = doc.mutateSection(sectionIndex) { s ->
        s.mutateParagraphs { it[paragraphIndex] = p }
    }
}

/** Sisip paragraf baru setelah index tertentu (Enter key / insert object). */
class InsertParagraphCommand(
    private val sectionIndex: Int,
    private val afterIndex: Int,
    private val newParagraph: Paragraph
) : EditCommand {
    override fun execute(doc: Document): Document = doc.mutateSection(sectionIndex) { s ->
        s.mutateParagraphs { it.add(afterIndex + 1, newParagraph) }
    }
    override fun undo(doc: Document): Document = doc.mutateSection(sectionIndex) { s ->
        s.mutateParagraphs { it.removeAt(afterIndex + 1) }
    }
}

/** Hapus paragraf (Backspace di awal paragraf kosong / merge). */
class RemoveParagraphCommand(
    private val sectionIndex: Int,
    private val paragraphIndex: Int
) : EditCommand {
    private lateinit var removed: Paragraph
    override fun execute(doc: Document): Document = doc.mutateSection(sectionIndex) { s ->
        s.mutateParagraphs { removed = it.removeAt(paragraphIndex) }
    }
    override fun undo(doc: Document): Document = doc.mutateSection(sectionIndex) { s ->
        s.mutateParagraphs { it.add(paragraphIndex, removed) }
    }
}

/** Sisip objek inline (gambar/tabel/page-break) ke paragraf tertentu. */
class InsertInlineObjectCommand(
    private val sectionIndex: Int,
    private val paragraphIndex: Int,
    private val obj: InlineObject
) : EditCommand {
    override fun execute(doc: Document): Document = doc.mutateSection(sectionIndex) { s ->
        s.mutateParagraphs { list ->
            val p = list[paragraphIndex]
            list[paragraphIndex] = p.copy(inlineObjects = p.inlineObjects + obj)
        }
    }
    override fun undo(doc: Document): Document = doc.mutateSection(sectionIndex) { s ->
        s.mutateParagraphs { list ->
            val p = list[paragraphIndex]
            list[paragraphIndex] = p.copy(inlineObjects = p.inlineObjects.dropLast(1))
        }
    }
}

/** Ubah page setup (ukuran kertas/orientasi/margin) — berlaku dokumen penuh. */
class SetPageSetupCommand(private val newSetup: PageSetup) : EditCommand {
    private lateinit var previous: PageSetup
    override fun execute(doc: Document): Document {
        previous = doc.pageSetup
        return doc.copy(pageSetup = newSetup, modifiedAt = System.currentTimeMillis())
    }
    override fun undo(doc: Document): Document = doc.copy(pageSetup = previous)
}

/** Find & Replace (regex) di seluruh dokumen — dipakai feature:tools. */
class FindReplaceCommand(
    private val pattern: Regex,
    private val replacement: String
) : EditCommand {
    private lateinit var previousSections: List<Section>
    override fun execute(doc: Document): Document {
        previousSections = doc.sections
        val newSections = doc.sections.map { section ->
            section.copy(paragraphs = section.paragraphs.map { p ->
                p.copy(runs = p.runs.map { it.copy(text = pattern.replace(it.text, replacement)) })
            })
        }
        return doc.copy(sections = newSections, modifiedAt = System.currentTimeMillis())
    }
    override fun undo(doc: Document): Document = doc.copy(sections = previousSections)
}

/**
 * Stack undo/redo dengan batas kedalaman agar memori tidak membengkak
 * pada dokumen yang diedit lama (ratusan operasi).
 */
class CommandStack(private val maxDepth: Int = 100) {
    private val undoStack = ArrayDeque<EditCommand>()
    private val redoStack = ArrayDeque<EditCommand>()

    fun run(cmd: EditCommand, doc: Document): Document {
        val result = cmd.execute(doc)
        undoStack.addLast(cmd)
        if (undoStack.size > maxDepth) undoStack.removeFirst()
        redoStack.clear()
        return result
    }

    fun undo(doc: Document): Document? {
        val cmd = undoStack.removeLastOrNull() ?: return null
        redoStack.addLast(cmd)
        return cmd.undo(doc)
    }

    fun redo(doc: Document): Document? {
        val cmd = redoStack.removeLastOrNull() ?: return null
        undoStack.addLast(cmd)
        return cmd.execute(doc)
    }

    fun canUndo() = undoStack.isNotEmpty()
    fun canRedo() = redoStack.isNotEmpty()
}
