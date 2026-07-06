package com.ven.assistsxkit.plugin

import com.ven.assists.web.ASWebView
import com.ven.assists.web.floating.FloatWindowBridge
import com.ven.assistsxkit.XWebview
import com.ven.assistsxkit.model.getDomain
import com.ven.assistsxkit.server.PluginWebServerManager
import java.util.concurrent.atomic.AtomicBoolean

/**
 * AssistsX 插件 WebView 全局 Bridge 注册（无需子类 init 设置拦截器）
 */
object PluginWebViewBridge {

    private val installed = AtomicBoolean(false)

    @JvmStatic
    fun ensureInstalled() {
        if (!installed.compareAndSet(false, true)) {
            return
        }
        ASWebView.globalJavascriptCallIntercepts.add(PluginWebViewInterceptors.createMainCallIntercept())
        ASWebView.globalDbCallIntercepts.add(PluginWebViewInterceptors.createDbCallIntercept())
        ASWebView.globalUrlTransform = { url ->
            PluginWebServerManager.getRunningPlugin()?.getDomain()?.let { domain ->
                if (!url.startsWith("http")) {
                    return@let if (url.startsWith("/")) "$domain$url" else "$domain/$url"
                }
            }
            url
        }
        FloatWindowBridge.webViewProvider = { context -> XWebview(context) }
    }
}
