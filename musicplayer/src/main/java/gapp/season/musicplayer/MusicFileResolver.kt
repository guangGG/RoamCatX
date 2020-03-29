package gapp.season.musicplayer

import android.text.TextUtils
import gapp.season.util.file.FileUtil
import gapp.season.util.text.StringUtil
import org.json.JSONObject
import java.io.File
import java.util.ArrayList
import java.util.regex.Pattern
import kotlin.Comparator

/**
 * 解析音乐文件
 */
object MusicFileResolver {
    //耗时操作，需在子线程中进行
    fun getPlayList(path: String?): MusicFileList? {
        if (!path.isNullOrEmpty()) {
            val file = File(path)
            if (file.exists()) {
                //兼容".musics"文件
                if (path.endsWith(MusicPlayerHelper.MUSIC_LIST_SUFFIX)) {
                    val musics = parseMusicsFile(file)
                    if (musics != null) {
                        MusicHistoryBuffer.put(path)
                        MusicPlayerHelper.log("加载音乐列表成功：${musics.list?.size} $path")
                        return musics
                    }
                    return null
                }
                val parentFile = file.parentFile
                if (parentFile?.exists() == true) {
                    val array = parentFile.listFiles { f -> MusicPlayerHelper.isMusicFile(f, false) }
                    if (!array.isNullOrEmpty()) {
                        val list: MutableList<String> = mutableListOf()
                        array.forEach {
                            list.add(it.absolutePath)
                        }
                        list.sortWith(Comparator { p0, p1 -> StringUtil.compare(p0, p1, "GBK") })
                        var index = 0
                        list.forEachIndexed { i, it ->
                            run {
                                if (it == path) {
                                    index = i
                                    return@forEachIndexed
                                }
                            }
                        }
                        MusicHistoryBuffer.put(path)
                        MusicPlayerHelper.log("加载音乐列表成功：${list.size} $path")
                        val musicList = MusicFileList(list, index)
                        MusicHistoryBuffer.mark(musicList)
                        return musicList
                    }
                }
            }
        }
        return MusicHistoryBuffer.fetch()
    }

    private fun parseMusicsFile(musicsFile: File): MusicFileList? {
        val list = ArrayList<String>()
        try {
            if (!musicsFile.exists() || musicsFile.isDirectory) {
                return null
            }
            val dirPath = (musicsFile.parent ?: "") + "/"
            var musicDir: String? = null
            var content = FileUtil.getFileContent(musicsFile, FileUtil.getFileCharset(musicsFile))
            if (!TextUtils.isEmpty(content)) {
                // 去掉UTF-8编码头信息(utf8:0xEFBBBF,15711167; gbk:63; unicode:65279)
                if (content[0].toInt() == 65279 && content.length > 1) {
                    content = content.substring(1)
                }
                // 添加列表中的音乐
                val names = content.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()// 以换行符为分隔符
                for (i in names.indices) {
                    var name = names[i]
                    if (!TextUtils.isEmpty(name)) {
                        name = name.trim { it <= ' ' }
                        // 使用相对路径：格式：A/B/c.file 或 c.file 或 @{"path":"A/B/c.mp3","size":"3402344","cid":"xxx"}
                        var file: File? = null
                        if (name.startsWith("#")) {
                            if (i == 0) {
                                //...musicdir=${musicDir};...
                                val pattern = ".*musicdir=([^;]*);.*" //可能把文件名中的";"符号误判
                                val matcher = Pattern.compile(pattern).matcher(name)
                                if (matcher.find()) {
                                    //matcher.group()为元文本信息，matcher.group(1)为匹配到的信息
                                    musicDir = matcher.group(1)
                                    MusicPlayerHelper.log("解析音乐目录成功：$musicDir")
                                }
                            }
                            continue
                        } else if (!TextUtils.isEmpty(musicDir) && name.startsWith("@{")) {
                            try {
                                val obj = JSONObject(name.substring(1))
                                val path = obj.optString("path")
                                val size = obj.optInt("size").toLong()
                                //val cid = obj.optString("cid")
                                if (!TextUtils.isEmpty(path)) {
                                    file = File(musicDir!!, path)
                                }
                                if (file == null || !file.exists() || file.length() != size) {
                                    file = null
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }

                        } else {
                            file = File(dirPath, name)
                        }
                        if (file != null && file.exists() && !file.isDirectory) {
                            list.add(file.absolutePath)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return if (list.isNullOrEmpty()) {
            null
        } else {
            MusicFileList(list, 0)
        }
    }

    class MusicFileList(internal var list: List<String>?, internal var index: Int)
}
