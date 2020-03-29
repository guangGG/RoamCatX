package gapp.season.musicplayer

interface MusicPlayerController {
    fun hasInit(): Boolean
    fun initMusicList(musicList: List<String>?, index: Int)
    fun playIndex(index: Int)
    fun playNext(toPre: Boolean)
    fun play()
    fun pause()
    fun stop()
    fun seek(percent: Float)
    fun switchMode(mode: Int): Int
    fun setMusicPlayerCallBack(musicPlayerCallBack: MusicPlayerCallBack)
    fun requestPlayCallBack() //请求musicPlayerCallBack回调一次数据


    interface MusicPlayerCallBack {
        /**
         * 回调播放信息
         *
         * @param musicList       当前播放列表
         * @param index           当前播放文件序号
         * @param currentPosition 当前播放时间位置
         * @param duration        总时间
         * @param playState       播放状态(NO_PLAYER、IS_PLAYING、NOT_PLAYING)
         * @param playMode       播放模式：循环、随机
         */
        fun playCallBack(musicList: List<String>?, index: Int, currentPosition: Int, duration: Int, playState: Int, playMode: Int)

        /**
         * 播放另一首音乐时回调
         */
        fun onToggleMusic(musicList: List<String>?, index: Int)
    }
}
