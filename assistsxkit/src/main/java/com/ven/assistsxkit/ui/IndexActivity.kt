package com.ven.assistsxkit.ui

import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.Menu
import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
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
        BarUtils.setStatusBarColor(this, "#23252A".toColorInt(), true)
        BarUtils.setStatusBarLightMode(this, false)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        // 设置返回按钮图标
        binding.toolbar.setNavigationIcon(R.drawable.abc_ic_ab_back_material)
        // 设置返回按钮颜色为colorPrimary
        binding.toolbar.navigationIcon?.setTint(resources.getColor(R.color.colorPrimary, theme))
        // 设置菜单项文字颜色
        binding.toolbar.setTitleTextColor(resources.getColor(R.color.colorPrimary, theme))
        binding.toolbar.setSubtitleTextColor(resources.getColor(R.color.colorPrimary, theme))
        // 设置返回按钮点击事件
        binding.toolbar.setNavigationOnClickListener {
            // 返回
            finish()
            AssistsWindowManager.removeAllWindow()
            PluginWebServerManager.stopServer()
        }
        FragmentUtils.add(supportFragmentManager, IndexFragment.get(intent.getSerializableExtra("plugin") as Plugin).apply {
            onReceivedTitle = {
                this@IndexActivity.binding.toolbar.title = it
            }
        }, binding.container.id)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
//                // 执行你的自定义逻辑
//                AssistsWindowManager.removeAllWindow()
//                PluginWebServerManager.stopServer()
//                finish()
            }
        })
    }

}