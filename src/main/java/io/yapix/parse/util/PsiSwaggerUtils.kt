package io.yapix.parse.util

import com.intellij.codeInsight.AnnotationUtil
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiParameter
import io.yapix.parse.constant.SwaggerConstants
import io.yapix.parse.util.PsiAnnotationUtils.getAnnotation
import io.yapix.parse.util.PsiAnnotationUtils.getStringAttributeValueByAnnotation

/**
 * Swagger解析相关工具
 */
object PsiSwaggerUtils {

    fun getApiCategory(psiClass: PsiClass): String? {
        val api = getAnnotation(psiClass, SwaggerConstants.Api) ?: return null
        return getStringAttributeValueByAnnotation(api, "tags")
    }

    fun getApiSummary(psiMethod: PsiMethod): String? {
        val apiOperation = getAnnotation(psiMethod, SwaggerConstants.ApiOperation) ?: return null
        return getStringAttributeValueByAnnotation(apiOperation)
    }

    fun getParameterDescription(psiParameter: PsiParameter): String? {
        val apiParam = getAnnotation(psiParameter, SwaggerConstants.ApiParam) ?: return null
        return getStringAttributeValueByAnnotation(apiParam)
    }

    fun getFieldDescription(psiField: PsiField): String? {
        val apiModelProperty = getAnnotation(psiField, SwaggerConstants.ApiModelProperty) ?: return null
        return getStringAttributeValueByAnnotation(apiModelProperty)
    }

    fun isFieldIgnore(psiField: PsiField): Boolean {
        val apiModelProperty = getAnnotation(psiField!!, SwaggerConstants.ApiModelProperty) ?: return false
        val hidden = AnnotationUtil.getBooleanAttributeValue(apiModelProperty, "hidden")
        return java.lang.Boolean.TRUE == hidden
    }

}
