package gapp.season.musicplayer

import android.app.*
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.*
import androidx.core.app.NotificationCompat
import gapp.season.util.file.FileUtil
import java.util.*

class MusicPlayerService : Service() {
    companion object {
        // 通知ID
        private const val MUSIC_PLAYER_NOTIFY_ID = 32456
        private const val MUSIC_PLAYER_NOTIFY_CHANNEL_ID = MUSIC_PLAYER_NOTIFY_ID.toString()
        private const val MUSIC_PLAYER_NOTIFY_CHANNEL_NAME = MUSIC_PLAYER_NOTIFY_CHANNEL_ID
        // 播放器状态
        const val MUSIC_NOT_PLAY = 0// 未初始化(停止播放)
        const val MUSIC_IS_PLAYING = 1// 正在播放
        const val MUSIC_IS_PAUSE = 2// 未播放(暂停)
        // 播放模式
        const val PLAY_MODE_RANDOM = "随机播放"
        const val PLAY_MODE_ONE = "单曲播放"
        const val PLAY_MODE_ALL = "循环播放"
        val PLAY_MODE_LIST = arrayOf(PLAY_MODE_ALL, PLAY_MODE_ONE, PLAY_MODE_RANDOM)
    }

    private var musicBinder: MusicBinder? = null
    // 回调计时器
    private var callbackTimer: Timer? = null
    // 播放器
    private var mediaPlayer: MediaPlayer? = null
    // 播放完成回调器
    private var onCompletionListener: MediaPlayer.OnCompletionListener? = null
    // 播放信息
    private var hasInit = false
    private var playMusicList: List<String>? = null
    private var playIndex: Int = 0
    private var playerCallBack: MusicPlayerController.MusicPlayerCallBack? = null
    private var playState: Int = MUSIC_NOT_PLAY// 播放状态
    private var playMode: Int = 0// 播放模式
    private var retryTimes: Int = 0//播放失败时重试次数

    override fun onBind(intent: Intent?): IBinder? {
        MusicPlayerHelper.log("MusicPlayerService onBind")
        musicBinder = MusicBinder(this)
        return musicBinder
    }

    override fun onCreate() {
        super.onCreate()
        MusicPlayerHelper.log("MusicPlayerService onCreate")
        onCompletionListener = MediaPlayer.OnCompletionListener { playComplete() }
        callbackTimer = Timer()
        callbackTimer!!.schedule(object : TimerTask() {
            override fun run() {
                if (playState == MUSIC_IS_PLAYING) {
                    doPlayCallBack()
                }
            }
        }, 0, 300)
        createPlayer()
    }

    override fun onDestroy() {
        musicBinder?.stop()
        musicBinder = null
        onCompletionListener = null
        if (callbackTimer != null) {
            callbackTimer!!.cancel()
            callbackTimer = null
        }
        cancelNotification()
        MusicPlayerHelper.log("MusicPlayerService onDestroy")
        super.onDestroy()
    }

    private fun createPlayer() {
        MusicPlayerHelper.log("MusicPlayerService createPlayer")
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer()
            MusicPlayerHelper.log("MusicPlayerService create MediaPlayer")
        }
        mediaPlayer?.reset()
        // 设置播放类型为音乐
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mediaPlayer?.setAudioAttributes(AudioAttributes.Builder().setLegacyStreamType(AudioManager.STREAM_MUSIC).build())
        } else {
            @Suppress("DEPRECATION")
            mediaPlayer?.setAudioStreamType(AudioManager.STREAM_MUSIC)
        }
    }

    private fun playMusic(musicList: List<String>?, index: Int) {
        MusicPlayerHelper.log("MusicPlayerService playMusic $index/${musicList?.size ?: 0}")
        if (musicList != null && musicList.size > index) {
            try {
                playState = MUSIC_NOT_PLAY //MediaPlayer准备中暂时标记为未播放
                createPlayer()
                mediaPlayer?.setOnErrorListener { _, _, _ ->
                    MusicPlayerHelper.log("MusicPlayerService MediaPlayer OnError")
                    false
                }
                mediaPlayer?.setOnCompletionListener(onCompletionListener)
                mediaPlayer?.setOnPreparedListener {
                    mediaPlayer?.start()
                    // 标记播放状态
                    playState = MUSIC_IS_PLAYING
                    // 显示系统通知栏消息
                    showNotification(FileUtil.getFileName(musicList[index]))
                    // 回调播放信息
                    doPlayCallBack()
                    playerCallBack?.onToggleMusic(musicList, index)
                }
                mediaPlayer?.setDataSource(musicList[index])
                mediaPlayer?.prepareAsync()
                retryTimes = 0
            } catch (e: Exception) {
                //prepare()后立即start()会报错： android.media.MediaPlayer._prepare(Native Method) -> java.lang.IllegalStateException
                //改为prepareAsync()异步prepare后在回调中start()
                e.printStackTrace()
                if (retryTimes < 3) {
                    retryTimes++
                    //异常时延时一小会儿再重试
                    Handler(Looper.getMainLooper()).postDelayed({ playMusic(musicList, index) }, 200)
                } else {
                    // 自动播放下一首
                    retryTimes = 0
                    playComplete()
                }
            }
        }
    }

    private fun playComplete() {
        MusicPlayerHelper.log("MusicPlayerService playComplete")
        try {
            val total = playMusicList?.size ?: 0
            when (PLAY_MODE_LIST[playMode]) {
                PLAY_MODE_ALL -> {
                    playIndex = if (playIndex + 1 >= total) {
                        0
                    } else {
                        playIndex + 1
                    }
                    playMusic(playMusicList, playIndex)
                }
                PLAY_MODE_ONE -> {
                    playMusic(playMusicList, playIndex)
                }
                PLAY_MODE_RANDOM -> {
                    // 随机播放(文件大于1时不重复播放)
                    var nextPosition = playIndex
                    while (total > 1 && nextPosition == playIndex) {
                        nextPosition = Random(System.currentTimeMillis()).nextInt(total)
                    }
                    playIndex = nextPosition
                    playMusic(playMusicList, playIndex)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun doPlayCallBack() {
        try {
            var currentPosition = 0
            var duration = 0
            if (playState != MUSIC_NOT_PLAY) { //MediaPlayer在Idle状态获取duration会抛异常
                currentPosition = mediaPlayer?.currentPosition ?: 0
                duration = mediaPlayer?.duration ?: 0
            }
            playerCallBack?.playCallBack(playMusicList, playIndex, currentPosition, duration, playState, playMode)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 播放时显示(或更新)通知栏图标
     */
    @Suppress("DEPRECATION")
    private fun showNotification(musicName: String) {
        //通知在API-26以上系统需要特殊处理
        MusicPlayerHelper.log("MusicPlayerService showNotification $musicName")
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val intent = Intent()
        intent.action = MusicPlayerHelper.MUSIC_PLAYER_PAGE_ACTION
        intent.addCategory("android.intent.category.DEFAULT")
        val resultPendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(MUSIC_PLAYER_NOTIFY_CHANNEL_ID,
                    MUSIC_PLAYER_NOTIFY_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
            val builder = Notification.Builder(this, MUSIC_PLAYER_NOTIFY_CHANNEL_ID)
            builder.setTicker(musicName)//状态栏提示
            builder.setSmallIcon(R.drawable.mplayer_ic_music)//状态栏图标
            builder.setContentTitle("正在播放：")//通知栏标题
            builder.setContentText(musicName)//通知栏内容
            builder.setContentIntent(resultPendingIntent)//通知栏点击跳转事件
            builder.build()
        } else {
            val builder = NotificationCompat.Builder(this)
            builder.setTicker(musicName)//状态栏提示
            builder.setSmallIcon(R.drawable.mplayer_ic_music)//状态栏图标
            builder.setContentTitle("正在播放：")//通知栏标题
            builder.setContentText(musicName)//通知栏内容
            builder.setContentIntent(resultPendingIntent)//通知栏点击跳转事件
            builder.build()
        }
        notification.flags = notification.flags or Notification.FLAG_NO_CLEAR// 不清除通知
        notificationManager.notify(MUSIC_PLAYER_NOTIFY_ID, notification)
    }

    private fun cancelNotification() {
        MusicPlayerHelper.log("MusicPlayerService cancelNotification")
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(MUSIC_PLAYER_NOTIFY_ID)
    }

    private class MusicBinder(service: MusicPlayerService) : Binder(), MusicPlayerController {
        private var service: MusicPlayerService? = service
        override fun hasInit(): Boolean {
            return service?.hasInit ?: false
        }

        override fun initMusicList(musicList: List<String>?, index: Int) {
            stop() //停止播放
            //初始化音乐列表
            service?.playMusicList = musicList
            service?.playIndex = index
            service?.hasInit = true
            playNew()
        }

        override fun playIndex(index: Int) {
            service?.playIndex = index
            playNew()
        }

        override fun playNext(toPre: Boolean) {
            if (PLAY_MODE_LIST[service?.playMode ?: 0] == PLAY_MODE_RANDOM) {
                service?.playComplete() //随机播放
                return
            }
            val total = service?.playMusicList?.size ?: 0
            val current = service?.playIndex ?: 0
            if (total > 0) {
                if (toPre) {
                    if (current - 1 >= 0) {
                        service?.playIndex = current - 1
                    } else {
                        service?.playIndex = total - 1
                    }
                } else {
                    if (current + 1 < total) {
                        service?.playIndex = current + 1
                    } else {
                        service?.playIndex = 0
                    }
                }
            }
            playNew()
        }

        fun playNew() {
            if (service?.playMusicList != null && service?.playMusicList!!.isNotEmpty()) {
                if (service?.playIndex!! < 0 || service?.playIndex!! >= service?.playMusicList!!.size) {
                    service?.playIndex = 0
                }
                service?.playMusic(service?.playMusicList!!, service?.playIndex!!)
            }
        }

        override fun play() {
            if (service?.mediaPlayer != null && (service!!.playState != MUSIC_NOT_PLAY)) {
                service?.mediaPlayer!!.start()
                service?.playState = MUSIC_IS_PLAYING
            } else {
                service?.playState = MUSIC_NOT_PLAY
            }
            requestPlayCallBack()
        }

        override fun pause() {
            if (service?.mediaPlayer != null && (service!!.playState != MUSIC_NOT_PLAY)) {
                service?.mediaPlayer!!.pause()
                service?.playState = MUSIC_IS_PAUSE
            } else {
                service?.playState = MUSIC_NOT_PLAY
            }
            requestPlayCallBack()
        }

        override fun stop() {
            if (service?.mediaPlayer != null) {
                service!!.mediaPlayer!!.stop()
                service!!.mediaPlayer!!.release()
                service!!.mediaPlayer = null
            }
            service?.playState = MUSIC_NOT_PLAY
            service?.cancelNotification()
            requestPlayCallBack()
        }

        override fun seek(percent: Float) {
            if (service?.mediaPlayer != null && (service!!.playState != MUSIC_NOT_PLAY)) {
                val leng = service?.mediaPlayer!!.duration
                val msec = (leng * percent).toInt()
                service?.mediaPlayer!!.seekTo(msec)
                service?.mediaPlayer!!.start()
                service?.playState = MUSIC_IS_PLAYING
            } else {
                service?.playState = MUSIC_NOT_PLAY
            }
            requestPlayCallBack()
        }

        override fun switchMode(mode: Int): Int {
            var toMode = mode
            if (mode < 0 || mode >= PLAY_MODE_LIST.size) {
                toMode = (service?.playMode ?: 0) + 1
                if (toMode >= PLAY_MODE_LIST.size) {
                    toMode = 0
                }
            }
            service?.playMode = toMode
            requestPlayCallBack()
            return toMode
        }

        override fun setMusicPlayerCallBack(musicPlayerCallBack: MusicPlayerController.MusicPlayerCallBack) {
            service?.playerCallBack = musicPlayerCallBack
        }

        override fun requestPlayCallBack() {
            service?.doPlayCallBack()
        }
    }
}
