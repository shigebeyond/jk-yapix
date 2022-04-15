package io.yapix.parse.parser.jkmvc

import com.google.common.collect.Lists
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiParameter
import com.intellij.psi.javadoc.PsiDocTag
import io.yapix.config.YapixConfig
import io.yapix.model.*
import io.yapix.parse.constant.DocumentTags
import io.yapix.parse.model.RequestParseInfo
import io.yapix.parse.parser.DateParser
import io.yapix.parse.parser.IRequestParser
import io.yapix.parse.parser.KernelParser
import io.yapix.parse.parser.ParseHelper
import io.yapix.parse.util.PsiDocCommentUtils
import io.yapix.parse.util.PsiTypeUtils
import org.apache.commons.lang3.StringUtils
import java.util.function.Predicate
import java.util.stream.Collectors

/**
 * jkmvc请求信息解析
 *
 * @see .parse
 */
class JkmvcRequestParser(project: Project, module: Module, private val settings: YapixConfig) : IRequestParser {

    private val kernelParser = KernelParser(project, module, settings, false)

    private val dateParser = DateParser(settings)

    private val parseHelper = ParseHelper(project, module)

    /**
     * 解析请求参数信息
     *
     * @param method     待处理的方法
     * @param httpMethod 当前方法的http请求方法
     * @return 请求参数信息
     */
    override fun parse(method: PsiMethod, httpMethod: HttpMethod): RequestParseInfo {
        // 解析参数: 请求体类型，普通参数，请求体
        val requestParameters = getRequestParameters(method)
        val requestBody = getRequestBody(method, httpMethod, requestParameters)
        val hasFile = requestParameters.any { it.type == DataTypes.FILE }

        // 数据填充
        val info = RequestParseInfo()
        info.parameters = requestParameters
        info.requestBodyType = if(hasFile) RequestBodyType.form_data else RequestBodyType.form // 请求体类型: form/form_data(有上传)/json(暂不支持)
        info.requestBodyForm = requestBody
        if (info.requestBodyForm == null) {
            info.requestBodyForm = emptyList()
        }
        return info
    }

    /**
     * 解析请求体内容
     *   暂不支持Json请求，只支持表单请求
     */
    private fun getRequestBody(method: PsiMethod, httpMethod: HttpMethod,
                               requestParameters: MutableList<Property>): List<Property> {
        if (!httpMethod.isAllowBody)
            return emptyList()

        val items: MutableList<Property> = Lists.newArrayList()

        // 表单：查询参数合并查询参数到表单
        val queries = requestParameters.filter { p: Property ->
            p.getIn() == ParameterIn.query
        }
        for (query in queries) {
            query.setIn(null)
            items.add(query)
        }
        requestParameters.removeAll(queries)
        return items
    }

    /**
     * 解析普通参数
     */
    fun getRequestParameters(method: PsiMethod): MutableList<Property> {
        // 获取方法@param标记信息
        val params = PsiDocCommentUtils.findTagsByNames(method, DocumentTags.Param, DocumentTags.RouteParam)
        val items: MutableList<Property> = Lists.newArrayListWithExpectedSize(params.size)
        for (param in params){
            val item = doParseParameter(param)
            // 当参数是bean时，需要获取bean属性作为参数
            val parameterItems = resolveItemToParameters(item)
            items.addAll(parameterItems)
        }
        for(item in items){
            doSetPropertyDateFormat(item)
        }
        return items
    }

    private fun doSetPropertyDateFormat(item: Property) {
        // 附加时间格式
        val sb = StringBuilder()
        if (StringUtils.isNotEmpty(item.description)) {
            sb.append(item.description)
        }
        if (StringUtils.isNotEmpty(item.dateFormat)) {
            if (sb.length > 0) {
                sb.append(" : ")
            }
            sb.append(item.dateFormat)
        }
        item.description = sb.toString()
    }

    /**
     * 解析单个参数
     * @param paramTag 参数注释，如 @param *参数名:类型:默认值，其中*表示必填
     */
    private fun doParseParameter(paramTag: PsiDocTag): Property? {
        val elements = paramTag.dataElements
        if (elements.isEmpty())
            return null

        // 解析参数注释
        val part = elements[0].text.trim().split(':') // *参数名:类型:默认值
        var name = part[0] // 参数
        val required = name.startsWith('*') // 必填
        if (required)
            name = name.substring(1)
        val type = part.getOrNull(1) ?: DataTypes.STRING // 类型
        val defaultValue = part.getOrNull(2) // 默认值

        val description = elements[1].text.trim() // 参数描述

        // 拼接参数信息
        //val item = Property()
        /**
         * 解析类型
         * 1 一级解析: dataTypeParser.parseType(psiType)
         *   数组、集合 -> array
         *   枚举 -> string
         *   其他：根据配置文件 types.properties 中的类型映射，来获得目标类型
         * 2 二级解析
         *    Map类型：解析泛型为Property.properties
         *    数组：解析泛型为Property.items
         *    集合：解析泛型为Property.items
         *    对象:解析属性为Property.properties
         */
        val item = kernelParser.parseType(null, type)
        item.required = required
        item.name = name
        item.type = type
        item.defaultValue = defaultValue
        item.values = null
        item.setIn(ParameterIn.query)
        item.description = description
        return item
    }

    /**
     * 解析Item为扁平结构的parameter
     * @param item {@link io.yapix.model.Property}
     */
    private fun resolveItemToParameters(item: Property?): Collection<Property> {
        if (item == null) {
            return emptyList()
        }
        val needFlat = item.isObjectType && item.properties != null && ParameterIn.query == item.getIn()
        if (needFlat) {
            val flatItems: Collection<Property> = item.properties.values
            for(one in flatItems)
                one.setIn(item.getIn())
            return flatItems
        }
        return listOf(item)
    }
}
