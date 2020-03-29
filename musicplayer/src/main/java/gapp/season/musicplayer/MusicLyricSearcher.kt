package gapp.season.musicplayer

import android.text.SpannableStringBuilder
import gapp.season.util.file.FileUtil
import gapp.season.util.text.StringUtil
import java.io.File
import java.io.FileFilter

object MusicLyricSearcher {
    private const val LYRIC_SUFFIX = ".lrc"
    private val LYRIC_DIR_NAMES = arrayOf("lyric", "_lyric", "lyrics", "_lyrics", "libretto", "librettos", "lrc", "_lrc", "geci", "gc", "歌词", "_lrc歌词")
    private val lyricFileFilter = FileFilter { file ->
        file.absolutePath.endsWith(LYRIC_SUFFIX, ignoreCase = true)
    }
    private val lyrics: MutableMap<String, String> = mutableMapOf()

    private fun getLyric(musicPath: String): String {
        if (lyrics[musicPath] != null) {
            return lyrics[musicPath]!!
        } else {
            //比较耗时的操作
            var lrc = "未匹配到歌词文件"
            try {
                val lrcDir = File(MusicPlayerHelper.musicDir, "/lyric")
                if (!lrcDir.exists()) {
                    lrcDir.mkdirs()
                } else if (!lrcDir.isDirectory) {
                    lrcDir.delete()
                    lrcDir.mkdirs()
                }
                val lrcList = mutableListOf<File>()
                // 默认目录
                val fs1 = lrcDir.listFiles(lyricFileFilter)
                fs1?.forEach {
                    lrcList.add(it)
                }
                // 当前目录
                val music = File(musicPath)
                val fs2 = music.parentFile?.listFiles(lyricFileFilter)
                fs2?.forEach {
                    lrcList.add(it)
                }
                // 当前目录下的歌词目录
                val listFiles = music.parentFile?.listFiles()
                if (listFiles != null) {
                    for (dirFile in listFiles) {
                        if (dirFile.isDirectory) {
                            val dirName = dirFile.name.toLowerCase()
                            for (lrcName in LYRIC_DIR_NAMES) {
                                if (lrcName == dirName) {
                                    val fs3 = dirFile.listFiles(lyricFileFilter)
                                    fs3?.forEach {
                                        lrcList.add(it)
                                    }
                                }
                            }
                        }
                    }
                }
                if (lrcList.isNotEmpty()) {
                    var simpleMusicName = FileUtil.getFileNameWithoutExtName(musicPath)
                    simpleMusicName = StringUtil.removeNeedlessBlank(simpleMusicName)
                    val lrcName = simpleMusicName + LYRIC_SUFFIX
                    for (i in lrcList.indices) {
                        if (lrcName.equals(StringUtil.removeNeedlessBlank(lrcList[i].name), ignoreCase = true)) {
                            // 匹配到歌词文件
                            lrc = FileUtil.getFileContent(lrcList[i], FileUtil.getFileCharset(lrcList[i]))
                            return lrc
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return lrc
        }
    }

    /**
     * 获取音乐指定播放位置处的歌词
     */
    fun showLyric(musicPath: String, timeMs: Int): SpannableStringBuilder {
        val lrc = getLyric(musicPath)
        MusicLyricResolver.initFormatLyric(musicPath, lrc)
        return MusicLyricResolver.getLyricSchedule(musicPath, lrc, timeMs)
    }
}
