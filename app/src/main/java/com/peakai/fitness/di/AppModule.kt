package com.peakai.fitness.di

import android.content.Context
import androidx.room.Room
import com.peakai.fitness.data.local.CoachingDao
import com.peakai.fitness.data.local.LogDao
import com.peakai.fitness.data.local.LogDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideLogDatabase(@ApplicationContext context: Context): LogDatabase {
        return Room.databaseBuilder(
            context,
            LogDatabase::class.java,
            "peak_ai_log.db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideLogDao(db: LogDatabase): LogDao = db.logDao()

    @Provides
    @Singleton
    fun provideCoachingDao(db: LogDatabase): CoachingDao = db.coachingDao()
}
