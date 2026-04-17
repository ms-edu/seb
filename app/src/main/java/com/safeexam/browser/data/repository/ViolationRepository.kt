package com.safeexam.browser.data.repository

import com.safeexam.browser.data.db.dao.ViolationDao
import com.safeexam.browser.data.db.entity.Violation
import com.safeexam.browser.data.db.entity.ViolationType
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ViolationRepository @Inject constructor(
    private val violationDao: ViolationDao
) {
    fun getViolationsByExam(examId: Long): Flow<List<Violation>> =
        violationDao.getViolationsByExam(examId)

    suspend fun countViolations(examId: Long): Int =
        violationDao.countViolationsByExam(examId)

    suspend fun recordViolation(examId: Long, type: ViolationType, detail: String = ""): Long =
        violationDao.insertViolation(
            Violation(examId = examId, type = type, detail = detail)
        )

    suspend fun clearViolations(examId: Long) =
        violationDao.clearViolationsForExam(examId)
}
