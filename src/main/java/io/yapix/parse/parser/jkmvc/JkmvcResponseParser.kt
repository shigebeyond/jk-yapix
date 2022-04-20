package io.yapix.parse.parser.jkmvc

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiMethod
import io.yapix.config.YapixConfig
import io.yapix.model.Property
import io.yapix.parse.constant.DocumentTags
import io.yapix.parse.parser.ResponseParser
import io.yapix.parse.util.doc.KtPsiDocCommentHelper
import io.yapix.parse.util.doc.PsiDocCommentHelperProxy

/**
 * 响应解析
 *  1 先尝试解析注释
 *  2 再解析方法返回值
 */
class JkmvcResponseParser(project: Project, module: Module, settings: YapixConfig): ResponseParser(project, module, settings) {

    override fun parse(method: PsiMethod): Property? {
        // 1 先尝试解析注释
        val type = PsiDocCommentHelperProxy.getReturnTagLinkText(method)// 获得 @return 标记 中link的类
        if(type != null) // 解析
            return kernelParser.parseType(null, type, method)

        // 2 再解析方法返回值
        return super.parse(method)
    }
}
