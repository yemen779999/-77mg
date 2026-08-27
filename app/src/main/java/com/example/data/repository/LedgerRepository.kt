package com.example.data.repository

import com.example.data.local.AccountDao
import com.example.data.local.NotificationLogDao
import com.example.data.local.TransactionDao
import com.example.data.local.TrashDao
import com.example.data.model.Account
import com.example.data.model.DeletedAccount
import com.example.data.model.DeletedTransaction
import com.example.data.model.NotificationLog
import com.example.data.model.Transaction
import kotlinx.coroutines.flow.Flow

class LedgerRepository(
    private val accountDao: AccountDao,
    private val transactionDao: TransactionDao,
    private val trashDao: TrashDao,
    private val notificationLogDao: NotificationLogDao? = null
) {
    val allAccounts: Flow<List<Account>> = accountDao.getAllAccounts()
    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()
    val allDeletedAccounts: Flow<List<DeletedAccount>> = trashDao.getAllDeletedAccounts()
    val allDeletedTransactions: Flow<List<DeletedTransaction>> = trashDao.getAllDeletedTransactions()
    val allNotificationLogs: Flow<List<NotificationLog>> = notificationLogDao?.getAllLogs() ?: kotlinx.coroutines.flow.flowOf(emptyList())

    suspend fun insertNotificationLog(log: NotificationLog): Long =
        notificationLogDao?.insertLog(log) ?: 0L

    suspend fun deleteNotificationLog(log: NotificationLog) {
        notificationLogDao?.deleteLog(log)
    }

    suspend fun deleteNotificationLogById(id: Int) {
        notificationLogDao?.deleteLogById(id)
    }

    suspend fun clearNotificationLogs() {
        notificationLogDao?.clearAllLogs()
    }

    fun getAccountById(id: Int): Flow<Account?> = accountDao.getAccountById(id)

    fun getTransactionsForAccount(accountId: Int): Flow<List<Transaction>> = 
        transactionDao.getTransactionsForAccount(accountId)

    fun getTransactionsForAccountAsc(accountId: Int): Flow<List<Transaction>> = 
        transactionDao.getTransactionsForAccountAsc(accountId)

    suspend fun insertAccount(account: Account): Long = accountDao.insertAccount(account)

    suspend fun updateAccount(account: Account) = accountDao.updateAccount(account)

    suspend fun deleteAccount(account: Account) {
        moveToTrash(account)
    }

    // Soft delete Account by moving it to Trash and cascading manually
    suspend fun moveToTrash(account: Account) {
        val deletedAccount = DeletedAccount(
            id = account.id,
            name = account.name,
            phone = account.phone,
            type = account.type,
            createdAt = account.createdAt,
            creditLimit = account.creditLimit,
            tag = account.tag,
            deletedAt = System.currentTimeMillis(),
            initialBalance = account.initialBalance
        )
        trashDao.insertDeletedAccount(deletedAccount)

        val accountTxs = transactionDao.getTransactionsForAccountSync(account.id)
        accountTxs.forEach { tx ->
            val deletedTx = DeletedTransaction(
                id = tx.id,
                accountId = tx.accountId,
                day = tx.day,
                date = tx.date,
                details = tx.details,
                quantity = tx.quantity,
                unitPrice = tx.unitPrice,
                addition = tx.addition,
                total = tx.total,
                isPayment = tx.isPayment,
                timestamp = tx.timestamp,
                currency = tx.currency,
                exchangeRate = tx.exchangeRate,
                dueDate = tx.dueDate,
                deletedAt = System.currentTimeMillis()
            )
            trashDao.insertDeletedTransaction(deletedTx)
        }

        accountDao.deleteAccount(account)
    }

    // Soft delete Transaction
    suspend fun moveToTrash(transaction: Transaction) {
        val deletedTx = DeletedTransaction(
            id = transaction.id,
            accountId = transaction.accountId,
            day = transaction.day,
            date = transaction.date,
            details = transaction.details,
            quantity = transaction.quantity,
            unitPrice = transaction.unitPrice,
            addition = transaction.addition,
            total = transaction.total,
            isPayment = transaction.isPayment,
            timestamp = transaction.timestamp,
            currency = transaction.currency,
            exchangeRate = transaction.exchangeRate,
            dueDate = transaction.dueDate,
            deletedAt = System.currentTimeMillis()
        )
        trashDao.insertDeletedTransaction(deletedTx)
        transactionDao.deleteTransaction(transaction)
    }

    // Restore Deleted Account & its transactions
    suspend fun restoreAccount(deletedAccount: DeletedAccount) {
        val account = Account(
            id = deletedAccount.id,
            name = deletedAccount.name,
            phone = deletedAccount.phone,
            type = deletedAccount.type,
            createdAt = deletedAccount.createdAt,
            creditLimit = deletedAccount.creditLimit,
            tag = deletedAccount.tag,
            initialBalance = deletedAccount.initialBalance
        )
        accountDao.insertAccount(account)

        val deletedTxs = trashDao.getDeletedTransactionsForAccount(deletedAccount.id)
        deletedTxs.forEach { delTx ->
            val tx = Transaction(
                id = delTx.id,
                accountId = delTx.accountId,
                day = delTx.day,
                date = delTx.date,
                details = delTx.details,
                quantity = delTx.quantity,
                unitPrice = delTx.unitPrice,
                addition = delTx.addition,
                total = delTx.total,
                isPayment = delTx.isPayment,
                timestamp = delTx.timestamp,
                currency = delTx.currency,
                exchangeRate = delTx.exchangeRate,
                dueDate = delTx.dueDate
            )
            transactionDao.insertTransaction(tx)
            trashDao.removeDeletedTransactionFromTrash(delTx.id)
        }

        trashDao.removeDeletedAccountFromTrash(deletedAccount.id)
    }

    // Restore Deleted Transaction
    suspend fun restoreTransaction(deletedTransaction: DeletedTransaction) {
        val tx = Transaction(
            id = deletedTransaction.id,
            accountId = deletedTransaction.accountId,
            day = deletedTransaction.day,
            date = deletedTransaction.date,
            details = deletedTransaction.details,
            quantity = deletedTransaction.quantity,
            unitPrice = deletedTransaction.unitPrice,
            addition = deletedTransaction.addition,
            total = deletedTransaction.total,
            isPayment = deletedTransaction.isPayment,
            timestamp = deletedTransaction.timestamp,
            currency = deletedTransaction.currency,
            exchangeRate = deletedTransaction.exchangeRate,
            dueDate = deletedTransaction.dueDate
        )
        transactionDao.insertTransaction(tx)
        trashDao.removeDeletedTransactionFromTrash(deletedTransaction.id)
    }

    suspend fun removeDeletedAccountPermanently(id: Int) {
        trashDao.removeDeletedAccountFromTrash(id)
        trashDao.removeDeletedTransactionsForAccountFromTrash(id)
    }

    suspend fun removeDeletedTransactionPermanently(id: Int) {
        trashDao.removeDeletedTransactionFromTrash(id)
    }

    suspend fun pruneTrash(olderThanTimestamp: Long) {
        trashDao.pruneOldAccounts(olderThanTimestamp)
        trashDao.pruneOldTransactions(olderThanTimestamp)
    }

    suspend fun clearTrash() {
        trashDao.clearAccountsTrash()
        trashDao.clearTransactionsTrash()
    }

    suspend fun insertTransaction(transaction: Transaction): Long = 
        transactionDao.insertTransaction(transaction)

    suspend fun updateTransaction(transaction: Transaction) = 
        transactionDao.updateTransaction(transaction)

    suspend fun deleteTransaction(transaction: Transaction) = 
        moveToTrash(transaction)

    suspend fun deleteTransactionsForAccount(accountId: Int) = 
        transactionDao.deleteTransactionsForAccount(accountId)

    suspend fun deleteTransactionById(id: Int) = 
        transactionDao.deleteTransactionById(id)
}
