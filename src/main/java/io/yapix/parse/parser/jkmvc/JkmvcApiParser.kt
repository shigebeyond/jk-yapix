package io.yapix.parse.parser.jkmvc

import com.google.gson.Gson
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import com.intellij.psi.search.GlobalSearchScope
import io.yapix.config.YapixConfig
import io.yapix.config.YapixConfigUtils
import io.yapix.model.Api
import io.yapix.model.HttpMethod
import io.yapix.parse.constant.DocumentTags
import io.yapix.parse.model.ControllerApiInfo
import io.yapix.parse.model.MethodParseData
import io.yapix.parse.model.PathParseInfo
import io.yapix.parse.parser.AbstractApiParser
import io.yapix.parse.parser.IRequestParser
import io.yapix.parse.parser.ResponseParser
import io.yapix.parse.util.PathUtils
import io.yapix.parse.util.PsiUtils
import io.yapix.parse.util.doc.PsiDocCommentHelperProxy
import net.jkcode.jkutil.common.Config
import net.jkcode.jkutil.common.camel2Underline
import org.apache.commons.lang3.StringUtils
import net.jkcode.jkutil.common.lcFirst

/**
 * jkmvc Api接口解析器
 */
public class JkmvcApiParser(project: Project, module: Module, settings: YapixConfig) : AbstractApiParser(project, module, settings) {

    companion object {
        private val gson = Gson()

        /**
         * 忽略的方法
         */
        private val ignoreMethods: Array<String> = arrayOf(
            "before",
            "after",
            "hashCode",
            "toString",
            "equals"
        )
    }

    // 请求解析器
    protected override val requestParser: IRequestParser = JkmvcRequestParser(project, module, settings)

    // 响应解析器
    protected override val responseParser: ResponseParser = JkmvcResponseParser(project, module, settings)

    /**
     * controller基类
     */
    protected val controllerBaseClass = PsiUtils.findPsiClass(project, null, "net.jkcode.jkmvc.http.controller.Controller")!!

    /**
     * 判断是否是控制类或接口
     */
    override fun isNeedParseController(psiClass: PsiClass): Boolean {
        // 1 以Controller结尾
        if(!psiClass.qualifiedName!!.endsWith("Controller"))
            return false

        // 2 继承controller基类
        return psiClass.isInheritor(controllerBaseClass, true)
    }

    /**
     * 获取待处理的方法列表
     */
    override fun filterPsiMethods(psiClass: PsiClass): List<PsiMethod> {
        // 不要用 psiClass.allMethods，会包含父类Controller的方法(setReq/view/renderXXX之类的方法)
        // 要用 psiClass.methods，只包含本类方法
        return psiClass.methods.filter {m: PsiMethod ->
            val modifier = m.modifierList
            (!modifier.hasModifierProperty(PsiModifier.PRIVATE) // 非私有
                && !modifier.hasModifierProperty(PsiModifier.STATIC) // 非静态
                && !ignoreMethods.contains(m.name) // 不包含忽略的方法
                && m.name != psiClass.name) // 不包含构造函数
        }
    }

    /**
     * 解析类级别信息
     */
    override fun parseController(controller: PsiClass): ControllerApiInfo? {
        var path: String = getControllerPath(controller)
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
     * 获得controller的uri前缀
     */
    private fun getControllerPath(controller: PsiClass): String {
        // 目前仅仅是简单处理，取controller名即
        return controller.name!!.removeSuffix("Controller").lcFirst()

        /* TODO: controller.qualifiedName 是类全路径，需要去掉http.yaml中指定的包名, 但jkmvc暂时还不支持多一层子包的controller路由解析
        var path = controller.qualifiedName!!

        // 1 加载 http.yaml
        val file = YapixConfigUtils.findResourceFile(project, module, "http.yaml")
        val config = Config.buildProperties(file.path, "yaml")
        // 2 获得加载controller的包名
        val packs = config["controllerPackages"] as List<String>
        for (pack in packs){
            if(path.contains(pack)){
                path = path.removePrefix(pack).removeSuffix("Controller")
                break
            }
        }
        return path.replace('.', '/')
        */
    }

    /**
     * 解析某个方法的接口信息
     */
    override fun parseMethod(controllerInfo: ControllerApiInfo, method: PsiMethod): MethodParseData? {
        if (PsiDocCommentHelperProxy.hasTagByName(method, DocumentTags.Ignore))
            return null

        // 1.解析路径信息
        val mapping = PathParseInfo()
        mapping.method = HttpMethod.POST // 写死post方法
        mapping.paths = listOf(method.name.lcFirst()) // 路径=方法名

        // 2.解析方法上信息: 请求、响应等.
        val methodApi = doParseMethod(method, mapping)

        // 3.合并信息
        val apis = mapping.paths.map { path: String? ->
            var api = methodApi
            if (mapping.paths.size > 1) {
                api = gson.fromJson(gson.toJson(methodApi), Api::class.java)
            }
            api.method = mapping.method
            api.path = PathUtils.path(controllerInfo.path, path)
            api.category = controllerInfo.category
            api
        }
        val data = MethodParseData(method)
        data.declaredApiSummary = methodApi.summary
        data.apis = apis
        return data
    }

}
