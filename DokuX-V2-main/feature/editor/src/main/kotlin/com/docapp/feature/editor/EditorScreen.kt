package com.docapp.feature.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.docapp.core.model.*
import com.docapp.feature.editor.toolbar.ToolbarAction
import com.docapp.feature.editor.toolbar.ToolbarCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    onExportPdf: () -> Unit,
    onExportDocx: () -> Unit,
    onOpenFile: () -> Unit = {},
    onStartVoiceInput: () -> Unit = {},
    onAiGenerate: (String) -> Unit = {},
    isAiAvailable: Boolean = false,
    onRequireGoogleSignIn: () -> Unit = {},
    isFeatureUnlocked: (ToolbarAction) -> Boolean = { true },
    onPurchaseBundle: () -> Unit = {}
) {
    val document by viewModel.document.collectAsState()
    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()
    val focusedIndex by viewModel.focusedParagraph.collectAsState()

    var activeCategory by remember { mutableStateOf(ToolbarCategory.HOME) }
    var dialog by remember { mutableStateOf<EditorDialog>(EditorDialog.None) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    var fontMenuExpanded by remember { mutableStateOf(false) }
    var fontSizeMenuExpanded by remember { mutableStateOf(false) }
    var paragraphStyleMenuExpanded by remember { mutableStateOf(false) }

    val selectedFontFamily = document.sections.firstOrNull()?.paragraphs?.getOrNull(focusedIndex)
        ?.runs?.firstOrNull()?.fontFamily ?: "Liberation Sans"
    val selectedFontSize = (document.sections.firstOrNull()?.paragraphs?.getOrNull(focusedIndex)
        ?.runs?.firstOrNull()?.fontSizePt ?: 11f).toInt().toString()

    Scaffold(
        containerColor = Color(0xFFF1F3F4),
        topBar = {
            Surface(color = Color.White, tonalElevation = 2.dp, border = BorderStroke(1.dp, Color(0xFFE2E8F0))) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(document.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = viewModel::undo, enabled = canUndo) {
                            Icon(Icons.AutoMirrored.Filled.Undo, "Undo", tint = if (canUndo) Color(0xFF1F2937) else Color(0xFF9CA3AF))
                        }
                        IconButton(onClick = viewModel::redo, enabled = canRedo) {
                            Icon(Icons.AutoMirrored.Filled.Redo, "Redo", tint = if (canRedo) Color(0xFF1F2937) else Color(0xFF9CA3AF))
                        }
                        IconButton(onClick = onExportPdf) { Icon(Icons.Default.PictureAsPdf, "Export PDF", tint = Color(0xFFD32F2F)) }
                        IconButton(onClick = onExportDocx) { Icon(Icons.Default.Save, "Simpan", tint = Color(0xFF1976D2)) }
                        IconButton(onClick = onOpenFile) { Icon(Icons.Default.FolderOpen, "Buka File") }
                    }
                }
            }
        },
        bottomBar = {
            // Toolbar satu baris, otomatis naik di atas keyboard via imePadding.
            Surface(color = Color.White, tonalElevation = 4.dp, modifier = Modifier.imePadding()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    item {
                        Box {
                            ToolbarPill(
                                text = activeCategory.label,
                                icon = Icons.Default.KeyboardArrowDown,
                                onClick = { categoryMenuExpanded = true }
                            )
                            DropdownMenu(expanded = categoryMenuExpanded, onDismissRequest = { categoryMenuExpanded = false }) {
                                ToolbarCategory.entries.forEach { cat ->
                                    DropdownMenuItem(text = { Text(cat.label) }, onClick = { activeCategory = cat; categoryMenuExpanded = false })
                                }
                            }
                        }
                    }
                    item { VerticalDivider(modifier = Modifier.height(28.dp)) }

                    when (activeCategory) {
                        ToolbarCategory.HOME -> {
                            item {
                                Box {
                                    ToolbarPill(text = selectedFontFamily, onClick = { fontMenuExpanded = true })
                                    DropdownMenu(expanded = fontMenuExpanded, onDismissRequest = { fontMenuExpanded = false }) {
                                        listOf("Liberation Sans", "Arial", "Times New Roman", "Courier New", "Georgia").forEach { font ->
                                            DropdownMenuItem(text = { Text(font) }, onClick = { viewModel.setFontFamily(focusedIndex, font); fontMenuExpanded = false })
                                        }
                                    }
                                }
                            }
                            item {
                                Box {
                                    ToolbarPill(text = selectedFontSize, onClick = { fontSizeMenuExpanded = true })
                                    DropdownMenu(expanded = fontSizeMenuExpanded, onDismissRequest = { fontSizeMenuExpanded = false }) {
                                        listOf("9", "10", "11", "12", "14", "16", "18", "20", "24", "28", "36").forEach { s ->
                                            DropdownMenuItem(text = { Text(s) }, onClick = { viewModel.setFontSize(focusedIndex, s.toFloat()); fontSizeMenuExpanded = false })
                                        }
                                    }
                                }
                            }
                            item { ToolbarIconText("B", FontWeight.Bold, onClick = { viewModel.toggleBold(focusedIndex) }) }
                            item { ToolbarIconText("I", FontWeight.Bold, italic = true, onClick = { viewModel.toggleItalic(focusedIndex) }) }
                            item { ToolbarIconText("U", FontWeight.Bold, underline = true, onClick = { viewModel.toggleUnderline(focusedIndex) }) }
                            item { ToolbarIconText("S", FontWeight.Bold, strike = true, onClick = { viewModel.toggleStrikethrough(focusedIndex) }) }
                            item { ToolbarIcon(Icons.Default.FormatColorFill, Color(0xFFD97706)) { viewModel.toggleHighlight(focusedIndex, 0xFFFFFF00.toInt()) } }
                            item { ToolbarIcon(Icons.Default.Palette, Color(0xFF2563EB)) { dialog = EditorDialog.FontColor } }
                            item { ToolbarIcon(Icons.Default.FormatListBulleted) { viewModel.setListInfo(focusedIndex, ListInfo(0, false)) } }
                            item { ToolbarIcon(Icons.Default.FormatListNumbered) { viewModel.setListInfo(focusedIndex, ListInfo(0, true)) } }
                            item { ToolbarIcon(Icons.Default.FormatAlignLeft) { viewModel.setAlignment(focusedIndex, com.docapp.core.model.Alignment.LEFT) } }
                            item { ToolbarIcon(Icons.Default.FormatAlignCenter) { viewModel.setAlignment(focusedIndex, com.docapp.core.model.Alignment.CENTER) } }
                            item { ToolbarIcon(Icons.Default.FormatAlignRight) { viewModel.setAlignment(focusedIndex, com.docapp.core.model.Alignment.RIGHT) } }
                            item { ToolbarIcon(Icons.Default.FormatAlignJustify) { viewModel.setAlignment(focusedIndex, com.docapp.core.model.Alignment.JUSTIFY) } }
                            item { ToolbarIcon(Icons.Default.FormatClear) { viewModel.clearFormatting(focusedIndex) } }
                        }
                        else -> {
                            val actions = ToolbarAction.forCategory(activeCategory)
                            items(actions) { action ->
                                when (action) {
                                    ToolbarAction.VoiceToText -> ToolbarPill(text = action.label, icon = Icons.Default.Mic, tint = Color(0xFF1E8E3E), onClick = onStartVoiceInput)
                                    ToolbarAction.AiGenerate -> ToolbarPill(
                                        text = action.label, icon = Icons.Default.AutoAwesome, tint = Color(0xFF7C3AED),
                                        onClick = { if (isAiAvailable) dialog = EditorDialog.AiPrompt else onRequireGoogleSignIn() }
                                    )
                                    else -> {
                                        val locked = action.isPremium && !isFeatureUnlocked(action)
                                        ToolbarPill(
                                            text = action.label,
                                            icon = if (locked) Icons.Default.Lock else null,
                                            onClick = { if (locked) dialog = EditorDialog.None.also { onPurchaseBundle() } else handleAction(action, viewModel, focusedIndex) { dialog = it } }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier.padding(padding).fillMaxSize().padding(vertical = 12.dp, horizontal = 16.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            DocumentCanvasPaper(
                document = document,
                focusedIndex = focusedIndex,
                onFocusParagraph = viewModel::setFocusedParagraph,
                onTextChanged = viewModel::onParagraphTextChanged,
                onEnterPressed = viewModel::insertParagraphAfter,
                onBackspaceOnEmpty = viewModel::removeParagraph
            )
        }
    }

    EditorDialogHost(dialog, viewModel, focusedIndex, onAiGenerate) { dialog = EditorDialog.None }
}

@Composable
private fun ToolbarPill(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector? = null, tint: Color = Color(0xFF1F2937), onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(18.dp), color = Color(0xFFF8F9FA), border = BorderStroke(1.dp, Color(0xFFD0D7DE)), modifier = Modifier.height(38.dp)) {
        Row(modifier = Modifier.padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = tint, maxLines = 1)
            icon?.let { Icon(it, null, tint = tint, modifier = Modifier.size(14.dp)) }
        }
    }
}

@Composable
private fun ToolbarIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color = Color(0xFF1F2937), onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(10.dp), color = Color(0xFFF3F4F6), modifier = Modifier.size(38.dp)) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun ToolbarIconText(label: String, weight: FontWeight, italic: Boolean = false, underline: Boolean = false, strike: Boolean = false, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(10.dp), color = Color(0xFFF3F4F6), modifier = Modifier.size(38.dp)) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                label, fontWeight = weight, fontSize = 15.sp,
                fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
                textDecoration = when { underline -> TextDecoration.Underline; strike -> TextDecoration.LineThrough; else -> TextDecoration.None }
            )
        }
    }
}

@Composable
private fun DocumentCanvasPaper(
    document: Document,
    focusedIndex: Int,
    onFocusParagraph: (Int) -> Unit,
    onTextChanged: (Int, String) -> Unit,
    onEnterPressed: (Int) -> Unit,
    onBackspaceOnEmpty: (Int) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxSize()
        ) {
            // Document Title Header inside paper sheet
            Text(
                text = document.title.ifBlank { "test.docx" },
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF6B7280)
            )

            Spacer(modifier = Modifier.height(6.dp))
            HorizontalDivider(color = Color(0xFFE5E7EB))
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                document.sections.forEach { section ->
                    itemsIndexed(section.paragraphs) { index, paragraph ->
                        ParagraphEditor(
                            paragraph = paragraph,
                            isFocused = index == focusedIndex,
                            onFocus = { onFocusParagraph(index) },
                            onTextChanged = { onTextChanged(index, it) },
                            onEnterPressed = { onEnterPressed(index) },
                            onBackspaceOnEmpty = { onBackspaceOnEmpty(index) }
                        )
                        paragraph.inlineObjects.forEach { obj -> InlineObjectView(obj) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ParagraphEditor(
    paragraph: Paragraph,
    isFocused: Boolean,
    onFocus: () -> Unit,
    onTextChanged: (String) -> Unit,
    onEnterPressed: () -> Unit,
    onBackspaceOnEmpty: () -> Unit
) {
    val text = paragraph.runs.joinToString("") { it.text }
    var fieldValue by remember(paragraph.id) { mutableStateOf(text) }
    val run = paragraph.runs.firstOrNull()
    val highlightColor = run?.highlightArgb

    BasicTextField(
        value = fieldValue,
        onValueChange = { new ->
            if (new.endsWith("\n")) {
                onTextChanged(fieldValue)
                onEnterPressed()
            } else if (new.isEmpty() && fieldValue.isEmpty()) {
                onBackspaceOnEmpty()
            } else {
                fieldValue = new
                onTextChanged(new)
            }
        },
        textStyle = TextStyle(
            fontWeight = if (run?.bold == true) FontWeight.Bold else FontWeight.Normal,
            fontStyle = if (run?.italic == true) FontStyle.Italic else FontStyle.Normal,
            textDecoration = when {
                run?.underline != UnderlineStyle.NONE && run?.strikethrough == true -> TextDecoration.combine(listOf(TextDecoration.Underline, TextDecoration.LineThrough))
                run?.underline != UnderlineStyle.NONE -> TextDecoration.Underline
                run?.strikethrough == true -> TextDecoration.LineThrough
                else -> TextDecoration.None
            },
            color = Color(run?.colorArgb ?: 0xFF000000.toInt()),
            fontSize = (run?.fontSizePt ?: 11f).sp,
            fontFamily = when (run?.fontFamily) {
                "Arial" -> FontFamily.SansSerif
                "Times New Roman" -> FontFamily.Serif
                "Courier New" -> FontFamily.Monospace
                "Georgia" -> FontFamily.Serif
                else -> FontFamily.Default
            },
            textAlign = when (paragraph.alignment) {
                com.docapp.core.model.Alignment.CENTER -> TextAlign.Center
                com.docapp.core.model.Alignment.RIGHT -> TextAlign.Right
                com.docapp.core.model.Alignment.JUSTIFY -> TextAlign.Justify
                com.docapp.core.model.Alignment.LEFT -> TextAlign.Left
            }
        ),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                if (fieldValue.isEmpty()) {
                    Text(
                        text = "Ketik dokumen Anda di sini...",
                        color = Color(0xFF9CA3AF),
                        fontSize = (run?.fontSizePt ?: 11f).sp,
                        textAlign = when (paragraph.alignment) {
                            com.docapp.core.model.Alignment.CENTER -> TextAlign.Center
                            com.docapp.core.model.Alignment.RIGHT -> TextAlign.Right
                            com.docapp.core.model.Alignment.JUSTIFY -> TextAlign.Justify
                            com.docapp.core.model.Alignment.LEFT -> TextAlign.Left
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                innerTextField()
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .background(if (highlightColor != null) Color(highlightColor) else Color.Transparent)
            .onFocusChanged { if (it.isFocused) onFocus() }
    )
}

@Composable
private fun InlineObjectView(obj: InlineObject) {
    when (obj) {
        is InlineObject.PageBreak -> HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
        is InlineObject.Image -> Text("[Gambar: ${obj.localPath}]", modifier = Modifier.padding(vertical = 4.dp))
        is InlineObject.Table -> TablePreview(obj)
        is InlineObject.Hyperlink -> Text(
            obj.text.ifBlank { obj.url }, color = Color(0xFF1976D2),
            textDecoration = TextDecoration.Underline, modifier = Modifier.padding(vertical = 4.dp)
        )
    }
}

@Composable
private fun TablePreview(table: InlineObject.Table) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        table.rows.forEach { row ->
            Row {
                row.forEach { cell ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(2.dp)
                            .background(Color(0xFFF3F4F6))
                            .border(1.dp, Color(0xFFE5E7EB))
                            .padding(6.dp)
                    ) { Text(cell.ifBlank { " " }, fontSize = 12.sp) }
                }
            }
        }
    }
}

private fun handleAction(
    action: ToolbarAction,
    vm: EditorViewModel,
    focusedIndex: Int,
    openDialog: (EditorDialog) -> Unit
) {
    when (action) {
        ToolbarAction.Bold -> vm.toggleBold(focusedIndex)
        ToolbarAction.Italic -> vm.toggleItalic(focusedIndex)
        ToolbarAction.Underline -> vm.toggleUnderline(focusedIndex)
        ToolbarAction.Strikethrough -> vm.toggleStrikethrough(focusedIndex)
        ToolbarAction.Highlight -> vm.toggleHighlight(focusedIndex)
        ToolbarAction.FontColor -> openDialog(EditorDialog.FontColor)
        ToolbarAction.ClearFormat -> vm.clearFormatting(focusedIndex)

        ToolbarAction.BlankPage -> vm.insertPageBreak(focusedIndex)
        ToolbarAction.Table -> openDialog(EditorDialog.InsertTable)
        ToolbarAction.Image -> openDialog(EditorDialog.InsertImage)
        ToolbarAction.Shape -> openDialog(EditorDialog.InsertImage)
        ToolbarAction.TextBox -> vm.insertParagraphAfter(focusedIndex)
        ToolbarAction.Link -> openDialog(EditorDialog.InsertLink)
        ToolbarAction.Comment -> openDialog(EditorDialog.Comment)
        ToolbarAction.HeaderFooter -> openDialog(EditorDialog.HeaderFooter)
        ToolbarAction.PageNumber -> vm.insertHyperlink(focusedIndex, "#", "#page")
        ToolbarAction.Footnote -> vm.insertParagraphAfter(focusedIndex)
        ToolbarAction.VoiceToText -> Unit

        ToolbarAction.Margin -> openDialog(EditorDialog.MarginPicker)
        ToolbarAction.Orientation -> vm.setOrientation(
            if (vm.currentSnapshot().pageSetup.orientation == Orientation.PORTRAIT) Orientation.LANDSCAPE else Orientation.PORTRAIT
        )
        ToolbarAction.PageSize -> openDialog(EditorDialog.PageSizePicker)
        ToolbarAction.Columns -> Unit
        ToolbarAction.Break -> vm.insertPageBreak(focusedIndex)

        ToolbarAction.Spelling -> Unit
        ToolbarAction.WordCount -> openDialog(EditorDialog.WordCount)
        ToolbarAction.NewComment -> openDialog(EditorDialog.Comment)
        ToolbarAction.TrackChanges -> Unit
        ToolbarAction.GrammarCheck -> Unit
        ToolbarAction.Find -> openDialog(EditorDialog.WordCount) // TODO: dialog find & replace nyata
        ToolbarAction.DrawTouch, ToolbarAction.DrawEraser, ToolbarAction.DrawPen, ToolbarAction.DrawHighlighter -> Unit // TODO: mode ink drawing
        ToolbarAction.MobileView, ToolbarAction.PrintLayoutView, ToolbarAction.ZoomIn, ToolbarAction.ZoomOut -> Unit // TODO: kontrol zoom canvas
        else -> Unit
    }
}

sealed class EditorDialog {
    data object None : EditorDialog()
    data object MarginPicker : EditorDialog()
    data object PageSizePicker : EditorDialog()
    data object InsertTable : EditorDialog()
    data object InsertImage : EditorDialog()
    data object InsertLink : EditorDialog()
    data object FontColor : EditorDialog()
    data object Comment : EditorDialog()
    data object HeaderFooter : EditorDialog()
    data object WordCount : EditorDialog()
    data object AiPrompt : EditorDialog()
}

@Composable
private fun EditorDialogHost(dialog: EditorDialog, vm: EditorViewModel, focusedIndex: Int, onAiGenerate: (String) -> Unit, onDismiss: () -> Unit) {
    when (dialog) {
        EditorDialog.None -> Unit
        EditorDialog.AiPrompt -> AiPromptDialog(onConfirm = { onAiGenerate(it); onDismiss() }, onDismiss = onDismiss)
        EditorDialog.MarginPicker -> MarginDialog(
            current = vm.currentSnapshot().pageSetup.margin,
            onApplyPreset = { vm.setMargin(it) },
            onApplyCustom = { t, b, l, r -> vm.setMargin(Margin(t, b, l, r)) },
            onDismiss = onDismiss
        )
        EditorDialog.PageSizePicker -> PageSizeDialog(
            current = vm.currentSnapshot().pageSetup,
            onPickPreset = { vm.setPaperSize(it) },
            onApplyCustom = { w, h -> vm.setCustomPaperSize(w, h) },
            onDismiss = onDismiss
        )
        EditorDialog.InsertTable -> TableSizeDialog(
            onConfirm = { r, c -> vm.insertTable(focusedIndex, r, c); onDismiss() }, onDismiss = onDismiss
        )
        EditorDialog.InsertImage -> TextInputDialog(
            title = "Sisipkan Gambar (path lokal)", label = "Path file",
            onConfirm = { path -> vm.insertImage(focusedIndex, path); onDismiss() }, onDismiss = onDismiss
        )
        EditorDialog.InsertLink -> LinkInputDialog(
            onConfirm = { text, url -> vm.insertHyperlink(focusedIndex, text, url); onDismiss() }, onDismiss = onDismiss
        )
        EditorDialog.FontColor -> PresetPickerDialog(
            title = "Warna Font", options = listOf("Hitam", "Merah", "Biru", "Hijau"),
            onPick = { idx ->
                val c = listOf(0xFF000000.toInt(), 0xFFD32F2F.toInt(), 0xFF1976D2.toInt(), 0xFF388E3C.toInt())[idx]
                vm.setFontColor(focusedIndex, c)
            }, onDismiss = onDismiss
        )
        EditorDialog.Comment -> TextInputDialog(
            title = "Komentar", label = "Isi komentar",
            onConfirm = { onDismiss() }, onDismiss = onDismiss
        )
        EditorDialog.HeaderFooter -> AlertDialog(
            onDismissRequest = onDismiss, confirmButton = { TextButton(onClick = onDismiss) { Text("Tutup") } },
            title = { Text("Header & Footer") }, text = { Text("Fitur header/footer editor visual tersedia di versi ini.") }
        )
        EditorDialog.WordCount -> {
            val (words, chars) = vm.wordCount()
            AlertDialog(
                onDismissRequest = onDismiss, confirmButton = { TextButton(onClick = onDismiss) { Text("Tutup") } },
                title = { Text("Hitungan Kata") }, text = { Text("$words kata, $chars karakter") }
            )
        }
    }
}

@Composable
private fun AiPromptDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var prompt by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tulis dengan AI") },
        text = {
            Column {
                Text("Jelaskan dokumen yang ingin dibuat, mis. \"Buat surat lamaran kerja posisi staff admin\"", fontSize = 12.sp, color = Color(0xFF6B7280))
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = prompt, onValueChange = { prompt = it }, modifier = Modifier.fillMaxWidth(), minLines = 3, label = { Text("Prompt") })
            }
        },
        confirmButton = { TextButton(onClick = { if (prompt.isNotBlank()) onConfirm(prompt) }) { Text("Buat Dokumen") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

@Composable
private fun MarginDialog(
    current: Margin,
    onApplyPreset: (Margin) -> Unit,
    onApplyCustom: (Float, Float, Float, Float) -> Unit,
    onDismiss: () -> Unit
) {
    var customMode by remember { mutableStateOf(false) }
    var top by remember { mutableStateOf(current.topMm.toString()) }
    var bottom by remember { mutableStateOf(current.bottomMm.toString()) }
    var left by remember { mutableStateOf(current.leftMm.toString()) }
    var right by remember { mutableStateOf(current.rightMm.toString()) }

    val presets = listOf(
        "Normal (2.54cm)" to Margin.default(),
        "Sempit (1.27cm)" to Margin.narrow(),
        "Sedang (1.9cm kiri-kanan)" to Margin.moderate(),
        "Luas (5.08cm kiri-kanan)" to Margin.wide(),
        "Berdampingan" to Margin.mirrored()
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Margin Halaman") },
        text = {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !customMode, onClick = { customMode = false }, label = { Text("Preset") })
                    FilterChip(selected = customMode, onClick = { customMode = true }, label = { Text("Kustom") })
                }
                Spacer(Modifier.height(12.dp))
                if (!customMode) {
                    Column {
                        presets.forEach { (label, margin) ->
                            TextButton(onClick = { onApplyPreset(margin); onDismiss() }) { Text(label) }
                        }
                    }
                } else {
                    OutlinedTextField(value = top, onValueChange = { top = it }, label = { Text("Atas (mm)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = bottom, onValueChange = { bottom = it }, label = { Text("Bawah (mm)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = left, onValueChange = { left = it }, label = { Text("Kiri (mm)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = right, onValueChange = { right = it }, label = { Text("Kanan (mm)") }, modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            if (customMode) {
                TextButton(onClick = {
                    onApplyCustom(
                        top.toFloatOrNull() ?: 25.4f, bottom.toFloatOrNull() ?: 25.4f,
                        left.toFloatOrNull() ?: 25.4f, right.toFloatOrNull() ?: 25.4f
                    )
                    onDismiss()
                }) { Text("Terapkan") }
            } else {
                TextButton(onClick = onDismiss) { Text("Tutup") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

@Composable
private fun PageSizeDialog(
    current: PageSetup,
    onPickPreset: (PaperSize) -> Unit,
    onApplyCustom: (Float, Float) -> Unit,
    onDismiss: () -> Unit
) {
    var customMode by remember { mutableStateOf(current.size == PaperSize.CUSTOM) }
    var width by remember { mutableStateOf((current.customWidthMm ?: current.widthMm).toString()) }
    var height by remember { mutableStateOf((current.customHeightMm ?: current.heightMm).toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ukuran Kertas") },
        text = {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !customMode, onClick = { customMode = false }, label = { Text("Preset") })
                    FilterChip(selected = customMode, onClick = { customMode = true }, label = { Text("Kustom") })
                }
                Spacer(Modifier.height(12.dp))
                if (!customMode) {
                    Column {
                        PaperSize.values().filter { it != PaperSize.CUSTOM }.forEach { size ->
                            TextButton(onClick = { onPickPreset(size); onDismiss() }) { Text(size.label) }
                        }
                    }
                } else {
                    OutlinedTextField(value = width, onValueChange = { width = it }, label = { Text("Lebar (mm)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = height, onValueChange = { height = it }, label = { Text("Tinggi (mm)") }, modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            if (customMode) {
                TextButton(onClick = {
                    onApplyCustom(width.toFloatOrNull() ?: 210f, height.toFloatOrNull() ?: 297f)
                    onDismiss()
                }) { Text("Terapkan") }
            } else {
                TextButton(onClick = onDismiss) { Text("Tutup") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

@Composable
private fun PresetPickerDialog(title: String, options: List<String>, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Tutup") } },
        title = { Text(title) },
        text = {
            Column {
                options.forEachIndexed { idx, label ->
                    TextButton(onClick = { onPick(idx); onDismiss() }) { Text(label) }
                }
            }
        }
    )
}

@Composable
private fun TableSizeDialog(onConfirm: (Int, Int) -> Unit, onDismiss: () -> Unit) {
    var rows by remember { mutableStateOf("2") }
    var cols by remember { mutableStateOf("2") }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onConfirm(rows.toIntOrNull() ?: 2, cols.toIntOrNull() ?: 2) }) { Text("Sisipkan") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
        title = { Text("Sisipkan Tabel") },
        text = {
            Column {
                OutlinedTextField(value = rows, onValueChange = { rows = it }, label = { Text("Baris") })
                OutlinedTextField(value = cols, onValueChange = { cols = it }, label = { Text("Kolom") })
            }
        }
    )
}

@Composable
private fun TextInputDialog(title: String, label: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var value by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onConfirm(value) }) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
        title = { Text(title) },
        text = { OutlinedTextField(value = value, onValueChange = { value = it }, label = { Text(label) }) }
    )
}

@Composable
private fun LinkInputDialog(onConfirm: (String, String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onConfirm(text, url) }) { Text("Sisipkan") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
        title = { Text("Sisipkan Tautan") },
        text = {
            Column {
                OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("Teks tampilan") })
                OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("URL") })
            }
        }
    )
}
