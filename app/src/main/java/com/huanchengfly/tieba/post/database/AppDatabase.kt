package com.huanchengfly.tieba.post.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.huanchengfly.tieba.post.models.database.Account
import com.huanchengfly.tieba.post.models.database.Block
import com.huanchengfly.tieba.post.models.database.Draft
import com.huanchengfly.tieba.post.models.database.History
import com.huanchengfly.tieba.post.models.database.SearchHistory
import com.huanchengfly.tieba.post.models.database.SearchPostHistory
import com.huanchengfly.tieba.post.models.database.TopForum
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Database(
    entities = [
        Account::class,
        Draft::class,
        History::class,
        Block::class,
        TopForum::class,
        SearchHistory::class,
        SearchPostHistory::class,
    ],
    version = 40,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun draftDao(): DraftDao
    abstract fun historyDao(): HistoryDao
    abstract fun blockDao(): BlockDao
    abstract fun topForumDao(): TopForumDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun searchPostHistoryDao(): SearchPostHistoryDao
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AppDatabaseEntryPoint {
    fun appDatabase(): AppDatabase
}
