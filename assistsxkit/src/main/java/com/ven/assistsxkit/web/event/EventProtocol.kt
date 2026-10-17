package com.ven.assistsxkit.web.event

/**
 * 跨 WebView 事件总线的协议常量（与 assistsx-js 的 event 模块一一对应）。
 */
object EventProtocol {

    /** 注入到页面的 JS 接口对象名（JS → Native） */
    const val interfaceName = "assistsxEvent"

    /** JS → Native 回包所调用的全局函数名 */
    const val callbackFunction = "assistsxEventCallback"

    /** Native → JS 推送所调用的全局函数名 */
    const val pushFunction = "onAssistsxEvent"

    /** 客户端加入（保留事件名，不进入业务 handler 空间） */
    const val eventClientJoined = "__client_joined"

    /** 客户端离开（保留事件名，不进入业务 handler 空间） */
    const val eventClientLeft = "__client_left"

    /** 页面角色：插件主页 */
    const val roleIndex = "index"

    /** 页面角色：浮窗内页面 */
    const val roleFloat = "float"

    /** 广播给同作用域其他全部客户端 */
    const val targetAll = "all"

    /** 仅发给自身（本页） */
    const val targetSelf = "self"

    /** 按角色选择目标，如 role:float */
    const val targetPrefixRole = "role:"

    /** 按页面声明的名称选择目标，如 name:settings */
    const val targetPrefixName = "name:"

    /** 无法确定插件作用域时的兜底值 */
    const val defaultScope = "default"

    /** 单条 payload 序列化后的最大字符数（超出直接拒绝，避免阻塞主线程） */
    const val maxPayloadChars = 64 * 1024

    /** 目标页面尚未完成注册时的待投递队列上限 */
    const val pendingLimit = 50

    /** 待投递消息的存活时间（毫秒），超时丢弃 */
    const val pendingTtlMs = 30_000L
}
