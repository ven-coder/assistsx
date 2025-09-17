package com.ven.assistsxkit.db.dao

import androidx.room.*
import com.ven.assistsxkit.db.entity.PluginEntity
import kotlinx.coroutines.flow.Flow

/**
 * Plugin数据访问对象
 */
@Dao
interface PluginDao {
    
    /**
     * 获取所有插件
     */
    @Query("SELECT * FROM plugins")
    fun getAllPlugins(): Flow<List<PluginEntity>>
    
    /**
     * 根据ID获取插件
     */
    @Query("SELECT * FROM plugins WHERE id = :id")
    suspend fun getPluginById(id: Long): PluginEntity?
    
    /**
     * 根据包名获取插件
     */
    @Query("SELECT * FROM plugins WHERE packageName = :packageName")
    suspend fun getPluginByPackageName(packageName: String): PluginEntity?
    
    /**
     * 插入插件
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlugin(plugin: PluginEntity)
    
    /**
     * 插入多个插件
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlugins(plugins: List<PluginEntity>)
    
    /**
     * 更新插件
     */
    @Update
    suspend fun updatePlugin(plugin: PluginEntity)
    
    /**
     * 删除插件
     */
    @Delete
    suspend fun deletePlugin(plugin: PluginEntity)
    
    /**
     * 根据ID删除插件
     */
    @Query("DELETE FROM plugins WHERE id = :id")
    suspend fun deletePluginById(id: Long)
    
    /**
     * 根据包名删除插件
     */
    @Query("DELETE FROM plugins WHERE packageName = :packageName")
    suspend fun deletePluginByPackageName(packageName: String)
    
    /**
     * 清空所有插件
     */
    @Query("DELETE FROM plugins")
    suspend fun deleteAllPlugins()
    
    /**
     * 获取所有已使用的port值
     */
    @Query("SELECT port FROM plugins WHERE port > 0")
    suspend fun getAllUsedPorts(): List<Int>
}
