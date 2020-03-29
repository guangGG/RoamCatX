package gapp.season.musicplayer

import android.content.Context
import android.text.TextUtils
import gapp.season.util.file.FileUtil
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.*

/**
 * 播放历史记录及上次播放配置
 */
object MusicHistoryBuffer {
    private const val MAX_BUFFER_SIZE = 16
    private var mHistory: MutableList<String>? = null
    private var mHistoryFile: File? = null

    val historys: List<String>?
        get() = mHistory

    init {
        // 读取历史记录
        mHistory = LinkedList()
        try {
            mHistoryFile = File(MusicPlayerHelper.context!!.filesDir, "music_history.json")
            val content = FileUtil.getFileContent(mHistoryFile!!, "UTF-8")
            if (!TextUtils.isEmpty(content)) {
                val jArray = JSONArray(content)
                for (i in 0 until jArray.length()) {
                    mHistory!!.add(jArray.getString(i))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveToFile() {
        try {
            val jArray = JSONArray()
            for (s in mHistory!!) {
                jArray.put(s)
            }
            val content = jArray.toString()
            FileUtil.saveToFile(mHistoryFile!!, content, "UTF-8")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun put(path: String): Boolean {
        if (TextUtils.isEmpty(path)) {
            return false
        }
        // 移除重复记录后
        mHistory!!.remove(path)
        mHistory!!.add(path)
        while (mHistory!!.size > MAX_BUFFER_SIZE) {
            mHistory!!.removeAt(0)
        }
        saveToFile()
        return true
    }

    fun remove(path: String): Boolean {
        if (TextUtils.isEmpty(path)) {
            return false
        }
        val b = mHistory!!.remove(path)
        saveToFile()
        return b
    }

    fun clear() {
        mHistory!!.clear()
        saveToFile()
    }


    //标记上次播放列表

    fun mark(musicList: MusicFileResolver.MusicFileList) {
        try {
            if (!musicList.list.isNullOrEmpty()) {
                val jObj = JSONObject()
                val jArray = JSONArray()
                musicList.list!!.forEach {
                    jArray.put(it)
                }
                jObj.put("list", jArray)
                jObj.put("index", musicList.index)
                val markFile = File(MusicPlayerHelper.context!!.filesDir, "music_mark.json")
                val content = jObj.toString()
                FileUtil.saveToFile(markFile, content, "UTF-8")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun fetch(): MusicFileResolver.MusicFileList? {
        try {
            val markFile = File(MusicPlayerHelper.context!!.filesDir, "music_mark.json")
            val content = FileUtil.getFileContent(markFile, "UTF-8")
            if (!TextUtils.isEmpty(content)) {
                val jObj = JSONObject(content)
                val index = jObj.optInt("index", 0)
                val jArray = jObj.optJSONArray("list")
                if (jArray != null) {
                    val list = mutableListOf<String>()
                    for (i in 0 until jArray.length()) {
                        list.add(jArray[i] as String)
                    }
                    return MusicFileResolver.MusicFileList(list, index)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }


    //标记上次播放配置

    fun setPlayMode(mode: Int) {
        MusicPlayerHelper.context?.getSharedPreferences("MusicPlayer", Context.MODE_PRIVATE)?.edit()?.putInt("last_play_mode", mode)?.apply()
    }

    fun setShowMode(mode: Int) {
        MusicPlayerHelper.context?.getSharedPreferences("MusicPlayer", Context.MODE_PRIVATE)?.edit()?.putInt("last_show_mode", mode)?.apply()
    }

    fun getPlayMode(): Int {
        return MusicPlayerHelper.context?.getSharedPreferences("MusicPlayer",
                Context.MODE_PRIVATE)?.getInt("last_play_mode", 0) ?: 0
    }

    fun getShowMode(): Int {
        return MusicPlayerHelper.context?.getSharedPreferences("MusicPlayer",
                Context.MODE_PRIVATE)?.getInt("last_show_mode", 0) ?: 0
    }
}
