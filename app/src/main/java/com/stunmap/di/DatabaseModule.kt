package com.stunmap.di

import android.content.Context
import androidx.room.Room
import com.stunmap.db.AppDatabase
import com.stunmap.db.GeoResultDao
import com.stunmap.db.SessionDao
import com.stunmap.db.StunHitDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.DB_NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideSessionDao(db: AppDatabase): SessionDao = db.sessionDao()

    @Provides
    fun provideStunHitDao(db: AppDatabase): StunHitDao = db.stunHitDao()

    @Provides
    fun provideGeoResultDao(db: AppDatabase): GeoResultDao = db.geoResultDao()
}
