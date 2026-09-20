package com.huanchengfly.tieba.post.models.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "topforum")
data class TopForum(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "forumid") val forumId: String,
)
