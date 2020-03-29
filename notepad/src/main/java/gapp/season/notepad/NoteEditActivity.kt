package gapp.season.notepad

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import gapp.season.encryptlib.hash.HashUtil
import gapp.season.notepad.db.NoteDbHelper
import gapp.season.util.sys.ScreenUtil
import gapp.season.util.task.OnTaskDone
import gapp.season.util.text.StringUtil
import gapp.season.util.tips.ToastUtil
import gapp.season.util.view.ThemeUtil
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.note_edit_activity.*
import java.util.concurrent.Callable
import kotlin.math.max
import kotlin.math.min

class NoteEditActivity : AppCompatActivity() {
    companion object {
        fun navigation(context: Context, privacy: Boolean = false, noteId: String? = null) {
            val intent = Intent(context, NoteEditActivity::class.java)
            intent.putExtra("privacy", privacy)
            intent.putExtra("noteId", noteId)
            if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    private var compositeDisposable: CompositeDisposable? = null
    private var privacy = false
    private var noteId: String? = null
    private var noteItem: NotePadItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        compositeDisposable = CompositeDisposable()
        privacy = intent.getBooleanExtra("privacy", false)
        noteId = intent.getStringExtra("noteId")
        ThemeUtil.setTheme(this, 0)
        setContentView(R.layout.note_edit_activity)
        initView()
        initData()
    }

    override fun onDestroy() {
        compositeDisposable?.dispose()
        super.onDestroy()
    }

    override fun onBackPressed() {
        saveNote() //关闭时自动保存
        super.onBackPressed()
    }

    private fun initView() {
        noteEditBack.setOnClickListener { onBackPressed() }
        noteEditSave.setOnClickListener { saveNote() }
        noteEditMinus.setOnClickListener {
            val size = max(ScreenUtil.pxToDp(noteEditView.textSize) - 1, 12f)
            noteEditView.textSize = size
            NoteHelper.saveFontSize(this, size)
        }
        noteEditAdd.setOnClickListener {
            val size = min(ScreenUtil.pxToDp(noteEditView.textSize) + 1, 50f)
            noteEditView.textSize = size
            NoteHelper.saveFontSize(this, size)
        }
        noteEditView.textSize = NoteHelper.getFontSize(this)
    }

    private fun initData() {
        if (!noteId.isNullOrEmpty()) {
            compositeDisposable?.add(NoteDbHelper.execute(Callable { NoteDbHelper.getNoteDao().loadById(noteId!!) },
                    OnTaskDone { code, _, data ->
                        if (code == OnTaskDone.CODE_SUCCESS && data != null) {
                            noteItem = NotePadItem(data)
                            noteEditView.setText(noteItem!!.content)
                            //noteEditView.setSelection(noteEditView.text.length)
                        } else {
                            ToastUtil.showShort("获取便签内容失败")
                            finish()
                        }
                    }))
        }
    }

    private fun saveNote() {
        if (noteId.isNullOrEmpty()) {
            noteId = generateNoteId()
        }
        val time = System.currentTimeMillis()
        if (noteItem == null) {
            noteItem = NotePadItem()
            noteItem?.noteId = noteId
            noteItem?.privacy = privacy
            noteItem?.createTime = time
        }
        noteItem?.title = generateNoteTitle(noteEditView.text.toString())
        noteItem?.content = noteEditView.text.toString()
        noteItem?.modifyTime = time
        val entity = noteItem?.toNoteEntity()
        if (entity != null) {
            if (entity.content.isNullOrEmpty()) {
                compositeDisposable?.add(NoteDbHelper.execute(Callable { NoteDbHelper.getNoteDao().delete(entity) }, null))
            } else {
                compositeDisposable?.add(NoteDbHelper.execute(Callable { NoteDbHelper.getNoteDao().insert(entity) }, null))
            }
        }
    }

    private fun generateNoteId(): String {
        return HashUtil.md5("${System.currentTimeMillis()}-${Math.random()}")
    }

    private fun generateNoteTitle(content: String?): String? {
        if (!content.isNullOrEmpty()) {
            val rc = StringUtil.removeNeedlessBlank(content)
            rc?.split("\n")?.forEach {
                if (!it.isBlank()) {
                    return it
                }
            }
        }
        return null
    }
}
