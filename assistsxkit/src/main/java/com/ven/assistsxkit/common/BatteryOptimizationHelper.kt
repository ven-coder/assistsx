package com.ven.assistsxkit.common

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.ToastUtils
import com.ven.assists.service.AssistsService
import java.util.Locale
import androidx.core.net.toUri

object BatteryOptimizationHelper {

    fun requestIgnoreBatteryOptimization() {
        val context: Context = AssistsService.instance ?: return
        val packageName = context.packageName

        if (isMotorola()) {
            AppUtils.launchAppDetailsSettings()
            return
        }
        if (isOnePlus()) {
            openOnePlusBatteryWhitelistPage(context)
            return
        }
        if (ManufacturerUtils.isXiaomi()) {
            requestBatteryOptimizationWhitelist(context)
            return
        }


        // 根据厂商做特定适配
        try {
            val manufacturer = Build.MANUFACTURER.lowercase(Locale.getDefault())
            val fallbackIntent = when {
                manufacturer.contains("xiaomi") -> Intent().apply {
//                    component = ComponentName("com.miui.powerkeeper", "com.miui.powerkeeper.ui.HiddenAppsContainerManagementActivity")
                    requestBatteryOptimizationWhitelist(context)
                    return
                }

                manufacturer.contains("huawei") -> Intent().apply {
                    component = ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity")
                }

                manufacturer.contains("oppo") -> Intent().apply {
                    component = ComponentName("com.coloros.oppoguardelf", "com.coloros.powermanager.fuelgaue.PowerUsageModelActivity")
                }

                manufacturer.contains("oppo") -> Intent().apply {
                    component = ComponentName("com.coloros.oppoguardelf", "com.coloros.powermanager.fuelgaue.PowerUsageModelActivity")
                }

                manufacturer.contains("vivo") -> Intent().apply {
                    component = ComponentName("com.vivo.abe", "com.vivo.applicationbehaviorengine.ui.ExcessivePowerManagerActivity")
                }

                else -> null
            }

            if (fallbackIntent != null) {
                fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (fallbackIntent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(fallbackIntent)
                    return
                }
            }
        } catch (_: Exception) {
        }


        try {
            // 首先尝试标准方式跳转
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                return
            }
        } catch (_: Exception) {
        }
    }

    /**
     * 请求电池优化白名单
     */
    private fun requestBatteryOptimizationWhitelist(context: Context) {
        try {
            val intent = Intent().apply {
                action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            ToastUtils.showShort("请在系统设置中将应用加入电池优化白名单")
        } catch (e: Exception) {
            // 如果上面的方式不支持，使用通用的电池优化设置页面
            try {
                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                ToastUtils.showShort("请手动将应用加入电池优化白名单")
            } catch (e2: Exception) {
                ToastUtils.showShort("无法打开电池优化设置，请手动在系统设置中关闭")
            }
        }
    }


    fun isMotorola(): Boolean {
        val manufacturer = Build.MANUFACTURER?.lowercase(Locale.getDefault()) ?: ""
        val brand = Build.BRAND?.lowercase(Locale.getDefault()) ?: ""
        return manufacturer.contains("motorola") || brand.contains("motorola") || brand.contains("moto")
    }

    fun openOnePlusBatteryWhitelistPage(context: Context) {
        try {
            val intent = Intent().apply {
                component = ComponentName(
                    "com.oneplus.security",
                    "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"
                )
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                return
            }
        } catch (_: Exception) {
        }

        // ColorOS 兼容入口（适配近年ColorOS内核的一加机型）
        try {
            val intent = Intent().apply {
                component = ComponentName(
                    "com.coloros.safecenter",
                    "com.coloros.safecenter.startupapp.StartupAppListActivity"
                )
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                return
            }
        } catch (_: Exception) {
        }

        // Fallback: 打开应用详情页
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = "package:${context.packageName}".toUri()
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "请手动在系统设置中关闭电池优化", Toast.LENGTH_LONG).show()
        }
    }

    fun isOnePlus(): Boolean {
        val brand = Build.BRAND?.lowercase(Locale.getDefault()) ?: ""
        val manufacturer = Build.MANUFACTURER?.lowercase(Locale.getDefault()) ?: ""
        val device = Build.DEVICE?.lowercase(Locale.getDefault()) ?: ""

        return brand.contains("oneplus") || manufacturer.contains("oneplus") || device.contains("oneplus")
    }

}
