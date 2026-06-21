package com.nickdegs.sosyalpanel.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// iOS TrackedAccount karşılığı.
@Entity(tableName = "accounts")
data class TrackedAccount(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val platformId: String,
    val username: String,
    val sortOrder: Int = 0,
    val addedAt: Long = System.currentTimeMillis(),
    val goalFollowers: Int? = null      // kullanıcı hedefi (opsiyonel)
) {
    val platform: Platform get() = Platform.from(platformId)
}

// iOS MetricSnapshot karşılığı — bir hesabın belirli andaki metrikleri.
@Entity(
    tableName = "snapshots",
    foreignKeys = [ForeignKey(
        entity = TrackedAccount::class,
        parentColumns = ["id"],
        childColumns = ["accountId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("accountId")]
)
data class MetricSnapshot(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    val followers: Int,
    val following: Int? = null,
    val posts: Int? = null,
    val capturedAt: Long = System.currentTimeMillis()
)

// Hesap + tüm snapshot'ları (ekranlar için birleşik model).
data class AccountWithSnapshots(
    val account: TrackedAccount,
    val snapshots: List<MetricSnapshot>
) {
    val latest: MetricSnapshot? get() = snapshots.maxByOrNull { it.capturedAt }
    val sorted: List<MetricSnapshot> get() = snapshots.sortedBy { it.capturedAt }

    // Seçilen son N gün içindeki ilk→son takipçi % büyümesi.
    fun growthPercent(): Double {
        val s = sorted
        if (s.size < 2) return 0.0
        val first = s.first().followers.toDouble()
        val last = s.last().followers.toDouble()
        if (first <= 0) return 0.0
        return (last - first) / first * 100.0
    }

    fun dailyAverage(): Int {
        val s = sorted
        if (s.size < 2) return 0
        val days = ((s.last().capturedAt - s.first().capturedAt) / 86_400_000.0).coerceAtLeast(1.0)
        return ((s.last().followers - s.first().followers) / days).toInt()
    }

    // --- Hedef & Milestone (içerik üretici) ---
    val currentFollowers: Int get() = latest?.followers ?: 0
    val reachedMilestones: List<Int> get() = Milestone.thresholds.filter { currentFollowers >= it }
    val nextMilestone: Int? get() = Milestone.thresholds.firstOrNull { it > currentFollowers }
    val effectiveGoal: Int? get() = account.goalFollowers ?: nextMilestone
    val goalProgress: Float get() {
        val g = effectiveGoal ?: return 0f
        if (g <= 0) return 0f
        return (currentFollowers.toFloat() / g).coerceIn(0f, 1f)
    }
    val goalEtaText: String? get() {
        val g = effectiveGoal ?: return null
        if (currentFollowers >= g) return null
        val growth = dailyAverage()
        if (growth <= 0) return null
        val days = Math.ceil((g - currentFollowers).toDouble() / growth).toInt()
        return when {
            days <= 0 -> null
            days < 30 -> "~$days gün"
            days < 365 -> "~${days / 30} ay"
            else -> "~${days / 365} yıl"
        }
    }
}

object Milestone {
    val thresholds = listOf(1_000, 5_000, 10_000, 25_000, 50_000, 100_000,
                            250_000, 500_000, 1_000_000, 5_000_000, 10_000_000)
    fun label(n: Int): String = when {
        n >= 1_000_000 -> "${n / 1_000_000}M"
        n >= 1_000 -> "${n / 1_000}K"
        else -> "$n"
    }
}
