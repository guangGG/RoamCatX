package gapp.season.roamcat

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.multidex.MultiDex
import com.didichuxing.doraemonkit.DoraemonKit
import com.rx2androidnetworking.Rx2AndroidNetworking
import gapp.season.filemanager.FileManager
import gapp.season.fileselector.FileSelectorHelper
import gapp.season.imageviewer.ImageViewerHelper
import gapp.season.manageapps.ManageAppsHelper
import gapp.season.musicplayer.MusicPlayerHelper
import gapp.season.notepad.NoteHelper
import gapp.season.poem.PoemReader
import gapp.season.qrcode.QrcodeHelper
import gapp.season.reader.BookReader
import gapp.season.roamcat.data.file.MmkvUtil
import gapp.season.roamcat.data.net.AppNetwork
import gapp.season.roamcat.data.runtime.ClipboardHelper
import gapp.season.roamcat.data.runtime.LanguageHelper
import gapp.season.roamcat.data.runtime.PluginHelper
import gapp.season.roamcat.util.SchedulersUtil
import gapp.season.star.SkyStar
import gapp.season.star.solar.Solar
import gapp.season.textviewer.TextViewerHelper
import gapp.season.util.SeasonUtil
import gapp.season.util.file.FileShareUtil
import gapp.season.util.log.LogUtil
import gapp.season.util.task.OnTaskDone
import gapp.season.videoplayer.VideoPlayerHelper
import gapp.season.webbrowser.WebViewHelper
import io.reactivex.SingleObserver
import io.reactivex.disposables.Disposable
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@SuppressLint("LogNotTimber")
class App : Application() {
    companion object {
        var instance: App? = null
        var baseDir: String? = null
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("AppTag", "App onCreate")
        instance = this
        val baseDirFile = File(Environment.getExternalStorageDirectory(), "mine/RoamCatX/")
        baseDirFile.mkdirs()
        baseDir = baseDirFile.absolutePath

        SeasonUtil.init(this, BuildConfig.DEV)
        MmkvUtil.init(this)
        AppNetwork.init(this)
        initDoraemonKit(this)
        initPlugins()
        PluginHelper.init(this)
        ClipboardHelper.init(this)
    }

    private fun initDoraemonKit(app: Application) {
        DoraemonKit.disableUpload()
        DoraemonKit.install(app)
        DoraemonKit.setWebDoorCallback { context, url ->
            WebViewHelper.showWebPage(context, url)
        }
        DoraemonKit.hide()
    }

    private fun initPlugins() {
        //WebBrowser
        WebViewHelper.init(object : WebViewHelper.Config {
            override fun getBaseDir(): String {
                return baseDir!!
            }

            override fun isDev(): Boolean {
                return BuildConfig.DEV
            }

            override fun getCallBack(): OnTaskDone<Any>? {
                return OnTaskDone { code, _, data ->
                    when (code) {
                        WebViewHelper.EVENT_ON_CREATE -> if (data is ComponentActivity) {
                            //data.lifecycle.addObserver(observer)
                            LogUtil.d("Web Page onCreate")
                        }
                        WebViewHelper.EVENT_ON_DESTROY -> if (data is ComponentActivity) {
                            //data.lifecycle.removeObserver(observer)
                            LogUtil.d("Web Page onDestroy")
                        }
                    }
                }
            }

            override fun attachBaseContext(context: Context?): Context? {
                return LanguageHelper.getLanguageContext(context!!)
            }

            override fun getRemoveAdUrl(): String? {
                return null //这里可以配置去广告js脚本
            }
        })
        //SkyStar
        SkyStar.config(BuildConfig.DEV, 0.0, 0)
        Rx2AndroidNetworking.get(AppNetwork.API_SOLAR_DATUM_DATE) //设置基准日期(从服务器接口拉取)
                .build().jsonObjectSingle
                .subscribeOn(SchedulersUtil.io())
                .observeOn(SchedulersUtil.ui())
                .subscribe(object : SingleObserver<JSONObject> {
                    override fun onSubscribe(d: Disposable) {
                    }

                    override fun onError(e: Throwable) {
                    }

                    override fun onSuccess(obj: JSONObject) {
                        try {
                            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)
                            putDatumDate(Solar.Moon, obj, "moon", sdf)
                            putDatumDate(Solar.Mercury, obj, "mercury", sdf)
                            putDatumDate(Solar.Venus, obj, "venus", sdf)
                            putDatumDate(Solar.Mars, obj, "mars", sdf)
                            putDatumDate(Solar.Jupiter, obj, "jupiter", sdf)
                            putDatumDate(Solar.Saturn, obj, "saturn", sdf)
                            putDatumDate(Solar.Uranus, obj, "uranus", sdf)
                            putDatumDate(Solar.Neptune, obj, "neptune", sdf)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    private fun putDatumDate(solar: Solar, obj: JSONObject, key: String, sdf: SimpleDateFormat) {
                        if (obj.has(key)) SkyStar.putDatumDate(solar, sdf.parse(obj.getString(key)).time)
                    }
                })
        //BookReader
        BookReader.config(BuildConfig.DEV, 0, baseDir)
        BookReader.setBrListener(object : BookReader.BrListener { //设置文件导入方式，不设置则使用系统默认方式
            override fun importBook(activity: Activity?, requestCode: Int) {
                FileSelectorHelper.selectFile(activity!!, requestCode)
            }

            override fun onImportBook(activity: Activity?, intent: Intent?): String {
                return FileShareUtil.getPath(activity, intent?.data)
            }
        })
        //PoemReader
        PoemReader.config(BuildConfig.DEV, 0)
        //VideoPlayer
        VideoPlayerHelper.init(this, BuildConfig.DEV)
        VideoPlayerHelper.listener = object : VideoPlayerHelper.VideoPlayerListener {
            override fun import(activity: Activity, requestCode: Int): Boolean {
                return FileSelectorHelper.selectFile(activity, requestCode)
            }

            override fun onImport(activity: Activity, data: Intent): String? {
                return FileShareUtil.getPath(activity, data.data)
            }
        }
        //Qrcode
        QrcodeHelper.init(object : QrcodeHelper.QrcodeListener {
            override fun onScanQRCodeSuccess(context: Context, result: String?): Boolean {
                return false //使用默认处理方式
            }

            override fun onBrowseResult(context: Context, result: String?): Boolean {
                WebViewHelper.showWebPage(context, result)
                return true //拦截并自定义浏览扫描结果
            }

            override fun getFilePath(context: Context, uri: Uri?): String? {
                return FileShareUtil.getPath(context, uri)
            }
        })
        //ManageApps
        ManageAppsHelper.init(this)
        //ImageViewer
        ImageViewerHelper.init(this, BuildConfig.DEV, OnTaskDone { code, _, data ->
            if (code == ImageViewerHelper.TASK_CODE_IMG_POSITION) {
                //定位到文件，data为文件路径
                FileManager.enter(instance!!, data as String?)
            }
        })
        //FileSelector
        FileSelectorHelper.listener = object : FileSelectorHelper.FileSelectorListener {
            override fun onClickFile(activity: Activity, file: File) {
                when {
                    ImageViewerHelper.isImage(file) -> ImageViewerHelper.showImage(activity, file.absolutePath)
                    VideoPlayerHelper.isVideoFile(file) -> VideoPlayerHelper.play(activity, file.absolutePath)
                    TextViewerHelper.isTextFile(file) -> TextViewerHelper.show(activity, file.absolutePath)
                    MusicPlayerHelper.isMusicFile(file) -> MusicPlayerHelper.play(activity, file.absolutePath)
                    else -> {
                        TextViewerHelper.show(activity, file.absolutePath)
                    }
                }
            }
        }
        //MusicPlayer
        MusicPlayerHelper.init(this, "$baseDir/music")
        MusicPlayerHelper.listener = object : MusicPlayerHelper.MusicPlayerListener {
            override fun import(activity: Activity, requestCode: Int): Boolean {
                return FileSelectorHelper.selectFile(activity, requestCode)
            }

            override fun onImport(activity: Activity, data: Intent): String? {
                return FileShareUtil.getPath(activity, data.data)
            }
        }
        //NotePad
        NoteHelper.init(this, "$baseDir/note")
    }

    override fun attachBaseContext(base: Context?) {
        Log.d("AppTag", "App attachBaseContext") //attachBaseContext会在onCreate前调用
        MmkvUtil.init(base!!) //LanguageHelper获取语言配置信息时会用到MmkvUtil，需提前初始化
        super.attachBaseContext(LanguageHelper.getLanguageContext(base))
        MultiDex.install(base)
    }
}
