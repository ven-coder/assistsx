package com.ven.assistsxkit

import android.app.Application
import com.blankj.utilcode.util.Utils
import com.ven.assistsxkit.common.LegacyPluginMigration
import com.ven.assistsxkit.db.AppDatabase
import com.ven.assistsxkit.plugin.PluginWebViewBridge
import com.ven.assistsxkit.repository.PluginRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

open class App : Application() {

    companion object {
        lateinit var database: AppDatabase
            private set

        lateinit var pluginRepository: PluginRepository
            private set
    }

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        Utils.init(this)
        PluginWebViewBridge.ensureInstalled()
        initDatabase()
    }

    private fun initDatabase() {
        database = AppDatabase.getDatabase(this)
        pluginRepository = PluginRepository(this)
        applicationScope.launch {
            LegacyPluginMigration.migrateIfNeeded(pluginRepository)
        }
    }
}
