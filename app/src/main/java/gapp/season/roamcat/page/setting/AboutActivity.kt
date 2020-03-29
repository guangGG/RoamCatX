package gapp.season.roamcat.page.setting

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.rx2androidnetworking.Rx2AndroidNetworking
import gapp.season.roamcat.BuildConfig
import gapp.season.roamcat.R
import gapp.season.roamcat.data.net.AppNetwork
import gapp.season.roamcat.page.BaseActivity
import gapp.season.roamcat.util.SchedulersUtil
import gapp.season.util.tips.ToastUtil
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
        checkUpdate.setOnClickListener {
            if (hasNewVersion) {
                val uri = Uri.parse(newVersionDownloadUrl)
                val intent = Intent(Intent.ACTION_VIEW, uri)
                startActivity(intent)
            } else {
                checkUpdate(true)
                updateUI()
            }
        }
        checkUpdate(false)
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    private fun updateUI() {
        if (hasNewVersion) {
            checkUpdate.text = String.format(getString(R.string.find_new_version), newVersion)
        } else {
            checkUpdate.setText(R.string.check_update)
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
}
