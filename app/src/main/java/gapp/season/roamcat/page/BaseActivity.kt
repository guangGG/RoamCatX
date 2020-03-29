package gapp.season.roamcat.page

import android.app.ProgressDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.gyf.immersionbar.ktx.immersionBar
import gapp.season.roamcat.R
import gapp.season.roamcat.data.runtime.LanguageHelper
import io.reactivex.disposables.CompositeDisposable

abstract class BaseActivity : AppCompatActivity() {
    private var progressDialog: ProgressDialog? = null
    var compositeDisposable: CompositeDisposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        compositeDisposable = CompositeDisposable()
        if (!customImmersionBar()) {
            immersionBar {
                fitsSystemWindows(true)
                statusBarColor(R.color.colorPrimaryDark)
                statusBarDarkFont(false)
                navigationBarColor(R.color.colorPrimaryDark)
                navigationBarDarkIcon(false)
            }
        }
    }

    override fun onDestroy() {
        compositeDisposable?.dispose()
        super.onDestroy()
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(LanguageHelper.getLanguageContext(newBase!!))
    }

    override fun setTitle(titleId: Int) {
        super.setTitle(titleId)
        supportActionBar?.setTitle(titleId)
    }

    override fun setTitle(title: CharSequence?) {
        super.setTitle(title)
        supportActionBar?.title = title
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            // Respond to the action bar's Up/Home button
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        try {
            var intercepted = false
            if (isSafe()) {
                supportFragmentManager.fragments.forEach {
                    if (it is BaseFragment && it.isSafe()) {
                        intercepted = it.onBackPressed()
                        if (intercepted) {
                            return@forEach
                        }
                    }
                }
            }
            if (!intercepted && !dealBackPressed()) {
                super.onBackPressed()
            }
        } catch (e: Exception) {
            finish()
        }
    }

    fun setDisplayHomeAsUpEnabled(enabled: Boolean) {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        if (toolbar != null) setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(enabled)
        supportActionBar?.setDisplayShowHomeEnabled(enabled)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.icon_action_back)
    }

    fun isSafe(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            !isDestroyed
        } else {
            !(compositeDisposable?.isDisposed ?: false)
        }
    }

    fun hideLoading() {
        if (progressDialog != null && progressDialog!!.isShowing()) {
            progressDialog!!.cancel()
        }
    }

    fun showLoading() {
        showLoading(true)
    }

    fun showLoading(cancelable: Boolean) {
        hideLoading()
        progressDialog = showLoadingDialog(this, null, cancelable)
    }

    @Suppress("DEPRECATION")
    fun showLoadingDialog(context: Context, msg: String?, cancelable: Boolean): ProgressDialog {
        val progressDialog = ProgressDialog(context)
        progressDialog.show()
        if (progressDialog.window != null) {
            progressDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
        progressDialog.setContentView(R.layout.dialog_progress)
        if (!TextUtils.isEmpty(msg)) {
            progressDialog.setMessage(msg)
        }
        progressDialog.isIndeterminate = true
        progressDialog.setCancelable(cancelable)
        progressDialog.setCanceledOnTouchOutside(false)
        return progressDialog
    }

    fun supportImmersionBarVersion() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M

    /**
     * 重写此方法自定义页面ImmersionBar
     */
    @Suppress("DEPRECATION")
    open fun customImmersionBar(): Boolean {
        //设置ImmersionBar后在华为5.1系统手机上宽度样式异常
        val unSupportVersion = !supportImmersionBarVersion()
        if (unSupportVersion && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.navigationBarColor = resources.getColor(R.color.colorPrimaryDark)
        }
        return unSupportVersion
    }

    /**
     * 重写此方法自定义处理返回事件
     */
    open fun dealBackPressed(): Boolean {
        return false
    }
}
