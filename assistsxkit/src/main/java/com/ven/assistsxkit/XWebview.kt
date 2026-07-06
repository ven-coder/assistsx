package com.ven.assistsxkit

import android.content.Context
import android.util.AttributeSet
import com.ven.assists.web.ASWebView
import com.ven.assistsxkit.plugin.PluginWebViewBridge

class XWebview @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : ASWebView(context, attrs) {

    init {
        PluginWebViewBridge.ensureInstalled()
    }
}
