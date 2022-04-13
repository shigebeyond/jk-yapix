package io.yapix.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

/**
 * Yapi X菜单分组
 */
public class YapixActionGroup extends DefaultActionGroup {

    @Override
    public void update(@NotNull AnActionEvent event) {
        // 不存在模块不展示：选择多个模块
        Project project = event.getData(CommonDataKeys.PROJECT);
        Module module = event.getData(LangDataKeys.MODULE);
        if (project == null || module == null) {
            event.getPresentation().setVisible(false);
            return;
        }

        // 非java的文件不显示
        VirtualFile file = event.getDataContext().getData(CommonDataKeys.VIRTUAL_FILE);
        if (file != null && !file.isDirectory() &&
                !"java".equals(file.getExtension())  // java文件
                && !"kt".equals(file.getExtension())) { // kotlin文件
            event.getPresentation().setVisible(false);
            return;
        }
    }
}
