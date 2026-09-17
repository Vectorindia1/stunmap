package com.stunmap.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.stunmap.db.entities.GeoResultEntity
import com.stunmap.db.entities.SessionEntity
import com.stunmap.db.entities.StunHitEntity

@Database(
    entities = [
        SessionEntity::class,
        StunHitEntity::class,
        GeoResultEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun stunHitDao(): StunHitDao
    abstract fun geoResultDao(): GeoResultDao

    companion object {
        const val DB_NAME = "stunmap.db"
    }
}
