package com.stunmap.db

import androidx.room.*
import com.stunmap.db.entities.GeoResultEntity

@Dao
interface GeoResultDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(result: GeoResultEntity)

    @Query("SELECT * FROM geo_results WHERE ip = :ip")
    suspend fun getByIp(ip: String): GeoResultEntity?

    @Query("SELECT * FROM geo_results WHERE ip IN (:ips)")
    suspend fun getByIps(ips: List<String>): List<GeoResultEntity>

    @Query("DELETE FROM geo_results")
    suspend fun deleteAll()
}
