package com.ven.assistsxkit.server

import android.util.Log
import fi.iki.elonen.NanoHTTPD
import java.io.File

/**
 * 基于 NanoHTTPD 的简易本地服务器，用于为保存在应用私有目录中的插件站点提供 HTTP 访问。
 */
object PluginWebServerManager {
    // 选择一个较不常用的端口，避免与常见端口冲突
    private const val DEFAULT_PORT = 12987

    private var server: PluginHttpServer? = null
    private var currentPort: Int = DEFAULT_PORT

    /**
     * 启动本地服务器，根目录为 [rootDir]。
     * @return 实际启动的端口号
     */
    @Synchronized
    fun startServer(rootDir: File, port: Int = DEFAULT_PORT): Int {
        // 若已有服务在运行，先停止
        stopServer()
        currentPort = port
        return try {
            server = PluginHttpServer(rootDir, currentPort).apply {
                start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
            }
            currentPort
        } catch (e: Exception) {
            Log.e("PluginWebServer", "Start server failed: ${e.message}")
            // 启动失败时返回 -1 以示错误
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
        }
    }
}

private class PluginHttpServer(private val rootDir: File, port: Int) : NanoHTTPD(port) {
    override fun serve(session: IHTTPSession): Response {
        var uriPath = session.uri.trimStart('/')
        if (uriPath.isEmpty()) {
            uriPath = "index.html" // 默认首页
        }
        val targetFile = File(rootDir, uriPath)
        if (!targetFile.exists() || targetFile.isDirectory) {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "404 Not Found")
        }
        val mime = NanoHTTPD.getMimeTypeForFile(targetFile.name)
        return newChunkedResponse(Response.Status.OK, mime, targetFile.inputStream())
    }
} 