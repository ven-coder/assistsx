package com.ven.assistsxkit.repository

import android.content.Context
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.SPUtils
import com.ven.assistsxkit.common.SPKeys
import com.ven.assistsxkit.db.AppDatabase
import com.ven.assistsxkit.db.entity.PluginEntity
import com.ven.assistsxkit.db.toEntity
import com.ven.assistsxkit.db.toPlugin
import com.ven.assistsxkit.model.Plugin
import com.ven.assistsxkit.model.isRemote
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Plugin 数据仓库，负责 SP 与 DB 的插件数据同步及 CRUD。
 */
class PluginRepository(context: Context) {

    private val database = AppDatabase.getDatabase(context)
    private val pluginDao = database.pluginDao()

    /**
     * 从 SP 读取已安装插件列表
     */
    fun getPluginsFromSharedPreferences(): List<Plugin> {
        return runCatching {
            val pluginsJson = SPUtils.getInstance().getString(SPKeys.INSTALLED_PLUGINS, "[]")
            GsonUtils.fromJson<List<Plugin>>(pluginsJson, GsonUtils.getListType(Plugin::class.java))
                ?: emptyList()
        }.getOrDefault(emptyList())
    }

    /**
     * 将插件列表写入 SP
     */
    fun savePluginsToSharedPreferences(plugins: List<Plugin>) {
        SPUtils.getInstance().put(SPKeys.INSTALLED_PLUGINS, GsonUtils.toJson(plugins))
    }

    /**
     * 启动时将 SP 数据全量同步到 DB，保证历史数据字段完整
     */
    suspend fun syncFromSharedPreferences() {
        getPluginsFromSharedPreferences().forEach { plugin ->
            insertPlugin(plugin)
        }
    }

    /**
     * 保存单个插件：写入 DB 并返回含 localPort 等完整对象，供回写 SP
     */
    suspend fun savePlugin(plugin: Plugin): Plugin {
        return insertPlugin(plugin)
    }

    /**
     * 获取所有插件
     */
    fun getAllPlugins(): Flow<List<Plugin>> {
        return pluginDao.getAllPlugins().map { entities ->
            entities.map { it.toPlugin() }
        }
    }

    /**
     * 根据 ID 获取插件
     */
    suspend fun getPluginById(id: Long): Plugin? {
        return pluginDao.getPluginById(id)?.toPlugin()
    }

    /**
     * 根据包名获取插件
     */
    suspend fun getPluginByPackageName(packageName: String): Plugin? {
        return pluginDao.getPluginByPackageName(packageName)?.toPlugin()
    }

    /**
     * 插入或更新插件，按路径类型写入 localPath / remoteAddress；返回含最终 localPort 的 Plugin
     */
    suspend fun insertPlugin(plugin: Plugin): Plugin {
        val existingPlugin = pluginDao.getPluginByPackageName(plugin.packageName)
        val localPort = resolveLocalPort(plugin, existingPlugin)
        val entity = plugin.toEntity(
            dbId = existingPlugin?.id ?: 0,
            localPort = localPort
        ).copy(updateTime = System.currentTimeMillis())

        if (existingPlugin != null) {
            pluginDao.updatePlugin(entity)
        } else {
            pluginDao.insertPlugin(entity)
        }
        return entity.toPlugin()
    }

    /**
     * 插入多个插件
     */
    suspend fun insertPlugins(plugins: List<Plugin>) {
        plugins.forEach { plugin ->
            insertPlugin(plugin)
        }
    }

    /**
     * 更新插件
     */
    suspend fun updatePlugin(plugin: Plugin): Plugin {
        val existing = pluginDao.getPluginByPackageName(plugin.packageName)
        val localPort = resolveLocalPort(plugin, existing)
        val entity = plugin.toEntity(
            dbId = existing?.id ?: 0,
            localPort = localPort
        ).copy(updateTime = System.currentTimeMillis())
        pluginDao.updatePlugin(entity)
        return entity.toPlugin()
    }

    /**
     * 删除插件
     */
    suspend fun deletePlugin(plugin: Plugin) {
        pluginDao.deletePluginByPackageName(plugin.packageName)
    }

    /**
     * 根据 ID 删除插件
     */
    suspend fun deletePluginById(id: Long) {
        pluginDao.deletePluginById(id)
    }

    /**
     * 根据包名删除插件
     */
    suspend fun deletePluginByPackageName(packageName: String) {
        pluginDao.deletePluginByPackageName(packageName)
    }

    /**
     * 清空所有插件
     */
    suspend fun deleteAllPlugins() {
        pluginDao.deleteAllPlugins()
    }

    /**
     * 确定 localPort：远程插件不分配；本地插件保证全局唯一
     */
    private suspend fun resolveLocalPort(plugin: Plugin, existing: PluginEntity?): Int {
        if (plugin.isRemote()) {
            return -1
        }
        val usedPorts = pluginDao.getAllUsedLocalPorts()
            .filter { existing == null || it != existing.localPort }
            .toSet()

        if (plugin.port > 0 && plugin.port !in usedPorts) {
            return plugin.port
        }
        if (existing != null && existing.localPort > 0 && existing.localPort !in usedPorts) {
            return existing.localPort
        }
        return generateUniqueLocalPort(usedPorts)
    }

    /**
     * 生成唯一的 localPort，从 3127 起递增
     */
    private fun generateUniqueLocalPort(usedPorts: Set<Int>): Int {
        var port = 3127
        while (usedPorts.contains(port)) {
            port++
        }
        return port
    }
}
