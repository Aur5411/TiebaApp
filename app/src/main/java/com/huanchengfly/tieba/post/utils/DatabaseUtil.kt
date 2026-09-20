package com.huanchengfly.tieba.post.utils

import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.database.AppDatabase
import com.huanchengfly.tieba.post.database.AppDatabaseEntryPoint
import com.huanchengfly.tieba.post.models.database.Account
import com.huanchengfly.tieba.post.models.database.Block
import com.huanchengfly.tieba.post.models.database.Draft
import com.huanchengfly.tieba.post.models.database.History
import com.huanchengfly.tieba.post.models.database.SearchHistory
import com.huanchengfly.tieba.post.models.database.SearchPostHistory
import com.huanchengfly.tieba.post.models.database.TopForum
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.Flow

object DatabaseUtil {
    private val appDatabase: AppDatabase by lazy {
        EntryPointAccessors.fromApplication(
            App.INSTANCE,
            AppDatabaseEntryPoint::class.java,
        ).appDatabase()
    }

    // ── Account ─────────────────────────────────────────────────
    suspend fun getAllAccounts(): List<Account> = appDatabase.accountDao().getAll()

    suspend fun getAccountById(id: Int): Account? = appDatabase.accountDao().getById(id)

    suspend fun getAccountByUid(uid: String): Account? = appDatabase.accountDao().getByUid(uid)

    suspend fun getAccountByBduss(bduss: String): Account? = appDatabase.accountDao().getByBduss(bduss)

    suspend fun upsertAccountByUid(account: Account) = appDatabase.accountDao().upsertByUid(account)

    suspend fun updateAccount(account: Account) = appDatabase.accountDao().update(account)

    suspend fun deleteAccount(account: Account) = appDatabase.accountDao().delete(account)

    // ── History ─────────────────────────────────────────────────
    suspend fun getAllHistory(): List<History> = appDatabase.historyDao().getAll()

    suspend fun getHistoryByType(type: Int, pageSize: Int = 100, offset: Int = 0): List<History> =
        appDatabase.historyDao().getByType(type, pageSize, offset)

    fun getHistoryFlowByType(type: Int, pageSize: Int = 100, offset: Int = 0): Flow<List<History>> =
        appDatabase.historyDao().getFlowByType(type, pageSize, offset)

    suspend fun upsertHistory(history: History) = appDatabase.historyDao().upsert(history)

    suspend fun deleteHistoryById(id: Long) = appDatabase.historyDao().deleteById(id)

    suspend fun deleteAllHistory() = appDatabase.historyDao().deleteAll()

    // ── Block ───────────────────────────────────────────────────
    suspend fun getAllBlocks(): List<Block> = appDatabase.blockDao().getAll()

    fun getAllBlocksFlow(): Flow<List<Block>> = appDatabase.blockDao().getAllFlow()

    suspend fun insertBlock(block: Block): Long = appDatabase.blockDao().insert(block)

    suspend fun deleteBlockById(id: Long) = appDatabase.blockDao().deleteById(id)

    // ── Draft ───────────────────────────────────────────────────
    suspend fun getDraft(hash: String): Draft? = appDatabase.draftDao().getByHash(hash)

    suspend fun saveDraft(hash: String, content: String) {
        appDatabase.draftDao().upsert(Draft(hash = hash, content = content))
    }

    suspend fun deleteDraft(hash: String) = appDatabase.draftDao().deleteByHash(hash)

    // ── TopForum ────────────────────────────────────────────────
    suspend fun getTopForums(): List<TopForum> = appDatabase.topForumDao().getAll()

    suspend fun addTopForum(forumId: String) {
        appDatabase.topForumDao().insertOrReplace(TopForum(forumId = forumId))
    }

    suspend fun deleteTopForum(forumId: String) = appDatabase.topForumDao().deleteByForumId(forumId)

    // ── SearchHistory ───────────────────────────────────────────
    suspend fun getAllSearchHistories(): List<SearchHistory> = appDatabase.searchHistoryDao().getAll()

    suspend fun saveSearchHistory(content: String) {
        appDatabase.searchHistoryDao().upsert(SearchHistory(content = content))
    }

    suspend fun deleteSearchHistory(id: Long) = appDatabase.searchHistoryDao().deleteById(id)

    suspend fun clearSearchHistory() = appDatabase.searchHistoryDao().deleteAll()

    // ── SearchPostHistory ───────────────────────────────────────
    suspend fun getAllSearchPostHistories(): List<SearchPostHistory> = appDatabase.searchPostHistoryDao().getAll()

    suspend fun saveSearchPostHistory(content: String, forumName: String) {
        appDatabase.searchPostHistoryDao().upsert(
            SearchPostHistory(content = content, forumName = forumName)
        )
    }

    suspend fun deleteSearchPostHistory(id: Long) = appDatabase.searchPostHistoryDao().deleteById(id)

    suspend fun clearSearchPostHistory() = appDatabase.searchPostHistoryDao().deleteAll()
}
