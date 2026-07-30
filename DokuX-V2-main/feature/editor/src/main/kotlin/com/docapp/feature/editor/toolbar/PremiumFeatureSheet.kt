package com.docapp.feature.editor.toolbar

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Paywall kontekstual: muncul saat user menyentuh fitur premium terkunci
 * (Suara ke Teks file, Tata Bahasa Lanjutan, dll — semua di bawah gate K7).
 * Ditampilkan di titik minat, bukan interupsi di awal (lihat strategi marketing).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumFeatureSheet(
    featureName: String,
    onDismiss: () -> Unit,
    onBuyBundle: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(40.dp))
            Spacer(Modifier.height(12.dp))
            Text("$featureName adalah fitur Pro", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))

            BundleBenefit("Suara ke teks — file audio tanpa batas durasi")
            BundleBenefit("Find & replace lanjutan, compare dokumen, batch convert")
            BundleBenefit("Semua template premium")
            BundleBenefit("Export tanpa watermark")

            Spacer(Modifier.height(20.dp))
            Button(onClick = onBuyBundle, modifier = Modifier.fillMaxWidth()) {
                Text("Buka semua — $1,99 (sekali bayar)")
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onDismiss) { Text("Nanti saja") }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun BundleBenefit(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}
