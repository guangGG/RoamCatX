package gapp.season.mediastore

import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import gapp.season.util.tips.ToastUtil
import gapp.season.util.view.ThemeUtil
import io.reactivex.Single
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.media_activity_bucket.*
import java.io.File

class MediaBucketActivity : AppCompatActivity() {
    private var compositeDisposable: CompositeDisposable? = null
    private var mediaType = 0
    private var adapter: Adapter? = null
    private var mediaObserver: ContentObserver? = null
    private var pageTilte: String = ""

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        getIntentData(intent)
        loadData()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getIntentData(intent)
        compositeDisposable = CompositeDisposable()
        ThemeUtil.setTheme(this, 0)
        setContentView(R.layout.media_activity_bucket)
        mediaBack.setOnClickListener { finish() }
        mediaMenu.setOnClickListener {
            val name = when (mediaType) {
                MediaStoreHelper.INTENT_EXTRA_TYPE_VIDEO -> "视频"
                MediaStoreHelper.INTENT_EXTRA_TYPE_MUSIC -> "音乐"
                else -> "相册"
            }
            MediaStoreHelper.showMediaFiles(this, mediaType, null, name)
        }

        val dm = resources.displayMetrics
        val spanCount = (dm.widthPixels / dm.density / 89).toInt()
        val layoutManager = GridLayoutManager(this, spanCount)
        mediaList.layoutManager = layoutManager
        adapter = Adapter(mediaType)
        mediaList.adapter = adapter
        adapter?.setOnItemClickListener { adapter, _, position ->
            val item = adapter.getItem(position)
            if (item is MediaBucket) {
                MediaStoreHelper.showMediaFiles(this, mediaType, item.id, item.name)
            }
        }
        adapter?.setOnItemLongClickListener { adapter, _, position ->
            val item = adapter.getItem(position)
            if (item is MediaBucket) {
                AlertDialog.Builder(this)
                        .setMessage("目录名称：" + item.name + "\n文件数量：" + item.count)
                        .setPositiveButton("确定", null)
                        .show()
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
    }

    private fun loadData() {
        pageTilte = when (mediaType) {
            MediaStoreHelper.INTENT_EXTRA_TYPE_VIDEO -> "视频库"
            MediaStoreHelper.INTENT_EXTRA_TYPE_MUSIC -> "音乐库"
            MediaStoreHelper.INTENT_EXTRA_TYPE_IMAGE -> "图库"
            else -> "媒体库"
        }
        mediaTitle.text = pageTilte
        Single.fromCallable {
            when (mediaType) {
                MediaStoreHelper.INTENT_EXTRA_TYPE_VIDEO -> return@fromCallable MediaLoader.loadVideoBucketsSync(this)
                MediaStoreHelper.INTENT_EXTRA_TYPE_MUSIC -> return@fromCallable MediaLoader.loadAudioBucketsSync(this)
                else -> return@fromCallable MediaLoader.loadImageBucketsSync(this)
            }
        }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : SingleObserver<List<MediaBucket>> {
                    override fun onSuccess(t: List<MediaBucket>) {
                        adapter?.setNewData(t)
                        mediaTitle.text = String.format("%s(%d)", pageTilte, t.size)
                    }

                    override fun onSubscribe(d: Disposable) {
                        compositeDisposable?.add(d)
                    }

                    override fun onError(e: Throwable) {
                        ToastUtil.showLong("加载媒体库失败")
                    }
                })
    }

    class Adapter(private var mediaType: Int) : BaseQuickAdapter<MediaBucket, BaseViewHolder>(R.layout.media_item_bucket_grid) {
        override fun convert(helper: BaseViewHolder, item: MediaBucket?) {
            if (item != null) {
                helper.setText(R.id.media_bucket_count, item.count.toString())
                helper.setText(R.id.media_bucket_name, item.name)
                val file = File(item.tagPath ?: "")
                val placeHolder = if (mediaType == MediaStoreHelper.INTENT_EXTRA_TYPE_MUSIC) R.drawable.media_music else R.drawable.media_image
                Glide.with(mContext).load(file).placeholder(placeHolder).into(helper.getView(R.id.media_bucket_icon))
            }
        }
    }
}
