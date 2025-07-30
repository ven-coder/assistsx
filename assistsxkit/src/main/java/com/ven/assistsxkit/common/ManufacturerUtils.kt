package com.ven.assistsxkit.common

import android.os.Build
import java.util.Locale

/**
 * 用于判断当前设备系统属于哪个厂商的工具类
 * 所有返回的厂商名及常量均使用英文，方便后续逻辑判断；
 * 注释使用中文以符合项目规范。
 */
object ManufacturerUtils {

    /**
     * 设备厂商枚举
     */
    enum class Manufacturer {
        XIAOMI,
        HUAWEI,
        HONOR,
        OPPO,
        VIVO,
        SAMSUNG,
        GOOGLE,
        ONEPLUS,
        MEIZU,
        MOTOROLA,
        NOKIA,
        ASUS,
        REALME,
        LENOVO,
        LG,
        ZTE,
        GIONEE,
        SONY,
        UNKNOWN
    }

    // 使用 lazy 保证只计算一次，避免重复调用带来开销
    private val cachedManufacturer: Manufacturer by lazy { detectManufacturer() }

    /**
     * 获取当前设备厂商枚举
     */
    fun getManufacturer(): Manufacturer = cachedManufacturer

    // 便捷判断函数，按需添加即可
    fun isXiaomi(): Boolean = cachedManufacturer == Manufacturer.XIAOMI
    fun isHuawei(): Boolean = cachedManufacturer == Manufacturer.HUAWEI || cachedManufacturer == Manufacturer.HONOR
    fun isOppo(): Boolean = cachedManufacturer == Manufacturer.OPPO || cachedManufacturer == Manufacturer.REALME
    fun isVivo(): Boolean = cachedManufacturer == Manufacturer.VIVO
    fun isSamsung(): Boolean = cachedManufacturer == Manufacturer.SAMSUNG
    fun isGoogle(): Boolean = cachedManufacturer == Manufacturer.GOOGLE
    fun isOnePlus(): Boolean = cachedManufacturer == Manufacturer.ONEPLUS

    /**
     * 通过 Build.MANUFACTURER 字段和部分品牌特征字段来判断厂商
     */
    private fun detectManufacturer(): Manufacturer {
        val name = (Build.MANUFACTURER ?: Build.BRAND ?: "").lowercase(Locale.getDefault())
        return when {
            name.contains("xiaomi") -> Manufacturer.XIAOMI
            name.contains("huawei") -> Manufacturer.HUAWEI
            name.contains("honor") -> Manufacturer.HONOR
            name.contains("oppo") -> Manufacturer.OPPO
            name.contains("vivo") -> Manufacturer.VIVO
            name.contains("samsung") -> Manufacturer.SAMSUNG
            name.contains("google") -> Manufacturer.GOOGLE
            name.contains("oneplus") -> Manufacturer.ONEPLUS
            name.contains("meizu") -> Manufacturer.MEIZU
            name.contains("motorola") -> Manufacturer.MOTOROLA
            name.contains("nokia") -> Manufacturer.NOKIA
            name.contains("asus") -> Manufacturer.ASUS
            name.contains("realme") -> Manufacturer.REALME
            name.contains("lenovo") -> Manufacturer.LENOVO
            name.contains("lge") || name == "lg" -> Manufacturer.LG
            name.contains("zte") -> Manufacturer.ZTE
            name.contains("gionee") -> Manufacturer.GIONEE
            name.contains("sony") -> Manufacturer.SONY
            else -> Manufacturer.UNKNOWN
        }
    }
} 