package com.ven.assistsxkit.plugin

import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.LogUtils
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.ven.assists.web.CallInterceptResult
import com.ven.assists.web.CallMethod
import com.ven.assists.web.CallRequest
import com.ven.assists.web.CallResponse
import com.ven.assists.web.createResponse
import com.ven.assists.web.floating.FloatCallMethod
import com.ven.assists.web.db.DbCallMethod
import com.ven.assists.web.db.DbDatabaseManager
import com.ven.assists.web.log.AssistsLogCallMethod
import com.ven.assistsxkit.model.getDomain
import com.ven.assistsxkit.server.PluginWebServerManager

/**
 * AssistsX 插件 WebView 统一拦截器：getCurrentPlugin、URL 补全、db 目录隔离
 */
object PluginWebViewInterceptors {

    fun createMainCallIntercept(): (String) -> CallInterceptResult = intercept@{ json ->
        val request = parseRequest(json) ?: return@intercept CallInterceptResult(false, json)

        if (request.method == PluginCallMethod.getCurrentPlugin) {
            val plugin = PluginWebServerManager.getRunningPlugin()
            val data = plugin?.let { PluginInfoMapper.toJsonObject(it) }
            val response = request.createResponse(code = 0, data = data)
            return@intercept CallInterceptResult(true, GsonUtils.toJson(response))
        }

        if (request.method == CallMethod.loadWebViewOverlay || request.method == FloatCallMethod.open) {
            val url = request.arguments?.get("url")?.asString ?: ""
            PluginWebServerManager.getRunningPlugin()?.getDomain()?.let { domain ->
                if (!url.startsWith("http")) {
                    val targetUrl = if (url.startsWith("/")) "$domain$url" else "$domain/$url"
                    request.arguments?.addProperty("url", targetUrl)
                    return@intercept CallInterceptResult(false, GsonUtils.toJson(request))
                }
            }
        }

        CallInterceptResult(false, json)
    }

    fun createDbCallIntercept(): (String) -> CallInterceptResult = intercept@{ json ->
        val request = parseRequest(json) ?: return@intercept CallInterceptResult(false, json)
        if (request.method !in DbCallMethod.pathAwareMethods) {
            return@intercept CallInterceptResult(false, json)
        }

        val packageName = PluginWebServerManager.getRunningPlugin()?.packageName
        if (packageName.isNullOrBlank()) {
            LogUtils.w("PluginWebViewInterceptors", "db intercept skipped: no running plugin packageName")
            return@intercept CallInterceptResult(false, json)
        }

        val dbPathArg = request.arguments?.get("dbPath")?.takeIf { !it.isJsonNull }?.asString
        val dbName = request.arguments?.get("dbName")?.takeIf { !it.isJsonNull }?.asString

        if (!dbPathArg.isNullOrBlank() && PluginDbPaths.isScopedDbPath(dbPathArg, packageName)) {
            return@intercept CallInterceptResult(false, json)
        }

        if (!dbPathArg.isNullOrBlank()) {
            val response = request.createResponse(
                code = -1,
                message = "插件环境不支持自定义 dbPath，请使用 dbName",
                data = null,
            )
            return@intercept CallInterceptResult(true, GsonUtils.toJson(response))
        }

        val effectiveDbName = dbName?.takeIf { it.isNotBlank() } ?: DbDatabaseManager.DEFAULT_DB_NAME
        val scopedPath = PluginDbPaths.scopedDbPath(effectiveDbName, packageName)
        val args = request.arguments ?: JsonObject()
        args.addProperty("dbPath", scopedPath)
        args.remove("dbName")
        val updated = CallRequest(
            method = request.method,
            arguments = args,
            nodes = request.nodes,
            node = request.node,
            callbackId = request.callbackId,
        )
        CallInterceptResult(false, GsonUtils.toJson(updated))
    }

    fun createLogCallIntercept(): (String) -> CallInterceptResult = intercept@{ json ->
        val request = parseRequest(json) ?: return@intercept CallInterceptResult(false, json)
        if (request.method !in AssistsLogCallMethod.pathAwareMethods) {
            return@intercept CallInterceptResult(false, json)
        }

        val packageName = PluginWebServerManager.getRunningPlugin()?.packageName
        if (packageName.isNullOrBlank()) {
            LogUtils.w("PluginWebViewInterceptors", "log intercept skipped: no running plugin packageName")
            return@intercept CallInterceptResult(false, json)
        }

        val originalDirPath = request.arguments?.get("dirPath")?.takeIf { !it.isJsonNull }?.asString
        val originalFileName = request.arguments?.get("fileName")?.takeIf { !it.isJsonNull }?.asString
        val scopedDir = PluginLogPaths.scopedLogDir(originalDirPath, packageName)
        val effectiveFileName = originalFileName?.trim()?.takeIf { it.isNotEmpty() }
            ?: PluginLogPaths.DEFAULT_LOG_FILE_NAME
        val args = request.arguments ?: JsonObject()
        args.addProperty("dirPath", scopedDir)
        args.addProperty("fileName", effectiveFileName)
        val updated = CallRequest(
            method = request.method,
            arguments = args,
            nodes = request.nodes,
            node = request.node,
            callbackId = request.callbackId,
        )
        CallInterceptResult(false, GsonUtils.toJson(updated))
    }

    private fun parseRequest(json: String): CallRequest<JsonObject>? {
        return runCatching {
            GsonUtils.fromJson<CallRequest<JsonObject>>(
                json,
                object : TypeToken<CallRequest<JsonObject>>() {}.type,
            )
        }.getOrNull()
    }
}
