package gapp.season.musicplayer

import android.graphics.Color
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import java.text.DecimalFormat
import java.util.*
import java.util.regex.Pattern

/**
 * 解析Lyric格式文件
 */
object MusicLyricResolver {
    private var mTagMusic: String? = null
    private var mTagLrc: String? = null
    private var mTagLyric: List<LyricLine>? = null
    private var mTagOffset: Int = 0

    fun initFormatLyric(music: String, lyric: String) {
        if (mTagMusic != music || mTagLrc != lyric) {
            mTagMusic = music
            mTagLrc = lyric
            /*lrc歌词文本中含有两类标签：
            1、标识标签（ID-tags）---这里暂不处理
            [ar:艺人名]
            [ti:曲名]
            [al:专辑名]
            [by:编者（指编辑LRC歌词的人）]
            [offset:时间补偿值] 其单位是毫秒，正值表示整体提前，负值相反
            2、 时间标签（Time-tag）
            标准格式： [分钟:秒.毫秒] 歌词*/
            if (!TextUtils.isEmpty(lyric)) {
                try {
                    var offset = 0
                    val list = ArrayList<LyricLine>()
                    val regexOffset = "\\[offset:(-?\\d+)\\]"//减号在[]中时需要转义为"\\-"
                    val regularExpression = "\\[(\\d{1,2}):(\\d{1,2}).(\\d{1,3})\\]"
                    val pattern1 = Pattern.compile(regexOffset) // 创建 Pattern 对象
                    val pattern = Pattern.compile(regularExpression) // 创建 Pattern 对象
                    val lines = lyric.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    val times = HashSet<Int>()
                    var text: String
                    for (lineStr in lines) {
                        if (TextUtils.isEmpty(lineStr)) {
                            continue
                        }
                        val matcher = pattern.matcher(lineStr)
                        //一个句子中可能有多个时间点
                        times.clear()
                        text = ""
                        while (matcher.find()) {
                            try {
                                //m.group(0)-->[02:34.94] ----对应---> [分钟:秒.毫秒]
                                val min = matcher.group(1) // 分钟
                                val sec = matcher.group(2) // 秒
                                val mill = matcher.group(3) // 毫秒，注意这里如果是两位数还要乘以10
                                times.add(getLongTime(min, sec, mill))
                                // 获取当前时间的歌词信息(选用最后一个正则匹配的内容)
                                text = lineStr.substring(matcher.end())
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }

                        }
                        if (times.size > 0) {
                            for (time in times) {
                                list.add(LyricLine(time, text))
                            }
                        } else {
                            //处理[offset:时间补偿值]
                            try {
                                val matcher1 = pattern1.matcher(lineStr)
                                if (matcher1.find()) {
                                    val offsetStr = matcher1.group(1)//offset值
                                    offset = Integer.parseInt(offsetStr)
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }

                        }
                    }
                    if (list.size > 5) {
                        list.sortWith(Comparator { lhs, rhs -> lhs.time - rhs.time })
                        mTagLyric = list
                        mTagOffset = offset
                        return
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            mTagLyric = null
            mTagOffset = 0
        }
    }

    fun getLyricSchedule(music: String, lyric: String, time: Int): SpannableStringBuilder {
        val sb = SpannableStringBuilder()
        if (TextUtils.equals(music, mTagMusic) && mTagLyric != null && mTagLyric!!.isNotEmpty()) {
            val offStr = if (mTagOffset == 0) "" else "[offset:" + mTagOffset + "ms]"
            sb.append("歌词$offStr：\n")
            val lines = ArrayList<String>()
            var currentLine = -1
            for (i in mTagLyric!!.indices) {
                val lineTime = mTagLyric!![i].time - mTagOffset
                val lineString = "[" + toMinuteForm(lineTime) + "] " + mTagLyric!![i].content + "\n"
                lines.add(lineString)
                if (time > 0 && time > lineTime) {
                    currentLine = i
                }
            }
            for (j in lines.indices) {
                if (currentLine == j) {
                    //高亮展示
                    val line = SpannableString(lines[j])
                    line.setSpan(ForegroundColorSpan(Color.BLUE), 0, line.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    sb.append(line)
                } else {
                    sb.append(lines[j])
                }
            }
            //补一空行(可以优化居中对齐方式显示)
            sb.append("\u3000\u3000\u3000\u3000\u3000\u3000\u3000\u3000\u3000\u3000" +
                    "\u3000\u3000\u3000\u3000\u3000\u3000\u3000\u3000\u3000\u3000" +
                    "\u3000\u3000\u3000\u3000\u3000\u3000\u3000\u3000\u3000\u3000")
        } else {
            sb.append(lyric)
        }
        return sb
    }

    private fun getLongTime(min: String, sec: String, mill: String): Int {
        try {
            // 转成整型
            val m = Integer.parseInt(min)
            val s = Integer.parseInt(sec)
            val cs: Int
            if (mill.length > 2) {
                cs = Integer.parseInt(mill.substring(0, 2))
            } else {
                cs = Integer.parseInt(mill)//这里还要乘以10才是毫秒
            }
            /*if (s >= 60) {
				System.out.println("警告: 出现了一个时间不正确的项 --> [" + min + ":" + sec + "." + mill.substring(0, 2) + "]");
			}*/
            // 组合成一个长整型表示的以毫秒为单位的时间
            return m * 60 * 1000 + s * 1000 + cs * 10
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return 0
    }

    fun toMinuteForm(ms: Int): String {
        var millisecond = ms
        millisecond = if (millisecond > 0) millisecond else 0
        val second = (millisecond + 500) / 1000
        val simpleSecond = DecimalFormat("00").format((second % 60).toLong())
        return (second / 60).toString() + ":" + simpleSecond
    }

    class LyricLine(internal var time: Int, internal var content: String)
}
