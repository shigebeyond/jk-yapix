package io.yapix.base.util;

import com.google.common.collect.Lists;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiClassOwner;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiModifier;
import java.util.Arrays;
import java.util.List;

public class PsiFileUtils {

    public PsiFileUtils() {
    }


    /**
     * 获取Java文件
     */
    public static List<PsiClassOwner> getPsiClassFiles(Project project, VirtualFile[] psiFiles) {
        List<PsiClassOwner> files = Lists.newArrayListWithExpectedSize(psiFiles.length);
        PsiManager psiManager = PsiManager.getInstance(project);
        for (VirtualFile f : psiFiles) {
            if (f.isDirectory()) {
                VirtualFile[] children = f.getChildren();
                List<PsiClassOwner> theFiles = getPsiClassFiles(project, children);
                files.addAll(theFiles);
                continue;
            }
            PsiFile file = psiManager.findFile(f);
            // if (file instanceof PsiJavaFileImpl) { //只处理java文件
            if (file instanceof PsiClassOwner) { //处理java+kotlin文件
                files.add((PsiClassOwner) file);
            }
        }
        return files;
    }

    /**
     * 获取PsiClass: 从类文件(java/kotlin)中取得类
     */
    public static List<PsiClass> getPsiClassByFile(List<PsiClassOwner> psiClassFiles) {
        List<PsiClass> psiClassList = Lists.newArrayListWithCapacity(psiClassFiles.size());
        for (PsiClassOwner psiClassFile : psiClassFiles) {
            Arrays.stream(psiClassFile.getClasses()) // 从类文件(java/kotlin)中取得类
                    .filter(o -> !o.isInterface()
                            && o.getModifierList() != null
                            && o.getModifierList().hasModifierProperty(PsiModifier.PUBLIC)) // 公开类
                    .findFirst().ifPresent(psiClassList::add);
        }
        return psiClassList;
    }
}
