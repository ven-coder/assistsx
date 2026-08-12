package com.ven.assistsxkit.ui

import android.content.Intent
import android.os.Bundle
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
import com.ven.assistsxkit.server.PluginWebServerManager

class IndexActivity : AppCompatActivity() {

    companion object {
        fun open(plugin: Plugin) {
            ActivityUtils.getTopActivity().let {
                it.startActivity(Intent(it, IndexActivity::class.java).putExtra("plugin", plugin))
            }
        }
    }

    val binding: FragmentContainerActivityBinding by lazy { FragmentContainerActivityBinding.inflate(layoutInflater) }

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

        FloatingActionBar(this).apply {
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
    }

    /** 关闭插件页面并清理悬浮窗与本地服务 */
    private fun closePluginAndFinish() {
        finish()
        AssistsWindowManager.removeAllWindow()
        PluginWebServerManager.stopServer()
    }

}