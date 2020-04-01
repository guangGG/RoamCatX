package gapp.season.roamcat.page.setting

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import com.rx2androidnetworking.Rx2AndroidNetworking
import gapp.season.encryptlib.code.HexUtil
import gapp.season.encryptlib.hash.HashUtil
import gapp.season.roamcat.BuildConfig
import gapp.season.roamcat.R
import gapp.season.roamcat.data.net.AppNetwork
import gapp.season.roamcat.page.BaseActivity
import gapp.season.roamcat.util.SchedulersUtil
import gapp.season.util.tips.ToastUtil
import gapp.season.webbrowser.WebViewHelper
import io.reactivex.SingleObserver
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_about.*
import org.json.JSONObject

class AboutActivity : BaseActivity() {
    private var hasNewVersion = false
    private var newVersion = "" //3.0.0
    private var newVersionDownloadUrl = "" //http://download.xxx.com/xxx.apk
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
        setDisplayHomeAsUpEnabled(true)
        setTitle(R.string.menu_about)
        versionText.text = String.format(getString(R.string.current_version), BuildConfig.VERSION_NAME,
                if (BuildConfig.DEV) (" (" + BuildConfig.VERSION_CODE + ")") else "")
        checkUpdateView.setOnClickListener {
            if (hasNewVersion) {
                val uri = Uri.parse(newVersionDownloadUrl)
                val intent = Intent(Intent.ACTION_VIEW, uri)
                startActivity(intent)
            } else {
                checkUpdate(true)
                updateUI()
            }
        }
        if (BuildConfig.DEV) readMeView.visibility = View.VISIBLE
        readMeView.setOnClickListener { WebViewHelper.showWebPage(this, AppNetwork.URL_GITHUB_APP) }
        if (isDebugVersion()) debugTipsView.visibility = View.VISIBLE
        debugTipsView.setOnClickListener { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("${AppNetwork.URL_DOWNLOAD_APP}?_ts=${System.currentTimeMillis()}"))) }
        checkUpdate(false)
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    private fun updateUI() {
        if (hasNewVersion) {
            checkUpdateResult.text = String.format(getString(R.string.find_new_version), newVersion)
        } else {
            checkUpdateResult.text = ""
        }
    }

    private fun checkUpdate(toast: Boolean) {
        Rx2AndroidNetworking.get(AppNetwork.API_APP_UPDATE_CONFIG)
                .build().jsonObjectSingle
                .subscribeOn(SchedulersUtil.io())
                .observeOn(SchedulersUtil.ui())
                .subscribe(object : SingleObserver<JSONObject> {
                    override fun onSubscribe(d: Disposable) {
                        compositeDisposable?.add(d)
                    }

                    override fun onError(e: Throwable) {
                        if (toast) ToastUtil.showLong(R.string.check_update_failed)
                    }

                    override fun onSuccess(obj: JSONObject) {
                        try {
                            val version = obj.optInt("version")
                            val verName = obj.optString("vername")
                            val downloadUrl = obj.optString("download_url")
                            if (version > BuildConfig.VERSION_CODE) {
                                hasNewVersion = true
                                newVersion = verName
                                newVersionDownloadUrl = downloadUrl
                                updateUI()
                            } else {
                                if (toast) ToastUtil.showLong(R.string.has_be_latest_version)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            if (toast) ToastUtil.showLong(R.string.check_update_failed)
                        }
                    }
                })
    }

    private fun isDebugVersion(): Boolean {
        return BuildConfig.DEBUG || BuildConfig.DEV || "bebafaa5713ad933b1d7ec436ed0c2d9" != getSignMd5()
    }

    @SuppressLint("PackageManagerGetSignatures")
    @Suppress("DEPRECATION")
    private fun getSignMd5(): String? {
        try {
            val signInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
            return HexUtil.toHexStr(HashUtil.encode(signInfo.signatures[0].toByteArray(), HashUtil.ALGORITHM_MD5))
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}
