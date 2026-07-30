package com.docapp.feature.tools

import com.docapp.core.gate.FeatureIds
import com.docapp.core.security.NativeBridge

/** Fitur produktivitas lanjutan: regex find&replace, compare dokumen, batch convert. */
class ToolsPath(private val native: NativeBridge = NativeBridge) {
    fun regexReplaceEnabled(): Boolean = native.chk(FeatureIds.K7)
    fun compareDocumentsEnabled(): Boolean = native.chk(FeatureIds.K7)
    fun batchConvertEnabled(): Boolean = native.chk(FeatureIds.K7)
}
