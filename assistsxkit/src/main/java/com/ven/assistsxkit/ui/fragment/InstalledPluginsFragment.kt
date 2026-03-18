package com.ven.assistsxkit.ui.fragment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.ven.assistsxkit.ui.adapter.InstalledPluginAdapter
import com.google.android.material.bottomsheet.BottomSheetDialog
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.text.InputType
import android.widget.Toast
import java.io.File
import java.util.zip.ZipInputStream
import androidx.appcompat.app.AlertDialog
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.KeyboardUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import com.blankj.utilcode.util.SPUtils
import com.blankj.utilcode.util.ToastUtils
import com.google.android.material.textfield.TextInputEditText
import com.ven.assistsxkit.model.Plugin
import com.ven.assistsxkit.overlays.OverlayWeb
import com.ven.assists.utils.CoroutineWrapper
import com.ven.assists.utils.runMain
import com.ven.assistsxkit.ScanActivity
import com.ven.assistsxkit.databinding.FragmentInstalledPluginsBinding
import java.io.FileReader
import org.json.JSONObject
import com.ven.assists.mp.MPManager
import com.ven.assistsxkit.common.SPKeys
import com.ven.assistsxkit.common.FirstInstallHelper
import com.lzy.okgo.OkGo
import com.ven.assistsxkit.App
import com.ven.assistsxkit.R
import com.ven.assistsxkit.overlays.OverlayIndex
import com.ven.assistsxkit.server.PluginWebServerManager
import com.ven.assistsxkit.ui.IndexActivity
import com.ven.assistsxkit.ui.PluginPlatformFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay

open class InstalledPluginsFragment : Fragment() {

    private val _binding: FragmentInstalledPluginsBinding by lazy { FragmentInstalledPluginsBinding.inflate(layoutInflater, null, false) }
    protected val binding get() = _binding
    protected lateinit var pluginAdapter: InstalledPluginAdapter

    // 添加临时目录和插件目录的引用
    protected val tempDir: File by lazy { File(context?.cacheDir, "temp_plugins").apply { mkdirs() } }
    protected val pluginsDir: File by lazy { File(context?.filesDir, "plugins").apply { mkdirs() } }

    protected var defaultAssetsPlugin = "assistsx-simple"
    var pluginList = listOf<Plugin>()

    // 注册文件选择结果处理器
    protected val pickPluginFile = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            handleSelectedPluginFile(it)
        }
    }

    // 注册扫描结果处理器
    private val scanLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let { intent ->
                if (intent.getStringExtra("action") == "install_plugin") {
                    val pluginJson = intent.getStringExtra("plugin_data")
                    if (pluginJson != null) {
                        // 在协程中处理插件安装
                        lifecycleScope.launch(Dispatchers.IO) {
                            try {
                                val plugin = GsonUtils.fromJson(pluginJson, Plugin::class.java)
                                // 保存插件信息
                                savePlugin(plugin)
                                // 在主线程更新UI
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "插件安装成功", Toast.LENGTH_SHORT).show()
                                    // 刷新插件列表
                                    loadInstalledPlugins()
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "插件安装失败：${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
//        binding.toolbar.setBackgroundColor("#1E1E1E".toColorInt())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSearchView()
        setupFab()
        setupEmptyView()
        setupSwipeRefresh()
        checkFirstInstall()
    }

    private fun setupRecyclerView() {
        pluginAdapter = InstalledPluginAdapter(
            onItemClick = { plugin ->
                // 显示插件详情对话框
                showPluginDetailDialog(plugin)
            },
            onActionClick = { plugin ->
                // 处理插件操作按钮点击
                handlePluginAction(plugin)
            },
            onItemLongClick = { plugin ->
                // 显示删除确认对话框
                showDeleteConfirmDialog(plugin)
            }
        )

        binding.recyclerPlugins.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = pluginAdapter
        }

        // 加载已安装插件列表
        loadInstalledPlugins()
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return true
            }
        })
    }

    private fun setupFab() {
        binding.fabAdd.setOnClickListener {
            // 显示底部弹出菜单
            showInstallOptionsBottomSheet()
        }
    }

    private fun setupEmptyView() {
        // 设置"立即安装"按钮点击事件
        binding.btnInstall.setOnClickListener {
            showInstallOptionsBottomSheet()
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.apply {
            // 设置刷新颜色
            setColorSchemeResources(
                R.color.colorPrimary,
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light
            )

            // 设置刷新监听器
            setOnRefreshListener {
                // 执行刷新操作
                refreshPluginList()
            }
        }
    }

    open fun showInstallOptionsBottomSheet(): BottomSheetDialog {
        val bottomSheet = BottomSheetDialog(requireContext())
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_install_options, null)

        // 设置各个选项的点击事件
        view.findViewById<View>(R.id.option_local_install).setOnClickListener {
            // 处理本地安装
            handleLocalInstall()
            bottomSheet.dismiss()
        }


        view.findViewById<View>(R.id.option_lan_scan).setOnClickListener {
            // 处理局域网扫描
            handleLanScan()
            bottomSheet.dismiss()
        }

        view.findViewById<View>(R.id.option_add_online).setOnClickListener {
            // 处理添加在线插件
            handleAddOnlinePlugin()
            bottomSheet.dismiss()
        }

        bottomSheet.setContentView(view)
        bottomSheet.show()
        return bottomSheet
    }

    open fun handleLocalInstall() {
        // 启动文件选择器，支持选择 zip 文件
        pickPluginFile.launch("application/zip")
    }

    open fun handleSelectedPluginFile(uri: Uri) {
        // 获取文件名
        val fileName = context?.contentResolver?.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            cursor.moveToFirst()
            val name = cursor.getString(nameIndex)
            val size = cursor.getLong(sizeIndex)
            Pair(name, size)
        } ?: Pair("未知文件", 0L)

        // 检查文件扩展名
        if (!fileName.first.endsWith(".zip")) {
            Toast.makeText(context, "请选择有效的插件压缩包（.zip）", Toast.LENGTH_SHORT).show()
            return
        }

        // 检查文件大小（限制为50MB）
        if (fileName.second > 50 * 1024 * 1024) {
            Toast.makeText(context, "插件文件过大，请选择小于50MB的文件", Toast.LENGTH_SHORT).show()
            return
        }



        // 在后台线程处理文件
        CoroutineWrapper.launch(false) {
            val dialog = runMain {
                // 显示进度对话框
                val progressDialog = MaterialAlertDialogBuilder(requireContext())
                    .setTitle("正在安装插件")
                    .setMessage("正在处理：${fileName.first}")
                    .setCancelable(false)
                    .setView(R.layout.dialog_progress)
                    .create()

                progressDialog.show()
                return@runMain progressDialog
            }
            try {
                installPlugin(uri, fileName.first, dialog)
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    dialog.dismiss()
                    ToastUtils.showShort("插件安装失败：${e.message}")
                }
            }
        }
    }

    open suspend fun installPlugin(uri: Uri, fileName: String, progressDialog: AlertDialog) {
// 将tempFile声明移到try块外部
        val tempFile = File(tempDir, "temp_plugin.zip")
        val tempUnzipDir = File(tempDir, "temp_unzip")

        try {
            // 使用类属性的tempDir和pluginsDir
            tempDir.mkdirs()
            pluginsDir.mkdirs()

            // 清理可能存在的旧文件
            if (tempFile.exists()) {
                tempFile.delete()
            }

            // 更新进度提示
            updateProgressDialog(progressDialog, "正在复制文件...")

            // 2. 复制文件到临时目录
            context?.contentResolver?.openInputStream(uri)?.use { input ->
                tempFile.outputStream().buffered().use { output ->
                    input.copyTo(output)
                }
            }

            // 3. 先解压到临时目录读取配置文件
            tempUnzipDir.apply {
                if (exists()) deleteRecursively()
                mkdirs()
            }

            updateProgressDialog(progressDialog, "正在读取插件信息...")

            // 临时解压以读取配置文件
            unzipFile(tempFile, tempUnzipDir, progressDialog, isTemp = true)

            // 查找并读取配置文件，失败则使用 fileName 作为 packageName
            val configFile = findConfigFile(tempUnzipDir)
            var packageName: String
            var useDefaultPlugin: Boolean
            if (configFile != null) {
                try {
                    packageName = readPackageNameFromConfig(configFile)
                    useDefaultPlugin = false
                } catch (e: Exception) {
                    packageName = sanitizeFileNameToPackageName(fileName)
                    useDefaultPlugin = true
                }
            } else {
                packageName = sanitizeFileNameToPackageName(fileName)
                useDefaultPlugin = true
            }

            // 4. 创建插件专属目录
            val pluginDir = File(pluginsDir, packageName).apply {
                if (exists()) {
                    // 如果目录已存在，先删除旧版本
                    deleteRecursively()
                }
                mkdirs()
            }

            // 5. 解压插件到最终目录
            updateProgressDialog(progressDialog, "正在安装插件...")
            unzipFile(tempFile, pluginDir, progressDialog, isTemp = false)

            // 6. 清理临时文件
            tempFile.delete()
            tempUnzipDir.deleteRecursively()

            // 7. 创建插件对象并保存
            val displayName = fileName.removeSuffix(".zip").removeSuffix(".ZIP")
            val plugin = if (useDefaultPlugin) {
                val index = findIndexInPluginDir(pluginDir)
                Plugin(
                    id = UUID.randomUUID().toString(),
                    name = displayName,
                    version = "1.0.0",
                    versionName = "1.0.0",
                    description = displayName,
                    path = pluginDir.absolutePath,
                    index = index,
                    indexInOverlay = true,
                    packageName = packageName
                )
            } else {
                val parsed = parsePluginFromConfig(findConfigFile(pluginDir)!!, fileName, packageName)
                // 与 handleOnlinePluginInstall 一致：字段无效时赋默认值；index 无效则扫描目录
                val indexValue = parsed.index.takeIf { !it.isNullOrBlank() } ?: findIndexInPluginDir(pluginDir)
                parsed.copy(
                    name = parsed.name.takeIf { it.isNotBlank() } ?: displayName,
                    description = parsed.description.takeIf { it.isNotBlank() } ?: displayName,
                    packageName = parsed.packageName.takeIf { it.isNotBlank() } ?: packageName,
                    version = parsed.version.takeIf { it.isNotBlank() } ?: "1.0.0",
                    versionName = parsed.versionName.takeIf { it.isNotBlank() } ?: "1.0.0",
                    index = indexValue
                )
            }
            savePlugin(plugin)

            // 在主线程更新UI
            withContext(Dispatchers.Main) {
                progressDialog.dismiss()
                Toast.makeText(context, "插件安装成功", Toast.LENGTH_SHORT).show()
                // 刷新插件列表
                loadInstalledPlugins()
            }

        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                progressDialog.dismiss()
                Toast.makeText(context, "插件安装失败：${e.message}", Toast.LENGTH_SHORT).show()
            }
            // 清理失败的安装文件和临时文件
            if (tempUnzipDir.exists()) {
                tempUnzipDir.deleteRecursively()
            }
            if (tempFile.exists()) {
                tempFile.delete()
            }
        }
    }

    open fun readPackageNameFromConfig(configFile: File): String {
        return try {
            val jsonString = FileReader(configFile).use { it.readText() }
            val jsonObject = JSONObject(jsonString)
            jsonObject.getString("packageName").takeIf { it.isNotBlank() }
                ?: throw Exception("packageName不能为空")
        } catch (e: Exception) {
            throw Exception("读取packageName失败：${e.message}")
        }
    }

    /** 从文件名生成有效的 packageName（用于目录名） */
    open fun sanitizeFileNameToPackageName(fileName: String): String {
        return fileName.removeSuffix(".zip").removeSuffix(".ZIP")
            .replace(Regex("[^a-zA-Z0-9._-]"), "_")
            .ifEmpty { "plugin" }
    }

    /**
     * 扫描插件目录获取 index 值：优先 index.html，否则第一个 html 文件，都没有则抛出异常
     */
    open fun findIndexInPluginDir(pluginDir: File): String {
        val indexHtmlPaths = mutableListOf<String>()
        val allHtmlPaths = mutableListOf<String>()

        fun scan(dir: File, relativePath: String) {
            dir.listFiles()?.forEach { file ->
                val path = if (relativePath.isEmpty()) file.name else "$relativePath/${file.name}"
                when {
                    file.isDirectory -> scan(file, path)
                    file.isFile && file.name.endsWith(".html", ignoreCase = true) -> {
                        allHtmlPaths.add(path)
                        if (file.name.equals("index.html", ignoreCase = true)) {
                            indexHtmlPaths.add(path)
                        }
                    }
                }
            }
        }
        scan(pluginDir, "")
        return indexHtmlPaths.firstOrNull() ?: allHtmlPaths.firstOrNull()
        ?: throw Exception("插件添加失败")
    }

    open suspend fun unzipFile(zipFile: File, targetDirectory: File, progressDialog: AlertDialog, isTemp: Boolean) {
        ZipInputStream(zipFile.inputStream().buffered()).use { zipInputStream ->
            var zipEntry = zipInputStream.nextEntry
            var extractedFiles = 0

            while (zipEntry != null) {
                val newFile = File(targetDirectory, zipEntry.name)

                // 安全检查：确保解压的文件路径在目标目录内
                if (!newFile.canonicalPath.startsWith(targetDirectory.canonicalPath)) {
                    throw SecurityException("发现潜在的安全风险：检测到路径穿越攻击")
                }

                if (zipEntry.isDirectory) {
                    newFile.mkdirs()
                } else {
                    newFile.parentFile?.mkdirs()
                    newFile.outputStream().buffered().use { output ->
                        zipInputStream.copyTo(output)
                    }
                }

                extractedFiles++
                // 更新解压进度
                if (!isTemp) {
                    updateProgressDialog(progressDialog, "正在解压文件: $extractedFiles 个文件已处理")
                }

                zipEntry = zipInputStream.nextEntry
            }
        }
    }

    open suspend fun updateProgressDialog(dialog: AlertDialog, message: String) {
        withContext(Dispatchers.Main) {
            dialog.findViewById<TextView>(R.id.progress_message)?.text = message
        }
    }

    open fun formatFileSize(size: Long): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            else -> "${size / (1024 * 1024)} MB"
        }
    }

    open fun handleLanScan() {
        // 直接使用已注册的scanLauncher
        scanLauncher.launch(Intent(context, ScanActivity::class.java))
    }

    open fun handleAddOnlinePlugin() {
        // 显示URL输入弹窗
        showUrlInputDialog()
    }

    open fun showUrlInputDialog() {
        // 创建输入框布局
        val inputLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 0)
        }


        // 创建输入框
        val editText = TextInputEditText(requireContext()).apply {
            inputType = InputType.TYPE_TEXT_VARIATION_URI
            hint = "输入有效的URL"
        }

        inputLayout.addView(editText)

        // 创建对话框
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("添加在线插件")
            .setView(inputLayout)
            .setPositiveButton("确认") { _, _ ->
                val url = editText.text.toString().trim()
                if (url.isNotEmpty()) {
                    KeyboardUtils.hideSoftInput(requireActivity())
                    // 处理在线插件添加
                    handleOnlinePluginInstall(url)
                } else {
                    Toast.makeText(context, "请输入有效的URL", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    open fun handleOnlinePluginInstall(url: String) {
        // 显示进度对话框
        val progressDialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("正在添加在线插件")
            .setMessage("正在获取插件信息...")
            .setCancelable(false)
            .setView(R.layout.dialog_progress)
            .create()

        progressDialog.show()

        // 在后台线程处理网络请求
        lifecycleScope.launch(Dispatchers.IO) {
            val baseUrl = if (url.endsWith("/")) url.dropLast(1) else url
            val configUrls = listOf(
                "$baseUrl/config.json",
                "$baseUrl/assistsx_plugin_config.json"
            )

            var plugin: Plugin? = null
            for (configUrl in configUrls) {
                try {
                    val response = com.lzy.okgo.OkGo.get<String>(configUrl).execute()
                    if (response.isSuccessful) {
                        val jsonString = response.body?.string()
                        if (!jsonString.isNullOrBlank()) {
                            val parsed = GsonUtils.fromJson(jsonString, Plugin::class.java)
                            // 生成有效的 packageName（用于目录名）
                            val sanitizedPackageName = baseUrl
                                .replace("https://", "")
                                .replace("http://", "")
                                .replace("/", ".")
                                .replace(":", "_")
                                .replace(Regex("[^a-zA-Z0-9._-]"), "_")
                                .ifEmpty { "online_plugin" }
                            plugin = parsed.copy(
                                id = UUID.randomUUID().toString(),
                                path = baseUrl,
                                name = parsed.name.takeIf { it.isNotBlank() } ?: baseUrl,
                                description = parsed.description.takeIf { it.isNotBlank() } ?: baseUrl,
                                packageName = parsed.packageName.takeIf { it.isNotBlank() } ?: sanitizedPackageName,
                                version = parsed.version.takeIf { it.isNotBlank() } ?: "1.0.0",
                                versionName = parsed.versionName.takeIf { it.isNotBlank() } ?: "1.0.0"
                            )
                            break
                        }
                    }
                } catch (e: Exception) {
                    // 当前 URL 失败，尝试下一个
                    continue
                }
            }

            if (plugin != null) {
                try {
                    savePlugin(plugin)
                    withContext(Dispatchers.Main) {
                        progressDialog.dismiss()
                        Toast.makeText(context, "在线插件添加成功", Toast.LENGTH_SHORT).show()
                        loadInstalledPlugins()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        progressDialog.dismiss()
                        Toast.makeText(context, "保存插件失败：${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                // 两个配置文件都请求失败，创建默认插件并保存
                try {
                    val sanitizedPackageName = baseUrl
                        .replace("https://", "")
                        .replace("http://", "")
                        .replace("/", ".")
                        .replace(":", "_")
                        .replace(Regex("[^a-zA-Z0-9._-]"), "_")
                        .ifEmpty { "online_plugin" }

                    val defaultPlugin = Plugin(
                        id = UUID.randomUUID().toString(),
                        name = baseUrl,
                        version = "1.0.0",
                        versionName = "1.0.0",
                        description = baseUrl,
                        path = baseUrl,
                        indexInOverlay = true,
                        packageName = sanitizedPackageName
                    )

                    savePlugin(defaultPlugin)

                    withContext(Dispatchers.Main) {
                        progressDialog.dismiss()
                        Toast.makeText(context, "配置获取失败，已使用默认配置添加在线插件", Toast.LENGTH_LONG).show()
                        loadInstalledPlugins()
                    }
                } catch (saveException: Exception) {
                    withContext(Dispatchers.Main) {
                        progressDialog.dismiss()
                        Toast.makeText(context, "保存默认插件失败：${saveException.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    open fun loadInstalledPlugins() {
        try {
            val pluginsJson = SPUtils.getInstance().getString(SPKeys.INSTALLED_PLUGINS, "[]")
            val pluginsArray = GsonUtils.fromJson<List<Plugin>>(pluginsJson, GsonUtils.getListType(Plugin::class.java))
            pluginList = pluginsArray.reversed()
            pluginAdapter.submitList(pluginList)
            // 更新空状态显示
            updateEmptyState(pluginsArray)
        } catch (e: Exception) {
            Toast.makeText(context, "加载插件列表失败：${e.message}", Toast.LENGTH_SHORT).show()
            // 发生错误时也要更新空状态显示
            updateEmptyState(emptyList())
        } finally {
            // 停止刷新动画
            binding.swipeRefreshLayout.isRefreshing = false
        }
    }

    open fun refreshPluginList() {
        // 重新加载插件列表
        loadInstalledPlugins()
    }

    open fun updateEmptyState(plugins: List<Plugin>) {
        binding.layoutEmpty.visibility = if (plugins.isEmpty()) View.VISIBLE else View.GONE
        binding.swipeRefreshLayout.visibility = if (plugins.isNotEmpty()) View.VISIBLE else View.GONE
    }

    override fun onResume() {
        super.onResume()
        refreshPluginList()

    }

    open fun handlePluginAction(plugin: Plugin) {
        CoroutineWrapper.launch {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R && plugin.needScreenCapture && !MPManager.isEnable) {
                val result = MPManager.request(autoAllow = true)
                if (!result) {
                    Toast.makeText(requireContext(), "插件需要屏幕录制权限，权限被拒绝，插件启动失败", Toast.LENGTH_SHORT).show()
                    return@launch
                }
            }
            if (plugin.indexInOverlay == true || plugin.isShowOverlay) {
                runMain { OverlayIndex.show(plugin) }
            } else {
                IndexActivity.open(plugin)
            }
        }

    }


    open fun parsePluginFromConfig(configFile: File, fileName: String, packageName: String): Plugin {
        return try {
            // 获取插件专属目录
            val pluginDir = File(pluginsDir, packageName)
            if (!pluginDir.exists() || !pluginDir.isDirectory) {
                throw Exception("插件目录不存在：$packageName")
            }

            // 在插件专属目录中查找配置文件
            val newConfigFile = findConfigFile(pluginDir)
                ?: throw Exception("在插件目录 $packageName 中未找到有效的配置文件")

            // 读取配置文件内容
            val jsonString = FileReader(newConfigFile).use { it.readText() }
            val plugin = GsonUtils.fromJson(jsonString, Plugin::class.java)
            // 验证packageName是否匹配
            val configPackageName = plugin.packageName
            if (configPackageName != packageName) {
                throw Exception("配置文件中的packageName(${configPackageName})与目录名($packageName)不匹配")
            }

            with(plugin) {
                id = UUID.randomUUID().toString()

                path = newConfigFile.parent ?: ""
            }

            plugin
        } catch (e: Exception) {
            throw Exception("解析插件配置文件失败：${e.message}")
        }
    }

    private suspend fun savePlugin(plugin: Plugin) {
        try {
            // 获取已安装插件列表
            val pluginsJson = SPUtils.getInstance().getString(SPKeys.INSTALLED_PLUGINS, "[]")
            val pluginsArray = GsonUtils.fromJson<List<Plugin>>(pluginsJson, GsonUtils.getListType(Plugin::class.java))
            val newPluginsArray = arrayListOf<Plugin>()

            // 检查是否存在相同packageName的插件
            var existingFound = false
            for (i in 0 until pluginsArray.size) {
                val existingPackageName = pluginsArray[i].packageName

                // 如果找到相同packageName的插件，跳过它（从列表中删除）
                if (existingPackageName == plugin.packageName) {
                    existingFound = true
                    continue
                }

                // 保留其他插件
                newPluginsArray.add(pluginsArray[i])
            }

            // 如果删除了已存在的插件信息，提示用户
            if (existingFound) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "已更新插件信息", Toast.LENGTH_SHORT).show()
                }
            }


            // 添加新插件到数组
            newPluginsArray.add(plugin)

            // 保存更新后的列表
            SPUtils.getInstance().put(SPKeys.INSTALLED_PLUGINS, GsonUtils.toJson(newPluginsArray))

            runCatching {
                App.pluginRepository.insertPlugin(plugin)
            }

        } catch (e: Exception) {
            throw Exception("保存插件信息失败：${e.message}")
        }
    }

    private fun findConfigFile(directory: File): File? {
        // 定义可能的配置文件名列表，按优先级排序
        val possibleConfigNames = listOf(
            "assistsx_plugin_config.json",
            "plugin.json",
            "config.json"
        )

        // 递归搜索目录
        fun searchInDirectory(dir: File): File? {
            dir.listFiles()?.forEach { file ->
                when {
                    // 如果是配置文件，直接返回
                    file.isFile && possibleConfigNames.contains(file.name.lowercase()) ->
                        return file
                    // 如果是目录，递归搜索
                    file.isDirectory ->
                        searchInDirectory(file)?.let { return it }
                }
            }
            return null
        }

        return searchInDirectory(directory)
    }

    // 显示插件详情对话框
    private fun showPluginDetailDialog(plugin: Plugin) {
        val message = """
            名称：${plugin.name}
            版本：${plugin.versionName.ifEmpty { plugin.version }}
            描述：${plugin.description}
            包名：${plugin.packageName}
        """.trimIndent()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("插件详情")
            .setMessage(message)
            .setPositiveButton("确定", null)
            .show()
    }

    // 显示删除确认对话框
    private fun showDeleteConfirmDialog(plugin: Plugin) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("删除插件")
            .setMessage("确定要删除插件\"${plugin.name}\"吗？")
            .setPositiveButton("确定") { _, _ ->
                deletePlugin(plugin)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    // 删除插件
    private fun deletePlugin(plugin: Plugin) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 1. 删除插件文件


                runCatching {
                    App.pluginRepository.deletePluginByPackageName(plugin.packageName)
                }

                val pluginDir = File(pluginsDir, plugin.packageName)
                if (pluginDir.exists() && pluginDir.isDirectory) {
                    pluginDir.deleteRecursively()
                }

                // 2. 从已安装插件列表中移除
                val pluginsJson = SPUtils.getInstance().getString(SPKeys.INSTALLED_PLUGINS, "[]")
                val pluginsArray = GsonUtils.fromJson<List<Plugin>>(pluginsJson, GsonUtils.getListType(Plugin::class.java))
                val newPluginsArray = arrayListOf<Plugin>()

                for (i in 0 until pluginsArray.size) {
                    val currentPlugin = pluginsArray[i]
                    if (currentPlugin.packageName != plugin.packageName) {
                        newPluginsArray.add(currentPlugin)
                    }
                }

                // 3. 保存更新后的插件列表
                SPUtils.getInstance().put(SPKeys.INSTALLED_PLUGINS, GsonUtils.toJson(newPluginsArray))

                // 4. 在主线程更新UI
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "插件已删除", Toast.LENGTH_SHORT).show()
                    // 重新加载插件列表
                    loadInstalledPlugins()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "删除插件失败：${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    open fun checkFirstInstall() {
        // 检查是否是首次安装
        if (FirstInstallHelper.isFirstInstall()) {
            // 首次安装，安装默认插件
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    installDefaultPlugin()
                    // 标记为已安装
                    FirstInstallHelper.markFirstInstallCompleted()
                    FirstInstallHelper.markDefaultPluginInstalled()

                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "欢迎使用AssistsX！已为您安装默认示例插件", Toast.LENGTH_LONG).show()
                        // 刷新插件列表
                        loadInstalledPlugins()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "安装默认插件失败：${e.message}", Toast.LENGTH_SHORT).show()
                    }
                    // 即使安装失败也标记为已安装，避免重复尝试
                    FirstInstallHelper.markFirstInstallCompleted()
                }
            }
        }
    }

    /**
     * 安装默认插件
     */
    open suspend fun installDefaultPlugin() {
        withContext(Dispatchers.IO) {
            try {
                // 先读取配置文件获取插件信息
                val configJson = readDefaultPluginConfig()
                val packageName = configJson.getString("packageName")

                val pluginDir = File(pluginsDir, packageName).apply {
                    if (exists()) {
                        deleteRecursively()
                    }
                    mkdirs()
                }

                // 从assets目录复制默认插件文件
                copyAssetsToPluginDir(defaultAssetsPlugin, pluginDir)

                // 从配置文件创建默认插件对象
                val defaultPlugin = Plugin(
                    id = UUID.randomUUID().toString(),
                    name = configJson.optString("name", ""),
                    version = configJson.optString("version", ""),
                    versionName = configJson.optString("versionName", ""),
                    versionCode = configJson.optInt("versionCode", 0),
                    description = configJson.optString("description", ""),
                    path = pluginDir.absolutePath,
                    packageName = packageName,
                    isShowOverlay = configJson.optBoolean("isShowOverlay", false),
                    needScreenCapture = configJson.optBoolean("needScreenCapture", false),
                    overlayTitle = configJson.optString("overlayTitle", ""),
                    main = configJson.optString("main", ""),
                    icon = configJson.optString("icon", "")
                )

                // 保存插件信息
                savePlugin(defaultPlugin)
            } catch (e: Exception) {
                throw Exception("安装默认插件失败：${e.message}")
            }
        }
    }

    /**
     * 读取默认插件配置文件
     */
    private fun readDefaultPluginConfig(): JSONObject {
        return try {
            val assetManager = context?.assets ?: throw Exception("无法获取AssetManager")
            val configContent = assetManager.open("$defaultAssetsPlugin/assistsx_plugin_config.json").use { inputStream ->
                inputStream.bufferedReader().use { it.readText() }
            }
            val configJson = JSONObject(configContent)

            // 验证必需字段
            val requiredFields = listOf("name", "versionCode", "description", "packageName")
            for (field in requiredFields) {
                if (!configJson.has(field) || configJson.getString(field).isBlank()) {
                    throw Exception("配置文件缺少必需字段：$field")
                }
            }

            configJson
        } catch (e: Exception) {
            throw Exception("读取默认插件配置文件失败：${e.message}")
        }
    }

    /**
     * 从assets目录复制文件到插件目录
     */
    private fun copyAssetsToPluginDir(assetsPath: String, targetDir: File) {
        try {
            val assetManager = context?.assets ?: return
            val files = assetManager.list(assetsPath) ?: return

            for (filename in files) {
                val assetFilePath = "$assetsPath/$filename"
                val targetFile = File(targetDir, filename)

                try {
                    // 检查是否是目录
                    val subFiles = assetManager.list(assetFilePath)
                    if (subFiles != null && subFiles.isNotEmpty()) {
                        // 是目录，递归复制
                        targetFile.mkdirs()
                        copyAssetsToPluginDir(assetFilePath, targetFile)
                    } else {
                        // 是文件，直接复制
                        assetManager.open(assetFilePath).use { inputStream ->
                            targetFile.outputStream().use { outputStream ->
                                inputStream.copyTo(outputStream)
                            }
                        }
                    }
                } catch (e: Exception) {
                    // 如果作为目录访问失败，尝试作为文件处理
                    try {
                        assetManager.open(assetFilePath).use { inputStream ->
                            targetFile.outputStream().use { outputStream ->
                                inputStream.copyTo(outputStream)
                            }
                        }
                    } catch (fileException: Exception) {
                        throw Exception("复制文件失败 $assetFilePath: ${fileException.message}")
                    }
                }
            }
        } catch (e: Exception) {
            throw Exception("复制assets文件失败：${e.message}")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }

    companion object {
        private const val REQUEST_CODE_PICK_PLUGIN = 1001
        private const val DEFAULT_BUFFER_SIZE = 8192
    }
} 