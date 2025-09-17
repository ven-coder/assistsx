package com.ven.assistsxkit.common

import android.content.Context
import com.ven.assistsxkit.App
import com.ven.assistsxkit.repository.PluginRepository

/**
 * Context扩展函数
 * 提供便捷的方式获取App实例和Repository
 */

/**
 * 获取App实例
 */
fun Context.getApp(): App {
    return applicationContext as App
}

/**
 * 获取PluginRepository实例
 */
fun Context.getPluginRepository(): PluginRepository {
    return App.pluginRepository
}
