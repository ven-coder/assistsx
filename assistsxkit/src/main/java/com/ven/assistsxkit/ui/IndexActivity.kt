package com.ven.assistsxkit.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import androidx.core.view.WindowCompat
import androidx.core.view.isInvisible
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.BarUtils
import com.blankj.utilcode.util.FragmentUtils
import com.ven.assists.window.AssistsWindowManager
import com.ven.assistsxkit.R
import com.ven.assistsxkit.databinding.FragmentContainerActivityBinding
import com.ven.assistsxkit.model.Plugin
import com.ven.assistsxkit.plugin.PluginChromeController
import com.ven.assistsxkit.server.PluginWebServerManager

open class IndexActivity : AppCompatActivity() {

    companion object {
        fun open(plugin: Plugin) {
            ActivityUtils.getTopActivity().let {
                it.startActivity(Intent(it, IndexActivity::class.java).putExtra("plugin", plugin))
            }
        }
    }

    val binding: FragmentContainerActivityBinding by lazy { FragmentContainerActivityBinding.inflate(layoutInflater) }

    /** 插件页悬浮操作条，供子类按需配置（如隐藏退出按钮） */
    protected lateinit var floatingActionBar: FloatingActionBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        BarUtils.setStatusBarColor(this, "#23252A".toColorInt(), true)
        BarUtils.setStatusBarLightMode(this, false)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setTitleTextColor(resources.getColor(R.color.colorPrimary, theme))
        binding.toolbar.setSubtitleTextColor(resources.getColor(R.color.colorPrimary, theme))
        binding.toolbar.isInvisible
        FragmentUtils.add(supportFragmentManager, IndexFragment.get(intent.getSerializableExtra("plugin") as Plugin).apply {
            onReceivedTitle = {
                this@IndexActivity.binding.toolbar.title = it
            }
        }, binding.container.id)

        floatingActionBar = FloatingActionBar(this).apply {
            onCloseClick = {
                MaterialAlertDialogBuilder(this@IndexActivity)
                    .setMessage(R.string.plugin_close_confirm_message)
                    .setNegativeButton(R.string.action_cancel, null)
                    .setPositiveButton(R.string.action_confirm) { _, _ ->
                        closePluginAndFinish()
                    }
                    .show()
            }
            onBackClick = {
                (FragmentUtils.findFragment(supportFragmentManager, IndexFragment::class.java) as? IndexFragment)
                    ?.binding?.webView?.goBack()
            }
            onForwardClick = {
                (FragmentUtils.findFragment(supportFragmentManager, IndexFragment::class.java) as? IndexFragment)
                    ?.binding?.webView?.goForward()
            }
            onRefreshClick = {
                (FragmentUtils.findFragment(supportFragmentManager, IndexFragment::class.java) as? IndexFragment)
                    ?.binding?.webView?.reload()
            }
            attachToActivity(this@IndexActivity)
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
//                // 执行你的自定义逻辑
//                AssistsWindowManager.removeAllWindow()
//                PluginWebServerManager.stopServer()
//                finish()
                val fragment = FragmentUtils.findFragment(supportFragmentManager, IndexFragment::class.java)
                if (fragment is IndexFragment) {
                    fragment.binding.webView.apply {
                        if (canGoBack()) {
                            goBack()
                        }
                    }
                }
            }
        })

        PluginChromeController.bind(this)
    }

    override fun onDestroy() {
        PluginChromeController.unbind(this)
        super.onDestroy()
    }

    /** 获取当前 IndexFragment 内的 WebView（供 Chrome 控制器调用） */
    private fun findIndexWebView(): WebView? {
        val fragment = FragmentUtils.findFragment(supportFragmentManager, IndexFragment::class.java)
        return (fragment as? IndexFragment)?.binding?.webView
    }

    /** 设置顶部 ActionBar 标题栏显隐（由 JS Bridge 拦截器派发） */
    internal fun setChromeActionBarVisible(visible: Boolean) {
        binding.toolbar.visibility = if (visible) View.VISIBLE else View.GONE
    }

    /** 设置悬浮操作按钮显隐（由 JS Bridge 拦截器派发） */
    internal fun setChromeFloatingButtonVisible(visible: Boolean) {
        floatingActionBar.visibility = if (visible) View.VISIBLE else View.GONE
    }

    /** 插件 WebView 后退（由 JS Bridge 拦截器派发） */
    internal fun chromeWebViewGoBack() {
        findIndexWebView()?.goBack()
    }

    /** 插件 WebView 前进（由 JS Bridge 拦截器派发） */
    internal fun chromeWebViewGoForward() {
        findIndexWebView()?.goForward()
    }

    /** 插件 WebView 刷新（由 JS Bridge 拦截器派发） */
    internal fun chromeWebViewReload() {
        findIndexWebView()?.reload()
    }

    /** 退出当前插件（由 JS Bridge 拦截器派发，无确认框） */
    internal fun chromeExitPlugin() {
        closePluginAndFinish()
    }

    /** 关闭插件页面并清理悬浮窗与本地服务 */
    internal fun closePluginAndFinish() {
        finish()
        AssistsWindowManager.removeAllWindow()
        PluginWebServerManager.stopServer()
    }

}