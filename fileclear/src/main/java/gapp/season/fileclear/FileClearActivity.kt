@file:Suppress("DEPRECATION")

package gapp.season.fileclear

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.os.Bundle
import android.os.Environment
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.google.android.material.appbar.AppBarLayout
import gapp.season.util.task.OnTaskDone
import gapp.season.util.text.StringUtil
import gapp.season.util.tips.AlertUtil
import gapp.season.util.tips.ToastUtil
import gapp.season.util.view.ThemeUtil
import kotlinx.android.synthetic.main.fclear_activity.*
import java.io.File

@SuppressLint("DefaultLocale")
class FileClearActivity : AppCompatActivity() {
    private var progressDialog: ProgressDialog? = null
    private var adapter: Adapter? = null
    private var hasScroll = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeUtil.setTheme(this, 0)
        setContentView(R.layout.fclear_activity)
        initView()
        initData()
    }

    override fun onRestart() {
        super.onRestart()
        updateList()
    }

    private fun initView() {
        fclearBack.setOnClickListener { finish() }
        fclearClear.setOnClickListener {
            AlertUtil.confirm(this, "清理提示", "清理掉的文件不可恢复，您确定按当前清理名单配置清理文件吗？",
                    "确定", "取消", true) { code, _, _ ->
                if (code == AlertUtil.POSITIVE_BUTTON) {
                    showLoading()
                    FileClearHelper.clearList(adapter?.clearList, OnTaskDone { _, _, ret ->
                        hideLoading()
                        updateList()
                        if (ret) {
                            ToastUtil.showShort("一键清理完成")
                        } else {
                            ToastUtil.showShort("一键清理失败")
                        }
                    })
                }
            }
        }
        fclearTips.setOnClickListener {
            val list = adapter?.clearList
            list?.sortWith(Comparator { p0, p1 ->
                return@Comparator StringUtil.compare(p0.toLowerCase(), p1.toLowerCase(), "GBK")
            })
            val msg = StringBuilder()
            list?.forEach {
                if (msg.isNotEmpty()) msg.append("\n")
                msg.append(it)
            }
            AlertUtil.showMsg(this, "清理名单", msg, "关闭")
        }
        fclearAppBar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
            if (!hasScroll && verticalOffset < 0) {
                updateList() //修复部分手机刚进入页面时RecyclerView长度不正确的问题
                hasScroll = true
            }
        })
        val layoutManager = LinearLayoutManager(this)
        layoutManager.orientation = RecyclerView.VERTICAL
        fclearList.layoutManager = layoutManager
        adapter = Adapter()
        fclearList.adapter = adapter
        adapter?.setOnItemClickListener { adapter, _, position ->
            val item = adapter?.getItem(position)
            if (item is File) {
                FileClearHelper.doOpenFile(this, item)
            }
        }
        adapter?.setOnItemLongClickListener { _, _, _ ->
            adapter?.isEdit = true
            true
        }
    }

    private fun initData() {
        adapter?.clearList = FileClearHelper.getClearList()
        updateList()
    }

    private fun updateList() {
        val files = Environment.getExternalStorageDirectory().listFiles()
        files?.sortWith(Comparator<File> { p0, p1 ->
            val d0 = if (p0.isDirectory) 0 else 1
            val d1 = if (p1.isDirectory) 0 else 1
            if (d0 == d1) {
                return@Comparator StringUtil.compare(p0.name.toLowerCase(), p1.name.toLowerCase(), "GBK")
            } else {
                return@Comparator d0 - d1
            }
        })
        adapter?.setNewData(files?.asList())
    }

    override fun onBackPressed() {
        if (adapter?.isEdit == true) {
            adapter?.isEdit = false
        } else {
            super.onBackPressed()
        }
    }

    private fun showLoading() {
        if (progressDialog == null) {
            progressDialog = ProgressDialog(this)
            progressDialog!!.setMessage("清理中……")
            progressDialog!!.setCancelable(false)
        }
        progressDialog?.show()
    }

    private fun hideLoading() {
        progressDialog?.dismiss()
    }


    class Adapter : BaseQuickAdapter<File, BaseViewHolder>(R.layout.fclear_file_item) {
        var isEdit = false
            set(value) {
                field = value
                notifyDataSetChanged()
            }
        var clearList = mutableListOf<String>()

        override fun convert(helper: BaseViewHolder, item: File?) {
            if (item != null) {
                if (item.isDirectory) {
                    Glide.with(mContext).load(R.drawable.fclear_format_folder).into(helper.getView(R.id.fclear_icon))
                } else {
                    Glide.with(mContext).load(item).placeholder(R.drawable.fclear_format_file).into(helper.getView(R.id.fclear_icon))
                }
                helper.setText(R.id.fclear_title, item.name)
                if (clearList.contains(item.name)) {
                    helper.setGone(R.id.fclear_gray, true)
                    helper.setText(R.id.fclear_discard, "恢复")
                } else {
                    helper.setGone(R.id.fclear_gray, false)
                    helper.setText(R.id.fclear_discard, "清理")
                }
                if (isEdit) {
                    helper.setGone(R.id.fclear_discard, true)
                    helper.setOnClickListener(R.id.fclear_discard) {
                        if (clearList.contains(item.name)) {
                            clearList.remove(item.name)
                        } else {
                            clearList.add(item.name)
                        }
                        notifyDataSetChanged()
                        FileClearHelper.saveClearList(clearList)
                    }
                } else {
                    helper.setGone(R.id.fclear_discard, false)
                    helper.setOnClickListener(R.id.fclear_discard, null)
                }
            }
        }
    }
}
