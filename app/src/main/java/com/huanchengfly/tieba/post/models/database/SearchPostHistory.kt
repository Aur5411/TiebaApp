package com.huanchengfly.tieba.post.models.database

import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Immutable
@Entity(tableName = "searchposthistory")
data class SearchPostHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val content: String,
    @ColumnInfo(name = "forumname") val forumName: String,
    val timestamp: Long = System.currentTimeMillis(),
)
