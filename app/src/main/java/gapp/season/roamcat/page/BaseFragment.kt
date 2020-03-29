package gapp.season.roamcat.page

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import com.gyf.immersionbar.components.SimpleImmersionOwner

abstract class BaseFragment : Fragment(), SimpleImmersionOwner {
    var baseActivity: BaseActivity? = null
    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is BaseActivity) {
            this.baseActivity = context
        }
    }

    override fun onDetach() {
        baseActivity = null
        super.onDetach()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(getLayoutId(), container, false)
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    /**
     * After the sub-threading is finished, Call this method before call the UI thread
     */
    fun isSafe(): Boolean {
        return !(this.isRemoving || this.isDetached || !this.isAdded
                || this.view == null || baseActivity == null)
    }

    fun hideLoading() {
        if (baseActivity != null) {
            baseActivity!!.hideLoading()
        }
    }

    fun showLoading() {
        if (baseActivity != null) {
            baseActivity!!.showLoading()
        }
    }

    fun showLoading(cancelable: Boolean) {
        if (baseActivity != null) {
            baseActivity!!.showLoading(cancelable)
        }
    }

    /**
     * 重写此方法设置布局文件
     */
    @LayoutRes
    abstract fun getLayoutId(): Int

    /**
     * 重写此方法设置是否自定义ImmersionBar
     */
    override fun immersionBarEnabled(): Boolean {
        return false
    }

    /**
     * 重写此方法配置ImmersionBar
     */
    override fun initImmersionBar() {
    }

    /**
     * 重写此方法自定义处理返回键(返回true表示拦截返回键点击向Activity传递)
     */
    open fun onBackPressed(): Boolean {
        return false
    }
}
