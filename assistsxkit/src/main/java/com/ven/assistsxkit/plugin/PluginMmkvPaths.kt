package com.ven.assistsxkit.plugin

import com.blankj.utilcode.util.PathUtils
import com.ven.assists.web.mmkv.MmkvManager
import java.io.File

/**
 * 插件 MMKV 目录隔离：在 internalAppFiles 下追加 mmkv-{packageName} 子目录。
 */
object PluginMmkvPaths {

    /**
     * 将插件包名转换为隔离后的 MMKV 根目录。
     * 例：package=com.aaa.bbb → {internalAppFiles}/mmkv-com.aaa.bbb
     */
    fun scopedRootPath(pluginPackageName: String): String {
        if (pluginPackageName.isBlank()) {
            return PathUtils.getInternalAppFilesPath()
        }
        val pluginDir = "mmkv-$pluginPackageName"
        return File(PathUtils.getInternalAppFilesPath(), pluginDir).absolutePath
    }

    /**
     * 判断路径是否已在插件隔离目录 mmkv-{packageName} 下（幂等检测）。
     */
    fun isScopedRootPath(rootPath: String, pluginPackageName: String): Boolean {
        if (pluginPackageName.isBlank() || rootPath.isBlank()) return false
        val pluginDir = "mmkv-$pluginPackageName"
        val normalized = File(rootPath).absolutePath
        val suffix = "${File.separator}$pluginDir"
        return normalized.endsWith(suffix) || normalized.contains("$suffix${File.separator}")
    }

    /**
     * 兜底：当无法使用自定义 rootPath 时，将逻辑 mmkvId 改写为带插件前缀的名称。
     */
    fun scopedMmkvId(logicalMmkvId: String, pluginPackageName: String): String {
        val effectiveId = logicalMmkvId.trim().takeIf { it.isNotEmpty() }
            ?: MmkvManager.DEFAULT_MMKV_ID
        if (pluginPackageName.isBlank()) {
            return effectiveId
        }
        val prefix = "mmkv-$pluginPackageName-"
        return if (effectiveId.startsWith(prefix)) {
            effectiveId
        } else {
            "$prefix$effectiveId"
        }
    }
}
