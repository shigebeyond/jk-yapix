package io.yapix.parse.model;

import com.intellij.psi.PsiMethod;
import io.yapix.model.Api;
import java.util.Collections;
import java.util.List;

/**
 * 方法解析数据
 */
public class MethodParseData {

    /**
     * 是否是有效的接口方法
     */
    public boolean valid;

    /**
     * 目标方法
     */
    public PsiMethod method;


    /**
     * 指定的接口名称
     */
    public String declaredApiSummary;

    /**
     * 接口列表
     */
    public List<Api> apis;

    public MethodParseData(PsiMethod method) {
        this.method = method;
    }

    public List<Api> getApis() {
        return apis != null ? apis : Collections.emptyList();
    }
}
