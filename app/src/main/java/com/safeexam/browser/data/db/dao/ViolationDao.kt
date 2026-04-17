package com.safeexam.browser.data.db.dao

import androidx.room.*
import com.safeexam.browser.data.db.entity.Violation
import kotlinx.coroutines.flow.Flow

@Dao
interface ViolationDao {

    @Query("SELECT * FROM violations WHERE examId = :examId ORDER BY timestamp DESC")
    fun getViolationsByExam(examId: Long): Flow<List<Violation>>

    @Query("SELECT COUNT(*) FROM violations WHERE examId = :examId")
    suspend fun countViolationsByExam(examId: Long): Int

    @Insert
    suspend fun insertViolation(violation: Violation): Long

    @Query("DELETE FROM violations WHERE examId = :examId")
    suspend fun clearViolationsForExam(examId: Long)
}
