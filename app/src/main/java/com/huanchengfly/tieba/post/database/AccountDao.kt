package com.huanchengfly.tieba.post.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.huanchengfly.tieba.post.models.database.Account
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM account ORDER BY id ASC")
    suspend fun getAll(): List<Account>

    @Query("SELECT * FROM account WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): Account?

    @Query("SELECT * FROM account WHERE uid = :uid LIMIT 1")
    suspend fun getByUid(uid: String): Account?

    @Query("SELECT * FROM account WHERE bduss = :bduss LIMIT 1")
    suspend fun getByBduss(bduss: String): Account?

    @Insert
    suspend fun insert(account: Account): Long

    @Update
    suspend fun update(account: Account)

    @Delete
    suspend fun delete(account: Account)

    @Query("DELETE FROM account WHERE uid = :uid")
    suspend fun deleteByUid(uid: String)

    @Query("SELECT * FROM account")
    fun getAllFlow(): Flow<List<Account>>

    @Transaction
    suspend fun upsertByUid(account: Account): Long {
        val existing = getByUid(account.uid)
        return if (existing != null) {
            update(account.copy(id = existing.id))
            existing.id.toLong()
        } else {
            insert(account.copy(id = 0))
        }
    }
}
