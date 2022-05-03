package io.yapix.process.jksoa

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.psi.PsiClass
import com.intellij.psi.util.PsiTreeUtil
import io.yapix.action.EventData
import io.yapix.base.util.NotificationUtils
import io.yapix.base.util.PsiFileUtils
import io.yapix.parse.util.PsiUtils
import java.io.File

/**
 * 为jksoa的java接口类，生成php映射类，用于给php调用
 */
class JksoaPhpMappingAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val data = EventData.of(e)
        if (!data.shouldHandle())
            return

        // 尝试获得jksoa的rpc接口类
        var psiClasses: List<PsiClass>
        if(data.selectedClass != null) // 1 选中类
            psiClasses = listOf(data.selectedClass)
        else // 2 批量： 选中多个包或文件
            psiClasses = PsiFileUtils.getPsiClassByFile(data.selectedClassFiles){ psiClass ->
                isServiceClass(psiClass)
            }

        for(psiClass in psiClasses) {
            // 生成php映射类代码
            val phpCode = JksoaPhpMappingGenerator.generate(psiClass)

            // 写php文件
            val dir = PsiUtils.getDirectory(psiClass) // 目录
            File("$dir/${psiClass.name}.php").writeText(phpCode)
        }
        NotificationUtils.notifyInfo(ACTION_TEXT, ACTION_TEXT + " success")
    }

    override fun update(e: AnActionEvent) {
        e.presentation.text = ACTION_TEXT
        e.presentation.isEnabledAndVisible = isActionEnable(e)
    }

    /**
     * 是否启用该按钮
     */
    private fun isActionEnable(e: AnActionEvent): Boolean {
        val editor = e.dataContext.getData(CommonDataKeys.EDITOR)
        val editorFile = e.dataContext.getData(CommonDataKeys.PSI_FILE)
        if (editor != null && editorFile != null) {
            val referenceAt = editorFile.findElementAt(editor.caretModel.offset)
            val selectClass = PsiTreeUtil.getContextOfType(referenceAt, PsiClass::class.java)!!

            // 有 @RemoteService 注解
            return isServiceClass(selectClass)
        }
        // 没有解析到类时：默认启用
        return true
    }

    /**
     * 是否jksoa服务接口
     */
    private fun isServiceClass(psiClass: PsiClass): Boolean {
        val ann = psiClass.getAnnotation("net.jkcode.jksoa.common.annotation.RemoteService")
        return ann != null
    }

    companion object {
        const val ACTION_TEXT = "Generate jksoa php mapping class"
    }
}
