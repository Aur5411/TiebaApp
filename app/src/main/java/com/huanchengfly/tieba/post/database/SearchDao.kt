package com.huanchengfly.tieba.post.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.huanchengfly.tieba.post.models.database.SearchHistory
import com.huanchengfly.tieba.post.models.database.SearchPostHistory

@Dao
interface SearchHistoryDao {
    @Query("SELECT * FROM searchhistory ORDER BY timestamp DESC")
    suspend fun getAll(): List<SearchHistory>

    @Query("DELETE FROM searchhistory")
    suspend fun deleteAll()

    @Query("DELETE FROM searchhistory WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM searchhistory WHERE content = :content LIMIT 1")
    suspend fun getByContent(content: String): SearchHistory?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: SearchHistory): Long

    @Transaction
    suspend fun upsert(history: SearchHistory) {
        val existing = getByContent(history.content)
        if (existing != null) {
            insert(history.copy(id = existing.id, timestamp = System.currentTimeMillis()))
        } else {
            insert(history.copy(timestamp = System.currentTimeMillis()))
        }
    }
}

@Dao
interface SearchPostHistoryDao {
    @Query("SELECT * FROM searchposthistory ORDER BY timestamp DESC")
    suspend fun getAll(): List<SearchPostHistory>

    @Query("DELETE FROM searchposthistory")
    suspend fun deleteAll()

    @Query("DELETE FROM searchposthistory WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM searchposthistory WHERE content = :content LIMIT 1")
    suspend fun getByContent(content: String): SearchPostHistory?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: SearchPostHistory): Long

    @Transaction
    suspend fun upsert(history: SearchPostHistory) {
        val existing = getByContent(history.content)
        if (existing != null) {
            insert(history.copy(id = existing.id, timestamp = System.currentTimeMillis()))
        } else {
            insert(history.copy(timestamp = System.currentTimeMillis()))
        }
    }
}
