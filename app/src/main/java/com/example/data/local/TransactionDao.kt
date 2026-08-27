package com.example.data.local

import androidx.room.*
import com.example.data.model.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE accountId = :accountId ORDER BY timestamp DESC")
    fun getTransactionsForAccount(accountId: Int): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE accountId = :accountId")
    suspend fun getTransactionsForAccountSync(accountId: Int): List<Transaction>

    @Query("SELECT * FROM transactions WHERE accountId = :accountId ORDER BY date ASC, timestamp ASC")
    fun getTransactionsForAccountAsc(accountId: Int): Flow<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction): Long

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    @Query("DELETE FROM transactions WHERE accountId = :accountId")
    suspend fun deleteTransactionsForAccount(accountId: Int)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Int)
}
