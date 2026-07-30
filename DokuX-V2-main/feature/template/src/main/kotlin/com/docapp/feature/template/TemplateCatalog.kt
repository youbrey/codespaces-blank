package com.docapp.feature.template

import com.docapp.core.model.*

/** Isi konten default untuk tiap template (dipakai TemplatePath.listAvailable()). */
object TemplateCatalog {

    fun all(): List<TemplateItem> = listOf(
        TemplateItem("blank", "Dokumen Kosong", tier = 0) { blank() },
        TemplateItem("surat_resmi", "Surat Resmi", tier = 0) { suratResmi() },
        TemplateItem("cv", "CV / Riwayat Hidup", tier = 1) { cv() },
        TemplateItem("undangan", "Undangan Acara", tier = 1) { undangan() },
        TemplateItem("laporan", "Laporan Kegiatan", tier = 1) { laporan() },
    )

    private fun para(text: String, bold: Boolean = false, align: Alignment = Alignment.LEFT, size: Float = 11f) =
        Paragraph(id = "p_${System.nanoTime()}", runs = listOf(TextRun(text = text, bold = bold, fontSizePt = size)), alignment = align)

    private fun doc(title: String, paragraphs: List<Paragraph>): Document {
        val now = System.currentTimeMillis()
        return Document(id = "tpl_$now", title = title, pageSetup = PageSetup(),
            sections = listOf(Section(paragraphs = paragraphs)), createdAt = now, modifiedAt = now)
    }

    private fun blank() = doc("Dokumen Baru", listOf(para("")))

    private fun suratResmi() = doc("Surat Resmi", listOf(
        para("Nomor: .../.../2026"), para("Lampiran: -"), para("Perihal: ..."), para(""),
        para("Kepada Yth.", bold = true), para("..."), para("di tempat"), para(""),
        para("Dengan hormat,"), para("..."), para(""),
        para("Hormat kami,"), para(""), para(""), para("(Nama & Jabatan)")
    ))

    private fun cv() = doc("CV - Riwayat Hidup", listOf(
        para("NAMA LENGKAP", bold = true, align = Alignment.CENTER, size = 16f),
        para("Email · Telepon · Kota", align = Alignment.CENTER),
        para(""), para("RINGKASAN", bold = true), para("..."),
        para(""), para("PENGALAMAN KERJA", bold = true), para("Posisi — Perusahaan (Tahun-Tahun)"), para("• ..."),
        para(""), para("PENDIDIKAN", bold = true), para("Institusi — Jurusan (Tahun)"),
        para(""), para("KETERAMPILAN", bold = true), para("• ...")
    ))

    private fun undangan() = doc("Undangan Acara", listOf(
        para("UNDANGAN", bold = true, align = Alignment.CENTER, size = 18f), para(""),
        para("Dengan hormat mengundang Bapak/Ibu/Saudara/i untuk hadir pada:", align = Alignment.CENTER),
        para("Acara: ...", align = Alignment.CENTER), para("Hari/Tanggal: ...", align = Alignment.CENTER),
        para("Waktu: ...", align = Alignment.CENTER), para("Tempat: ...", align = Alignment.CENTER),
        para(""), para("Atas kehadirannya kami ucapkan terima kasih.", align = Alignment.CENTER)
    ))

    private fun laporan() = doc("Laporan Kegiatan", listOf(
        para("LAPORAN KEGIATAN", bold = true, align = Alignment.CENTER, size = 16f), para(""),
        para("1. Latar Belakang", bold = true), para("..."),
        para("2. Tujuan", bold = true), para("..."),
        para("3. Pelaksanaan", bold = true), para("..."),
        para("4. Hasil & Evaluasi", bold = true), para("..."),
        para("5. Penutup", bold = true), para("...")
    ))
}
