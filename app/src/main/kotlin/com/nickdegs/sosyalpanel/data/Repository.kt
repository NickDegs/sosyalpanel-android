package com.nickdegs.sosyalpanel.data

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Veri erişim cephesi — DB singleton + iş kuralları (örn. ücretsiz hesap limiti).
class Repository private constructor(context: Context) {

    private val db = Room.databaseBuilder(
        context.applicationContext, AppDatabase::class.java, "sosyalpanel.db"
    ).fallbackToDestructiveMigration().build()

    private val dao = db.accountDao()
    private val planner = db.plannerDao()

    val accounts: Flow<List<AccountWithSnapshots>> =
        dao.observeAccountsWithSnapshots().map { list -> list.map { it.toModel() } }

    val scheduledPosts: Flow<List<ScheduledPost>> = planner.observe()

    suspend fun addScheduledPost(post: ScheduledPost): Long = planner.insert(post)
    suspend fun deleteScheduledPost(post: ScheduledPost) = planner.delete(post)

    suspend fun addAccount(platform: Platform, username: String): Long {
        val clean = username.trim().removePrefix("@")
        return dao.insertAccount(TrackedAccount(platformId = platform.id, username = clean))
    }

    suspend fun addSnapshot(accountId: Long, followers: Int, following: Int?, posts: Int?) {
        dao.insertSnapshot(MetricSnapshot(accountId = accountId, followers = followers, following = following, posts = posts))
    }

    suspend fun setGoal(accountId: Long, goal: Int?) = dao.setGoal(accountId, goal)

    // --- Bulut geri yükleme (CloudSyncService) ---
    // Tam hesap nesnesini (hedef + sıra) ekler, üretilen id'yi döndürür.
    suspend fun insertFullAccount(account: TrackedAccount): Long = dao.insertAccount(account)
    // capturedAt dahil hazır snapshot ekler (geri yüklemede zaman damgası korunur).
    suspend fun insertSnapshotRaw(snapshot: MetricSnapshot) = dao.insertSnapshot(snapshot)

    suspend fun delete(account: TrackedAccount) = dao.deleteAccount(account)
    suspend fun deleteAll() = dao.deleteAllAccounts()
    suspend fun count() = dao.accountCount()

    companion object {
        const val FREE_ACCOUNT_LIMIT = 3
        @Volatile private var INSTANCE: Repository? = null
        fun get(context: Context): Repository =
            INSTANCE ?: synchronized(this) { INSTANCE ?: Repository(context).also { INSTANCE = it } }
    }
}
