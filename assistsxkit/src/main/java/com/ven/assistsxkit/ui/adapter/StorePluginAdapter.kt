package com.ven.assistsxkit.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ven.assistsxkit.R
import com.ven.assistsxkit.databinding.ItemStorePluginBinding
import com.ven.assistsxkit.model.Plugin

class StorePluginAdapter(
    private val onItemClick: (Plugin) -> Unit,
    private val onInstallClick: (Plugin) -> Unit
) : ListAdapter<Plugin, StorePluginAdapter.StorePluginViewHolder>(PluginDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StorePluginViewHolder {
        return StorePluginViewHolder(
            ItemStorePluginBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: StorePluginViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class StorePluginViewHolder(
        private val binding: ItemStorePluginBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }

            binding.btnInstall.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onInstallClick(getItem(position))
                }
            }
        }

        fun bind(plugin: Plugin) {
            binding.apply {
                // 设置插件名称
                txtName.text = plugin.name
                
                // 设置版本信息
                txtVersion.text = "版本: ${plugin.version}"
                
                // 设置描述信息
                txtDescription.text = plugin.description
                
                // 设置包名信息
                txtPackageName.text = plugin.packageName
                
                // 加载插件图标
                // 这里需要根据实际的图标URL格式进行调整
                // 如果icon字段只是文件名，可能需要拼接完整的URL
                imgIcon.setImageResource(R.drawable.ic_baseline_extension_24)
                
                // 如果需要加载网络图标，可以使用以下代码：
                // GlideApp.with(itemView.context)
                //     .load("https://your-base-url/${plugin.icon}")
                //     .placeholder(R.drawable.ic_baseline_extension_24)
                //     .error(R.drawable.ic_baseline_extension_24)
                //     .into(imgIcon)
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