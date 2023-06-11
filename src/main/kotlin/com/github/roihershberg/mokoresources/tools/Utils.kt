package com.github.roihershberg.mokoresources.tools

import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.guessModuleDir
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.PathUtil
import org.jetbrains.annotations.SystemIndependent
import org.jetbrains.plugins.gradle.util.GradleConstants
import org.jetbrains.plugins.gradle.util.GradleUtil
import java.io.File

fun Module.getDirPath(): @SystemIndependent String? {
    val linkedProjectPath = ExternalSystemApiUtil.getExternalProjectPath(this)
    if (!linkedProjectPath.isNullOrEmpty()) {
        return linkedProjectPath
    }
    return guessModuleDir()?.path
}

fun findGradleModuleDataNode(module: Module): DataNode<ModuleData>? {
    val projectPath = ExternalSystemApiUtil.getExternalProjectPath(module) ?: return null
    return ExternalSystemApiUtil.findModuleNode(module.project, GradleConstants.SYSTEM_ID, projectPath)
}

@Suppress("UNCHECKED_CAST")
fun findModuleById(contextModule: Module, id: String): Module? {
    val moduleDataNode = findGradleModuleDataNode(contextModule) ?: return null
    if (moduleDataNode.data.id == id) return contextModule

    val projectDataNode = ExternalSystemApiUtil.findParent(moduleDataNode, ProjectKeys.PROJECT) ?: return null
    val desiredModuleDataNode = ExternalSystemApiUtil.findFirstRecursively(projectDataNode) {
        (it.data as? ModuleData)?.id == id
    } as? DataNode<out ModuleData>? ?: return null

    return GradleUtil.findGradleModule(contextModule.project, desiredModuleDataNode.data)
}

fun toRelativePath(module: Module?, absolutePath: String): String? {
    if (module == null) return null
    val absPath: @SystemIndependent String = FileUtil.toSystemIndependentName(absolutePath)
    val moduleDirPath: @SystemIndependent String? = module.getDirPath()
    return moduleDirPath?.let { FileUtil.getRelativePath(it, absPath, '/') }
}

fun toAbsolutePath(module: Module?, relativePath: String): String? {
    if (module == null) {
        return null
    }
    if (relativePath.isEmpty()) {
        return ""
    }
    val moduleDirPath: @SystemIndependent String = module.getDirPath() ?: return null
    val path = FileUtil.toCanonicalPath(File(moduleDirPath, relativePath).path)
    return if (path != null) PathUtil.getLocalPath(path) else null
}
