package io.yapix.parse.util.doc

import com.intellij.psi.PsiDocCommentOwner

/**
 * PsiDocComment相关工具类
 */
object PsiDocCommentHelperProxy: IPsiDocCommentHelper {

    /**
     * bean which class qualifiedName contains kotlin may be a instance
     * of class that in kotlin-plugin
     * e.g.
     * [org.jetbrains.kotlin]
     * [org.jetbrains.kotlin.psi]
     */
    fun isKtPsiInst(any: Any): Boolean {
        return any::class.qualifiedName?.contains("kotlin") ?: false
    }

    /**
     * 获得真正的 IPsiDocCommentHelper 实现
     */
    fun getTargetHelper(element: PsiDocCommentOwner): IPsiDocCommentHelper{
        return if(isKtPsiInst(element)) KtPsiDocCommentHelper else JavaPsiDocCommentHelper
    }

    /**
     * 获取标记自定义字段名(包括字段描述)
     * @param element 文档元素
     */
    override fun getTagParamTextMap(element: PsiDocCommentOwner): Map<String, String> {
        return getTargetHelper(element).getTagParamTextMap(element)
    }

    /**
     * 获取标记自定义字段名(不包括字段描述)
     * @param tag 实例变量类型为接口类/实体类  可通过@see 来获取扁平后的字段
     *
     * / **
     * * @see SubInterfaceImpl {field1, field2}
     * * @see SubxInterfaceImpl {field3, field4, field5}
     * *\/
     * interface BaseInterface {}
     *
     * BaseInterface {field1, field2, field3, ...}
     * 至于如何区分field1,field2,field3隶属于哪个实现类, 是用户写desc需要考量的, 而非插件程序逻辑.
     */
    override fun getTagTextSet(element: PsiDocCommentOwner, tag: String): Set<String> {
        return getTargetHelper(element).getTagTextSet(element, tag)
    }

    /**
     * 获取标记文本值
     */
    override fun getTagText(element: PsiDocCommentOwner, tagName: String): String? {
        return getTargetHelper(element).getTagText(element, tagName)
    }

    /**
     * 获取文档标记内容
     */
    override fun getDocCommentTagText(element: PsiDocCommentOwner, tagName: String): String? {
        return getTargetHelper(element).getDocCommentTagText(element, tagName)
    }

    /**
     * 获取文档标题行
     */
    override fun getDocCommentTitle(element: PsiDocCommentOwner): String? {
        return getTargetHelper(element).getDocCommentTitle(element)
    }

    /**
     * 检查是否存在文档注释上的标记
     */
    override fun hasTagByName(element: PsiDocCommentOwner, tagName: String): Boolean {
        return getTargetHelper(element).hasTagByName(element, tagName)
    }

    /**
     * 获取注释中link标记的内容
     *   对类的引用：java {@link io.yapix.model.Property}
     */
    override fun getInlineLinkContent(element: PsiDocCommentOwner): String? {
        return getTargetHelper(element).getInlineLinkContent(element)
    }
}
