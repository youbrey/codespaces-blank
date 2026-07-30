package com.docapp.feature.editor.toolbar

/** Struktur toolbar mengikuti kategori Word: Beranda, Sisipkan, Tata Letak, Tinjau. */
enum class ToolbarCategory(val label: String) {
    HOME("Beranda"),
    INSERT("Sisipkan"),
    DRAW("Gambar"),
    LAYOUT("Tata Letak"),
    REVIEW("Tinjau"),
    VIEW("Tampilan")
}

sealed class ToolbarAction(val label: String, val category: ToolbarCategory, val isPremium: Boolean = false) {
    // Beranda
    data object Bold : ToolbarAction("Bold", ToolbarCategory.HOME)
    data object Italic : ToolbarAction("Italic", ToolbarCategory.HOME)
    data object Underline : ToolbarAction("Underline", ToolbarCategory.HOME)
    data object Strikethrough : ToolbarAction("Strikethrough", ToolbarCategory.HOME)
    data object Highlight : ToolbarAction("Sorot", ToolbarCategory.HOME)
    data object FontColor : ToolbarAction("Warna Font", ToolbarCategory.HOME)
    data object ClearFormat : ToolbarAction("Bersihkan Pemformatan", ToolbarCategory.HOME)

    // Sisipkan
    data object BlankPage : ToolbarAction("Halaman Kosong", ToolbarCategory.INSERT)
    data object Table : ToolbarAction("Tabel", ToolbarCategory.INSERT)
    data object Image : ToolbarAction("Gambar", ToolbarCategory.INSERT)
    data object Shape : ToolbarAction("Bentuk", ToolbarCategory.INSERT)
    data object TextBox : ToolbarAction("Kotak Teks", ToolbarCategory.INSERT)
    data object Link : ToolbarAction("Tautan", ToolbarCategory.INSERT)
    data object Comment : ToolbarAction("Komentar", ToolbarCategory.INSERT)
    data object HeaderFooter : ToolbarAction("Header & Footer", ToolbarCategory.INSERT)
    data object PageNumber : ToolbarAction("Nomor Halaman", ToolbarCategory.INSERT)
    data object Footnote : ToolbarAction("Catatan Kaki", ToolbarCategory.INSERT)
    data object VoiceToText : ToolbarAction("Suara ke Teks", ToolbarCategory.INSERT, isPremium = true)
    data object AiGenerate : ToolbarAction("Tulis dengan AI", ToolbarCategory.INSERT, isPremium = true)

    // Tata Letak
    data object Margin : ToolbarAction("Margin", ToolbarCategory.LAYOUT)
    data object Orientation : ToolbarAction("Orientasi", ToolbarCategory.LAYOUT)
    data object PageSize : ToolbarAction("Ukuran", ToolbarCategory.LAYOUT)
    data object Columns : ToolbarAction("Kolom", ToolbarCategory.LAYOUT)
    data object Break : ToolbarAction("Pemisah", ToolbarCategory.LAYOUT)

    // Tinjau
    data object Spelling : ToolbarAction("Ejaan", ToolbarCategory.REVIEW)
    data object WordCount : ToolbarAction("Hitungan Kata", ToolbarCategory.REVIEW)
    data object NewComment : ToolbarAction("Komentar Baru", ToolbarCategory.REVIEW)
    data object TrackChanges : ToolbarAction("Lacak Perubahan", ToolbarCategory.REVIEW)
    data object GrammarCheck : ToolbarAction("Tata Bahasa Lanjutan", ToolbarCategory.REVIEW, isPremium = true)
    data object Find : ToolbarAction("Temukan", ToolbarCategory.REVIEW)

    // Gambar (Draw)
    data object DrawTouch : ToolbarAction("Gambar dengan Sentuhan", ToolbarCategory.DRAW)
    data object DrawEraser : ToolbarAction("Penghapus Coretan", ToolbarCategory.DRAW)
    data object DrawPen : ToolbarAction("Pena", ToolbarCategory.DRAW)
    data object DrawHighlighter : ToolbarAction("Penyorot", ToolbarCategory.DRAW)

    // Tampilan (View)
    data object MobileView : ToolbarAction("Tampilan Seluler", ToolbarCategory.VIEW)
    data object PrintLayoutView : ToolbarAction("Tata Letak Cetak", ToolbarCategory.VIEW)
    data object ZoomIn : ToolbarAction("Perbesar", ToolbarCategory.VIEW)
    data object ZoomOut : ToolbarAction("Perkecil", ToolbarCategory.VIEW)

    companion object {
        fun forCategory(category: ToolbarCategory): List<ToolbarAction> = listOf(
            Bold, Italic, Underline, Strikethrough, Highlight, FontColor, ClearFormat,
            BlankPage, Table, Image, Shape, TextBox, Link, Comment, HeaderFooter, PageNumber, Footnote, VoiceToText, AiGenerate,
            Margin, Orientation, PageSize, Columns, Break,
            Spelling, WordCount, NewComment, TrackChanges, GrammarCheck, Find,
            DrawTouch, DrawEraser, DrawPen, DrawHighlighter,
            MobileView, PrintLayoutView, ZoomIn, ZoomOut
        ).filter { it.category == category }
    }
}
