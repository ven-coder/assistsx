package com.ven.assistsxkit.plugin

import com.blankj.utilcode.util.PathUtils
import java.io.File

/**
 * 插件数据库目录隔离：在 internalAppDbs 下追加 db-{packageName} 子目录存放逻辑库名。
 */
object PluginDbPaths {

    /**
     * 将逻辑库名转换为插件隔离后的物理数据库文件路径。
     * 例：dbName=data.db, package=com.aaa.bbb → {internalAppDbs}/db-com.aaa.bbb/data.db
     */
    fun scopedDbPath(logicalDbName: String, pluginPackageName: String): String {
        if (logicalDbName.isBlank()) {
            throw IllegalArgumentException("dbName不能为空")
        }
        if (pluginPackageName.isBlank()) {
            return PathUtils.getInternalAppDbPath(logicalDbName)
        }
        val pluginDir = "db-$pluginPackageName"
        val scopedDir = File(PathUtils.getInternalAppDbsPath(), pluginDir).absolutePath
        return File(scopedDir, logicalDbName).absolutePath
    }

    /**
     * 判断路径是否已在插件隔离目录 db-{packageName} 下（幂等检测）。
     */
    fun isScopedDbPath(dbPath: String, pluginPackageName: String): Boolean {
        if (pluginPackageName.isBlank() || dbPath.isBlank()) return false
        val pluginDir = "db-$pluginPackageName"
        val normalized = File(dbPath).absolutePath
        val suffix = "${File.separator}$pluginDir${File.separator}"
        return normalized.contains(suffix) || normalized.endsWith("${File.separator}$pluginDir")
    }
}
