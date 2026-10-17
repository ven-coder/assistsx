package com.ven.assistsxkit

import android.content.Context
import android.util.AttributeSet
import com.ven.assists.web.ASWebView
import com.ven.assistsxkit.util.ErrorPageLoader
import com.ven.assistsxkit.web.event.AssistsxWebEventBus

/**
 * AssistsX 插件 WebView：主 frame 加载失败时显示内置错误页，
 * 错误页通过 assistsx Bridge 与 Native 交互（重试 / 关闭）。
 *
 * 同时承担跨 WebView 事件总线的挂载点：插件主页 / 浮窗 / 旧 overlay 三类宿主都使用本类，
 * 因此在这里注入 `assistsxEvent` 即可覆盖全部插件页面。
 */
class XWebview @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : ASWebView(context, attrs) {

    /** 当前是否正显示错误页（防旧/新 API + onReceivedHttpError 重复触发） */
    private var errorPageShowing = false

    /** 最近一次加载失败的主页面 URL（重试目标） */
    private var failedUrl: String? = null

    init {
        // 跨 WebView 事件总线：注入 JS 接口并登记客户端（必须在首次 loadUrl 之前）
        AssistsxWebEventBus.install(this)
        // 加载真实 http(s) 页面时说明已脱离错误页数据流，重置错误标志
        onPageStarting = { url ->
            if (url?.startsWith("http") == true) {
                errorPageShowing = false
            }
        }
    }

    override fun onDetachedFromWindow() {
        AssistsxWebEventBus.detach(this)
        super.onDetachedFromWindow()
    }

    override fun onMainFrameError(failedUrl: String?, errorCode: Int, description: String) {
        if (errorPageShowing) return
        if (failedUrl.isNullOrBlank()) return

        errorPageShowing = true
        this.failedUrl = failedUrl
        // 本地插件走 127.0.0.1 / localhost，其余 http(s) 视为线上插件
        val pluginType = if (isLocalUrl(failedUrl)) "本地插件" else "线上插件"
        ErrorPageLoader.show(this, pluginType, failedUrl, errorCode, description)
    }

    /** 判断是否本地插件地址（本机 HTTP 服务） */
    private fun isLocalUrl(url: String): Boolean {
        return url.startsWith("http://127.0.0.1", ignoreCase = true) ||
            url.startsWith("http://localhost", ignoreCase = true) ||
            url.startsWith("http://10.0.2.2", ignoreCase = true)
    }

    /** 重试加载最近失败的 URL；失败 URL 为空时回退到 reload() */
    fun retryFailedUrl() {
        val target = failedUrl?.takeIf { it.isNotBlank() }
        if (target == null) reload() else loadUrl(target)
    }
}
