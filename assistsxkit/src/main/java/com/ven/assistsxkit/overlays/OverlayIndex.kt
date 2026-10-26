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
import com.ven.assistsxkit.plugin.PluginErrorPageInteractor
import com.ven.assistsxkit.plugin.PluginRuntimeManager
import com.ven.assistsxkit.server.PluginWebServerManager

@SuppressLint("StaticFieldLeak")
object OverlayIndex : AssistsServiceListener {

    private var plugin: Plugin? = null
    private var pluginSessionId: String? = null

    /** 错误页 Handler 实例（需为字段以保证 PluginErrorPageInteractor.unbind 引用相等） */
    private var errorPageHandler: PluginErrorPageInteractor.Handler? = null

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


    fun show(plugin: Plugin, sessionId: String? = null): Boolean {
        val id = sessionId ?: when (val reservation = PluginRuntimeManager.reserve(plugin)) {
            is PluginRuntimeManager.Reservation.Acquired -> reservation.session.id
            is PluginRuntimeManager.Reservation.Existing,
            is PluginRuntimeManager.Reservation.Busy -> return false
        }
        if (PluginRuntimeManager.current()?.id != id) return false
        if (!AssistsService.listeners.contains(this)) {
            AssistsService.listeners.add(this)
        }
        this.plugin = plugin
        pluginSessionId = id
        PluginWebServerManager.setRunningPlugin(plugin)
        PluginRuntimeManager.bindStopAction(id) { hide() }
        // 注册错误页交互：错误页 JS 触发「重试/关闭」时由本浮窗处理
        errorPageHandler = object : PluginErrorPageInteractor.Handler {
            override fun onErrorRetry(url: String?) {
                val target = url?.takeIf { it.isNotBlank() }
                    ?: this@OverlayIndex.plugin?.url()
                if (target.isNullOrBlank()) return
                viewBinding?.web?.post { viewBinding?.web?.loadUrl(target) }
            }

            override fun onErrorClose() {
                hide()
            }
        }.also { PluginErrorPageInteractor.bind(it) }
        if (!AssistsWindowManager.contains(assistWindowWrapper?.getView())) {
            AssistsWindowManager.add(assistWindowWrapper)
        }
        if (plugin.isRemote()) {
            viewBinding?.web?.loadUrl(plugin.url())
            PluginRuntimeManager.markRunning(id)
        } else {
            CoroutineWrapper.launch {
                val port = PluginWebServerManager.startServer(plugin, port = plugin.port)
                if (port > 0) {
                    runMain {
                        viewBinding?.web?.loadUrl(plugin.url(port = port))
                        PluginRuntimeManager.markRunning(id)
                    }
                } else {
                    clear()
                    "启动插件失败，请检查插件配置文件".overlayToast()
                }
            }
        }
        return true

    }

    fun hide() {
        clear()
    }

    override fun onUnbind() {
        clear()
    }

    fun clear() {
        val sessionId = pluginSessionId
        if (sessionId != null && PluginRuntimeManager.current()?.id != sessionId) return
        errorPageHandler?.let { PluginErrorPageInteractor.unbind(it) }
        errorPageHandler = null
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
        plugin = null
        pluginSessionId = null
        sessionId?.let { PluginRuntimeManager.clear(it) }
    }


}
