package io.yapix.parse.util

object PathUtils {
    /**
     * 路径处理, 会增加前缀
     */
    fun path(path: String?): String? {
        if (path.isNullOrEmpty()) {
            return null
        }
        if (path.startsWith("/"))
            return path.trim()

        return  "/" + path.trim()
    }

    /**
     * 路径拼接
     */
    fun path(path: String?, subPath: String?): String? {
        var path = path(path)
        var subPath = path(subPath)
        if (path == null)
            return subPath

        if (subPath == null)
            return path

        if (path.endsWith("/") && subPath.startsWith("/"))
            return path + subPath.substring(1)

        return path + subPath
    }
}
