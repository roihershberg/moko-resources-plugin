/*
 * Copied and modified from https://github.com/JetBrains/android/blob/master/android/src/org/jetbrains/android/facet/AndroidFacetEditorTab.java
 * under the Apache License, Version 2.0 (see third-party-licenses/APACHE-2.0 from root of project) and is licensed
 * under the GNU GENERAL PUBLIC LICENSE Version 3. See "LICENSE" file in root of project.
 */
package com.github.roihershberg.mokoresources.facet

import com.github.roihershberg.mokoresources.MokoResBundle
import com.github.roihershberg.mokoresources.tools.toAbsolutePath
import com.github.roihershberg.mokoresources.tools.toRelativePath
import com.intellij.application.options.ModulesComboBox
import com.intellij.facet.ui.FacetEditorContext
import com.intellij.facet.ui.FacetEditorTab
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.toNullableProperty
import org.jetbrains.kotlin.idea.project.platform
import org.jetbrains.kotlin.platform.isCommon
import javax.swing.JComponent

class MokoResFacetEditorTab(
    private val context: FacetEditorContext,
    private val facetConfiguration: MokoResFacetConfiguration
) : FacetEditorTab() {
    class Provider : MokoResFacetConfiguration.EditorTabProvider {
        override fun createFacetEditorTab(
            editorContext: FacetEditorContext,
            configuration: MokoResFacetConfiguration
        ): FacetEditorTab {
            return MokoResFacetEditorTab(editorContext, configuration)
        }
    }

    private data class PanelModel(
        var resourcesModule: Module? = null,
        var resDirectory: String = "",
        var resClassName: String = "",
        var resPackageName: String = "",
        var mrVisibility: MRVisibility = MRVisibility.Public,
    )

    private val myPanelConfigurable = object : BoundConfigurable(MokoResBundle.message("facet.settings"), null) {
        override fun createPanel(): DialogPanel = panel {
            val resourcesModule = facetConfiguration.getResourcesModule(context.module)
            val model = PanelModel(
                resourcesModule,
                toAbsolutePath(resourcesModule, facetConfiguration.resFolderRelativeFromResModule) ?: "",
                facetConfiguration.resClassName,
                facetConfiguration.resPackageName,
                facetConfiguration.mrVisibility,
            )

            val modulesComboBox = ModulesComboBox().apply {
                val allModules = ModuleManager.getInstance(context.project).modules
                setModules(allModules.filter { it.platform.isCommon() }) // Resources should only reside in the common modules
                selectedModule = model.resourcesModule
                addItemListener {
                    model.resourcesModule = selectedModule
                }
            }
            row(MokoResBundle.message("facet.editor.tab.resources.module")) {
                cell(modulesComboBox)
            }
            row(MokoResBundle.message("facet.editor.tab.resources.directory")) {
                textFieldWithBrowseButton(
                    MokoResBundle.message("facet.editor.tab.resources.directory.chooser.title"),
                    context.project,
                    FileChooserDescriptorFactory.createSingleFolderDescriptor(),
                ).bindText(model::resDirectory)
            }
            row(MokoResBundle.message("facet.editor.tab.resources.class.name")) {
                textField().bindText(model::resClassName)
            }
            row(MokoResBundle.message("facet.editor.tab.resources.package.name")) {
                textField().bindText(model::resPackageName)
            }
            row(MokoResBundle.message("facet.editor.tab.resources.class.visibility")) {
                comboBox(MRVisibility.values().toList()).bindItem(model::mrVisibility.toNullableProperty())
            }

            onIsModified {
                val selectedModuleId = ExternalSystemApiUtil.getExternalProjectId(model.resourcesModule)
                val storedModuleId = facetConfiguration.resourcesModuleId

                (selectedModuleId != null && selectedModuleId != storedModuleId)
                        || (model.resDirectory != toAbsolutePath(model.resourcesModule, facetConfiguration.resFolderRelativeFromResModule))
                        || (model.resClassName != facetConfiguration.resClassName)
                        || (model.resPackageName != facetConfiguration.resPackageName)
                        || (model.mrVisibility != facetConfiguration.mrVisibility)
            }

            onReset {
                model.resourcesModule = facetConfiguration.getResourcesModule(context.module)
                model.resDirectory =
                    toAbsolutePath(model.resourcesModule, facetConfiguration.resFolderRelativeFromResModule) ?: ""
                model.resClassName = facetConfiguration.resClassName
                model.resPackageName = facetConfiguration.resPackageName
                model.mrVisibility = facetConfiguration.mrVisibility
            }

            onApply {
                facetConfiguration.setResourcesModule(model.resourcesModule)
                facetConfiguration.resFolderRelativeFromResModule =
                    toRelativePath(model.resourcesModule, model.resDirectory) ?: ""
                facetConfiguration.resClassName = model.resClassName
                facetConfiguration.resPackageName = model.resPackageName
                facetConfiguration.mrVisibility = model.mrVisibility
            }
        }
    }

    // Delegate methods to the UI dsl bounded configurable
    override fun getDisplayName(): String = myPanelConfigurable.displayName
    override fun createComponent(): JComponent = myPanelConfigurable.createComponent()
    override fun isModified(): Boolean = myPanelConfigurable.isModified
    override fun reset() = myPanelConfigurable.reset()
    override fun apply() = myPanelConfigurable.apply()
}
