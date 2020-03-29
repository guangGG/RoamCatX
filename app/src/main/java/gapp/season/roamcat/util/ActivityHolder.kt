package gapp.season.roamcat.util

import android.annotation.SuppressLint
import android.app.Activity
import android.util.Log
import gapp.season.roamcat.BuildConfig

@Deprecated("use gapp.season.util.app.ActivityHolder instead")
@SuppressLint("LogNotTimber")
object ActivityHolder {
    private val TAG = ActivityHolder::class.java.simpleName
    private val printLog = BuildConfig.DEV

    private var activityList: MutableList<Activity> = mutableListOf()

    /**
     * 获得当前任务栈activity列表（用于手动管理任务栈）
     */
    fun getActivityList(): List<Activity> {
        return activityList
    }

    /**
     * 添加activity
     */
    fun addActivity(activity: Activity?) {
        if (activity != null) {
            if (activityList.contains(activity)) {
                activityList.remove(activity)
                activityList.add(activity)
            } else {
                activityList.add(activity)
            }
            if (printLog) Log.d(TAG, "addActivity:" + activity + ";activityList.size=" + activityList.size)
        }
    }

    /**
     * 从列表中移除要关闭的activity
     */
    fun removeActivity(activity: Activity?) {
        if (activity != null) {
            val isRemoved = activityList.remove(activity)
            if (printLog) Log.d(TAG, "removeActivity:" + activity + "(" + isRemoved + ");activityList.size=" + activityList.size)
        }
    }

    /**
     * 关闭所有的Activity和Service
     */
    fun finishAllActivity() {
        if (printLog) Log.d(TAG, "finishAllActivity: size=" + activityList.size)
        activityList.forEach {
            it.finish()
        }
    }

    /**
     * 获得当前的activity
     */
    fun getCurrentActivity(): Activity? {
        if (activityList.isNotEmpty()) {
            return activityList[activityList.size - 1]
        }
        return null
    }

    /**
     * 判断应用是否有activity
     */
    fun hasActivity(): Boolean {
        return activityList.isNotEmpty()
    }

    /**
     * 判断activity是否已经添加到列表中
     */
    operator fun contains(activity: Activity): Boolean {
        return activityList.contains(activity)
    }

    /**
     * 判断列表中是否含有该类型的activity
     */
    operator fun contains(clazz: Class<*>): Boolean {
        activityList.forEach {
            if (it.javaClass == clazz) {
                return true
            }
        }
        return false
    }
}