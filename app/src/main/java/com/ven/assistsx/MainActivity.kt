package com.ven.assistsx

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.accessibility.AccessibilityEvent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.toColorInt
import androidx.core.view.isVisible
import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.BarUtils
import com.blankj.utilcode.util.FragmentUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.PermissionUtils
import com.blankj.utilcode.util.PermissionUtils.SimpleCallback
import com.blankj.utilcode.util.ToastUtils
import com.lxj.xpopup.XPopup
import com.ven.assists.AssistsCore
import com.ven.assists.service.AssistsService
import com.ven.assists.service.AssistsServiceListener
import com.ven.assists.utils.CoroutineWrapper
import com.ven.assistsx.databinding.ActivityMainBinding
import com.ven.assistsxkit.AssistsXForegroundService
import com.ven.assistsxkit.MainFragment
import com.ven.assistsx.R
import com.ven.assistsxkit.ScanActivity
import com.ven.assistsxkit.SettingGuideActivity
import kotlinx.coroutines.delay
import com.ven.assistsxkit.ui.PluginPlatformFragment


class MainActivity : AppCompatActivity(), AssistsServiceListener {
    private var isActivityResumed = false
    val viewBind: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater).apply {
            FragmentUtils.add(supportFragmentManager, MainFragment(), R.id.fl_container)
            LogUtils.d("add MainFragment")

        }
    }


    // 注册扫描结果处理器
    private val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.let { intent ->
                if (intent.getStringExtra("action") == "install_plugin") {
                    val pluginJson = intent.getStringExtra("plugin_data")
                    if (pluginJson != null) {
                        ToastUtils.showShort("插件安装成功")
                    }
                }
            }
        }
    }

    private val foregroundServiceIntent: Intent by lazy {
        Intent(this, AssistsXForegroundService::class.java)
    }

    companion object {
        private const val SP_KEY_PLUGINS = "plugins"
    }

    override fun onResume() {
        super.onResume()
        isActivityResumed = true
        checkServiceEnable()
    }

    override fun onPause() {
        super.onPause()
        isActivityResumed = false
    }

    private fun checkServiceEnable() {
        if (!isActivityResumed) return
        if (AssistsCore.isA11yEnabled()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(foregroundServiceIntent)
            }
            FragmentUtils.findFragment(supportFragmentManager, MainFragment::class.java)?.let {
                FragmentUtils.remove(it)
            }
            FragmentUtils.findFragment(supportFragmentManager, PluginPlatformFragment::class.java) ?: let {
                FragmentUtils.add(supportFragmentManager, PluginPlatformFragment(), R.id.fl_container)
            }
        } else {
            stopService(foregroundServiceIntent)
            FragmentUtils.findFragment(supportFragmentManager, PluginPlatformFragment::class.java)?.let {
                FragmentUtils.remove(it)
            }
            if (FragmentUtils.findFragment(supportFragmentManager, MainFragment::class.java) == null) {
                FragmentUtils.add(supportFragmentManager, MainFragment(), R.id.fl_container)
                LogUtils.d("add MainFragment")
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        super.onAccessibilityEvent(event)
    }

    override fun onServiceConnected(service: AssistsService) {
//        onBackApp()
        checkServiceEnable()
        if (AssistsCore.getPackageName() != AppUtils.getAppPackageName()) {
            CoroutineWrapper.launch { AssistsCore.launchApp(AppUtils.getAppPackageName()) }
        }
    }

    private fun onBackApp() {
        CoroutineWrapper.launch {
            while (AssistsCore.getPackageName() != packageName) {
                AssistsCore.back()
                delay(500)
            }
        }
    }

    override fun onUnbind() {
        checkServiceEnable()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(viewBind.root)
        // 设置状态栏样式与AppBar一致
        BarUtils.setStatusBarColor(this, "#23252A".toColorInt(), true)
        BarUtils.setStatusBarLightMode(this, false)
        BarUtils.setNavBarLightMode(this, false)
        BarUtils.setNavBarColor(this, "#23252A".toColorInt())
        AssistsService.listeners.add(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        AssistsService.listeners.remove(this)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            moveTaskToBack(true)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        moveTaskToBack(true)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // 添加加号菜单项
//        menu.add(Menu.NONE, 1, Menu.NONE, "添加")
//            .setIcon(R.drawable.round_add_24)
//            .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
//        menu.add("扫描局域网插件")
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        if (!AssistsCore.isAccessibilityServiceEnabled()) {
            ToastUtils.showShort("请先开启服务")
            return true
        }

        if (item.title == "扫描局域网插件") {
            resultLauncher.launch(Intent(this, ScanActivity::class.java))
            return true
        }

        return when (item.itemId) {
            1 -> {
//                if (!AssistsCore.isAccessibilityServiceEnabled()) {
//                    ToastUtils.showShort("请先开启服务")
//                    return true
//                }
//
//                // 显示插件添加弹窗
//                val dialogView = LayoutInflater.from(this)
//                    .inflate(R.layout.dialog_add_plugin, null)
//                val etName = dialogView.findViewById<EditText>(R.id.etPluginName)
//                val etPath = dialogView.findViewById<EditText>(R.id.etPluginPath)
//
//                AlertDialog.Builder(this)
//                    .setTitle("添加插件")
//                    .setView(dialogView)
//                    .setPositiveButton("确定") { dialog, _ ->
//                        // 创建新的插件对象
//                        val name = etName.text.toString().trim()
//                        val path = etPath.text.toString().trim()
//
//                        if (name.isNotEmpty() && path.isNotEmpty()) {
//                            val plugin = Plugin(name, path)
//                            // 添加到列表
//                            sampleData.add(plugin)
//                            // 保存到本地
//                            savePlugins()
//                            // 更新UI
//                            adapter?.notifyItemInserted(sampleData.size - 1)
//                            // 滚动到新添加的项目
//                            viewBind.recyclerView.smoothScrollToPosition(sampleData.size - 1)
//                        } else {
//                            // 显示错误提示
//                            ToastUtils.showShort("插件名称和路径不能为空")
//                        }
//                    }
//                    .setNegativeButton("取消", null)
//                    .show()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }


}