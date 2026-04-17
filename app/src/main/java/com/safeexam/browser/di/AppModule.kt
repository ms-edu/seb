package com.safeexam.browser.di

import android.content.Context
import androidx.room.Room
import com.safeexam.browser.data.db.AppDatabase
import com.safeexam.browser.data.db.dao.ExamDao
import com.safeexam.browser.data.db.dao.ViolationDao
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
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        ).build()

    @Provides
    fun provideExamDao(db: AppDatabase): ExamDao = db.examDao()

    @Provides
    fun provideViolationDao(db: AppDatabase): ViolationDao = db.violationDao()
}
