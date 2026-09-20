package com.huanchengfly.tieba.post.models.database

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey

@Immutable
@Entity(tableName = "history")
data class History(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String = "",
    val data: String = "",
    val type: Int = 0,
    val timestamp: Long = 0,
    val count: Int = 0,
    val extras: String? = null,
    val avatar: String? = null,
    val username: String? = null,
)
