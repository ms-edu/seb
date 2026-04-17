package com.safeexam.browser.data.db.dao

import androidx.room.*
import com.safeexam.browser.data.db.entity.Exam
import kotlinx.coroutines.flow.Flow

@Dao
interface ExamDao {

    @Query("SELECT * FROM exams ORDER BY createdAt DESC")
    fun getAllExams(): Flow<List<Exam>>

    @Query("SELECT * FROM exams WHERE id = :id")
    suspend fun getExamById(id: Long): Exam?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExam(exam: Exam): Long

    @Update
    suspend fun updateExam(exam: Exam)

    @Delete
    suspend fun deleteExam(exam: Exam)

    @Query("DELETE FROM exams WHERE id = :id")
    suspend fun deleteExamById(id: Long)
}
