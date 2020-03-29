package gapp.season.notepad.db

import androidx.annotation.NonNull
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.Gson
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

@Entity(tableName = "NOTE_ENTITY")
class NoteEntity {
    @NonNull
    @PrimaryKey
    @ColumnInfo(name = "NOTE_ID")
    @Expose
    @SerializedName("noteId")
    var noteId: String? = null

    @ColumnInfo(name = "CREATE_TIME")
    @Expose
    @SerializedName("createTime")
    var createTime: Long = 0 //ms

    @ColumnInfo(name = "TITLE")
    @Expose
    @SerializedName("title")
    var title: String? = null

    @ColumnInfo(name = "CONTENT")
    @Expose
    @SerializedName("content")
    var content: String? = null

    @ColumnInfo(name = "MODIFY_TIME")
    @Expose
    @SerializedName("modifyTime")
    var modifyTime: Long = 0 //ms

    @ColumnInfo(name = "PRIVACY")
    @Expose
    @SerializedName("privacy")
    var privacy: Int = 0 //1表示私密便签

    //@Entity类中不能存在空构造函数(否则报错：NoSuchElementException: Collection contains no element matching the predicate.)
    constructor(noteId: String, createTime: Long, title: String?, content: String?, modifyTime: Long, privacy: Int) {
        this.noteId = noteId
        this.createTime = createTime
        this.title = title
        this.content = content
        this.modifyTime = modifyTime
        this.privacy = privacy
    }

    fun toJsonString(): String {
        return Gson().toJson(this) ?: ""
    }

    companion object {
        fun fromJsonString(json: String): NoteEntity? {
            return Gson().fromJson(json, NoteEntity::class.java)
        }
    }
}
