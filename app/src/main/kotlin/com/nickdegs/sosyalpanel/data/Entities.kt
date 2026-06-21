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
    val addedAt: Long = System.currentTimeMillis()
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
}
