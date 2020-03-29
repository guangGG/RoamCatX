package gapp.season.roamcat.data.net.interceptor;

import android.text.TextUtils;

import java.io.IOException;
import java.util.HashMap;

import gapp.season.roamcat.BuildConfig;
import gapp.season.roamcat.data.runtime.LanguageHelper;
import okhttp3.Interceptor;
import okhttp3.Request;

public final class AppHeaderInterceptor implements Interceptor {
    private HashMap<String, String> mHosts;

    public AppHeaderInterceptor() {
        mHosts = new HashMap<>(); //格式：Pair("127.0.0.1", "localhost")
        mHosts.put("127.0.0.1", "localhost");
    }

    @Override
    public okhttp3.Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        Request.Builder builder = request.newBuilder();
        builder.addHeader("App-Version", BuildConfig.VERSION_NAME);
        builder.addHeader("Accept-Language", LanguageHelper.INSTANCE.getLanguage());
        String host = getHost(request.url().toString());
        if (!TextUtils.isEmpty(host)) {
            builder.addHeader("Host", host);
        }
        return chain.proceed(builder.build());
    }


    /**
     * config the Host for ip
     */
    private String getHost(String url) {
        if (BuildConfig.DEV && url != null && mHosts != null) {
            for (String key : mHosts.keySet()) {
                if (url.contains(key)) {
                    return mHosts.get(key);
                }
            }
        }
        return null;
    }
}
