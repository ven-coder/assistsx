package com.ven.assistsxkit.ui.adapter

import android.content.res.ColorStateList
import android.graphics.drawable.PictureDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.ActivityUtils
import com.ven.assistsxkit.common.GlideApp
import com.ven.assistsxkit.databinding.ItemPluginBinding
import com.ven.assistsxkit.model.Plugin
import java.io.File
import androidx.core.net.toUri
import androidx.core.view.isVisible
import com.ven.assistsxkit.R

class InstalledPluginAdapter(
    private val onItemClick: (Plugin) -> Unit,
    private val onActionClick: (Plugin) -> Unit,
    private val onItemLongClick: (Plugin) -> Unit
) : ListAdapter<Plugin, InstalledPluginAdapter.PluginViewHolder>(PluginDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PluginViewHolder {
        return PluginViewHolder(
            ItemPluginBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: PluginViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PluginViewHolder(
        private val binding: ItemPluginBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            // 设置整个项的点击事件
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }

            // 设置长按事件
            binding.root.setOnLongClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemLongClick(getItem(position))
                }
                true
            }

            // 设置操作按钮的点击事件
            binding.btnAction.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onActionClick(getItem(position))
                }
            }
        }

        fun bind(plugin: Plugin) {
            binding.apply {
                // 设置基本信息
                txtName.text = plugin.name
                txtVersion.text = plugin.versionName.ifEmpty { plugin.version }
                txtDescription.text = plugin.description
                txtDescription.isVisible = plugin.description.isNotEmpty()

                // 加载插件图标
                if (plugin.icon.endsWith(".svg")) {
                    GlideApp.with(ActivityUtils.getTopActivity())
                        .`as`(PictureDrawable::class.java).let {
                            if (plugin.path.startsWith("http")) {
                                it.load(plugin.path.toUri().buildUpon().appendPath(plugin.icon).build())
                            } else {
                                val fileIcon = File(plugin.path, plugin.icon)
                                it.load(fileIcon)
                            }
                        }
                        .placeholder(R.drawable.ic_baseline_extension_24)
                        .error(R.drawable.ic_baseline_extension_24)
                        .into(imgIcon)

                } else {
                    GlideApp.with(ActivityUtils.getTopActivity())
                        .let {
                            if (plugin.path.startsWith("http")) {
                                it.load(plugin.path.toUri().buildUpon().appendPath(plugin.icon).build())
                            } else {
                                val fileIcon = File(plugin.path, plugin.icon)
                                it.load(fileIcon)
                            }
                        }
                        .placeholder(R.drawable.ic_baseline_extension_24)
                        .error(R.drawable.ic_baseline_extension_24)
                        .into(imgIcon)

                }

                // 设置操作按钮样式
                btnAction.apply {
                    val isRunning = false // 这里可以根据实际状态判断
                    setIconResource(if (isRunning) R.drawable.ic_plugin_stop else R.drawable.ic_plugin_start)
                    contentDescription = if (isRunning) "停止插件" else "启动插件"
                    // 设置图标颜色
                    val iconTint = if (isRunning)
                        context.getColor(android.R.color.holo_red_dark)
                    else
                        "#3EDA83".toColorInt()
                    setIconTint(ColorStateList.valueOf(iconTint))
                }
            }
        }
    }

    private class PluginDiffCallback : DiffUtil.ItemCallback<Plugin>() {
        override fun areItemsTheSame(oldItem: Plugin, newItem: Plugin): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Plugin, newItem: Plugin): Boolean {
            return oldItem == newItem
        }
    }
} 