package com.docapp.feature.tools

import com.docapp.core.model.Document

data class DiffLine(val text: String, val type: DiffType)
enum class DiffType { SAME, ADDED, REMOVED }

data class MatchResult(val paragraphIndex: Int, val snippet: String)

/** Logika murni fitur produktivitas lanjutan — gate akses tetap di ToolsPath (StateVault/NativeBridge). */
object ToolsEngine {

    fun findAll(doc: Document, query: String, regexMode: Boolean, ignoreCase: Boolean = true): List<MatchResult> {
        if (query.isEmpty()) return emptyList()
        val pattern = if (regexMode) Regex(query, if (ignoreCase) setOf(RegexOption.IGNORE_CASE) else emptySet())
        else Regex(Regex.escape(query), if (ignoreCase) setOf(RegexOption.IGNORE_CASE) else emptySet())

        return doc.sections.flatMap { it.paragraphs }.mapIndexedNotNull { idx, p ->
            val text = p.runs.joinToString("") { it.text }
            if (pattern.containsMatchIn(text)) MatchResult(idx, text.take(80)) else null
        }
    }

    fun replaceCount(doc: Document, query: String, regexMode: Boolean): Int {
        if (query.isEmpty()) return 0
        val pattern = if (regexMode) Regex(query) else Regex(Regex.escape(query))
        return doc.sections.flatMap { it.paragraphs }.sumOf { p ->
            p.runs.sumOf { pattern.findAll(it.text).count() }
        }
    }

    /** Diff sederhana paragraf-per-paragraf (bukan LCS penuh) — cukup untuk perbandingan revisi cepat. */
    fun compareDocuments(a: Document, b: Document): List<DiffLine> {
        val linesA = a.sections.flatMap { it.paragraphs }.map { p -> p.runs.joinToString("") { it.text } }
        val linesB = b.sections.flatMap { it.paragraphs }.map { p -> p.runs.joinToString("") { it.text } }
        val result = mutableListOf<DiffLine>()
        val max = maxOf(linesA.size, linesB.size)
        for (i in 0 until max) {
            val la = linesA.getOrNull(i); val lb = linesB.getOrNull(i)
            when {
                la == null && lb != null -> result += DiffLine(lb, DiffType.ADDED)
                la != null && lb == null -> result += DiffLine(la, DiffType.REMOVED)
                la == lb -> result += DiffLine(la ?: "", DiffType.SAME)
                else -> { result += DiffLine(la ?: "", DiffType.REMOVED); result += DiffLine(lb ?: "", DiffType.ADDED) }
            }
        }
        return result
    }

    fun wordCount(doc: Document): Pair<Int, Int> {
        val text = doc.sections.flatMap { it.paragraphs }.joinToString(" ") { p -> p.runs.joinToString("") { it.text } }
        val words = text.trim().split(Regex("\\s+")).count { it.isNotBlank() }
        return words to text.length
    }
}
