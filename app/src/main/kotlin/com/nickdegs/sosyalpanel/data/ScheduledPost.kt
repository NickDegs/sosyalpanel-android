package com.nickdegs.sosyalpanel.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

// İçerik takvimi — planlanmış paylaşım. Uygulama otomatik paylaşmaz, hatırlatır.
@Entity(tableName = "scheduled_posts")
data class ScheduledPost(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val note: String,
    val platformId: String,
    val scheduledAt: Long,
    val notify: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) {
    val platform: Platform get() = Platform.from(platformId)
    val isPast: Boolean get() = scheduledAt < System.currentTimeMillis()
}

@Dao
interface PlannerDao {
    @Query("SELECT * FROM scheduled_posts ORDER BY scheduledAt")
    fun observe(): Flow<List<ScheduledPost>>

    @Insert
    suspend fun insert(post: ScheduledPost): Long

    @Delete
    suspend fun delete(post: ScheduledPost)
}
