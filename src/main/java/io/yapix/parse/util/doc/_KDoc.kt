package io.yapix.parse.util.doc

import com.intellij.psi.PsiDocCommentOwner
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.source.PsiJavaFileImpl
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.idea.caches.resolve.util.getJavaOrKotlinMemberDescriptor
import org.jetbrains.kotlin.idea.kdoc.findKDoc
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.kdoc.psi.impl.KDocSection
import org.jetbrains.kotlin.kdoc.psi.impl.KDocTag
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile

/**
 * 获得kotlin文档
 */
/*public fun PsiDocCommentOwner.findKDoc(): KDocSection?{
    // 运行时有莫名其妙的错误：java.lang.AssertionError: Resolver for 'project source roots and libraries with settings=
    // https://intellij-support.jetbrains.com/hc/en-us/community/posts/4412499761042-PsiElementFinder-analogue-for-Kotlin
    return this.getJavaOrKotlinMemberDescriptor()!!.findKDoc() as KDocSection?
}*/
public fun PsiDocCommentOwner.findKDoc(): KDocSection? {
    //kotlin doc
    if (this is KtLightElement<*, *>) {
        val ko = this.kotlinOrigin
        if (ko is KtDeclaration)
            return ko.docComment?.getDefaultSection()

        return null
    }

    if (this is KtDeclaration)
        return (this as KtDeclaration).docComment?.getDefaultSection()

    return null
}


/**
 * 获得注释标签的内容
 */
public fun KDocTag.contentOrLink(): String? {
    val content = this.getContent()
    if (!content.isNullOrBlank())
        return content

    val link = this.getSubjectLink()?.text
    if(!link.isNullOrBlank())
        return link

    return null
}

/**
 * 获得import列表
 */
public fun PsiFile.importsInFile(): Map<String, String>{
    val file = this
    var r: Map<String, String>? = null
    if(file is PsiJavaFileImpl)
        r = file.importList?.allImportStatements?.associate {
            val path = it.importReference!!.qualifiedName
            path.substringBeforeLast('.') to path
        }
    else if(file is KtFile)
        r = file.importList?.imports?.associate {
            it.importPath!!.alias.toString() to it.importPath!!.fqName.toString()
        }

    return r ?: emptyMap()
}
