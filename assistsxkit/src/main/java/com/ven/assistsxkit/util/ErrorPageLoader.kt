package com.ven.assistsxkit.util

import android.content.Context
import android.webkit.WebView
import com.ven.assistsxkit.R
import java.nio.charset.StandardCharsets

/**
 * 插件 WebView 内置错误页加载器：读取 raw 模板、HTML/JSON 转义插值并加载。
 */
object ErrorPageLoader {

    private const val PLACEHOLDER_TYPE = "{{PLUGIN_TYPE}}"
    private const val PLACEHOLDER_CODE = "{{CODE}}"
    private const val PLACEHOLDER_DESC = "{{DESC}}"
    private const val PLACEHOLDER_RETRY_URL_JSON = "{{RETRY_URL_JSON}}"

    /**
     * 读取 raw 模板（UTF-8）。失败时返回空字符串，调用方据此放弃显示错误页。
     */
    fun loadTemplate(context: Context): String {
        return runCatching {
            context.resources.openRawResource(R.raw.plugin_error_page).use { input ->
                input.readBytes().toString(StandardCharsets.UTF_8)
            }
        }.getOrDefault("")
    }

    /**
     * 将 raw 模板插值并加载到 [webView]。
     *
     * @param pluginType 插件类型展示文案（如「本地插件」/「线上插件」），HTML 转义后展示，不泄露 URL
     * @param failedUrl  重试目标 URL，仅作 JSON 转义后注入重试，不展示
     */
    fun show(webView: WebView, pluginType: String, failedUrl: String, errorCode: Int, description: String) {
        val template = loadTemplate(webView.context)
        if (template.isBlank()) return

        val safeType = escapeHtml(pluginType)
        val safeDesc = escapeHtml(description)
        val retryUrlJson = toJsonStringLiteral(failedUrl)

        val html = template
            .replace(PLACEHOLDER_TYPE, safeType)
            .replace(PLACEHOLDER_CODE, errorCode.toString())
            .replace(PLACEHOLDER_DESC, safeDesc)
            .replace(PLACEHOLDER_RETRY_URL_JSON, retryUrlJson)

        // baseUrl 为 null：错误页与插件站点隔离，不访问本地服务/域名
        webView.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
    }

    /** 简单 HTML 转义，防止 URL / 描述内的特殊字符破坏页面 */
    private fun escapeHtml(input: String): String = buildString(input.length + 16) {
        input.forEach { c ->
            when (c) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&#39;")
                else -> append(c)
            }
        }
    }

    /** 生成可安全嵌入 <script> 的 JSON 字符串字面量（含 JS 特殊转义） */
    private fun toJsonStringLiteral(input: String): String {
        val escaped = buildString(input.length + 16) {
            input.forEach { c ->
                when (c) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    '<' -> append("\\u003c") // 防 </script> 闭合注入
                    '>' -> append("\\u003e")
                    '&' -> append("\\u0026")
                    else -> append(c)
                }
            }
        }
        return "\"$escaped\""
    }
}