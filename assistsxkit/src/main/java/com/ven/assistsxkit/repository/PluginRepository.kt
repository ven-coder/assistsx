package com.ven.assistsxkit.repository

import android.content.Context
import com.ven.assistsxkit.db.AppDatabase
import com.ven.assistsxkit.db.entity.PluginEntity
import com.ven.assistsxkit.model.Plugin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Plugin数据仓库
 * 负责管理Plugin数据的CRUD操作
 */
class PluginRepository(context: Context) {

    private val database = AppDatabase.getDatabase(context)
    private val pluginDao = database.pluginDao()

    /**
     * 获取所有插件
     */
    fun getAllPlugins(): Flow<List<Plugin>> {
        return pluginDao.getAllPlugins().map { entities ->
            entities.map { it.toPlugin() }
        }
    }

    /**
     * 根据ID获取插件
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
     * 插入插件
     * 如果存在相同的packageName则更新，否则插入
     * 插入时会自动生成不重复的port值，并自动设置更新时间
     */
    suspend fun insertPlugin(plugin: Plugin) {
        val existingPlugin = pluginDao.getPluginByPackageName(plugin.packageName)
        if (existingPlugin != null) {
            // 存在相同packageName的插件，更新数据
            val updatedEntity = plugin.toEntity().copy(
                id = existingPlugin.id, 
                port = existingPlugin.port,
                updateTime = System.currentTimeMillis()
            )
            pluginDao.updatePlugin(updatedEntity)
        } else {
            // 不存在相同packageName的插件，插入新数据
            val entity = plugin.toEntity()
            val entityWithPort = entity.copy(
                port = generateUniquePort(),
                updateTime = System.currentTimeMillis()
            )
            pluginDao.insertPlugin(entityWithPort)
        }
    }

    /**
     * 插入多个插件
     * 如果存在相同的packageName则更新，否则插入
     */
    suspend fun insertPlugins(plugins: List<Plugin>) {
        plugins.forEach { plugin ->
            insertPlugin(plugin) // 复用单个插入的逻辑
        }
    }

    /**
     * 更新插件
     * 更新时会自动设置更新时间
     */
    suspend fun updatePlugin(plugin: Plugin) {
        val entity = plugin.toEntity().copy(updateTime = System.currentTimeMillis())
        pluginDao.updatePlugin(entity)
    }

    /**
     * 删除插件
     */
    suspend fun deletePlugin(plugin: Plugin) {
        pluginDao.deletePlugin(plugin.toEntity())
    }

    /**
     * 根据ID删除插件
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
     * 生成唯一的port值
     * 从3127开始，找到第一个未被使用的port值
     */
    private suspend fun generateUniquePort(): Int {
        val usedPorts = pluginDao.getAllUsedPorts().toSet()
        var port = 3127 // 起始端口号
        
        // 从起始端口开始查找第一个未被使用的端口
        while (usedPorts.contains(port)) {
            port++
        }
        
        return port
    }
}

/**
 * 将PluginEntity转换为Plugin
 */
private fun PluginEntity.toPlugin(): Plugin {
    return Plugin(
        name = name,
        versionName = versionName,
        versionCode = versionCode,
        description = description,
        needScreenCapture = needScreenCapture,
        path = path,
        index = index,
        indexInOverlay = indexInOverlay,
        icon = icon,
        packageName = packageName,
        port = port
    )
}

/**
 * 将Plugin转换为PluginEntity
 */
private fun Plugin.toEntity(): PluginEntity {
    return PluginEntity(
        name = name,
        versionName = versionName,
        versionCode = versionCode,
        description = description,
        needScreenCapture = needScreenCapture,
        path = path,
        index = index,
        indexInOverlay = indexInOverlay,
        icon = icon,
        packageName = packageName,
        port = port
    )
}
