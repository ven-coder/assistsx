package com.ven.assistsxkit

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.blankj.utilcode.util.BarUtils
import com.ven.assists.utils.CoroutineWrapper
import com.ven.assistsxkit.databinding.SettingGuideBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class SettingGuideActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BarUtils.setStatusBarColor(this, Color.TRANSPARENT)
        CoroutineWrapper.launch {
            delay(500)
            withContext(Dispatchers.Main) {
                SettingGuideBinding.inflate(layoutInflater).apply {
                    setContentView(root)
                    ivClose.setOnClickListener {
                        finish()
                    }
                }
            }
        }
    }
}
