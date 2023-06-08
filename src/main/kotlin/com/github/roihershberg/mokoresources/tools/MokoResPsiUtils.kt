/*
 * Copied and modified from https://github.com/JetBrains/android/blob/ee612e46427cb96ea4cd2170d0f38a7c2746dce3/android-common/src/com/android/tools/idea/AndroidPsiUtils.java
 * under the Apache License, Version 2.0 (see third-party-licenses/APACHE-2.0 from root of project) and is licensed
 * under the GNU GENERAL PUBLIC LICENSE Version 3. See "LICENSE" file in root of project.
 */
package com.github.roihershberg.mokoresources.tools

//import com.android.resources.ResourceType
import com.intellij.lang.Language
import com.intellij.lang.xml.XMLLanguage
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.ModificationTracker
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.psi.impl.PsiModificationTrackerImpl
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import org.jetbrains.annotations.Contract
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UastFacade
import org.jetbrains.uast.getAsJavaPsiElement

/**
 * Looks up the [PsiFile] for a given [VirtualFile] in a given [Project], in
 * a safe way (meaning it will acquire a read lock first, and will check that the file is valid
 *
 * @param project the project
 * @param file the file
 * @return the corresponding [PsiFile], or null if not found or valid
 */
fun getPsiFileSafely(project: Project, file: VirtualFile): PsiFile? =
    ApplicationManager.getApplication().runReadAction(
        Computable {
            if (project.isDisposed) {
                return@Computable null
            }
            if (file.isValid) PsiManager.getInstance(project).findFile(file) else null
        }
    )

/**
 * Looks up the [Module] for a given [PsiElement], in a safe way (meaning it will
 * acquire a read lock first).
 *
 * @return the module containing the element, or null if not found
 */
fun PsiElement.getModuleSafely(): Module? =
    ApplicationManager.getApplication().runReadAction(
        Computable {
            ModuleUtilCore.findModuleForPsiElement(this)
        }
    )

/**
 * Looks up the [Module] containing a given [VirtualFile] in a given [Project], in
 * a safe way (meaning it will acquire a read lock first
 *
 * @param project the project
 * @param file the file
 * @return the corresponding [Module], or null if not found
 */
fun getModuleSafely(project: Project, file: VirtualFile): Module? =
    ApplicationManager.getApplication().runReadAction(
        Computable {
            if (project.isDisposed) {
                return@Computable null
            }
            val psiFile = PsiManager.getInstance(project).findFile(file)
            if (psiFile == null) null else ModuleUtilCore.findModuleForPsiElement(psiFile)
        }
    )

/**
 * Returns the root tag for the given [XmlFile], if any, acquiring the read
 * lock to do so if necessary
 *
 * @return the corresponding root tag, if any
 */
fun XmlFile.getRootTagSafely(): XmlTag? =
    if (project.isDisposed) {
        null
    } else if (ApplicationManager.getApplication().isReadAccessAllowed) {
        rootTag
    } else ApplicationManager.getApplication().runReadAction(Computable { rootTag })

/**
 * Get the value of an attribute in the [XmlFile] safely (meaning it will acquire the read lock first).
 */
fun XmlFile.getRootTagAttributeSafely(
    attribute: String,
    namespace: String?,
): String? {
    val application = ApplicationManager.getApplication()
    if (!application.isReadAccessAllowed) {
        return application.runReadAction(
            Computable {
                getRootTagAttributeSafely(
                    attribute,
                    namespace
                )
            }
        )
    } else {
        val tag = rootTag ?: return null
        val attr =
            if (namespace != null) tag.getAttribute(attribute, namespace) else tag.getAttribute(attribute)
        return attr?.value
    }
}

/**
 * Returns the [PsiDirectory] for the given [VirtualFile], with a read lock.
 *
 * @param dir the file to look up the PSI directory for
 * @return the corresponding PSI directory, if any
 */
fun getPsiDirectorySafely(project: Project, dir: VirtualFile): PsiDirectory? =
    ApplicationManager.getApplication().runReadAction(
        Computable {
            if (project.isDisposed) {
                return@Computable null
            }
            PsiManager.getInstance(project).findDirectory(dir)
        }
    )

/**
 * Returns the parent element of the given [PsiElement] acquiring the read lock to do so
 * if necessary.
 */
fun PsiElement.getPsiParentSafely(): PsiElement? =
    ApplicationManager.getApplication().runReadAction(
        Computable { parent }
    )

/**
 * This is similar to [com.intellij.psi.util.PsiTreeUtil.getParentOfType] with the addition
 * that the method uses the UAST tree (if available) as a fallback mechanism. This is useful if `element` originates
 * from a Kotlin [PsiFile].
 */
@Contract("null, _, _ -> null")
fun <T : PsiElement> PsiElement?.getPsiParentOfType(
    parentClass: Class<T>,
    strict: Boolean,
): T? {
    if (this == null) {
        return null
    }
    val parentElement = PsiTreeUtil.getParentOfType(this, parentClass, strict)
    if (parentElement != null) {
        return parentElement
    }

    // UElement heritage tree is not necessarily a subset of the corresponding PsiElement tree
    // e.g. if PsiIdentifier has PsiMethod as parent, converting it to UElement gives us a UIdentifier with null parent
    var psiElement = this
    while (psiElement != null) {
        val uElement = project.getService(UastFacade::class.java)
            .convertElementWithParent(psiElement, UElement::class.java)
        if (uElement != null) {
            val parentPsiElement = uElement.getPsiParentOfType(parentClass, strict && psiElement === this)
            if (parentPsiElement != null) {
                return parentPsiElement
            }
        }
        psiElement = psiElement.parent
    }
    return null
}

/**
 * Returns a lazy [Sequence] of parent elements of the given type, using the UAST tree as a fallback mechanism.
 *
 * This is similar to [com.intellij.psi.util.PsiTreeUtilKt.parentsOfType] but uses
 * [.getPsiParentOfType] from this class, which means it uses UAST where necessary.
 *
 * @see getPsiParentOfType
 * @see com.intellij.psi.util.PsiTreeUtilKt.parentsOfType
 */
fun <T : PsiElement> PsiElement.getPsiParentsOfType(
    parentClass: Class<T>,
    strict: Boolean,
): Sequence<T> = generateSequence(getPsiParentOfType(parentClass, strict)) { e ->
    getPsiParentOfType(parentClass, true)
}

/**
 * This is similar to [UastUtils.getParentOfType], except `parentClass`
 * is of type [PsiElement] instead of [UElement].
 */
@Contract("null, _, _ -> null")
fun <T : PsiElement> UElement?.getPsiParentOfType(
    parentClass: Class<T>,
    strict: Boolean,
): T? {
    var elem: UElement? = this ?: return null
    if (strict) {
        elem = elem?.uastParent
    }
    while (elem != null) {
        val psiElement: PsiElement? = getAsJavaPsiElement(parentClass)
        if (psiElement != null) {
            return parentClass.cast(psiElement)
        }
        elem = elem.uastParent
    }
    return null
}

/**
 * Returns true if the given PsiElement is a reference to an Android Resource.
 * The element can either be an identifier such as y in R.x.y, or the expression R.x.y itself.
 */
val PsiElement.isResourceReference: Boolean get() = getResourceReferenceType() != ResourceReferenceType.NONE

/**
 * Returns the type of resource reference for the given PSiElement; for R fields and android.R
 * fields it will return [ResourceReferenceType.APP] and [ResourceReferenceType.FRAMEWORK]
 * respectively, and otherwise it returns [ResourceReferenceType.NONE].
 *
 *
 * The element can either be an identifier such as y in R.x.y, or the expression R.x.y itself.
 */
fun PsiElement.getResourceReferenceType(): ResourceReferenceType {
    if (this is PsiReferenceExpression) {
        return getResourceReferenceType()
    }
    return if (this is PsiIdentifier && parent is PsiReferenceExpression) {
        (parent as PsiReferenceExpression).getResourceReferenceType()
    } else ResourceReferenceType.NONE
}

/**
 * Returns the resource name; e.g. for "MR.string.foo" it returns "foo".
 * NOTE: This method should only be called for elements **known** to be
 * resource references!
 */
val PsiElement.resourceName: String get() {
    assert(isResourceReference)
    if (this is PsiReferenceExpression) {
        val name = referenceName
        if (name != null) {
            return name
        }
    }
    return text
}

/*fun PsiReferenceExpression.getResourceReferenceType(): ResourceReferenceType {
    val resolvedElement = resolve() ?: return ResourceReferenceType.NONE

    // Examples of valid resources references are my.package.MR.strings.app_name or my.package.MR.colors.my_black
    // First parent is the resource type - e.g. string or color, etc.
    val elementType = resolvedElement.parent as? PsiClass ?: return ResourceReferenceType.NONE

    // Second parent is the package
    val elementPackage = elementType.parent as? PsiClass ?: return ResourceReferenceType.NONE
    return if (R_CLASS.equals(elementPackage.name)) {
        ResourceReferenceType.APP
    } else ResourceReferenceType.NONE
}

/** Returns the [ResourceType] given a PSI reference to a moko resource.  */
fun PsiElement.getResourceType(): ResourceType? {
    if (!isResourceReference) {
        return null
    }
    val exp =
        if (this is PsiReferenceExpression) this else (parent as PsiReferenceExpression)
    val resolvedElement = exp.resolve() ?: return null
    val elemParent = resolvedElement.parent
    return if (elemParent !is PsiClass) {
        null
    } else ResourceType.fromClassName(elemParent.name)
}*/

/**
 * Returns the [PsiClass.getQualifiedName] and acquires a read lock
 * if necessary
 *
 * @param psiClass the class to look up the qualified name for
 * @return the qualified name, or null
 */
fun PsiClass.getQualifiedNameSafely(): String? =
    if (ApplicationManager.getApplication().isReadAccessAllowed) {
        qualifiedName
    } else {
        ApplicationManager.getApplication().runReadAction(
            Computable { qualifiedName })
    }

/**
 * Returns the value of the given tag's attribute and acquires a read lock if necessary
 *
 * @return the attribute value, or null
 */
fun XmlTag.getAttributeSafely(namespace: String?, name: String): String? =
    if (ApplicationManager.getApplication().isReadAccessAllowed) {
        getAttributeValue(name, namespace)
    } else {
        ApplicationManager.getApplication().runReadAction(
            Computable { getAttributeValue(name, namespace) })
    }

fun XmlTag.isValidSafely(): Boolean = if (ApplicationManager.getApplication().isReadAccessAllowed) {
        isValid
    } else {
        ApplicationManager.getApplication().runReadAction(Computable { isValid })
    }

/** Returns a modification tracker which tracks changes only to physical XML PSI.  */
fun getXmlPsiModificationTracker(project: Project): ModificationTracker {
    val psiTracker = PsiManager.getInstance(project).modificationTracker as PsiModificationTrackerImpl
    return psiTracker.forLanguage(XMLLanguage.INSTANCE)
}

/**
 * Returns a modification tracker which tracks changes to all physical PSI *except* XML PSI.
 */
fun getPsiModificationTrackerIgnoringXml(project: Project): ModificationTracker {
    // Note: we also ignore the Language.ANY modification count, because that modification count
    // is incremented unconditionally on every PSI change (see PsiModificationTrackerImpl#incLanguageCounters).
    val psiTracker = PsiManager.getInstance(project).modificationTracker as PsiModificationTrackerImpl
    return psiTracker.forLanguages { lang: Language ->
        !lang.`is`(
            XMLLanguage.INSTANCE
        ) && !lang.`is`(Language.ANY)
    }
}

fun PsiClass.toPsiType(): PsiType {
    return JavaPsiFacade.getElementFactory(project).createType(this)
}

/** Type of resource reference: MR.type.name or none  */
enum class ResourceReferenceType {
    NONE,
    APP,
}
