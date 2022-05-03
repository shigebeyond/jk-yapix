package io.yapix.parse.util

import com.intellij.lang.jvm.JvmModifier
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.impl.source.PsiJavaFileImpl
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache
import net.jkcode.jkutil.common.getFunction
import org.jetbrains.kotlin.asJava.elements.FakeFileForLightClass

object PsiUtils {

    /**
     * 获得目录路径
     */
    public fun getDirectory(element: PsiDocCommentOwner): String {
        return element.containingFile.containingDirectory.toString() // PsiDirectory:目录路径
            .substringAfter(':') // 目录路径
    }

    /**
     * 获得包名
     */
    public fun getPakcageName(element: PsiDocCommentOwner): String {
        val file = element.containingFile
        if(file is PsiJavaFileImpl)
            return file.packageName

        if(file is FakeFileForLightClass)
            return file.packageName

        //反射调用
        return file::class.getFunction("getPackageName")!!.call(file) as String
    }

    /**
     * 获得import列表
     * @param file
     * @return Map<类短名, 类全名>
     */
    public fun getImportsInFile(file: PsiFile): Map<String, String>{
        var r: Map<String, String>? = null
        if(file is PsiJavaFileImpl)
            r = file.importList?.allImportStatements?.associate {
                val ref = it.importReference!!
                ref.referenceName!! to ref.qualifiedName!!
            }
        else if(file is FakeFileForLightClass)
            r = file.ktFile.importList?.imports?.associate {
                it.importedName.toString() to it.importedFqName.toString()
            }

        return r ?: emptyMap()
    }


    /**
     * 获得非静态字段
     */
    fun getFields(t: PsiClass): List<PsiField> {
        return t.allFields.filter { field ->
            !field.hasModifier(JvmModifier.STATIC) // 非静态字段
        }
    }

    /**
     * 根据类短名来获取PsiClass, 而非类全限定名
     * 优先从当前模块依赖, 其次当前工程作用域
     */
    fun findPsiClassByShortName(project: Project, module: Module?, shortName: String): PsiClass? {
        var psiClass: PsiClass? = null
        if (module != null) {
            psiClass = PsiShortNamesCache.getInstance(project)
                .getClassesByName(shortName, GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module, false))
                .firstOrNull()
        }
        if (psiClass == null) {
            psiClass = PsiShortNamesCache.getInstance(project)
                .getClassesByName(shortName, GlobalSearchScope.projectScope(project))
                .firstOrNull()
        }
        return psiClass
    }

    /**
     * 根据类全限定名获取PsiClass
     * 优先从当前模块依赖, 其次当前工程作用域
     */
    @JvmStatic
    fun findPsiClass(project: Project, module: Module?, qualifiedName: String): PsiClass? {
        var psiClass: PsiClass? = null
        if (module != null) {
            psiClass = JavaPsiFacade.getInstance(project).findClass(qualifiedName, GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module))
        }
        if (psiClass == null) {
            psiClass = JavaPsiFacade.getInstance(project).findClass(qualifiedName, GlobalSearchScope.allScope(project))
        }
        return psiClass
    }

    /**
     * 获得getter方法
     * @param psiClass
     * @param onlyThisClassMethod 是否仅限于当前类的方法，否则包含父类的方法
     * @return
     */
    fun getGetterMethods(psiClass: PsiClass, onlyThisClassMethod: Boolean): List<PsiMethod> {
        val methods = if (onlyThisClassMethod) psiClass.methods else psiClass.allMethods
        return methods.filter { method: PsiMethod ->
            val methodName = method.name
            val returnType = method.returnType
            val modifierList = method.modifierList
            val isAccessMethod = (!modifierList.hasModifierProperty("static")
                && methodName != "getClass"
                && (methodName.startsWith("get") && methodName.length > 3 && returnType != null
                || methodName.startsWith("is") && methodName.length > 2 && returnType != null && returnType
                .canonicalText == "boolean"))
            isAccessMethod
        }
    }

    /**
     * 获取枚举字段名
     */
    fun getEnumFieldNames(psiClass: PsiClass): List<String> {
        return psiClass.fields.mapNotNull { field ->
            if (field is PsiEnumConstant) // 只收集 PsiEnumConstant 的字段名
                field.name
            else
                null
        }
    }
}
