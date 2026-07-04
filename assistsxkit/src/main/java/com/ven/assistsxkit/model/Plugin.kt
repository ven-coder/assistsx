package com.ven.assistsxkit.model

import com.ven.assistsxkit.server.PluginWebServerManager
import java.io.Serializable

/**
 * 插件实体类
 * @property name 插件名称
 * @property path 插件路径
 */
data class Plugin(
    var id: String = "",
    // 插件名称
    val name: String = "",
    @Deprecated("使用versionName")
    val version: String = "",
    val versionName: String = "",
    val versionCode: Int = 0,
    val description: String = "",
    @Deprecated("indexInFloatWindow")
    val isShowOverlay: Boolean = false,
    val needScreenCapture: Boolean = false,
    @Deprecated("由web定义")
    val overlayTitle: String = "",
    var path: String = "",
    @Deprecated("使用index")
    val main: String = "",
    val index: String? = "",
    val indexInOverlay: Boolean? = false,
    val icon: String = "",
    val packageName: String = "",  // 插件包名，用于创建插件目录
    // 插件端口号
    val port: Int = PluginWebServerManager.DEFAULT_PORT,
    // 是否启用安装密码
    val passwordEnabled: Boolean = false,
    // 安装密码
    val password: String? = ""
) : Serializable {

}

/** 是否为远程插件（http/https 地址） */
fun Plugin.isRemote(): Boolean = path.startsWith("http", ignoreCase = true)

fun Plugin.url(port: Int = this.port): String {

    if (isRemote()) {
        return "$path/$index"
    } else {
        return "http://127.0.0.1:$port/$index"
    }

}

fun Plugin.getDomain(): String {
    return if (isRemote()) {
        path
    } else {
        "http://127.0.0.1:$port"
    }
}

fun sanitizeRemotePluginPackageName(baseUrl: String, fallback: String = "online_plugin"): String {
    return baseUrl
        .replace("https://", "")
        .replace("http://", "")
        .replace("/", ".")
        .replace(":", "_")
        .replace(Regex("[^a-zA-Z0-9._-]"), "_")
        .ifEmpty { fallback }
}

fun Plugin.withRemoteDefaults(baseUrl: String): Plugin {
    val sanitizedPackageName = sanitizeRemotePluginPackageName(baseUrl)
    return copy(
        path = baseUrl,
        name = name.takeIf { it.isNotBlank() } ?: baseUrl,
        description = description.takeIf { it.isNotBlank() } ?: baseUrl,
        packageName = packageName.takeIf { it.isNotBlank() } ?: sanitizedPackageName,
        version = version.takeIf { it.isNotBlank() } ?: "1.0.0",
        versionName = versionName.takeIf { it.isNotBlank() } ?: "1.0.0"
    )
}

fun createDefaultRemotePlugin(baseUrl: String, id: String = ""): Plugin {
    return Plugin(
        id = id,
        name = baseUrl,
        version = "1.0.0",
        versionName = "1.0.0",
        description = baseUrl,
        path = baseUrl,
        indexInOverlay = true,
        packageName = sanitizeRemotePluginPackageName(baseUrl)
    )
}