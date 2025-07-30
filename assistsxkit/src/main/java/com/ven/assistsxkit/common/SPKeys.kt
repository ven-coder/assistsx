package com.ven.assistsxkit.common

/**
 * SharedPreferences 键值常量类
 * 统一管理所有SP存储的key
 */
object SPKeys {
    
    /**
     * 已安装插件列表
     */
    const val INSTALLED_PLUGINS = "installed_plugins"
    
    /**
     * 是否首次安装应用
     */
    const val IS_FIRST_INSTALL = "is_first_install"
    
    /**
     * 应用版本号（用于版本更新检测）
     */
    const val APP_VERSION = "app_version"
    
    /**
     * 用户设置相关
     */
    object Settings {
        const val AUTO_START_SERVICE = "auto_start_service"
        const val ENABLE_OVERLAY = "enable_overlay"
        const val SCREEN_CAPTURE_PERMISSION = "screen_capture_permission"
    }
    
    /**
     * 插件相关设置
     */
    object Plugin {
        const val DEFAULT_PLUGIN_INSTALLED = "default_plugin_installed"
        const val PLUGIN_UPDATE_CHECK = "plugin_update_check"
    }
} 