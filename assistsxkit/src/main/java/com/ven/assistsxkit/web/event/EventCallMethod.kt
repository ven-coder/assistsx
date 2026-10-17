package com.ven.assistsxkit.web.event

/**
 * 跨 WebView 事件总线的方法名常量（window.assistsxEvent.call 使用）
 */
object EventCallMethod {

    /** 注册当前页面为事件客户端，返回自身身份、在线客户端列表与保留消息 */
    const val registerClient = "registerClient"

    /** 注销当前页面 */
    const val unregisterClient = "unregisterClient"

    /** 发送事件（广播或定向） */
    const val emit = "emit"

    /** 查询同作用域在线客户端 */
    const val getClients = "getClients"

    /** 查询保留消息 */
    const val getRetained = "getRetained"

    /** 清空保留消息 */
    const val clearRetained = "clearRetained"
}
