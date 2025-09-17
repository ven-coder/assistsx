package com.ven.assistsxkit

import android.app.Application
import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.Utils
import com.ven.assistsxkit.db.AppDatabase
import com.ven.assistsxkit.repository.PluginRepository

class App : Application() {

    companion object {
        // 静态数据库实例
        lateinit var database: AppDatabase
            private set
        
        // 静态Plugin数据仓库实例
        lateinit var pluginRepository: PluginRepository
            private set
    }

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
        // 初始化数据库
        database = AppDatabase.getDatabase(this)
        
        // 初始化PluginRepository
        pluginRepository = PluginRepository(this)
    }
}