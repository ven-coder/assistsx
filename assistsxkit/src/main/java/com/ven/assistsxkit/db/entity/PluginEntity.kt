package com.ven.assistsxkit.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Plugin数据表实体类
 * 对应Plugin.kt的数据表结构，不包含Deprecated字段和password相关字段
 */
@Entity(tableName = "plugins")
data class PluginEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    // 插件名称
    val name: String,
    val versionName: String,
    val versionCode: Int,
    val description: String,
    val needScreenCapture: Boolean,
    val path: String,
    val index: String?,
    val indexInOverlay: Boolean?,
    val icon: String,
    val packageName: String,
    // 新增的port字段，默认值12987
    val port: Int = -1,
    // 数据更新时间
    val updateTime: Long = System.currentTimeMillis()
)
