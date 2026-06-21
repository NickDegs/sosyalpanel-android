package com.nickdegs.sosyalpanel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nickdegs.sosyalpanel.billing.BillingManager
import com.nickdegs.sosyalpanel.data.AccountWithSnapshots
import com.nickdegs.sosyalpanel.data.Platform
import com.nickdegs.sosyalpanel.data.PublicMetricsService
import com.nickdegs.sosyalpanel.data.Repository
import com.nickdegs.sosyalpanel.data.TrackedAccount
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = Repository.get(app)
    val billing = BillingManager(app)

    val accounts: StateFlow<List<AccountWithSnapshots>> =
        repo.accounts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val scheduledPosts: StateFlow<List<com.nickdegs.sosyalpanel.data.ScheduledPost>> =
        repo.scheduledPosts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init { billing.start() }

    fun canAddAccount(): Boolean =
        billing.isPro.value || accounts.value.size < Repository.FREE_ACCOUNT_LIMIT

    fun addAccount(platform: Platform, username: String) = viewModelScope.launch {
        val id = repo.addAccount(platform, username)
        // Desteklenen platformda public veriyi otomatik çek → ilk snapshot.
        if (PublicMetricsService.isSupported(platform)) {
            PublicMetricsService.fetch(platform, username)?.let { m ->
                repo.addSnapshot(id, m.followers, m.following, m.posts)
            }
        }
    }

    // Desteklenen platformlardaki hesapların public verisini tazeler (değişmişse snapshot).
    fun refreshSupported() = viewModelScope.launch {
        accounts.value.forEach { acc ->
            if (!PublicMetricsService.isSupported(acc.account.platform)) return@forEach
            val m = PublicMetricsService.fetch(acc.account.platform, acc.account.username) ?: return@forEach
            val latest = acc.latest
            if (latest?.followers == m.followers && latest.posts == m.posts) return@forEach
            repo.addSnapshot(acc.account.id, m.followers, m.following, m.posts)
        }
    }

    fun addSnapshot(accountId: Long, followers: Int, following: Int?, posts: Int?) = viewModelScope.launch {
        repo.addSnapshot(accountId, followers, following, posts)
    }

    fun setGoal(accountId: Long, goal: Int?) = viewModelScope.launch { repo.setGoal(accountId, goal) }

    fun addScheduledPost(note: String, platform: Platform, atMillis: Long, notify: Boolean) = viewModelScope.launch {
        val post = com.nickdegs.sosyalpanel.data.ScheduledPost(
            note = note, platformId = platform.id, scheduledAt = atMillis, notify = notify)
        val id = repo.addScheduledPost(post)
        if (notify) {
            com.nickdegs.sosyalpanel.data.ReminderScheduler.schedule(
                getApplication(), id, platform.displayName, note, atMillis)
        }
    }

    fun deleteScheduledPost(post: com.nickdegs.sosyalpanel.data.ScheduledPost) = viewModelScope.launch {
        com.nickdegs.sosyalpanel.data.ReminderScheduler.cancel(getApplication(), post.id)
        repo.deleteScheduledPost(post)
    }

    fun delete(account: TrackedAccount) = viewModelScope.launch { repo.delete(account) }

    fun deleteAll() = viewModelScope.launch { repo.deleteAll() }
}
