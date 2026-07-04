package com.ven.assistsxkit.db

import com.ven.assistsxkit.db.entity.PluginEntity
import com.ven.assistsxkit.model.Plugin
import com.ven.assistsxkit.model.isRemote
import com.ven.assistsxkit.server.PluginWebServerManager

/**
 * Plugin 与 PluginEntity 之间的字段映射（不含 @Deprecated 字段）。
 */
internal fun PluginEntity.toPlugin(): Plugin {
    val resolvedPath = remoteAddress.ifBlank { localPath }
    return Plugin(
        id = pluginUuid,
        name = name,
        versionName = versionName,
        versionCode = versionCode,
        description = description,
        needScreenCapture = needScreenCapture,
        path = resolvedPath,
        index = index,
        indexInOverlay = indexInOverlay,
        icon = icon,
        packageName = packageName,
        port = if (localPort > 0) localPort else PluginWebServerManager.DEFAULT_PORT,
        passwordEnabled = passwordEnabled,
        password = password
    )
}

internal fun Plugin.toEntity(dbId: Long = 0, localPort: Int = -1): PluginEntity {
    return PluginEntity(
        id = dbId,
        pluginUuid = id,
        name = name,
        versionName = versionName,
        versionCode = versionCode,
        description = description,
        needScreenCapture = needScreenCapture,
        localPath = if (isRemote()) "" else path,
        remoteAddress = if (isRemote()) path else "",
        index = index,
        indexInOverlay = indexInOverlay,
        icon = icon,
        packageName = packageName,
        passwordEnabled = passwordEnabled,
        password = password,
        localPort = if (isRemote()) -1 else localPort
    )
}

/** 将 DB 同步后的有效字段合并回 SP 对象，保留 @Deprecated 字段 */
internal fun Plugin.withDbSyncedFields(dbPlugin: Plugin): Plugin {
    return dbPlugin.copy(
        version = version,
        isShowOverlay = isShowOverlay,
        overlayTitle = overlayTitle,
        main = main
    )
}
