package com.ven.assistsxkit

import android.graphics.drawable.PictureDrawable
import android.net.wifi.WifiManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.BarUtils
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.google.android.material.button.MaterialButton
import com.lzy.okgo.OkGo
import com.ven.assistsxkit.model.Plugin
import com.ven.assists.utils.CoroutineWrapper
import com.ven.assists.utils.runMain
import com.ven.assistsxkit.common.GlideApp
import com.ven.assistsxkit.databinding.ActivityScanBinding
import com.ven.assistsxkit.databinding.ItemListPluginScanBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import java.net.InetSocketAddress
import java.net.Socket
import androidx.core.net.toUri
import java.util.UUID
import android.content.Intent
import android.util.Log
import com.blankj.utilcode.util.KeyboardUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.ven.assistsxkit.databinding.ItemPluginBinding
import kotlinx.coroutines.CancellationException
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class ScanActivity : AppCompatActivity() {
    private var scanJob: Job? = null
    private val viewBind by lazy {
        ActivityScanBinding.inflate(layoutInflater).apply {

        }
    }
    private val scanResults = mutableListOf<ScanResult>()
    private var adapter: ScanAdapter? = null

    data class ScanResult(
        val url: String,
        val plugin: Plugin
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BarUtils.setStatusBarColor(this, "#23252A".toColorInt(), true)
        BarUtils.setStatusBarLightMode(this, false)

        setContentView(viewBind.root)

        // 使用自定义Toolbar替换ActionBar
        setSupportActionBar(viewBind.toolbar)
        // 设置标题
        viewBind.toolbar.title = "扫描局域网插件"
        // 设置返回按钮图标
        viewBind.toolbar.setNavigationIcon(R.drawable.abc_ic_ab_back_material)
        // 设置返回按钮颜色为colorPrimary
        viewBind.toolbar.navigationIcon?.setTint(resources.getColor(R.color.colorPrimary, theme))
        // 设置菜单项文字颜色
        viewBind.toolbar.setTitleTextColor(resources.getColor(R.color.colorPrimary, theme))
        viewBind.toolbar.setSubtitleTextColor(resources.getColor(R.color.colorPrimary, theme))
        // 设置返回按钮点击事件
        viewBind.toolbar.setNavigationOnClickListener {
            // 返回
            finish()
        }

        // 初始化列表
        adapter = ScanAdapter(scanResults)
        viewBind.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@ScanActivity)
            adapter = this@ScanActivity.adapter
        }

        // 设置扫描按钮点击事件
        viewBind.btnScan.setOnClickListener {
            KeyboardUtils.hideSoftInput(this)
            startScan()
        }

        // 启动时自动扫描
        startScan()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // 不显示菜单，因为扫描按钮已经移到端口输入框旁边
        return false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                // 处理返回按钮点击
                finish()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        return super.onPrepareOptionsMenu(menu)
    }

    companion object {
        private const val MENU_REFRESH = 1
    }

    private fun startScan() {
        // 取消之前的扫描
        scanJob?.cancel()

        // 获取端口号
        val portText = viewBind.etPort.text.toString()
        if (portText.isEmpty()) {
            ToastUtils.showShort("请输入端口号")
            return
        }

        val port = portText.toIntOrNull()
        if (port == null || port <= 0 || port > 65535) {
            ToastUtils.showShort("请输入有效的端口号 (1-65535)")
            return
        }

        // 清空结果
        scanResults.clear()
        adapter?.notifyDataSetChanged()

        // 显示空视图
        updateEmptyView()

        // 显示进度条
        viewBind.layoutProgress.isVisible = true
        viewBind.progressBar.progress = 0
        viewBind.progressBar.max = 100  // 修改为100以便于百分比显示

        // 设置toolbar标题为扫描中
        viewBind.toolbar.title = "局域网插件扫描中..."

        scanJob = CoroutineWrapper.launch {

//            runCatching {
//                val url = "http://192.168.10.236:5173"
//
//                val request = Request.Builder().url("$url/assistsx_plugin_config.json")
//                    .get()
//                    .build()
//                val httpClient = OkHttpClient.Builder()
////                    .callTimeout(1000, TimeUnit.MILLISECONDS)
////                    .readTimeout(1000, TimeUnit.MILLISECONDS)
////                    .writeTimeout(1000, TimeUnit.MILLISECONDS)
//                    .build()
//                val response = httpClient.newCall(request).execute()
//
//                if (response.isSuccessful) {
//                    response.body?.string()?.let { jsonString ->
//                        val config = GsonUtils.fromJson(jsonString, Plugin::class.java)
//                        config.path = url
//                        runMain {
//                            scanResults.add(ScanResult(url, config))
//                            adapter?.notifyItemInserted(scanResults.size - 1)
//                            updateEmptyView()
//                        }
//                    }
//                }
//            }.onFailure { LogUtils.e(it) }


            runCatching {
                val subnet = getSubnetAddress()
                val totalIps = 254
                val scannedCount = AtomicInteger(0)

                // 使用 10 个协程并发扫描
                coroutineScope {
                    repeat(50) { offset ->
                        launch {
                            var i = offset + 1
                            while (i <= totalIps && isActive) {
                                // 当前 IP
                                val ip = "$subnet.$i"
                                // 尝试连接端口判断是否开放
                                // 找到服务器，尝试获取配置
                                val url = "http://$ip:$port"
                                runCatching {
                                    val request = Request.Builder().url("$url/assistsx_plugin_config.json")
                                        .get()
                                        .build()
                                    val httpClient = OkHttpClient.Builder()
                                        .callTimeout(500, TimeUnit.MILLISECONDS)
                                        .readTimeout(500, TimeUnit.MILLISECONDS)
                                        .writeTimeout(500, TimeUnit.MILLISECONDS)
                                        .build()

//                                    if (url == "http://192.168.10.236:5173") {
//                                        LogUtils.d("扫描", url)
//                                    }

                                    val response = httpClient.newCall(request).execute()

                                    if (response.isSuccessful) {
                                        response.body?.string()?.let { jsonString ->
                                            val config = GsonUtils.fromJson(jsonString, Plugin::class.java)
                                            config.path = url
                                            runMain {
                                                scanResults.add(ScanResult(url, config))
                                                adapter?.notifyItemInserted(scanResults.size - 1)
                                                updateEmptyView()
                                            }
                                        }
                                    }
                                }.onFailure {
                                    // 忽略单个 IP 扫描异常，继续下一次
//                                    if (url == "http://192.168.10.236:5173") {
//                                        LogUtils.d("扫描失败", url, it)
//                                    }
                                }

                                // 更新进度
                                val done = scannedCount.incrementAndGet()
                                val percentage = ((done.toFloat() / totalIps) * 100).toInt()
                                runMain {
                                    viewBind.progressBar.progress = percentage
                                    viewBind.tvProgress.text = "正在扫描: ${percentage}% (已发现${scanResults.size}个插件)"
                                }

                                // 递增到下一个待扫描 IP
                                i += 50
                            }
                        }
                    }
                }

            }.onFailure {
                if (it !is CancellationException) {
                    LogUtils.e("扫描失败", it)
                    ToastUtils.showShort("扫描失败：${it.message}")
                }
            }
            runMain {
                // 扫描结束，恢复标题和隐藏进度条
                viewBind.toolbar.title = "扫描插件"
                viewBind.layoutProgress.isVisible = false
            }
        }
    }

    private fun updateEmptyView() {
        viewBind.tvEmpty.isVisible = scanResults.isEmpty()
        viewBind.recyclerView.isVisible = scanResults.isNotEmpty()
        CoroutineWrapper.launch {
            val address = getSubnetAddress()
            runMain {
                if (viewBind.tvEmpty.isVisible) {
                    viewBind.tvEmpty.text = "未发现插件\n\n局域IP：$address\n请检查手机与插件\n是否在相同局域IP以及端口是否一致"
                }
            }
        }
    }

    private fun getSubnetAddress(): String {
        val wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        val ipAddress = wifiManager.connectionInfo.ipAddress
        return String.format(
            "%d.%d.%d",
            ipAddress and 0xff,
            ipAddress shr 8 and 0xff,
            ipAddress shr 16 and 0xff
        )
    }

    inner class ScanAdapter(private val items: List<ScanResult>) :
        RecyclerView.Adapter<ScanAdapter.ViewHolder>() {

        inner class ViewHolder(val view: ItemPluginBinding) : RecyclerView.ViewHolder(view.root) {
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_list_plugin_scan, parent, false)
            return ViewHolder(ItemPluginBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.view.txtName.text = item.plugin.name
            holder.view.txtVersion.text = "v${item.plugin.versionName.ifEmpty { item.plugin.version }}"
            holder.view.txtDescription.text = item.plugin.description
            holder.view.txtDescription.isVisible = item.plugin.description.isNotEmpty()
            holder.view.btnAction.isVisible = false
            holder.view.btnAdd.isVisible = true
            // 设置点击事件显示详情对话框
            holder.itemView.setOnClickListener {
                showPluginDetailDialog(item)
            }

            holder.view.btnAdd.setOnClickListener {
                // 生成唯一ID
                item.plugin.id = UUID.randomUUID().toString()
                // 设置插件路径为URL
                item.plugin.path = item.url
                // 创建插件对象并返回
                val intent = Intent()
                intent.putExtra("action", "install_plugin")
                intent.putExtra("plugin_data", GsonUtils.toJson(item.plugin))
                setResult(RESULT_OK, intent)
                finish()
            }

            if (item.url.endsWith(".svg")) {
                GlideApp.with(ActivityUtils.getTopActivity())
                    .`as`(PictureDrawable::class.java)
                    .load(item.url.toUri().buildUpon().appendPath(item.plugin.icon).build())
                    .placeholder(R.drawable.ic_baseline_extension_24)
                    .error(R.drawable.ic_baseline_extension_24)
                    .into(holder.view.imgIcon)

            } else {
                GlideApp.with(ActivityUtils.getTopActivity())
                    .load(item.url.toUri().buildUpon().appendPath(item.plugin.icon).build())
                    .placeholder(R.drawable.ic_baseline_extension_24)
                    .error(R.drawable.ic_baseline_extension_24)
                    .into(holder.view.imgIcon)

            }
        }

        override fun getItemCount() = items.size
    }

    // 显示插件详情对话框
    private fun showPluginDetailDialog(scanResult: ScanResult) {
        val plugin = scanResult.plugin
        val message = """
            名称：${plugin.name}
            版本：${plugin.versionName.ifEmpty { plugin.version }}
            描述：${plugin.description}
            包名：${plugin.packageName}
            URL：${scanResult.url}
        """.trimIndent()

        MaterialAlertDialogBuilder(this)
            .setTitle("插件详情")
            .setMessage(message)
            .setPositiveButton("确定", null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        scanJob?.cancel()
    }
} 