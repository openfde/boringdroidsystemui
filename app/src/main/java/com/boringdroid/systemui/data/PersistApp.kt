package com.boringdroid.systemui.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "persist_app_table")
data class PersistApp(@PrimaryKey @ColumnInfo(name = "packageName")
                      val packageName: String,      // 唯一标识，android package, linux "com.fde.x11#mate-terminal"
                      var position: Int?,           // 在程序坞的位置
                      val platformType: Int,       // 类型 -1 overview, 0 android, 1 X11
                      val icon: String?,        // 图标路径
                      val programName: String,     // 程序名称，android application label, linux 窗口名
                      val componentName: String?,   // 组件名
                      val path: String?,            // linux 执行路径
                      val name: String?,            // linux 显示名称
                      val iconSuffix: String?)      // drawable SURFFIX_SVG  SURFFIX_SVGZ SURFFIX_PNG