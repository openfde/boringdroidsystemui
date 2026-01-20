package com.boringdroid.systemui.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.boringdroid.systemui.view.AboutWindow.Companion.ACTION_DEFER_UPDATE
import com.boringdroid.systemui.view.AboutWindow.Companion.ACTION_UPDATE_NOW

class UpdateActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_UPDATE_NOW -> {
                // TODO: 执行“立即更新”逻辑（如开始安装、调用系统更新服务等）
                android.util.Log.d("UpdateActionReceiver", "User chose: Update Now")
            }
            ACTION_DEFER_UPDATE -> {
                // TODO: 执行“下次开机更新”逻辑（如保存标记）
                android.util.Log.d("UpdateActionReceiver", "User chose: Defer to Boot")
                // 例如：保存到 SharedPreferences
                context.getSharedPreferences("update_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean("defer_update_on_boot", true)
                    .apply()
            }
        }
    }
}