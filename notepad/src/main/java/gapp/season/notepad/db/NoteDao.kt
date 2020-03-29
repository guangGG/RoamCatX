package gapp.season.notepad.db

import androidx.room.*

@Dao
interface NoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entity: NoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(entities: List<NoteEntity>)

    @Delete
    fun delete(assetEntity: NoteEntity)

    @Query("SELECT * FROM NOTE_ENTITY")
    fun loadAll(): List<NoteEntity>

    @Query("SELECT * FROM NOTE_ENTITY WHERE NOTE_ID = :id")
    fun loadById(id: String): NoteEntity
}
