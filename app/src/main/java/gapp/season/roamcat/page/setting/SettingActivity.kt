package gapp.season.roamcat.page.setting

import android.content.DialogInterface
import android.os.Build
import android.os.Bundle
import android.os.Handler
import gapp.season.roamcat.App
import gapp.season.roamcat.R
import gapp.season.roamcat.data.file.MmkvUtil
import gapp.season.roamcat.data.runtime.ClipboardHelper
import gapp.season.roamcat.data.runtime.LanguageHelper
import gapp.season.roamcat.page.BaseActivity
import gapp.season.util.app.AppUtil
import gapp.season.util.tips.AlertUtil
import gapp.season.util.tips.ToastUtil
import kotlinx.android.synthetic.main.activity_setting.*

class SettingActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)
        setDisplayHomeAsUpEnabled(true)
        setTitle(R.string.menu_setting)
        languageView.setOnClickListener {
            AlertUtil.list(this, null, arrayOf(getString(R.string.language_en),
                    getString(R.string.language_cn)), object : DialogInterface.OnClickListener {
                override fun onClick(dialog: DialogInterface?, which: Int) {
                    val lanOld = LanguageHelper.getLanguage()
                    var lanNew = lanOld
                    when (which) {
                        0 -> lanNew = LanguageHelper.LANGUAGE_EN
                        1 -> lanNew = LanguageHelper.LANGUAGE_CN
                    }
                    if (lanNew != lanOld) {
                        LanguageHelper.setLanguage(lanNew)
                        updateUI()
                        var delay = 1000L
                        if (!configLangVersion()) {
                            ToastUtil.showLong(R.string.set_language_s_tips)
                            delay = 2000L
                        }
                        showLoading()
                        Handler().postDelayed({
                            hideLoading()
                            //问题：Android-N以上重启时Application无法应用到新设置(attachBaseContext)，只有Activity更新了配置
                            if (configLangVersion()) {
                                LanguageHelper.getLanguageContext(App.instance!!) //对7.1.1系统以上无效
                                AppUtil.restartApp()
                            } else {
                                AppUtil.closeApp()
                            }
                        }, delay)
                    }
                }

                private fun configLangVersion() = Build.VERSION.SDK_INT < Build.VERSION_CODES.N
            }, true)
        }
        clipboardSetSwitch.setOnCheckedChangeListener { _, _ ->
            MmkvUtil.map()?.encode(MmkvUtil.CLIPBOARD_AUTO_JUMP, clipboardSetSwitch.isChecked)
            ClipboardHelper.updateAutoJump()
            updateUI()
        }
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    private fun updateUI() {
        if (LanguageHelper.LANGUAGE_CN == LanguageHelper.getLanguage()) {
            languageTextView.setText(R.string.language_cn)
        } else {
            languageTextView.setText(R.string.language_en)
        }
        clipboardSetSwitch.isChecked = ClipboardHelper.autoJump
    }
}
