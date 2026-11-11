package com.boringdroid.systemui.view

/**
 * 通知面板 / 顶栏弹窗之间的互斥控制。
 *
 * 原先声明在 SystemStateLayout 里；dock/navi 代码移除后把接口独立出来，
 * 避免为了一个接口保留整个废弃的 SystemStateLayout。
 */
interface SystemStateListener {
    fun hideNotification()
    fun showNotification()
    fun syncVisible(which: Int)
}
