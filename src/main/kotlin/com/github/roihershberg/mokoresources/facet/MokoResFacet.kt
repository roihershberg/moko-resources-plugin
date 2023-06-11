/*
 * Copied and modified from https://github.com/JetBrains/android/blob/ee612e46427cb96ea4cd2170d0f38a7c2746dce3/android-common/src/org/jetbrains/android/facet/AndroidFacet.java
 * under the Apache License, Version 2.0 (see third-party-licenses/APACHE-2.0 from root of project) and is licensed
 * under the GNU GENERAL PUBLIC LICENSE Version 3. See "LICENSE" file in root of project.
 */
package com.github.roihershberg.mokoresources.facet

import com.github.roihershberg.mokoresources.tools.getModuleSafely
import com.intellij.facet.Facet
import com.intellij.facet.FacetManager
import com.intellij.facet.FacetTypeId
import com.intellij.facet.FacetTypeRegistry
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.util.xml.ConvertContext
import com.intellij.util.xml.DomElement

class MokoResFacet(module: Module, name: String, configuration: MokoResFacetConfiguration) :
    Facet<MokoResFacetConfiguration?>(facetType, module, name, configuration, null) {
    val properties: MokoResFacetProperties
        get() = configuration.state

    companion object {
        val ID = FacetTypeId<MokoResFacet>("moko-res")
        const val NAME = "Moko Resources"

        fun getInstance(file: VirtualFile, project: Project): MokoResFacet? {
            val module = ModuleUtilCore.findModuleForFile(file, project) ?: return null
            return getInstance(module)
        }

        fun getInstance(context: ConvertContext): MokoResFacet? {
            return findMokoResFacet(context.module)
        }

        fun getInstance(element: PsiElement): MokoResFacet? {
            return findMokoResFacet(element.getModuleSafely())
        }

        fun getInstance(element: DomElement): MokoResFacet? {
            return findMokoResFacet(element.module)
        }

        private fun findMokoResFacet(module: Module?): MokoResFacet? {
            return if (module != null) getInstance(module) else null
        }

        fun getInstance(module: Module): MokoResFacet? {
            return if (!module.isDisposed) FacetManager.getInstance(module).getFacetByType(ID) else null
        }

        val facetType: MokoResFacetType
            get() = FacetTypeRegistry.getInstance().findFacetType(ID) as MokoResFacetType
    }
}
