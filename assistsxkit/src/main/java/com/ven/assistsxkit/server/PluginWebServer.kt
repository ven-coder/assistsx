package com.ven.assistsxkit.server

import android.util.Log
import com.ven.assists.utils.CoroutineWrapper
import com.ven.assistsxkit.model.Plugin
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File

/**
 * 基于 NanoHTTPD 的简易本地服务器，用于为保存在应用私有目录中的插件站点提供 HTTP 访问。
 */
object PluginWebServerManager {
    // 选择一个较不常用的端口，避免与常见端口冲突
    const val DEFAULT_PORT = 12987

    private var server: PluginHttpServer? = null
    private var currentPort: Int = DEFAULT_PORT

    val startFlow = MutableSharedFlow<Plugin>()

    /**
     * 启动本地服务器。
     * @return 实际启动的端口号
     */
    @Synchronized
    fun startServer(plugin: Plugin, port: Int = DEFAULT_PORT): Int {
        // 若已有服务在运行，先停止
        stopServer()
        currentPort = port
        return try {
            val rootDir = File(plugin.path)
            server = PluginHttpServer(rootDir, currentPort).apply {
                start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
            }
            CoroutineWrapper.launch { startFlow.emit(plugin) }
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
        }
    }
}

private class PluginHttpServer(private val rootDir: File, port: Int) : NanoHTTPD(port) {
    override fun serve(session: IHTTPSession): Response {
        // 处理 CORS 预检请求
        if (session.method == Method.OPTIONS) {
            return handleCorsPreflight()
        }
        
        var uriPath = session.uri.trimStart('/')
        if (uriPath.isEmpty()) {
            uriPath = "index.html" // 默认首页
        }
        val targetFile = File(rootDir, uriPath)
        if (!targetFile.exists() || targetFile.isDirectory) {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "404 Not Found")
        }
        val mime = NanoHTTPD.getMimeTypeForFile(targetFile.name)
        val response = newChunkedResponse(Response.Status.OK, mime, targetFile.inputStream())
        
        // 添加 CORS 头
        addCorsHeaders(response)
        
        return response
    }
    
    /**
     * 处理 CORS 预检请求
     */
    private fun handleCorsPreflight(): Response {
        val response = newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "")
        addCorsHeaders(response)
        return response
    }
    
    /**
     * 添加 CORS 头信息
     */
    private fun addCorsHeaders(response: Response) {
        response.addHeader("Access-Control-Allow-Origin", "*")
        response.addHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
        response.addHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With")
        response.addHeader("Access-Control-Max-Age", "86400")
        response.addHeader("Access-Control-Allow-Credentials", "true")
    }
} 