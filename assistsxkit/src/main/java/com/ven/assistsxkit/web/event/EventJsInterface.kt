package com.ven.assistsxkit.web.event

import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.blankj.utilcode.util.GsonUtils
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.ven.assists.web.CallRequest
import com.ven.assists.web.CallRequestParser
import com.ven.assists.web.CallResponse
import com.ven.assists.web.createResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.nio.charset.StandardCharsets

/**
 * 跨 WebView 事件总线的 JS 接口（注入对象名 `assistsxEvent`）。
 *
 * 与 [com.ven.assists.web.floating.FloatJsInterface] 同构：
 * - JS 调用 `window.assistsxEvent.call(JSON)`，立即返回空响应；
 * - 处理结果通过 `window.assistsxEventCallback(base64(CallResponse))` 回传，按 callbackId 匹配。
 *
 * 接口实例与单个 WebView 一对一绑定，因此原生侧天然知道消息发送方是谁
 * （这是全局拦截链 `globalJavascriptCallIntercepts` 只拿得到 json 所不具备的能力）。
 */
class EventJsInterface(val webView: WebView) {

    private val scope = CoroutineScope(Dispatchers.Main)

    /** 以 Base64(CallResponse) 回调 JS */
    fun callbackResponse(result: CallResponse<*>) {
        scope.launch {
            runCatching {
                callback(GsonUtils.toJson(result))
            }.onFailure {
                EventLog.e("callbackResponse failed: ${it.message}")
            }
        }
    }

    fun callback(json: String) {
        val encoded = Base64.encodeToString(json.toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP)
        val js = "javascript:try{$CALLBACK_FUNCTION('$encoded')}catch(e){console.error('assistsxEventCallback error',e)}"
        runCatching { webView.evaluateJavascript(js, null) }
            .onFailure { EventLog.e("callback failed: ${it.message}") }
    }

    @JavascriptInterface
    fun call(originJson: String): String {
        // 复杂逻辑统一放主线程执行，避免多线程访问注册表
        scope.launch(Dispatchers.Main) {
            runCatching { handle(originJson) }.onFailure { EventLog.e(it) }
        }
        return GsonUtils.toJson(CallResponse<Any>(code = 0))
    }

    private fun handle(originJson: String) {
        val request = CallRequestParser.parse(originJson) ?: run {
            callbackResponse(CallResponse<Any>(code = -1, message = "请求解析失败", data = null))
            return
        }
        runCatching {
            val response = when (request.method) {
                EventCallMethod.registerClient -> register(request)
                EventCallMethod.unregisterClient -> unregister(request)
                EventCallMethod.emit -> emit(request)
                EventCallMethod.getClients -> getClients(request)
                EventCallMethod.getRetained -> getRetained(request)
                EventCallMethod.clearRetained -> clearRetained(request)
                else -> request.createResponse(-1, message = "方法未支持：${request.method}")
            }
            callbackResponse(response)
        }.onFailure {
            EventLog.e(it)
            callbackResponse(request.createResponse(-1, message = it.message, data = null))
        }
    }

    private fun register(request: CallRequest<JsonObject>): CallResponse<JsonObject> {
        val args = request.arguments
        val name = args?.get("name")?.takeIf { it.isJsonPrimitive }?.asString
        val url = args?.get("url")?.takeIf { it.isJsonPrimitive }?.asString
        val title = args?.get("title")?.takeIf { it.isJsonPrimitive }?.asString
        val data = AssistsxWebEventBus.register(webView, name, url, title)
        return request.createResponse(0, data = data)
    }

    private fun unregister(request: CallRequest<JsonObject>): CallResponse<Boolean> {
        AssistsxWebEventBus.unregister(webView)
        return request.createResponse(0, data = true)
    }

    private fun emit(request: CallRequest<JsonObject>): CallResponse<JsonObject> {
        val args = request.arguments
        val event = args?.get("event")?.takeIf { it.isJsonPrimitive }?.asString
        if (event.isNullOrBlank()) {
            return request.createResponse(-1, message = "event 不能为空", data = null)
        }
        val payload = args.get("payload")?.takeIf { !it.isJsonNull }
        val toElement = args.get("to")?.takeIf { !it.isJsonNull }
        val to = toElement?.takeIf { it.isJsonPrimitive }?.asString
        val target = toElement?.takeIf { it.isJsonObject }?.asJsonObject
        val retain = args.get("retain")?.takeIf { it.isJsonPrimitive }?.asBoolean ?: false
        val excludeSelf = args.get("excludeSelf")?.takeIf { it.isJsonPrimitive }?.asBoolean ?: true

        val data = AssistsxWebEventBus.emit(
            senderView = webView,
            event = event,
            payload = payload,
            to = to,
            target = target,
            retain = retain,
            excludeSelf = excludeSelf,
        )
        return request.createResponse(0, data = data)
    }

    private fun getClients(request: CallRequest<JsonObject>): CallResponse<JsonArray> {
        return request.createResponse(0, data = AssistsxWebEventBus.clients(webView))
    }

    private fun getRetained(request: CallRequest<JsonObject>): CallResponse<JsonArray> {
        val event = request.arguments?.get("event")?.takeIf { it.isJsonPrimitive }?.asString
        return request.createResponse(0, data = AssistsxWebEventBus.retainedMessages(webView, event))
    }

    private fun clearRetained(request: CallRequest<JsonObject>): CallResponse<Boolean> {
        val event = request.arguments?.get("event")?.takeIf { it.isJsonPrimitive }?.asString
        return request.createResponse(0, data = AssistsxWebEventBus.clearRetained(webView, event))
    }

    companion object {
        private const val CALLBACK_FUNCTION = EventProtocol.callbackFunction
        private const val PUSH_FUNCTION = EventProtocol.pushFunction

        /**
         * 向指定页面推送事件信封（Native → JS）。
         * @return true 表示已提交 evaluateJavascript；页面未定义推送函数或已销毁时返回 false
         */
        @JvmStatic
        fun push(webView: WebView, envelope: JsonObject): Boolean {
            return runCatching {
                val json = GsonUtils.toJson(CallResponse(code = 0, data = envelope))
                val encoded = Base64.encodeToString(json.toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP)
                val js = "javascript:try{if(typeof $PUSH_FUNCTION==='function'){$PUSH_FUNCTION('$encoded')}}catch(e){console.error('assistsxEvent push error',e)}"
                webView.evaluateJavascript(js, null)
                true
            }.onFailure {
                EventLog.e("push failed: ${it.message}")
            }.getOrDefault(false)
        }
    }
}
