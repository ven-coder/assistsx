package com.ven.assistsxkit.plugin

import com.ven.assistsxkit.ui.IndexActivity
import java.lang.ref.WeakReference

/**
 * AssistsX 插件宿主 Chrome 控制器：注册当前活跃的 [IndexActivity]，
 * 供 JS Bridge 拦截器在 binder 线程调用、统一跳主线程派发 UI 操作。
 */
object PluginChromeController {

    @Volatile
    private var hostRef: WeakReference<IndexActivity>? = null

    /** 在 IndexActivity.onCreate 末尾调用，注册当前宿主 */
    @JvmStatic
    fun bind(activity: IndexActivity) {
        hostRef = WeakReference(activity)
    }

    /** 在 IndexActivity.onDestroy 调用，仅当注册者一致才清空，避免子类重建时被误清 */
    @JvmStatic
    fun unbind(activity: IndexActivity) {
        val ref = hostRef ?: return
        if (ref.get() === activity) {
            hostRef = null
        }
    }

    /** 设置顶部 ActionBar 标题栏显隐；返回宿主是否存在且已派发 */
    @JvmStatic
    fun setActionBarVisible(visible: Boolean): Boolean {
        val host = currentHost() ?: return false
        host.runOnUiThread { host.setChromeActionBarVisible(visible) }
        return true
    }

    /** 设置悬浮操作按钮显隐；返回宿主是否存在且已派发 */
    @JvmStatic
    fun setFloatingButtonVisible(visible: Boolean): Boolean {
        val host = currentHost() ?: return false
        host.runOnUiThread { host.setChromeFloatingButtonVisible(visible) }
        return true
    }

    /** 插件 WebView 后退；返回宿主是否存在且已派发 */
    @JvmStatic
    fun webViewGoBack(): Boolean {
        val host = currentHost() ?: return false
        host.runOnUiThread { host.chromeWebViewGoBack() }
        return true
    }

    /** 插件 WebView 前进；返回宿主是否存在且已派发 */
    @JvmStatic
    fun webViewGoForward(): Boolean {
        val host = currentHost() ?: return false
        host.runOnUiThread { host.chromeWebViewGoForward() }
        return true
    }

    /** 插件 WebView 刷新；返回宿主是否存在且已派发 */
    @JvmStatic
    fun webViewReload(): Boolean {
        val host = currentHost() ?: return false
        host.runOnUiThread { host.chromeWebViewReload() }
        return true
    }

    /** 退出当前插件（直接关闭页面，无确认框）；返回宿主是否存在且已派发 */
    @JvmStatic
    fun exitPlugin(): Boolean {
        val host = currentHost() ?: return false
        host.runOnUiThread { host.chromeExitPlugin() }
        return true
    }

    /** 控制插件页顶部状态栏占位 View 显隐；返回宿主是否存在且已派发 */
    @JvmStatic
    fun setStatusBarPlaceholderVisible(visible: Boolean): Boolean {
        val host = currentHost() ?: return false
        host.runOnUiThread { host.setStatusBarPlaceholderVisible(visible) }
        return true
    }

    /** 设置插件页顶部状态栏占位 View 背景色；返回宿主是否存在且已派发 */
    @JvmStatic
    fun setStatusBarPlaceholderColor(color: Int): Boolean {
        val host = currentHost() ?: return false
        host.runOnUiThread { host.setStatusBarPlaceholderColor(color) }
        return true
    }

    private fun currentHost(): IndexActivity? {
        val host = hostRef?.get() ?: return null
        return if (!host.isFinishing && !host.isDestroyed) host else null
    }
}
