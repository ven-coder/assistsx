package com.ven.assistsxkit.model

import com.google.gson.annotations.SerializedName
import com.ven.assistsxkit.db.entity.PluginEntity
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

fun Plugin.url(port: Int = this.port): String {

    if (path.startsWith("http")) {
        return "$path/$index"
    } else {
        return "http://127.0.0.1:$port/$index"
    }

}

fun Plugin.getDomain(): String {
    return if (path.startsWith("http")) {
        path
    } else {
        "http://127.0.0.1:$port"
    }
}