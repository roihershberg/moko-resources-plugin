/*
 * Copied and modified from https://github.com/JetBrains/android/blob/master/jps-model/src/org/jetbrains/android/facet/AndroidFacetProperties.java
 * under the Apache License, Version 2.0 (see third-party-licenses/APACHE-2.0 from root of project) and is licensed
 * under the GNU GENERAL PUBLIC LICENSE Version 3. See "LICENSE" file in root of project.
 */
package com.github.roihershberg.mokoresources.facet

/**
 * Configuration of plugin saved in the IML file corresponding to the module.
 *
 * These objects are serialized to XML by [com.github.roihershberg.mokoresources.facet.MokoResFacetConfiguration] using [com.intellij.util.xmlb.XmlSerializer].
 */
@Suppress("PropertyName")
class MokoResFacetProperties {
    @JvmField var RESOURCES_MODULE_ID = ""
    @JvmField var RES_FOLDER_RELATIVE_FROM_RES_MODULE = ""
    @JvmField var RES_CLASS_NAME = ""
    @JvmField var RES_PACKAGE_NAME = ""
    @JvmField var MR_VISIBILITY: String = MRVisibility.Public.toString()
}

enum class MRVisibility {
    Public,
    Internal,
}
