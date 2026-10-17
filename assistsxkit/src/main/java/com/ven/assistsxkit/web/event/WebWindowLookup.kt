package com.ven.assistsxkit.web.event

import android.view.View
import android.webkit.WebView
import com.ven.assists.window.AssistsWindowManager

/**
 * 浮窗归属查询：判断 WebView 是否挂在 [AssistsWindowManager] 的浮窗内，并取回窗口 uniqueId。
 *
 * 浮窗内 WebView 的 parent 链最终会经过 `ViewWrapper.view`（即浮窗根 View），
 * 因此沿 parent 链上溯做身份比较即可，无需给 assists-web 增加钩子。
 */
object WebWindowLookup {

    /**
     * 沿 parent 链上溯查找所属浮窗的 uniqueId。
     * @return 浮窗唯一标识；不在任何浮窗内（如插件主页）返回 null
     */
    fun findWindowUniqueId(webView: View): String? {
        val roots = runCatching {
            // viewList 为同步包装的 map，遍历时需按约定手动加锁
            synchronized(AssistsWindowManager.viewList) {
                AssistsWindowManager.viewList.entries.associate { it.value.view to it.key }
            }
        }.getOrNull() ?: return null
        if (roots.isEmpty()) return null

        var current: View? = webView
        while (current != null) {
            roots[current]?.let { return it }
            current = current.parent as? View
        }
        return null
    }

    /** 该 WebView 的角色：浮窗内为 [EventProtocol.roleFloat]，否则为 [EventProtocol.roleIndex] */
    fun resolveRole(webView: WebView): String =
        if (findWindowUniqueId(webView) != null) EventProtocol.roleFloat else EventProtocol.roleIndex
}
