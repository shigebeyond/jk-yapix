package io.yapix.parse.util

import com.intellij.psi.PsiEnumConstant
import com.intellij.psi.PsiField
import com.intellij.psi.PsiPrimitiveType
import com.intellij.psi.impl.JavaConstantExpressionEvaluator
import com.intellij.psi.util.PsiTypesUtil
import io.yapix.parse.util.doc.PsiDocCommentHelperProxy

object PsiFieldUtils {

    /**
     * 获得字段描述
     *    字段名:字段值(字段描述)
     */
    @JvmStatic
    fun getFieldRemark(field: PsiField): String {
        val name = field.name
        val value = getFieldDefaultValue(field)
        var title = PsiDocCommentHelperProxy.getDocCommentTitle(field)
        if (!title.isNullOrBlank()) {
            title = "($title)"
        }
        return "$name:$value$title"
    }

    /**
     * 获取字段默认值
     */
    @JvmStatic
    fun getFieldDefaultValue(field: PsiField): String? {
        val fieldType = field.type
        val initializer = field.initializer
        if (initializer == null) {
            // 可能解析不到从文本解析
            val dv = getDefaultValueFromFieldText(field)
            if (dv != null)
                return dv

            if (fieldType is PsiPrimitiveType)
                return PsiTypesUtil.getDefaultValueOfType(fieldType)

            return null
        }

        val reference = initializer.reference
        if (reference == null) {
            val constant = JavaConstantExpressionEvaluator.computeConstantExpression(initializer, false)
            return constant?.toString()
        }

        val resolve = reference.resolve()
        if (resolve is PsiEnumConstant)  // 枚举常量
            return resolve.name

        if (resolve is PsiField) // 引用其他字段
            return getFieldDefaultValue(resolve)

        return null
    }

    /**
     * 从字段声明的代码中，获得字段声明的默认值
     *   如 private int page = 1;
     */
    private fun getDefaultValueFromFieldText(field: PsiField): String? {
        var text = field.text // 字段声明的代码
        text = text.substringAfter(field.name)
        // 从字段文本解析出默认值: private int page = 1;
        // 暂时不处理枚举类
        val eqIdx = text.lastIndexOf('=')
        if (eqIdx == -1)
            return null

        val subText = text.substring(eqIdx + 1).trim() // 要去掉前后多余的空格
        var beginIdx = 0
        var endIdx = subText.length
        if (subText.endsWith(";")) {
            endIdx--
        }
        if (subText.startsWith("\"")) {
            beginIdx++
            endIdx--
        }
        return subText.substring(beginIdx, endIdx)
    }
}
