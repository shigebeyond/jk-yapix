package io.yapix.parse.util.doc

import com.intellij.psi.PsiDocCommentOwner
import org.jetbrains.kotlin.idea.caches.resolve.util.getJavaOrKotlinMemberDescriptor
import org.jetbrains.kotlin.idea.kdoc.findKDoc
import org.jetbrains.kotlin.kdoc.psi.impl.KDocSection
import org.jetbrains.kotlin.kdoc.psi.impl.KDocTag

/**
 * 获得kotlin文档
 */
public fun PsiDocCommentOwner.findKDoc(): KDocSection?{
    return this.getJavaOrKotlinMemberDescriptor()!!.findKDoc() as KDocSection?
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
