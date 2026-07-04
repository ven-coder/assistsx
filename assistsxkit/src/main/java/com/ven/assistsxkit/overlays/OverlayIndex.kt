package com.ven.assistsxkit.overlays

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import androidx.core.view.isVisible
import com.blankj.utilcode.util.ScreenUtils
import com.blankj.utilcode.util.ToastUtils
import com.ven.assists.AssistsCore
import com.ven.assists.service.AssistsService
import com.ven.assists.service.AssistsServiceListener
import com.ven.assists.utils.CoroutineWrapper
import com.ven.assists.utils.runMain
import com.ven.assists.window.AssistsWindowManager
import com.ven.assists.window.AssistsWindowManager.overlayToast
import com.ven.assists.window.AssistsWindowWrapper
import com.ven.assistsxkit.databinding.WebOverlayBinding
import com.ven.assistsxkit.model.Plugin
import com.ven.assistsxkit.model.isRemote
import com.ven.assistsxkit.model.url
import com.ven.assistsxkit.server.PluginWebServerManager

@SuppressLint("StaticFieldLeak")
object OverlayIndex : AssistsServiceListener {

    private var plugin: Plugin? = null

    private var viewBinding: WebOverlayBinding? = null
        get() {
            if (field == null) {
                field = WebOverlayBinding.inflate(LayoutInflater.from(AssistsService.instance)).apply {
                    web.setBackgroundColor(0)
                    web.onReceivedTitle = {
                        assistWindowWrapper?.viewBinding?.tvTitle?.text = it
                    }
                    root.keepScreenOn = true
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
                                this@OverlayIndex.viewBinding?.web?.goBack()
                            }
                            ivWebForward.setOnClickListener {
                                this@OverlayIndex.viewBinding?.web?.goForward()
                            }
                            ivWebRefresh.setOnClickListener {
                                this@OverlayIndex.viewBinding?.web?.reload()
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
        if (plugin.isRemote()) {
            viewBinding?.web?.loadUrl(plugin.url())
        } else {
            CoroutineWrapper.launch {
                val port = PluginWebServerManager.startServer(plugin, port = plugin.port)
                if (port > 0) {
                    runMain { viewBinding?.web?.loadUrl(plugin.url(port = port)) }
                } else {
                    "启动插件失败，请检查插件配置文件".overlayToast()
                }
            }
        }


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
        AssistsWindowManager.removeView(assistWindowWrapper?.getView())
        viewBinding = null
        assistWindowWrapper = null
        // 停止本地 HTTP 服务，释放端口
        PluginWebServerManager.stopServer()
        AssistsCore.clearKeepScreenOn()
    }


}