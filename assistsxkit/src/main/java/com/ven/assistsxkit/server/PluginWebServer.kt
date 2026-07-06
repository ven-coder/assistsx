package com.ven.assistsxkit.server

import com.ven.assists.utils.CoroutineWrapper
import com.ven.assistsxkit.model.Plugin
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.flow.MutableSharedFlow
import java.io.File
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/**
 * 基于 NanoHTTPD 的简易本地服务器，用于为保存在应用私有目录中的插件站点提供 HTTP 访问。
 */
object PluginWebServerManager {
    // 选择一个较不常用的端口，避免与常见端口冲突
    const val DEFAULT_PORT = 12987

    private var server: PluginHttpServer? = null
    private var currentPort: Int = DEFAULT_PORT

    val startFlow = MutableSharedFlow<Plugin>()

    var plugin: Plugin? = null

    /** 获取当前正在运行的插件（本地 HTTP 服务已启动或远程插件已加载时有效） */
    @JvmStatic
    fun getRunningPlugin(): Plugin? = plugin

    /** 设置当前运行的插件（远程插件等未走 startServer 的场景） */
    @JvmStatic
    fun setRunningPlugin(plugin: Plugin?) {
        this.plugin = plugin
    }

    /**
     * 启动本地服务器，默认使用插件在数据库中分配的 localPort（映射为 plugin.port）。
     * @return 实际启动的端口号
     */
    @Synchronized
    fun startServer(plugin: Plugin, port: Int = plugin.port): Int {
        // 若已有服务在运行，先停止
        stopServer()
        currentPort = port
        return try {
            val rootDir = File(plugin.path)
            val indexRelativePath = plugin.index?.takeIf { it.isNotBlank() } ?: "index.html"
            server = PluginHttpServer(rootDir, currentPort, indexRelativePath).apply {
                start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
            }
            CoroutineWrapper.launch { startFlow.emit(plugin) }
            this.plugin = plugin
            currentPort
        } catch (e: Exception) {
            -1
        }
    }

    /** 停止本地服务器 */
    @Synchronized
    fun stopServer() {
        try {
            server?.stop()
        } catch (ignored: Exception) {
        } finally {
            server = null
            plugin = null
        }
    }
}

private class PluginHttpServer(
    rootDir: File,
    port: Int,
    private val indexRelativePath: String,
) : NanoHTTPD(port) {

    private val rootCanonical: File = rootDir.canonicalFile

    override fun serve(session: IHTTPSession): Response {
        val rawPath = session.uri.substringBefore('?').trimStart('/')
        val relativeForRequest = if (rawPath.isEmpty()) {
            indexRelativePath
        } else {
            try {
                URLDecoder.decode(rawPath, StandardCharsets.UTF_8.name())
            } catch (_: Exception) {
                rawPath
            }
        }
        val resolved = resolveUnderRoot(relativeForRequest)
            ?: return newFixedLengthResponse(Response.Status.FORBIDDEN, MIME_PLAINTEXT, "403 Forbidden")
        if (resolved.isFile) {
            return serveStaticFile(resolved)
        }
        return serveSpaIndex()
    }

    /** 将相对路径解析为 root 下的规范路径；若路径逃出根目录则返回 null */
    private fun resolveUnderRoot(relativePath: String): File? {
        val candidate = File(rootCanonical, relativePath).canonicalFile
        val rootPath = rootCanonical.path
        val candPath = candidate.path
        if (candPath != rootPath && !candPath.startsWith(rootPath + File.separator)) {
            return null
        }
        return candidate
    }

    private fun serveStaticFile(file: File): Response {
        val mime = NanoHTTPD.getMimeTypeForFile(file.name)
        return newChunkedResponse(Response.Status.OK, mime, file.inputStream())
    }

    /** SPA History 模式：非真实静态文件时返回入口 index.html（与直接打开站点一致） */
    private fun serveSpaIndex(): Response {
        val indexFile = resolveUnderRoot(indexRelativePath)
            ?: return newFixedLengthResponse(Response.Status.FORBIDDEN, MIME_PLAINTEXT, "403 Forbidden")
        if (!indexFile.isFile) {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "404 Not Found")
        }
        return newChunkedResponse(Response.Status.OK, MIME_HTML, indexFile.inputStream())
    }
} 