package com.huanchengfly.tieba.post.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.huanchengfly.tieba.post.models.database.History
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY timestamp DESC, count DESC LIMIT 100")
    suspend fun getAll(): List<History>

    @Query("SELECT * FROM history WHERE type = :type ORDER BY timestamp DESC, count DESC LIMIT :pageSize OFFSET :offset")
    suspend fun getByType(type: Int, pageSize: Int = 100, offset: Int = 0): List<History>

    @Query("SELECT * FROM history WHERE type = :type ORDER BY timestamp DESC, count DESC LIMIT :pageSize OFFSET :offset")
    fun getFlowByType(type: Int, pageSize: Int = 100, offset: Int = 0): Flow<List<History>>

    @Query("SELECT * FROM history WHERE data = :data LIMIT 1")
    suspend fun getByData(data: String): History?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: History): Long

    @Update
    suspend fun update(history: History)

    @Query("DELETE FROM history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM history")
    suspend fun deleteAll()

    @Transaction
    suspend fun upsert(history: History) {
        val existing = getByData(history.data)
        if (existing != null) {
            update(
                history.copy(
                    id = existing.id,
                    timestamp = System.currentTimeMillis(),
                    title = history.title,
                    extras = history.extras,
                    avatar = history.avatar,
                    username = history.username,
                    count = existing.count + 1,
                )
            )
        } else {
            insert(history.copy(count = 1, timestamp = System.currentTimeMillis()))
        }
    }
}
