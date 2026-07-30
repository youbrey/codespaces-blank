# DOKUMENTASI.md — Status Project DocEditor

## CATATAN PENTING - LEWATKAN PEMBUATAN PEMBELIAN DALAM APLIKASI, BUAT SEMUA FITUR MENJADI GRATIS, JANGAN DULU MENERAPKAN VERIFIKASI LICENSE DAN LAINNYA ATAU TERKAIT FIREBASE, BELUM DIBUTUHKAN SAAT INI ##

## YANG TERPENTING SAAT INI ADALAH AGAR APLIKASI DAPAT BERJALAN DAN SEMUA FITUR DAN FUNGSI DAPAT BERJALAN DENGAN BAIK###

**Update terakhir:** 30 Juli 2026 (lanjutan sesi 2)

## 0. Ringkasan Perubahan Sesi Ini

| Item | Status |
|---|---|
| `handleAction()` EditorScreen | ✅ Selesai — semua ToolbarAction non-reserved terhubung ke command/dialog |
| Input teks nyata (BasicTextField → CommandStack) | ✅ Selesai — ketik, Enter (paragraf baru), Backspace (hapus paragraf), format visual live |
| `EditCommand` set | ✅ Diperluas: ToggleParagraphFormat, ApplyParagraphStyle, InsertParagraph, RemoveParagraph, InsertInlineObject, SetPageSetup, FindReplace |
| `DocxReader` | ✅ Sekarang parse `w:sectPr` (ukuran+margin+orientasi), rPr lengkap (b/i/u/strike/color/highlight/sz), `w:tbl`, deteksi `w:numPr`, `w:drawing` (id relasi gambar) |
| `DocxWriter` | ✅ Tulis rPr lengkap, `w:tbl`, `w:drawing`+media rels dasar, `word/numbering.xml`, orientasi landscape |
| File browser (SAF) | ✅ Baru: `data:filesystem` (SafFileRepository) + `feature:file-browser` (FileBrowserScreen) + wiring `ACTION_OPEN_DOCUMENT`/`ACTION_CREATE_DOCUMENT` di MainActivity |
| Autosave | ✅ Baru: debounce 15s di ViewModel → `SafFileRepository.saveInternal` → `AutosaveScheduler` (WorkManager, guaranteed execution) → update Room + snapshot revisi + prune |
| `feature:tools` | ✅ `ToolsEngine`: find&replace regex, compare dokumen (diff paragraf), word count |
| `feature:template` | ✅ `TemplateCatalog`: 5 template isi nyata (Kosong, Surat Resmi, CV, Undangan, Laporan) |

**Belum tersentuh sesi ini** (masih sesuai bagian 2/4 di bawah): caching incremental LayoutEngine, header/footer editor visual, spelling/Hunspell, resolusi path gambar dari `word/media` saat baca (rIdImg belum di-mapping ke file fisik di DocxReader), multi-kolom, tracked changes, testing.

---

Dokumen ini merangkum apa yang sudah dibangun, apa yang masih kerangka/placeholder, dan apa yang sama sekali belum dikerjakan — supaya siapa pun (termasuk kamu sendiri nanti) bisa lanjut tanpa harus baca ulang seluruh riwayat chat.

---

## 0. Status Testing (Baca Dulu Sebelum Rilis)

Aplikasi saat ini dalam **tahap pengujian** — semua gate pembelian dalam aplikasi (IAP) **dilewati sementara**:

- `MainActivity.kt` → `isFeatureUnlocked` di-hardcode `true` untuk semua fitur (lihat komentar TESTING MODE di kode)
- Fitur AI Generate (`ToolbarAction.AiGenerate`) hanya butuh **login Google**, tidak dicek status pembelian
- Fitur Suara ke Teks (`ToolbarAction.VoiceToText`) juga belum tergate nyata karena `isFeatureUnlocked` selalu `true`

**Rencana kategori premium saat masuk tahap produksi** (bagian dari bundle K7 — fitur produktivitas lanjutan, $1,99 one-time):
| Fitur | Status Target Produksi |
|---|---|
| Suara ke Teks (rekam & transkrip file) | Premium (K7) |
| Tulis dengan AI (Gemini/ChatGPT) | Premium (K7) — sudah ditandai `isPremium = true` di kode, tinggal aktifkan gate |
| Find & replace lanjutan, compare dokumen, batch convert | Premium (K7) |

**Sebelum rilis produksi, wajib:**
1. Kembalikan `isFeatureUnlocked` di `MainActivity.kt` ke pengecekan asli: `!action.isPremium || NativeBridge.chk(FeatureIds.K7)`
2. Tambahkan pengecekan `isFeatureUnlocked`/status pembelian juga ke jalur `AiGenerate` (saat ini AI generate hanya gate login, belum gate pembelian)
3. Uji ulang alur beli → verifikasi server → fitur premium benar-benar terkunci/terbuka sesuai status

---

## 1. Sudah Selesai (Kode Nyata, Bukan Placeholder)

### Struktur Project
- Multi-module Gradle: 14 module (`core:*`, `data:*`, `feature:*`, `app`) + `server/functions`
- Version catalog terpusat (`gradle/libs.versions.toml`)
- Bisa dibuka & Gradle-sync di Android Studio (belum tentu langsung build sukses — lihat bagian 3)

### Model & Logika Inti (`core/`)
| Module | Isi | Status |
|---|---|---|
| `core:model` | `Document`, `Paragraph`, `TextRun`, `PageSetup`, `InlineObject`, dll — pure Kotlin | ✅ Lengkap |
| `core:command` | `CommandStack`, `EditParagraphCommand`, `ApplyFormatCommand` — undo/redo | ✅ Lengkap |
| `core:layout-engine` | Pagination via `StaticLayout`, hitung line-break & page-break | ✅ Fungsional dasar (lihat batasan di bagian 2) |
| `core:gate` | `StateVault` (bitmask), `FeatureIds` (nama non-deskriptif) | ✅ Lengkap |
| `core:security` | `NativeBridge` (JNI), `native-lib.cpp`, `LicenseCache` (terenkripsi), `EnvGuard` (root/debugger/emulator detection) | ✅ Lengkap secara struktur (hash signature masih placeholder — lihat bagian 3) |

### Data Layer (`data/`)
| Module | Isi | Status |
|---|---|---|
| `data:docx-engine` | `DocxReader` (parsing dasar `w:p`, `w:r`, `w:jc`, proteksi zip bomb), `DocxWriter` (atomic write, `w:sectPr`) | ⚠️ Fungsional untuk teks dasar, belum lengkap (lihat bagian 2) |
| `data:pdf-export` | `PdfExportEngine` — render pakai `LayoutEngine` yang sama dengan editor, terhubung ke gate watermark | ✅ Fungsional dasar |
| `data:local-db` | Room: `DocumentEntity`, `RevisionEntity`, DAO, kebijakan retensi revisi (maks 10) | ✅ Lengkap |
| `data:billing` | `PurchaseRepository` (Play Billing lengkap: init, purchase flow, acknowledge, revalidasi periodik, handle refund), `LicenseApi`, `VerifyModels` | ✅ Lengkap secara kode |

### Fitur (`feature/`)
| Module | Isi | Status |
|---|---|---|
| `feature:editor` | `EditorScreen` (Compose), `EditorViewModel`, `CategorizedToolbar` (4 kategori: Beranda/Sisipkan/Tata Letak/Tinjau), `PremiumFeatureSheet` (paywall bottom sheet) | ⚠️ UI ada, tapi `handleAction()` belum di-mapping ke command nyata |
| `feature:export` | `ExportPath` — gate watermark PDF via native check | ✅ Lengkap |
| `feature:template` | `TemplatePath` — gate template premium | ✅ Lengkap (belum ada template isinya — lihat bagian 2) |
| `feature:tools` | `ToolsPath` — gate fitur produktivitas lanjutan | ✅ Kerangka gate lengkap, logika fitur (regex, compare) belum ada |
| `feature:speech-to-text` | `MicTranscriber` (SpeechRecognizer offline-preferred), `AudioDecoder` (MediaCodec native, PCM 16kHz mono), `VoskModelManager` (download-on-demand), `FileTranscriber` (Vosk), `SpeechAccessGate` | ✅ Kode lengkap, **belum pernah dites di device nyata** |

### Server (`server/`)
- Firebase Cloud Functions `verifyPurchase`: validasi via Google Play Developer API, anomaly detection (device binding per token), bit-mask per produk
- `package.json`, `tsconfig.json`, `firebase.json` — siap `npm install` & deploy

### Keamanan
- ProGuard rules (`repackageclasses`, strip logging, keep native methods)
- Distributed feature gating (3 jalur berbeda: `ExportPath`, `TemplatePath`, `ToolsPath` — bukan satu titik cek)
- AndroidManifest tanpa izin storage broad (Scoped Storage + SAF by design)

### Dokumentasi
- `README.md` — panduan setup, build, testing checklist, daftar TODO per bagian

---

## 2. Kerangka Ada, Tapi Belum Lengkap (Perlu Dikembangkan)

Ini bukan bug — ini memang scope yang sengaja belum digarap karena butuh keputusan desain/waktu lebih:

| Bagian | Yang Kurang |
|---|---|
| `ToolbarCategory.DRAW` | Aksi terdaftar (DrawTouch/Pen/Eraser/Highlighter), tapi mode ink-drawing di canvas belum diimplementasi |
| `ToolbarCategory.VIEW` | Aksi terdaftar (MobileView/PrintLayout/Zoom), tapi kontrol zoom canvas & switch layout belum diimplementasi |
| `ToolbarAction.Find` | Terdaftar, sementara masih diarahkan ke dialog Word Count (placeholder) — perlu dialog find & replace nyata |
| `DocxReader` | sectPr/numbering/rPr/tabel ✅; masih kurang: mapping `r:embed` gambar ke file fisik di `word/media` (relasi id sudah ditangkap, isi byte belum diekstrak ke path lokal) |
| `DocxWriter` | rPr/tabel/gambar dasar/numbering ✅; masih kurang: header/footer, style bawaan Word (numbering visual butuh `styles.xml` terhubung) |
| `LayoutEngine` | Masih belum handle `InlineObject` dalam pagination (tabel/gambar tidak dihitung tinggi halamannya), belum caching incremental |
| `EditorScreen` / `DocumentCanvas` | ✅ Selesai — BasicTextField live, format visual (bold/italic/underline/strike/color/highlight), Enter/Backspace terhubung CommandStack |
| `handleAction()` di `EditorScreen.kt` | ✅ Selesai — lihat bagian 0 |
| `feature:template` | ✅ `TemplateCatalog` — 5 template isi nyata |
| `feature:tools` | ✅ `ToolsEngine` — regex find&replace, compare (diff paragraf sederhana bukan LCS), word count |
| File browser | ✅ `data:filesystem` + `feature:file-browser` — SAF open/create/save |
| Autosave | ✅ Debounce ViewModel + `AutosaveWorker` (WorkManager) |

---

## 3. Perlu Aset/Keputusan dari Kamu (Tidak Bisa Di-generate)

| Item | Kenapa Tidak Bisa Dibuat di Sini |
|---|---|
| Hash signature APK (`EXPECTED_SIG` di `native-lib.cpp`) | Baru ada setelah kamu generate keystore produksi sungguhan |
| Ikon app, tema Compose (`Theme.DocApp`) | Aset desain, perlu keputusan visual dari kamu |
| Firebase project ID & deploy sungguhan | Perlu akun Firebase/Google Cloud aktif milik kamu |
| Service account Play Developer API | Perlu akses Play Console kamu |
| Model Vosk (download & test nyata) | File model besar (~50MB), perlu diunduh & dites di device fisik |
| Play Console: produk IAP (`pro_bundle_onetime`, `essential_monthly`) | Perlu didaftarkan manual di Play Console |

---

## 4. Belum Disentuh Sama Sekali

- Testing (unit test maupun instrumented test) — belum ada satu file test pun
- CI/CD pipeline
- Analytics/crash reporting (Firebase Crashlytics, dll)
- Localization (saat ini semua string hardcoded Bahasa Indonesia, belum ada `strings.xml` multi-bahasa)
- Aksesibilitas (`contentDescription` sebagian ada, belum diaudit menyeluruh)
- Cek ejaan offline (Hunspell) — disebut di system design awal, belum ada kode
- UI onboarding / tutorial pertama kali buka app

---

## 5. Urutan Prioritas yang Disarankan (Kalau Lanjut Development)

1. **`handleAction()` di EditorScreen** — tanpa ini, toolbar cuma tampilan, tidak fungsional
2. **Input teks nyata** (TextField/BasicTextField terhubung ke `CommandStack`) — saat ini dokumen cuma bisa ditampilkan, belum bisa diketik
3. **DocxReader/Writer lengkap** — supaya file yang dibuat bisa dibuka balik di Word asli tanpa kehilangan data
4. **File browser (SAF)** — supaya user bisa benar-benar simpan/buka file, bukan cuma satu dokumen blank di memory
5. Baru setelah alur inti jalan: template, tools lanjutan, speech-to-text di-test nyata, billing di-test dengan akun sandbox

---

## 6. Struktur File Referensi Cepat

```
DocEditor/
├── app/                          — entry point, DI, manifest
├── core/
│   ├── model/                    — ✅ Document.kt
│   ├── command/                  — ✅ EditCommand.kt
│   ├── layout-engine/            — ✅ LayoutEngine.kt (dasar)
│   ├── gate/                     — ✅ StateVault.kt
│   └── security/                 — ✅ NativeBridge, LicenseCache, EnvGuard, native-lib.cpp
├── data/
│   ├── docx-engine/               — ⚠️ DocxReader/Writer (dasar)
│   ├── pdf-export/                 — ✅ PdfExportEngine.kt
│   ├── local-db/                    — ✅ AppDatabase.kt
│   └── billing/                      — ✅ PurchaseRepository, LicenseApi
├── feature/
│   ├── editor/                        — ⚠️ EditorScreen, CategorizedToolbar, PremiumFeatureSheet
│   ├── export/                         — ✅ ExportPath.kt
│   ├── template/                        — ⚠️ TemplatePath.kt (gate saja)
│   ├── tools/                             — ⚠️ ToolsPath.kt (gate saja)
│   └── speech-to-text/                     — ✅ MicTranscriber, FileTranscriber, AudioDecoder, VoskModelManager
└── server/functions/                          — ✅ verifyPurchase (Firebase Cloud Functions)
```

Legenda: ✅ selesai secara kode · ⚠️ ada tapi belum lengkap/belum tersambung penuh
