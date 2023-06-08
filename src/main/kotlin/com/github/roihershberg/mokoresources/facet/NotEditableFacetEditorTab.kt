/*
 * Copied and modified from https://github.com/JetBrains/android/blob/master/android-common/src/org/jetbrains/android/facet/NotEditableAndroidFacetEditorTab.kt
 * under the Apache License, Version 2.0 (see third-party-licenses/APACHE-2.0 from root of project) and is licensed
 * under the GNU GENERAL PUBLIC LICENSE Version 3. See "LICENSE" file in root of project.
 */
package com.github.roihershberg.mokoresources.facet

import com.github.roihershberg.mokoresources.MokoResBundle
import com.intellij.facet.ui.FacetEditorTab
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

class NotEditableFacetEditorTab : FacetEditorTab() {
    override fun isModified() = false

    override fun getDisplayName() = MokoResBundle.message("facet.settings")

    override fun createComponent(): JComponent = panel {
        row { rowComment(MokoResBundle.message("facet.editor.tab.error")) }
    }
}
