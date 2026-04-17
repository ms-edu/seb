package com.safeexam.browser

import com.safeexam.browser.data.db.dao.ViolationDao
import com.safeexam.browser.data.db.entity.Violation
import com.safeexam.browser.data.db.entity.ViolationType
import com.safeexam.browser.data.repository.ViolationRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ViolationRepositoryTest {

    private lateinit var violationDao: ViolationDao
    private lateinit var repository: ViolationRepository

    @Before
    fun setUp() {
        violationDao = mockk(relaxed = true)
        repository   = ViolationRepository(violationDao)
    }

    @Test
    fun `recordViolation inserts correct Violation entity`() = runTest {
        coEvery { violationDao.insertViolation(any()) } returns 1L

        repository.recordViolation(
            examId = 5L,
            type   = ViolationType.APP_SWITCH,
            detail = "User switched to another app"
        )

        coVerify {
            violationDao.insertViolation(
                match { v ->
                    v.examId == 5L &&
                        v.type == ViolationType.APP_SWITCH &&
                        v.detail == "User switched to another app"
                }
            )
        }
    }

    @Test
    fun `countViolations delegates to dao`() = runTest {
        coEvery { violationDao.countViolationsByExam(3L) } returns 7

        val count = repository.countViolations(3L)

        assertEquals(7, count)
    }

    @Test
    fun `clearViolations delegates to dao`() = runTest {
        repository.clearViolations(10L)
        coVerify { violationDao.clearViolationsForExam(10L) }
    }

    @Test
    fun `recordViolation returns inserted id from dao`() = runTest {
        coEvery { violationDao.insertViolation(any()) } returns 42L

        val id = repository.recordViolation(
            examId = 1L,
            type   = ViolationType.CLIPBOARD_COPY
        )

        assertEquals(42L, id)
    }
}
