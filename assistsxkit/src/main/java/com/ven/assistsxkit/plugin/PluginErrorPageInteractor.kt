package com.ven.assistsxkit.plugin

import java.lang.ref.WeakReference

/**
 * 错误页动作（重试/关闭）交互器。
 *
 * 错误页 JS 通过 assistsx Bridge 触发 [PluginCallMethod.webErrorRetry] /
 * [PluginCallMethod.webErrorClose]，经 [PluginWebViewInterceptors] 分发到这里。
 * 当前活动容器（IndexActivity / OverlayIndex / OverlayWeb）在显示插件时注册
 * [Handler]，实现与具体容器解耦（拦截器运行在 Binder 线程，不直接触碰 UI）。
 */
object PluginErrorPageInteractor {

    interface Handler {
        /** 重新加载失败 URL；url 为空时使用默认行为 */
        fun onErrorRetry(url: String?)

        /** 关闭：应用内退出页面 / 浮窗隐藏并清理 */
        fun onErrorClose()
    }

    @Volatile
    private var handlerRef: WeakReference<Handler>? = null

    /** 容器显示插件时注册（同一时刻仅一个活动容器，后注册覆盖前者） */
    @JvmStatic
    fun bind(handler: Handler) {
        handlerRef = WeakReference(handler)
    }

    /** 容器销毁/隐藏时解绑；仅当注册者一致才清除，避免误清 */
    @JvmStatic
    fun unbind(handler: Handler) {
        val ref = handlerRef ?: return
        if (ref.get() === handler) {
            handlerRef = null
        }
    }

    /** 重试；返回是否已派发到宿主 */
    @JvmStatic
    fun retry(url: String?): Boolean {
        val handler = handlerRef?.get() ?: return false
        handler.onErrorRetry(url)
        return true
    }

    /** 关闭；返回是否已派发到宿主 */
    @JvmStatic
    fun close(): Boolean {
        val handler = handlerRef?.get() ?: return false
        handler.onErrorClose()
        return true
    }
}