package io.yapix.model

object DataTypes {

    const val BOOLEAN = "boolean" // boolean
    const val INTEGER = "integer" // byte、short、int、long
    const val NUMBER = "number" // float、double
    const val STRING = "string" // 字符串、枚举、date
    const val OBJECT = "object" // bean类型
    const val ARRAY = "array" // 数组、集合
    const val DATETIME = "datetime"
    const val FILE = "file"

    /**
     * 是否基础类型
     */
    fun isBasicType(type: String): Boolean {
        return when(type){
            BOOLEAN, INTEGER, NUMBER, STRING, DATETIME, FILE -> true
            else -> false
        }
    }
}
