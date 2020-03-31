package gapp.season.filemanager

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import gapp.season.imageviewer.ImageViewerHelper
import gapp.season.musicplayer.MusicPlayerHelper
import gapp.season.textviewer.TextViewerHelper
import gapp.season.util.file.FileUtil
import gapp.season.util.tips.ToastUtil
import gapp.season.videoplayer.VideoPlayerHelper
import java.io.File

//手机文件管理推荐ES文件浏览器： http://www.estrongs.com/
object FileManager {
    const val DISKS_DIR_TAG = "存储卡列表"
    private var application: Application? = null
    internal var configDir: String? = null

    fun init(application: Application, configDir: String) {
        this.application = application
        this.configDir = configDir
        FileManagerBuffer.init()
    }

    fun enter(context: Context, path: String? = null) {
        val intent = Intent(context, FileManagerActivity::class.java)
        intent.putExtra("path", path)
        if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun getFavorList(): List<String> {
        val list: MutableList<String> = mutableListOf()
        FileManagerBuffer.fmFavorites?.forEach {
            list.add(it)
        }
        return list
    }

    @SuppressLint("SdCardPath")
    internal fun isTopDir(dir: File?): Boolean {
        if (dir?.exists() == true && dir.isDirectory) {
            if ("/" == dir.absolutePath || "/sdcard" == dir.absolutePath || "/sdcard/" == dir.absolutePath) return true
            val sdCards = FileUtil.getSdCards(application)
            sdCards.forEach {
                if (dir.absolutePath == it) {
                    return true
                }
            }
        }
        return false
    }

    fun openFileWithDefault(context: Context, file: File) {
        when {
            !file.exists() -> ToastUtil.showShort("文件打开失败：文件不存在")
            file.isDirectory -> {
                enter(context, file.absolutePath)
            }
            ImageViewerHelper.isImage(file) -> ImageViewerHelper.showImage(context, file.absolutePath)
            VideoPlayerHelper.isVideoFile(file) -> VideoPlayerHelper.play(context, file.absolutePath)
            MusicPlayerHelper.isMusicFile(file, false) -> MusicPlayerHelper.play(context, file.absolutePath)
            TextViewerHelper.isTextFile(file) -> TextViewerHelper.show(context, file.absolutePath)
            else -> TextViewerHelper.show(context, file.absolutePath)
        }
    }
}
