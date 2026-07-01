package com.ven.assistsxkit.ui

import android.graphics.Rect
import android.view.View
import android.view.ViewTreeObserver
import android.webkit.WebView
import com.blankj.utilcode.util.SizeUtils

/**
 * 监听软键盘开闭，仅通知 Web 层重试滚动。
 * 布局收缩由 Activity 的 adjustResize 负责，不再向页面注入 --keyboard-inset，避免重复补偿。
 */
class WebViewKeyboardInsetBridge(
    private val anchorView: View,
    private val webView: WebView,
) {
    private var keyboardOpen = false

    private val layoutListener = ViewTreeObserver.OnGlobalLayoutListener {
        val rect = Rect()
        anchorView.getWindowVisibleDisplayFrame(rect)
        val screenHeight = anchorView.rootView.height
        val keyboardHeight = (screenHeight - rect.bottom).coerceAtLeast(0)
        val threshold = SizeUtils.dp2px(100f)
        val open = keyboardHeight > threshold
        if (open == keyboardOpen) {
            return@OnGlobalLayoutListener
        }
        keyboardOpen = open
        val openLiteral = if (open) "true" else "false"
        webView.evaluateJavascript(
            """
            window.dispatchEvent(new CustomEvent('assistsx:keyboard-change', { detail: { open: $openLiteral } }));
            """.trimIndent(),
            null,
        )
    }

    fun attach() {
        anchorView.viewTreeObserver.addOnGlobalLayoutListener(layoutListener)
    }

    fun detach() {
        anchorView.viewTreeObserver.removeOnGlobalLayoutListener(layoutListener)
        if (keyboardOpen) {
            keyboardOpen = false
            webView.evaluateJavascript(
                "window.dispatchEvent(new CustomEvent('assistsx:keyboard-change', { detail: { open: false } }));",
                null,
            )
        }
    }
}
