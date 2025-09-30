package com.ven.assistsxkit

import android.content.Context
import android.util.AttributeSet
import com.blankj.utilcode.util.GsonUtils
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.ven.assists.web.ASWebView
import com.ven.assists.web.CallInterceptResult
import com.ven.assists.web.CallMethod
import com.ven.assists.web.CallRequest
import com.ven.assistsxkit.model.getDomain
import com.ven.assistsxkit.server.PluginWebServerManager

class XWebview @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : ASWebView(context, attrs) {

    init {
        callIntercept = { json ->

            var targetJson = json

            val request = GsonUtils.fromJson<CallRequest<JsonObject>>(json, object : TypeToken<CallRequest<JsonObject>>() {}.type)
            if (request.method == CallMethod.loadWebViewOverlay) {
                val url = request.arguments?.get("url")?.asString ?: ""

                PluginWebServerManager.plugin?.getDomain()?.let {
                    if (!url.startsWith("http")) {
                        val targetUrl = if (url.startsWith("/")) "$it$url" else "$it/$url"
                        request.arguments?.addProperty("url", targetUrl)
                        targetJson = GsonUtils.toJson(request)
                    }
                }

            }


            CallInterceptResult(false, targetJson)
        }
    }

    override fun loadUrl(url: String) {
        PluginWebServerManager.plugin?.getDomain()?.let {
            if (!url.startsWith("http")) {
                val targetUrl = if (url.startsWith("/")) "$it$url" else "$it/$url"
                super.loadUrl(targetUrl)
                return
            }
        }
        super.loadUrl(url)
    }
}