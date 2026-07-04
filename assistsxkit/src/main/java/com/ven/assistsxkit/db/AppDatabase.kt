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
    version = 5,
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        /**
         * 数据库迁移：版本 1 → 2，添加 port / updateTime
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                addColumnIfNotExists(
                    database,
                    "ALTER TABLE plugins ADD COLUMN port INTEGER NOT NULL DEFAULT -1"
                )
                addColumnIfNotExists(
                    database,
                    "ALTER TABLE plugins ADD COLUMN updateTime INTEGER NOT NULL DEFAULT ${System.currentTimeMillis()}"
                )
            }
        }

        /**
         * 数据库迁移：版本 2 → 3，补齐有效字段（不含 @Deprecated 字段）
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                addColumnIfNotExists(
                    database,
                    "ALTER TABLE plugins ADD COLUMN pluginUuid TEXT NOT NULL DEFAULT ''"
                )
                addColumnIfNotExists(
                    database,
                    "ALTER TABLE plugins ADD COLUMN passwordEnabled INTEGER NOT NULL DEFAULT 0"
                )
                addColumnIfNotExists(
                    database,
                    "ALTER TABLE plugins ADD COLUMN password TEXT"
                )
            }
        }

        /**
         * 数据库迁移：版本 3 → 4
         * 重建表以移除旧版 v3 误加的 @Deprecated 列（version / isShowOverlay / overlayTitle / main）
         */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS plugins_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        pluginUuid TEXT NOT NULL,
                        name TEXT NOT NULL,
                        versionName TEXT NOT NULL,
                        versionCode INTEGER NOT NULL,
                        description TEXT NOT NULL,
                        needScreenCapture INTEGER NOT NULL,
                        path TEXT NOT NULL,
                        `index` TEXT,
                        indexInOverlay INTEGER,
                        icon TEXT NOT NULL,
                        packageName TEXT NOT NULL,
                        passwordEnabled INTEGER NOT NULL,
                        password TEXT,
                        port INTEGER NOT NULL,
                        updateTime INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    INSERT INTO plugins_new (
                        id, pluginUuid, name, versionName, versionCode, description,
                        needScreenCapture, path, `index`, indexInOverlay, icon, packageName,
                        passwordEnabled, password, port, updateTime
                    )
                    SELECT
                        id, pluginUuid, name, versionName, versionCode, description,
                        needScreenCapture, path, `index`, indexInOverlay, icon, packageName,
                        passwordEnabled, password, port, updateTime
                    FROM plugins
                    """.trimIndent()
                )
                database.execSQL("DROP TABLE plugins")
                database.execSQL("ALTER TABLE plugins_new RENAME TO plugins")
            }
        }

        /**
         * 数据库迁移：版本 4 → 5
         * path 拆分为 localPath / remoteAddress，port 重命名为 localPort
         */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS plugins_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        pluginUuid TEXT NOT NULL,
                        name TEXT NOT NULL,
                        versionName TEXT NOT NULL,
                        versionCode INTEGER NOT NULL,
                        description TEXT NOT NULL,
                        needScreenCapture INTEGER NOT NULL,
                        localPath TEXT NOT NULL,
                        remoteAddress TEXT NOT NULL,
                        `index` TEXT,
                        indexInOverlay INTEGER,
                        icon TEXT NOT NULL,
                        packageName TEXT NOT NULL,
                        passwordEnabled INTEGER NOT NULL,
                        password TEXT,
                        localPort INTEGER NOT NULL,
                        updateTime INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    INSERT INTO plugins_new (
                        id, pluginUuid, name, versionName, versionCode, description,
                        needScreenCapture, localPath, remoteAddress, `index`, indexInOverlay,
                        icon, packageName, passwordEnabled, password, localPort, updateTime
                    )
                    SELECT
                        id, pluginUuid, name, versionName, versionCode, description,
                        needScreenCapture,
                        CASE WHEN LOWER(path) LIKE 'http%' THEN '' ELSE path END,
                        CASE WHEN LOWER(path) LIKE 'http%' THEN path ELSE '' END,
                        `index`, indexInOverlay, icon, packageName,
                        passwordEnabled, password,
                        CASE WHEN LOWER(path) LIKE 'http%' THEN -1 ELSE port END,
                        updateTime
                    FROM plugins
                    """.trimIndent()
                )
                database.execSQL("DROP TABLE plugins")
                database.execSQL("ALTER TABLE plugins_new RENAME TO plugins")
            }
        }

        private fun addColumnIfNotExists(database: SupportSQLiteDatabase, sql: String) {
            try {
                database.execSQL(sql)
            } catch (e: Exception) {
                if (e.message?.contains("duplicate column name", ignoreCase = true) != true) {
                    throw e
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
