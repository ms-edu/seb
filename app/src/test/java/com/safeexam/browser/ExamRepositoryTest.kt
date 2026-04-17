package com.safeexam.browser

import com.safeexam.browser.data.db.dao.ExamDao
import com.safeexam.browser.data.db.entity.Exam
import com.safeexam.browser.data.repository.ExamRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ExamRepositoryTest {

    private lateinit var examDao: ExamDao
    private lateinit var repository: ExamRepository

    @Before
    fun setUp() {
        examDao     = mockk(relaxed = true)
        repository  = ExamRepository(examDao)
    }

    // ── URL Validation ────────────────────────────────────────────────────────

    @Test
    fun `isValidGoogleFormUrl returns true for valid https URL`() {
        assertTrue(
            repository.isValidGoogleFormUrl(
                "https://docs.google.com/forms/d/1FAIpQLSe.../viewform"
            )
        )
    }

    @Test
    fun `isValidGoogleFormUrl returns true for valid http URL`() {
        assertTrue(
            repository.isValidGoogleFormUrl(
                "http://docs.google.com/forms/d/1FAIpQLSe.../viewform"
            )
        )
    }

    @Test
    fun `isValidGoogleFormUrl returns false for random URL`() {
        assertFalse(repository.isValidGoogleFormUrl("https://www.google.com"))
    }

    @Test
    fun `isValidGoogleFormUrl returns false for empty string`() {
        assertFalse(repository.isValidGoogleFormUrl(""))
    }

    @Test
    fun `isValidGoogleFormUrl returns false for partial match`() {
        assertFalse(repository.isValidGoogleFormUrl("https://docs.google.com/spreadsheets"))
    }

    @Test
    fun `isValidGoogleFormUrl trims whitespace before validating`() {
        assertTrue(
            repository.isValidGoogleFormUrl(
                "  https://docs.google.com/forms/d/abc/viewform  "
            )
        )
    }

    // ── DAO delegation ────────────────────────────────────────────────────────

    @Test
    fun `saveExam delegates to examDao insertExam`() = runTest {
        val exam = Exam(namaUjian = "Ujian Matematika", url = "https://docs.google.com/forms/d/abc")
        coEvery { examDao.insertExam(exam) } returns 42L

        val id = repository.saveExam(exam)

        coVerify(exactly = 1) { examDao.insertExam(exam) }
        assertTrue(id == 42L)
    }

    @Test
    fun `deleteExam delegates to examDao deleteExam`() = runTest {
        val exam = Exam(id = 1L, namaUjian = "Ujian IPA", url = "https://docs.google.com/forms/d/xyz")

        repository.deleteExam(exam)

        coVerify(exactly = 1) { examDao.deleteExam(exam) }
    }

    @Test
    fun `getExamById returns null when exam not found`() = runTest {
        coEvery { examDao.getExamById(999L) } returns null

        val result = repository.getExamById(999L)

        assertTrue(result == null)
    }
}
