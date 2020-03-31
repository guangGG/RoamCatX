package gapp.season.filemanager

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.text.TextUtils
import gapp.season.util.file.FileUtil
import org.json.JSONArray
import java.io.File

//记录：收藏夹、历史记录、打开方式
@SuppressLint("DefaultLocale")
object FileManagerBuffer {
    private const val MAX_BUFFER_SIZE = 32
    var fmHistory: MutableList<String>? = null
    private var fmHistoryFile: File? = null
    var fmFavorites: MutableList<String>? = null
    private var fmFavoriteFile: File? = null

    internal fun init() {
        fmHistory = mutableListOf()
        fmHistoryFile = File(FileManager.configDir, "fm_history.json")
        fmFavorites = mutableListOf()
        fmFavoriteFile = File(FileManager.configDir, "fm_favorite.json")
        try {
            val contentFavor = FileUtil.getFileContent(fmFavoriteFile, null)
            if (!TextUtils.isEmpty(contentFavor)) {
                val jArray = JSONArray(contentFavor)
                for (i in 0 until jArray.length()) {
                    fmFavorites?.add(jArray.getString(i))
                }
            }
            val contentHistory = FileUtil.getFileContent(fmHistoryFile, null)
            if (!TextUtils.isEmpty(contentHistory)) {
                val jArray = JSONArray(contentHistory)
                for (i in 0 until jArray.length()) {
                    fmHistory?.add(jArray.getString(i))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun putHistory(path: String): Boolean {
        if (TextUtils.isEmpty(path)) {
            return false
        }
        //移除重复记录
        fmHistory?.remove(path)
        fmHistory?.add(0, path)
        while (fmHistory?.size ?: 0 > MAX_BUFFER_SIZE) {
            fmHistory?.removeAt(MAX_BUFFER_SIZE)
        }
        saveToFile(false)
        return true
    }

    fun removeHistory(path: String): Boolean {
        if (TextUtils.isEmpty(path)) {
            return false
        }
        val b = fmHistory?.remove(path) ?: false
        saveToFile(false)
        return b
    }

    fun clearHistory() {
        fmHistory?.clear()
        saveToFile(false)
    }

    fun putFavorite(path: String): Boolean {
        if (TextUtils.isEmpty(path)) {
            return false
        }
        //移除重复记录
        fmFavorites?.remove(path)
        fmFavorites?.add(0, path)
        while (fmFavorites?.size ?: 0 > MAX_BUFFER_SIZE) {
            fmFavorites?.removeAt(MAX_BUFFER_SIZE)
        }
        saveToFile(true)
        return true
    }

    fun removeFavorite(path: String): Boolean {
        if (TextUtils.isEmpty(path)) {
            return false
        }
        val b = fmFavorites?.remove(path) ?: false
        saveToFile(true)
        return b
    }

    fun clearFavorite() {
        fmFavorites?.clear()
        saveToFile(true)
    }

    private fun saveToFile(favor: Boolean) {
        try {
            val jArray = JSONArray()
            if (favor) {
                fmFavorites?.forEach {
                    jArray.put(it)
                }
                FileUtil.saveToFile(fmFavoriteFile, jArray.toString(), null)
            } else {
                fmHistory?.forEach {
                    jArray.put(it)
                }
                FileUtil.saveToFile(fmHistoryFile, jArray.toString(), null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }


    private const val SP_FILE_NAME = "FileManagerSuffix"
    private const val IS_OPEN_FILE_MINE = "open_file_mine" //是否用自带工具浏览文件

    fun isOpenFileMine(context: Context?): Boolean {
        return context?.getSharedPreferences(SP_FILE_NAME, Activity.MODE_PRIVATE)
                ?.getBoolean(IS_OPEN_FILE_MINE, false) ?: false
    }

    fun setOpenFileMine(context: Context?, isOpenFileInMine: Boolean) {
        context?.getSharedPreferences(SP_FILE_NAME, Activity.MODE_PRIVATE)
                ?.edit()?.putBoolean(IS_OPEN_FILE_MINE, isOpenFileInMine)?.apply()
    }

    /**
     * 设置默认打开方式
     * @param suffix 文件后缀类型(不为空)
     * @param pkg  默认打开应用包名
     * @param cls 默认打开应用类名
     */
    fun setOpenManner(context: Context, suffix: String, pkg: String, cls: String) {
        if (TextUtils.isEmpty(suffix) || TextUtils.isEmpty(pkg) || TextUtils.isEmpty(cls))
            return
        val fileSuffix = suffix.toLowerCase()// 字母以小写形式记录
        val editor = context.getSharedPreferences(SP_FILE_NAME, Context.MODE_PRIVATE).edit()
        editor.putString(fileSuffix + "__pkg", pkg)
        editor.putString(fileSuffix + "__cls", cls)
        editor.apply()
    }

    /**
     * 获取默认打开方式-应用包名
     */
    fun getOpenMannerPkg(context: Context, suffix: String): String? {
        if (TextUtils.isEmpty(suffix))
            return null
        val fileSuffix = suffix.toLowerCase()// 字母以小写形式记录
        val sp = context.getSharedPreferences(SP_FILE_NAME, Context.MODE_PRIVATE)
        return sp.getString(fileSuffix + "__pkg", null)
    }

    /**
     * 获取默认打开方式-应用类名
     */
    fun getOpenMannerCls(context: Context, suffix: String): String? {
        if (TextUtils.isEmpty(suffix))
            return null
        val fileSuffix = suffix.toLowerCase()// 字母以小写形式记录
        val sp = context.getSharedPreferences(SP_FILE_NAME, Context.MODE_PRIVATE)
        return sp.getString(fileSuffix + "__cls", null)
    }

    fun removeOpenManner(context: Context, suffix: String) {
        val fileSuffix = suffix.toLowerCase()// 字母以小写形式记录
        val editor = context.getSharedPreferences(SP_FILE_NAME, Context.MODE_PRIVATE).edit()
        editor.remove(fileSuffix + "__pkg")
        editor.remove(fileSuffix + "__cls")
        editor.apply()
    }

    fun clearOpenManner(context: Context) {
        context.getSharedPreferences(SP_FILE_NAME, Context.MODE_PRIVATE).edit().clear().apply()
    }
}
