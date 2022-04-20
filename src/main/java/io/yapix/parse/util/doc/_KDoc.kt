package io.yapix.parse.util.doc

import com.intellij.psi.PsiDocCommentOwner
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.kdoc.psi.impl.KDocSection
import org.jetbrains.kotlin.kdoc.psi.impl.KDocTag
import org.jetbrains.kotlin.psi.KtDeclaration

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
 * 获得注释标记的内容
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
