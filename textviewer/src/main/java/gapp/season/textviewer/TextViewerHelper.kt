package gapp.season.textviewer

import android.app.Activity
import android.content.Context
import android.content.Intent
import java.io.File

/**
 * TextViewer用于浏览文件的文本内容(不限于文本类型文件，可浏览各种类型的文件的指定编码的文本内容)，
 * 采用按一定字节数量分页加载，因此可以浏览非常大的文件而不用担心对应用内存开销造成负担。
 * TextViewer仅用于帮助了解文件的内容概要，加载速度会很快，但功能并不全面，阅读txt书籍仍推荐使用读书(BookReader)功能。
 */
object TextViewerHelper {
    private const val TEXT_SUFFIX = ".txt,.html,.xml,.json,.log,.gradle,.properties,.ini,.md,.js,.css,.yml"
    const val INTENT_FILE_PATH = "intent_file_path"

    /**
     * 浏览上次看的文件
     */
    fun show(context: Context) {
        show(context, null)
    }

    /**
     * 浏览文件文本内容
     */
    fun show(context: Context, filePath: String?) {
        val intent = Intent(context, TextViewerActivity::class.java)
        intent.putExtra(INTENT_FILE_PATH, filePath)
        if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /**
     * 判断文件类型是否支持浏览文本内容
     */
    fun isTextFile(file: File?): Boolean {
        if (file?.exists() == true) {
            val extension = "." + file.extension
            return extension.length > 1 && TEXT_SUFFIX.contains(extension, true)
        }
        return false
    }

    //记录最后一次浏览的文件
    fun savePath(context: Context?, path: String?) {
        context?.getSharedPreferences("TextViewer", Context.MODE_PRIVATE)?.edit()?.putString("last_show_file", path)?.apply()
    }

    fun getSavedPath(context: Context?): String? {
        return context?.getSharedPreferences("TextViewer", Context.MODE_PRIVATE)?.getString("last_show_file", null)
    }
}
