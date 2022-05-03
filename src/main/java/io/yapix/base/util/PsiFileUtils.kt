package io.yapix.base.util

import com.google.common.collect.Lists
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassOwner
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiModifier
import java.util.*

object PsiFileUtils {
    /**
     * 获取Java文件
     */
    @JvmStatic
    fun getPsiClassFiles(project: Project?, psiFiles: Array<VirtualFile>): List<PsiClassOwner> {
        val files: MutableList<PsiClassOwner> = Lists.newArrayListWithExpectedSize(psiFiles.size)
        val psiManager = PsiManager.getInstance(project!!)
        for (f in psiFiles) {
            if (f.isDirectory) {
                val children = f.children
                val theFiles = getPsiClassFiles(project, children)
                files.addAll(theFiles)
                continue
            }
            val file = psiManager.findFile(f)
            // if (file instanceof PsiJavaFileImpl) { //只处理java文件
            if (file is PsiClassOwner) { //处理java+kotlin文件
                files.add(file)
            }
        }
        return files
    }

    /**
     * 获取PsiClass: 从类文件(java/kotlin)中取得类
     * @param psiClassFiles
     * @param allowInterface 是否包含接口
     */
    @JvmStatic
    fun getPsiClassByFile(psiClassFiles: List<PsiClassOwner>, allowInterface: Boolean): List<PsiClass> {
        return getPsiClassByFile(psiClassFiles){ o ->
            allowInterface || !o.isInterface
        }
    }

    /**
     * 获取PsiClass: 从类文件(java/kotlin)中取得类
     * @param psiClassFiles
     * @param filter 过滤回调
     */
    @JvmStatic
    fun getPsiClassByFile(psiClassFiles: List<PsiClassOwner>, filter: (PsiClass) -> Boolean): List<PsiClass> {
        val psiClassList: MutableList<PsiClass> = Lists.newArrayListWithCapacity(psiClassFiles.size)
        for (psiClassFile in psiClassFiles) {
            Arrays.stream(psiClassFile.classes) // 从类文件(java/kotlin)中取得类
                .filter { o: PsiClass ->
                    o.modifierList != null && o.modifierList!!.hasModifierProperty(PsiModifier.PUBLIC) // 公开类
                        && filter(o) // 过滤
                } // 公开类
                .findFirst().ifPresent { e: PsiClass -> psiClassList.add(e) }
        }
        return psiClassList
    }
}
