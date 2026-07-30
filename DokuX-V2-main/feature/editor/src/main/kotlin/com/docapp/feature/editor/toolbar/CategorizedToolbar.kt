package com.docapp.feature.editor.toolbar

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Toolbar dua lapis: tab kategori (Beranda/Sisipkan/Tata Letak/Tinjau) di atas,
 * daftar aksi horizontal di bawah — sesuai pola navigasi Word mobile.
 */
@Composable
fun CategorizedToolbar(
    onAction: (ToolbarAction) -> Unit,
    isFeatureUnlocked: (ToolbarAction) -> Boolean
) {
    var selectedCategory by remember { mutableStateOf(ToolbarCategory.HOME) }

    Column {
        TabRow(selectedTabIndex = ToolbarCategory.entries.indexOf(selectedCategory)) {
            ToolbarCategory.entries.forEach { category ->
                Tab(
                    selected = category == selectedCategory,
                    onClick = { selectedCategory = category },
                    text = { Text(category.label) }
                )
            }
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(ToolbarAction.forCategory(selectedCategory)) { action ->
                ToolbarActionChip(
                    action = action,
                    locked = action.isPremium && !isFeatureUnlocked(action),
                    onClick = { onAction(action) }
                )
            }
        }
    }
}

@Composable
private fun ToolbarActionChip(action: ToolbarAction, locked: Boolean, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text(action.label) },
        trailingIcon = {
            if (locked) Icon(Icons.Default.Lock, contentDescription = "Terkunci", modifier = Modifier.size(16.dp))
        }
    )
}
