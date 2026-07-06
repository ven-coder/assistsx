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
import com.ven.assistsxkit.model.getDomain
import com.ven.assistsxkit.server.PluginWebServerManager

/**
 * AssistsX 插件 WebView 统一拦截器：getCurrentPlugin、URL 补全、dbName 隔离前缀
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
        val dbName = request.arguments?.get("dbName")?.asString
        if (dbName.isNullOrBlank()) {
            return@intercept CallInterceptResult(false, json)
        }

        val packageName = PluginWebServerManager.getRunningPlugin()?.packageName
        if (packageName.isNullOrBlank()) {
            LogUtils.w("PluginWebViewInterceptors", "dbName intercept skipped: no running plugin packageName")
            return@intercept CallInterceptResult(false, json)
        }

        val scopedName = PluginDbNames.scopedDbName(packageName, dbName)
        if (scopedName != dbName) {
            request.arguments?.addProperty("dbName", scopedName)
            return@intercept CallInterceptResult(false, GsonUtils.toJson(request))
        }

        CallInterceptResult(false, json)
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
