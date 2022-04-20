package io.yapix.parse.util

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiArrayType
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiPrimitiveType
import com.intellij.psi.PsiType
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
     * 是否是原生类型
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
