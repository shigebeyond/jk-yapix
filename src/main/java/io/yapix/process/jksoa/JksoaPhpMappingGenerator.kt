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
        // 映射方法 = 非默认方法
        val mappingMethods = psiClass.methods.filter {
            !it.hasAnnotation("kotlin.jvm.JvmDefault") // 忽略默认方法
        }

        // 2.1 注解属性：所有方法的注解
        sb.append("\n\t// ------------ 方法注解 ------------")
        val methodAnns = generateMethodAnotations(mappingMethods, sb)

        // 2.2 映射方法
        sb.append("\n\t// ------------ php对java调用映射的方法 ------------")
        for (method in mappingMethods) {
            generateMappingMethod(method, sb)
        }

        // 2.3 降级方法
        // 降级方法, 也是默认方法
        val fallbackMethodNames = getDegradeFallbackMethods(methodAnns)
        val fallbackMethods = fallbackMethodNames.mapNotNull {
            psiClass.findMethodsByName(it, false).firstOrNull()
        }
        sb.append("\n\t// ------------ 降级方法 ------------")
        for (method in fallbackMethods) {
            generateFallbackMethod(method, sb)
        }

        sb.append("\n}")
        return sb.toString()
    }

    /**
     * 收集类中所有方法中的 @Degrade 注解中 fallbackMethod - 降级的方法
     */
    public fun getDegradeFallbackMethods(methodAnns: Map<String, Map<String, Any?>>): List<String>{
        return methodAnns.values.mapNotNull { m ->
            val ann = m["net.jkcode.jkguard.annotation.Degrade"] as Map<String, Any?>?
            ann?.get("fallbackMethod") as String?
        }
    }

    /**
     * 生成注解属性
     */
    private fun generateMethodAnotations(methods: List<PsiMethod>, sb: StringBuilder): Map<String, Map<String, Any?>> {
        // 1 收集所有方法的注解信息: {方法名:{注解类名:{注解属性}}}, 其中 注解类名:{注解属性} 如 "net.jkcode.jkguard.annotation.GroupCombine":{"batchMethod":"listUsersByNameAsync","reqArgField":"name","respField":"","one2one":"true","flushQuota":"100","flushTimeoutMillis":"100"}
        val methodAnns = HashMap<String, Map<String, Any?>>()
        for(method in methods){
            val annMap = buildMethodAnnotationMap(method)
            if(annMap.isNotEmpty()) // 无注解的不收集，省的浪费注意力
                methodAnns[method.name] = annMap
        }

        // 2 注解转php array
        val json = PsiAnnotationUtils.gson.toJson(methodAnns) // 注解信息转json
        var arr = json.replace('{', '[').replace('}', ']') // json转php array表达式
        if(arr != "[]") { // 非空，继续格式化
            arr = arr.replace(":", " => ")
                .replace("],", "],\n\t\t")
                .replace("^\\[".toRegex(), "[\n\t\t")
                .replace("\\]$".toRegex(), "\n\t]")
                .replace("(?<=\n)\\s*\"\\w+\\.".toRegex()){
                    "\t\t\t\t\t\t\t\t" + it.value
                }
        }

        // 3 php属性声明
        sb.append("\n\tpublic static \$_methodAnnotations = ").append(arr).append(";\n")
        return methodAnns
    }

    /**
     * 构建单个方法的注解信息
     *   注解类名:{注解属性} 如 "net.jkcode.jkguard.annotation.GroupCombine":{"batchMethod":"listUsersByNameAsync","reqArgField":"name","respField":"","one2one":"true","flushQuota":"100","flushTimeoutMillis":"100"}
     */
    private fun buildMethodAnnotationMap(method: PsiMethod): HashMap<String, Any?> {
        val anns = HashMap<String, Any?>()
        for (ann in method.annotations) {
            val name = ann.qualifiedName
            if (name != null && !name.contains("NotNull") && !name.contains("Throws") && !name.contains("Suspendable") && !name.contains("JvmDefault")) {
                val attrs = PsiAnnotationUtils.getAnnotationAttributeMap(ann, true)
                anns[name] = attrs
            }
        }
        return anns
    }

    /**
     * 生成php映射方法
     */
    private fun generateMappingMethod(method: PsiMethod, sb: StringBuilder) {
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

    /**
     * 生成降级方法
     */
    private fun generateFallbackMethod(method: PsiMethod, sb: StringBuilder) {
        // 1 php函数声明
        method.parameters.joinTo(sb, ",", "\n\tpublic static function ${method.name}(", "){\n") {
            '$' + it.name!!
        }
        // 2 php函数体
        sb.append("\t\t// TODO: 手动实现降级方法 \n\t}\n")
    }
}
