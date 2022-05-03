package io.yapix.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassOwner;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;
import io.yapix.base.util.PsiFileUtils;
import java.util.List;

/**
 * 封装idea动作(菜单)事件
 */
public class EventData {

    /**
     * 源事件
     */
    public AnActionEvent event;
    /**
     * 项目
     */
    public Project project;

    /**
     * 模块
     */
    public Module module;

    /**
     * 选择的文件
     */
    public VirtualFile[] selectedFiles;

    /**
     * 选择的Java/Kotlin文件
     */
    //public List<PsiJavaFile> selectedJavaFiles;
    public List<PsiClassOwner> selectedClassFiles;

    /**
     * 选择类
     */
    public PsiClass selectedClass;

    /**
     * 选择方法
     */
    public PsiMethod selectedMethod;

    /**
     * 是否应当继续解析处理
     */
    public boolean shouldHandle() {
        return project != null
                && module != null
                && (selectedClassFiles != null || selectedClass != null);
    }

    /**
     * 从事件中解析需要的通用数据
     */
    public static EventData of(AnActionEvent event) {
        EventData data = new EventData();
        data.event = event;
        data.project = event.getData(CommonDataKeys.PROJECT);
        data.module = event.getData(LangDataKeys.MODULE);
        data.selectedFiles = event.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);
        if (data.project != null && data.selectedFiles != null) {
            data.selectedClassFiles = PsiFileUtils.getPsiClassFiles(data.project, data.selectedFiles);
        }
        Editor editor = event.getDataContext().getData(CommonDataKeys.EDITOR);
        PsiFile editorFile = event.getDataContext().getData(CommonDataKeys.PSI_FILE);
        if (editor != null && editorFile != null) {
            PsiElement referenceAt = editorFile.findElementAt(editor.getCaretModel().getOffset());
            data.selectedClass = PsiTreeUtil.getContextOfType(referenceAt, PsiClass.class);
            data.selectedMethod = PsiTreeUtil.getContextOfType(referenceAt, PsiMethod.class);
        }
        return data;
    }
}
