package com.example.data.local

import androidx.room.*
import com.example.data.model.DeletedAccount
import com.example.data.model.DeletedTransaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TrashDao {
    @Query("SELECT * FROM deleted_accounts ORDER BY deletedAt DESC")
    fun getAllDeletedAccounts(): Flow<List<DeletedAccount>>

    @Query("SELECT * FROM deleted_transactions ORDER BY deletedAt DESC")
    fun getAllDeletedTransactions(): Flow<List<DeletedTransaction>>

    @Query("SELECT * FROM deleted_transactions WHERE accountId = :accountId ORDER BY deletedAt DESC")
    suspend fun getDeletedTransactionsForAccount(accountId: Int): List<DeletedTransaction>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeletedAccount(deletedAccount: DeletedAccount)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeletedTransaction(deletedTransaction: DeletedTransaction)

    @Query("DELETE FROM deleted_accounts WHERE id = :id")
    suspend fun removeDeletedAccountFromTrash(id: Int)

    @Query("DELETE FROM deleted_transactions WHERE id = :id")
    suspend fun removeDeletedTransactionFromTrash(id: Int)

    @Query("DELETE FROM deleted_transactions WHERE accountId = :accountId")
    suspend fun removeDeletedTransactionsForAccountFromTrash(accountId: Int)

    @Query("DELETE FROM deleted_accounts WHERE deletedAt < :timestamp")
    suspend fun pruneOldAccounts(timestamp: Long)

    @Query("DELETE FROM deleted_transactions WHERE deletedAt < :timestamp")
    suspend fun pruneOldTransactions(timestamp: Long)

    @Query("DELETE FROM deleted_accounts")
    suspend fun clearAccountsTrash()

    @Query("DELETE FROM deleted_transactions")
    suspend fun clearTransactionsTrash()
}
