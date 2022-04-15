package io.yapix.parse.util

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiJavaDocumentedElement
import com.intellij.psi.javadoc.PsiDocTag
import com.intellij.psi.javadoc.PsiDocToken
import com.intellij.psi.javadoc.PsiInlineDocTag
import io.yapix.parse.constant.DocumentTags
import net.jkcode.jkutil.common.substringBetween
import java.util.*
import java.util.function.Function
import java.util.function.Predicate
import java.util.stream.Collectors
import kotlin.collections.ArrayList

/**
 * PsiDocComment相关工具类
 */
object PsiDocCommentUtils {

    /**
     * 获取标记自定义字段名(包括字段描述)
     * @param element 文档元素
     */
    @JvmStatic
    fun getTagParamTextMap(element: PsiJavaDocumentedElement): Map<String, String> {
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
    @JvmStatic
    fun getTagTextSet(element: PsiJavaDocumentedElement, tag: String): Set<String> {
        return findTagsByName(element, tag)
            .mapNotNull{ obj: PsiDocTag ->
                obj.dataElements.firstOrNull()?.text?.trim()
            }
            .toSet()
    }

    /**
     * 获取标记文本值
     */
    @JvmStatic
    fun getTagText(element: PsiJavaDocumentedElement, tagName: String?): String? {
        val tag = findTagByName(element, tagName)
        if (tag == null)
            return null

        val splits = tag.text.split("\\s".toRegex(), 2)
        return splits.firstOrNull()
    }

    /**
     * 获取文档标记内容
     */
    @JvmStatic
    fun getDocCommentTagText(element: PsiJavaDocumentedElement, tagName: String): String? {
        val comment = element.docComment
        if (comment != null) {
            val tag = comment.findTagByName(tagName)
            if (tag != null && tag.valueElement != null) {
                return tag.dataElements.joinToString { e ->
                    e.text.trim()
                }
            }
        }

        return null
    }

    /**
     * 获取文档标题行
     */
    @JvmStatic
    fun getDocCommentTitle(element: PsiJavaDocumentedElement): String? {
        val comment = element.docComment
        if (comment == null)
            return null

        val title =  comment.descriptionElements.firstOrNull { o: PsiElement ->
            o is PsiDocToken
        }
        return title?.text?.trim()
    }

    /**
     * 获取文档注释上的标记
     */
    @JvmStatic
    fun findTagByName(element: PsiJavaDocumentedElement, tagName: String?): PsiDocTag? {
        val comment = element.docComment
        return comment?.findTagByName(tagName)
    }

    /**
     * 获取文档注释上的标记
     */
    fun findTagsByName(element: PsiJavaDocumentedElement, tagName: String): Array<PsiDocTag> {
        val comment = element.docComment
        return comment?.findTagsByName(tagName)
            ?: emptyArray()
    }

    /**
     * 获取文档注释上的标记
     */
    fun findTagsByNames(element: PsiJavaDocumentedElement, vararg tagNames: String): List<PsiDocTag> {
        val comment = element.docComment
        if(comment == null)
            return emptyList()

        val r = ArrayList<PsiDocTag>()
        for(tagName in tagNames) {
            r.addAll(comment.findTagsByName(tagName))
        }
        return r
    }

    /**
     * 获取注释中link标记的内容
     *   对类的引用：java {@link io.yapix.model.Property}
     */
    fun getInlineLinkContent(element: PsiJavaDocumentedElement): String? {
        val comment = element.docComment ?: return null
        for(ele in comment.descriptionElements){
            if(ele is PsiInlineDocTag && ele.text.startsWith("{@link"))
                return ele.text.substringBetween("{@link", "}")
        }
        return null
    }

    /**
     * 获取注释中link标记的内容
     *   对类的引用： kotlin [io.yapix.model.Property]
     *   TODO: kotlin PsiJavaDocumentedElement
     */
    /*fun getInlineLinkContent(element: PsiJavaDocumentedElement): String? {
        val comment = element.docComment ?: return null
        for(ele in comment.descriptionElements){
            if(ele is PsiInlineDocTag && ele.text.startsWith("["))
                return ele.text.substringBetween("[", "]")
        }
        return null
    }*/
}
