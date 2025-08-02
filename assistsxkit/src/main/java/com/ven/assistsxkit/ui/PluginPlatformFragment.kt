package com.ven.assistsxkit.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.toColorInt
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.blankj.utilcode.util.BarUtils
import com.ven.assistsxkit.ui.fragment.InstalledPluginsFragment
import com.ven.assistsxkit.R
import com.ven.assistsxkit.databinding.ActivityPluginPlatformBinding
import com.ven.assistsxkit.ui.fragment.SettingsFragment

open class PluginPlatformFragment : Fragment() {

    val binding: ActivityPluginPlatformBinding by lazy { ActivityPluginPlatformBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BarUtils.setStatusBarColor(requireActivity(), "#23252A".toColorInt())
        BarUtils.setNavBarColor(requireActivity(), "#23252A".toColorInt())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViewPager()
        setupBottomNavigation()
    }

    open fun setupViewPager() {
        binding.viewPager.apply {
            adapter = object : FragmentStateAdapter(this@PluginPlatformFragment) {
                override fun getItemCount(): Int = 2

                override fun createFragment(position: Int): Fragment {
                    return when (position) {
                        0 -> InstalledPluginsFragment()
                        1 -> SettingsFragment()
                        else -> throw IllegalArgumentException("Invalid position $position")
                    }
                }
            }
            isUserInputEnabled = false // 禁用滑动切换
        }
    }

    open fun setupBottomNavigation() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_installed -> {
                    binding.viewPager.currentItem = 0
                }

                R.id.nav_settings -> {
                    binding.viewPager.currentItem = 1
                }
            }
            true
        }
    }

} 