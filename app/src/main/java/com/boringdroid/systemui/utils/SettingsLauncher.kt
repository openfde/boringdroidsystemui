package com.boringdroid.systemui.utils

import android.app.ActivityOptions
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Bundle
import android.text.TextUtils
import android.util.Log

/**
 * Settings 页面跳转辅助。
 *
 * 两条硬规则（对照 packages_apps_settings 的实现）：
 *
 * 1. `SettingsActivity.shouldShowMultiPaneDeepLink()` 只在 Intent 带 action 时才走 deep link /
 *    两栏内嵌流程；`Intent()` + component 的空 action 写法会被当成普通子页面，全屏独立打开。
 * 2. 内嵌时显示哪一页，由 **解析出来的目标 Activity** 的 `com.android.settings.FRAGMENT_CLASS`
 *    meta-data 决定。不要把 component 换成 Settings 首页再塞 `:settings:show_fragment`，
 *    `SettingsHomepageActivity` 不读这个 extra，结果就是“打开了 Settings 但停在首页”。
 *
 * 另外这台机器的 Settings 有一批页面是被裁掉/禁用的，直接 startActivity 会抛
 * ActivityNotFoundException（比如 SOUND_SETTINGS 根本没有 handler，电池页被
 * PackageManager 禁用）。所以统一走 [open] / [openFirst]：先 resolve，按顺序退回候选页，
 * 全都不行只打日志，不让进程崩。
 */
object SettingsLauncher {

    private const val TAG = "SettingsLauncher"

    /**
     * 纯 ComponentName 跳转的兜底 action。
     * Settings 只判断 action 是否为空，实际打开哪一页仍由 component 解析，不会被这个值影响。
     */
    private const val FALLBACK_ACTION = "com.boringdroid.systemui.action.OPEN_SETTINGS"

    /**
     * 打开 Settings 页面。
     *
     * @param source   首选 intent（保留原 action / component / data / extras）
     * @param fallback 首选 action 在这台机器上不可用时的替补
     */
    fun open(context: Context, source: Intent, fallback: Intent? = null) {
        if (fallback == null) {
            openFirst(context, source)
        } else {
            openFirst(context, source, fallback)
        }
    }

    /** 依次尝试多个候选 intent，用第一个能解析到的；返回是否成功发起了跳转。 */
    fun openFirst(context: Context, vararg candidates: Intent): Boolean {
        for (candidate in candidates) {
            if (!canResolve(context, candidate)) continue
            val intent = prepare(candidate)
            try {
                context.startActivity(intent)
//                context.startActivity(intent, launchOptions(context))
                return true
            } catch (e: ActivityNotFoundException) {
                Log.e(TAG, "startActivity failed: $intent", e)
            } catch (e: SecurityException) {
                Log.e(TAG, "startActivity denied: $intent", e)
            }
            return false
        }
        Log.w(TAG, "no candidate resolved: " + candidates.joinToString(" | "))
        return false
    }

    /**
     * Settings 的两栏布局要求窗口足够宽。从悬浮窗 / 插件的 Context 起 Activity 时，系统会给新的
     * freeform 窗口一个默认小尺寸（实测 412dp 宽，config 里是 `w412dp ... compactNeeded`），
     * Settings 于是退化成单栏 —— 看起来就是“又变成了单 activity”。
     *
     * 这里显式给一个“整屏略留边”的启动边界，保证新窗口够宽、两栏能立起来。
     */
    private fun launchOptions(context: Context): Bundle {
        val metrics = context.resources.displayMetrics
        val inset = (24 * metrics.density).toInt()
        val bounds = Rect(
            inset,
            inset,
            (metrics.widthPixels - inset).coerceAtLeast(inset + 1),
            (metrics.heightPixels - inset).coerceAtLeast(inset + 1),
        )
        return ActivityOptions.makeBasic().setLaunchBounds(bounds).toBundle()
    }

    /** 返回补齐了 action / FLAG_ACTIVITY_NEW_TASK 的 Intent。 */
    fun prepare(source: Intent): Intent {
        val intent = Intent(source)
        if (TextUtils.isEmpty(intent.action)) {
            intent.action = FALLBACK_ACTION
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return intent
    }

    private fun canResolve(context: Context, source: Intent): Boolean {
        val intent = prepare(source)
        return try {
            context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null
        } catch (e: Exception) {
            false
        }
    }
}
