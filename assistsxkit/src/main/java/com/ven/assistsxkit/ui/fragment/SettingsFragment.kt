package com.ven.assistsxkit.ui.fragment

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.ToastUtils
import com.ven.assistsxkit.common.BatteryOptimizationHelper
import com.ven.assistsxkit.databinding.FragmentSettingsBinding
import java.util.Locale
import androidx.core.net.toUri


open class SettingsFragment : Fragment() {

    val binding: FragmentSettingsBinding by lazy { FragmentSettingsBinding.inflate(layoutInflater, null, false) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupVersionInfo()
        setupKeepAliveSettings()
    }

    open fun setupVersionInfo() {
        // 设置版本信息
        binding.tvVersion.text = "版本：${AppUtils.getAppVersionName()}${AppUtils.getAppVersionCode()}"

    }

    open fun setupKeepAliveSettings() {
        // 电池白名单点击
        binding.layoutBatteryWhitelist.setOnClickListener {
            BatteryOptimizationHelper.requestIgnoreBatteryOptimization()
        }

        // 自启动白名单点击
        binding.layoutAutostartWhitelist.setOnClickListener {
            requestAutostartPermission()
        }
    }


    /**
     * 请求自启动权限
     */
    open fun requestAutostartPermission() {

        if (BatteryOptimizationHelper.isMotorola()) {
            AppUtils.launchAppDetailsSettings()
            return
        }

        val intent = Intent()
        val manufacturer = Build.MANUFACTURER.lowercase(Locale.getDefault())
        when (manufacturer) {
            "xiaomi" -> intent.setComponent(
                ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"
                )
            )

            "huawei" -> intent.setComponent(
                ComponentName(
                    "com.huawei.systemmanager",
                    "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
                )
            )

            "oppo" -> intent.setComponent(
                ComponentName(
                    "com.coloros.safecenter",
                    "com.coloros.safecenter.startupapp.StartupAppListActivity"
                )
            )

            "vivo" -> intent.setComponent(
                ComponentName(
                    "com.iqoo.secure",
                    "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager"
                )
            )

            "meizu" -> intent.setComponent(
                ComponentName(
                    "com.meizu.safe",
                    "com.meizu.safe.permission.SmartBGActivity"
                )
            )

            else -> {
                AppUtils.launchAppDetailsSettings()
                return
            }
        }
        try {
            requireActivity().startActivity(intent)
            return
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }

        try {
            // 尝试打开自启动管理页面（不同厂商可能不同）
            val intent = Intent().apply {
                action = "android.settings.APPLICATION_DETAILS_SETTINGS"
                data = "package:${requireContext().packageName}".toUri()
            }
            startActivity(intent)
            ToastUtils.showShort("请在应用信息中开启自启动权限")
        } catch (e: Exception) {
            ToastUtils.showShort("请在系统设置中手动开启自启动权限")
        }
    }


    override fun onResume() {
        super.onResume()
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }
} 