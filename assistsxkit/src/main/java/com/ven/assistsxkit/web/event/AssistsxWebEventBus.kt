package com.ven.assistsxkit.web.event

import android.webkit.WebView
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import java.lang.ref.WeakReference
import java.util.UUID

/**
 * 跨 WebView 事件总线。
 *
 * 同一插件作用域（[scopeProvider] 返回值）内的所有 WebView 客户端可以互相收发事件：
 * - JS 侧经 [EventJsInterface]（`window.assistsxEvent`）调用 [register] / [emit] 等方法；
 * - Native 侧经 [EventJsInterface.push] 把事件以 Base64(CallResponse) 推给目标页面。
 *
 * 可靠性：
 * - 目标页面尚未完成注册（页面加载中）时，消息进入待投递队列，注册时随响应一次性下发；
 * - `retain = true` 的事件会保留最后一条，新客户端注册时在响应中回放（用于跨窗口状态同步）；
 * - 目标已销毁或无法定位时通过 `missed` 回报，调用方可自行重试或打开对应页面。
 *
 * 线程约束：本对象所有公开方法都必须在主线程调用（[EventJsInterface] 已用 Dispatchers.Main 保证）。
 */
object AssistsxWebEventBus {

    /**
     * 运行插件作用域提供者，由宿主在启动时注入（通常返回插件包名）。
     * 返回空时使用 [EventProtocol.defaultScope]。
     */
    @JvmStatic
    var scopeProvider: (() -> String?)? = null

    /** webViewId -> 客户端条目；仅在主线程读写 */
    private val entries = LinkedHashMap<String, Entry>()

    /** scope -> (event -> 最后一条保留消息) */
    private val retainedMap = HashMap<String, LinkedHashMap<String, JsonObject>>()

    /** 事件总线内部异常，由 JS 接口层转换为错误响应 */
    class EventBusException(message: String) : IllegalArgumentException(message)

    private class Queued(val envelope: JsonObject, val ts: Long)

    private class Entry(val id: String, webView: WebView) {
        val viewRef = WeakReference(webView)
        var name: String? = null
        var role: String = EventProtocol.roleIndex
        var uniqueId: String? = null
        var url: String? = null
        var title: String? = null
        var scope: String = EventProtocol.defaultScope

        /** 页面是否已完成 registerClient 握手（未完成时消息进入待投递队列） */
        var ready: Boolean = false
        val pending = ArrayDeque<Queued>()

        fun toJson(isSelf: Boolean): JsonObject = JsonObject().apply {
            addProperty("webViewId", id)
            addProperty("name", name)
            addProperty("role", role)
            addProperty("uniqueId", uniqueId)
            addProperty("url", url)
            addProperty("title", title)
            addProperty("scope", scope)
            addProperty("ready", ready)
            addProperty("isSelf", isSelf)
        }
    }

    private class Resolved {
        val targets = ArrayList<Entry>()
        val missed = ArrayList<String>()
    }

    // ---------------------------------------------------------------- 生命周期

    /**
     * 注入 JS 接口并登记客户端。必须在 WebView 首次 loadUrl 之前调用
     * （[com.ven.assistsxkit.XWebview] 已在 init 中调用）。
     */
    @JvmStatic
    fun install(webView: WebView) {
        runCatching {
            webView.addJavascriptInterface(EventJsInterface(webView), EventProtocol.interfaceName)
        }.onFailure {
            EventLog.e("install failed: ${it.message}")
            return
        }
        attach(webView)
    }

    /** 仅登记客户端（宿主自行注入 JS 接口时使用） */
    @JvmStatic
    fun attach(webView: WebView) {
        ensureEntry(webView)
    }

    /** WebView 销毁时注销；会向同作用域其他客户端广播 `__client_left` */
    @JvmStatic
    fun detach(webView: WebView) {
        removeEntry(webView, notify = true)
    }

    /** JS 主动注销（页面即将卸载等） */
    @JvmStatic
    fun unregister(webView: WebView) {
        removeEntry(webView, notify = true)
    }

    /**
     * 清空指定插件作用域的全部客户端与保留消息（插件关闭时调用，避免切换插件后串消息）。
     * @param scope 为空时按 [scopeProvider] 解析当前作用域
     */
    @JvmStatic
    fun clearScope(scope: String? = null) {
        prune()
        val target = scope?.takeIf { it.isNotBlank() } ?: currentScope()
        entries.values.filter { it.scope == target }.map { it.id }.forEach { entries.remove(it) }
        retainedMap.remove(target)
    }

    /** 清空全部作用域（宿主卸载时调用） */
    @JvmStatic
    fun clearAll() {
        entries.clear()
        retainedMap.clear()
    }

    // ---------------------------------------------------------------- 注册与查询

    /**
     * 注册（或重新注册）当前页面为事件客户端。
     *
     * @param name  页面自定义名称（如 main / settings），用于 `name:xxx` 定向；为空时保留原值
     * @param url   页面地址（用于调试展示），为空时用 WebView 当前地址兜底
     * @param title 页面标题
     */
    @JvmStatic
    fun register(webView: WebView, name: String?, url: String?, title: String?): JsonObject {
        prune()
        val entry = ensureEntry(webView)

        name?.trim()?.takeIf { it.isNotEmpty() }?.let { entry.name = it }
        url?.takeIf { it.isNotBlank() }?.let { entry.url = it }
        if (entry.url.isNullOrBlank()) {
            entry.url = runCatching { webView.url }.getOrNull()
        }
        title?.takeIf { it.isNotBlank() }?.let { entry.title = it }

        val previousRole = entry.role
        val previousUniqueId = entry.uniqueId
        val previousScope = entry.scope
        val previousName = entry.name
        entry.role = WebWindowLookup.resolveRole(webView)
        entry.uniqueId = WebWindowLookup.findWindowUniqueId(webView)
        entry.scope = currentScope()
        val metadataChanged = previousRole != entry.role ||
            previousUniqueId != entry.uniqueId ||
            previousScope != entry.scope ||
            previousName != entry.name

        val wasReady = entry.ready
        entry.ready = true

        // 待投递消息随注册响应一次性下发，避免与推送产生时序竞态
        val pending = drainPending(entry)
        val result = JsonObject().apply {
            addProperty("native", true)
            add("self", entry.toJson(isSelf = true))
            add("clients", JsonArray().apply {
                sameScopeEntries(entry).forEach { add(it.toJson(isSelf = it.id == entry.id)) }
            })
            add("pending", JsonArray().apply { pending.forEach { add(it) } })
            add("retained", JsonArray().apply { retainedOf(entry.scope).values.forEach { add(it) } })
        }

        if (!wasReady || metadataChanged) {
            broadcastClientChange(entry, EventProtocol.eventClientJoined)
        }
        return result
    }

    /** 查询同作用域在线客户端 */
    @JvmStatic
    fun clients(webView: WebView): JsonArray {
        prune()
        val selfId = findEntry(webView)?.id
        val scope = findEntry(webView)?.scope ?: currentScope()
        return JsonArray().apply {
            entries.values.filter { it.scope == scope }.forEach { add(it.toJson(isSelf = it.id == selfId)) }
        }
    }

    /** 查询保留消息；event 为空时返回全部 */
    @JvmStatic
    fun retainedMessages(webView: WebView, event: String?): JsonArray {
        prune()
        val scope = findEntry(webView)?.scope ?: currentScope()
        return JsonArray().apply {
            retainedOf(scope).forEach { (nameKey, envelope) ->
                if (event.isNullOrBlank() || nameKey == event) add(envelope)
            }
        }
    }

    /** 清空保留消息；event 为空时清空全部 */
    @JvmStatic
    fun clearRetained(webView: WebView, event: String?): Boolean {
        val scope = findEntry(webView)?.scope ?: currentScope()
        val store = retainedMap[scope] ?: return true
        if (event.isNullOrBlank()) {
            retainedMap.remove(scope)
        } else {
            store.remove(event)
            if (store.isEmpty()) retainedMap.remove(scope)
        }
        return true
    }

    // ---------------------------------------------------------------- 发送

    /**
     * 发送事件。
     *
     * @param to           字符串选择器：all（默认）/ self / role:xxx / name:xxx / webViewId / 浮窗 uniqueId
     * @param target       对象选择器：{ webViewId?, uniqueId?, name?, role? }（多字段为与关系）
     * @param retain       true 时保留最后一条，供后续注册的客户端回放
     * @param excludeSelf  仅对广播（all）生效，默认排除发送方自身
     */
    @JvmStatic
    fun emit(
        senderView: WebView,
        event: String,
        payload: JsonElement?,
        to: String?,
        target: JsonObject?,
        retain: Boolean,
        excludeSelf: Boolean,
    ): JsonObject {
        prune()
        if (event.isBlank()) throw EventBusException("event 不能为空")
        val sender = findEntry(senderView) ?: throw EventBusException("发送方未注册，请先调用 registerClient")

        val payloadText = if (payload == null || payload is JsonNull) "" else payload.toString()
        if (payloadText.length > EventProtocol.maxPayloadChars) {
            throw EventBusException("payload 过大：${payloadText.length} > ${EventProtocol.maxPayloadChars}")
        }

        val messageId = UUID.randomUUID().toString()
        val live = envelope(
            event = event,
            payload = payload,
            sender = sender,
            messageId = messageId,
            retained = false,
        )

        val broadcast = target == null && (to.isNullOrBlank() || to == EventProtocol.targetAll)
        val resolved = resolveTargets(sender, to, target)
        val targets = if (broadcast && excludeSelf) {
            resolved.targets.filter { it.id != sender.id }
        } else {
            resolved.targets
        }

        val delivered = ArrayList<String>()
        val missed = ArrayList<String>(resolved.missed)
        targets.forEach { entry ->
            if (entry.scope != sender.scope) {
                missed.add(entry.id)
                return@forEach
            }
            if (dispatch(entry, live)) delivered.add(entry.id) else missed.add(entry.id)
        }

        if (retain) {
            storeRetained(sender.scope, event, live)
        }

        return JsonObject().apply {
            addProperty("messageId", messageId)
            add("delivered", JsonArray().apply { delivered.forEach { add(it) } })
            add("missed", JsonArray().apply { missed.forEach { add(it) } })
        }
    }

    // ---------------------------------------------------------------- 内部实现

    private fun currentScope(): String =
        scopeProvider?.invoke()?.takeIf { it.isNotBlank() } ?: EventProtocol.defaultScope

    private fun prune() {
        val dead = entries.values.filter { it.viewRef.get() == null }.map { it.id }
        dead.forEach { entries.remove(it) }
    }

    private fun findEntry(webView: WebView): Entry? =
        entries.values.firstOrNull { it.viewRef.get() === webView }

    private fun ensureEntry(webView: WebView): Entry =
        findEntry(webView) ?: Entry(id = UUID.randomUUID().toString(), webView = webView).apply {
            scope = currentScope()
            role = WebWindowLookup.resolveRole(webView)
            uniqueId = WebWindowLookup.findWindowUniqueId(webView)
            entries[id] = this
        }

    private fun removeEntry(webView: WebView, notify: Boolean) {
        val entry = findEntry(webView) ?: return
        entries.remove(entry.id)
        if (notify && entry.ready) {
            broadcastClientChange(entry, EventProtocol.eventClientLeft)
        }
    }

    private fun sameScopeEntries(entry: Entry): List<Entry> =
        entries.values.filter { it.scope == entry.scope }

    private fun retainedOf(scope: String): Map<String, JsonObject> =
        retainedMap[scope] ?: emptyMap()

    private fun storeRetained(scope: String, event: String, envelope: JsonObject) {
        val stored = envelope.deepCopy()
        stored.addProperty("retained", true)
        retainedMap.getOrPut(scope) { LinkedHashMap() }[event] = stored
    }

    private fun envelope(
        event: String,
        payload: JsonElement?,
        sender: Entry,
        messageId: String,
        retained: Boolean,
    ): JsonObject = JsonObject().apply {
        addProperty("event", event)
        add("payload", payload ?: JsonNull.INSTANCE)
        add("sender", sender.toJson(isSelf = false))
        addProperty("messageId", messageId)
        addProperty("ts", System.currentTimeMillis())
        addProperty("retained", retained)
        addProperty("scope", sender.scope)
    }

    private fun broadcastClientChange(entry: Entry, event: String) {
        val envelope = envelope(
            event = event,
            payload = entry.toJson(isSelf = false),
            sender = entry,
            messageId = UUID.randomUUID().toString(),
            retained = false,
        )
        sameScopeEntries(entry)
            .filter { it.id != entry.id }
            .forEach { dispatch(it, envelope) }
    }

    /**
     * 投递到目标客户端。
     * @return true 表示已投递或已进入待投递队列；false 表示目标不可达（计入 missed）
     */
    private fun dispatch(entry: Entry, envelope: JsonObject): Boolean {
        if (!entry.ready) {
            enqueue(entry, envelope)
            return true
        }
        val view = entry.viewRef.get() ?: return false
        if (!view.isAttachedToWindow) return false
        return EventJsInterface.push(view, envelope)
    }

    private fun enqueue(entry: Entry, envelope: JsonObject) {
        val now = System.currentTimeMillis()
        while (entry.pending.isNotEmpty() && now - entry.pending.first().ts > EventProtocol.pendingTtlMs) {
            entry.pending.removeFirst()
        }
        while (entry.pending.size >= EventProtocol.pendingLimit) {
            entry.pending.removeFirst()
        }
        entry.pending.addLast(Queued(envelope, now))
    }

    private fun drainPending(entry: Entry): List<JsonObject> {
        val now = System.currentTimeMillis()
        val alive = entry.pending.filter { now - it.ts <= EventProtocol.pendingTtlMs }.map { it.envelope }
        entry.pending.clear()
        return alive
    }

    private fun resolveTargets(sender: Entry, to: String?, target: JsonObject?): Resolved {
        val resolved = Resolved()
        val sameScope = sameScopeEntries(sender)

        if (target != null) {
            val id = target.get("webViewId")?.takeIf { it.isJsonPrimitive }?.asString
            val uniqueId = target.get("uniqueId")?.takeIf { it.isJsonPrimitive }?.asString
            val name = target.get("name")?.takeIf { it.isJsonPrimitive }?.asString
            val role = target.get("role")?.takeIf { it.isJsonPrimitive }?.asString
            val matched = sameScope.filter { entry ->
                (id == null || entry.id == id) &&
                    (uniqueId == null || entry.uniqueId == uniqueId) &&
                    (name == null || entry.name == name) &&
                    (role == null || entry.role == role)
            }
            if (matched.isEmpty()) resolved.missed.add(target.toString()) else resolved.targets.addAll(matched)
            return resolved
        }

        val selector = to?.trim().orEmpty()
        when {
            selector.isEmpty() || selector == EventProtocol.targetAll -> resolved.targets.addAll(sameScope)
            selector == EventProtocol.targetSelf -> resolved.targets.add(sender)
            selector.startsWith(EventProtocol.targetPrefixRole) -> {
                val role = selector.removePrefix(EventProtocol.targetPrefixRole)
                val matched = sameScope.filter { it.role == role }
                if (matched.isEmpty()) resolved.missed.add(selector) else resolved.targets.addAll(matched)
            }
            selector.startsWith(EventProtocol.targetPrefixName) -> {
                val name = selector.removePrefix(EventProtocol.targetPrefixName)
                val matched = sameScope.filter { it.name == name }
                if (matched.isEmpty()) resolved.missed.add(selector) else resolved.targets.addAll(matched)
            }
            else -> {
                val matched = sameScope.filter { it.id == selector || (it.uniqueId != null && it.uniqueId == selector) }
                if (matched.isEmpty()) resolved.missed.add(selector) else resolved.targets.addAll(matched)
            }
        }
        return resolved
    }
}
