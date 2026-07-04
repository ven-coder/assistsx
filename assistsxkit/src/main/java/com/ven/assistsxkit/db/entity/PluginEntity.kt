package com.ven.assistsxkit.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Plugin 数据表实体类，仅包含有效字段（不含 @Deprecated 字段）；
 * 本地插件使用 localPath + localPort，远程插件使用 remoteAddress。
 */
@Entity(tableName = "plugins")
data class PluginEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    // SP 中的 UUID 标识
    val pluginUuid: String = "",
    val name: String = "",
    val versionName: String = "",
    val versionCode: Int = 0,
    val description: String = "",
    val needScreenCapture: Boolean = false,
    // 本地插件解压目录
    val localPath: String = "",
    // 远程插件地址（http/https）
    val remoteAddress: String = "",
    val index: String? = "",
    val indexInOverlay: Boolean? = false,
    val icon: String = "",
    val packageName: String = "",
    val passwordEnabled: Boolean = false,
    val password: String? = null,
    // 本地 Web 服务端口，-1 表示未分配或远程插件
    val localPort: Int = -1,
    val updateTime: Long = System.currentTimeMillis()
)
