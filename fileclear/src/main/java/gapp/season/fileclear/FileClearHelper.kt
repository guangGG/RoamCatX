package gapp.season.fileclear

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.os.Handler
import android.os.Looper
import gapp.season.util.file.FileUtil
import gapp.season.util.task.OnTaskDone
import gapp.season.util.task.ThreadPoolExecutor
import org.json.JSONArray
import java.io.File

object FileClearHelper {
    private var configDir: String? = null
    private var listener: ClearListener? = null

    fun init(configDir: String, listener: ClearListener) {
        this.configDir = configDir
        this.listener = listener
    }

    fun openPage(context: Context) {
        val intent = Intent(context, FileClearActivity::class.java)
        if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    internal fun doOpenFile(activity: Activity, item: File) {
        listener?.onClickFile(activity, item)
    }

    internal fun saveClearList(clearList: MutableList<String>?) {
        try {
            val file = File(configDir, "clear_file_config.json")
            val jArray = JSONArray()
            clearList?.forEach {
                jArray.put(it)
            }
            FileUtil.saveToFile(file, jArray.toString(), null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    internal fun getClearList(): MutableList<String> {
        val list = mutableListOf<String>()
        try {
            val file = File(configDir, "clear_file_config.json")
            val content = FileUtil.getFileContent(file, null)
            if (!content.isNullOrBlank()) {
                val jArray = JSONArray(content)
                for (index in 0 until jArray.length()) {
                    list.add(jArray.optString(index, ""))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    internal fun clearList(clearList: MutableList<String>?, onTaskDone: OnTaskDone<Boolean>?) {
        ThreadPoolExecutor.getInstance().execute {
            var ret = true
            try {
                val sdCard = Environment.getExternalStorageDirectory()
                clearList?.forEach {
                    FileUtil.deleteFile(File(sdCard, it))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                ret = false
            }
            Handler(Looper.getMainLooper()).post {
                onTaskDone?.onTaskDone(0, null, ret)
            }
        }
    }

    interface ClearListener {
        fun onClickFile(activity: Activity, file: File)
    }
}
