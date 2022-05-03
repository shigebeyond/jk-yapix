package io.yapix.parse.util

import com.google.common.collect.Lists
import com.google.gson.GsonBuilder
import com.intellij.psi.*
import com.intellij.psi.impl.JavaConstantExpressionEvaluator
import io.yapix.parse.util.PsiFieldUtils.getFieldDefaultValue

/**
 * 注解相关工具类
 */
object PsiAnnotationUtils {

    val gson = GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create()

    /**
     * 有指定注解
     */
    fun hasAnnotation(element: PsiModifierListOwner, fqn: String): Boolean {
        return getAnnotation(element, fqn) != null
    }

    /**
     * 获取指定注解
     */
    @JvmStatic
    fun getAnnotation(element: PsiModifierListOwner, fqn: String): PsiAnnotation? {
        return element.getAnnotation(fqn)
    }

    /**
     * 获取指定注解
     */
    fun getAnnotationIncludeExtends(element: PsiClass, fqn: String): PsiAnnotation? {
        var annotation = element.getAnnotation(fqn)
        if (annotation == null) {
            for (type in element.extendsListTypes) {
                val psiClass = type.resolve() ?: continue
                annotation = psiClass.getAnnotation(fqn)
                if (annotation != null)
                    break
            }
        }
        return annotation
    }

    /**
     * 获取指定元素注解的value属性值
     */
    fun getStringAttributeValue(element: PsiModifierListOwner, fqn: String): String? {
        return getStringAttributeValue(element, fqn, "value")
    }

    /**
     * 获取指定元素注解的某个属性值
     */
    @JvmStatic
    fun getStringAttributeValue(element: PsiModifierListOwner, fqn: String, attribute: String?): String? {
        val annotation = getAnnotation(element, fqn) ?: return null
        return getStringAttributeValueByAnnotation(annotation, attribute)
    }

    /**
     * 获取指定元素注解的某个属性值
     */
    @JvmStatic
    fun getStringAttributeValueByAnnotation(annotation: PsiAnnotation, attribute: String?): String? {
        val attributeValue = annotation.findAttributeValue(attribute) ?: return null
        return getAnnotationMemberValue(attributeValue)
    }

    /**
     * 获取指定元素注解的某个属性值
     */
    @JvmStatic
    fun getStringAttributeValueByAnnotation(annotation: PsiAnnotation): String? {
        return getStringAttributeValueByAnnotation(annotation, "value")
    }

    @JvmStatic
    fun getStringArrayAttribute(annotation: PsiAnnotation, attribute: String?): List<String?> {
        val memberValue = annotation.findAttributeValue(attribute) ?: return emptyList<String>()
        val paths: MutableList<String?> = Lists.newArrayListWithExpectedSize(1)
        if (memberValue is PsiArrayInitializerMemberValue) {
            val values = memberValue.initializers
            for (value in values) {
                val text = getAnnotationMemberValue(value)
                paths.add(text)
            }
        } else {
            val text = getAnnotationMemberValue(memberValue)
            paths.add(text)
        }
        return paths
    }

    /**
     * 获取注解值
     */
    fun getAnnotationMemberValue(memberValue: PsiAnnotationMemberValue): String? {
        if (memberValue is PsiExpression) {
            val constant = JavaConstantExpressionEvaluator.computeConstantExpression(memberValue, false)
            return constant?.toString()
        }

        val reference = memberValue.reference
        if (reference == null)
            return ""

        val resolve = reference.resolve()
        if (resolve is PsiEnumConstant) // 枚举常量
            return resolve.name

        if (resolve is PsiField) // 引用其他字段
            return getFieldDefaultValue(resolve)

        return ""
    }

    /**
     * 获得注解的备注，包含注解的属性名与属性值
     *    如 @net.jkcode.jksoa.rpc.client.combiner.annotation.GroupCombine={"batchMethod":"listUsersByNameAsync","reqArgField":"name","respField":"","one2one":"true","flushQuota":"100","flushTimeoutMillis":"100"}
     */
    fun getAnnotationRemark(ann: PsiAnnotation): String? {
        // 1 注解的代码，如 @GroupCombine("listUsersByNameAsync", "name", "", true, 100, 100)
        //return ann.text

        // 2 包含注解的属性名与属性值, 如 @net.jkcode.jksoa.rpc.client.combiner.annotation.GroupCombine{"batchMethod":"listUsersByNameAsync","reqArgField":"name","respField":"","one2one":"true","flushQuota":"100","flushTimeoutMillis":"100"}
        if(ann.qualifiedName == null)
            return null
        val attrs = getAnnotationAttributeMap(ann, true)
        return "@" + ann.qualifiedName + "=" + gson.toJson(attrs)
    }

    /**
     * 获得注解实例的属性值
     * @param ann
     * @param withDefault 是否包含 未填但有默认值的属性
     * @return Map<属性名, 属性值>
     */
    public fun getAnnotationAttributeMap(ann: PsiAnnotation, withDefault: Boolean): Map<String?, String?> {
        // 1 填写值的属性，不包含[未填但有默认值的属性]
        if(!withDefault)
            return ann.parameterList.attributes.associate {
            it.name to it.literalValue
        }

        // 2 所有属性(包含默认值的属性)
        val annClass = PsiUtils.findPsiClass(ann.project, null, ann.qualifiedName!!)!!
        // 方法名即属性名
        return annClass.methods.associate { method ->
            val attrName = method.name
            attrName to ann.findAttributeValue(attrName)?.text
        }
    }
}
