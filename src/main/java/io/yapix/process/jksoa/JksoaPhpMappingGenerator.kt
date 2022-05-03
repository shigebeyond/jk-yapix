package io.yapix.process.jksoa

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiType
import io.yapix.parse.util.PsiAnnotationUtils
import io.yapix.parse.util.PsiTypeUtils
import io.yapix.parse.util.PsiUtils

/**
 * 为jksoa的java接口类，生成php映射类
 */
object JksoaPhpMappingGenerator {

    fun generate(psiClass: PsiClass): String {
        // 拼接php代码
        // 1 拼接类
        val pack = PsiUtils.getPakcageName(psiClass) // java包
        val namespace = pack.replace('.', '\\') // php命名空间
        val sb = StringBuilder("<?php\nnamespace ${namespace};\n\nclass ${psiClass.name} {\n")
        // 2 拼接方法
        val methods = psiClass.methods.filter {
            !it.hasAnnotation("kotlin.jvm.JvmDefault") // 忽略默认方法
        }
        for (method in methods) {
            // 2.1 php函数声明
            method.parameters.joinTo(sb, ",", "\n\tfunction ${method.name}(", "){\n") {
                '$' + it.name!!
            }
            // 2.2 php函数体
            sb.append("\t\treturn '")
            // 2.2.1 java注解
            for(ann in method.annotations){
//                val text = ann.text;
                val text = PsiAnnotationUtils.getAnnotationRemark(ann);
                if(text != null && !text.contains("NotNull") && !text.contains("Throws") && !text.contains("Suspendable"))
                    sb.append(text).append("\n\t\t\t\t")
            }
            // 2.2.2 java函数声明
            val returnType = PsiTypeUtils.getRefClass(method.returnType, method)
            method.parameters.joinTo(sb, ",", "$returnType ${method.name}(", ")") {
                PsiTypeUtils.getRefClass(it.type as PsiType?, method)
            }
            sb.append("';\n\t}\n")
        }
        sb.append("\n}")
        return sb.toString()
    }
}
