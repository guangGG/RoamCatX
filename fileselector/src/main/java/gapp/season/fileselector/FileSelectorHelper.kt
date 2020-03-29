package gapp.season.fileselector

import android.app.Activity
import android.content.Context
import android.content.Intent
import java.io.File

object FileSelectorHelper {
    var listener: FileSelectorListener? = null

    fun selectFile(context: Context, requestCode: Int): Boolean {
        return selectFile(context, requestCode, false, null)
    }

    fun selectFile(context: Context, requestCode: Int, allowDir: Boolean, path: String?): Boolean {
        val intent = Intent(context, FileSelectorActivity::class.java)
        if (context is Activity) {
            intent.putExtra(FileSelectorActivity.EXTRA_ALLOW_DIR, allowDir)
            intent.putExtra(FileSelectorActivity.EXTRA_FILE_PATH, path)
            context.startActivityForResult(intent, requestCode)
            return true
        }
        return false
    }

    fun saveFile(context: Context, file: File) {
        context.getSharedPreferences("FileSelector", Context.MODE_PRIVATE).edit().putString("last_select_file", file.absolutePath).apply()
    }

    fun getSavedFile(context: Context): String? {
        return context.getSharedPreferences("FileSelector", Context.MODE_PRIVATE).getString("last_select_file", null)
    }

    interface FileSelectorListener {
        fun onClickFile(activity: Activity, file: File)
    }
}
