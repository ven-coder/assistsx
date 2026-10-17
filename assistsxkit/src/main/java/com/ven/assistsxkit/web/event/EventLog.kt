package com.ven.assistsxkit.web.event

import com.blankj.utilcode.util.LogUtils

/**
 * 事件总线内部日志：统一 tag，仅输出到系统日志（走 logcat）。
 *
 * 按工作区约定，开源库不直接写宿主日志文件（AssistsLog / logAppend）。
 */
internal object EventLog {
    private const val TAG = "AssistsxWebEventBus"

    fun d(message: String) {
        LogUtils.d(TAG, message)
    }

    fun e(message: String) {
        LogUtils.e(TAG, message)
    }

    fun e(throwable: Throwable) {
        LogUtils.e(TAG, throwable)
    }
}
