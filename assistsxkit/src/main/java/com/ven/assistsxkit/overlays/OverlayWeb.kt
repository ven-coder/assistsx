package com.ven.assistsxkit.overlays

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import androidx.core.view.isVisible
import com.blankj.utilcode.util.ScreenUtils
import com.ven.assists.service.AssistsService
import com.ven.assists.service.AssistsServiceListener
import com.ven.assists.window.AssistsWindowManager
import com.ven.assists.window.AssistsWindowWrapper
import com.ven.assistsxkit.databinding.WebOverlayBinding
import com.ven.assistsxkit.model.Plugin
import com.ven.assistsxkit.model.url
import com.ven.assistsxkit.server.PluginWebServerManager

@SuppressLint("StaticFieldLeak")
object OverlayWeb : AssistsServiceListener {

    private var plugin: Plugin? = null

    private var viewBinding: WebOverlayBinding? = null
        get() {
            if (field == null) {
                field = WebOverlayBinding.inflate(LayoutInflater.from(AssistsService.instance)).apply {
                    web.setBackgroundColor(0)
                    web.onReceivedTitle = {
                        assistWindowWrapper?.viewBinding?.tvTitle?.text = it
                    }
                }
            }
            return field
        }


    var onClose: ((parent: View) -> Unit)? = null

    var showed = false
        private set
        get() {
            assistWindowWrapper?.let {
                return AssistsWindowManager.isVisible(it.getView())
            } ?: return false
        }

    var assistWindowWrapper: AssistsWindowWrapper? = null
        private set
        get() {
            viewBinding?.let {
                if (field == null) {
                    field = AssistsWindowWrapper(it.root, wmLayoutParams = AssistsWindowManager.createLayoutParams().apply {
                        width = (ScreenUtils.getScreenWidth() * 0.8).toInt()
                        height = (ScreenUtils.getScreenHeight() * 0.5).toInt()
                    }, onClose = { hide() }).apply {

                        minWidth = (ScreenUtils.getScreenWidth() * 0.6).toInt()
                        minHeight = (ScreenUtils.getScreenHeight() * 0.4).toInt()
                        initialCenter = true
                        with(viewBinding) {
//                            tvTitle.text = plugin?.overlayTitle ?: ""
                            ivWebBack.isVisible = true
                            ivWebForward.isVisible = true
                            ivWebRefresh.isVisible = true
                            ivWebBack.setOnClickListener {
                                this@OverlayWeb.viewBinding?.web?.goBack()
                            }
                            ivWebForward.setOnClickListener {
                                this@OverlayWeb.viewBinding?.web?.goForward()
                            }
                            ivWebRefresh.setOnClickListener {
                                this@OverlayWeb.viewBinding?.web?.reload()
                            }
                        }
                    }
                }
            }
            return field
        }


    fun show(plugin: Plugin) {
        if (!AssistsService.listeners.contains(this)) {
            AssistsService.listeners.add(this)
        }
        this.plugin = plugin
        if (!AssistsWindowManager.contains(assistWindowWrapper?.getView())) {
            AssistsWindowManager.add(assistWindowWrapper)
        }
        viewBinding?.web?.loadUrl(this.plugin?.url() ?: "")
    }

    fun hide() {
        clear()
    }

    override fun onUnbind() {
        clear()
    }

    fun clear() {
        viewBinding?.web?.stopLoading()
        viewBinding?.web?.clearHistory()
        viewBinding?.web?.removeAllViews()
        viewBinding?.web?.destroy()
        assistWindowWrapper?.viewBinding?.root?.removeAllViews()
        AssistsWindowManager.removeWindow(assistWindowWrapper?.getView())
        viewBinding = null
        assistWindowWrapper = null
        // 停止本地 HTTP 服务，释放端口
        PluginWebServerManager.stopServer()
    }


}