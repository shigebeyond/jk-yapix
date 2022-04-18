package io.yapix.parse.parser.spring

import com.google.gson.Gson
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import io.yapix.config.YapixConfig
import io.yapix.model.Api
import io.yapix.parse.constant.DocumentTags
import io.yapix.parse.constant.SpringConstants
import io.yapix.parse.model.ControllerApiInfo
import io.yapix.parse.model.MethodParseData
import io.yapix.parse.parser.AbstractApiParser
import io.yapix.parse.parser.IRequestParser
import io.yapix.parse.util.PathUtils
import io.yapix.parse.util.PsiAnnotationUtils
import io.yapix.parse.util.doc.PsiDocCommentHelperProxy
import org.apache.commons.lang3.StringUtils
import java.util.stream.Collectors

/**
 * spring Api接口解析器
 */
public class SpringApiParser(project: Project, module: Module, settings: YapixConfig) : AbstractApiParser(project, module, settings) {

    companion object {
        private val gson = Gson()
    }

    // 请求解析器
    protected override val requestParser: IRequestParser = SpringRequestParser(project, module, settings)

    /**
     * 判断是否是控制类或接口
     */
    override fun isNeedParseController(psiClass: PsiClass): Boolean {
        // 接口是为了满足接口继承的情况
        return (psiClass.isInterface
            || PsiAnnotationUtils.getAnnotation(psiClass, SpringConstants.RestController) != null || PsiAnnotationUtils.getAnnotation(psiClass, SpringConstants.Controller) != null)
    }

    /**
     * 获取待处理的方法列表
     */
    override fun filterPsiMethods(psiClass: PsiClass): List<PsiMethod> {
        return psiClass.allMethods.filter {m: PsiMethod ->
            val modifier = m.modifierList
            (!modifier.hasModifierProperty(PsiModifier.PRIVATE) // 非私有
                && !modifier.hasModifierProperty(PsiModifier.STATIC)) // 非静态
        }
    }

    /**
     * 解析类级别信息
     */
    override fun parseController(controller: PsiClass): ControllerApiInfo? {
        var path: String? = null
        val annotation = PsiAnnotationUtils.getAnnotationIncludeExtends(controller,
            SpringConstants.RequestMapping)
        if (annotation != null) {
            val mapping = PathParser.parseRequestMappingAnnotation(annotation)
            path = mapping.path
        }
        val info = ControllerApiInfo()
        info.path = PathUtils.path(path)
        info.declareCategory = parseHelper.getDeclareApiCategory(controller)
        info.category = info.declareCategory
        if (StringUtils.isEmpty(info.category)) {
            info.category = parseHelper.getDefaultApiCategory(controller)
        }
        return info
    }

    /**
     * 解析某个方法的接口信息
     */
    override fun parseMethod(controllerInfo: ControllerApiInfo, method: PsiMethod): MethodParseData? {
        if (PsiDocCommentHelperProxy.hasTagByName(method, DocumentTags.Ignore))
            return null

        // 1.解析路径信息: @XxxMapping
        val mapping = PathParser.parse(method)
        if (mapping == null || mapping.paths == null)
            return null

        // 2.解析方法上信息: 请求、响应等.
        val methodApi = doParseMethod(method, mapping)

        // 3.合并信息
        val apis = mapping.paths.stream().map { path: String? ->
            var api = methodApi
            if (mapping.paths.size > 1) {
                api = gson.fromJson(gson.toJson(methodApi), Api::class.java)
            }
            api.method = mapping.method
            api.path = PathUtils.path(controllerInfo.path, path)
            api.category = controllerInfo.category
            api
        }.collect(Collectors.toList())
        val data = MethodParseData(method)
        data.declaredApiSummary = methodApi.summary
        data.apis = apis
        return data
    }

}
