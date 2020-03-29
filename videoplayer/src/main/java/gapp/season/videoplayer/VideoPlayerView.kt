package gapp.season.videoplayer

import android.content.Context
import android.graphics.Matrix
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.Surface
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.TextView
import com.shuyu.gsyvideoplayer.utils.GSYVideoType
import com.shuyu.gsyvideoplayer.video.StandardGSYVideoPlayer
import com.shuyu.gsyvideoplayer.video.base.GSYBaseVideoPlayer
import com.shuyu.gsyvideoplayer.video.base.GSYVideoPlayer

/**
 * 增加：镜像、旋转、设置长宽比例
 */
class VideoPlayerView : StandardGSYVideoPlayer {
    var moreScaleView: TextView? = null
    var changeRotateView: TextView? = null
    var changeTransformView: TextView? = null

    private var mType = 0 //记住切换长宽比例
    private var mTransformSize = 0 //记录镜像类型

    constructor(context: Context?) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    /**
     * 1.5.0开始加入，如果需要不同布局区分功能，需要重载
     */
    constructor(context: Context, fullFlag: Boolean) : super(context, fullFlag)

    override fun getLayoutId(): Int {
        return R.layout.vplayer_view_player
    }

    override fun init(context: Context) {
        super.init(context)
        initView()
    }

    private fun initView() {
        moreScaleView = findViewById<View>(R.id.moreScale) as TextView
        changeRotateView = findViewById<View>(R.id.change_rotate) as TextView
        changeTransformView = findViewById<View>(R.id.change_transform) as TextView

        //切换比例
        moreScaleView!!.setOnClickListener(OnClickListener {
            if (!mHadPlay) {
                return@OnClickListener
            }
            if (mType == 0) {
                mType = 1
            } else if (mType == 1) {
                mType = 2
            } else if (mType == 2) {
                mType = 3
            } else if (mType == 3) {
                mType = 4
            } else if (mType == 4) {
                mType = 0
            }
            resolveTypeUI()
        })

        //旋转播放角度
        changeRotateView!!.setOnClickListener(OnClickListener {
            if (!mHadPlay) {
                return@OnClickListener
            }
            if (mTextureView.rotation - mRotate == 270f) {
                mTextureView.rotation = mRotate.toFloat()
                mTextureView.requestLayout()
            } else {
                mTextureView.rotation = mTextureView.rotation + 90
                mTextureView.requestLayout()
            }
        })

        //镜像旋转
        changeTransformView!!.setOnClickListener(OnClickListener {
            if (!mHadPlay) {
                return@OnClickListener
            }
            if (mTransformSize == 0) {
                mTransformSize = 1
            } else if (mTransformSize == 1) {
                mTransformSize = 2
            } else if (mTransformSize == 2) {
                mTransformSize = 0
            }
            resolveTransform()
        })

    }

    override fun onSurfaceSizeChanged(surface: Surface?, width: Int, height: Int) {
        super.onSurfaceSizeChanged(surface, width, height)
        resolveTransform()
    }

    /**
     * 处理显示逻辑
     */
    override fun onSurfaceAvailable(surface: Surface) {
        super.onSurfaceAvailable(surface)
        resolveRotateUI()
        resolveTransform()
    }

    /**
     * 全屏时将对应处理参数逻辑赋给全屏播放器
     */
    override fun startWindowFullscreen(context: Context, actionBar: Boolean, statusBar: Boolean): GSYBaseVideoPlayer {
        val view = super.startWindowFullscreen(context, actionBar, statusBar) as VideoPlayerView
        view.mType = mType
        view.mTransformSize = mTransformSize
        //view.resolveTransform();
        view.resolveTypeUI()
        //view.resolveRotateUI();
        return view
    }

    /**
     * 退出全屏时将对应处理参数逻辑返回给非播放器
     */
    override fun resolveNormalVideoShow(oldF: View?, vp: ViewGroup, gsyVideoPlayer: GSYVideoPlayer?) {
        super.resolveNormalVideoShow(oldF, vp, gsyVideoPlayer)
        if (gsyVideoPlayer != null) {
            val view = gsyVideoPlayer as VideoPlayerView
            mType = view.mType
            mTransformSize = view.mTransformSize
            resolveTypeUI()
        }
    }

    /**
     * 处理镜像
     */
    private fun resolveTransform() {
        when (mTransformSize) {
            1 -> {
                val transform = Matrix()
                transform.setScale(-1f, 1f, (mTextureView.width / 2).toFloat(), 0f)
                mTextureView.setTransform(transform)
                changeTransformView!!.text = "左右镜像"
                mTextureView.invalidate()
            }
            2 -> {
                val transform = Matrix()
                transform.setScale(1f, -1f, 0f, (mTextureView.height / 2).toFloat())
                mTextureView.setTransform(transform)
                changeTransformView!!.text = "上下镜像"
                mTextureView.invalidate()
            }
            0 -> {
                val transform = Matrix()
                transform.setScale(1f, 1f, (mTextureView.width / 2).toFloat(), 0f)
                mTextureView.setTransform(transform)
                changeTransformView!!.text = "镜像操作"
                mTextureView.invalidate()
            }
        }
    }

    /**
     * 处理旋转逻辑
     */
    private fun resolveRotateUI() {
        if (!mHadPlay) {
            return
        }
        mTextureView.rotation = mRotate.toFloat()
        mTextureView.requestLayout()
    }

    /**
     * 处理显示比例，注意，GSYVideoType.setShowType是全局静态生效，除非重启APP。
     */
    private fun resolveTypeUI() {
        if (!mHadPlay) {
            return
        }
        if (mType == 1) {
            moreScaleView!!.text = "16:9"
            GSYVideoType.setShowType(GSYVideoType.SCREEN_TYPE_16_9)
        } else if (mType == 2) {
            moreScaleView!!.text = "4:3"
            GSYVideoType.setShowType(GSYVideoType.SCREEN_TYPE_4_3)
        } else if (mType == 3) {
            moreScaleView!!.text = "全屏"
            GSYVideoType.setShowType(GSYVideoType.SCREEN_TYPE_FULL)
        } else if (mType == 4) {
            moreScaleView!!.text = "拉伸全屏"
            GSYVideoType.setShowType(GSYVideoType.SCREEN_MATCH_FULL)
        } else if (mType == 0) {
            moreScaleView!!.text = "默认比例"
            GSYVideoType.setShowType(GSYVideoType.SCREEN_TYPE_DEFAULT)
        }
        changeTextureViewShowType()
        if (mTextureView != null)
            mTextureView.requestLayout()
    }

    override fun touchSurfaceMove(deltaX: Float, deltaY: Float, y: Float) {
        //未全屏时滑动切换上下集(在VideoPlayerActivity中控制)，全屏时改变亮度音量
        if (!mIfCurrentIsFullscreen) {
            if (mChangeVolume || mBrightness) {
                return
            }
        }
        super.touchSurfaceMove(deltaX, deltaY, y)
    }

    var surfaceTouchListener: OnTouchListener? = null
    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        if (v?.id == R.id.surface_container) {
            surfaceTouchListener?.onTouch(v, event) //增加对外开放的Touch事件处理方式，但不覆盖原方式
        }
        return super.onTouch(v, event)
    }
}
