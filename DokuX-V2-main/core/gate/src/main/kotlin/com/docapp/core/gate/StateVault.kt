package com.docapp.core.gate

/**
 * Penyimpan status fitur dalam bentuk bitmask, bukan boolean per-fitur.
 * Nilai disinkron dari NativeBridge setelah verifikasi server (lihat data:billing).
 * Nama kelas & anggota sengaja generik — hindari nama seperti isPro/isPurchased
 * yang jadi target pencarian pertama saat reverse engineering.
 */
object StateVault {
    @Volatile private var f: Long = 0L

    fun sync(mask: Long) { f = mask }
    fun has(bit: Int): Boolean = (f and (1L shl bit)) != 0L
    fun current(): Long = f
}

/** ID bit acak, tidak berurutan, tanpa makna langsung dari namanya. */
object FeatureIds {
    const val K7 = 3 // fitur produktivitas lanjutan (regex, compare, batch)
    const val M2 = 9 // template premium
    const val R4 = 5 // export tanpa watermark (bundle atau subscription)
}
