package com.google.android.accessibility.switchaccess

import android.accessibilityservice.AccessibilityServiceInfo
import android.os.Build
import com.ven.assists.service.AssistsService

class SwitchAccessService : AssistsService() {
    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = serviceInfo


        // 设置所有需要的 flags（按位或）

        info.flags = info.flags or
                AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE or
                AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            info.flags = info.flags or
                    AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                    AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                    AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS or
                    AccessibilityServiceInfo.FLAG_REQUEST_ACCESSIBILITY_BUTTON or
                    AccessibilityServiceInfo.FLAG_REQUEST_FINGERPRINT_GESTURES
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            info.flags = info.flags or
                    AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                    AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                    AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS or
                    AccessibilityServiceInfo.FLAG_REQUEST_ACCESSIBILITY_BUTTON or
                    AccessibilityServiceInfo.FLAG_REQUEST_FINGERPRINT_GESTURES or
                    AccessibilityServiceInfo.FLAG_REQUEST_MULTI_FINGER_GESTURES
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            info.flags = info.flags or
                    AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                    AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                    AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS or
                    AccessibilityServiceInfo.FLAG_REQUEST_ACCESSIBILITY_BUTTON or
                    AccessibilityServiceInfo.FLAG_REQUEST_FINGERPRINT_GESTURES or
                    AccessibilityServiceInfo.FLAG_REQUEST_MULTI_FINGER_GESTURES or
                    AccessibilityServiceInfo.FLAG_REQUEST_2_FINGER_PASSTHROUGH
        }


        // 更新服务配置
        this.serviceInfo = info
    }
}