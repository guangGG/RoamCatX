package gapp.season.notepad

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import gapp.season.notepad.db.NoteEntity

class NotePadItem {
    @Expose
    @SerializedName("noteId")
    var noteId: String? = null
    @Expose
    @SerializedName("title")
    var title: String? = null
    @Expose
    @SerializedName("content")
    var content: String? = null
    @Expose
    @SerializedName("modifyTime")
    var modifyTime: Long = 0
    @Expose
    @SerializedName("privacy")
    var privacy = false
    @Expose
    @SerializedName("createTime")
    var createTime: Long = 0

    constructor()

    //NoteEntity为数据库中的源数据，NotePadItem为数据解密后用于显示的数据
    constructor(noteEntity: NoteEntity?) {
        this.noteId = noteEntity?.noteId
        this.privacy = (noteEntity?.privacy == 1)
        this.title = NoteHelper.decode(noteEntity?.title, this.privacy)
        this.content = NoteHelper.decode(noteEntity?.content, this.privacy)
        this.modifyTime = noteEntity?.modifyTime ?: 0
        this.createTime = noteEntity?.createTime ?: 0
    }

    fun toNoteEntity(): NoteEntity? {
        return if (noteId.isNullOrEmpty()) {
            null
        } else {
            val et = if (privacy) NoteHelper.encode(title) else title
            val ec = if (privacy) NoteHelper.encode(content) else content
            val rp = if (privacy) 1 else 0
            NoteEntity(noteId!!, createTime, et, ec, modifyTime, rp)
        }
    }
}
