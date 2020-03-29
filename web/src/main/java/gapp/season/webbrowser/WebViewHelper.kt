package gapp.season.webbrowser

import android.content.Context
import android.os.Build
import android.text.TextUtils
import android.webkit.WebView
import com.google.gson.Gson
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import gapp.season.util.file.FileUtil
import gapp.season.util.task.OnTaskDone
import java.io.File
import java.util.*

object WebViewHelper {
    const val HOME_PAGE: String = "file:///android_asset/web_home.html"
    const val EVENT_ON_CREATE: Int = 1
    const val EVENT_ON_DESTROY: Int = 2


    val marks = mutableListOf<Record>()
    val history = mutableListOf<Record>()
    var config: Config? = null
    fun init(config: Config) {
        WebViewHelper.config = config
        readMarks(config)
    }

    fun showWebPage(context: Context, url: String?) {
        WebActivity.navigation(context, url)
    }

    fun readMarks(config: Config) {
        val marksData = FileUtil.readFile(File(config.getBaseDir(), "/webview/web_marks.json"))
        val historyData = FileUtil.readFile(File(config.getBaseDir(), "/webview/web_history.json"))
        val type = object : TypeToken<List<Record>>() {
        }.type
        marks.clear()
        if (!marksData.isNullOrEmpty()) marks.addAll(Gson().fromJson(marksData, type))
        history.clear()
        if (!historyData.isNullOrEmpty()) history.addAll(Gson().fromJson(historyData, type))
    }

    fun saveMarks() {
        val data = Gson().toJson(marks)
        FileUtil.saveToFile(File(config?.getBaseDir(), "/webview/web_marks.json"), data, FileUtil.UTF_8)
    }

    fun saveHistory() {
        val data = Gson().toJson(history)
        FileUtil.saveToFile(File(config?.getBaseDir(), "/webview/web_history.json"), data, FileUtil.UTF_8)
    }

    //去广告脚本
    fun removeAd(webView: WebView, url: String?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && !config?.getRemoveAdUrl().isNullOrEmpty()) {
            val removeAdUrl = config?.getRemoveAdUrl() ?: ""
            if (url?.startsWith("http") == true && removeAdUrl != url) {
                val sniffText = "var s = document.createElement('script');\n" +
                        "s.charset='utf8';\n" +
                        "document.head.appendChild(s);\n" +
                        "s.src = '$removeAdUrl';"
                webView.evaluateJavascript(sniffText, null)//注入JS脚本
            }
        }
    }


    private var userAgent: String? = null
    /**
     * User-Agent:
     * Mozilla/5.0 (Linux; U; Android 5.0.2; zh-cn; Redmi Note 3 Build/LRX22G)
     * AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Mobile Safari/537.36
     */
    fun getUserAgent(): String? {
        if (TextUtils.isEmpty(userAgent)) {
            val webUserAgent = "Mozilla/5.0 (Linux; U; Android %s) " +
                    "AppleWebKit/533.1 (KHTML, like Gecko) Version/5.0 %sSafari/533.1"
            val locale = Locale.getDefault()
            val buffer = StringBuffer()
            // Add version
            val version = Build.VERSION.RELEASE
            if (version.isNotEmpty()) {
                buffer.append(version)
            } else {
                // default to "1.0"
                buffer.append("1.0")
            }
            buffer.append("; ")
            val language = locale.language
            if (language != null) {
                buffer.append(language.toLowerCase(locale))
                val country = locale.country
                if (!TextUtils.isEmpty(country)) {
                    buffer.append("-")
                    buffer.append(country.toLowerCase(locale))
                }
            } else {
                // default to "en"
                buffer.append("en")
            }
            // add the model for the release build
            if ("REL" == Build.VERSION.CODENAME) {
                val model = Build.MODEL
                if (model.isNotEmpty()) {
                    buffer.append("; ")
                    buffer.append(model)
                }
            }
            val id = Build.ID
            if (id.isNotEmpty()) {
                buffer.append(" Build/")
                buffer.append(id)
            }
            userAgent = String.format(webUserAgent, buffer, "Mobile ")
            return userAgent
        }
        return userAgent
    }

    class Record {
        @Expose
        @SerializedName("title")
        var title: String? = null
        @Expose
        @SerializedName("url")
        var url: String? = null

        constructor()

        constructor(title: String?, url: String?) {
            this.title = title
            this.url = url
        }

        fun showTitle(): String { //title为空时，标题显示url
            if (title.isNullOrBlank()) return (url ?: "--")
            return title!!
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Record

            if (url != other.url) return false

            return true
        }

        override fun hashCode(): Int {
            return url?.hashCode() ?: 0
        }
    }

    interface Config {
        fun getBaseDir(): String
        fun isDev(): Boolean
        fun getCallBack(): OnTaskDone<Any>?
        fun attachBaseContext(context: Context?): Context?
        fun getRemoveAdUrl(): String?
    }
}
