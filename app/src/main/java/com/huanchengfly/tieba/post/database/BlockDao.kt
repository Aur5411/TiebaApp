package com.huanchengfly.tieba.post.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import com.huanchengfly.tieba.post.models.database.Block
import kotlinx.coroutines.flow.Flow
import androidx.room.Query

@Dao
interface BlockDao {
    @Query("SELECT * FROM block")
    fun getAllFlow(): Flow<List<Block>>

    @Query("SELECT * FROM block")
    suspend fun getAll(): List<Block>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(block: Block): Long

    @Query("DELETE FROM block WHERE id = :id")
    suspend fun deleteById(id: Long)
}
