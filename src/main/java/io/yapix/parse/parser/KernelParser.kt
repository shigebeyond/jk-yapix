package io.yapix.parse.parser

import com.google.common.collect.Maps
import com.google.common.collect.Sets
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTypesUtil
import io.yapix.base.util.NotificationUtils
import io.yapix.config.BeanCustom
import io.yapix.config.YapixConfig
import io.yapix.model.DataTypes
import io.yapix.model.Property
import io.yapix.parse.constant.DocumentTags
import io.yapix.parse.constant.JavaConstants
import io.yapix.parse.util.*
import io.yapix.parse.util.doc.PsiDocCommentHelperProxy.getTagTextSet
import org.apache.commons.lang3.StringUtils
import java.util.*

/**
 * 解析一个完整的类型
 */
class KernelParser(private val project: Project, private val module: Module, private val settings: YapixConfig, protected val isResponse: Boolean) {

    protected val mockParser: MockParser = MockParser(project, module, settings)

    protected val dataTypeParser: DataTypeParser = DataTypeParser(project, module, settings)

    protected val dateParser: DateParser = DateParser(settings)

    protected val parseHelper: ParseHelper = ParseHelper(project, module)

    /**
     * 解析类型
     * 1 一级解析: dataTypeParser.parseType(psiType)
     *   数组、集合 -> array
     *   枚举 -> string
     *   其他：根据配置文件 types.properties 中的类型映射，来获得目标类型
     * 2 二级解析
     *   Map类型：解析泛型为Property.properties
     *   数组：解析泛型为Property.items
     *   集合：解析泛型为Property.items
     *   对象:解析属性为Property.properties
     * @param psiType
     * @param canonicalType
     * @param srcMethod 来源的方法，要先排除该方法的类
     * @return
     */
    fun parseType(psiType: PsiType?, canonicalType: String?, srcMethod: PsiMethod): Property? {
        return doParseType(psiType, canonicalType!!, mutableSetOf(srcMethod.containingClass!!))
    }

    /**
     * 解析类型
     * 1 一级解析: dataTypeParser.parseType(psiType)
     *   数组、集合 -> array
     *   枚举 -> string
     *   其他：根据配置文件 types.properties 中的类型映射，来获得目标类型
     * 2 二级解析
     *   Map类型：解析泛型为Property.properties
     *   数组：解析泛型为Property.items
     *   集合：解析泛型为Property.items
     *   对象:解析属性为Property.properties
     * @param psiType
     * @param canonicalType
     * @param chains 防止重复解析
     * @return
     */
    private fun doParseType(psiType: PsiType?, canonicalType: String, chains: Set<PsiClass>): Property? {
        var psiType = psiType
        val item = Property()
        item.required = false
        item.type = DataTypes.OBJECT //默认类型object
        if (StringUtils.isEmpty(canonicalType))
            return item

        // 泛型分割处理
        val types = PsiGenericUtils.splitTypeAndGenericPair(canonicalType)
        var type = types[0] //类型
        val genericTypes = types[1] //泛型
        if (PsiTypeUtils.isVoid(type))
            return null

        // 先过滤原始类型，以便减少查找其他类型，优化性能
        val primitiveType = PsiTypeUtils.getPrimitiveType(type)
        if(primitiveType != null){
            item.type = dataTypeParser.parseType(primitiveType)
            return item
        }
        // string类简写处理: 虽然string不属于原始类型，但很常用，支持简写, 参考 types.properties
        val strTypes = "string|date"
        if(strTypes.contains(type.toLowerCase())){
            item.type = "string"
            return item
        }

        // 获得非原始类型的PsiClass
        // val psiClass = PsiUtils.findPsiClass(project, module, type)
        val psiClass = PsiLinkUtils.getLinkClass(chains.last() /*发起调用的类*/, type)
        if (psiClass != null)
            psiType = PsiTypesUtil.getClassType(psiClass)
        if (psiType == null)
            return item

        /**
         * 获取字段类型
         * 1 数组、集合 -> array
         * 2 枚举 -> string
         * 3 其他：根据配置文件 types.properties 中的类型映射，来获得目标类型
         */
        item.type = dataTypeParser.parseType(psiType)
        item.values = parseHelper.getTypeValues(psiType)
        // 文件： 无需继续解析
        if (DataTypes.FILE == item.type)
            return item

        // Map类型：解析泛型为Property.properties
        if (PsiTypeUtils.isMap(psiType, project, module) || JavaConstants.Object == type) {
            item.type = DataTypes.OBJECT
            doHandleMap(item, genericTypes, chains)
            return item
        }

        // 数组：解析泛型为Property.items
        if (PsiTypeUtils.isArray(psiType)) {
            val arrayType = psiType as PsiArrayType
            val componentType = arrayType.componentType
            val items = doParseType(componentType, componentType.canonicalText, chains)
            item.items = items
        }

        // 集合：解析泛型为Property.items
        if (PsiTypeUtils.isCollection(psiType, project, module)) {
            // 递归调用
            val items = doParseType(null, genericTypes, chains)
            item.items = items
        }

        // 对象:解析属性为Property.properties
        val isNeedParseObject = (psiClass != null && item.isObjectType
            && (chains == null || !chains.contains(psiClass))) // 防止重复解析
        if (isNeedParseObject) {
            // 递归调用
            val properties = doParseBean(type, genericTypes, psiClass!!, chains)
            item.properties = properties
        }
        return item
    }

    /**
     * 处理Map类型
     */
    private fun doHandleMap(item: Property, genericTypes: String?, chains: Set<PsiClass>) {
        if (StringUtils.isEmpty(genericTypes))
            return

        // 尝试解析map值得类型
        val kvGenericTypes = PsiGenericUtils.splitGenericParameters(genericTypes)
        if (kvGenericTypes.size >= 2) {
            val mapValueProperty = doParseType(null, kvGenericTypes[1], chains)
            if (mapValueProperty != null) {
                mapValueProperty.name = "KEY"
                val properties: MutableMap<String, Property> = Maps.newHashMap()
                properties[mapValueProperty.name] = mapValueProperty
                item.properties = properties
            }
        }
    }

    /**
     * orm基类
     */
    protected val ormBaseClass = JavaPsiFacade.getInstance(project).findClass("net.jkcode.jkmvc.orm.Orm", GlobalSearchScope.allScope(project))!!

    /**
     * 解析bean类
     * @param type 类型
     * @param genericTypes 泛型
     * @param psiClass 类型的PsiClass
     * @param chains 防止重复解析
     * @return
     */
    private fun doParseBean(type: String, genericTypes: String?, psiClass: PsiClass, chains: Set<PsiClass>): Map<String, Property> {
        // 防止循环引用
        val newChains = if (chains != null) Sets.newHashSet(chains) else Sets.newHashSet<PsiClass>()
        newChains.add(psiClass)
        val properties: MutableMap<String, Property> = LinkedHashMap()

        // 针对接口/实体类, 检查是否存在@see引用, 引用属性被收集到properties
        doParseBeanSees(type, psiClass, chains, properties)

        val isOrm = psiClass.isInheritor(ormBaseClass, true) // 继承orm基类
        if (psiClass.isInterface || isOrm) {
            // 接口/orm类型: 解析getter
            doParseBeanGetters(type, genericTypes, psiClass, newChains, isOrm, properties)
        } else {
            // 实体类: 解析字段
            doParseBeanFields(type, genericTypes, psiClass, newChains, properties)
        }
        return properties
    }

    /**
     * 针对接口/实体类, 检查是否存在@see引用
     * @param type 类型
     * @param psiClass 类型的PsiClass
     * @param chains 防止重复解析
     * @param properties 收集属性
     */
    private fun doParseBeanSees(type: String, psiClass: PsiClass, chains: Set<PsiClass>, properties: MutableMap<String, Property>) {
        for (typeName in getTagTextSet(psiClass!!, DocumentTags.See)) {
            // 优先根据全限定名获取引用类, 其次根据非限定名(短名)获取.
            // [note] 建议用户尽量使用全限定名, 短名容易出现重名冲突.
            // 其实可以通过获取当前类的import作filter.
            // 但既然用户使用javadoc, 就应该对输入作严格规范, 而非对插件输出作强要求.
            val refPsiClass = PsiUtils.findPsiClass(project, module, typeName)
                            ?: PsiUtils.findPsiClassByShortName(project, module, typeName)

            // 引用类必须跟当前类存在派生关系(适用于接口和实体类)
            if (refPsiClass == null || !refPsiClass.isInheritor(psiClass, true)) {
                NotificationUtils.notifyWarning("Parse skipped", String.format("%s @see %s", type, typeName))
                continue
            }

            // 获得引用类的属性
            val item = PsiTypesUtil.getClassType(refPsiClass)?.let {
                doParseType(it, it.getCanonicalText(), chains)
            }
            // 添加引用类的属性
            if (item != null)
                properties.putAll(item.properties)
        }
    }

    /**
     * 接口/orm类型: 解析getter
     * @param type 类型
     * @param genericTypes 泛型
     * @param psiClass 类型的PsiClass
     * @param chains 防止重复解析
     * @param isOrm 是否orm类
     * @param properties 收集属性
     */
    private fun doParseBeanGetters(type: String, genericTypes: String?, psiClass: PsiClass, chains: HashSet<PsiClass>, isOrm: Boolean, properties: MutableMap<String, Property>) {
        val beanCustom = getBeanCustomSettings(type)
        val methods = PsiUtils.getGetterMethods(psiClass, isOrm)
        for (method in methods) {
            val methodName = method.name
            val filedType = method.returnType
            val filedName = StringUtils.uncapitalize(methodName.substring(if (methodName.startsWith("get")) 3 else 2))
            // 自定义配置决定是否处理该字段
            if (beanCustom != null && !beanCustom.isNeedHandleField(filedName))
                continue

            val realType = PsiGenericUtils.getRealTypeWithGeneric(psiClass, filedType, genericTypes)
            val fieldProperty = doParseType(filedType, realType, chains) ?: continue
            fieldProperty.name = filedName
            fieldProperty.deprecated = parseHelper.getApiDeprecated(method)
            fieldProperty.description = parseHelper.getMethodDescription(method)
            fieldProperty.mock = mockParser.parseMock(fieldProperty, filedType, null, filedName)
            if(beanCustom != null)
                handleWithBeanCustomField(fieldProperty, filedName, beanCustom)

            properties[fieldProperty.name] = fieldProperty
        }
    }

    /**
     * 实体类: 解析字段
     * @param type 类型
     * @param genericTypes 泛型
     * @param psiClass 类型的PsiClass
     * @param chains 防止重复解析
     * @param properties 收集属性
     */
    private fun doParseBeanFields(type: String, genericTypes: String?, psiClass: PsiClass, chains: HashSet<PsiClass>, properties: MutableMap<String, Property>) {
        val beanCustom = getBeanCustomSettings(type)
        val fields = PsiUtils.getFields(psiClass)
        for (field in fields) {
            val filedName = field.name
            val fieldType = field.type
            if (parseHelper.isFieldIgnore(field))
                continue

            // 自定义配置决定是否处理该字段
            if (beanCustom != null && !beanCustom.isNeedHandleField(filedName))
                continue

            val realType = PsiGenericUtils.getRealTypeWithGeneric(psiClass, fieldType, genericTypes)
            val fieldProperty = doParseType(fieldType, realType, chains) ?: continue
            dateParser.handle(fieldProperty, field)
            // 响应参数不要默认值
            if (!isResponse) {
                val defaultValue = PsiFieldUtils.getFieldDefaultValue(field)
                if (defaultValue != null) {
                    fieldProperty.defaultValue = defaultValue
                }
            }
            fieldProperty.values = parseHelper.getFieldValues(field)
            fieldProperty.name = parseHelper.getFieldName(field)
            fieldProperty.description = parseHelper.getFieldDescription(field, fieldProperty.values)
            fieldProperty.deprecated = parseHelper.getFieldDeprecated(field)
            fieldProperty.required = parseHelper.getFieldRequired(field)
            fieldProperty.mock = mockParser.parseMock(fieldProperty, fieldType, field, filedName)
            if(beanCustom != null)
                handleWithBeanCustomField(fieldProperty, filedName, beanCustom)

            properties[fieldProperty.name] = fieldProperty
        }
    }

    /**
     * 处理自定义的bean配置
     */
    private fun handleWithBeanCustomField(filedItem: Property, fieldName: String, beanCustom: BeanCustom) {
        if (beanCustom.fields == null || !beanCustom.fields.containsKey(fieldName))
            return

        val customItem = beanCustom.fields[fieldName] ?: return
        filedItem.mergeCustom(customItem)
    }

    /**
     * 获取指定类型自定义的bean配置
     */
    private fun getBeanCustomSettings(type: String): BeanCustom? {
        var custom: BeanCustom? = null
        val beans = settings.beans
        if (beans != null) {
            custom = beans[type]
        }

        if (custom != null) {
            if (custom.includes == null) {
                custom.includes = emptySet()
            }
            if (custom.excludes == null) {
                custom.excludes = emptySet()
            }
            if (custom.fields == null) {
                custom.fields = emptyMap()
            }
        }
        return custom
    }


}
