package com.ven.assistsxkit.plugin

import com.google.gson.JsonObject
import com.ven.assistsxkit.model.Plugin

/**
 * 将 Plugin 转为可安全暴露给 JS 的 JSON（不含 password 等敏感字段）
 */
object PluginInfoMapper {

    fun toJsonObject(plugin: Plugin): JsonObject {
        return JsonObject().apply {
            addProperty("id", plugin.id)
            addProperty("name", plugin.name)
            addProperty("packageName", plugin.packageName)
            addProperty("versionName", plugin.versionName)
            addProperty("versionCode", plugin.versionCode)
            addProperty("description", plugin.description)
            addProperty("path", plugin.path)
            addProperty("index", plugin.index ?: "")
            addProperty("port", plugin.port)
            addProperty("needScreenCapture", plugin.needScreenCapture)
        }
    }
}
