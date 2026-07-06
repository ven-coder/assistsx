package com.ven.assistsxkit.plugin

/**
 * 插件数据库命名工具：为 dbName 加上插件包名前缀以实现隔离
 */
object PluginDbNames {

    /**
     * 将逻辑库名转换为插件隔离后的物理库名。
     * 例：packageName=com.douyin.auto, dbName=data.db → com.douyin.auto_data.db
     * 已带前缀时幂等返回，不重复拼接。
     */
    fun scopedDbName(pluginPackageName: String, dbName: String): String {
        if (pluginPackageName.isBlank() || dbName.isBlank()) {
            return dbName
        }
        val prefix = "${pluginPackageName}_"
        return if (dbName.startsWith(prefix)) dbName else prefix + dbName
    }
}
