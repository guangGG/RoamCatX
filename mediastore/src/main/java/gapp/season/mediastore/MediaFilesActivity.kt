package gapp.season.mediastore

import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.chad.library.adapter.base.BaseViewHolder
import com.chad.library.adapter.base.MultipleItemRvAdapter
import com.chad.library.adapter.base.provider.BaseItemProvider
import gapp.season.imageviewer.ImageViewerHelper
import gapp.season.musicplayer.MusicPlayerHelper
import gapp.season.util.file.MediaScanUtil
import gapp.season.util.sys.MemoryUtil
import gapp.season.util.task.OnTaskDone
import gapp.season.util.tips.ToastUtil
import gapp.season.util.view.ThemeUtil
import gapp.season.videoplayer.VideoPlayerHelper
import io.reactivex.Single
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.media_activity_files.*

class MediaFilesActivity : AppCompatActivity() {
    private var compositeDisposable: CompositeDisposable? = null
    private var mediaType = 0
    private var bucketId: String? = null
    private var adapter: Adapter? = null
    private var mediaObserver: ContentObserver? = null
    private var pageTilte: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getIntentData(intent)
        compositeDisposable = CompositeDisposable()
        ThemeUtil.setTheme(this, 0)
        setContentView(R.layout.media_activity_files)
        mediaTitle.text = pageTilte
        mediaBack.setOnClickListener { finish() }
        mediaMenu.setOnClickListener { loadData() }

        val dm = resources.displayMetrics
        val spanCount = (dm.widthPixels / dm.density / 89).toInt()
        val layoutManager = GridLayoutManager(this, spanCount)
        mediaList.layoutManager = layoutManager
        adapter = Adapter(mediaType)
        mediaList.adapter = adapter
        //注意：必须设置Adapter后再设置SpanSizeLookup才会生效
        //(使用BaseQuickAdapter.setSpanSizeLookup方法和GridLayoutManager.setSpanSizeLookup方法等效)
        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                val item = adapter?.getItem(position)
                if (item is MediaFileItem) {
                    return if (mediaType == MediaStoreHelper.INTENT_EXTRA_TYPE_MUSIC || item.isTitleTag) layoutManager.spanCount else 1 //设置音乐及标题占一整行的宽度
                }
                return 0
            }
        }
        adapter?.setOnItemClickListener { adapter, _, position ->
            val item = adapter.getItem(position)
            if (item is MediaFileItem && !item.isTitleTag && item.file != null) {
                when {
                    VideoPlayerHelper.isVideoFile(item.file) -> VideoPlayerHelper.play(this, item.file.absolutePath)
                    ImageViewerHelper.isImage(item.file) -> ImageViewerHelper.showImage(this, item.file.absolutePath)
                    MusicPlayerHelper.isMusicFile(item.file) -> MusicPlayerHelper.play(this, item.file.absolutePath)
                }
            }
        }
        adapter?.setOnItemLongClickListener { adapter, _, position ->
            val item = adapter.getItem(position)
            if (item is MediaFileItem && !item.isTitleTag && item.file != null) {
                if (ImageViewerHelper.isImage(item.file)) {
                    ImageViewerHelper.showImageInfo(this, item.file)
                } else {
                    val builder = AlertDialog.Builder(this)
                            .setMessage("文件路径：" + item.file.absoluteFile + "\n文件大小："
                                    + MemoryUtil.formatMemorySize(item.file?.length() ?: 0, 2)
                                    + (if (item.file.exists()) "" else "\n注：文件不存在"))
                            .setPositiveButton("确定", null)
                    when (mediaType) {
                        MediaStoreHelper.INTENT_EXTRA_TYPE_VIDEO -> builder.setTitle("视频信息")
                        MediaStoreHelper.INTENT_EXTRA_TYPE_MUSIC -> builder.setTitle("音乐信息")
                        else -> builder.setTitle("图片信息")
                    }
                    //文件不存在但在媒体库有记录的，使用MediaScannerConnection发送命令更新媒体库文件
                    if (!item.file.exists()) builder.setNeutralButton("移除不存在的文件") { _, _ ->
                        adapter.data.forEach {
                            if (it is MediaFileItem) {
                                MediaScanUtil.scanFile(it.file?.absolutePath)
                            }
                        }
                    }
                    builder.show()
                }
            }
            return@setOnItemLongClickListener true
        }

        loadData()

        // 监听系统媒体库变化
        mediaObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri) {
                loadData()
            }
        }
        val uri = when (mediaType) {
            MediaStoreHelper.INTENT_EXTRA_TYPE_VIDEO -> MediaLoader.MEDIA_VIDEO_URI
            MediaStoreHelper.INTENT_EXTRA_TYPE_MUSIC -> MediaLoader.MEDIA_AUDIO_URI
            else -> MediaLoader.MEDIA_IMAGE_URI
        }
        contentResolver.registerContentObserver(uri, false, mediaObserver!!)
    }

    override fun onDestroy() {
        if (mediaObserver != null) contentResolver.unregisterContentObserver(mediaObserver!!)
        compositeDisposable?.dispose()
        super.onDestroy()
    }

    private fun getIntentData(intent: Intent?) {
        mediaType = intent?.getIntExtra(MediaStoreHelper.INTENT_EXTRA_TYPE, 0) ?: 0
        bucketId = intent?.getStringExtra(MediaStoreHelper.INTENT_EXTRA_BUCKET_ID)
        pageTilte = intent?.getStringExtra(MediaStoreHelper.INTENT_EXTRA_BUCKET_NAME) ?: ""
    }

    private fun loadData() {
        val uri = when (mediaType) {
            MediaStoreHelper.INTENT_EXTRA_TYPE_VIDEO -> MediaLoader.MEDIA_VIDEO_URI
            MediaStoreHelper.INTENT_EXTRA_TYPE_MUSIC -> MediaLoader.MEDIA_AUDIO_URI
            else -> MediaLoader.MEDIA_IMAGE_URI
        }
        Single.fromCallable {
            //音乐列表无拍摄时间分割
            val joinTiteItem = (mediaType != MediaStoreHelper.INTENT_EXTRA_TYPE_MUSIC)
            MediaLoader.getBucketFilesSync(this, uri, bucketId, joinTiteItem)
        }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : SingleObserver<List<MediaFileItem>> {
                    override fun onSuccess(t: List<MediaFileItem>) {
                        adapter?.setNewData(t)
                        var count = 0
                        t.forEach {
                            if (!it.isTitleTag) count++
                        }
                        mediaTitle.text = String.format("%s(%d)", pageTilte, count)
                    }

                    override fun onSubscribe(d: Disposable) {
                        compositeDisposable?.add(d)
                    }

                    override fun onError(e: Throwable) {
                        ToastUtil.showLong("加载媒体库失败")
                    }
                })
    }

    class Adapter(private var mediaType: Int) : MultipleItemRvAdapter<MediaFileItem, BaseViewHolder>(null) {
        init {
            finishInitialize()
        }

        override fun getViewType(t: MediaFileItem?): Int {
            return when {
                t?.isTitleTag == true -> 1
                mediaType == MediaStoreHelper.INTENT_EXTRA_TYPE_MUSIC -> 2
                else -> 0
            }
        }

        override fun registerItemProvider() {
            mProviderDelegate.registerProvider(object : BaseItemProvider<MediaFileItem, BaseViewHolder>() {
                override fun viewType(): Int {
                    return 0
                }

                override fun layout(): Int {
                    return R.layout.media_item_file_grid
                }

                override fun convert(helper: BaseViewHolder, item: MediaFileItem?, position: Int) {
                    if (item != null) {
                        Glide.with(mContext).load(item.file).placeholder(R.drawable.media_image).into(helper.getView(R.id.media_file_icon))
                        val tv: TextView = helper.getView(R.id.media_file_duration)
                        val path = item.file.absolutePath
                        tv.tag = path
                        tv.visibility = View.GONE
                        if (mediaType == MediaStoreHelper.INTENT_EXTRA_TYPE_VIDEO) {
                            //加载视频时长(对加载过的时长进行缓存)
                            MediaStoreHelper.getVideoDuration(item.file, (mContext as MediaFilesActivity).compositeDisposable,
                                    OnTaskDone { _, _, duration ->
                                        if (tv.tag == path) {
                                            val duStr = formatDuration(duration)
                                            if (duStr.isNotEmpty()) {
                                                tv.visibility = View.VISIBLE
                                                tv.text = duStr
                                            }
                                        }
                                    })
                        }
                    }
                }
            })
            mProviderDelegate.registerProvider(object : BaseItemProvider<MediaFileItem, BaseViewHolder>() {
                override fun viewType(): Int {
                    return 1
                }

                override fun layout(): Int {
                    return R.layout.media_item_file_title
                }

                override fun convert(helper: BaseViewHolder, item: MediaFileItem?, position: Int) {
                    if (item != null) {
                        val title = if (item.dateTag?.length == 8) {
                            item.dateTag.substring(0, 4) + "年" + item.dateTag.substring(4, 6) + "月" + item.dateTag.substring(6) + "日"
                        } else item.dateTag
                        helper.setText(R.id.media_file_title, title)
                    }
                }
            })
            mProviderDelegate.registerProvider(object : BaseItemProvider<MediaFileItem, BaseViewHolder>() {
                override fun viewType(): Int {
                    return 2
                }

                override fun layout(): Int {
                    return R.layout.media_item_file_music
                }

                override fun convert(helper: BaseViewHolder, item: MediaFileItem?, position: Int) {
                    if (item != null) {
                        helper.setText(R.id.media_file_name, "${position + 1}.${item.file.name}")
                    }
                }
            })
        }

        private fun formatDuration(du: Long?): String {
            if (du == null || du <= 0) return ""
            val duration = du / 1000
            val sb = StringBuilder()
            val s: Long
            val f: String
            val m: String
            s = duration / 3600
            if (s > 0) {
                sb.append(s).append(":")
            }
            f = (duration % 3600 / 60).toString()
            if (f.length == 1) {
                sb.append("0")
            }
            sb.append(f).append(":")
            m = (duration % 3600 % 60).toString()
            if (m.length == 1) {
                sb.append("0")
            }
            sb.append(m)
            return sb.toString()
        }
    }
}
