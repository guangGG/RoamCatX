package gapp.season.notepad

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.lxj.xpopup.XPopup
import gapp.season.notepad.db.NoteDbHelper
import gapp.season.notepad.db.NoteEntity
import gapp.season.util.file.FileUtil
import gapp.season.util.task.OnTaskDone
import gapp.season.util.tips.AlertUtil
import gapp.season.util.tips.ToastUtil
import gapp.season.util.view.ThemeUtil
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.note_activity.*
import java.util.concurrent.Callable

class NotePadActivity : AppCompatActivity() {
    private var compositeDisposable: CompositeDisposable? = null
    private var privacy = false
    private var adapter: NotePadAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        compositeDisposable = CompositeDisposable()
        privacy = intent.getBooleanExtra("privacy", false)
        ThemeUtil.setTheme(this, 0)
        setContentView(R.layout.note_activity)
        initView()
        initData()
    }

    override fun onRestart() {
        super.onRestart()
        updateNotes()
    }

    override fun onDestroy() {
        compositeDisposable?.dispose()
        super.onDestroy()
    }

    private fun initView() {
        noteBack.setOnClickListener { onBackPressed() }
        if (privacy) noteTitle.text = "私密便签"
        noteMenu.setOnClickListener { showMenu(noteMenu) }
        noteAddBtn.setOnClickListener { NoteEditActivity.navigation(this, privacy) }
        val layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        noteList.layoutManager = layoutManager
        adapter = NotePadAdapter()
        adapter?.bindToRecyclerView(noteList)
        adapter?.setOnItemClickListener { adapter, _, position ->
            val item = adapter.getItem(position)
            if (item is NotePadItem) NoteEditActivity.navigation(this, privacy, item.noteId)
        }
        adapter?.setOnItemLongClickListener { adapter, _, position ->
            val item = adapter.getItem(position)
            if (item is NotePadItem) {
                showNoteDetail(item)
            }
            true
        }
        adapter?.setEmptyView(R.layout.note_empty_view, noteList)
    }

    private fun initData() {
        updateNotes()
    }

    private fun updateNotes() {
        compositeDisposable?.add(NoteDbHelper.execute(Callable { NoteDbHelper.getNoteDao().loadAll() },
                OnTaskDone { code, _, data ->
                    if (code == OnTaskDone.CODE_SUCCESS) {
                        val list = mutableListOf<NotePadItem>()
                        data.forEach {
                            if ((it.privacy == 1) == privacy) {
                                list.add(NotePadItem(it))
                            }
                        }
                        list.sortWith(Comparator { o1, o2 ->
                            ((o2?.modifyTime ?: 0) - (o1?.modifyTime ?: 0)).toInt()
                        })
                        adapter?.setNewData(list)
                    } else {
                        ToastUtil.showShort("获取便签内容失败")
                        finish()
                    }
                }))
    }

    private fun showMenu(view: View) {
        val actions = arrayOf("备份全部便签", "还原全部便签")
        XPopup.Builder(this).atView(view).hasShadowBg(false)
                .asAttachList(actions, null) { position, _ ->
                    when (position) {
                        0 -> { //备份
                            compositeDisposable?.add(NoteDbHelper.execute(Callable { NoteDbHelper.getNoteDao().loadAll() },
                                    OnTaskDone { code, _, data ->
                                        if (code == OnTaskDone.CODE_SUCCESS) {
                                            val list = mutableListOf<String>()
                                            data?.forEach {
                                                list.add(it.toJsonString())
                                            }
                                            val saveContent = Gson().toJson(list)
                                            FileUtil.saveToFile(NoteHelper.backUpFile(), saveContent, null)
                                            ToastUtil.showShort("备份便签成功")
                                        } else {
                                            ToastUtil.showShort("备份便签失败")
                                        }
                                    }))
                        }
                        1 -> { //还原
                            val saveContent = FileUtil.getFileContent(NoteHelper.backUpFile(), null)
                            if (!saveContent.isNullOrBlank()) {
                                val list = Gson().fromJson<List<String>>(saveContent, object : TypeToken<List<String>>() {}.type)
                                if (!list.isNullOrEmpty()) {
                                    AlertUtil.showMsg(this, "温馨提示",
                                            "共发现${list.size}条备份(包括普通便签和私密便签)，如果您当前列表中的便签有备份版本，那么将会被备份版本覆盖，您确定要还原全部便签吗？",
                                            "确定", null, "取消", true) { code, _, _ ->
                                        if (code == AlertUtil.POSITIVE_BUTTON) {
                                            val notes = mutableListOf<NoteEntity>()
                                            list.forEach {
                                                val noteEntity = NoteEntity.fromJsonString(it)
                                                if (noteEntity != null) notes.add(noteEntity)
                                            }
                                            compositeDisposable?.add(NoteDbHelper.execute(Callable { NoteDbHelper.getNoteDao().insertAll(notes) },
                                                    OnTaskDone { ret, _, _ ->
                                                        if (ret == OnTaskDone.CODE_SUCCESS) {
                                                            ToastUtil.showShort("还原便签成功")
                                                        } else {
                                                            ToastUtil.showShort("还原便签失败")
                                                        }
                                                        updateNotes()
                                                    }))
                                        }
                                    }
                                    return@asAttachList
                                }
                            }
                            ToastUtil.showShort("未找到备份的便签")
                        }
                    }
                }.show()
    }

    private fun showNoteDetail(item: NotePadItem) {
        var msg = "便签ID: ${item.noteId}"
        msg += "\n创建时间: ${NoteHelper.formatTime(item.createTime)}"
        msg += "\n修改时间: ${NoteHelper.formatTime(item.modifyTime)}"
        if (item.title != null) msg += "\n便签标题: ${item.title}"
        if (item.content != null) msg += "\n便签内容: ${item.content}"
        AlertUtil.showMsg(this@NotePadActivity, item.title, msg, "关闭",
                null, "删除", true) { code, _, _ ->
            when (code) {
                AlertUtil.NEGATIVE_BUTTON -> deleteNote(item)
            }
        }
    }

    private fun deleteNote(item: NotePadItem) {
        val entity = item.toNoteEntity()
        if (entity != null) {
            compositeDisposable?.add(NoteDbHelper.execute(Callable { NoteDbHelper.getNoteDao().delete(entity) },
                    OnTaskDone { _, _, _ -> updateNotes() }))
        }
    }


    class NotePadAdapter : BaseQuickAdapter<NotePadItem, BaseViewHolder>(R.layout.note_item) {
        override fun convert(helper: BaseViewHolder, item: NotePadItem?) {
            if (item != null) {
                helper.setText(R.id.note_item_title, item.title)
                helper.setText(R.id.note_item_content, item.content)
                helper.setText(R.id.note_item_date, NoteHelper.formatTime(item.modifyTime))
            }
        }
    }
}
