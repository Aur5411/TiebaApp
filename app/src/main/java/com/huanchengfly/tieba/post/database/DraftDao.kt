package com.huanchengfly.tieba.post.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.huanchengfly.tieba.post.models.database.Draft

@Dao
interface DraftDao {
    @Query("SELECT * FROM draft WHERE hash = :hash LIMIT 1")
    suspend fun getByHash(hash: String): Draft?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(draft: Draft)

    @Query("DELETE FROM draft WHERE hash = :hash")
    suspend fun deleteByHash(hash: String)
}
