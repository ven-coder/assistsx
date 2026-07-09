package com.ven.assistsxkit.plugin

import com.ven.assists.web.ASWebView
import com.ven.assists.web.CallInterceptResult
import com.ven.assists.web.floating.FloatWindowBridge
import com.ven.assistsxkit.XWebview
import com.ven.assistsxkit.model.getDomain
import com.ven.assistsxkit.server.PluginWebServerManager

/**
 * AssistsX 插件 WebView 全局 Bridge 注册（应用启动时安装一次）
 */
object PluginWebViewBridge {

    private var mainIntercept: ((String) -> CallInterceptResult)? = null
    private var dbIntercept: ((String) -> CallInterceptResult)? = null
    private var logIntercept: ((String) -> CallInterceptResult)? = null
    private var mmkvIntercept: ((String) -> CallInterceptResult)? = null

    @Volatile
    private var installed = false

    /** 应用启动时调用一次，重复调用会先移除旧注册再重新安装 */
    @JvmStatic
    fun ensureInstalled() {
        if (installed) {
            return
        }
        synchronized(this) {
            if (installed) {
                return
            }
            uninstallInternal()
            mainIntercept = PluginWebViewInterceptors.createMainCallIntercept().also {
                ASWebView.globalJavascriptCallIntercepts.add(it)
            }
            dbIntercept = PluginWebViewInterceptors.createDbCallIntercept().also {
                ASWebView.globalDbCallIntercepts.add(it)
            }
            logIntercept = PluginWebViewInterceptors.createLogCallIntercept().also {
                ASWebView.globalLogCallIntercepts.add(it)
            }
            mmkvIntercept = PluginWebViewInterceptors.createMmkvCallIntercept().also {
                ASWebView.globalMmkvCallIntercepts.add(it)
            }
            ASWebView.globalUrlTransform = { url ->
                PluginWebServerManager.getRunningPlugin()?.getDomain()?.let { domain ->
                    if (!url.startsWith("http")) {
                        return@let if (url.startsWith("/")) "$domain$url" else "$domain/$url"
                    }
                }
                url
            }
            FloatWindowBridge.webViewProvider = { context -> XWebview(context) }
            installed = true
        }
    }

    private fun uninstallInternal() {
        mainIntercept?.let { ASWebView.globalJavascriptCallIntercepts.remove(it) }
        dbIntercept?.let { ASWebView.globalDbCallIntercepts.remove(it) }
        logIntercept?.let { ASWebView.globalLogCallIntercepts.remove(it) }
        mmkvIntercept?.let { ASWebView.globalMmkvCallIntercepts.remove(it) }
        mainIntercept = null
        dbIntercept = null
        logIntercept = null
        mmkvIntercept = null
        ASWebView.globalUrlTransform = null
        FloatWindowBridge.webViewProvider = null
    }
}
