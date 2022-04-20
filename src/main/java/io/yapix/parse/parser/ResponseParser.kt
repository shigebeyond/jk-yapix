package io.yapix.parse.parser

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiType
import com.intellij.psi.util.PsiTypesUtil
import io.yapix.config.YapixConfig
import io.yapix.model.Property
import io.yapix.parse.util.PsiGenericUtils
import io.yapix.parse.util.PsiTypeUtils
import io.yapix.parse.util.PsiUtils
import org.apache.commons.lang3.StringUtils

/**
 * 响应解析
 *    主要是解析方法返回值
 *
 * @see .parse
 */
open class ResponseParser(private val project: Project, private val module: Module, private val settings: YapixConfig) {

    protected val kernelParser: KernelParser = KernelParser(project, module, settings, true)

    protected val parseHelper: ParseHelper = ParseHelper(project, module)

    /**
     * 解析方法响应数据
     *
     * @param method 待解析的方法
     */
    open fun parse(method: PsiMethod): Property? {
        val returnType = method.returnType ?: return null
        var type: PsiType? = returnType
        var typeText: String? = returnType.canonicalText
        val unwrappedType = getUnwrapType(returnType)
        if (unwrappedType != null) {
            // 需要解开包赚类处理
            val types = PsiGenericUtils.splitTypeAndGenericPair(unwrappedType)
            val psiClass = PsiUtils.findPsiClass(project, module, types[0])
            type = if (psiClass != null) PsiTypesUtil.getClassType(psiClass) else null
            typeText = unwrappedType
        } else {
            // 包装类处理
            val returnClass = getWrapperPsiClass(method)
            if (returnClass != null) {
                type = PsiTypesUtil.getClassType(returnClass)
                typeText = type.getCanonicalText() + "<" + returnType.canonicalText + ">"
            }
        }

        // 解析
        val item = kernelParser.parseType(type, typeText)
        if (item != null) {
            item.description = parseHelper.getTypeDescription(type, item.values)
        }
        return item
    }

    /**
     * 解开类型, 例如输入: ResponseEntity<User>, 那么应当处理类型: User
     */
    private fun getUnwrapType(type: PsiType): String? {
        // 获取类型：types[0]=原始类型, types[1]=泛型参数
        val types = PsiGenericUtils.splitTypeAndGenericPair(type.canonicalText)

        // 是解开包装类， 例如： ResponseEntity<User>,
        val unwrapOpt = settings.returnUnwrapTypes.any {
            it == types[0]
        }
        return if (unwrapOpt) {
            types[1]
        } else null
    }

    /**
     * 返回需要需要的包装类
     */
    private fun getWrapperPsiClass(method: PsiMethod): PsiClass? {
        if (StringUtils.isEmpty(settings.returnWrapType))
            return null

        val returnClass = PsiUtils.findPsiClass(project, module, settings.returnWrapType) ?: return null

        // 是否是byte[]
        val returnType = method.returnType!!
        if (PsiTypeUtils.isBytes(returnType))
            return null

        // 是否是相同类型
        val types = PsiGenericUtils.splitTypeAndGenericPair(returnType!!.canonicalText)
        val theReturnType = types[0]
        return if (theReturnType == returnClass.qualifiedName) null else returnClass
    }

}
