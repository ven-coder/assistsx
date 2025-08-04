package com.ven.assistsxkit.model

import com.google.gson.annotations.SerializedName
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
    // 是否启用安装密码
    val passwordEnabled: Boolean = false,
    // 安装密码
    val password: String? = ""
) : Serializable {
    @Deprecated("使用indexPath")
    fun mainPath(): String {
        if (path.startsWith("http")) {
            return "$path/$main"
        } else {
            return "file://$path/$main"
        }
    }

    fun indexPath(): String {
        return if (path.startsWith("http")) {
            "$path/$index"
        } else {
            "file://$path/$index"
        }
    }
}