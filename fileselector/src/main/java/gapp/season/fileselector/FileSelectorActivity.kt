@file:Suppress("DEPRECATION")

package gapp.season.fileselector

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import gapp.season.util.file.FileUtil
import gapp.season.util.text.StringUtil
import gapp.season.util.view.ThemeUtil
import kotlinx.android.synthetic.main.filesel_activity.*
import java.io.File

class FileSelectorActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_ALLOW_DIR = "extra_allow_dir"
        const val EXTRA_FILE_PATH = "extra_file_path"
    }

    private var sdCards: List<File>? = null
    private var allowDir = false
    private var dir: File? = null

    private var useGrid = false
    private var linearAdapter: LinearAdapter? = null
    private var gridAdapter: GridAdapter? = null

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        getIntentData(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeUtil.setTheme(this, 0)
        setContentView(R.layout.filesel_activity)
        fileselBack.setOnClickListener { finish() }
        fileselUp.setOnClickListener {
            if (dir?.exists() == true) {
                sdCards?.forEach {
                    if (it == dir) {
                        dir = null
                        return@forEach
                    }
                }
                dir = dir?.parentFile
            } else {
                dir = null
            }
            updateUI()
        }
        fileselDisk.setOnClickListener {
            dir = null
            updateUI()
        }
        fileselToggle.setOnClickListener {
            useGrid = !useGrid
            updateAdapter()
        }

        linearAdapter = LinearAdapter()
        gridAdapter = GridAdapter()
        val itemClickListener = BaseQuickAdapter.OnItemClickListener { adapter, _, position ->
            val item = adapter.getItem(position)
            if (item is File) {
                if (item.isDirectory) {
                    dir = item
                    updateUI()
                } else {
                    FileSelectorHelper.listener?.onClickFile(this, item)
                }
            }
        }
        linearAdapter?.onItemClickListener = itemClickListener
        gridAdapter?.onItemClickListener = itemClickListener
        updateAdapter()

        val sdCards: MutableList<File> = mutableListOf()
        FileUtil.getSdCards(this).forEach {
            sdCards.add(File(it))
        }
        this.sdCards = sdCards

        getIntentData(intent)
    }

    override fun onBackPressed() {
        if (dir != null) {
            fileselUp.performClick()
        } else {
            super.onBackPressed()
        }
    }

    private fun getIntentData(intent: Intent?) {
        allowDir = intent?.extras?.getBoolean(EXTRA_ALLOW_DIR) ?: false
        var path = intent?.extras?.getString(EXTRA_FILE_PATH)
        if (path.isNullOrEmpty()) {
            //使用上次的目录
            path = FileSelectorHelper.getSavedFile(this)
        }
        dir = if (path.isNullOrEmpty()) null else File(path)
        updateUI()
    }

    private fun updateAdapter() {
        if (useGrid) {
            val dm = resources.displayMetrics
            val spanCount = (dm.widthPixels / dm.density / 79).toInt()
            val layoutManager = GridLayoutManager(this, spanCount)
            fileselList.layoutManager = layoutManager
            fileselList.adapter = gridAdapter
        } else {
            val layoutManager = LinearLayoutManager(this)
            layoutManager.orientation = RecyclerView.VERTICAL
            fileselList.layoutManager = layoutManager
            fileselList.adapter = linearAdapter
        }
    }

    @SuppressLint("DefaultLocale")
    private fun updateUI() {
        if (dir == null || !dir!!.exists()) {
            dir = null
            fileselPath.text = "存储卡列表"
            linearAdapter?.setNewData(sdCards)
            gridAdapter?.setNewData(sdCards)
        } else {
            if (!dir!!.isDirectory) {
                dir = dir!!.parentFile
                if (dir == null) {
                    updateUI()
                    return
                }
            }
            fileselPath.text = dir!!.absolutePath
            val list = mutableListOf<File>()
            dir!!.listFiles()?.forEach { list.add(it) }
            //按文件名排序(MutableList才有sortWith方法)
            list.sortWith(Comparator { p0, p1 ->
                val d0 = if (p0.isDirectory) 0 else 1
                val d1 = if (p1.isDirectory) 0 else 1
                if (d0 == d1) {
                    return@Comparator StringUtil.compare(p0.name.toLowerCase(), p1.name.toLowerCase(), "GBK")
                } else {
                    return@Comparator d0 - d1
                }
            })
            linearAdapter?.setNewData(list)
            gridAdapter?.setNewData(list)
        }
        fileselList.scrollToPosition(0)
    }

    class LinearAdapter : BaseQuickAdapter<File, BaseViewHolder>(R.layout.filesel_item) {
        override fun convert(helper: BaseViewHolder, item: File?) {
            (mContext as FileSelectorActivity).convert(mContext, item, helper)
        }
    }

    class GridAdapter : BaseQuickAdapter<File, BaseViewHolder>(R.layout.filesel_item_grid) {
        override fun convert(helper: BaseViewHolder, item: File?) {
            (mContext as FileSelectorActivity).convert(mContext, item, helper)
        }
    }

    private fun convert(context: Context, item: File?, helper: BaseViewHolder) {
        if (item != null) {
            if (item.isDirectory) {
                Glide.with(context).load(R.drawable.filesel_format_folder).into(helper.getView(R.id.filesel_icon))
                helper.setGone(R.id.filesel_select, (context as FileSelectorActivity).allowDir)
            } else {
                Glide.with(context).load(item).placeholder(R.drawable.filesel_format_file).into(helper.getView(R.id.filesel_icon))
                helper.setGone(R.id.filesel_select, true)
            }
            helper.setText(R.id.filesel_title, item.name)
            helper.setOnClickListener(R.id.filesel_select) {
                FileSelectorHelper.saveFile(context, item)
                val activity = (context as FileSelectorActivity)
                val data = Intent()
                data.data = Uri.fromFile(item)
                activity.setResult(Activity.RESULT_OK, data)
                activity.finish()
            }
        }
    }
}
