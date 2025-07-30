package com.ven.assistsxkit.common

import com.blankj.utilcode.util.SPUtils

/**
 * 首次安装帮助类
 * 管理应用首次安装相关的逻辑
 */
object FirstInstallHelper {
    
    /**
     * 检查是否是首次安装
     * @return true 如果是首次安装，false 如果已经安装过
     */
    fun isFirstInstall(): Boolean {
        return SPUtils.getInstance().getBoolean(SPKeys.IS_FIRST_INSTALL, true)
    }
    
    /**
     * 标记首次安装已完成
     */
    fun markFirstInstallCompleted() {
        SPUtils.getInstance().put(SPKeys.IS_FIRST_INSTALL, false)
    }
    
    /**
     * 重置首次安装状态（用于测试或重置应用）
     */
    fun resetFirstInstallStatus() {
        SPUtils.getInstance().put(SPKeys.IS_FIRST_INSTALL, true)
    }
    
    /**
     * 检查默认插件是否已安装
     */
    fun isDefaultPluginInstalled(): Boolean {
        return SPUtils.getInstance().getBoolean(SPKeys.Plugin.DEFAULT_PLUGIN_INSTALLED, false)
    }
    
    /**
     * 标记默认插件已安装
     */
    fun markDefaultPluginInstalled() {
        SPUtils.getInstance().put(SPKeys.Plugin.DEFAULT_PLUGIN_INSTALLED, true)
    }
    
    /**
     * 获取安装状态信息（用于调试）
     */
    fun getInstallStatusInfo(): String {
        return """
            首次安装状态: ${if (isFirstInstall()) "是" else "否"}
            默认插件已安装: ${if (isDefaultPluginInstalled()) "是" else "否"}
        """.trimIndent()
    }
} 