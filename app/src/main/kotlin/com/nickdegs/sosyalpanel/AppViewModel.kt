package com.nickdegs.sosyalpanel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nickdegs.sosyalpanel.billing.BillingManager
import com.nickdegs.sosyalpanel.data.AccountWithSnapshots
import com.nickdegs.sosyalpanel.data.Platform
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

    init { billing.start() }

    fun canAddAccount(): Boolean =
        billing.isPro.value || accounts.value.size < Repository.FREE_ACCOUNT_LIMIT

    fun addAccount(platform: Platform, username: String) = viewModelScope.launch {
        repo.addAccount(platform, username)
    }

    fun addSnapshot(accountId: Long, followers: Int, following: Int?, posts: Int?) = viewModelScope.launch {
        repo.addSnapshot(accountId, followers, following, posts)
    }

    fun delete(account: TrackedAccount) = viewModelScope.launch { repo.delete(account) }

    fun deleteAll() = viewModelScope.launch { repo.deleteAll() }
}
