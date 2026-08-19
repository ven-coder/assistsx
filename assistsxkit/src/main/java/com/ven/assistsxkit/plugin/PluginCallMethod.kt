package com.ven.assistsxkit.plugin

/**
 * AssistsX 插件宿主扩展的主接口方法名
 */
object PluginCallMethod {
    const val getCurrentPlugin = "getCurrentPlugin"

    /** 控制宿主 IndexActivity 顶部 ActionBar 标题栏显隐 */
    const val setActionBarVisible = "setActionBarVisible"

    /** 控制宿主 IndexActivity 悬浮操作按钮显隐 */
    const val setFloatingButtonVisible = "setFloatingButtonVisible"

    /** 插件 WebView 后退 */
    const val webViewGoBack = "webViewGoBack"

    /** 插件 WebView 前进 */
    const val webViewGoForward = "webViewGoForward"

    /** 插件 WebView 刷新 */
    const val webViewReload = "webViewReload"

    /** 退出当前插件（关闭页面并清理悬浮窗与本地服务） */
    const val exitPlugin = "exitPlugin"
}
