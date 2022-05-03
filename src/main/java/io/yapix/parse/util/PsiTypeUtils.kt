package io.yapix.parse.util

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.impl.source.PsiClassReferenceType
import com.intellij.psi.util.PsiTypesUtil
import io.yapix.model.DataTypes
import io.yapix.parse.constant.JavaConstants
import io.yapix.parse.parser.DataTypeParser

/**
 * PsiType相关工具.
 */
object PsiTypeUtils {

    /**
     * 获得 PsiClassReferenceType/PsiType 引用的类
     */
    public fun getRefClass(type: PsiType?, element: PsiDocCommentOwner): String {
        if(type == null)
            return ""
        val clazz = type.toString() // PsiType:类
            .substringAfter(':') // 类
        // 原始类型
        if(PsiTypeUtils.isPrimitive(type))
            return clazz
        // 对象类型
        return PsiLinkUtils.getLinkClass(element, clazz)?.qualifiedName ?: "?"
    }

    /**
     * 原始类型映射
     */
    private val PRIMITIVE_TYPES: Map<String, PsiType> = mapOf(
        PsiType.VOID.canonicalText to PsiType.VOID,
        PsiType.BYTE.canonicalText to PsiType.BYTE,
        PsiType.CHAR.canonicalText to PsiType.CHAR,
        PsiType.DOUBLE.canonicalText to PsiType.DOUBLE,
        PsiType.FLOAT.canonicalText to PsiType.FLOAT,
        PsiType.LONG.canonicalText to PsiType.LONG,
        PsiType.INT.canonicalText to PsiType.INT,
        PsiType.SHORT.canonicalText to PsiType.SHORT,
        PsiType.BOOLEAN.canonicalText to PsiType.BOOLEAN,
    )

    /**
     * 是否是原始类型
     */
    fun isPrimitive(typeName: String): Boolean {
        return PRIMITIVE_TYPES.contains(typeName)
    }

    /**
     * 获得原始类型
     */
    fun getPrimitiveType(typeName: String): PsiType? {
        return PRIMITIVE_TYPES[typeName]
    }

    /**
     * 是否是原始类型
     */
    fun isPrimitive(type: PsiType): Boolean {
        return type is PsiPrimitiveType
    }

    /**
     * 是否为空类型
     */
    fun isVoid(name: String): Boolean {
        return name == "void" || name == "java.lang.Void"
    }

    /**
     * 是否是字节数组
     */
    fun isBytes(type: PsiType): Boolean {
        return "byte[]" == type.canonicalText
    }

    /**
     * 是否是数组类型
     */
    @JvmStatic
    fun isArray(type: PsiType): Boolean {
        return type is PsiArrayType
    }

    /**
     * 是否是集合类型或其子类型
     */
    @JvmStatic
    fun isCollection(type: PsiType, project: Project, module: Module?): Boolean {
        val mapPsiClass = PsiUtils.findPsiClass(project, module, JavaConstants.Collection)
        val mapPsiType = PsiTypesUtil.getClassType(mapPsiClass!!)
        return mapPsiType.isAssignableFrom(type)
    }

    /**
     * 是否是Map，以及其子类型
     */
    fun isMap(type: PsiType, project: Project, module: Module?): Boolean {
        val mapPsiClass = PsiUtils.findPsiClass(project, module, JavaConstants.Map)
        val mapPsiType = PsiTypesUtil.getClassType(mapPsiClass!!)
        return mapPsiType.isAssignableFrom(type)
    }

    /**
     * 是否是枚举类型
     */
    @JvmStatic
    fun isEnum(type: PsiType): Boolean {
        val psiClass = PsiTypesUtil.getPsiClass(type)
        return psiClass != null && psiClass.isEnum
    }

    /**
     * 获取枚举类
     */
    fun getEnumClassIncludeArray(project: Project, module: Module?, type: PsiType): PsiClass? {
        var enumType: PsiType? = null
        if (isEnum(type)) {
            enumType = type
        } else if (isArray(type)) {
            enumType = (type as PsiArrayType).componentType
        } else if (type is PsiClassReferenceType && isCollection(type, project, module)) {
            enumType = type.parameters.firstOrNull()
        }
        if (enumType == null)
            return null

        val enumClass = PsiUtils.findPsiClass(project, module, enumType.canonicalText)
        if (enumClass != null && enumClass.isEnum)
            return enumClass

        return null
    }

    /**
     * 是否是文件上传
     */
    @JvmStatic
    fun isFileIncludeArray(type: PsiType): Boolean {
        return DataTypes.FILE == DataTypeParser.getTypeInProperties(type)
    }
}
