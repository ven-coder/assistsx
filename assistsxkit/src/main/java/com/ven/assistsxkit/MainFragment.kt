package com.ven.assistsxkit

import android.Manifest
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.toColorInt
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.BarUtils
import com.blankj.utilcode.util.FragmentUtils
import com.blankj.utilcode.util.PermissionUtils
import com.blankj.utilcode.util.PermissionUtils.SimpleCallback
import com.blankj.utilcode.util.ToastUtils
import com.lxj.xpopup.XPopup
import com.ven.assists.AssistsCore
import com.ven.assists.service.AssistsService
import com.ven.assists.service.AssistsServiceListener
import com.ven.assists.utils.CoroutineWrapper
import com.ven.assistsxkit.databinding.FragmentMainBinding
import com.ven.assistsxkit.ui.PluginPlatformFragment
import kotlinx.coroutines.delay

class MainFragment : Fragment() {
    val viewBind: FragmentMainBinding by lazy {
        FragmentMainBinding.inflate(layoutInflater).apply {
            btnEnable.setOnClickListener {
                AssistsCore.openAccessibilitySetting()
                startActivity(Intent(requireActivity(), SettingGuideActivity::class.java))
            }
            tvVersion.setText("版本：${AppUtils.getAppVersionName()}")
        }
    }

    lateinit var notificationsPermissionLauncher: ActivityResultLauncher<Intent?>


    // 注册扫描结果处理器
    private val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.let { intent ->
                if (intent.getStringExtra("action") == "install_plugin") {
                    val pluginJson = intent.getStringExtra("plugin_data")
                    if (pluginJson != null) {
                        ToastUtils.showShort("插件安装成功")
                        // 这里可以添加更多的安装成功后的处理逻辑
                    }
                }
            }
        }
    }

    private val foregroundServiceIntent: Intent by lazy {
        Intent(requireActivity(), AssistsXForegroundService::class.java)
    }

    companion object {
        private const val SP_KEY_PLUGINS = "plugins"
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return viewBind.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        notificationsPermissionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult(), ActivityResultCallback() {

        })

        // 设置状态栏样式与AppBar一致
        BarUtils.setStatusBarColor(requireActivity(), "#23252A".toColorInt(), true)
        BarUtils.setStatusBarLightMode(requireActivity(), false)
        BarUtils.setNavBarLightMode(requireActivity(), false)
        BarUtils.setNavBarColor(requireActivity(), "#23252A".toColorInt())

        checkPermission()

    }

    private fun checkPermission() {
        // 通知权限未开启，提示用户去设置
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PermissionUtils.permission(Manifest.permission.POST_NOTIFICATIONS).callback(object : SimpleCallback {
                override fun onGranted() {

                }

                override fun onDenied() {
                    showNotificationPermissionOpenDialog()
                }
            }).request()
        }
    }

    private fun showNotificationPermissionOpenDialog() {
        XPopup.Builder(requireActivity()).asConfirm("提示", "未开启通知权限，开启通知权限以获得完整测试相关通知提示") {
            val intent = Intent()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Android 8.0及以上版本，跳转到应用的通知设置页面
                intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, requireActivity().packageName)
            } else {
                // Android 8.0以下版本，跳转到应用详情页面
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.setData(Uri.parse("package:" + requireActivity().packageName))
            }
            startActivity(intent)
        }.show()

    }

    override fun onDestroy() {
        super.onDestroy()
    }

}