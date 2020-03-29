package gapp.season.filemanager

import android.content.Context
import gapp.season.util.log.LogUtil
import gapp.season.util.tips.ToastUtil

//TODO 管理文件(path为文件夹则进入文件夹，为文件则进入父文件夹并定位到此文件，为null则打开管理器主页)
object FileManager {
    fun enter(context: Context, path: String? = null) {
        LogUtil.d("进入文件管理：$path")
        ToastUtil.showShort("敬请期待")
    }

    fun getFavorList(): List<String> {
        val list: MutableList<String> = mutableListOf()
        list.add("/sdcard")
        return list
    }
}
