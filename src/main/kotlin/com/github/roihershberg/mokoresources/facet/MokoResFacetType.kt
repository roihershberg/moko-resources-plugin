/*
 * Copyright 2000-2010 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.roihershberg.mokoresources.facet

import com.intellij.facet.Facet
import com.intellij.facet.FacetType
import com.intellij.openapi.module.JavaModuleType
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleType

class MokoResFacetType : FacetType<MokoResFacet, MokoResFacetConfiguration>(MokoResFacet.ID, TYPE_ID, MokoResFacet.NAME) {
    override fun createDefaultConfiguration(): MokoResFacetConfiguration {
        return MokoResFacetConfiguration()
    }

    override fun createFacet(
        module: Module,
        name: String,
        configuration: MokoResFacetConfiguration,
        underlyingFacet: Facet<*>?,
    ): MokoResFacet {
        // DO NOT COMMIT MODULE-ROOT MODELS HERE!
        // modules are not initialized yet, so some data may be lost
        return MokoResFacet(module, name, configuration)
    }

    override fun isSuitableModuleType(moduleType: ModuleType<*>?): Boolean {
        return moduleType is JavaModuleType
    }

    companion object {
        const val TYPE_ID = "moko-res"
    }
}
