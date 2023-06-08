/*
 * Copied and modified from https://github.com/JetBrains/android/blob/master/android-common/src/org/jetbrains/android/facet/AndroidFacetConfiguration.java
 * under the Apache License, Version 2.0 (see third-party-licenses/APACHE-2.0 from root of project) and is licensed
 * under the GNU GENERAL PUBLIC LICENSE Version 3. See "LICENSE" file in root of project.
 */
package com.github.roihershberg.mokoresources.facet

import com.github.roihershberg.mokoresources.tools.findModuleById
import com.intellij.facet.FacetConfiguration
import com.intellij.facet.ui.FacetEditorContext
import com.intellij.facet.ui.FacetEditorTab
import com.intellij.facet.ui.FacetValidatorsManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.module.Module

/**
 * Implementation of [FacetConfiguration] for [MokoResFacet].
 *
 * Stores configuration by serializing [MokoResFacetProperties] with [PersistentStateComponent].
 */
class MokoResFacetConfiguration : FacetConfiguration, PersistentStateComponent<MokoResFacetProperties> {
    private var myProperties: MokoResFacetProperties = MokoResFacetProperties()

    /**
     * Application service implemented in JPS code.
     */
    internal interface EditorTabProvider {
        fun createFacetEditorTab(
            editorContext: FacetEditorContext,
            configuration: MokoResFacetConfiguration,
        ): FacetEditorTab
    }

    override fun createEditorTabs(
        editorContext: FacetEditorContext,
        validatorsManager: FacetValidatorsManager,
    ): Array<FacetEditorTab> {
        val editorTabProvider = ApplicationManager.getApplication().getService(EditorTabProvider::class.java)
        if (editorTabProvider != null) {
            return arrayOf(editorTabProvider.createFacetEditorTab(editorContext, this))
        }
        return arrayOf(NotEditableFacetEditorTab())
    }

    // Properties delegates

    var resourcesModuleId: String
        get() = myProperties.RESOURCES_MODULE_ID
        set(value) {
            myProperties.RESOURCES_MODULE_ID = value
        }

    fun getResourcesModule(contextModule: Module): Module? =
        findModuleById(contextModule, resourcesModuleId)

    fun setResourcesModule(module: Module?) {
        resourcesModuleId = ExternalSystemApiUtil.getExternalProjectId(module) ?: ""
    }

    var resFolderRelativeFromResModule: String
        get() = myProperties.RES_FOLDER_RELATIVE_FROM_RES_MODULE
        set(value) {
            myProperties.RES_FOLDER_RELATIVE_FROM_RES_MODULE = value
        }
    var resClassName: String
        get() = myProperties.RES_CLASS_NAME
        set(value) {
            myProperties.RES_CLASS_NAME = value
        }
    var resPackageName: String
        get() = myProperties.RES_PACKAGE_NAME
        set(value) {
            myProperties.RES_PACKAGE_NAME = value
        }

    var mrVisibility: MRVisibility
        get() = try {
            MRVisibility.valueOf(myProperties.MR_VISIBILITY)
        } catch (_: IllegalArgumentException) {
            MRVisibility.Public
        }
        set(value) {
            myProperties.MR_VISIBILITY = value.toString()
        }

    override fun getState(): MokoResFacetProperties {
        return myProperties
    }

    override fun loadState(properties: MokoResFacetProperties) {
        myProperties = properties
    }
}
