package com.docapp.feature.filebrowser

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.docapp.data.db.DocumentEntity
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileBrowserScreen(
    recentDocuments: List<DocumentEntity>,
    onOpenRecent: (DocumentEntity) -> Unit,
    onOpenFromDevice: () -> Unit,
    onCreateNew: () -> Unit,
    onDelete: (DocumentEntity) -> Unit,
    onToggleFavorite: (DocumentEntity) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(0) } // 0 = Semua Dokumen, 1 = Favorit
    var showRevisionDialogForDoc by remember { mutableStateOf<DocumentEntity?>(null) }

    val filteredDocuments = remember(recentDocuments, searchQuery, selectedFilter) {
        recentDocuments.filter { doc ->
            val matchesQuery = searchQuery.isBlank() || doc.title.contains(searchQuery, ignoreCase = true)
            val matchesFilter = if (selectedFilter == 1) doc.isFavorite else true
            matchesQuery && matchesFilter
        }
    }

    Scaffold(
        containerColor = Color(0xFFF8F9FA)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // System info header pill bar
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0F172A))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "01.51", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Surface(
                        color = Color(0xFF1E293B),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = "Android 15 (API 35)",
                            color = Color(0xFF4ADE80),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.WifiOff, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Text(text = "Offline", color = Color.White, fontSize = 11.sp)
                        Text(text = "100%", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Icon(Icons.Default.BatteryFull, contentDescription = null, tint = Color(0xFF4ADE80), modifier = Modifier.size(14.dp))
                    }
                }
            }

            // Top Header Card ("DocPro Enterprise")
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Title row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1A73E8)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "D", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(text = "DocPro", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF0F172A))
                                    Text(text = "Enterprise", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1A73E8))

                                    Surface(
                                        color = Color(0xFFE6F4EA),
                                        border = BorderStroke(1.dp, Color(0xFFCEEAD6)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(Icons.Default.WifiOff, contentDescription = null, tint = Color(0xFF1E8E3E), modifier = Modifier.size(12.dp))
                                            Text(text = "Offline Engine", color = Color(0xFF1E8E3E), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF34A853)))
                                    Text(text = "INTEGRITY GUARD: ACTIVE", color = Color(0xFF5F6368), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Bottom Actions inside card
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedIconButton(
                                onClick = {},
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(Icons.Outlined.Shield, contentDescription = null, tint = Color(0xFF1E8E3E), modifier = Modifier.size(18.dp))
                            }

                            Button(
                                onClick = {},
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8F0FE), contentColor = Color(0xFF1A73E8)),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.ShoppingBag, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text(text = "Google Play Billing", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            OutlinedIconButton(
                                onClick = {},
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(Icons.Outlined.LockOpen, contentDescription = null, tint = Color(0xFF5F6368), modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Cari dokumen .docx lokal...", color = Color(0xFF757575), fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF757575)) },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = Color(0xFF1A73E8),
                        unfocusedBorderColor = Color(0xFFE0E0E0)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Action Row: Layers button & + Dokumen Baru
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedIconButton(
                        onClick = onOpenFromDevice,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                        colors = IconButtonDefaults.outlinedIconButtonColors(containerColor = Color.White),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(Icons.Default.Layers, contentDescription = "Buka Perangkat", tint = Color(0xFF1A73E8))
                    }

                    Button(
                        onClick = onCreateNew,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8)),
                        shape = RoundedCornerShape(22.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                            Text(text = "Dokumen Baru", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Category Filter Pills
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { selectedFilter = 0 },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedFilter == 0) Color(0xFF1A73E8) else Color.White,
                            contentColor = if (selectedFilter == 0) Color.White else Color(0xFF3C4043)
                        ),
                        border = if (selectedFilter == 0) null else BorderStroke(1.dp, Color(0xFFE0E0E0)),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(text = "Semua Dokumen (${recentDocuments.size})", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = { selectedFilter = 1 },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (selectedFilter == 1) Color(0xFF1A73E8) else Color.White,
                            contentColor = if (selectedFilter == 1) Color.White else Color(0xFF3C4043)
                        ),
                        border = BorderStroke(1.dp, if (selectedFilter == 1) Color(0xFF1A73E8) else Color(0xFFE0E0E0)),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = if (selectedFilter == 1) Color.White else Color(0xFFFBBC04),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(text = "Favorit", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Document List
            if (filteredDocuments.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (selectedFilter == 1) "Belum ada dokumen favorit." else "Belum ada dokumen. Ketuk + Dokumen Baru.",
                            color = Color(0xFF757575),
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                items(filteredDocuments, key = { it.id }) { doc ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenRecent(doc) }
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Top part of document card
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFFE8F0FE)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Description,
                                        contentDescription = null,
                                        tint = Color(0xFF1A73E8),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = doc.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color(0xFF0F172A),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${formatSize(doc.sizeBytes)}  •  A4",
                                        fontSize = 12.sp,
                                        color = Color(0xFF5F6368)
                                    )
                                }
                            }

                            HorizontalDivider(color = Color(0xFFF1F3F4))

                            // Bottom part of document card
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = Color(0xFF757575),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = formatDate(doc.modifiedAt),
                                        fontSize = 12.sp,
                                        color = Color(0xFF5F6368)
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    IconButton(
                                        onClick = { showRevisionDialogForDoc = doc },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.History,
                                            contentDescription = "Riwayat Revisi",
                                            tint = Color(0xFF5F6368),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { onToggleFavorite(doc) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            if (doc.isFavorite) Icons.Default.Star else Icons.Outlined.StarOutline,
                                            contentDescription = "Favorit",
                                            tint = if (doc.isFavorite) Color(0xFFFBBC04) else Color(0xFF5F6368),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { onDelete(doc) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.DeleteOutline,
                                            contentDescription = "Hapus",
                                            tint = Color(0xFFD32F2F),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    showRevisionDialogForDoc?.let { doc ->
        AlertDialog(
            onDismissRequest = { showRevisionDialogForDoc = null },
            confirmButton = {
                TextButton(onClick = { showRevisionDialogForDoc = null }) {
                    Text("Tutup")
                }
            },
            title = { Text("Riwayat Revisi Lokal") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Dokumen: ${doc.title}")
                    Text("Versi aktif: Autosave snapshot (${formatDate(doc.modifiedAt)})", fontSize = 13.sp)
                    HorizontalDivider()
                    Text("1. Autosave — ${formatDate(doc.modifiedAt)}", fontSize = 12.sp, color = Color(0xFF1E8E3E))
                    Text("2. Versi Awal — ${formatDate(doc.createdAt)}", fontSize = 12.sp, color = Color(0xFF5F6368))
                }
            }
        )
    }
}

private fun formatDate(epochMs: Long): String =
    SimpleDateFormat("dd MMM, HH.mm", Locale("id", "ID")).format(Date(epochMs))

private fun formatSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${String.format(Locale.US, "%.1f", bytes / 1024f)} KB"
    else -> "${String.format(Locale.US, "%.1f", bytes / (1024f * 1024f))} MB"
}
