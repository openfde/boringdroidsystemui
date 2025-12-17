package com.boringdroid.systemui.data

import com.boringdroid.systemui.TaskInfo

data class DockContext(
    val action: String?, val type: Int,
    val name: String?, val app: AppData?, val taskInfo: TaskInfo ?,
    val enable: Boolean = true
)
