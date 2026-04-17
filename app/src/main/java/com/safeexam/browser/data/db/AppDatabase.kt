package com.safeexam.browser.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.safeexam.browser.data.db.dao.ExamDao
import com.safeexam.browser.data.db.dao.ViolationDao
import com.safeexam.browser.data.db.entity.Exam
import com.safeexam.browser.data.db.entity.Violation

@Database(
    entities = [Exam::class, Violation::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun examDao(): ExamDao
    abstract fun violationDao(): ViolationDao

    companion object {
        const val DATABASE_NAME = "safe_exam_db"
    }
}
