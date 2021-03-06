/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.caches.resolve.util

import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.idea.caches.project.ModuleSourceInfo
import org.jetbrains.kotlin.idea.caches.project.ScriptModuleInfo
import org.jetbrains.kotlin.idea.caches.project.SourceForBinaryModuleInfo
import org.jetbrains.kotlin.idea.caches.project.getModuleInfo
import org.jetbrains.kotlin.idea.stubindex.KotlinSourceFilterScope
import org.jetbrains.kotlin.psi.KtCodeFragment
import org.jetbrains.kotlin.psi.KtFile

//NOTE: idea default API returns module search scope for file under module but not in source or production source (for example, test data )
// this scope can't be used to search for kotlin declarations in index in order to resolve in that case
// see com.intellij.psi.impl.file.impl.ResolveScopeManagerImpl.getInherentResolveScope
fun getResolveScope(file: KtFile): GlobalSearchScope {
    if (file is KtCodeFragment) {
        // scope should be corrected when KT-6223 is implemented
        file.getContextContainingFile()?.resolveScope?.let {
            return when (file.getModuleInfo()) {
                is SourceForBinaryModuleInfo -> KotlinSourceFilterScope.libraryClassFiles(it, file.project)
                else -> KotlinSourceFilterScope.sourceAndClassFiles(it, file.project)
            }
        }
    }

    return when (file.getModuleInfo()) {
        is ModuleSourceInfo -> KotlinSourceFilterScope.projectSourceAndClassFiles(file.resolveScope, file.project)
        is ScriptModuleInfo -> file.getModuleInfo().dependencies().map { it.contentScope() }.let { GlobalSearchScope.union(it.toTypedArray()) }
        else -> GlobalSearchScope.EMPTY_SCOPE
    }
}
