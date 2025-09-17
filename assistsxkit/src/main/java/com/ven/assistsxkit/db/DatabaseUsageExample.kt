package com.ven.assistsxkit.db

import android.content.Context
import com.ven.assistsxkit.common.getPluginRepository
import com.ven.assistsxkit.model.Plugin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 数据库使用示例
 * 展示如何使用Room数据库进行Plugin数据的CRUD操作
 */
class DatabaseUsageExample(private val context: Context) {
    
    // 使用扩展函数获取Repository实例
    private val pluginRepository = context.getPluginRepository()
    
    /**
     * 示例：插入插件数据
     */
    fun insertPluginExample() {
        CoroutineScope(Dispatchers.IO).launch {
            val plugin = Plugin(
                id = "", // 空字符串，数据库会自动生成Long类型的自增ID
                name = "Test Plugin",
                versionName = "1.0.0",
                versionCode = 1,
                description = "A test plugin",
                needScreenCapture = false,
                path = "/data/data/com.example.plugin",
                index = "index.html",
                indexInOverlay = true,
                icon = "icon.png",
                packageName = "com.example.plugin",
                port = 12987
            )
            
            pluginRepository.insertPlugin(plugin)
        }
    }
    
    /**
     * 示例：获取所有插件
     */
    fun getAllPluginsExample() {
//        pluginRepository.getAllPlugins().collect { plugins ->
//            // 处理插件列表
//            plugins.forEach { plugin ->
//                println("Plugin: ${plugin.name}, Port: ${plugin.port}")
//            }
//        }
    }
    
    /**
     * 示例：根据ID获取插件
     */
    fun getPluginByIdExample() {
        CoroutineScope(Dispatchers.IO).launch {
            val plugin = pluginRepository.getPluginById(1L) // 使用Long类型的ID
            plugin?.let {
                println("Found plugin: ${it.name}")
            }
        }
    }
    
    /**
     * 示例：更新插件
     */
    fun updatePluginExample() {
        CoroutineScope(Dispatchers.IO).launch {
            val plugin = pluginRepository.getPluginById(1L) // 使用Long类型的ID
            plugin?.let {
                val updatedPlugin = it.copy(
                    name = "Updated Plugin Name",
                    port = 12988
                )
                pluginRepository.updatePlugin(updatedPlugin)
            }
        }
    }
    
    /**
     * 示例：删除插件
     */
    fun deletePluginExample() {
        CoroutineScope(Dispatchers.IO).launch {
            pluginRepository.deletePluginById(1L) // 使用Long类型的ID
        }
    }
}
