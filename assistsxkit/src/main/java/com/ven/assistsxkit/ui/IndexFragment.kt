package com.ven.assistsxkit.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.blankj.utilcode.util.ToastUtils
import com.ven.assists.service.AssistsService
import com.ven.assists.service.AssistsServiceListener
import com.ven.assists.utils.CoroutineWrapper
import com.ven.assists.utils.runMain
import com.ven.assistsxkit.databinding.FragmentIndexBinding
import com.ven.assistsxkit.model.Plugin
import com.ven.assistsxkit.model.url
import com.ven.assistsxkit.server.PluginWebServerManager

class IndexFragment : Fragment(), AssistsServiceListener {

    companion object {
        fun get(plugin: Plugin): IndexFragment {
            return IndexFragment().apply {
                arguments = Bundle().apply {
                    putSerializable("plugin", plugin)
                }
            }
        }
    }

    val binding: FragmentIndexBinding by lazy {
        FragmentIndexBinding.inflate(layoutInflater).apply {
            ivWebBack.setOnClickListener { webView.goBack() }
            ivWebForward.setOnClickListener { webView.goForward() }
            ivWebRefresh.setOnClickListener { webView.reload() }
        }
    }

    var plugin: Plugin? = null
    var onReceivedTitle: ((title: String) -> Unit)? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val plugin = arguments?.getSerializable("plugin")
        if (plugin is Plugin) {
            this.plugin = plugin
        } else {
            throw IllegalArgumentException("plugin is invalid")
        }
        AssistsService.listeners.add(this)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.webView.onReceivedTitle = onReceivedTitle
        plugin?.let {
            PluginWebServerManager.plugin = it
            if (it.path.startsWith("http")) {
                binding.webView.loadUrl(plugin?.url() ?: "")
            } else {
                CoroutineWrapper.launch {
                    val port = PluginWebServerManager.startServer(it)
                    if (port > 0) {
                        runMain { binding.webView.loadUrl(it.url(port = port)) }
                    } else {
                        ToastUtils.showShort("启动插件失败，请检查插件配置文件")
                    }
                }
            }
        }
    }

    override fun onUnbind() {
        super.onUnbind()
        requireActivity().finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.webView.destroy()
        binding.webView.removeAllViews()
        binding.webView.clearHistory()
        AssistsService.listeners.remove(this)
        PluginWebServerManager.stopServer()
    }


}