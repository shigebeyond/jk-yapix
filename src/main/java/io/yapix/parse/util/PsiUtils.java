package io.yapix.parse.util;

import com.google.common.collect.Lists;
import com.intellij.lang.jvm.JvmModifier;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiEnumConstant;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiType;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiShortNamesCache;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class PsiUtils {

    private PsiUtils() {
    }

    public static PsiField[] getFields(PsiClass t) {
        PsiField[] fields = t.getAllFields();
        return Arrays.stream(fields).filter(PsiUtils::isNeedField).toArray(PsiField[]::new);
    }

    public static boolean isNeedField(PsiField field) {
        return !field.hasModifier(JvmModifier.STATIC);
    }

    /**
     * 根据类短名来获取PsiClass, 而非类全限定名
     * 优先从当前模块依赖, 其次当前工程作用域
     */
    public static PsiClass findPsiClassByShortName(Project project, Module module, String shortName) {
        PsiClass psiClass = null;
        if (module != null) {
            psiClass = Optional.ofNullable(PsiShortNamesCache.getInstance(project)
                    .getClassesByName(shortName, GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module, false)))
                    .filter(it -> it.length >= 1)
                    .map(it -> it[0])
                    .orElse(null);
        }
        if (psiClass == null) {
            psiClass = Optional.ofNullable(PsiShortNamesCache.getInstance(project)
                    .getClassesByName(shortName, GlobalSearchScope.projectScope(project)))
                    .filter(it -> it.length >= 1)
                    .map(it -> it[0])
                    .orElse(null);
        }
        return psiClass;
    }

    /**
     * 根据类全限定名获取PsiClass
     * 优先从当前模块依赖, 其次当前工程作用域
     */
    public static PsiClass findPsiClass(Project project, Module module, String qualifiedName) {
        PsiClass psiClass = null;
        if (module != null) {
            psiClass = JavaPsiFacade.getInstance(project)
                    .findClass(qualifiedName, GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module));
        }
        if (psiClass == null) {
            psiClass = JavaPsiFacade.getInstance(project).findClass(qualifiedName, GlobalSearchScope.allScope(project));
        }
        return psiClass;
    }

    /**
     * 获得getter方法
     * @param psiClass
     * @param onlyThisClassMethod 是否仅限于当前类的方法，否则包含父类的方法
     * @return
     */
    public static PsiMethod[] getGetterMethods(PsiClass psiClass, boolean onlyThisClassMethod) {
        PsiMethod[] methods = onlyThisClassMethod ? psiClass.getMethods() : psiClass.getAllMethods();
        return Arrays.stream(methods).filter(method -> {
            String methodName = method.getName();
            PsiType returnType = method.getReturnType();
            PsiModifierList modifierList = method.getModifierList();
            boolean isAccessMethod = !modifierList.hasModifierProperty("static")
                    && !methodName.equals("getClass")
                    && ((methodName.startsWith("get") && methodName.length() > 3 && returnType != null)
                    || (methodName.startsWith("is") && methodName.length() > 2 && returnType != null && returnType
                    .getCanonicalText().equals("boolean"))
            );
            return isAccessMethod;
        }).toArray(len -> new PsiMethod[len]);
    }

    /**
     * 获取枚举字段名
     */
    public static List<String> getEnumFieldNames(PsiClass psiClass) {
        List<String> names = Lists.newArrayListWithExpectedSize(psiClass.getFields().length);
        for (PsiField field : psiClass.getFields()) {
            if (field instanceof PsiEnumConstant) {
                names.add(field.getName());
            }
        }
        return names;
    }
}
