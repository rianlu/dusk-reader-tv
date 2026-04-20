package com.wzl.duskreader.tv.data

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.wzl.duskreader.tv.data.local.AppDatabase
import com.wzl.duskreader.tv.data.local.BookDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "dusk_reader_database")
            .fallbackToDestructiveMigration()
            .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
            .build()

    @Provides
    fun provideBookDao(database: AppDatabase): BookDao = database.bookDao()
}
