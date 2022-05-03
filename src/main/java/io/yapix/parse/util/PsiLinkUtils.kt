package io.yapix.parse.util

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassOwner
import com.intellij.psi.PsiDocCommentOwner
import com.intellij.psi.PsiField
import com.intellij.psi.impl.source.PsiClassImpl
import io.yapix.parse.util.doc.JavaPsiDocCommentHelper

object PsiLinkUtils {

    /**
     * 获得link的类全路径
     */
    public fun getLinkFullPath(element: PsiDocCommentOwner): String? {
        return getLinkClass(element)?.name
    }

    /**
     * 获得link的类
     *   要修复类的全路径
     * @param element
     * @param classPath 类路径，但不一定是全路径
     */
    public fun getLinkClass(element: PsiDocCommentOwner, classPath: String? = JavaPsiDocCommentHelper.getLinkText(element)): PsiClass? {
        if (classPath == null)
            return null

        val classPath = classPath.substringBefore('<') // 去掉泛型

        // 1 直接全路径
        val project = element.project
        var psiClass = PsiUtils.findPsiClass(project, null, classPath)
        if (psiClass != null)
            return psiClass

        // 2 非全路径，尝试获得全路径
        // 2.1 处理import的类
        val imports = PsiUtils.getImportsInFile(element.containingFile)

        // 将类路径拆为2片段
        val parts = classPath.split('.', limit = 2)
        val key = parts[0] // 要跟import对比的片段
        var rest = "" // 剩余片段
        if (parts.size > 1)
            rest = '.' + parts[1] // 剩下部分

        // 命中import的类
        if (key in imports) {
            // 加上import的全路径
            val fullPath = imports[key] + rest
            psiClass = PsiUtils.findPsiClass(project, null, fullPath)
        }

        // 2.2 处理同包的类
        if (psiClass == null) {
            // 加上包名的全路径
            val fullPath = PsiUtils.getPakcageName(element) + "." + classPath
            psiClass = PsiUtils.findPsiClass(project, null, fullPath)
        }

        // 2.3 处理java.lang包的类短名: 如果类不存在(如String)，则尝试加上 java.lang.，变为 java.lang.String
        if(psiClass == null && !classPath.contains('.')){
            val fullPath = "java.lang." + classPath
            psiClass = PsiUtils.findPsiClass(project, null, fullPath)
        }

        // 2.4 处理java.util包的类短名: 如果类不存在(如List)，则尝试加上 java.util.，变为 java.util.List
        if(psiClass == null && !classPath.contains('.')){
            val fullPath = "java.util." + classPath
            psiClass = PsiUtils.findPsiClass(project, null, fullPath)
        }

        return psiClass
    }

    /**
     * 获得link的类备注
     *   类名+所有字段描述
     */
    fun getLinkRemark(field: PsiField): String {
        // 获得类
        val psiClass = getLinkClass(field)
        if (psiClass == null)
            return ""

        // 返回类名+所有字段描述
        return psiClass.fields.joinToString(", ", psiClass.name + "[", "]"){
            PsiFieldUtils.getFieldRemark(field)
        }
    }
}
