package com.ven.assistsxkit.db

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import com.ven.assistsxkit.db.dao.PluginDao
import com.ven.assistsxkit.db.entity.PluginEntity

/**
 * 应用数据库配置
 */
@Database(
    entities = [PluginEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun pluginDao(): PluginDao

    companion object {
        private const val DATABASE_NAME = "assistsx_database"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * 获取数据库实例
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
//                .addMigrations(MIGRATION_1_2) // 添加迁移配置
                    .build()
                INSTANCE = instance
                instance
            }
        }

        /**
         * 数据库迁移配置
         * 从版本1到版本2的迁移
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    // 添加port字段到plugins表（如果不存在）
                    database.execSQL("ALTER TABLE plugins ADD COLUMN port INTEGER NOT NULL DEFAULT -1")
                } catch (e: Exception) {
                    // 如果字段已存在，忽略错误
                    if (e.message?.contains("duplicate column name") != true) {
                        throw e
                    }
                }

                try {
                    // 添加updateTime字段到plugins表（如果不存在）
                    database.execSQL("ALTER TABLE plugins ADD COLUMN updateTime INTEGER NOT NULL DEFAULT ${System.currentTimeMillis()}")
                } catch (e: Exception) {
                    // 如果字段已存在，忽略错误
                    if (e.message?.contains("duplicate column name") != true) {
                        throw e
                    }
                }
            }
        }

        /**
         * 销毁数据库实例
         */
        fun destroyInstance() {
            INSTANCE = null
        }
    }
}
