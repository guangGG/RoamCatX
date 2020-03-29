package gapp.season.nerverabbit

import android.annotation.SuppressLint
import android.media.AudioManager
import android.media.SoundPool
import android.os.Bundle
import android.util.SparseIntArray
import androidx.appcompat.app.AppCompatActivity
import gapp.season.util.log.LogUtil
import gapp.season.util.view.ThemeUtil
import kotlinx.android.synthetic.main.nrbt_activity.*

@SuppressLint("SetTextI18n")
class NerveRabbitActivity : AppCompatActivity() {
    private var soundPool: SoundPool? = null
    private val soundKv = SparseIntArray()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeUtil.setTheme(this, 0)
        setContentView(R.layout.nrbt_activity)
        initView()
        initData()
    }

    private fun initView() {
        nrbtBack.setOnClickListener { onBackPressed() }
        nrbtRetry.setOnClickListener { initGame() }
    }

    private fun initData() {
        initSoundPool()
        initGame()
    }

    @Suppress("DEPRECATION")
    private fun initSoundPool() {
        soundPool = SoundPool(20, AudioManager.STREAM_MUSIC, 0)
        soundKv.put(NerveRabbitView.STATUS_INIT, soundPool!!.load(assets.openFd("nrbt_start.mp3"), 0))
        soundKv.put(NerveRabbitView.STATUS_MOVE, soundPool!!.load(assets.openFd("nrbt_step.mp3"), 0))
        soundKv.put(NerveRabbitView.STATUS_LOSE, soundPool!!.load(assets.openFd("nrbt_lose.mp3"), 0))
        soundKv.put(NerveRabbitView.STATUS_WIN, soundPool!!.load(assets.openFd("nrbt_win.mp3"), 0))
    }

    private fun initGame() {
        nrbtView.init(nrbtView.width, nrbtView.height, 9, 10) { status, stap ->
            LogUtil.d("NerveRabbit status:$status stap:$stap")
            nrbtStatus.text = when (status) {
                NerveRabbitView.STATUS_INIT -> "游戏开始"
                NerveRabbitView.STATUS_MOVE -> "第${stap}步"
                NerveRabbitView.STATUS_LOSE -> "失败！"
                NerveRabbitView.STATUS_WIN -> "胜利！"
                NerveRabbitView.STATUS_GAMEOVER -> "游戏已结束"
                else -> ""
            }
            if (soundKv.get(status) > 0) {
                soundPool?.play(soundKv[status], 1f, 1f, 0, 0, 1f)
            }
        }
    }

    override fun onDestroy() {
        soundPool?.release()
        super.onDestroy()
    }
}
