package com.huanchengfly.tieba.post.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.huanchengfly.tieba.post.models.database.TopForum

@Dao
interface TopForumDao {
    @Query("SELECT * FROM topforum")
    suspend fun getAll(): List<TopForum>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(topForum: TopForum)

    @Query("DELETE FROM topforum WHERE forumId = :forumId")
    suspend fun deleteByForumId(forumId: String)
}
