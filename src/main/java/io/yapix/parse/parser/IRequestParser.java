package io.yapix.parse.parser;

import com.intellij.psi.PsiMethod;
import io.yapix.model.HttpMethod;
import io.yapix.parse.model.RequestParseInfo;

/**
 * 请求信息解析
 *
 * @see #parse(PsiMethod, HttpMethod)
 */
public interface IRequestParser {

    /**
     * 解析请求参数信息
     *
     * @param method     待处理的方法
     * @param httpMethod 当前方法的http请求方法
     * @return 请求参数信息
     */
    RequestParseInfo parse(PsiMethod method, HttpMethod httpMethod);
}
