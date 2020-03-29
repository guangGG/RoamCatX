package gapp.season.manageapps

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.widget.ImageView
import android.widget.TextView
import io.reactivex.Single
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

object ManageAppsHelper {
    var app: Application? = null

    fun init(application: Application) {
        this.app = application
    }

    fun manageApps(context: Context) {
        val intent = Intent(context, ManageAppsActivity::class.java)
        if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun loadIcon(imageView: ImageView, appId: String, cd: CompositeDisposable?) {
        imageView.tag = appId
        Single.fromCallable {
            val pm = app?.packageManager
            return@fromCallable (pm?.getPackageInfo(appId, 0)?.applicationInfo?.loadIcon(pm) as Drawable)
        }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : SingleObserver<Drawable> {
                    override fun onSuccess(t: Drawable) {
                        if (appId == imageView.tag) {
                            imageView.setImageDrawable(t)
                        }
                    }

                    override fun onSubscribe(d: Disposable) {
                        cd?.add(d)
                    }

                    override fun onError(e: Throwable) {
                        if (appId == imageView.tag) {
                            imageView.setImageResource(android.R.drawable.sym_def_app_icon)
                        }
                    }
                })
    }

    fun loadLabel(textView: TextView, appId: String, cd: CompositeDisposable?) {
        textView.tag = appId
        Single.fromCallable {
            val pm = app?.packageManager
            return@fromCallable (pm?.getPackageInfo(appId, 0)?.applicationInfo?.loadLabel(pm)?.toString() as String)
        }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : SingleObserver<String> {
                    override fun onSuccess(t: String) {
                        if (appId == textView.tag) {
                            textView.text = t
                        }
                    }

                    override fun onSubscribe(d: Disposable) {
                        cd?.add(d)
                    }

                    override fun onError(e: Throwable) {
                        if (appId == textView.tag) {
                            textView.text = ""
                        }
                    }
                })
    }
}
