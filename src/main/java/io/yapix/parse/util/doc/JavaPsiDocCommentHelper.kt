package io.yapix.parse.util.doc

import com.google.common.base.Strings
import com.intellij.psi.*
import com.intellij.psi.impl.source.PsiClassImpl
import com.intellij.psi.javadoc.PsiDocTag
import com.intellij.psi.javadoc.PsiDocToken
import com.intellij.psi.search.GlobalSearchScope
import io.yapix.parse.constant.DocumentTags
import net.jkcode.jkutil.common.substringBetween
import java.util.*

/**
 * PsiDocComment相关工具类
 */
object JavaPsiDocCommentHelper : IPsiDocCommentHelper {

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
            .mapNotNull { obj: PsiDocTag ->
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
     * 获取文档标题行: 文档第一行
     */
    override fun getDocCommentTitle(element: PsiDocCommentOwner): String? {
        val comment = element.docComment ?: return null
        val title = comment.descriptionElements.firstOrNull { o: PsiElement ->
            o is PsiDocToken
        }
        return title?.text?.trim()
    }

    /**
     * 获取文档内容
     */
    override fun getDocCommentText(element: PsiDocCommentOwner): String? {
        return element.docComment?.text
    }

    /**
     * 检查是否存在文档注释上的标记
     */
    override fun hasTagByName(element: PsiDocCommentOwner, tagName: String): Boolean {
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
     *   对类的引用: 如 java {@link io.yapix.model.Property}, kotlin [io.yapix.model.Property]
     */
    override fun getLinkText(element: PsiDocCommentOwner, comment: String): String? {
        if (comment.contains("{@link"))
            return comment.substringBetween("{@link", "}")

        return null
    }

    /**
     * @description: 获得link 备注
     * @param: [remark, project, field]
     * @return: java.lang.String
     * @author: chengsheng@qbb6.com
     * @date: 2019/5/18
     */
    fun getLinkRemark(field: PsiField): String? {
        var remark = ""

        var linkAddress = getLinkText(field)
        if (linkAddress == null)
            return null

        val project = field.project
        var psiClassLink = JavaPsiFacade.getInstance(project)
            .findClass(linkAddress, GlobalSearchScope.allScope(project))
        if (psiClassLink == null) {
            //可能没有获得全路径，尝试获得全路径
            val imports = field.containingFile.importsInFile()
            val parts = linkAddress.split('.', limit = 2)
            val key = parts[0]
            if(parts.size > 1)
                linkAddress = parts[1] // 剩下部分
            if(key in imports){
                linkAddress = imports[key] + '.' + linkAddress
                // 继续查找类
                psiClassLink = JavaPsiFacade.getInstance(project).findClass(linkAddress, GlobalSearchScope.allScope(project))
            }

            if (psiClassLink == null) {
                //如果是同包情况
                linkAddress = (((field.parent as PsiClassImpl).context as PsiClassOwner).packageName + "." + linkAddress)
                psiClassLink = JavaPsiFacade.getInstance(project).findClass(linkAddress, GlobalSearchScope.allScope(project))
            }
            //如果小于等于一为不存在import，不做处理
        }

        if (Objects.nonNull(psiClassLink)) {
            //说明获得了link 的class
            val linkFields = psiClassLink!!.fields
            if (linkFields.size > 0) {
                remark += "," + psiClassLink.name + "["
                for (i in linkFields.indices) {
                    val psiField = linkFields[i]
                    if (i > 0) {
                        remark += ","
                    }
                    // 先获得名称
                    remark += psiField.name
                    // 后获得value,通过= 来截取获得，第二个值，再截取;
                    val splitValue = psiField.text.split("=".toRegex()).toTypedArray()
                    if (splitValue.size > 1) {
                        val value = splitValue[1].split(";".toRegex()).toTypedArray()[0]
                        remark += ":$value"
                    }
                    val filedValue = PsiDocCommentHelperProxy.getDocCommentTitle(psiField)
                    if (!Strings.isNullOrEmpty(filedValue)) {
                        remark += "($filedValue)"
                    }
                }
                remark += "]"
            }
        }
        return remark
    }

}
