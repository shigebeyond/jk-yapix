package io.yapix.parse.parser

import com.google.common.base.Splitter
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import io.yapix.model.Value
import io.yapix.parse.constant.DocumentTags
import io.yapix.parse.constant.JavaConstants
import io.yapix.parse.constant.SpringConstants
import io.yapix.parse.util.*
import io.yapix.parse.util.doc.PsiDocCommentHelperProxy.getDocCommentTagText
import io.yapix.parse.util.doc.PsiDocCommentHelperProxy.getDocCommentTitle
import io.yapix.parse.util.doc.PsiDocCommentHelperProxy.hasTagByName
import net.jkcode.jkutil.common.camel2Underline
import org.apache.commons.lang3.StringUtils
import java.util.*
import java.util.stream.Collectors

/**
 * 解析辅助工具类
 */
class ParseHelper(private val project: Project, private val module: Module) {
    //----------------------- 方法相关 ------------------------------------//
    /**
     * 获取方法描述
     */
    fun getMethodDescription(psiMethod: PsiMethod): String? {
        return getDocCommentTitle(psiMethod)
    }

    //----------------------- 接口Api相关 ------------------------------------//
    /**
     * 获取接口分类
     */
    fun getDeclareApiCategory(psiClass: PsiClass?): String? {
        // 优先级: 文档注释标记@menu > @Api > 文档注释第一行
        var category = getDocCommentTagText(psiClass!!, DocumentTags.Category)
        if (category.isNullOrEmpty()) {
            category = PsiSwaggerUtils.getApiCategory(psiClass)
        }
        if (category.isNullOrEmpty()) {
            category = getDocCommentTitle(psiClass)
        }
        return category
    }

    /**
     * 获得默认分类
     */
    fun getDefaultApiCategory(psiClass: PsiClass): String {
        return psiClass.name!!.camel2Underline()
    }

    /**
     * 获取接口概述
     */
    fun getApiSummary(psiMethod: PsiMethod): String? {
        // 优先级: swagger注解@ApiOperation > 文档注释标记@description >  文档注释第一行
        var summary = PsiSwaggerUtils.getApiSummary(psiMethod)
        if (summary.isNullOrEmpty()) {
            val tags = arrayOf(DocumentTags.Description, DocumentTags.DescriptionYapiUpload)
            for (tag in tags) {
                summary = getDocCommentTagText(psiMethod, tag)
                if (!summary.isNullOrEmpty())
                    break
            }
        }
        if (summary.isNullOrEmpty()) {
            summary = getDocCommentTitle(psiMethod)
        }
        return summary?.trim()
    }

    /**
     * 获取接口描述
     */
    fun getApiDescription(psiMethod: PsiMethod): String {
        var description = psiMethod.text
        if (psiMethod.body != null) {
            description = description.replace(psiMethod.body!!.text, "")
        }
        // 去掉函数实现,即{}包住的代码块
        val idxComment = description.indexOf("*/") // 注释结束
        if(idxComment != -1) { // 有注释
            val idxImpl = description.indexOf('{', idxComment + 2)
            description = description.substring(0, idxImpl).trim()
        }
        description = description.replace("<", "&lt;").replace(">", "&gt;")
        return "   <pre><code>    $description</code></pre>"
    }

    /**
     * 获取接口是否标记过期
     */
    fun getApiDeprecated(method: PsiMethod): Boolean {
        // 检查注解
        val annotation = PsiAnnotationUtils.getAnnotation(method, JavaConstants.Deprecate)
        if (annotation != null)
            return true

        // 检查注释
        return hasTagByName(method, DocumentTags.Deprecated)
    }

    /**
     * 获取接口标签
     */
    fun getApiTags(method: PsiMethod): List<String> {
        val tagsContent = getDocCommentTagText(method, DocumentTags.Tags) ?: return emptyList()
        val list = tagsContent.split(',') as MutableList
        list.remove("") // 删除空的
        return list
    }

    //------------------------ 参数Parameter ----------------------------//
    /**
     * 获取参数描述
     */
    fun getParameterDescription(parameter: PsiParameter, paramTagMap: Map<String?, String?>, values: List<Value>?): String? {
        // @ApiParam > @param
        var summary = PsiSwaggerUtils.getParameterDescription(parameter)
        if (summary.isNullOrEmpty()) {
            summary = paramTagMap[parameter.name]
        }
        if (values != null && values.isNotEmpty()) {
            val valuesText = values.joinToString {
                it.text
            }
            if (summary.isNullOrEmpty()) {
                summary = valuesText
            } else {
                summary += " ($valuesText)"
            }
        }
        return summary?.trim()
    }

    /**
     * 获取参数是否必填
     */
    fun getParameterRequired(parameter: PsiParameter): Boolean? {
        val annotations = arrayOf(JavaConstants.NotNull, JavaConstants.NotBlank, JavaConstants.NotEmpty)
        return annotations.any{ annotation ->
            PsiAnnotationUtils.hasAnnotation(parameter, annotation)
        }
    }

    /**
     * 获取参数可能的值
     */
    fun getParameterValues(parameter: PsiParameter): List<Value>? {
        return getTypeValues(parameter.type)
    }

    /**
     * 获取枚举值列表
     */
    fun getEnumValues(psiClass: PsiClass): List<Value> {
        return psiClass.fields.mapNotNull { field ->
            if(field is PsiEnumConstant){
                val name = field.name
                val description = getDocCommentTitle(field)
                Value(name, description)
            }else
                null
        }
    }
    //---------------------- 字段相关 ------------------------------//
    /**
     * 获取字段名
     */
    fun getFieldName(field: PsiField): String {
        val property = PsiAnnotationUtils.getStringAttributeValue(field, SpringConstants.JsonProperty)
        return if (property.isNullOrBlank()) field.name else property
    }

    /**
     * 获取字段描述
     */
    fun getFieldDescription(field: PsiField, values: List<Value>?): String? {
        // 优先级: @ApiModelProperty > 文档注释标记@description >  文档注释第一行
        var summary = PsiSwaggerUtils.getFieldDescription(field)
        if (summary.isNullOrEmpty()) {
            summary = getDocCommentTitle(field)
        }

        // 枚举
        if (!values.isNullOrEmpty()) {
            val valuesText = values.joinToString {
                it.text
            }
            if (summary.isNullOrEmpty()) {
                summary = valuesText
            } else {
                summary += " ($valuesText)"
            }
        } else {
            summary = (summary ?: "") + " " + PsiLinkUtils.getLinkRemark(field)
        }
        return summary?.trim()
    }

    /**
     * 字段是否必填
     */
    fun getFieldRequired(field: PsiField): Boolean {
        val annotations = arrayOf(JavaConstants.NotNull, JavaConstants.NotBlank, JavaConstants.NotEmpty)
        return annotations.any { annotation ->
            PsiAnnotationUtils.hasAnnotation(field, annotation)
        }
    }

    /**
     * 获取字段可能的值
     */
    fun getFieldValues(field: PsiField): List<Value>? {
        return getTypeValues(field.type)
    }

    /**
     * 是否标记过期
     */
    fun getFieldDeprecated(field: PsiField): Boolean {
        val annotation = PsiAnnotationUtils.getAnnotation(field, JavaConstants.Deprecate)
        if (annotation != null)
            return true

        return hasTagByName(field, DocumentTags.Deprecated)
    }

    /**
     * 字段是否被跳过
     */
    fun isFieldIgnore(field: PsiField): Boolean {
        // swagger -> @ignore -> @JsonIgnore
        if (PsiSwaggerUtils.isFieldIgnore(field))
            return true

        if (hasTagByName(field, DocumentTags.Ignore))
            return true

        return "true" == PsiAnnotationUtils.getStringAttributeValue(field, SpringConstants.JsonIgnore)
    }

    //----------------------------- 类型 -----------------------------//
    fun getTypeDescription(type: PsiType?, values: List<Value>?): String? {
        if (values != null && values.isNotEmpty())
            return values.joinToString {
                it.text
            }

        return type?.presentableText
    }

    /**
     * 获取指定类型可能的值
     */
    fun getTypeValues(psiType: PsiType): List<Value>? {
        val isEnum = PsiTypeUtils.isEnum(psiType)
        if (isEnum) {
            val enumPsiClass = PsiTypeUtils.getEnumClassIncludeArray(project, module, psiType)
            if (enumPsiClass != null)
                return getEnumValues(enumPsiClass)
        }

        return null
    }

}
