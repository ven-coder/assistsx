package com.ven.assistsxkit.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

/**
 * 插件实体类
 * @property name 插件名称
 * @property path 插件路径
 */
data class Plugin(
    val name: String = "",
    var id: String = "",
    val version: String = "",
    val description: String = "",
    val isShowOverlay: Boolean = false,
    val needScreenCapture: Boolean = false,
    val overlayTitle: String = "",
    var path: String = "",
    val main: String = "",
    val icon: String = "",
    val packageName: String = "",  // 插件包名，用于创建插件目录
    // 是否启用安装密码
    val passwordEnabled: Boolean = false,
    // 安装密码
    val password: String? = ""
) : Serializable {
    fun mainPath(): String {
        if (path.startsWith("http")) {
            return "$path/$main"
        } else {
            return "file://$path/$main"
        }
    }

    fun domain(): String {
        return if (path.startsWith("http")) {
            return path
        } else {
            return "file://$path"
        }
    }
}