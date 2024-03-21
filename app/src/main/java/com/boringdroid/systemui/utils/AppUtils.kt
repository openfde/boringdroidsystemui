package com.boringdroid.systemui.utils

import android.app.ActivityManager
import android.app.PendingIntent
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import com.boringdroid.systemui.AppTask
import com.boringdroid.systemui.Log
import com.boringdroid.systemui.data.App
import com.boringdroid.systemui.data.AppData
import java.io.*
import java.util.*


object AppUtils {
    const val PINNED_LIST = "pinned.lst"
    const val DOCK_PINNED_LIST = "dock_pinned.lst"
    const val DESKTOP_LIST = "desktop.lst"
    var currentApp = ""

    fun getInstalledApps(pm: PackageManager): ArrayList<App>? {
        val apps = ArrayList<App>()
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)
        val appsInfo = pm.queryIntentActivities(intent, 0)

        //TODO: Filter Google App
        for (appInfo in appsInfo) {
            val label = appInfo.activityInfo.loadLabel(pm).toString()
            val icon = appInfo.activityInfo.loadIcon(pm)
            val packageName = appInfo.activityInfo.packageName
            apps.add(App(label, packageName, icon))
        }
        Collections.sort(
            apps
        ) { p1: App, p2: App ->
            p1.getName()!!
                .compareTo(p2.getName()!!, ignoreCase = true)
        }
        return apps
    }

    fun getPinnedApps(context: Context?, pm: PackageManager?, type: String?): ArrayList<App> {
        val apps = ArrayList<App>()
        try {
            val br = BufferedReader(
                FileReader(
                    File(
                        context!!.filesDir, type
                    )
                )
            )
            var applist: String
            try {
                if (br.readLine().also { applist = it } != null) {
                    val applist2 =
                        applist.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    for (app in applist2) {
                        try {
                            val appInfo = pm!!.getApplicationInfo(app, 0)
                            apps.add(
                                App(
                                    pm.getApplicationLabel(appInfo).toString(), app,
                                    pm.getApplicationIcon(app)
                                )
                            )
                        } catch (e: PackageManager.NameNotFoundException) {
                            //app is no longer available, lets unpin it
                            unpinApp(context, app, type)
                        }
                    }
                }
            } catch (e: IOException) {
            }
        } catch (e: FileNotFoundException) {
        }
        return apps
    }

    fun pinApp(context: Context?, app: String?, type: String?) {
        try {
            val file = File(context!!.filesDir, type)
            val fw = FileWriter(file, true)
            fw.write("$app ")
            fw.close()
        } catch (e: IOException) {
        }
    }

    fun unpinApp(context: Context?, app: String?, type: String?) {
        try {
            val file = File(context!!.filesDir, type)
            val br = BufferedReader(FileReader(file))
            var applist: String
            if (br.readLine().also { applist = it } != null) {
                applist = applist.replace("$app ", "")
                val fw = FileWriter(file, false)
                fw.write(applist)
                fw.close()
            }
        } catch (e: IOException) {
        }
    }

    fun moveApp(context: Context, app: String?, type: String?, direction: Int) {
        try {
            val file = File(context.filesDir, type)
            val br = BufferedReader(FileReader(file))
            var applist: String
            var what = ""
            var with = ""
            if (br.readLine().also { applist = it } != null) {
                val apps =
                    applist.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val pos = findInArray(app, apps)
                if (direction == 0 && pos > 0) {
                    what = apps[pos - 1] + " " + app
                    with = app + " " + apps[pos - 1]
                } else if (direction == 1 && pos < apps.size - 1) {
                    what = app + " " + apps[pos + 1]
                    with = apps[pos + 1] + " " + app
                }
                applist = applist.replace(what, with)
                val fw = FileWriter(file, false)
                fw.write(applist)
                fw.close()
            }
        } catch (e: IOException) {
        }
    }

    fun findInArray(key: String?, array: Array<String>): Int {
        for (i in array.indices) {
            if (array[i].contains(key!!)) return i
        }
        return -1
    }

    fun isPinned(context: Context?, app: String?, type: String?): Boolean {
        try {
            val br = BufferedReader(
                FileReader(
                    File(
                        context!!.filesDir, type
                    )
                )
            )
            var applist: String
            if (br.readLine().also { applist = it } != null) {
                return applist.contains(app!!)
            }
        } catch (e: IOException) {
        }
        return false
    }

    fun isGame(pm: PackageManager?, packageName: String?): Boolean {
        return try {
            val info = pm!!.getApplicationInfo(packageName!!, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                info.category == ApplicationInfo.CATEGORY_GAME
            } else {
                info.flags and ApplicationInfo.FLAG_IS_GAME == ApplicationInfo.FLAG_IS_GAME
            }
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun getCurrentLauncher(pm: PackageManager?): String {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        val resolveInfo = pm!!.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolveInfo!!.activityInfo.packageName
    }

    fun setWindowMode(am: ActivityManager, taskId: Int, mode: Int) {
        try {
            val setWindowMode = am.javaClass.getMethod(
                "setTaskWindowingMode",
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                Boolean::class.javaPrimitiveType
            )
            setWindowMode.invoke(am, taskId, mode, false)
        } catch (e: Exception) {
        }
    }

    fun getRunningTasks(am: ActivityManager?, pm: PackageManager?, max: Int): ArrayList<AppTask?> {
        val tasksInfo = am!!.getRunningTasks(max)
        currentApp = tasksInfo[0].baseActivity!!.packageName
        val appTasks = ArrayList<AppTask?>()
        for (taskInfo in tasksInfo) {
            try {
                //Exclude systemui, launcher and other system apps from the tasklist
                if (taskInfo.baseActivity!!.packageName.contains("com.android.systemui")
                    || taskInfo.baseActivity!!.packageName.contains("com.google.android.packageinstaller")
                ) continue

                //Hack to save Dock settings activity ftom being excluded
                if ((taskInfo.topActivity!!.className == "cu.axel.smartdock.activities.MainActivity" || taskInfo.topActivity!!.className != "cu.axel.smartdock.activities.DebugActivity" && taskInfo.topActivity!!.packageName == getCurrentLauncher(
                        pm
                    )
                            )
                ) continue
                if (Build.VERSION.SDK_INT > 29) {
                    try {
                        val isRunning = taskInfo.javaClass.getField("isRunning")
                        val running = isRunning.getBoolean(taskInfo)
                        if (!running) continue
                    } catch (e: Exception) {
                    }
                }
                appTasks.add(
                    AppTask(
                        taskInfo.id,
                        pm!!.getActivityInfo(taskInfo.topActivity!!, 0).loadLabel(
                            pm
                        ).toString(),
                        taskInfo.topActivity!!.packageName,
                        pm.getActivityIcon(taskInfo.topActivity!!)
                    )
                )
            } catch (e: PackageManager.NameNotFoundException) {
            }
        }
        return appTasks
    }

    fun getRecentTasks(context: Context?, max: Int): ArrayList<AppTask> {
        val usm = context!!.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val start = System.currentTimeMillis() - SystemClock.elapsedRealtime()
        val usageStats = usm.queryUsageStats(
            UsageStatsManager.INTERVAL_BEST, start,
            System.currentTimeMillis()
        )
        val appTasks = ArrayList<AppTask>()
        Collections.sort(usageStats) { p1: UsageStats, p2: UsageStats ->
            java.lang.Long.compare(
                p2.lastTimeUsed,
                p1.lastTimeUsed
            )
        }
        for (stat in usageStats) {
            val app = stat.packageName
            try {
                if (isLaunchable(context, app) && app != getCurrentLauncher(
                        context.packageManager
                    )
                ) appTasks.add(
                    AppTask(
                        -1, getPackageLabel(context, app), app,
                        context.packageManager.getApplicationIcon(app)
                    )
                )
            } catch (e: PackageManager.NameNotFoundException) {
            }
            if (appTasks.size >= max) break
        }
        return appTasks
    }

    fun isSystemApp(context: Context?, app: String?): Boolean {
        return try {
            val appInfo = context!!.packageManager.getApplicationInfo(app!!, 0)
            appInfo.flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun isLaunchable(context: Context?, app: String): Boolean {
        val resolveInfo = context!!.packageManager.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(app), 0
        )
        return resolveInfo.size > 0
    }

    fun removeTask(am: ActivityManager?, id: Int) {
        try {
            val removeTask = am!!.javaClass.getMethod("removeTask", Int::class.javaPrimitiveType)
            removeTask.invoke(am, id)
        } catch (e: Exception) {
            Log.e("Dock", e.toString() + e.cause.toString())
        }
    }

    fun getPackageLabel(context: Context?, packageName: String?): String {
        try {
            val pm = context!!.packageManager
            val appInfo = pm.getApplicationInfo(packageName!!, 0)
            return pm.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
        }
        return ""
    }

    fun getAppIcon(context: Context?, app: String?): Drawable? {
        try {
            return context!!.packageManager.getApplicationIcon(app!!)
        } catch (e: PackageManager.NameNotFoundException) {
        }
        return context!!.getDrawable(android.R.drawable.sym_def_app_icon)
    }

    public fun uninstallApp(mContext: Context, appData: AppData) {
        val packageUri = Uri.parse("package:${appData.packageName}")
        val uninstallIntent = Intent(Intent.ACTION_UNINSTALL_PACKAGE, packageUri)
        mContext?.startActivity(uninstallIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    public fun createShortcut(mContext: Context, app: AppData) {
//        Log.d(AllAppsWindow.TAG, "createShortcut() called with: app = [${app.name}]")
        val icon = Icon.createWithBitmap(Utils.drawableToBitmap(app.icon!!))
        val shortcutManager: ShortcutManager? =
            mContext?.getSystemService(ShortcutManager::class.java)
        if (shortcutManager != null && shortcutManager.isRequestPinShortcutSupported) {
            val launchIntentForPackage: Intent = mContext?.getPackageManager()
                ?.getLaunchIntentForPackage(app.packageName!!) as Intent
            launchIntentForPackage.action = Intent.ACTION_MAIN
            val pinShortcutInfo = ShortcutInfo.Builder(mContext, app.name)
                .setLongLabel(app.name!!)
                .setShortLabel(app.name!!)
                .setIcon(icon)
                .setIntent(launchIntentForPackage)
                .build()
            val pinnedShortcutCallbackIntent =
                shortcutManager.createShortcutResultIntent(pinShortcutInfo)
            val successCallback = PendingIntent.getBroadcast(
                mContext, 0,
                pinnedShortcutCallbackIntent, PendingIntent.FLAG_IMMUTABLE
            )
            shortcutManager.requestPinShortcut(pinShortcutInfo, successCallback.intentSender)
        }
    }

    public fun toConpatiblePage(mContext: Context, packageName: String, appName: String) {
//        var inte = Intent(mContext, CompatibleListActivity::class.java)
//        inte.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//        inte.putExtra("packageName", packageName)
//        inte.putExtra("appName", appName)
//        mContext.startActivity(inte);

//        CompatibleConfig.call(mContext)

//        LogTools.i("toConpatiblePage")
//        val result = CompatibleConfig.queryValueData(
//            mContext, "com.android.gallery3d",
//           "isAllowScreenshotAndRecord"
//        )
//        LogTools.i("toConpatiblePage result "+result)


        ParseUtils.parseListXML(mContext);
        ParseUtils.parseValueXML(mContext);

        val intent = Intent()
        val cn: ComponentName? = ComponentName.unflattenFromString("com.android.settings/.Settings\$SetCompatibleActivity")
        intent.component = cn;
        intent.putExtra("appName",appName)
        intent.putExtra("packageName",packageName)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        mContext.startActivity(intent)
    }


}