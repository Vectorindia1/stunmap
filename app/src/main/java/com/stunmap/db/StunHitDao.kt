package com.stunmap.db

import androidx.room.*
import com.stunmap.db.entities.StunHitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StunHitDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(hit: StunHitEntity)

    @Query("SELECT * FROM stun_hits WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getBySessionId(sessionId: String): Flow<List<StunHitEntity>>

    @Query("SELECT * FROM stun_hits WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getBySessionIdOnce(sessionId: String): List<StunHitEntity>

    @Query("DELETE FROM stun_hits WHERE sessionId = :sessionId")
    suspend fun deleteBySessionId(sessionId: String)
}
