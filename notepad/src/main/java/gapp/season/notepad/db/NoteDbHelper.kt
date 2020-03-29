package gapp.season.notepad.db

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import gapp.season.util.task.OnTaskDone
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.Callable

//数据库表或字段更新时，需要增加version
@androidx.room.Database(entities = [NoteEntity::class], version = 1)
abstract class NoteDatabase : RoomDatabase() {
    abstract fun getNoteDao(): NoteDao
}

/**
 * 数据库操作必须在子线程异步处理
 */
object NoteDbHelper {
    private const val DB_NAME = "room-gapp-note.db"
    private var database: NoteDatabase? = null

    fun init(context: Context) {
        database = Room.databaseBuilder(context, NoteDatabase::class.java, DB_NAME).fallbackToDestructiveMigration().build()
    }

    fun getNoteDao(): NoteDao {
        return database!!.getNoteDao()
    }

    fun <T> execute(callable: Callable<T>, listener: OnTaskDone<T>?): Disposable {
        return Observable.fromCallable(callable)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    listener?.onTaskDone(OnTaskDone.CODE_SUCCESS, null, it)
                }, {
                    listener?.onTaskDone(OnTaskDone.CODE_FAIL, null, null)
                })
    }
}
