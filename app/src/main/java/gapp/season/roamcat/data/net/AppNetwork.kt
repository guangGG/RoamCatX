package gapp.season.roamcat.data.net

import android.content.Context
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.internal.InternalNetworking
import com.facebook.stetho.Stetho
import com.facebook.stetho.okhttp3.StethoInterceptor
import gapp.season.roamcat.BuildConfig
import gapp.season.roamcat.data.net.interceptor.AppHeaderInterceptor
import gapp.season.roamcat.data.net.interceptor.AppLogInterceptor
import gapp.season.roamcat.data.net.ssl.SslTrustManager
import gapp.season.webbrowser.WebViewHelper

object AppNetwork {
    const val URL_GITHUB_APP = "https://github.com/guangGG/RoamCatX"
    const val URL_DOWNLOAD_APP_PAGE = "http://res.ehorizon.top/roamcat/download-x.html"

    const val URL_DOWNLOAD_APP = "http://res.ehorizon.top/roamcat/apk-x/RoamCatX.apk"
    const val URL_DOWNLOAD_APP_DEV = "http://res.ehorizon.top/roamcat/apk-x/RoamCatX_dev.apk"
    const val URL_DOWNLOAD_POEM = "http://poem.ehorizon.top/poetry/poem/Poem.apk"

    const val API_APP_UPDATE_CONFIG = "http://res.ehorizon.top/roamcat/apk-x/config.json"
    const val API_SOLAR_DATUM_DATE = "http://res.ehorizon.top/roamcat/config/SolarDatumDate.json"

    fun init(context: Context) {
        val builder = InternalNetworking.getClient().newBuilder()
                .addInterceptor(AppHeaderInterceptor())
        val logInterceptor = AppLogInterceptor("OkHttp")
        if (BuildConfig.DEV) {
            SslTrustManager.trustAll(builder) //开发版本信任所有HTTPS证书
            Stetho.initializeWithDefaults(context) //Stetho连接Chrome(地址:chrome://inspect/#devices)
            builder.addNetworkInterceptor(StethoInterceptor()) //在Chrome中调试OkHttp请求
            logInterceptor.setPrintLevel(AppLogInterceptor.Level.BODY)
        } else {
            logInterceptor.setPrintLevel(AppLogInterceptor.Level.BASIC)
        }
        builder.addInterceptor(logInterceptor) //日志打印拦截器推荐放在最后，以便打印出其他拦截器对请求的处理
        AndroidNetworking.initialize(context, builder.build())
        InternalNetworking.setUserAgent(WebViewHelper.getUserAgent())
    }
}
