package com.ven.assistsxkit

import android.app.Application
import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.Utils

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        Utils.init(this)
    }
}