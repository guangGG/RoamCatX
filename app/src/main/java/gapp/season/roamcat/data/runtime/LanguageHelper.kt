package gapp.season.roamcat.data.runtime

import android.content.Context
import android.os.Build
import gapp.season.roamcat.data.file.MmkvUtil
import java.util.*

object LanguageHelper {
    const val LANGUAGE_CN = "zh"
    const val LANGUAGE_EN = "en"

    fun setLanguage(lan: String) {
        MmkvUtil.map()?.encode(MmkvUtil.LANGUAGE_TAG, lan)
    }

    fun getLanguage(): String {
        var lan = MmkvUtil.map()?.decodeString(MmkvUtil.LANGUAGE_TAG)
        if (lan == null) {
            lan = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Locale.getDefault().toLanguageTag()
            } else {
                Locale.getDefault().language
            }
            lan = if (lan?.contains(LANGUAGE_CN) == true) {
                LANGUAGE_CN
            } else {
                LANGUAGE_EN
            }
        }
        return lan
    }

    fun getLanguageContext(context: Context): Context {
        val locale = getLocale()
        val resources = context.resources
        val configuration = resources.configuration
        //https://www.jianshu.com/p/9be83be8d1ef (configuration.setToDefaults()方法在Android7.0上影响弹窗展示)
        configuration.fontScale = 1f //解决修改android手机设置中字体大小后系统布局混乱
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            //7.1.1以上设置语言的方式
            configuration.setLocale(locale)
            return context.createConfigurationContext(configuration)
        } else {
            //7.1.1以下设置语言的方式
            configuration.locale = locale
            resources.updateConfiguration(configuration, resources.displayMetrics)
            return context
        }
    }

    private fun getLocale(): Locale {
        return if (LANGUAGE_CN == getLanguage()) {
            Locale.CHINA
        } else {
            Locale.ENGLISH
        }
    }
}