package io.yapix.parse.parser

import com.google.common.collect.Lists
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import io.yapix.config.YapixConfig
import io.yapix.model.Api
import io.yapix.parse.IApiParser
import io.yapix.parse.constant.DocumentTags
import io.yapix.parse.model.ClassParseData
import io.yapix.parse.model.ControllerApiInfo
import io.yapix.parse.model.MethodParseData
import io.yapix.parse.model.PathParseInfo
import io.yapix.parse.util.doc.PsiDocCommentHelperProxy
import org.slf4j.LoggerFactory

/**
 * Api接口解析器基类
 */
abstract class AbstractApiParser(protected val project: Project, protected val module: Module, settings: YapixConfig) : IApiParser {

    // 请求解析器
    protected abstract val requestParser: IRequestParser

    // 响应解析器
    protected abstract val responseParser: ResponseParser

    // 解析助手
    protected val parseHelper: ParseHelper = ParseHelper(project, module)

    companion object{

        val logger = LoggerFactory.getLogger(AbstractApiParser::class.java)
    }

    /**
     * 解析方法
     */
    override fun parse(method: PsiMethod): MethodParseData? {
        // 解析controller
        val psiClass = method.containingClass!!
        val controllerApiInfo = parseController(psiClass)
        if(controllerApiInfo == null)
            return null

        // 解析方法
        return parseMethod(controllerApiInfo, method)
    }

    /**
     * 解析接口(controller)
     */
    override fun parse(psiClass: PsiClass): ClassParseData? {
        if (!isNeedParseController(psiClass)
            || PsiDocCommentHelperProxy.hasTagByName(psiClass, DocumentTags.Ignore))  // 忽略
            return null

        // 获得待处理方法
        val methods = filterPsiMethods(psiClass)
        if (methods.isEmpty())
            return ClassParseData(psiClass) // 也返回一个空的

        // 解析controller
        val controllerApiInfo = parseController(psiClass)
        if(controllerApiInfo == null)
            return null

        // 解析方法
        val methodDataList: MutableList<MethodParseData> = Lists.newArrayListWithExpectedSize(methods.size)
        for (method in methods) {
            val methodData = parseMethod(controllerApiInfo, method)
            if (methodData != null)
                methodDataList.add(methodData)
        }
        val data = ClassParseData(psiClass)
        data.declaredCategory = controllerApiInfo.declareCategory
        data.methodDataList = methodDataList
        return data
    }

    /**
     * 解析方法的通用信息，除请求路径、请求方法外.
     */
    protected fun doParseMethod(method: PsiMethod, mapping: PathParseInfo): Api {
        val api = Api()
        // 基本信息
        api.method = mapping.method
        api.summary = parseHelper.getApiSummary(method)
        api.description = parseHelper.getApiDescription(method)
        api.deprecated = parseHelper.getApiDeprecated(method)
        api.tags = parseHelper.getApiTags(method)
        // 请求信息
        val requestInfo = requestParser.parse(method, mapping.method)
        api.parameters = requestInfo.parameters
        api.requestBodyType = requestInfo.requestBodyType
        api.requestBody = requestInfo.requestBody
        api.requestBodyForm = requestInfo.requestBodyForm
        // 响应信息
        val response = responseParser.parse(method)
        api.responses = response
        logger.debug("解析方法[{}.{}()]成为api: {}", method.containingClass?.name, method.name, api)
        return api
    }

    /**
     * 判断是否是控制类或接口
     */
    protected abstract fun isNeedParseController(psiClass: PsiClass): Boolean

    /**
     * 获取待处理的方法列表
     */
    protected abstract fun filterPsiMethods(psiClass: PsiClass): List<PsiMethod>

    /**
     * 解析类级别信息
     */
    protected abstract fun parseController(controller: PsiClass): ControllerApiInfo?

    /**
     * 解析某个方法的接口信息
     */
    protected abstract fun parseMethod(controllerInfo: ControllerApiInfo, method: PsiMethod): MethodParseData?
}
