# DocEditor — Panduan Setup & Produksi

## CATATAN PENTING - LEWATKAN PEMBUATAN PEMBELIAN DALAM APLIKASI, BUAT SEMUA FITUR SECARA GRATIS ##

## JANGAN PERNAH REWRITE ATAU MENGKONVERSI SOURCE CODE MENJADI PEMROGRAMAN WEB TERMASUK REACT DAN VITE!!!! ###

## SEMUA HAL YANG BERHUBUNGAN DENGAN PEMELIAN DALAM APLIKASI DAN SERVER FIREBASE TIDAK PERLU DILAKUKAN SEKARANG INI ##

## 1. Buka Project

1. Install Android Studio (Koala/2024.1 atau lebih baru), Android SDK 34, NDK, CMake.
2. `File > Open` → pilih folder `DocEditor/`.
3. Biarkan Gradle sync (module sudah didefinisikan di `settings.gradle.kts`).

## 2. Yang Harus Kamu Lengkapi Sebelum Build Berhasil

| Item | Lokasi | Kenapa |
|---|---|---|
| Ikon app | `app/src/main/res/mipmap/` | Placeholder belum ada |
| Tema Compose | `app/src/main/res/values/themes.xml` | Belum dibuat, tambahkan `Theme.DocApp` |
| Hash signature APK | `core/security/src/main/cpp/native-lib.cpp` (`EXPECTED_SIG`) | Isi setelah generate keystore produksi |
| Package Firebase project ID | `data/billing/.../LicenseApi.kt` (`VERIFY_ENDPOINT`) | Setelah deploy Cloud Functions |
| Service account Firebase | `server/functions/` | Untuk akses Play Developer API |

## 3. Setup Server Verifikasi (Firebase)

```bash
npm install -g firebase-tools
firebase login
cd server
firebase init functions   # pilih project, TypeScript, jangan overwrite src/index.ts
cd functions && npm install
```

**Aktifkan Play Developer API:**
1. Google Cloud Console → project sama dengan Firebase → aktifkan "Google Play Android Developer API".
2. Play Console → Setup → API access → link project, buat service account, beri akses "View financial data".
3. Download JSON key service account, set sebagai credential default Cloud Functions (atau via `GOOGLE_APPLICATION_CREDENTIALS`).

**Deploy:**
```bash
npm run deploy
```
Setelah deploy, salin URL endpoint ke `LicenseApi.kt`.

## 4. Setup Play Console

1. Buat app baru, isi `applicationId = com.docapp.editor` (samakan dengan `app/build.gradle.kts`).
2. Monetization → Products:
   - In-app product `pro_bundle_onetime` — one-time, $1.99
   - Subscription `essential_monthly` — Rp 3.000/bulan
3. Generate upload key + app signing key (Play App Signing direkomendasikan).
4. Ambil SHA-256 signature dari Play Console → App integrity, isi ke `EXPECTED_SIG` di native-lib.cpp.

## 5. Build Release

```bash
./gradlew assembleRelease
# atau bundle untuk Play Store:
./gradlew bundleRelease
```

Output: `app/build/outputs/bundle/release/app-release.aab`

## 6. Testing Checklist Sebelum Submit

- [ ] Unit test `core:model`, `core:command`, `core:layout-engine` (`./gradlew test`)
- [ ] Test billing flow pakai [License Testing](https://developer.android.com/google/play/billing/test) account di Play Console
- [ ] Verifikasi native lib ter-compile untuk semua ABI (`arm64-v8a`, `armeabi-v7a`, `x86_64`)
- [ ] Test alur: beli → verify server → StateVault ter-update → fitur premium terbuka
- [ ] Test offline: matikan internet, pastikan fitur premium tetap aktif (dari cache) dan editor tetap berfungsi penuh
- [ ] Test refund: refund dari Play Console sandbox, pastikan `periodicRevalidate()` mengunci ulang fitur
- [ ] Jalankan `pdftoppm`/render manual untuk cek hasil export PDF & DOCX terbuka benar di Word/LibreOffice

## 7. Yang Masih Perlu Dikembangkan (Belum Lengkap di Kerangka Ini)

- Parsing lengkap `w:sectPr`, `w:numbering.xml` (bullet/numbering) di DocxReader
- Rendering rich-text per-run (bold/italic/warna) di `DocumentCanvas` — saat ini baru render teks polos
- Insert gambar & tabel di UI editor
- Autosave scheduler (WorkManager, tiap N detik idle)
- File browser (SAF) untuk buka/simpan ke folder pilihan user
- UI paywall (`suggest_connectors`-style bottom sheet) untuk ketiga fitur premium
- Cek ejaan offline (Hunspell)

## 8. Struktur Module

```
core/model          — pure Kotlin, model dokumen
core/command         — undo/redo
core/layout-engine   — pagination (StaticLayout)
core/gate            — StateVault, FeatureIds
core/security        — NativeBridge (JNI), LicenseCache, EnvGuard
data/docx-engine     — baca/tulis .docx
data/pdf-export       — export PDF (pakai layout-engine yang sama)
data/local-db         — Room (metadata saja)
data/billing          — Play Billing + verifikasi server
feature/editor        — UI Compose
feature/export         — gate watermark
feature/template        — gate template premium
feature/tools            — gate fitur produktivitas lanjutan
server/functions          — Firebase Cloud Functions (verifikasi)
```
git config --global user.email "youbrey@gmail.com"
git config --global user.name "youbrey"
