package gapp.season.filemanager

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import gapp.season.imageviewer.ImageViewerHelper
import gapp.season.musicplayer.MusicPlayerHelper
import gapp.season.textviewer.TextViewerHelper
import gapp.season.util.file.FileUtil
import gapp.season.util.sys.MemoryUtil
import gapp.season.util.text.DateUtil
import gapp.season.util.text.StringUtil
import gapp.season.videoplayer.VideoPlayerHelper
import kotlinx.android.synthetic.main.fm_fragment.*
import java.io.File

class FileManagerFragment : Fragment() {
    private var activity: FileManagerActivity? = null
    var dir: String? = null
    private var toName: String? = null
    private var isDisksDir = false
    private var adapter: Adapter? = null
    var inSelectMode = false
        set(value) {
            field = value
            if (!value) {
                selectItems.clear()
                activity?.onSelectItemUpdate(selectItems.size, adapter?.data?.size ?: 0)
            }
            adapter?.notifyDataSetChanged()
        }
    var selectItems = mutableSetOf<Int>()
    fun getSelectFiles(): MutableList<File> {
        val list = mutableListOf<File>()
        adapter?.data?.forEachIndexed { index, file ->
            if (selectItems.contains(index) && file != null) list.add(file)
        }
        return list
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity = context as FileManagerActivity
    }

    override fun onDetach() {
        activity = null
        super.onDetach()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dir = arguments?.getString("dirPath")
        toName = arguments?.getString("fileName")
        isDisksDir = (FileManager.DISKS_DIR_TAG == dir)
        //加入历史记录
        if (!isDisksDir && !dir.isNullOrEmpty()) {
            FileManagerBuffer.putHistory(dir!!)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fm_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //initView
        val layoutManager = LinearLayoutManager(context)
        layoutManager.orientation = RecyclerView.VERTICAL
        fmFileList.layoutManager = layoutManager
        adapter = Adapter(this)
        fmFileList.adapter = adapter
        adapter?.setOnItemClickListener { adapter, _, position ->
            if (inSelectMode) {
                if (selectItems.contains(position)) {
                    selectItems.remove(position)
                } else {
                    selectItems.add(position)
                }
                activity?.onSelectItemUpdate(selectItems.size, adapter?.data?.size ?: 0)
                adapter?.notifyDataSetChanged()
            } else {
                val item = adapter.getItem(position)
                if (item is File && activity != null) {
                    if (FileManagerBuffer.isOpenFileMine(context)) {
                        FileManager.openFileWithDefault(activity!!, item)
                    } else {
                        FileManager.openAs(activity!!, item)
                    }
                }
            }
        }
        adapter?.setOnItemLongClickListener { _, _, position ->
            selectItems.add(position)
            activity?.onSelectItemUpdate(selectItems.size, adapter?.data?.size ?: 0)
            if (!isDisksDir && !inSelectMode) activity?.inSelectMode = true
            true
        }
    }

    override fun onResume() {
        super.onResume()
        updateList()
    }

    override fun onDestroyView() {
        adapter = null
        super.onDestroyView()
    }

    fun stackResume() { //上层的Fragment移开显示当前Fragment时回调
        updateList()
    }

    fun stackPause() { //当前正在显示的Fragment被其他Fragment覆盖时回调
    }

    fun onSelectBtn(selectInterval: Boolean) {
        if (selectInterval) {
            val min = selectItems.min() ?: -1
            val max = selectItems.max() ?: -1
            for (i in min..max) {
                if (i >= 0) selectItems.add(i)
            }
        } else {
            if (selectItems.size == adapter?.data?.size ?: 0) {
                selectItems.clear()
            } else {
                for (i in 0 until (adapter?.data?.size ?: 0)) {
                    selectItems.add(i)
                }
            }
        }
        activity?.onSelectItemUpdate(selectItems.size, adapter?.data?.size ?: 0)
        adapter?.notifyDataSetChanged()
    }

    @SuppressLint("DefaultLocale")
    fun updateList() {
        val list = mutableListOf<File>()
        var index = 0
        if (FileManager.DISKS_DIR_TAG == dir) {
            val sdcards = FileUtil.getSdCards(context)
            sdcards?.forEach {
                val f = File(it)
                if (f.exists()) list.add(f)
            }
        } else if (dir != null) {
            val dirFile = File(dir)
            dirFile.listFiles()?.forEach {
                list.add(it)
            }
            list.sortWith(Comparator { p0, p1 ->
                val d0 = if (p0.isDirectory) 0 else 1
                val d1 = if (p1.isDirectory) 0 else 1
                if (d0 == d1) {
                    return@Comparator StringUtil.compare(p0.name.toLowerCase(), p1.name.toLowerCase(), "GBK")
                } else {
                    return@Comparator d0 - d1
                }
            })
            if (!toName.isNullOrEmpty()) list.forEachIndexed { i, file ->
                if (file.name == toName) {
                    index = i
                    return@forEachIndexed
                }
            }
        }
        adapter?.setNewData(list)
        fmEmptyList?.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        if (index > 0) fmFileList?.scrollToPosition(index)
        toName = null //第一次定位到指定文件后，再次刷新不再定位到指定文件
    }


    @Suppress("DEPRECATION")
    class Adapter(private var fragment: FileManagerFragment) : BaseQuickAdapter<File, BaseViewHolder>(R.layout.fm_item_file) {
        override fun convert(helper: BaseViewHolder, item: File?) {
            if (item != null) {
                when {
                    fragment.isDisksDir -> Glide.with(mContext).load(R.drawable.fm_format_sdcard).into(helper.getView(R.id.fm_file_icon))
                    item.isDirectory -> Glide.with(mContext).load(R.drawable.fm_format_folder).into(helper.getView(R.id.fm_file_icon))
                    ImageViewerHelper.isImage(item) -> Glide.with(mContext).load(item).placeholder(R.drawable.fm_format_picture).into(helper.getView(R.id.fm_file_icon))
                    VideoPlayerHelper.isVideoFile(item) -> Glide.with(mContext).load(item).placeholder(R.drawable.fm_format_media).into(helper.getView(R.id.fm_file_icon))
                    MusicPlayerHelper.isMusicFile(item, false) -> Glide.with(mContext).load(R.drawable.fm_format_music).into(helper.getView(R.id.fm_file_icon))
                    TextViewerHelper.isTextFile(item) -> Glide.with(mContext).load(item).placeholder(R.drawable.fm_format_text).into(helper.getView(R.id.fm_file_icon))
                    else -> Glide.with(mContext).load(R.drawable.fm_format_file).into(helper.getView(R.id.fm_file_icon))
                }
                helper.setText(R.id.fm_file_name, item.name)
                when {
                    fragment.isDisksDir -> {
                        val totalSpace = FileUtil.getSdCardStorage(item.absolutePath, false)
                        val availableSpace = FileUtil.getSdCardStorage(item.absolutePath, true)
                        helper.setText(R.id.fm_file_size, "(${MemoryUtil.formatMemorySize(totalSpace -
                                availableSpace, 2)}/${MemoryUtil.formatMemorySize(totalSpace, 2)})")
                        helper.setGone(R.id.fm_file_authority, false)
                        helper.setGone(R.id.fm_file_date, false)
                    }
                    item.isDirectory -> {
                        helper.setText(R.id.fm_file_size, "${item.listFiles()?.size ?: 0}项")
                        helper.setGone(R.id.fm_file_authority, true)
                        helper.setGone(R.id.fm_file_date, true)
                        helper.setText(R.id.fm_file_authority, "d${if (item.canRead()) "r" else ""}${if (item.canWrite()) "w" else ""}")
                        helper.setText(R.id.fm_file_date, DateUtil.getDateStr(item.lastModified(), null))
                    }
                    else -> {
                        helper.setText(R.id.fm_file_size, MemoryUtil.formatMemorySize(item.length(), 2))
                        helper.setGone(R.id.fm_file_authority, true)
                        helper.setGone(R.id.fm_file_date, true)
                        helper.setText(R.id.fm_file_authority, "${if (item.canRead()) "r" else ""}${if (item.canWrite()) "w" else ""}")
                        helper.setText(R.id.fm_file_date, DateUtil.getDateStr(item.lastModified(), null))
                    }
                }
                helper.setGone(R.id.fm_select_file, fragment.inSelectMode)
                if (fragment.inSelectMode) helper.getView<CheckBox>(R.id.fm_select_file).isChecked = fragment.selectItems.contains(helper.layoutPosition)
            }
        }
    }
}
