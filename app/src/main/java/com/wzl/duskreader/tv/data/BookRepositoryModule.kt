package com.wzl.duskreader.tv.data

import com.wzl.duskreader.tv.data.repositories.BookChapterRepository
import com.wzl.duskreader.tv.data.repositories.BookChapterRepositoryImpl
import com.wzl.duskreader.tv.data.repositories.BookRepository
import com.wzl.duskreader.tv.data.repositories.BookRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class BookRepositoryModule {

    @Binds
    abstract fun bindBookRepository(impl: BookRepositoryImpl): BookRepository

    @Binds
    abstract fun bindBookChapterRepository(impl: BookChapterRepositoryImpl): BookChapterRepository
}
