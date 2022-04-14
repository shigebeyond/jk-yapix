package io.yapix.parse

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import io.yapix.config.YapixConfig
import io.yapix.parse.model.ClassParseData
import io.yapix.parse.model.MethodParseData
import io.yapix.parse.parser.spring.SpringApiParser
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult

/**
 * 组合的Api接口解析器
 *   包含spring + jkmvc的解析器
 */
class CompositeApiParser(project: Project, module: Module, settings: YapixConfig) : IApiParser {

    /**
     * 子解析器： spring + jkmvc
     */
    private val subApiParsers: Array<IApiParser> = arrayOf(SpringApiParser(project, module, settings))

    /**
     * 解析方法
     */
    override fun parse(method: PsiMethod): MethodParseData? {
        // 取子解析器中第一个解析成功的
        return subApiParsers.firstNotNullResult {parser ->
            parser.parse(method)
        }
    }

    /**
     * 解析接口(controller)
     */
    override fun parse(psiClass: PsiClass): ClassParseData? {
        // 取子解析器中第一个解析成功的
        return subApiParsers.firstNotNullResult {parser ->
            parser.parse(psiClass)
        }
    }


}
