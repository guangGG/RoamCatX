package gapp.season.webbrowser

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Message
import android.provider.MediaStore
import android.text.TextUtils
import android.view.View
import android.webkit.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.interfaces.OnInputConfirmListener
import com.rx2androidnetworking.Rx2AndroidNetworking
import gapp.season.util.app.AppUtil
import gapp.season.util.file.FileUtil
import gapp.season.util.file.ImgUtil
import gapp.season.util.file.MediaScanUtil
import gapp.season.util.log.LogUtil
import gapp.season.util.sys.ClipboardUtil
import gapp.season.util.task.OnTaskDone
import gapp.season.util.tips.AlertUtil
import gapp.season.util.tips.ToastUtil
import gapp.season.util.view.ThemeUtil
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.web_activity_layout.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class WebActivity : AppCompatActivity() {
    companion object {
        private const val TAG: String = "WebActivityTag"
        private const val INTENT_EXTRA_KEY_URL: String = "key_url"
        private const val REQUEST_CODE_FILE_CHOOSER = 1

        fun navigation(context: Context, url: String?) {
            val bundle = Bundle()
            bundle.putString(INTENT_EXTRA_KEY_URL, url)
            val intent = Intent(context, WebActivity::class.java)
            intent.putExtras(bundle)
            if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeUtil.setTheme(this, 0, resources.getColor(R.color.web_color_primary)) //ImmersionBar与软键盘adjustResize设置有冲突
        WebViewHelper.readMarks(WebViewHelper.config!!) //app启动时可能还未获取到存储权限，故需要在Activity启动时重新读取一次
        setContentView(R.layout.web_activity_layout)
        checkBackOrForward()
        wbMoreBtn.setColorFilter(Color.WHITE)
        wbMarkBtn.setColorFilter(Color.WHITE)
        wbHomeBtn.setColorFilter(Color.WHITE)
        wbCloseBtn.setColorFilter(Color.WHITE)
        wbMenuIcon1.setColorFilter(Color.WHITE)
        wbMenuIcon2.setColorFilter(Color.WHITE)
        wbMenuIcon3.setColorFilter(Color.WHITE)
        wbMenuIcon4.setColorFilter(Color.WHITE)
        wbMenuIcon5.setColorFilter(Color.WHITE)
        wbMenuIcon6.setColorFilter(Color.WHITE)
        wbMenuIcon7.setColorFilter(Color.WHITE)
        wbMenuIcon8.setColorFilter(Color.WHITE)
        wbMenuIcon9.setColorFilter(Color.WHITE)
        wbBackBtn.setOnClickListener {
            goBack()
            closeMenu()
        }
        wbNextBtn.setOnClickListener {
            goForward()
            closeMenu()
        }
        wbMarkBtn.setOnClickListener {
            showMarks()
            closeMenu()
        }
        wbMoreBtn.setOnClickListener {
            if (wbMenuBar.visibility == View.VISIBLE)
                closeMenu()
            else
                wbMenuBar.visibility = View.VISIBLE
        }
        wbHomeBtn.setOnClickListener {
            goHome()
            closeMenu()
        }
        wbCloseBtn.setOnClickListener { finish() }
        wbMenuBar.setOnClickListener { closeMenu() }
        wbMenuAddMark.setOnClickListener {
            addMark()
            closeMenu()
        }
        wbMenuMarks.setOnClickListener {
            showMarks()
            closeMenu()
        }
        wbMenuHistory.setOnClickListener {
            showHistory()
            closeMenu()
        }
        wbMenuRefresh.setOnClickListener {
            reload()
            closeMenu()
        }
        wbMenuShare.setOnClickListener {
            //分享网址
            val sendIntent = Intent()
            sendIntent.action = Intent.ACTION_SEND
            sendIntent.type = "text/plain"
            sendIntent.putExtra(Intent.EXTRA_TITLE, wbWebView.title) //title
            sendIntent.putExtra(Intent.EXTRA_TEXT, wbWebView.url)// msgText
            sendIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(Intent.createChooser(sendIntent, getString(R.string.web_tool_share)))
            closeMenu()
        }
        wbMenuInfo.setOnClickListener {
            val info = String.format(getString(R.string.web_page_info), wbWebView.title, wbWebView.url)
            AlertUtil.showMsg(this, null, info, getString(R.string.web_copy_url),
                    getString(R.string.web_open_in_browser), getString(R.string.web_btn_cancel),
                    true, object : OnTaskDone<DialogInterface> {
                override fun onTaskDone(code: Int, msg: String?, data: DialogInterface?) {
                    when (code) {
                        AlertUtil.POSITIVE_BUTTON -> {
                            ClipboardUtil.putText(this@WebActivity, wbWebView.url)
                            ToastUtil.showLong(R.string.web_copy_success)
                        }
                        AlertUtil.NEUTRAL_BUTTON -> AppUtil.openInBrowser(wbWebView.url)
                    }
                }
            })
            closeMenu()
        }
        wbMenuClearCache.setOnClickListener {
            wbWebView.clearCache(true)
            ToastUtil.showLong(R.string.web_clear_cache_done)
            closeMenu()
        }
        wbMenuInput.setOnClickListener {
            inputUrl()
            closeMenu()
        }
        wbMenuClose.setOnClickListener {
            closeMenu()
        }

        initWebView()

        isSafe = true
        WebViewHelper.config?.getCallBack()?.onTaskDone(WebViewHelper.EVENT_ON_CREATE, null, this)

        val url = intent?.extras?.getString(INTENT_EXTRA_KEY_URL)
        if (url.isNullOrEmpty()) {
            goHome()
        } else {
            loadUrl(url)
        }
    }

    @SuppressLint("SetJavaScriptEnabled", "AddJavascriptInterface", "JavascriptInterface")
    private fun initWebView() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(WebViewHelper.config?.isDev() ?: false) //调试设置
        }
        wbWebView.scrollBarStyle = WebView.SCROLLBARS_INSIDE_OVERLAY
        //设置背景透明(setBackgroundColor及alpha),须在布局文件中WebView设置background属性
        //webView.setBackgroundColor(0)
        //webView.background?.alpha = 0 //设置WebView背景透明后，返回首页会有菜单NavigationView背景变成透明的bug
        //WebSettings
        val webSettings = wbWebView.settings
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webSettings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }
        webSettings.javaScriptCanOpenWindowsAutomatically = true //支持js调用window.open方法
        webSettings.setSupportMultipleWindows(false) //设置是否允许开启多窗口(false表示在当前webview中打开)
        webSettings.javaScriptEnabled = true //允许JavaScript
        webSettings.domStorageEnabled = true
        webSettings.databaseEnabled = true
        webSettings.setAppCacheEnabled(false)
        webSettings.setAppCachePath(cacheDir.path) //设置缓存目录
        webSettings.cacheMode = WebSettings.LOAD_DEFAULT
        webSettings.allowFileAccess = true //允许访问文件
        webSettings.userAgentString = WebViewHelper.getUserAgent()
        webSettings.layoutAlgorithm = WebSettings.LayoutAlgorithm.NORMAL //设置WebView布局算法
        webSettings.setGeolocationEnabled(true) //允许地理位置可用
        webSettings.setSupportZoom(true) //设置可以支持缩放
        webSettings.builtInZoomControls = true //设置出现缩放工具并支持手势缩放
        webSettings.displayZoomControls = false //设置缩放工具是否显示
        //webSettings.useWideViewPort = true //允许扩大比例的缩放
        //webSettings.loadWithOverviewMode=true //覆盖模式
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            wbWebView.addJavascriptInterface(object : Any() {
                @JavascriptInterface
                fun getMarks(): String? {
                    //将书签列表传给web首页
                    try {
                        val array = JSONArray()
                        WebViewHelper.marks.forEach {
                            if (!it.url.isNullOrEmpty()) {
                                val obj = JSONObject()
                                obj.put("title", it.showTitle())
                                obj.put("url", it.url)
                                array.put(obj)
                            }
                        }
                        return array.toString()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    return null
                }
            }, "jsObj") //可自定义Js接口
        }
        wbWebView.setDownloadListener(object : DownloadListener {
            override fun onDownloadStart(url: String?, userAgent: String?, contentDisposition: String?,
                                         mimetype: String?, contentLength: Long) {
                LogUtil.d(TAG, "onDownloadStart url:$url contentDisposition:$contentDisposition" +
                        " mimetype:$mimetype userAgent:$userAgent contentLength:$contentLength")
                AlertUtil.confirm(this@WebActivity, getString(R.string.web_create_download_task),
                        String.format(getString(R.string.web_create_download_tips), url), getString(R.string.web_btn_ok),
                        getString(R.string.web_btn_cancel), true, object : OnTaskDone<DialogInterface> {
                    override fun onTaskDone(code: Int, msg: String?, dialog: DialogInterface?) {
                        if (code == AlertUtil.POSITIVE_BUTTON) {
                            val fileName = URLUtil.guessFileName(url, contentDisposition, mimetype)
                            try {
                                val request = DownloadManager.Request(Uri.parse(url))
                                request.allowScanningByMediaScanner()
                                // Notify client once download is completed!
                                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                request.setDestinationUri(Uri.fromFile(File(Environment.getExternalStoragePublicDirectory(
                                        Environment.DIRECTORY_DOWNLOADS), fileName)))
                                val dm = getSystemService(Context.DOWNLOAD_SERVICE)
                                if (dm is DownloadManager) dm.enqueue(request)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                })
            }
        })
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            wbWebView.setFindListener(object : WebView.FindListener {
                override fun onFindResultReceived(activeMatchOrdinal: Int, numberOfMatches: Int, isDoneCounting: Boolean) {
                    LogUtil.d(TAG, "onFindResultReceived activeMatchOrdinal:$activeMatchOrdinal" +
                            " numberOfMatches:$numberOfMatches isDoneCounting:$isDoneCounting")
                }
            })
        }
        wbWebView.webViewClient = object : WebViewClient() {
            override fun onLoadResource(view: WebView, url: String) {
                //WebView加载html、js、css、请求服务器接口时都会回调此方法
                super.onLoadResource(view, url)
                LogUtil.v(TAG, "onLoadResource url:$url")
            }

            override fun doUpdateVisitedHistory(view: WebView, url: String, isReload: Boolean) {
                //WebView加载URL、通过JS方法切换路由地址时，会回调此方法
                super.doUpdateVisitedHistory(view, url, isReload)
                LogUtil.d(TAG, "doUpdateVisitedHistory url:$url isReload:$isReload")
                checkBackOrForward()
            }

            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                LogUtil.d(TAG, "shouldOverrideUrlLoading url:$url")
                return (checkScheme(url) || super.shouldOverrideUrlLoading(view, url))
            }

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest?): Boolean {
                //api-24及以上会回调此方法
                if (request != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    LogUtil.d(TAG, "shouldOverrideUrlLoading request:" + request.url)
                    if (request.url != null && checkScheme(request.url.toString())) {
                        return true
                    }
                }
                return super.shouldOverrideUrlLoading(view, request)
            }

            private fun checkScheme(url: String): Boolean {
                //需要处理的Scheme
                val uri = Uri.parse(url)
                when (uri?.scheme) {
                    "tel" -> { //电话
                        return AppUtil.openTelDial(uri)
                    }
                    "alipay", "alipays", "weixin" -> {
                        // 调用原生APP
                        val intent = Intent(Intent.ACTION_VIEW, uri)
                        if (intent.resolveActivity(packageManager) != null) {
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                            startActivity(intent)
                            return true
                        }
                    }
                }
                return false
            }

            override fun onReceivedError(view: WebView, errorCode: Int, description: String, failingUrl: String) {
                LogUtil.d(TAG, "onReceivedError errorCode:$errorCode description:$description failingUrl:$failingUrl")
                checkBackOrForward()
            }

            override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, errorResponse: WebResourceResponse) {
                LogUtil.d(TAG, "onReceivedHttpError request:$request errorResponse:$errorResponse")
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        val pageUri = Uri.parse(view.url)
                        val reqUri = request.url
                        val reason = "(" + errorResponse.statusCode + ")" + errorResponse.reasonPhrase
                        //只处理页面url的Http异常，不阻断页面内部js、接口、图标等请求异常
                        if (TextUtils.equals(pageUri.host, reqUri.host) && TextUtils.equals(pageUri.path, reqUri.path)) {
                            LogUtil.v(TAG, "onReceivedHttpError same-uri url:$reqUri reason:$reason")
                        } else {
                            LogUtil.v(TAG, "onReceivedHttpError opt-uri url:$reqUri reason:$reason")
                            return
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                checkBackOrForward()
            }

            override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
                LogUtil.d(TAG, "onReceivedSslError handler:$handler error:$error")
                if (WebViewHelper.config?.isDev() == true) {
                    handler.proceed() //接受所有网站的证书
                } else {
                    super.onReceivedSslError(view, handler, error)
                    checkBackOrForward()
                }
            }

            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                LogUtil.d(TAG, "onPageStarted url:$url")
                checkBackOrForward()
            }

            override fun onPageFinished(view: WebView, url: String) {
                LogUtil.d(TAG, "onPageFinished url:$url")
                checkBackOrForward()
                addHistory()

                WebViewHelper.removeAd(view, url)
            }
        }
        wbWebView.webChromeClient = object : WebChromeClient() {
            override fun onReceivedTitle(view: WebView, title: String) {
                LogUtil.d(TAG, "onReceivedTitle title:$title")
            }

            override fun onCreateWindow(view: WebView, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message): Boolean {
                LogUtil.d(TAG, "onCreateWindow isDialog:$isDialog isUserGesture:$isUserGesture resultMsg:$resultMsg")
                try {
                    val windowWebView = WebView(view.context)
                    val transport = resultMsg.obj as WebView.WebViewTransport
                    transport.webView = windowWebView
                    resultMsg.sendToTarget()
                    return true
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                return super.onCreateWindow(view, isDialog, isUserGesture, resultMsg)
            }

            override fun onCloseWindow(window: WebView) {
                LogUtil.d(TAG, "onCloseWindow window:$window")
                super.onCloseWindow(window)
            }

            override fun onProgressChanged(view: WebView, newProgress: Int) {
                wbProgressBar.visibility = if (newProgress >= 100 || newProgress < 0) View.GONE else View.VISIBLE
                wbProgressBar.progress = newProgress

                if (newProgress == 50 || newProgress == 80) {
                    WebViewHelper.removeAd(view, view.url)
                }
            }

            override fun onGeolocationPermissionsShowPrompt(origin: String, callback: GeolocationPermissions.Callback) {
                LogUtil.d(TAG, "onGeolocationPermissionsShowPrompt origin:$origin")
                //网页申请定位权限时回调此方法(boolean allow 是否允许获取定位权限, boolean retain 是否记住获取状态)
                AlertUtil.confirm(this@WebActivity, null, getString(R.string.web_geolocation_permission_tips),
                        getString(R.string.web_btn_ok), getString(R.string.web_btn_cancel), true, object : OnTaskDone<DialogInterface> {
                    override fun onTaskDone(code: Int, msg: String?, data: DialogInterface?) {
                        if (code == AlertUtil.POSITIVE_BUTTON) {
                            callback.invoke(origin, true, true)
                        } else {
                            callback.invoke(origin, false, false)
                        }
                    }
                })
            }

            // 网页请求文件选择器的回调方法（不同api版本走不同的方法）
            // For Android < 3.0
            fun openFileChooser(uploadMsg: ValueCallback<Uri>) {
                LogUtil.d(TAG, "openFileChooser")
                fcUploadMessage = uploadMsg
                chooseFile(null)
            }

            // For Android  >= 3.0
            fun openFileChooser(uploadMsg: ValueCallback<Uri>, acceptType: String) {
                LogUtil.d(TAG, "openFileChooser acceptType:$acceptType")
                fcUploadMessage = uploadMsg
                chooseFile(null)
            }

            // For Android  >= 4.1
            fun openFileChooser(uploadMsg: ValueCallback<Uri>, acceptType: String, capture: String) {
                LogUtil.d(TAG, "openFileChooser acceptType:$acceptType capture:$capture")
                fcUploadMessage = uploadMsg
                chooseFile(arrayOf(acceptType))
            }

            // For Android >= 5.0
            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            override fun onShowFileChooser(webView: WebView, filePathCallback: ValueCallback<Array<Uri>>,
                                           fileChooserParams: FileChooserParams?): Boolean {
                LogUtil.d(TAG, "onShowFileChooser fileChooserParams:$fileChooserParams")
                fcFilePathCallback = filePathCallback
                chooseFile(fileChooserParams?.acceptTypes)
                return true
            }

            private fun chooseFile(acceptTypes: Array<String>?) {
                if (isSafe()) {
                    val intent: Intent
                    if (acceptTypes?.size == 1 && acceptTypes[0].contains("image")) {
                        intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                        intent.type = "image/*"
                    } else {
                        intent = Intent(Intent.ACTION_GET_CONTENT)
                        intent.addCategory(Intent.CATEGORY_OPENABLE)
                        if (acceptTypes?.size == 1) {
                            intent.type = acceptTypes[0]
                        } else {
                            intent.type = "*/*"
                        }
                    }
                    if (intent.resolveActivity(packageManager) != null) {
                        startActivityForResult(intent, REQUEST_CODE_FILE_CHOOSER)
                    } else {
                        ToastUtil.showLong(R.string.web_select_file_f)
                    }
                }
            }
        }
        //长按保存图片
        wbWebView.setOnLongClickListener {
            val result = wbWebView.hitTestResult
            if (result != null) {
                LogUtil.v(TAG, "WebView OnLongClick type:" + result.type + ", extra:" + result.extra)
                if (result.type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE
                        || result.type == WebView.HitTestResult.IMAGE_TYPE) {
                    val data = result.extra //可能是图片url或base64的图片内容
                    if (isSafe() && data != null) {
                        AlertUtil.list(this, null, arrayOf(getString(R.string.web_save_image)),
                                object : DialogInterface.OnClickListener {
                                    override fun onClick(dialog: DialogInterface?, which: Int) {
                                        try {
                                            if (URLUtil.isNetworkUrl(data)) {
                                                //从图片url保存图片
                                                /*Rx2AndroidNetworking.get(data).build().bitmapSingle
                                                        .subscribeOn(Schedulers.io())
                                                        .observeOn(AndroidSchedulers.mainThread())
                                                        .subscribe(object : SingleObserver<Bitmap> {
                                                            override fun onSubscribe(d: Disposable) {
                                                            }

                                                            override fun onSuccess(bitmap: Bitmap) {
                                                                if (isSafe()) saveBitmap(bitmap)
                                                            }

                                                            override fun onError(e: Throwable) {
                                                                e.printStackTrace()
                                                                if (isSafe()) ToastUtil.showLong(R.string.web_save_image_f)
                                                            }
                                                        })*/
                                                val dir = getImgDir().absolutePath
                                                val fileName = getImgName(data)
                                                Rx2AndroidNetworking.download(data, dir, fileName)
                                                        .build().downloadSingle
                                                        .subscribeOn(Schedulers.io())
                                                        .observeOn(AndroidSchedulers.mainThread())
                                                        .subscribe(object : SingleObserver<String> {
                                                            override fun onSubscribe(d: Disposable) {
                                                            }

                                                            override fun onSuccess(msg: String) {
                                                                MediaScanUtil.scanFile(File(dir, fileName).absolutePath)
                                                                ToastUtil.showLong(R.string.web_save_image_s)
                                                            }

                                                            override fun onError(e: Throwable) {
                                                                e.printStackTrace()
                                                                if (isSafe()) ToastUtil.showLong(R.string.web_save_image_f)
                                                            }
                                                        })
                                            } else if (data.matches("data:.*;base64,.*".toRegex())) {
                                                //从图片Base64数据保存图片
                                                val imgData = data.split("base64,".toRegex())[1]
                                                val bitmap = ImgUtil.base64ToBitmap(imgData)
                                                saveBitmap(bitmap)
                                            } else {
                                                ToastUtil.showLong(R.string.web_save_image_f)
                                            }
                                        } catch (e: Exception) {
                                            ToastUtil.showLong(R.string.web_save_image_f)
                                            e.printStackTrace()
                                        }
                                    }

                                    private fun saveBitmap(bitmap: Bitmap) {
                                        try {
                                            val saveFile = File(getImgDir(), getImgName(null))
                                            FileUtil.createFile(saveFile)
                                            val fos = FileOutputStream(saveFile)
                                            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos)
                                            fos.flush()
                                            fos.close()
                                            MediaScanUtil.scanFile(saveFile.absolutePath)
                                            ToastUtil.showLong(R.string.web_save_image_s)
                                        } catch (e: Exception) {
                                            ToastUtil.showLong(R.string.web_save_image_f)
                                            e.printStackTrace()
                                        }
                                    }

                                    @Suppress("DEPRECATION")
                                    private fun getImgDir() = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)

                                    private fun getImgName(uri: String?): String {
                                        val sdf = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault())
                                        val suffix = when (val ext = FileUtil.getExtName(uri)?.toLowerCase()) {
                                            "gif", "bmp", "png", "ico" -> ".$ext"
                                            else -> ".jpg"
                                        }
                                        return sdf.format(Date()) + suffix
                                    }
                                }, true)
                        return@setOnLongClickListener true
                    }
                }
            }
            false
        }
    }

    private var fcUploadMessage: ValueCallback<Uri>? = null
    private var fcFilePathCallback: ValueCallback<Array<Uri>>? = null
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_FILE_CHOOSER) {
            if (isSafe()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    if (intent == null) {
                        fcFilePathCallback?.onReceiveValue(null)
                    } else {
                        fcFilePathCallback?.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, intent))
                    }
                } else {
                    fcUploadMessage?.onReceiveValue(if (intent == null) null else intent.data)
                }
                fcUploadMessage = null
                fcFilePathCallback = null
            }
        }
    }

    override fun onBackPressed() {
        if (!goBack()) super.onBackPressed()
    }

    private fun loadUrl(url: String?) {
        LogUtil.i(TAG, "loadUrl url:$url")
        wbWebView.loadUrl(url)
        checkBackOrForward()
    }

    private fun reload() {
        wbWebView.reload()
        checkBackOrForward()
    }

    private fun canGoBack(): Boolean {
        return wbWebView.canGoBack()
    }

    private fun canGoForward(): Boolean {
        return wbWebView.canGoForward()
    }

    @Suppress("DEPRECATION")
    private fun checkBackOrForward() {
        if (canGoBack()) {
            wbBackBtn.setColorFilter(Color.WHITE)
            wbBackBtn.isEnabled = true
        } else {
            wbBackBtn.setColorFilter(resources.getColor(R.color.web_color_999))
            wbBackBtn.isEnabled = false
        }
        if (canGoForward()) {
            wbNextBtn.setColorFilter(Color.WHITE)
            wbNextBtn.isEnabled = true
        } else {
            wbNextBtn.setColorFilter(resources.getColor(R.color.web_color_999))
            wbNextBtn.isEnabled = false
        }
    }

    private fun goHome() {
        loadUrl(WebViewHelper.HOME_PAGE)
        wbWebView.postDelayed({
            //webView.goBackOrForward(0)
            wbWebView.clearHistory()
            checkBackOrForward()
        }, 200)
    }

    private fun goBack(): Boolean {
        return if (canGoBack()) {
            wbWebView.goBack()
            checkBackOrForward()
            true
        } else {
            false
        }
    }

    private fun goForward(): Boolean {
        return if (canGoForward()) {
            wbWebView.goForward()
            checkBackOrForward()
            true
        } else {
            false
        }
    }

    private fun closeMenu() {
        wbMenuBar.visibility = View.GONE
    }

    private fun inputUrl() {
        XPopup.Builder(this).asInputConfirm(getString(R.string.web_input_url), null, null,
                null, object : OnInputConfirmListener {
            override fun onConfirm(text: String?) {
                if (!TextUtils.isEmpty(text)) {
                    if (text?.contains("://".toRegex()) == true) {
                        loadUrl(text)
                    } else {
                        loadUrl("http://$text")
                    }
                }
            }
        }).show()
    }

    private fun addMark() {
        XPopup.Builder(this).asInputConfirm(getString(R.string.web_add_mark),
                String.format(getString(R.string.web_input_mark_title), wbWebView.url),
                wbWebView.title, null, object : OnInputConfirmListener {
            override fun onConfirm(text: String?) {
                val record = WebViewHelper.Record(text, wbWebView.url)
                WebViewHelper.marks.remove(record)
                WebViewHelper.marks.add(0, record)
                onMarksUpdate()
            }
        }).show()
    }

    private fun showMarks() {
        val titles = mutableListOf<String>()
        val urls = mutableListOf<String>()
        WebViewHelper.marks.forEach {
            if (!it.url.isNullOrBlank()) {
                titles.add(it.showTitle())
                urls.add(it.url!!)
            }
        }
        AlertDialog.Builder(this)
                .setTitle(R.string.web_tool_marks)
                .setItems(titles.toTypedArray(), object : DialogInterface.OnClickListener {
                    override fun onClick(dialog: DialogInterface?, which: Int) {
                        loadUrl(urls[which])
                    }
                }).setPositiveButton(R.string.web_btn_edit, object : DialogInterface.OnClickListener {
                    override fun onClick(dialog: DialogInterface?, which: Int) {
                        editMarks()
                    }
                }).setNeutralButton(R.string.web_tool_add_mark, object : DialogInterface.OnClickListener {
                    override fun onClick(dialog: DialogInterface?, which: Int) {
                        addMark()
                    }
                }).setNegativeButton(R.string.web_btn_cancel, null).show()
    }

    private fun editMarks() {
        val titles = mutableListOf<String>()
        val records = mutableListOf<WebViewHelper.Record>()
        val checkedItems = mutableListOf<Boolean>()
        WebViewHelper.marks.forEach {
            if (!it.url.isNullOrBlank()) {
                titles.add((it.title ?: "") + "\n" + it.url)
                records.add(it)
                checkedItems.add(false)
            }
        }
        AlertDialog.Builder(this)
                .setTitle(R.string.web_edit_web_marks)
                .setMultiChoiceItems(titles.toTypedArray(), checkedItems.toBooleanArray(),
                        object : DialogInterface.OnMultiChoiceClickListener {
                            override fun onClick(dialog: DialogInterface?, which: Int, isChecked: Boolean) {
                                checkedItems[which] = isChecked
                            }
                        })
                .setPositiveButton(R.string.web_btn_delete, object : DialogInterface.OnClickListener {
                    override fun onClick(dialog: DialogInterface?, which: Int) {
                        records.forEachIndexed { index, record ->
                            if (checkedItems[index]) {
                                WebViewHelper.marks.remove(record)
                            }
                        }
                        onMarksUpdate()
                    }
                }).setNeutralButton(R.string.web_btn_rename, object : DialogInterface.OnClickListener {
                    override fun onClick(dialog: DialogInterface?, which: Int) {
                        checkedItems.forEachIndexed { index, b ->
                            if (b) {
                                val record = records[index]
                                XPopup.Builder(this@WebActivity).asInputConfirm(getString(R.string.web_edit_web_marks),
                                        String.format(getString(R.string.web_input_mark_title), record.url),
                                        record.title, null, object : OnInputConfirmListener {
                                    override fun onConfirm(text: String?) {
                                        val recordNew = WebViewHelper.Record(text, record.url)
                                        WebViewHelper.marks.remove(recordNew)
                                        WebViewHelper.marks.add(0, recordNew)
                                        onMarksUpdate()
                                    }
                                }).show()
                                return
                            }
                        }
                    }
                }).setNegativeButton(R.string.web_btn_cancel, null).show()
    }

    private fun onMarksUpdate() {
        WebViewHelper.saveMarks()
        if (wbWebView.url == WebViewHelper.HOME_PAGE) {
            reload()
        }
    }

    private fun addHistory() {
        val record = WebViewHelper.Record(wbWebView.title, wbWebView.url)
        WebViewHelper.history.remove(record)
        WebViewHelper.history.add(0, record)
        while (WebViewHelper.history.size > 60) {
            WebViewHelper.history.removeAt(60)
        }
        WebViewHelper.saveHistory()
    }

    private fun showHistory() {
        val titles = mutableListOf<String>()
        val urls = mutableListOf<String>()
        WebViewHelper.history.forEach {
            if (!it.url.isNullOrBlank()) {
                titles.add(it.showTitle())
                urls.add(it.url!!)
            }
        }
        AlertDialog.Builder(this)
                .setTitle(R.string.web_tool_history)
                .setItems(titles.toTypedArray(), object : DialogInterface.OnClickListener {
                    override fun onClick(dialog: DialogInterface?, which: Int) {
                        loadUrl(urls[which])
                    }
                }).setPositiveButton(R.string.web_clear_history, object : DialogInterface.OnClickListener {
                    override fun onClick(dialog: DialogInterface?, which: Int) {
                        WebViewHelper.history.clear()
                        WebViewHelper.saveHistory()
                    }
                }).setNegativeButton(R.string.web_btn_cancel, null).show()
    }


    private var isSafe = false
    private fun isSafe(): Boolean {
        return isSafe
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(WebViewHelper.config?.attachBaseContext(newBase))
    }

    override fun onDestroy() {
        try {
            wbWebViewLayout.removeView(wbWebView)
            wbWebView.removeAllViews()
            wbWebView.destroy()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        WebViewHelper.config?.getCallBack()?.onTaskDone(WebViewHelper.EVENT_ON_DESTROY, null, this)
        isSafe = false
        super.onDestroy()
    }
}
