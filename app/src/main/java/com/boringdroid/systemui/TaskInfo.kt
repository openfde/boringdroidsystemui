package com.boringdroid.systemui

import android.content.ComponentName
import android.graphics.drawable.Drawable
import android.text.TextUtils
import android.util.Log

class TaskInfo(val packageName: String,
               val program: String) {

    companion object {
        const val ID_UNDEFINED = 0

        const val STATE_UNFEFINED = 0
        const val STATE_NORMAL = 0
        const val STATE_RUNNING = 1
        const val STATE_TOP = 2

        const val DOCK_TYPE_UNDEFINED = 0;
        const val DOCK_TYPE_PERSISIT = 1
        const val DOCK_TYPE_NORMAL = 2

        const val PLATFORM_TYPE_ANDROID = 0
        const val PLATFORM_TYPE_X11 = 1

        fun stateToString(state: Int): String = when (state) {
            STATE_UNFEFINED -> "UNDEFINED"
            STATE_NORMAL -> "NORMAL"
            STATE_RUNNING -> "RUNNING"
            STATE_TOP -> "TOP"
            else -> "UNKNOWN"
        }

        fun dockTypeToString(dockType: Int): String = when (dockType) {
            DOCK_TYPE_UNDEFINED -> "UNDEFINED"
            DOCK_TYPE_PERSISIT -> "PERSIST"
            DOCK_TYPE_NORMAL -> "NORMAL"
            else -> "UNKNOWN"
        }

        fun platformTypeToString(platformType: Int): String = when (platformType) {
            PLATFORM_TYPE_ANDROID -> "ANDROID"
            PLATFORM_TYPE_X11 -> "X11"
            else -> "UNKNOWN"
        }
    }

    var id = ID_UNDEFINED
    private var baseActivityComponentName: ComponentName? = null
    private var realActivityComponentName: ComponentName? = null
    //    var packageName: String? = null
//    var name: String? = null
    var icon: Drawable? = null
    var label:String ? = null
    private var state:Int  = STATE_UNFEFINED
    var action:String ? = null
    var componentName: ComponentName? = null
    var dockType = DOCK_TYPE_UNDEFINED
    var platformType = PLATFORM_TYPE_ANDROID

    fun getState():Int {
        return state
    }

    fun setState(state: Int){
        this.state = state
    }

    fun unTopState(){
        if(this.state == STATE_TOP){
            setState(STATE_RUNNING)
        }
    }

    fun setState(state: Int, list :MutableList<TaskInfo>){
        if(state == STATE_TOP){
            list.forEach { info ->
                if (!isBatchTaskInfo(info)){
                    info.unTopState()
                }
            }
        }
        setState(state)
        Log.d("DockAppAdapter", "setState: $this")
    }

    fun isBatchTaskInfo(info: TaskInfo?):Boolean{
        if(info == null){
            return false
        }
        if(id != ID_UNDEFINED && id == info.id){
            return true
        }
        return TextUtils.equals(program, info.program)
                && TextUtils.equals(packageName, info.packageName)
    }

    fun setBaseActivityComponentName(baseActivityComponentName: ComponentName?) {
        this.baseActivityComponentName = baseActivityComponentName
    }

    fun setRealActivityComponentName(realActivityComponentName: ComponentName?) {
        this.realActivityComponentName = realActivityComponentName
    }

    override fun equals(other: Any?): Boolean {
        if (other !is TaskInfo) {
            return false
        }
        // The task id is unique in system.
        return id == other.id
    }

    override fun hashCode(): Int {
        return packageName.hashCode() + program.hashCode()
    }

    override fun toString(): String {
        return """
            |id=$id
            |baseActivityComponentName=$baseActivityComponentName
            |realActivityComponentName=$realActivityComponentName
            |packageName=$packageName
            |icon=${if(icon != null)"Drawable@${Integer.toHexString(icon.hashCode())}" else "null"}
            |label=$label
            |state=${stateToString(state)}
            |action=$action
            |componentName=$componentName
            |program=$program
            |dockType=${dockTypeToString(dockType)}
            |platformType=${platformTypeToString(platformType)}
        """.trimMargin()
    }

    fun isPersist(): Boolean {
        return dockType == DOCK_TYPE_PERSISIT
    }

    fun finshTask() {
        setState(STATE_NORMAL)
        id = ID_UNDEFINED
    }

}
