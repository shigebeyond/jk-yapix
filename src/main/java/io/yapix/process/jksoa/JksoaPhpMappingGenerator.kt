package io.yapix.process.jksoa

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiType
import io.yapix.parse.util.PsiAnnotationUtils
import io.yapix.parse.util.PsiTypeUtils
import io.yapix.parse.util.PsiUtils

/**
 * 为jksoa的java接口类，生成php映射类
 */
object JksoaPhpMappingGenerator {

    /**
     * 生成php映射类
     */
    fun generate(psiClass: PsiClass): String {
        // 拼接php代码
        // 1 拼接php类
        val pack = PsiUtils.getPakcageName(psiClass) // java包
        val namespace = pack.replace('.', '\\') // php命名空间
        val sb = StringBuilder("<?php\nnamespace ${namespace};\n\nclass ${psiClass.name} {\n")
        // 2 拼接php方法
        val methods = psiClass.methods.filter {
            !it.hasAnnotation("kotlin.jvm.JvmDefault") // 忽略默认方法
        }
        // 2.1 注解属性：所有方法的注解
        generateMethodAnotations(methods, sb)

        // 2.2 普通方法
        for (method in methods) {
            generateMethod(method, sb)
        }
        sb.append("\n}")
        return sb.toString()
    }

    /**
     * 生成注解属性
     */
    private fun generateMethodAnotations(methods: List<PsiMethod>, sb: StringBuilder) {
        // 1 收集所有方法的注解信息: {方法名:{注解类名:{注解属性}}}, 其中 注解类名:{注解属性} 如 "net.jkcode.jkguard.annotation.GroupCombine":{"batchMethod":"listUsersByNameAsync","reqArgField":"name","respField":"","one2one":"true","flushQuota":"100","flushTimeoutMillis":"100"}
        val methodAnns = HashMap<String, Map<String, Any?>>()
        for(method in methods){
            val annMap = buildMethodAnnotationMap(method)
            if(annMap.isNotEmpty()) // 无注解的不收集，省的浪费注意力
                methodAnns[method.name] = annMap
        }

        // 2 注解转php array
        val json = PsiAnnotationUtils.gson.toJson(methodAnns) // 注解信息转json
        val arr = json.replace('{', '[').replace('}', ']') // json转php array表达式

        // 3 php属性声明
        sb.append("\n\tpublic static \$_methodAnnotations = ").append(arr).append(";\n")
    }

    /**
     * 构建单个方法的注解信息
     *   注解类名:{注解属性} 如 "net.jkcode.jkguard.annotation.GroupCombine":{"batchMethod":"listUsersByNameAsync","reqArgField":"name","respField":"","one2one":"true","flushQuota":"100","flushTimeoutMillis":"100"}
     */
    private fun buildMethodAnnotationMap(method: PsiMethod): HashMap<String, Any?> {
        val anns = HashMap<String, Any?>()
        for (ann in method.annotations) {
            val name = ann.qualifiedName
            if (name != null && !name.contains("NotNull") && !name.contains("Throws") && !name.contains("Suspendable")) {
                val attrs = PsiAnnotationUtils.getAnnotationAttributeMap(ann, true)
                anns[name] = attrs
            }
        }
        return anns
    }

    /**
     * 生成php映射方法
     */
    private fun generateMethod(method: PsiMethod, sb: StringBuilder) {
        // 1 php函数声明
        method.parameters.joinTo(sb, ",", "\n\tpublic static function ${method.name}(", "){\n") {
            '$' + it.name!!
        }
        // 2 php函数体
        sb.append("\t\treturn '")
        val returnType = PsiTypeUtils.getRefClass(method.returnType, method)
        // java函数声明
        method.parameters.joinTo(sb, ",", "$returnType ${method.name}(", ")") {
            PsiTypeUtils.getRefClass(it.type as PsiType?, method)
        }
        sb.append("';\n\t}\n")
    }
}
