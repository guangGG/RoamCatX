package gapp.season.videoplayer

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Build
import android.widget.Toast
import com.danikula.videocache.ProxyCacheUtils
import com.shuyu.gsyvideoplayer.cache.CacheFactory
import com.shuyu.gsyvideoplayer.cache.ProxyCacheManager
import com.shuyu.gsyvideoplayer.player.IjkPlayerManager
import com.shuyu.gsyvideoplayer.player.PlayerFactory
import com.shuyu.gsyvideoplayer.utils.GSYVideoType
import tv.danmaku.ijk.media.player.IjkMediaPlayer
import java.io.File


object VideoPlayerHelper {
    private const val SP_VIDEO_PLAYER = "VideoPlayer"
    const val TYPE_FILE = 0
    const val TYPE_URL = 1
    private const val VIDEO_SUFFIX = ".3gp,.avi,.flv,.m4v,.mkv,.mov,.mp4,.rm,.rmvb,.swf,.xv,.3g2,.asf,.ask,.c3d,.dat,.divx,.dvr-ms,.f4v,.fli,.flx," +
            ".m2p,.m2t,.m2ts,.m2v,.mlv,.mpe,.mpeg,.mpg,.mpv,.mts,.ogm,.qt,.ra,.tp,.trp,.ts,.uis,.uisx,.uvp,.vob,.vsp,.webm,.wmv,.wmvhd,.wtv,.xvid"

    private var app: Application? = null
    /**
     * 可自定义文件导入选择器(及其它回调处理)
     */
    var listener: VideoPlayerListener? = null

    /**
     * 初始化视频播放器
     */
    fun init(application: Application, dev: Boolean) {
        this.app = application
        //默认ijk内核:IjkPlayerManager,EXOPlayer内核:Exo2PlayerManager,系统内核:SystemPlayerManager
        PlayerFactory.setPlayManager(IjkPlayerManager::class.java)
        //默认ProxyCacheManager、ExoPlayerCacheManager
        CacheFactory.setCacheManager(ProxyCacheManager::class.java)
        //切换渲染模式(全屏、16:9、4:3等模式)
        GSYVideoType.setShowType(GSYVideoType.SCREEN_TYPE_DEFAULT)
        //切换绘制模式
        GSYVideoType.setRenderType(GSYVideoType.TEXTURE)
        //ijkPlayer打印log设置
        IjkPlayerManager.setLogLevel(if (dev) IjkMediaPlayer.IJK_LOG_DEFAULT else IjkMediaPlayer.IJK_LOG_SILENT)
        //exoPlayer自定义MediaSource
        /*ExoSourceManager.setExoMediaSourceInterceptListener(object : ExoMediaSourceInterceptListener {
            override fun getMediaSource(dataSource: String?, preview: Boolean, cacheEnable: Boolean, isLooping: Boolean, cacheDir: File?): MediaSource? {
                return null //可自定义MediaSource
            }
        })*/
    }

    //private val url1 = "http://9890.vod.myqcloud.com/9890_4e292f9a3dd011e6b4078980237cc3d3.f20.mp4"
    //private val url2 = "http://alvideo.ippzone.com/zyvd/98/90/b753-55fe-11e9-b0d8-00163e0c0248"
    //private val url3 = "http://7xjmzj.com1.z0.glb.clouddn.com/20171026175005_JObCxCE2.mp4"
    var playList: MutableList<VideoItem> = mutableListOf()
    var playIndex = 0

    /**
     * 播放指定视频文件
     */
    fun play(context: Context, filePath: String?) {
        //视频SDK最低兼容版本为19
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            setVideoFile(filePath ?: getSavedFile())
            val intent = Intent(context, VideoPlayerActivity::class.java)
            if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } else {
            Toast.makeText(context, "您的设备暂不支持视频播放", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * 播放视频列表(文件或url列表)
     */
    fun play(context: Context, list: List<VideoItem>?, index: Int) {
        //视频SDK最低兼容版本为19
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            playList.clear()
            if (list != null) playList.addAll(list)
            playIndex = index
            if (playList.isEmpty()) { //传入播放列表为空时，使用最近播放的视频文件
                setVideoFile(getSavedFile())
            }
            val intent = Intent(context, VideoPlayerActivity::class.java)
            if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } else {
            Toast.makeText(context, "您的设备暂不支持视频播放", Toast.LENGTH_LONG).show()
        }
    }

    fun setVideoFile(path: String?): Boolean {
        if (path.isNullOrEmpty()) return false
        val file = File(path)
        if (file.exists() && isVideoFile(file)) {
            val list: MutableList<VideoItem> = mutableListOf()
            var index = 0
            file.parentFile?.listFiles()?.forEach {
                val isVideo = isVideoFile(it)
                if (isVideo) {
                    if (it == file) {
                        index = list.size
                    }
                    list.add(VideoItem(TYPE_FILE, it.name, it.absolutePath))
                }
            }
            playList = list
            playIndex = index
            return true
        }
        return false
    }

    fun isVideoFile(file: File): Boolean {
        val extension = "." + file.extension
        return extension.length > 1 && VIDEO_SUFFIX.contains(extension, true)
    }

    //记录最近播放的文件(不记录网络视频)
    fun saveVideoFile(filePath: String?) {
        app?.getSharedPreferences(SP_VIDEO_PLAYER, Context.MODE_PRIVATE)?.edit()?.putString("last_play_file", filePath)?.apply()
    }

    private fun getSavedFile(): String? {
        return app?.getSharedPreferences(SP_VIDEO_PLAYER, Context.MODE_PRIVATE)?.getString("last_play_file", null)
    }

    //记录播放过视频的进度
    fun saveProgress(uri: String?, position: Int, duration: Int) {
        if (!uri.isNullOrEmpty() && duration > 60000) { //小于1分钟的短视频不记录进度
            var playPosition = position //刚开始和快播完的视频下次从头开始
            if (position.toFloat() / duration > 0.95) playPosition = 0
            if (position.toFloat() / duration < 0.05) playPosition = 0
            val key = "position_" + ProxyCacheUtils.computeMD5(uri)
            app?.getSharedPreferences(SP_VIDEO_PLAYER, Context.MODE_PRIVATE)?.edit()?.putInt(key, playPosition)?.apply()
        }
    }

    fun getSavedProgress(uri: String?): Int {
        if (!uri.isNullOrEmpty()) {
            val key = "position_" + ProxyCacheUtils.computeMD5(uri)
            return app?.getSharedPreferences(SP_VIDEO_PLAYER, Context.MODE_PRIVATE)?.getInt(key, 0)
                    ?: 0
        }
        return 0
    }

    class VideoItem {
        var type = TYPE_FILE
        var title: String? = null
        var url: String? = null
        var path: String? = null
        var thumbImage: Drawable? = null //缩略图

        constructor()

        constructor(type: Int, title: String?, value: String?) {
            this.type = type
            this.title = title
            when (type) {
                TYPE_FILE -> this.path = value
                TYPE_URL -> this.url = value
            }
        }

        fun getUri(): String? {
            return when (type) {
                TYPE_FILE -> "file://$path"
                TYPE_URL -> url
                else -> null
            }
        }
    }

    interface VideoPlayerListener {
        //自定义方式导入文件
        fun import(activity: Activity, requestCode: Int): Boolean

        //自定义方式导入文件
        fun onImport(activity: Activity, data: Intent): String?
    }
}
