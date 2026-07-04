package com.ven.assistsxkit.common

import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.SPUtils
import com.ven.assistsxkit.model.Plugin
import com.ven.assistsxkit.repository.PluginRepository

/**
 * 将旧版 SP 中的插件列表一次性迁移到数据库，迁移完成后清除 SP 数据。
 */
object LegacyPluginMigration {

    private const val LEGACY_SP_KEY = "installed_plugins"

    suspend fun migrateIfNeeded(repository: PluginRepository) {
        val legacyJson = SPUtils.getInstance().getString(LEGACY_SP_KEY, "")
        if (legacyJson.isBlank() || legacyJson == "[]") {
            return
        }
        val plugins = runCatching {
            GsonUtils.fromJson<List<Plugin>>(legacyJson, GsonUtils.getListType(Plugin::class.java))
        }.getOrNull().orEmpty()
        if (plugins.isEmpty()) {
            SPUtils.getInstance().remove(LEGACY_SP_KEY)
            return
        }
        plugins.forEach { repository.insertPlugin(it) }
        SPUtils.getInstance().remove(LEGACY_SP_KEY)
    }
}
