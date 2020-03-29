@file:Suppress("DEPRECATION", "SetTextI18n")

package gapp.season.imageviewer

import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.bumptech.glide.Glide
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.github.piasy.biv.view.BigImageView
import com.github.piasy.biv.view.GlideImageViewFactory
import gapp.season.util.file.FileShareUtil
import gapp.season.util.file.ImgUtil
import gapp.season.util.file.MediaScanUtil
import gapp.season.util.sys.MemoryUtil
import gapp.season.util.view.ThemeUtil
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.imgv_activity.*
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit

class ImageViewerActivity : AppCompatActivity() {
    private var uris: MutableList<Uri>? = null
    private var index = 0
    private var adapterBiv: BivPagerAdapter? = null //图片ViewPager
    private var adapter: Adapter? = null //缩略图列表
    private var hideBar = false

    //幻灯片播放
    private var intervalIndex = 3
    private var interval = 3000L
    private var slideRight = true
    private var slideDisposable: Disposable? = null
    private var compositeDisposable: CompositeDisposable? = null

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        getIntentData(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeUtil.setFullScreenTheme(this, 0, Color.BLACK)
        setContentView(R.layout.imgv_activity)

        val layoutManager = LinearLayoutManager(this)
        layoutManager.orientation = RecyclerView.HORIZONTAL
        imgvImgs.layoutManager = layoutManager
        adapter = Adapter()
        imgvImgs.adapter = adapter
        adapter?.setOnItemClickListener { _, _, position ->
            index = position
            showImage()
        }

        val longClick = View.OnLongClickListener {
            showMenu()
            return@OnLongClickListener true
        }
        imgvCloseBtn.setOnClickListener { finish() }
        imgvMenuBtn.setOnClickListener { showMenu() }

        adapterBiv = object : BivPagerAdapter() {
            override fun isViewFromObject(view: View, obj: Any): Boolean {
                return view == obj
            }

            override fun getCount(): Int {
                return uris?.size ?: 0
            }

            override fun instantiateItem(container: ViewGroup, position: Int): Any {
                var biv = ViewPagerCacheBuffer.bivCaches.pop()
                if (biv == null) {
                    biv = BigImageView(this@ImageViewerActivity)
                    biv.setFailureImage(resources.getDrawable(R.drawable.imgv_ic_image_broken))
                    biv.setOptimizeDisplay(true)
                    biv.setTapToRetry(false)
                    biv.setImageViewFactory(GlideImageViewFactory())
                    biv.setImageLoaderCallback(null) //图片加载的回调在setImageLoaderCallback中处理
                    biv.setOnClickListener {
                        if (slideDisposable != null) {
                            stopSlide()
                        } else {
                            if (hideBar) {
                                imgvToolBar.visibility = View.VISIBLE
                                imgvImgs.visibility = View.VISIBLE
                            } else {
                                imgvToolBar.visibility = View.GONE
                                imgvImgs.visibility = View.GONE
                            }
                            hideBar = !hideBar
                        }
                    }
                    biv.setOnLongClickListener(longClick)
                }
                loadImage(biv, position)
                container.addView(biv)
                return biv
            }

            override fun destroyItem(container: ViewGroup, position: Int, obj: Any) {
                val biv = obj as BigImageView
                biv.showImage(ImageViewerHelper.resourceIdToUri(this@ImageViewerActivity, R.drawable.imgv_ic_image))
                ViewPagerCacheBuffer.bivCaches.push(biv)
                container.removeView(biv)
            }

            override fun setPrimaryItem(container: ViewGroup, position: Int, obj: Any) {
                //setPrimaryItem只会设置已选中的page，但比onPageSelected回调会有延迟
                biv = obj as BigImageView
                bivIndex = position
                if (bivIndex != showingIndex) {
                    showImage()
                }
            }
        }
        imgvViewPager.adapter = adapterBiv
        imgvViewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {
            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
            }

            override fun onPageSelected(position: Int) {
                index = position
                showImage()
            }
        })

        getIntentData(intent)

        compositeDisposable = CompositeDisposable()
    }

    override fun onDestroy() {
        compositeDisposable?.dispose()
        super.onDestroy()
    }

    private fun getIntentData(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            val filePath = intent.data?.path ?: ""
            loadFileImage(filePath)
        } else {
            when (intent?.extras?.getInt(ImageViewerHelper.INTENT_TYPE) ?: -1) {
                ImageViewerHelper.INTENT_TYPE_SINGLE ->
                    loadFileImage(ImageViewerHelper.imgPath ?: "")
                ImageViewerHelper.INTENT_TYPE_MULTIPLE -> {
                    val uris = mutableListOf<Uri>()
                    ImageViewerHelper.imgPaths?.forEach {
                        uris.add(Uri.fromFile(File(it)))
                    }
                    this.uris = uris
                    this.index = ImageViewerHelper.index
                    adapterBiv?.notifyDataSetChanged()
                    showImage()
                }
                else ->  //加载上次查看的图片
                    loadFileImage(ImageViewerHelper.getSavedPath(applicationContext) ?: "")
            }
        }
    }

    private fun loadFileImage(filePath: String) {
        val file = File(filePath)
        if (file.exists()) {
            val uris = mutableListOf<Uri>()
            var index = 0
            var count = 0
            file.parentFile?.listFiles()?.forEach {
                if (ImageViewerHelper.isImage(it) || it == file) {
                    uris.add(Uri.fromFile(it))
                    if (it == file) index = count
                    count++
                }
            }
            this.uris = uris
            this.index = index
            adapterBiv?.notifyDataSetChanged()
            showImage()
        }
    }

    private fun showPre() {
        index--
        showImage()
    }

    private fun showNext() {
        index++
        showImage()
    }

    private fun showImage() {
        if (!uris.isNullOrEmpty()) {
            if (index < 0) index = uris!!.size - 1
            if (index >= uris!!.size) index = 0
            imgvViewPager.currentItem = index
            if (adapterBiv?.bivIndex == index && adapterBiv?.showingIndex != index) {
                ImageViewerHelper.savePath(applicationContext, uris?.getOrNull(index)?.path)
                loadImage(adapterBiv?.biv, index)
                adapterBiv?.showingIndex = index
            }
            adapter?.setNewData(uris)
            imgvImgs.scrollToPosition(index)
            updateToolBar()
        }
    }

    private fun loadImage(biv: BigImageView?, index: Int) {
        if (uris != null && index >= 0 && index < uris!!.size) {
            val uri = uris!![index]
            if (uri != biv?.tag) { //展示同一张图片时放闪动
                val thumbnail = ImageViewerHelper.resourceIdToUri(this, R.drawable.imgv_ic_image)
                biv?.showImage(thumbnail, uri)
                biv?.tag = uri
            }
        }
    }

    private fun updateToolBar() {
        val file = File(uris?.getOrNull(index)?.path ?: "")
        val dirName = (file.parentFile?.name ?: "")
        val no = if (uris.isNullOrEmpty()) "" else ("(" + (index + 1) + "/" + uris?.size + ")")
        imgvTitle.text = dirName + no + "\n" + file.name
        val fileSize = "[" + MemoryUtil.formatMemorySize(file.length(), 2) + "]"
        val rect = ImgUtil.getImageWidthHeight(file.absolutePath)
        val imgSize = "[" + rect.width() + "*" + rect.height() + "]"
        imgvSubTitle.text = fileSize + "\n" + imgSize
    }

    private fun showMenu() {
        val file = File(uris?.getOrNull(index)?.path ?: "")
        AlertDialog.Builder(this).setItems(arrayOf("查看图片详情", "旋转屏幕", "分享图片", "删除图片", "幻灯片播放"))
        { _, index ->
            when (index) {
                0 -> ImageViewerHelper.showImageInfo(this, file, ImageViewerHelper.isDev, ImageViewerHelper.callBack)
                1 -> {
                    // 改变屏幕方向
                    var orientation = requestedOrientation
                    orientation = when (orientation) {
                        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        else -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    }
                    requestedOrientation = orientation //会执行onDestroy、onCreate等生命周期方法
                }
                2 -> {
                    FileShareUtil.allowFileUriExposure() //分享前必须执行本代码，主要用于兼容高版本系统的FileUriExposedException
                    val intent = Intent()
                    intent.addCategory("android.intent.category.DEFAULT")
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    intent.action = Intent.ACTION_VIEW
                    val type = "image/*"
                    intent.setDataAndType(Uri.fromFile(file), type)
                    /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        //注：FileUriExposedException，Android 7.0以上不允许intent带有file://的URI离开自身的应用
                        val uri = FileProvider.getUriForFile(this, packageName, file)
                        intent.setDataAndType(uri, type) //实际大多数应用并不支持通过content://协议分享的文件
                    }*/
                    //指定应用打开: intent.setComponent(new ComponentName(pkg, cls));
                    startActivity(intent)
                }
                3 -> {
                    AlertDialog.Builder(this)
                            .setTitle("删除图片")
                            .setMessage("您确认要删除图片“" + file.absolutePath + "”吗？")
                            .setPositiveButton("确定") { _, _ ->
                                // 删除图片
                                if (file.delete()) {
                                    try {
                                        uris?.removeAt(this.index)
                                        adapterBiv?.showingIndex = -1
                                        adapterBiv?.notifyDataSetChanged()
                                        showImage()
                                    } catch (e: Exception) {
                                    }
                                }
                                // 更新系统图库
                                MediaScanUtil.scanFile(file.absolutePath)
                                // 若列表中图片删除完了关闭页面
                                if (uris.isNullOrEmpty()) {
                                    finish()
                                }
                            }.setNegativeButton("取消", null)
                            .show()
                }
                4 -> {
                    AlertDialog.Builder(this)
                            .setTitle("幻灯片播放")
                            .setSingleChoiceItems(arrayOf("间隔0.5秒", "间隔1秒", "间隔2秒", "间隔3秒", "间隔4秒", "间隔5秒", "间隔6秒", "间隔7秒"),
                                    intervalIndex) { _, whitch ->
                                intervalIndex = whitch
                                interval = (if (whitch < 1) 500 else (whitch * 1000)).toLong()
                            }.setPositiveButton("向右播放") { _, _ ->
                                if (uris?.size ?: 0 > 1) {
                                    slideRight = true
                                    startSlide()
                                }
                            }.setNegativeButton("向左播放") { _, _ ->
                                if (uris?.size ?: 0 > 1) {
                                    slideRight = false
                                    startSlide()
                                }
                            }.setNeutralButton("取消", null)
                            .show()
                }
            }
        }.show()
    }

    private fun startSlide() {
        if (slideDisposable != null) compositeDisposable?.remove(slideDisposable!!)
        slideDisposable = Observable.interval(interval, interval, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    if (slideRight) showNext() else showPre()
                }, {})
        compositeDisposable?.add(slideDisposable!!)
    }

    private fun stopSlide() {
        if (slideDisposable != null) {
            compositeDisposable?.remove(slideDisposable!!)
            slideDisposable = null
        }
    }

    class Adapter : BaseQuickAdapter<Uri, BaseViewHolder>(R.layout.imgv_item_thumbnail) {
        override fun convert(helper: BaseViewHolder, item: Uri?) {
            if (item != null) {
                Glide.with(mContext).load(item).placeholder(R.drawable.imgv_ic_image).into(helper.getView(R.id.imgv_thumb))
                val selected = ((mContext as ImageViewerActivity).index == helper.adapterPosition)
                helper.setBackgroundColor(R.id.imgv_bg, (if (!selected) Color.TRANSPARENT else 0xffdd9966.toInt()))
            }
        }
    }

    private abstract class BivPagerAdapter : PagerAdapter() {
        var biv: BigImageView? = null
        var bivIndex: Int = -1
        var showingIndex: Int = -1
    }

    private object ViewPagerCacheBuffer {
        // 为ViewPager增加回收view的缓存机制
        var bivCaches = NoExceptionStack<BigImageView>()
    }

    /**
     * 把Stack抛异常的情况返回为null
     */
    private class NoExceptionStack<E> : Stack<E>() {
        @Synchronized
        override fun pop(): E? {
            try {
                return super.pop()
            } catch (e: Exception) {
                //e.printStackTrace() //EmptyStackException
            }
            return null
        }
    }
}
