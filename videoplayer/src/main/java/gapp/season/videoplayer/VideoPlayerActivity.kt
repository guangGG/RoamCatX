package gapp.season.videoplayer

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.shuyu.gsyvideoplayer.GSYVideoManager
import com.shuyu.gsyvideoplayer.builder.GSYVideoOptionBuilder
import com.shuyu.gsyvideoplayer.listener.GSYSampleCallBack
import com.shuyu.gsyvideoplayer.utils.OrientationUtils
import com.shuyu.gsyvideoplayer.video.base.GSYVideoView
import gapp.season.util.file.FileShareUtil
import gapp.season.util.sys.ScreenUtil
import gapp.season.util.view.GestureDetectorUtil
import gapp.season.util.view.ThemeUtil
import kotlinx.android.synthetic.main.vplayer_activity_player.*

class VideoPlayerActivity : AppCompatActivity() {
    companion object {
        private const val REQUEST_CODE_OPEN = 1001
    }

    private var orientationUtils: OrientationUtils? = null
    private var isPlay = false
    private var isPause = false
    private var gestureDetector: GestureDetector? = null

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        getIntentData(intent)
        playVideo(0)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeUtil.setTheme(this, 0)
        ScreenUtil.setSysBarColor(this, Color.BLACK, false) //设置状态栏/虚拟导航栏颜色
        setContentView(R.layout.vplayer_activity_player)
        //设置title是否显示
        vplayerView.titleTextView.visibility = View.VISIBLE
        vplayerView.backButton.visibility = View.VISIBLE
        vplayerView.backButton.setOnClickListener { finish() }
        //设置扩展按钮是否显示
        if (vplayerView is VideoPlayerView) {
            (vplayerView as VideoPlayerView).changeTransformView?.visibility = View.GONE
            //(vplayerView as VideoPlayerView).changeRotateView?.visibility = View.GONE
            (vplayerView as VideoPlayerView).moreScaleView?.visibility = View.GONE
        }
        //外部辅助的旋转，帮助全屏
        orientationUtils = OrientationUtils(this, vplayerView)
        //初始化不打开外部的旋转
        orientationUtils?.isEnable = false
        //准备成功之后立即播放
        vplayerView.isStartAfterPrepared = true

        getIntentData(intent)
        playVideo(0)

        val fullscreenOption = View.OnClickListener {
            orientationUtils?.resolveByClick()  //直接横屏
            //第一个true是否需要隐藏actionbar，第二个true是否需要隐藏statusbar
            vplayerView.startWindowFullscreen(this, actionBar = true, statusBar = true)
        }
        vplayerView.fullscreenButton.setOnClickListener(fullscreenOption)

        //Options操作按钮
        vplayerOptions.setOnClickListener { vplayerOptions.visibility = View.GONE }
        val openOption = View.OnClickListener {
            try {
                if (VideoPlayerHelper.listener?.import(this, REQUEST_CODE_OPEN) != true) {
                    val intent = Intent(Intent.ACTION_GET_CONTENT)
                    intent.type = "video/*"
                    intent.addCategory(Intent.CATEGORY_OPENABLE)
                    startActivityForResult(intent, REQUEST_CODE_OPEN)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        vplayerOpenBtn.setOnClickListener(openOption)
        vplayerOpen.setOnClickListener(openOption)
        vplayerCloseBtn.setOnClickListener { finish() }
        vplayerPreOne.setOnClickListener { playVideo(-1) }
        vplayerNextOne.setOnClickListener { playVideo(1) }
        vplayerFullScreen.setOnClickListener(fullscreenOption)
        vplayerPlayBtn.setOnClickListener {
            if (vplayerView.currentState != GSYVideoView.CURRENT_STATE_PAUSE) {
                vplayerView.onVideoPause()
                isPause = true
            } else {
                vplayerView.onVideoResume(false)
                isPause = false
            }
        }
        //手势控制播放上一个/下一个(屏幕左侧上下滑动)
        gestureDetector = GestureDetectorUtil.flingInstance(vplayerView, object : GestureDetectorUtil.FlingCallBack {
            override fun onClick(v: View?, longPress: Boolean): Boolean {
                if (longPress) vplayerOptions.visibility = View.VISIBLE //showOptionsBtn
                return true
            }

            override fun onFling(v: View?, inLeft: Boolean, inTop: Boolean, flingVertical: Boolean, direction: Int): Boolean {
                if (flingVertical) {
                    playVideo(-1 * direction)
                }
                return false
            }
        })
        vplayerView.surfaceTouchListener = View.OnTouchListener { _, motionEvent ->
            gestureDetector?.onTouchEvent(motionEvent)
            return@OnTouchListener true
        }
    }

    private fun getIntentData(intent: Intent?) {
        if (Intent.ACTION_VIEW == intent?.action) { //intent-filter接收到的播放文件
            VideoPlayerHelper.setVideoFile(intent.data?.path)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_OPEN) {
            if (resultCode == Activity.RESULT_OK && data != null && data.data != null) {
                //少部分文件选择器支持返回实际文件路径Uri(低版本API)，一般为content:类型Uri通过媒体库查询path，推荐自定义文件选择器
                val path: String? = VideoPlayerHelper.listener?.onImport(this, data)
                        ?: FileShareUtil.getPath(applicationContext, data.data)
                if (VideoPlayerHelper.setVideoFile(path)) {
                    playVideo(0)
                } else {
                    Toast.makeText(this, "打开视频失败", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun playVideo(direction: Int) {
        if (VideoPlayerHelper.playList.isNotEmpty()) {
            VideoPlayerHelper.playIndex = VideoPlayerHelper.playIndex + direction
            if (VideoPlayerHelper.playIndex < 0) {
                VideoPlayerHelper.playIndex = VideoPlayerHelper.playList.size - 1
            } else if (VideoPlayerHelper.playIndex >= VideoPlayerHelper.playList.size) {
                VideoPlayerHelper.playIndex = 0
            }
            val video = VideoPlayerHelper.playList[VideoPlayerHelper.playIndex]
            val serialNum = if (VideoPlayerHelper.playList.size < 2) "" else
                String.format("(%d/%d)", VideoPlayerHelper.playIndex + 1, VideoPlayerHelper.playList.size)
            Log.i("VideoPlayer", String.format("playVideo:%s %s", serialNum, video.getUri()))
            if (video.type == VideoPlayerHelper.TYPE_FILE) VideoPlayerHelper.saveVideoFile(video.path)
            configPlayer(video, serialNum)
            vplayerView.startPlayLogic() //开始播放
            vplayerOpenBtn.visibility = View.GONE
        } else {
            vplayerOpenBtn.visibility = View.VISIBLE
        }
    }

    private fun configPlayer(video: VideoPlayerHelper.VideoItem, serialNum: String) {
        val playPosition = VideoPlayerHelper.getSavedProgress(video.getUri())
        val imageView = ImageView(this)
        imageView.scaleType = ImageView.ScaleType.CENTER_CROP
        imageView.setImageDrawable(video.thumbImage ?: ColorDrawable(Color.GRAY)) //设置封面
        GSYVideoOptionBuilder().setThumbImageView(imageView)
                .setIsTouchWiget(true) //设置是否允许手势亮度/音量/进度控制
                .setRotateViewAuto(false)
                .setLockLand(false)
                .setAutoFullWithSize(true)
                .setShowFullAnimation(false)
                .setSeekOnStart(playPosition.toLong()) //setPlayPosition初始化播放位置无效
                .setNeedLockFull(true)
                .setUrl(video.getUri())
                .setCacheWithPlay(false)
                .setVideoTitle(serialNum + (video.title ?: ""))
                .setVideoAllCallBack(object : GSYSampleCallBack() {
                    override fun onQuitFullscreen(url: String?, vararg objects: Any) {
                        super.onQuitFullscreen(url, *objects)
                        if (orientationUtils != null) {
                            orientationUtils?.backToProtVideo()
                        }
                    }

                    override fun onPrepared(url: String?, vararg objects: Any) {
                        super.onPrepared(url, *objects)
                        //开始播放了才能旋转和全屏
                        orientationUtils?.isEnable = true
                        isPlay = true
                    }

                    override fun onAutoComplete(url: String?, vararg objects: Any?) {
                        super.onAutoComplete(url, *objects)
                        vplayerView.postDelayed({
                            playVideo(1) //自动播放下一个
                        }, 60)
                    }
                }).setLockClickListener { _, lock ->
                    //配合下方的onConfigurationChanged
                    orientationUtils?.isEnable = !lock
                }.setGSYVideoProgressListener { progress, secProgress, currentPosition, duration ->
                    VideoPlayerHelper.saveProgress(video.getUri(), currentPosition, duration)
                }.build(vplayerView)
    }

    override fun onBackPressed() {
        orientationUtils?.backToProtVideo()
        if (GSYVideoManager.backFromWindowFull(this)) {
            return
        }
        super.onBackPressed()
    }


    override fun onPause() {
        vplayerView.currentPlayer.onVideoPause()
        super.onPause()
        isPause = true
    }

    override fun onResume() {
        vplayerView.currentPlayer.onVideoResume(false)
        super.onResume()
        isPause = false
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isPlay) {
            vplayerView.currentPlayer.release()
        }
        orientationUtils?.releaseListener()
    }


    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        //如果旋转了就全屏
        if (isPlay && !isPause) {
            vplayerView.onConfigurationChanged(this, newConfig, orientationUtils, true, true)
        }
    }
}
