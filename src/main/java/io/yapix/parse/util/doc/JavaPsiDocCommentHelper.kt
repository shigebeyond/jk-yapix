package io.yapix.parse.util.doc

import com.intellij.psi.PsiDocCommentOwner
import com.intellij.psi.PsiElement
import com.intellij.psi.javadoc.PsiDocTag
import com.intellij.psi.javadoc.PsiDocToken
import com.intellij.psi.javadoc.PsiInlineDocTag
import io.yapix.parse.constant.DocumentTags
import net.jkcode.jkutil.common.substringBetween
import java.util.*

/**
 * PsiDocComment相关工具类
 */
object JavaPsiDocCommentHelper: IPsiDocCommentHelper {

    /**
     * 获取标记自定义字段名(包括字段描述)
     * @param element 文档元素
     */
    override fun getTagParamTextMap(element: PsiDocCommentOwner): Map<String, String> {
        val map: MutableMap<String, String> = HashMap()
        val tags = findTagsByName(element, DocumentTags.Param)
        for (tag in tags) {
            val elements = tag.dataElements
            if (elements.size >= 2) {
                val name = elements[0].text.trim() // 参数名
                val description = elements[1].text.trim() // 参数描述
                map[name] = description
            }
        }
        return map
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
        return findTagsByName(element, tag)
            .mapNotNull{ obj: PsiDocTag ->
                obj.dataElements.firstOrNull()?.text?.trim()
            }
            .toSet()
    }

    /**
     * 获取标记文本值
     */
    override fun getTagText(element: PsiDocCommentOwner, tagName: String): String? {
        val tag = findTagByName(element, tagName)
        if (tag == null)
            return null

        val splits = tag.text.split("\\s".toRegex(), 2)
        return splits.firstOrNull()
    }

    /**
     * 获取文档标记内容
     */
    override fun getDocCommentTagText(element: PsiDocCommentOwner, tagName: String): String? {
        val comment = element.docComment
        val tag = comment?.findTagByName(tagName)
        if (tag != null && tag.valueElement != null) {
            return tag.dataElements.joinToString { e ->
                e.text.trim()
            }
        }

        return null
    }

    /**
     * 获取文档标题行
     */
    override fun getDocCommentTitle(element: PsiDocCommentOwner): String? {
        val comment = element.docComment ?: return null
        val title =  comment.descriptionElements.firstOrNull { o: PsiElement ->
            o is PsiDocToken
        }
        return title?.text?.trim()
    }

    /**
     * 检查是否存在文档注释上的标记
     */
    override fun hasTagByName(element: PsiDocCommentOwner, tagName: String): Boolean{
        return findTagByName(element, tagName) != null
    }

    /**
     * 获取文档注释上的标记
     */
    fun findTagByName(element: PsiDocCommentOwner, tagName: String): PsiDocTag? {
        val comment = element.docComment
        return comment?.findTagByName(tagName)
    }

    /**
     * 获取文档注释上的标记
     */
    fun findTagsByName(element: PsiDocCommentOwner, tagName: String): Array<PsiDocTag> {
        val comment = element.docComment
        return comment?.findTagsByName(tagName)
            ?: emptyArray()
    }

    /**
     * 获取注释中link标记的内容
     *   对类的引用：java {@link io.yapix.model.Property}
     */
    override fun getInlineLinkContent(element: PsiDocCommentOwner): String? {
        val comment = element.docComment ?: return null
        for(ele in comment.descriptionElements){
            if(ele is PsiInlineDocTag && ele.text.startsWith("{@link"))
                return ele.text.substringBetween("{@link", "}")
        }
        return null
    }

}
