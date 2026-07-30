package com.docapp.feature.template

import com.docapp.core.gate.FeatureIds
import com.docapp.core.gate.StateVault
import com.docapp.core.model.Document

data class TemplateItem(val id: String, val name: String, val tier: Int, val build: () -> Document)

/** tier 0 = gratis, tier 1 = premium (butuh FeatureIds.M2). */
class TemplatePath(private val allTemplates: List<TemplateItem>) {
    fun listAvailable(): List<TemplateItem> =
        allTemplates.filter { it.tier == 0 || StateVault.has(FeatureIds.M2) }

    fun isLocked(item: TemplateItem): Boolean = item.tier != 0 && !StateVault.has(FeatureIds.M2)
}
