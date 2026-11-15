package com.yuzlyn.mdeditor.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDraftDao {

    @Query("SELECT * FROM ${NoteDraftEntity.TABLE_NAME} ORDER BY updatedAt DESC")
    fun observeDrafts(): Flow<List<NoteDraftEntity>>

    @Query("SELECT * FROM ${NoteDraftEntity.TABLE_NAME}")
    suspend fun getAll(): List<NoteDraftEntity>

    @Query("SELECT * FROM ${NoteDraftEntity.TABLE_NAME} WHERE noteId = :noteId")
    suspend fun get(noteId: String): NoteDraftEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(draft: NoteDraftEntity)

    @Query("DELETE FROM ${NoteDraftEntity.TABLE_NAME} WHERE noteId = :noteId")
    suspend fun delete(noteId: String)

    @Query("DELETE FROM ${NoteDraftEntity.TABLE_NAME} WHERE updatedAt < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)

    @Query("DELETE FROM ${NoteDraftEntity.TABLE_NAME}")
    suspend fun deleteAll()
}
