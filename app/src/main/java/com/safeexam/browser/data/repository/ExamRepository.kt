package com.safeexam.browser.data.repository

import com.safeexam.browser.data.db.dao.ExamDao
import com.safeexam.browser.data.db.entity.Exam
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExamRepository @Inject constructor(
    private val examDao: ExamDao
) {
    fun getAllExams(): Flow<List<Exam>> = examDao.getAllExams()

    suspend fun getExamById(id: Long): Exam? = examDao.getExamById(id)

    suspend fun saveExam(exam: Exam): Long = examDao.insertExam(exam)

    suspend fun updateExam(exam: Exam) = examDao.updateExam(exam)

    suspend fun deleteExam(exam: Exam) = examDao.deleteExam(exam)

    /**
     * Validates that a URL is a Google Form URL.
     */
    fun isValidGoogleFormUrl(url: String): Boolean {
        val trimmed = url.trim()
        return trimmed.startsWith("https://docs.google.com/forms/") ||
            trimmed.startsWith("http://docs.google.com/forms/")
    }
}
