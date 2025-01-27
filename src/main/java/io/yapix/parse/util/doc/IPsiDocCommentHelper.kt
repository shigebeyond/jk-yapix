package io.yapix.parse.util.doc

import com.intellij.psi.PsiDocCommentOwner
import io.yapix.parse.constant.DocumentTags

/**
 * PsiDocComment相关工具类
 */
interface IPsiDocCommentHelper {

    /**
     * 获取@param标记的字段名+字段描述
     * @param element 文档元素，一般指方法
     * @return Map<字段名, 字段描述>
     */
    fun getParamTagTextMap(element: PsiDocCommentOwner): Map<String, String>

    /**
     * 获取@return标记的link文本
     * @param element 文档元素，一般指方法
     * @return
     */
    fun getReturnTagLinkText(element: PsiDocCommentOwner): String?{
        val ret = getTagText(element, DocumentTags.Return) // 获得 @return 标记
        if(ret != null)
            return getLinkText(element, ret) // 获得 @return 标记 中link的类

        return null
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
    fun getTagTextSet(element: PsiDocCommentOwner, tag: String): Set<String>

    /**
     * 获取标记文本值
     */
    fun getTagText(element: PsiDocCommentOwner, tagName: String): String?

    /**
     * 获取文档标记内容
     */
    fun getDocCommentTagText(element: PsiDocCommentOwner, tagName: String): String?

    /**
     * 获取文档标题行: 文档第一行
     */
    fun getDocCommentTitle(element: PsiDocCommentOwner): String?

    /**
     * 获取文档内容
     */
    fun getDocCommentText(element: PsiDocCommentOwner): String?

    /**
     * 检查是否存在文档注释上的标记
     */
    fun hasTagByName(element: PsiDocCommentOwner, tagName: String): Boolean

    /**
     * 获取注释中link标记的内容
     *   对类的引用: 如 java {@link io.yapix.model.Property}, kotlin [io.yapix.model.Property]
     *   返回 io.yapix.model.Property
     */
    fun getLinkText(element: PsiDocCommentOwner, comment: String = getDocCommentText(element)!!): String?
}
