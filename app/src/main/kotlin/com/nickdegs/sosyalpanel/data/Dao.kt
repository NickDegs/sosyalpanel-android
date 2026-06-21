package com.nickdegs.sosyalpanel.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Transaction
    @Query("SELECT * FROM accounts ORDER BY sortOrder, addedAt")
    fun observeAccountsWithSnapshots(): Flow<List<AccountRelation>>

    @Query("SELECT * FROM accounts ORDER BY sortOrder, addedAt")
    suspend fun allAccounts(): List<TrackedAccount>

    @Insert
    suspend fun insertAccount(account: TrackedAccount): Long

    @Insert
    suspend fun insertSnapshot(snapshot: MetricSnapshot): Long

    @Delete
    suspend fun deleteAccount(account: TrackedAccount)

    @Query("DELETE FROM accounts")
    suspend fun deleteAllAccounts()

    @Query("UPDATE accounts SET goalFollowers = :goal WHERE id = :id")
    suspend fun setGoal(id: Long, goal: Int?)

    @Query("SELECT COUNT(*) FROM accounts")
    suspend fun accountCount(): Int
}

// Room ilişki sınıfı → AccountWithSnapshots'a map'lenir.
data class AccountRelation(
    @androidx.room.Embedded val account: TrackedAccount,
    @androidx.room.Relation(parentColumn = "id", entityColumn = "accountId")
    val snapshots: List<MetricSnapshot>
) {
    fun toModel() = AccountWithSnapshots(account, snapshots)
}

@Database(entities = [TrackedAccount::class, MetricSnapshot::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
}
