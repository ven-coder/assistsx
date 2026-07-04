package com.ven.assistsxkit

import android.app.Application
import com.blankj.utilcode.util.Utils
import com.ven.assistsxkit.db.AppDatabase
import com.ven.assistsxkit.repository.PluginRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class App : Application() {

    companion object {
        // 静态数据库实例
        lateinit var database: AppDatabase
            private set
        
        // 静态Plugin数据仓库实例
        lateinit var pluginRepository: PluginRepository
            private set
    }

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        Utils.init(this)
        
        // 初始化数据库
        initDatabase()
    }
    
    /**
     * 初始化数据库和Repository
     */
    private fun initDatabase() {
        database = AppDatabase.getDatabase(this)
        pluginRepository = PluginRepository(this)
        // 启动时将 SP 历史数据同步到 DB，补齐缺失字段
        applicationScope.launch {
            pluginRepository.syncFromSharedPreferences()
        }
    }
}