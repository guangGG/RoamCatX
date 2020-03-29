package gapp.season.musicplayer

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.text.TextUtils
import android.view.View
import android.widget.SeekBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import gapp.season.util.file.FileShareUtil
import gapp.season.util.file.FileUtil
import gapp.season.util.task.ThreadPoolExecutor
import gapp.season.util.tips.ToastUtil
import gapp.season.util.view.ThemeUtil
import kotlinx.android.synthetic.main.mplayer_activity_player.*
import java.io.File

@SuppressLint("SetTextI18n")
class MusicPlayerActivity : AppCompatActivity() {
    companion object {
        private const val OPERATE_SERVICE_DELAYED = 200L
        private const val REQUEST_CODE_OPEN = 1002
    }

    private var mMusicPlayerController: MusicPlayerController? = null
    private var mServiceConnection: ServiceConnection? = null
    private var mPlayState = MusicPlayerService.MUSIC_NOT_PLAY
    private var mPlayingMusic: String? = null
    private var mShowLyric = false
    private var mResume = false
    private var mInitMusicFile = false

    private var mListAdapter: ListAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeUtil.setTheme(this, 0)
        setContentView(R.layout.mplayer_activity_player)
        initView()
        bindService()
        initIntentData()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        initIntentData()
    }

    private fun initView() {
        mplayerBack.setOnClickListener { onBackPressed() }
        mplayerMenu.setOnClickListener { showMenu() }
        mplayerTitle.setOnClickListener {
            //定位到列表中当前播放音乐
            if (mShowLyric) {
                mShowLyric = false
                updateShowMode()
                mMusicPlayerController?.requestPlayCallBack()
                MusicHistoryBuffer.setShowMode(if (mShowLyric) 1 else 0)
            }
            mplayerList.scrollToPosition(mListAdapter?.playIndex ?: 0)
        }
        mplayerModeBtn.setOnClickListener {
            val mode = mMusicPlayerController?.switchMode(-1) ?: 0
            //记住播放模式
            MusicHistoryBuffer.setPlayMode(mode)
        }
        mplayerPreBtn.setOnClickListener { mMusicPlayerController?.playNext(true) }
        mplayerPlayBtn.setOnClickListener {
            when (mPlayState) {
                MusicPlayerService.MUSIC_IS_PLAYING -> {
                    mMusicPlayerController?.pause()
                }
                MusicPlayerService.MUSIC_IS_PAUSE -> {
                    mMusicPlayerController?.play()
                }
                MusicPlayerService.MUSIC_NOT_PLAY -> {
                    initMusicFile(null)
                }
            }
        }
        mplayerNextBtn.setOnClickListener { mMusicPlayerController?.playNext(false) }
        mplayerListBtn.setOnClickListener {
            mShowLyric = !mShowLyric
            updateShowMode()
            mMusicPlayerController?.requestPlayCallBack()
            //记住显示模式
            MusicHistoryBuffer.setShowMode(if (mShowLyric) 1 else 0)
        }
        mplayerSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mMusicPlayerController?.seek(1f * progress / 10000)
                }
            }

            override fun onStartTrackingTouch(sb: SeekBar?) {
            }

            override fun onStopTrackingTouch(sb: SeekBar?) {
            }
        })

        mplayerListEmpty.setOnClickListener { openMusic() }
        mListAdapter = ListAdapter()
        val itemClickListener = BaseQuickAdapter.OnItemClickListener { adapter, _, position ->
            val item: String = adapter.getItem(position) as String
            MusicPlayerHelper.log("点击播放音乐：$item")
            mMusicPlayerController?.playIndex(position)
        }
        mListAdapter?.onItemClickListener = itemClickListener
        val layoutManager = LinearLayoutManager(this)
        layoutManager.orientation = RecyclerView.VERTICAL
        mplayerList.layoutManager = layoutManager
        mplayerList.adapter = mListAdapter

        mShowLyric = MusicHistoryBuffer.getShowMode() == 1
        updateShowMode()
        val playMode = MusicHistoryBuffer.getPlayMode()
        //延迟设置模式(等待初始化Service完成)
        Handler(Looper.getMainLooper()).postDelayed({
            mMusicPlayerController?.switchMode(playMode)
        }, OPERATE_SERVICE_DELAYED)
    }

    private fun bindService() {
        //绑定service
        mServiceConnection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                mMusicPlayerController = service as MusicPlayerController
                mMusicPlayerController!!.setMusicPlayerCallBack(object : MusicPlayerController.MusicPlayerCallBack {
                    override fun playCallBack(musicList: List<String>?, index: Int, currentPosition: Int, duration: Int, playState: Int, playMode: Int) {
                        mPlayState = playState
                        if (mResume) {
                            runOnUiThread {
                                val musicPath = if (musicList != null && musicList.size > index) musicList[index] else null
                                mPlayingMusic = musicPath
                                mplayerTitle.text = if (musicPath.isNullOrEmpty()) "音乐播放器" else FileUtil.getFileName(musicPath)
                                if (playState == MusicPlayerService.MUSIC_IS_PLAYING) {
                                    mplayerPlayBtn.setImageResource(R.drawable.mplayer_ic_pause)
                                } else {
                                    mplayerPlayBtn.setImageResource(R.drawable.mplayer_ic_play)
                                }
                                when (MusicPlayerService.PLAY_MODE_LIST[playMode]) {
                                    MusicPlayerService.PLAY_MODE_RANDOM -> mplayerModeBtn.setImageResource(R.drawable.mplayer_ic_mode_random)
                                    MusicPlayerService.PLAY_MODE_ONE -> mplayerModeBtn.setImageResource(R.drawable.mplayer_ic_mode_one)
                                    else -> mplayerModeBtn.setImageResource(R.drawable.mplayer_ic_mode_all)
                                }
                                if (mShowLyric) {
                                    mplayerLyric.text = if (musicPath.isNullOrEmpty()) "未播放" else MusicLyricSearcher.showLyric(musicPath, currentPosition)
                                } else {
                                    if (mListAdapter != null && (mListAdapter!!.playIndex != index || mListAdapter!!.data != musicList)) {
                                        //数据有变化才更新列表，降低性能消耗
                                        mListAdapter?.playIndex = index
                                        mListAdapter?.setNewData(musicList)
                                    }
                                    mplayerListEmpty.visibility = if (musicList.isNullOrEmpty()) View.VISIBLE else View.GONE
                                }
                                if (duration > 0) {
                                    val seekPosition = (10000f * currentPosition / duration).toInt()
                                    mplayerSeekBar.progress = seekPosition
                                } else {
                                    mplayerSeekBar.progress = 0
                                }
                                mplayerCurrentPosition.text = MusicLyricResolver.toMinuteForm(currentPosition)
                                mplayerDuration.text = MusicLyricResolver.toMinuteForm(duration)
                            }
                        }
                    }

                    override fun onToggleMusic(musicList: List<String>?, index: Int) {
                        if (!musicList.isNullOrEmpty()) {
                            MusicHistoryBuffer.mark(MusicFileResolver.MusicFileList(musicList, index))
                        }
                    }
                })
                mMusicPlayerController?.requestPlayCallBack()
                if (!mMusicPlayerController!!.hasInit() && !mInitMusicFile) {
                    initMusicFile(null)
                }
            }

            override fun onServiceDisconnected(name: ComponentName) {}
        }
        //开启并绑定服务
        val intent = Intent(this, MusicPlayerService::class.java)
        startService(intent)
        bindService(intent, mServiceConnection!!, BIND_AUTO_CREATE)
    }

    private fun initIntentData() {
        // 首先从 getIntent() 中获取
        var path: String? = intent.getStringExtra("path")
        // 其次从系统Intent拦截中获取
        if (TextUtils.isEmpty(path)) {
            // 从文件的打开方式进入
            val uri = intent.data
            if (uri != null && Intent.ACTION_VIEW == intent.action) {
                path = uri.path
            }
        }
        if (isAllowablePath(path)) {
            initMusicFile(path)
        }
    }

    private fun initMusicFile(path: String?) {
        ThreadPoolExecutor.getInstance().execute {
            val list = MusicFileResolver.getPlayList(path)
            if (list?.list != null) {
                mInitMusicFile = true
                //延迟播放(等待初始化Service完成)
                Handler(Looper.getMainLooper()).postDelayed({
                    mMusicPlayerController?.initMusicList(list.list!!, list.index)
                }, OPERATE_SERVICE_DELAYED)
            }
        }
    }

    private fun isAllowablePath(path: String?): Boolean {
        if (!TextUtils.isEmpty(path)) {
            val file = File(path!!)
            if (file.exists() && MusicPlayerHelper.isMusicFile(file)) {
                return true
            } else {
                ToastUtil.showShort("不支持该类型的文件")
            }
        }
        return false
    }

    private fun updateShowMode() {
        if (mShowLyric) mplayerListBtn.setImageResource(R.drawable.mplayer_ic_lyric)
        else mplayerListBtn.setImageResource(R.drawable.mplayer_ic_music)
        mplayerLyricLayout.visibility = if (mShowLyric) View.VISIBLE else View.GONE
        mplayerListLayout.visibility = if (!mShowLyric) View.VISIBLE else View.GONE
    }

    private fun showMenu() {
        val items = arrayOf("音乐详情", "播放记录", "选择音乐", "退出播放")
        AlertDialog.Builder(this)
                .setTitle("菜单")
                .setItems(items) { _, which ->
                    when (which) {
                        0 -> showDetail()
                        1 -> showHistorys()
                        2 -> openMusic()
                        3 -> {
                            mMusicPlayerController?.stop()
                            finish()
                        }
                    }
                }.show()
    }

    private fun showDetail() {
        if (!mPlayingMusic.isNullOrEmpty()) {
            val msg = MusicPlayerHelper.getAudioProperty(File(mPlayingMusic!!))
            AlertDialog.Builder(this)
                    .setTitle("音乐详情")
                    .setMessage(msg)
                    .setNegativeButton("关闭", null)
                    .show()
        } else {
            ToastUtil.showShort("未播放")
        }
    }

    private fun showHistorys() {
        if (!MusicHistoryBuffer.historys.isNullOrEmpty()) {
            val total = MusicHistoryBuffer.historys!!.size
            val items = Array(total) {
                FileUtil.getFileName(MusicHistoryBuffer.historys!![total - 1 - it])
            }
            AlertDialog.Builder(this)
                    .setTitle("播放记录")
                    .setItems(items) { _, index ->
                        if (MusicHistoryBuffer.historys?.size ?: 0 > index) {
                            val path = MusicHistoryBuffer.historys!![total - 1 - index]
                            initMusicFile(path)
                        }
                    }.setNegativeButton("关闭", null)
                    .show()
        } else {
            ToastUtil.showShort("暂无记录")
        }
    }

    private fun openMusic() {
        try {
            if (MusicPlayerHelper.listener?.import(this, REQUEST_CODE_OPEN) != true) {
                val intent = Intent(Intent.ACTION_GET_CONTENT)
                intent.type = "audio/*"
                intent.addCategory(Intent.CATEGORY_OPENABLE)
                startActivityForResult(intent, REQUEST_CODE_OPEN)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_OPEN) {
            if (resultCode == Activity.RESULT_OK && data != null && data.data != null) {
                //少部分文件选择器支持返回实际文件路径Uri(低版本API)，一般为content:类型Uri通过媒体库查询path，推荐自定义文件选择器
                val path: String? = MusicPlayerHelper.listener?.onImport(this, data)
                        ?: FileShareUtil.getPath(applicationContext, data.data)
                initMusicFile(path)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mResume = true
        mMusicPlayerController?.requestPlayCallBack()
    }

    override fun onPause() {
        mResume = false
        super.onPause()
    }

    override fun onDestroy() {
        if (mServiceConnection != null) {
            unbindService(mServiceConnection!!)
            mServiceConnection = null
        }
        if (mPlayState == MusicPlayerService.MUSIC_NOT_PLAY) {
            // 音乐已停止时，关闭音乐播放器服务
            val intent = Intent(this, MusicPlayerService::class.java)
            stopService(intent)
        }
        super.onDestroy()
    }

    class ListAdapter : BaseQuickAdapter<String, BaseViewHolder>(R.layout.mplayer_list_item) {
        var playIndex = -1
        override fun convert(helper: BaseViewHolder, item: String?) {
            helper.setText(R.id.mplayer_item_name, "${helper.layoutPosition + 1}.${FileUtil.getFileName(item)}")
            helper.setTextColor(R.id.mplayer_item_name, if ((playIndex == helper.layoutPosition)) 0XFF0088FF.toInt() else 0XFF666666.toInt())
            helper.setGone(R.id.mplayer_item_icon, (playIndex == helper.layoutPosition))
        }
    }
}
