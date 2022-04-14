package io.yapix.parse

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import io.yapix.parse.model.ClassParseData
import io.yapix.parse.model.MethodParseData

interface IApiParser {
    /**
     * 解析方法
     */
    fun parse(method: PsiMethod): MethodParseData?

    /**
     * 解析接口(controller)
     */
    fun parse(psiClass: PsiClass): ClassParseData?
}
