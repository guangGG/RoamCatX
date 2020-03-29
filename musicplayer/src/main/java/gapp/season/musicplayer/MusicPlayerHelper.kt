package gapp.season.musicplayer

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.os.Build
import gapp.season.util.log.LogUtil
import gapp.season.util.sys.MemoryUtil
import gapp.season.util.text.StringUtil
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

//音乐播放器
object MusicPlayerHelper {
    private const val TAG = "MusicPlayer"
    const val MUSIC_PLAYER_PAGE_ACTION = "gapp.season.musicplayer"
    private const val MUSIC_SUFFIX = ".mp3,.wma,.m4a,.wav,.aac,.amr,.ape,.flac"
    const val MUSIC_LIST_SUFFIX = ".musics"

    var context: Application? = null
    var musicDir: String? = null
    var listener: MusicPlayerListener? = null

    fun init(application: Application, musicDir: String) {
        context = application
        this.musicDir = musicDir
    }

    fun play(context: Context, filePath: String? = null) {
        val intent = Intent(context, MusicPlayerActivity::class.java)
        intent.putExtra("path", filePath)
        if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun isMusicFile(file: File?, includeList: Boolean = true): Boolean {
        if (file != null && file.exists() && file.isFile) {
            val extension = "." + file.extension
            val isMusic = extension.length > 1 && MUSIC_SUFFIX.contains(extension, true)
            return if (includeList) {
                isMusic || (extension.length > 1 && MUSIC_LIST_SUFFIX.contains(extension, true))
            } else {
                isMusic
            }
        }
        return false
    }

    @SuppressLint("DefaultLocale", "ObsoleteSdkInt")
    fun getAudioProperty(audio: File?): String {
        if (audio == null || !audio.exists()) {
            return "音乐不存在"
        }
        val sb = StringBuilder()
        sb.append("音乐路径：" + audio.absolutePath)
        sb.append("\n\n音乐大小：" + MemoryUtil.formatMemorySize(audio.length(), 2) + "(" + audio.length() + "B)")
        val sdf = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.CHINA)
        sb.append("\n修改时间：" + sdf.format(audio.lastModified()))
        if (Build.VERSION.SDK_INT >= 10) {
            val mmr = MediaMetadataRetriever()
            try {
                mmr.setDataSource(audio.absolutePath)
                val title = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: ""
                val album = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) ?: ""
                val mime = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE) ?: ""
                val artist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: ""
                // 播放时长单位为毫秒
                val durationStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                val duration = parseLong(durationStr)
                val date = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE) ?: ""
                sb.append("\n录制日期：$date")
                sb.append("\n音乐时长：" + StringUtil.getDurationString(duration))
                if (Build.VERSION.SDK_INT >= 14) {
                    val bitrateStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
                    val bitrate = parseLong(bitrateStr)
                    sb.append("\n音乐码率：" + MemoryUtil.formatMemorySize(bitrate, 2).toLowerCase() + "ps")
                }
                sb.append("\n音乐类型：$mime")
                sb.append("\n音乐标题：$title")
                sb.append("\n音乐专辑：$album")
                sb.append("\n音乐作者：$artist")
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try {
                    mmr.release()
                } catch (e: RuntimeException) {
                    e.printStackTrace()
                }

            }
        }
        return sb.toString()
    }

    private fun parseLong(longStr: String?): Long {
        return try {
            longStr?.toLong() ?: 0
        } catch (e: Exception) {
            0
        }
    }

    fun log(msg: String?) {
        if (!msg.isNullOrBlank()) LogUtil.d(TAG, msg)
    }

    interface MusicPlayerListener {
        //自定义方式导入文件
        fun import(activity: Activity, requestCode: Int): Boolean

        //自定义方式导入文件
        fun onImport(activity: Activity, data: Intent): String?
    }
}
