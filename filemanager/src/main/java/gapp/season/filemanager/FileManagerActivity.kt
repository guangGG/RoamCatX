@file:Suppress("DEPRECATION")

package gapp.season.filemanager

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import gapp.season.imageviewer.ImageViewerHelper
import gapp.season.mediastore.MediaStoreHelper
import gapp.season.musicplayer.MusicPlayerHelper
import gapp.season.textviewer.TextViewerHelper
import gapp.season.util.file.FileUtil
import gapp.season.util.log.LogUtil
import gapp.season.util.task.ThreadPoolExecutor
import gapp.season.util.tips.AlertUtil
import gapp.season.util.tips.ToastUtil
import gapp.season.util.view.ThemeUtil
import gapp.season.videoplayer.VideoPlayerHelper
import kotlinx.android.synthetic.main.fm_activity.*
import java.io.File

@Suppress("SameParameterValue")
@SuppressLint("SetTextI18n")
class FileManagerActivity : AppCompatActivity() {
    private var isSafe = false
    private var progressDialog: ProgressDialog? = null

    private var fmStack: FileManagerStack? = null
    private var originalPath: String? = null
    var inSelectMode = false
        set(value) {
            field = value
            fmStack?.getTopFragment()?.inSelectMode = value
            updateToolBar()
        }
    private var favorAdapter: FavorAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getIntentData(intent)
        ThemeUtil.setTheme(this, 0)
        setContentView(R.layout.fm_activity)
        initView()
        initData()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        getIntentData(intent)
        openOriginalPath()
    }

    private fun getIntentData(intent: Intent?) {
        originalPath = intent?.getStringExtra("path")
    }

    private fun initView() {
        fmMenu.setOnClickListener {
            //打开抽屉菜单
            if (!fmDrawerLayout.isDrawerOpen(GravityCompat.START)) {
                fmDrawerLayout.openDrawer(GravityCompat.START)
            }
        }
        fmSuperTitle.setOnClickListener {
            //进入父目录
            val currentDir = fmStack?.getTopFragment()?.dir
            if (!currentDir.isNullOrEmpty()) {
                val dirFile = File(currentDir)
                if (dirFile.exists() && dirFile.parentFile?.exists() == true) {
                    FileManager.enter(this, dirFile.parentFile.absolutePath)
                }
            }
        }
        fmTitle.setOnClickListener {
            val sdCards = FileUtil.getSdCards(this)
            val items = Array((FileManagerBuffer.fmHistory?.size ?: 0) + sdCards.size) {
                if (it < sdCards.size) {
                    "卡${it + 1}: ${sdCards[it]}"
                } else {
                    FileManagerBuffer.fmHistory?.get(it - sdCards.size) ?: "(空)"
                }
            }
            AlertDialog.Builder(this)
                    .setTitle("历史记录")
                    .setItems(items) { _, which ->
                        val path = if (which < sdCards.size) {
                            sdCards[which]
                        } else {
                            FileManagerBuffer.fmHistory?.get(which - sdCards.size)
                        }
                        FileManager.enter(this, path)
                    }.setPositiveButton("关闭", null)
                    .setNeutralButton("清空") { _, _ ->
                        FileManagerBuffer.clearHistory()
                    }.show()
        }
        fmClose.setOnClickListener { finish() }

        fmToolBtnDone.setOnClickListener { inSelectMode = false }
        fmToolBtnSelectInterval.setOnClickListener { fmStack?.getTopFragment()?.onSelectBtn(true) }
        fmToolBtnSelectAll.setOnClickListener { fmStack?.getTopFragment()?.onSelectBtn(false) }
        fmToolBtnDelete.setOnClickListener {
            //删除文件并更新系统媒体库
            val files = fmStack?.getTopFragment()?.getSelectFiles()
            if (!files.isNullOrEmpty()) {
                AlertUtil.confirm(this, "删除提示", "您确定要删除这${files.size}项吗？",
                        "确定", "取消", true) { code, _, _ ->
                    if (code == AlertUtil.POSITIVE_BUTTON) {
                        showLoading("删除中…")
                        ThreadPoolExecutor.getInstance().execute {
                            var count = 0
                            files.forEach {
                                if (FileUtil.deleteFile(it)) count++
                            }
                            runOnUiThread {
                                ToastUtil.showShort("${count}项删除完成")
                                if (isSafe) {
                                    hideLoading()
                                    inSelectMode = false
                                    fmStack?.getTopFragment()?.updateList()
                                }
                            }
                        }
                    }
                }
            } else {
                ToastUtil.showShort("请选择要删除的文件")
            }
        }
        fmToolBtnFavor.setOnClickListener {
            val files = fmStack?.getTopFragment()?.getSelectFiles()
            if (!files.isNullOrEmpty()) {
                files.forEach {
                    FileManagerBuffer.putFavorite(it.absolutePath)
                }
                inSelectMode = false
                ToastUtil.showShort("收藏成功")
                favorAdapter?.setNewData(FileManagerBuffer.fmFavorites)
            } else {
                ToastUtil.showShort("请选择要收藏的文件")
            }
        }
        fmToolBtnDetail.setOnClickListener {
            val files = fmStack?.getTopFragment()?.getSelectFiles()
            if (!files.isNullOrEmpty()) {
                FileManager.showFilesDetail(this, files)
            } else {
                ToastUtil.showShort("请选择文件")
            }
        }
        fmToolBtnOpenAs.setOnClickListener {
            val files = fmStack?.getTopFragment()?.getSelectFiles()
            if (files?.size == 1 && files[0].isFile) {
                FileManager.openAs(this, files[0], true)
                inSelectMode = false
            } else {
                ToastUtil.showShort("请选择单个文件")
            }
        }
        //将使用的图标替换成白色
        fmTitleDivider.setColorFilter(Color.WHITE)
        fmToolBtnDone.setColorFilter(Color.WHITE)
        fmToolBtnSelectInterval.setColorFilter(Color.WHITE)
        fmToolBtnSelectAll.setColorFilter(Color.WHITE)
        fmToolBtnDelete.setColorFilter(Color.WHITE)
        fmToolBtnFavor.setColorFilter(Color.WHITE)
        fmToolBtnDetail.setColorFilter(Color.WHITE)
        fmToolBtnOpenAs.setColorFilter(Color.WHITE)

        //抽屉菜单
        val menuLayout = fmStartDrawer.getHeaderView(0)
        fmStartDrawer.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.fmDrawerMenuStoreDisks -> {
                    FileManager.enter(this, FileManager.DISKS_DIR_TAG)
                    fmDrawerLayout.closeDrawer(GravityCompat.START)
                }
                R.id.fmDrawerMenuStoreDownload -> {
                    val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    FileManager.enter(this, downloadDir.absolutePath)
                    fmDrawerLayout.closeDrawer(GravityCompat.START)
                }
                R.id.fmDrawerMenuStoreImg -> MediaStoreHelper.showGallery(this)
                R.id.fmDrawerMenuStoreVideo -> MediaStoreHelper.showVideos(this)
                R.id.fmDrawerMenuStoreAudio -> MediaStoreHelper.showMusics(this)
            }
            return@setNavigationItemSelectedListener true
        }
        menuLayout.findViewById<TextView>(R.id.fmMenuClear).setOnClickListener {
            AlertUtil.confirm(this, "温馨提示", "您确定要清空收藏夹吗？",
                    "确定", "取消", true) { code, _, _ ->
                if (code == AlertUtil.POSITIVE_BUTTON) {
                    FileManagerBuffer.clearFavorite()
                    ToastUtil.showShort("已清空收藏夹")
                    favorAdapter?.setNewData(FileManagerBuffer.fmFavorites)
                }
            }
        }
        val fmMenuList = menuLayout.findViewById<RecyclerView>(R.id.fmMenuList)
        val layoutManager = LinearLayoutManager(this)
        layoutManager.orientation = RecyclerView.VERTICAL
        fmMenuList.layoutManager = layoutManager
        favorAdapter = FavorAdapter()
        fmMenuList.adapter = favorAdapter
        favorAdapter?.setOnItemClickListener { adapter, _, position ->
            val item = adapter.getItem(position)
            if (item is String) FileManager.enter(this, item)
            fmDrawerLayout.closeDrawer(GravityCompat.START)
        }
        favorAdapter?.setOnItemLongClickListener { adapter, _, position ->
            val item = adapter.getItem(position)
            if (item is String) AlertUtil.confirm(this, "温馨提示", "您确定要删除“${item}”吗？",
                    "确定", "取消", true) { code, _, _ ->
                if (code == AlertUtil.POSITIVE_BUTTON) {
                    FileManagerBuffer.removeFavorite(item)
                    favorAdapter?.setNewData(FileManagerBuffer.fmFavorites)
                }
            }
            true
        }
        fmDrawerLayout.addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {
            override fun onDrawerOpened(drawerView: View) {
                favorAdapter?.setNewData(FileManagerBuffer.fmFavorites) //更新收藏夹
            }

            override fun onDrawerClosed(drawerView: View) {
                fmStack?.getTopFragment()?.updateList() //更新列表
            }
        })

        updateToolBar()
    }

    private fun initData() {
        isSafe = true
        fmStack = FileManagerStack(this, R.id.fmFragmentContainer)
        fmStack?.listener = object : FileManagerStack.OnFmStackListener {
            override fun onStackChange(diskStack: FileManagerStack.SafeStack<FileManagerFragment>, push: Boolean) {
                LogUtil.v("FileManager", "onStackChange diskStack size: ${diskStack.size}")
                if (!push) showPath(fmStack?.getTopFragment()?.dir)
            }
        }
        favorAdapter?.setNewData(FileManagerBuffer.fmFavorites)
        openOriginalPath()
    }

    private fun openOriginalPath() {
        showPath(originalPath, true)
    }

    private fun showPath(path: String?, openPath: Boolean = false) {
        val isTopDir: Boolean
        var superDir: String? = null
        val currentDir: String?
        val dirPath: String?
        var fileName: String? = null
        when (path) {
            null -> {
                isTopDir = true
                if (FileUtil.getSdCards(this).size > 1) {
                    currentDir = FileManager.DISKS_DIR_TAG
                    dirPath = FileManager.DISKS_DIR_TAG
                } else {
                    currentDir = Environment.getExternalStorageDirectory().name
                    dirPath = Environment.getExternalStorageDirectory().absolutePath
                }
            }
            FileManager.DISKS_DIR_TAG -> {
                isTopDir = true
                currentDir = FileManager.DISKS_DIR_TAG
                dirPath = FileManager.DISKS_DIR_TAG
            }
            else -> {
                val file = File(path)
                if (file.exists()) {
                    var dir = file
                    if (!file.isDirectory) {
                        dir = file.parentFile
                        fileName = file.name
                    }
                    isTopDir = FileManager.isTopDir(dir)
                    currentDir = dir.name
                    if (!isTopDir) {
                        superDir = dir.parentFile.name
                    }
                    dirPath = dir.absolutePath
                } else {
                    isTopDir = true
                    currentDir = FileManager.DISKS_DIR_TAG
                    dirPath = FileManager.DISKS_DIR_TAG
                }
            }
        }
        if (isTopDir) {
            fmSuperTitle.visibility = View.GONE
            fmTitleDivider.visibility = View.GONE
        } else {
            fmSuperTitle.visibility = View.VISIBLE
            fmTitleDivider.visibility = View.VISIBLE
            fmSuperTitle.text = if (superDir.isNullOrEmpty()) "/" else superDir
        }
        fmTitle.text = if (currentDir.isNullOrEmpty()) "/" else currentDir
        if (openPath) {
            inSelectMode = false //切换页面时退出多选模式
            fmStack?.push(dirPath, fileName) //进入目录
        }
    }

    private fun updateToolBar() {
        if (inSelectMode) {
            fmToolBarTitle.visibility = View.GONE
            fmToolBarSelect.visibility = View.VISIBLE
            fmToolBarOption.visibility = View.VISIBLE
        } else {
            fmToolBarTitle.visibility = View.VISIBLE
            fmToolBarSelect.visibility = View.GONE
            fmToolBarOption.visibility = View.GONE
        }
    }

    fun onSelectItemUpdate(selectNum: Int, totalNum: Int) {
        fmSelectNum.text = "$selectNum/$totalNum"
        if (selectNum != 0 && selectNum == totalNum) {
            fmToolBtnSelectAll.setImageResource(R.drawable.fm_toolbar_edit_selectall)
        } else {
            fmToolBtnSelectAll.setImageResource(R.drawable.fm_toolbar_edit_selectnone)
        }
    }

    private fun showLoading(msg: String) {
        if (progressDialog == null) {
            progressDialog = ProgressDialog(this)
            progressDialog!!.setMessage(msg)
            progressDialog!!.setCancelable(false)
        }
        progressDialog?.show()
    }

    private fun hideLoading() {
        progressDialog?.dismiss()
    }

    override fun onBackPressed() {
        when {
            fmDrawerLayout.isDrawerOpen(GravityCompat.START) -> fmDrawerLayout.closeDrawer(GravityCompat.START)
            inSelectMode -> inSelectMode = false
            else -> {
                if (fmStack?.pop() != false) {
                    super.onBackPressed()
                }
            }
        }
    }

    override fun onDestroy() {
        isSafe = false
        fmStack = null
        favorAdapter = null
        super.onDestroy()
    }


    class FavorAdapter : BaseQuickAdapter<String, BaseViewHolder>(R.layout.fm_item_file) {
        override fun convert(helper: BaseViewHolder, item: String?) {
            if (item != null) {
                val file = File(item)
                when {
                    file.isDirectory -> Glide.with(mContext).load(R.drawable.fm_format_folder).into(helper.getView(R.id.fm_file_icon))
                    ImageViewerHelper.isImage(file) -> Glide.with(mContext).load(item).placeholder(R.drawable.fm_format_picture).into(helper.getView(R.id.fm_file_icon))
                    VideoPlayerHelper.isVideoFile(file) -> Glide.with(mContext).load(item).placeholder(R.drawable.fm_format_media).into(helper.getView(R.id.fm_file_icon))
                    MusicPlayerHelper.isMusicFile(file, false) -> Glide.with(mContext).load(R.drawable.fm_format_music).into(helper.getView(R.id.fm_file_icon))
                    TextViewerHelper.isTextFile(file) -> Glide.with(mContext).load(item).placeholder(R.drawable.fm_format_text).into(helper.getView(R.id.fm_file_icon))
                    else -> Glide.with(mContext).load(R.drawable.fm_format_file).into(helper.getView(R.id.fm_file_icon))
                }
                helper.setText(R.id.fm_file_name, file.name)
                helper.setGone(R.id.fm_file_infos, false)
                helper.setGone(R.id.fm_select_file, false)
            }
        }
    }
}
