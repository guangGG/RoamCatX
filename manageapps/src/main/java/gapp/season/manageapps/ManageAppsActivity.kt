@file:Suppress("DEPRECATION", "SimpleDateFormat", "PackageManagerGetSignatures", "SetTextI18n")

package gapp.season.manageapps

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import gapp.season.encryptlib.code.HexUtil
import gapp.season.encryptlib.hash.HashUtil
import gapp.season.util.view.ThemeUtil
import io.reactivex.Single
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.mapps_activity.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.Comparator
import kotlin.math.sign

class ManageAppsActivity : AppCompatActivity() {
    companion object { //这里的设置在应用生命周期内有效
        var sort = 0 //0包名正序，1安装时间逆序，2更新时间逆序
        var filter = 1 //0全部应用，1用户应用，2系统应用
    }

    var compositeDisposable: CompositeDisposable? = null
    var listAll: List<PackageInfo>? = null
    var adapter: ManageAppsAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        compositeDisposable = CompositeDisposable()
        ThemeUtil.setTheme(this, 0)
        setContentView(R.layout.mapps_activity)
        mappsBack.setOnClickListener { finish() }
        mappsSort.setOnClickListener {
            AlertDialog.Builder(this)
                    .setTitle("排序")
                    .setSingleChoiceItems(arrayOf("按包名排序", "按安装时间排序", "按更新时间排序"), sort) { dialog, index ->
                        sort = index
                        updateUI()
                        dialog.dismiss()
                    }.show()

        }
        mappsFilter.setOnClickListener {
            AlertDialog.Builder(this)
                    .setTitle("筛选")
                    .setSingleChoiceItems(arrayOf("显示全部应用", "只显示用户应用", "只显示系统应用"), filter) { dialog, index ->
                        filter = index
                        updateUI()
                        dialog.dismiss()
                    }.show()
        }

        val dm = resources.displayMetrics
        val spanCount = (dm.widthPixels / dm.density / 56).toInt()
        val layoutManager = GridLayoutManager(this, spanCount)
        mappsList.layoutManager = layoutManager
        adapter = ManageAppsAdapter()
        mappsList.adapter = adapter
        adapter?.setOnItemClickListener { adapter, _, position ->
            val item = adapter.getItem(position)
            if (item is PackageInfo) {
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                val signInfo = packageManager.getPackageInfo(item.packageName, PackageManager.GET_SIGNATURES)
                val signMsg = try {
                    HexUtil.toHexStr(HashUtil.encode(signInfo.signatures[0].toByteArray(), HashUtil.ALGORITHM_MD5))
                } catch (e: Exception) {
                    "(计算签名失败)"
                }
                val msg = String.format("包名：%s\n版本：%s\n版本号：%s\n安装时间：%s\n更新时间：%s\nMD5签名：%s", item.packageName,
                        item.versionName, (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) item.longVersionCode else item.versionCode),
                        sdf.format(Date(item.firstInstallTime)), sdf.format(Date(item.lastUpdateTime)), signMsg)
                AlertDialog.Builder(this)
                        .setTitle(item.applicationInfo.loadLabel(packageManager))
                        .setMessage(msg)
                        .setPositiveButton("打开") { _, _ ->
                            try {
                                val intent = packageManager.getLaunchIntentForPackage(item.packageName)
                                intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                startActivity(intent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }.setNeutralButton("应用详情") { _, _ ->
                            try {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                val uri = Uri.fromParts("package", item.packageName, null)
                                intent.data = uri
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                startActivity(intent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }

                        }.setNegativeButton("取消", null)
                        .show()
            }
        }

        loadAllApps()
    }

    override fun onDestroy() {
        compositeDisposable?.dispose()
        super.onDestroy()
    }

    private fun loadAllApps() {
        //异步加载从系统获取的app列表
        Single.fromCallable { return@fromCallable packageManager.getInstalledPackages(0) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : SingleObserver<List<PackageInfo>> {
                    override fun onSubscribe(d: Disposable) {
                        compositeDisposable?.add(d)
                    }

                    override fun onSuccess(t: List<PackageInfo>) {
                        listAll = t
                        updateUI()
                    }

                    override fun onError(e: Throwable) {
                    }
                })
    }

    private fun updateUI() {
        val list: MutableList<PackageInfo> = mutableListOf()
        listAll?.forEach {
            val sys = filter != 1
            val user = filter != 2
            if (user && it.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM == 0) { //非系统应用
                list.add(it)
            } else if (sys && it.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0) { //系统应用
                list.add(it)
            }
        }
        list.sortWith(Comparator { p0, p1 ->
            when (sort) {
                1 -> (p1.firstInstallTime - p0.firstInstallTime).sign
                2 -> (p1.lastUpdateTime - p0.lastUpdateTime).sign
                else -> p0.packageName?.compareTo(p1.packageName ?: "") ?: 0
            }
        })
        adapter?.setNewData(list)
        mappsTitle.text = "应用管理(" + list.size + ")"
    }


    class ManageAppsAdapter : BaseQuickAdapter<PackageInfo, BaseViewHolder>(R.layout.mapps_item) {
        override fun convert(helper: BaseViewHolder, item: PackageInfo?) {
            if (item != null) {
                ManageAppsHelper.loadIcon(helper.getView(R.id.mapps_item_icon),
                        item.packageName!!, (mContext as ManageAppsActivity).compositeDisposable)
                ManageAppsHelper.loadLabel(helper.getView(R.id.mapps_item_title),
                        item.packageName!!, (mContext as ManageAppsActivity).compositeDisposable)
            }
        }
    }
}
