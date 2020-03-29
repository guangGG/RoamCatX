package gapp.season.mediastore

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.tencent.mmkv.MMKV
import gapp.season.encryptlib.hash.HashUtil
import gapp.season.util.task.OnTaskDone
import io.reactivex.Single
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.io.File

object MediaStoreHelper {
    private const val MMKV_VIDEO_DURATION = "video_duration_" //缓存视频时长
    const val INTENT_EXTRA_TYPE = "intent_extra_type"
    const val INTENT_EXTRA_BUCKET_ID = "intent_extra_bucket_id"
    const val INTENT_EXTRA_BUCKET_NAME = "intent_extra_bucket_name"
    const val INTENT_EXTRA_TYPE_IMAGE = 0
    const val INTENT_EXTRA_TYPE_VIDEO = 1
    const val INTENT_EXTRA_TYPE_MUSIC = 2

    fun showGallery(context: Context) {
        val intent = Intent(context, MediaBucketActivity::class.java)
        intent.putExtra(INTENT_EXTRA_TYPE, INTENT_EXTRA_TYPE_IMAGE)
        if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun showVideos(context: Context) {
        val intent = Intent(context, MediaBucketActivity::class.java)
        intent.putExtra(INTENT_EXTRA_TYPE, INTENT_EXTRA_TYPE_VIDEO)
        if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun showMusics(context: Context) {
        //Audio库不存在Bucket逻辑，直接展示全部音乐列表
        showMediaFiles(context, INTENT_EXTRA_TYPE_MUSIC, null, "音乐库")
    }

    fun showMediaFiles(context: Context, type: Int, bucketId: String?, bucketName: String?) {
        val intent = Intent(context, MediaFilesActivity::class.java)
        intent.putExtra(INTENT_EXTRA_TYPE, type)
        intent.putExtra(INTENT_EXTRA_BUCKET_ID, bucketId)
        intent.putExtra(INTENT_EXTRA_BUCKET_NAME, bucketName)
        if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun getVideoDuration(file: File?, cd: CompositeDisposable?, onTaskDone: OnTaskDone<Long>?) {
        Single.fromCallable {
            if (file != null) {
                //优先从缓存中读取视频长度，读取不到则从文件读取并缓存下来
                val key = HashUtil.md5(file.absolutePath + "|" + file.length())
                val duCache = MMKV.mmkvWithID(MMKV_VIDEO_DURATION).decodeLong(MMKV_VIDEO_DURATION + key, -1)
                if (duCache >= 0) {
                    return@fromCallable duCache
                } else {
                    val du = MediaLoader.getVideoDuration(file.absolutePath)
                    MMKV.mmkvWithID(MMKV_VIDEO_DURATION).encode(MMKV_VIDEO_DURATION + key, du)
                    return@fromCallable du
                }
            } else {
                return@fromCallable -1L
            }
        }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : SingleObserver<Long> {
                    override fun onSuccess(t: Long) {
                        onTaskDone?.onTaskDone(0, null, t)
                    }

                    override fun onSubscribe(d: Disposable) {
                        cd?.add(d)
                    }

                    override fun onError(e: Throwable) {
                        onTaskDone?.onTaskDone(-1, null, -1)
                    }
                })
    }
}
