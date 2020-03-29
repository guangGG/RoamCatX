package gapp.season.roamcat.util

@Deprecated("use gapp.season.util.task.OnTaskDone instead")
interface OnTaskListener<T> {
    companion object {
        //基本状态码
        val CODE_FAIL = -1
        val CODE_SUCCESS = 0
        //扩展状态码
        val CODE_STATE_1 = 1
        val CODE_STATE_2 = 2
        val CODE_STATE_3 = 3
        val CODE_STATE_4 = 4
        val CODE_STATE_5 = 5
        val CODE_STATE_6 = 6
        val CODE_STATE_7 = 7
        val CODE_STATE_8 = 8
        val CODE_STATE_9 = 9
    }

    fun onTaskDone(code: Int, msg: String?, data: T?)
}
