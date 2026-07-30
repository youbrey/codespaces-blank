package com.docapp.editor

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import com.docapp.core.model.*
import com.docapp.data.db.AppDatabase
import com.docapp.data.db.DocumentEntity
import com.docapp.data.filesystem.SafFileRepository
import com.docapp.data.pdf.PdfExportEngine
import com.docapp.feature.editor.AutosaveScheduler
import com.docapp.feature.editor.EditorScreen
import com.docapp.feature.editor.EditorViewModel
import com.docapp.feature.filebrowser.FileBrowserScreen
import com.docapp.feature.ai.GoogleAuthGate
import com.docapp.feature.ai.GeminiDocumentGenerator
import com.docapp.feature.ai.AiGenerateResult
import com.docapp.feature.speech.MicTranscriber
import com.docapp.feature.speech.TranscriptionResult
import dagger.hilt.android.AndroidEntryPoint
import android.widget.Toast
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File

private const val DOCX_MIME = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"

private sealed class Screen {
    data object Browser : Screen()
    data object Editor : Screen()
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: EditorViewModel by viewModels {
        object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                EditorViewModel(blankDocument()) as T
        }
    }

    private val pdfExportEngine = PdfExportEngine()
    private lateinit var fileRepository: SafFileRepository
    private lateinit var db: AppDatabase
    private var saveUri: Uri? = null

    // Ganti dengan Web Client ID dari Google Cloud Console (OAuth 2.0 Client, tipe Web)
    private val authGate by lazy { GoogleAuthGate(this, webClientId = "YOUR_WEB_CLIENT_ID.apps.googleusercontent.com") }
    private val aiGenerator = GeminiDocumentGenerator(proxyEndpoint = "https://asia-southeast1-docapp-prod.cloudfunctions.net")
    private val micTranscriber by lazy { MicTranscriber(applicationContext) }

    private val openDocumentLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { openDocxFromUri(it) }
    }

    private val createDocumentLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument(DOCX_MIME)) { uri ->
        uri?.let { saveUri = it; saveCurrentToUri(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fileRepository = SafFileRepository(applicationContext)
        db = AppDatabase.instance(applicationContext)

        // Seed sample documents on first launch if DB is empty
        lifecycleScope.launch {
            if (db.documentDao().getAllOnce().isEmpty()) {
                val now = System.currentTimeMillis()
                val doc1Title = "Proposal_Pengembangan_Sistem_Offline.docx"
                val doc1File = File(getExternalFilesDir(null), doc1Title)
                val doc1 = Document(
                    id = "doc_sample_1",
                    title = doc1Title,
                    pageSetup = PageSetup(),
                    sections = listOf(Section(paragraphs = listOf(
                        Paragraph(id = "p1", runs = listOf(TextRun(text = "Proposal Pengembangan Sistem Offline")))
                    ))),
                    createdAt = now - 3600000,
                    modifiedAt = now - 3600000
                )
                fileRepository.saveInternal(doc1, doc1Title)
                db.documentDao().upsert(
                    DocumentEntity(
                        id = "doc_sample_1",
                        title = doc1Title,
                        filePath = doc1File.absolutePath,
                        createdAt = now - 3600000,
                        modifiedAt = now - 3600000,
                        sizeBytes = 42496, // 41.5 KB
                        isFavorite = false
                    )
                )

                val doc2Title = "Laporan_Keuangan_Proyek_2026.docx"
                val doc2File = File(getExternalFilesDir(null), doc2Title)
                val doc2 = Document(
                    id = "doc_sample_2",
                    title = doc2Title,
                    pageSetup = PageSetup(),
                    sections = listOf(Section(paragraphs = listOf(
                        Paragraph(id = "p2", runs = listOf(TextRun(text = "Laporan Keuangan Proyek 2026")))
                    ))),
                    createdAt = now - 86400000,
                    modifiedAt = now - 86400000
                )
                fileRepository.saveInternal(doc2, doc2Title)
                db.documentDao().upsert(
                    DocumentEntity(
                        id = "doc_sample_2",
                        title = doc2Title,
                        filePath = doc2File.absolutePath,
                        createdAt = now - 86400000,
                        modifiedAt = now - 86400000,
                        sizeBytes = 31232, // 30.5 KB
                        isFavorite = false
                    )
                )
            }
        }

        // Autosave: dipicu ViewModel setelah 15 detik idle -> tulis file lokal -> jadwalkan WorkManager
        // untuk update index Room + snapshot revisi (tetap selesai walau app di-kill setelah enqueue).
        viewModel.onAutosave = { doc ->
            lifecycleScope.launch {
                val file = fileRepository.saveInternal(doc, doc.title)
                AutosaveScheduler.schedule(applicationContext, doc.id, doc.title, file.path)
            }
        }

        // intent VIEW .docx dari app lain
        intent?.data?.let { openDocxFromUri(it) }

        setContent {
            var screen by remember { mutableStateOf<Screen>(Screen.Browser) }
            val recentDocs by db.documentDao().observeAll().collectAsState(initial = emptyList())

            MaterialTheme {
                Surface {
                    when (screen) {
                        Screen.Browser -> FileBrowserScreen(
                            recentDocuments = recentDocs,
                            onOpenRecent = { entity ->
                                openDocxFromFile(File(entity.filePath)); screen = Screen.Editor
                            },
                            onOpenFromDevice = { openDocumentLauncher.launch(arrayOf(DOCX_MIME)) },
                            onCreateNew = {
                                viewModel.loadDocument(blankDocument())
                                saveUri = null
                                screen = Screen.Editor
                            },
                            onDelete = { entity ->
                                lifecycleScope.launch { db.documentDao().delete(entity.id) }
                            },
                            onToggleFavorite = { entity ->
                                lifecycleScope.launch { db.documentDao().setFavorite(entity.id, !entity.isFavorite) }
                            }
                        )
                        Screen.Editor -> {
                            val account by authGate.account.collectAsState()
                            EditorScreen(
                                viewModel = viewModel,
                                onExportPdf = { exportCurrentToPdf() },
                                onExportDocx = { exportCurrentToDocx() },
                                onOpenFile = { screen = Screen.Browser },
                                onStartVoiceInput = { startVoiceInput() },
                                onAiGenerate = { prompt -> generateWithAi(prompt) },
                                isAiAvailable = account != null,
                                onRequireGoogleSignIn = { requestGoogleSignIn() },
                                isFeatureUnlocked = { action ->
                                    // TESTING MODE: semua fitur premium (K7) dibuka sementara.
                                    // Sebelum rilis produksi, ganti baris ini kembali ke:
                                    // !action.isPremium || com.docapp.core.security.NativeBridge.chk(com.docapp.core.gate.FeatureIds.K7)
                                    true
                                },
                                onPurchaseBundle = { /* purchaseRepository.launchPurchase(this, ProductIds.PRO_BUNDLE) */ }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun openDocxFromUri(uri: Uri) = lifecycleScope.launch {
        val doc = fileRepository.openFromUri(uri)
        viewModel.loadDocument(doc)
        saveUri = uri
    }

    private fun openDocxFromFile(file: File) {
        viewModel.loadDocument(fileRepository.openInternal(file))
        saveUri = null
    }

    private fun exportCurrentToPdf() {
        val output = File(getExternalFilesDir(null), "${viewModel.currentSnapshot().title}.pdf")
        pdfExportEngine.export(viewModel.currentSnapshot(), output)
    }

    private fun exportCurrentToDocx() {
        val uri = saveUri
        if (uri != null) {
            lifecycleScope.launch { fileRepository.saveToUri(viewModel.currentSnapshot(), uri); viewModel.markSaved() }
        } else {
            createDocumentLauncher.launch("${viewModel.currentSnapshot().title}.docx")
        }
    }

    private fun saveCurrentToUri(uri: Uri) = lifecycleScope.launch {
        fileRepository.saveToUri(viewModel.currentSnapshot(), uri)
        viewModel.markSaved()
    }

    /** Login akun Google — wajib untuk akses fitur AI. Toast jika belum login. */
    private fun requestGoogleSignIn() = lifecycleScope.launch {
        val result = authGate.signIn()
        if (result.isFailure) {
            Toast.makeText(this@MainActivity, "Login Google gagal. Silakan coba lagi.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this@MainActivity, "Berhasil login. Fitur AI aktif.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun generateWithAi(prompt: String) = lifecycleScope.launch {
        val account = authGate.account.value
        if (account == null) {
            Toast.makeText(this@MainActivity, "Silakan login akun Google untuk menggunakan fitur AI", Toast.LENGTH_LONG).show()
            requestGoogleSignIn()
            return@launch
        }
        Toast.makeText(this@MainActivity, "Membuat dokumen dengan AI...", Toast.LENGTH_SHORT).show()
        when (val result = aiGenerator.generate(prompt, account.idToken)) {
            is AiGenerateResult.Success -> viewModel.loadDocument(result.document)
            is AiGenerateResult.Error -> Toast.makeText(this@MainActivity, result.message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun startVoiceInput() = lifecycleScope.launch {
        micTranscriber.listen().collectLatest { result ->
            when (result) {
                is TranscriptionResult.Success -> viewModel.insertTranscribedText(viewModel.focusedParagraph.value, result.text)
                is TranscriptionResult.Error -> Toast.makeText(this@MainActivity, result.message, Toast.LENGTH_SHORT).show()
                is TranscriptionResult.Partial -> Unit
            }
        }
    }

    private fun blankDocument(): Document {
        val now = System.currentTimeMillis()
        return Document(
            id = "doc_$now",
            title = "Dokumen Baru",
            pageSetup = PageSetup(),
            sections = listOf(Section(paragraphs = listOf(
                Paragraph(id = "p0", runs = listOf(TextRun(text = "")))
            ))),
            createdAt = now,
            modifiedAt = now
        )
    }
}
