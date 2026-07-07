package com.ven.assistsxkit.plugin

import com.blankj.utilcode.util.PathUtils
import java.io.File

/**
 * 插件日志目录隔离：在逻辑目录下追加 log-{packageName} 子目录。
 */
object PluginLogPaths {

    /**
     * 将逻辑日志目录转换为插件隔离后的物理目录。
     * 例：logical=/abs/abc/cba, package=com.aaa.bbb → /abs/abc/cba/log-com.aaa.bbb
     * 未传 logical 时 → {internalFiles}/log-com.aaa.bbb
     * 已以 log-{packageName} 结尾时幂等返回。
     */
    fun scopedLogDir(logicalDirPath: String?, pluginPackageName: String): String {
        if (pluginPackageName.isBlank()) {
            return logicalDirPath?.trim()?.trimEnd('/')?.let { File(it).absolutePath }
                ?: PathUtils.getInternalAppFilesPath()
        }
        val pluginDir = "log-$pluginPackageName"
        if (logicalDirPath.isNullOrBlank()) {
            return File(PathUtils.getInternalAppFilesPath(), pluginDir).absolutePath
        }
        val trimmed = logicalDirPath.trim().trimEnd('/')
        val normalized = File(trimmed).absolutePath
        val suffix = "${File.separator}$pluginDir"
        if (normalized.endsWith(suffix) || normalized.endsWith(pluginDir)) {
            return normalized
        }
        return File(normalized, pluginDir).absolutePath
    }
}
