package gapp.season.filemanager

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.CheckBox
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import gapp.season.imageviewer.ImageViewerHelper
import gapp.season.musicplayer.MusicPlayerHelper
import gapp.season.textviewer.TextViewerHelper
import gapp.season.util.file.FileShareUtil
import gapp.season.util.file.FileTypeUtil
import gapp.season.util.file.FileUtil
import gapp.season.util.sys.ClipboardUtil
import gapp.season.util.sys.MemoryUtil
import gapp.season.util.task.ThreadPoolExecutor
import gapp.season.util.tips.ToastUtil
import gapp.season.videoplayer.VideoPlayerHelper
import java.io.File

//手机文件管理推荐ES文件浏览器： http://www.estrongs.com/
object FileManager {
    const val DISKS_DIR_TAG = "存储卡列表"
    private var application: Application? = null
    internal var configDir: String? = null
    private var isDev = false
    //缓存打开方式应用标签和图标
    private val openAsLabels = mutableMapOf<String, String>()
    private val openAsIcons = mutableMapOf<String, Drawable>()
    private var setOpenManner = false
    private var detailTimeTag = 0L

    fun init(application: Application, configDir: String, isDev: Boolean) {
        this.application = application
        this.configDir = configDir
        this.isDev = isDev
        FileManagerBuffer.init()
        if (!isDev) FileManagerBuffer.setOpenFileMine(application, false)
        cacheOpenAsInfos(application)
    }

    fun enter(context: Context, path: String? = null) {
        val intent = Intent(context, FileManagerActivity::class.java)
        intent.putExtra("path", path)
        if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun getFavorList(): List<String> {
        val list: MutableList<String> = mutableListOf()
        FileManagerBuffer.fmFavorites?.forEach {
            list.add(it)
        }
        return list
    }

    @SuppressLint("SdCardPath")
    internal fun isTopDir(dir: File?): Boolean {
        if (dir?.exists() == true && dir.isDirectory) {
            if ("/" == dir.absolutePath || "/sdcard" == dir.absolutePath || "/sdcard/" == dir.absolutePath) return true
            val sdCards = FileUtil.getSdCards(application)
            sdCards.forEach {
                if (dir.absolutePath == it) {
                    return true
                }
            }
        }
        return false
    }

    fun openFileWithDefault(context: Context, file: File) {
        when {
            !file.exists() -> ToastUtil.showShort("文件打开失败：文件不存在")
            file.isDirectory -> {
                enter(context, file.absolutePath)
            }
            ImageViewerHelper.isImage(file) -> ImageViewerHelper.showImage(context, file.absolutePath)
            VideoPlayerHelper.isVideoFile(file) -> VideoPlayerHelper.play(context, file.absolutePath)
            MusicPlayerHelper.isMusicFile(file, true) -> MusicPlayerHelper.play(context, file.absolutePath)
            TextViewerHelper.isTextFile(file) -> TextViewerHelper.show(context, file.absolutePath)
            else -> TextViewerHelper.show(context, file.absolutePath)
        }
    }

    internal fun openAs(activity: Activity?, file: File?, forceChoice: Boolean = false, choiceInAll: Boolean = false) {
        if (activity != null && file != null) {
            if (!file.exists()) {
                ToastUtil.showShort("文件打开失败，文件不存在")
            } else if (file.isDirectory) {
                enter(activity, file.absolutePath)
            } else {
                val suffix = FileUtil.getExtName(file.name)
                if (!forceChoice) {
                    val pkg = FileManagerBuffer.getOpenMannerPkg(activity, suffix)
                    val cls = FileManagerBuffer.getOpenMannerCls(activity, suffix)
                    if (!suffix.isNullOrEmpty() && !pkg.isNullOrEmpty() && !cls.isNullOrEmpty()) {
                        FileShareUtil.allowFileUriExposure() //防止Android-N报错: FileUriExposedException
                        FileTypeUtil.openFileOfComponent(activity, file, pkg, cls)
                        return
                    }
                }
                //方式1：使用系统文件打开弹窗
                /*FileShareUtil.allowFileUriExposure() //防止Android-N报错: FileUriExposedException
                activity.startActivity(FileTypeUtil.getOpenFileIntent(file))*/
                //方式2：自定义文件打开弹窗
                val resolveFile = if (choiceInAll) File("") else file
                val resolveInfos = FileTypeUtil.getOpenFileResolveInfos(activity, resolveFile)
                var dialog: Dialog? = null
                val builder = AlertDialog.Builder(activity).setTitle("打开: ${file.name}")
                val layout = View.inflate(activity, R.layout.fm_open_as_layout, null)
                val openMine = layout.findViewById<CheckBox>(R.id.fmOpenMine)
                openMine.isChecked = FileManagerBuffer.isOpenFileMine(activity)
                openMine.setOnCheckedChangeListener { _, isChecked -> FileManagerBuffer.setOpenFileMine(activity, isChecked) }
                openMine.visibility = if (isDev) View.VISIBLE else View.GONE
                val setDefault = layout.findViewById<CheckBox>(R.id.fmOpenAsCheck)
                setDefault.isChecked = setOpenManner
                setDefault.setOnCheckedChangeListener { _, isChecked -> setOpenManner = isChecked }
                val listView = layout.findViewById<RecyclerView>(R.id.fmOpenAsList)
                val layoutManager = LinearLayoutManager(activity)
                layoutManager.orientation = RecyclerView.VERTICAL
                listView.layoutManager = layoutManager
                val adapter = object : BaseQuickAdapter<ResolveInfo, BaseViewHolder>(R.layout.fm_item_file, resolveInfos) {
                    override fun convert(helper: BaseViewHolder, item: ResolveInfo?) {
                        if (item != null) {
                            val icon = getResolveIcon(activity, item)
                            if (icon != null) {
                                helper.setImageDrawable(R.id.fm_file_icon, icon)
                            } else {
                                helper.setImageResource(R.id.fm_file_icon, R.drawable.fm_format_file)
                            }
                            helper.setText(R.id.fm_file_name, getResolveLabel(activity, item))
                            val pkg = FileManagerBuffer.getOpenMannerPkg(activity, suffix)
                            val cls = FileManagerBuffer.getOpenMannerCls(activity, suffix)
                            val isSetOpenManner = (pkg != null && cls != null && ("$pkg-$cls" == "${item.activityInfo.packageName}-${item.activityInfo.name}"))
                            if (isSetOpenManner) {
                                helper.setGone(R.id.fm_file_infos, true)
                                helper.setText(R.id.fm_file_size, "已设置为默认打开方式")
                                helper.setGone(R.id.fm_file_authority, false)
                                helper.setGone(R.id.fm_file_date, false)
                            } else {
                                helper.setGone(R.id.fm_file_infos, false)
                            }
                            helper.setGone(R.id.fm_select_file, false)
                        }
                    }
                }
                adapter.setOnItemClickListener { quickAdapter, _, position ->
                    val resolveInfo = quickAdapter.getItem(position)
                    if (resolveInfo is ResolveInfo) {
                        val pkgItem = resolveInfo.activityInfo?.packageName
                        val clsItem = resolveInfo.activityInfo?.name
                        if (!pkgItem.isNullOrEmpty() && !clsItem.isNullOrEmpty()) {
                            FileShareUtil.allowFileUriExposure() //防止Android-N报错: FileUriExposedException
                            FileTypeUtil.openFileOfComponent(activity, file, pkgItem, clsItem)
                            if (setOpenManner) {
                                FileManagerBuffer.setOpenManner(activity, suffix, pkgItem, clsItem)
                            }
                            dialog?.dismiss()
                        } else {
                            ToastUtil.showShort("打开文件失败，要打开的应用不存在")
                        }
                    }
                }
                listView.adapter = adapter
                layout.findViewById<View>(R.id.fmOpenAsRemove).setOnClickListener {
                    FileManagerBuffer.removeOpenManner(activity, suffix)
                    adapter.notifyDataSetChanged()
                    ToastUtil.showShort("已清除当前文件类型的默认打开方式")
                }
                layout.findViewById<View>(R.id.fmOpenAsClear).setOnClickListener {
                    FileManagerBuffer.clearOpenManner(activity)
                    adapter.notifyDataSetChanged()
                    ToastUtil.showShort("已清除全部文件类型的默认打开方式")
                }
                builder.setView(layout)
                builder.setPositiveButton("关闭", null)
                if (!choiceInAll) builder.setNeutralButton("显示全部打开方式") { _, _ ->
                    openAs(activity, file, forceChoice = true, choiceInAll = true)
                }
                dialog = builder.show()
            }
        }
    }

    private fun getResolveLabel(context: Context, resolveInfo: ResolveInfo?): String {
        val pkg = resolveInfo?.activityInfo?.packageName
        val cls = resolveInfo?.activityInfo?.name
        var label = openAsLabels["$pkg-$cls"]
        if (label.isNullOrEmpty()) {
            label = resolveInfo?.loadLabel(context.packageManager)?.toString()
            if (!label.isNullOrEmpty()) openAsLabels["$pkg-$cls"] = label
        }
        return label ?: "--"
    }

    private fun getResolveIcon(context: Context, resolveInfo: ResolveInfo?): Drawable? {
        val pkg = resolveInfo?.activityInfo?.packageName
        val cls = resolveInfo?.activityInfo?.name
        var icon = openAsIcons["$pkg-$cls"]
        if (icon == null) {
            icon = resolveInfo?.loadIcon(context.packageManager)
            if (icon != null) openAsIcons["$pkg-$cls"] = icon
        }
        return icon
    }

    private fun cacheOpenAsInfos(context: Context) {
        //初始化时预缓存各种打开方式的图标和标签，防止后面使用时出现卡顿
        ThreadPoolExecutor.getInstance().execute {
            val resolveInfos = FileTypeUtil.getOpenFileResolveInfos(context, File(""))
            resolveInfos?.forEach {
                getResolveLabel(context, it)
                getResolveIcon(context, it)
            }
        }
    }

    internal fun showFilesDetail(activity: Activity, files: MutableList<File>) {
        val msg = StringBuilder()
        var fCount = 0
        var dCount = 0
        for (f in files) {
            if (f.isDirectory) {
                dCount++
            } else {
                fCount++
            }
        }
        if (fCount + dCount > 0) {
            if (fCount == 1 && dCount == 0) {
                msg.append("文件名: ${files[0].name}\n")
            } else {
                msg.append("已选择: ${fCount}个文件,${dCount}个目录\n")
            }
            msg.append("所在目录: ${files[0].parent}\n")
            msg.append("包含文件: %1\$s\n")
            msg.append("文件大小: %2\$s\n")
        }
        val format = msg.toString().trim()
        var fileCount = "计算中…"
        var fileSize = "计算中…"
        val dialog = AlertDialog.Builder(activity)
                .setTitle("文件详情")
                .setMessage(String.format(format, fileCount, fileSize))
                .setPositiveButton("关闭", null)
                .setNeutralButton("复制文件路径") { _, _ ->
                    when {
                        files.size == 1 -> {
                            ClipboardUtil.putText(activity, files[0].absolutePath)
                            ToastUtil.showShort("复制文件路径成功")
                        }
                        files.size > 1 -> {
                            ClipboardUtil.putText(activity, files[0].parent)
                            ToastUtil.showShort("复制文件路径成功")
                        }
                        else -> ToastUtil.showShort("文件不存在")
                    }
                }.show()
        val time = System.currentTimeMillis()
        detailTimeTag = time
        ThreadPoolExecutor.getInstance().execute {
            fileCount = "--"
            try {
                fileCount = getFileContains(files)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            Handler(Looper.getMainLooper()).post {
                if (detailTimeTag == time && dialog.isShowing)
                    dialog.setMessage(String.format(format, fileCount, fileSize))
            }
        }
        ThreadPoolExecutor.getInstance().execute {
            fileSize = "--"
            try {
                val size = getFileSize(files)
                fileSize = "${MemoryUtil.formatMemorySize(size, 2)} (" + MemoryUtil.FormatLongNum(size) + "字节)"
            } catch (e: Exception) {
                e.printStackTrace()
            }
            Handler(Looper.getMainLooper()).post {
                if (detailTimeTag == time && dialog.isShowing)
                    dialog.setMessage(String.format(format, fileCount, fileSize))
            }
        }
    }

    //获取所有子文件和子目录数量(耗时操作,需在子线程中进行)
    private fun getFileContains(files: List<File>?): String {
        if (files.isNullOrEmpty()) return "0个文件,0个目录"
        var fCount: Long = 0
        var dCount: Long = 0
        for (file in files) {
            fCount += getFileContainsFile(file)
            dCount += getFileContainsDir(file)
        }
        return fCount.toString() + "个文件," + dCount + "个目录"
    }

    private fun getFileContainsFile(file: File): Long {
        var size: Long = 0
        if (!file.isDirectory) {
            return 1
        } else {
            val files = file.listFiles()
            if (files != null)
                for (f in files) {
                    size += getFileContainsFile(f)
                }
        }
        return size
    }

    private fun getFileContainsDir(file: File): Long {
        var size: Long = 0
        if (!file.isDirectory) {
            return 0
        } else {
            size += 1
            val files = file.listFiles()
            if (files != null)
                for (f in files) {
                    size += getFileContainsDir(f)
                }
        }
        return size
    }

    //计算多个文件、目录的大小(耗时操作,需在子线程中进行)
    private fun getFileSize(files: List<File>?): Long {
        if (files.isNullOrEmpty()) return 0
        var size: Long = 0
        for (file in files) {
            size += getFileSize(file)
        }
        return size
    }

    private fun getFileSize(file: File): Long {
        var size: Long = 0
        if (!file.isDirectory) {
            size = file.length()
        } else {
            val files = file.listFiles()
            if (files != null)
                for (f in files) {
                    size += getFileSize(f)
                }
        }
        return size
    }
}
