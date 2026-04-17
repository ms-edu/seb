package com.safeexam.browser

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.safeexam.browser.data.db.entity.Exam
import com.safeexam.browser.data.repository.ExamRepository
import com.safeexam.browser.ui.addexam.AddExamViewModel
import com.safeexam.browser.ui.addexam.SaveState
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddExamViewModelTest {

    @get:Rule
    val instantTaskRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: ExamRepository
    private lateinit var viewModel: AddExamViewModel

    private val validUrl  = "https://docs.google.com/forms/d/1FAIpQLSe/viewform"
    private val invalidUrl = "https://www.somesite.com/form"

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        viewModel  = AddExamViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `saveExam with empty name emits Error state`() = runTest {
        viewModel.saveExam(name = "", url = validUrl)
        advanceUntilIdle()

        val state = viewModel.saveState.value
        assertTrue(state is SaveState.Error)
        assertTrue((state as SaveState.Error).message.contains("kosong", ignoreCase = true))
    }

    @Test
    fun `saveExam with invalid URL emits Error state`() = runTest {
        every { repository.isValidGoogleFormUrl(invalidUrl) } returns false

        viewModel.saveExam(name = "Ujian Fisika", url = invalidUrl)
        advanceUntilIdle()

        val state = viewModel.saveState.value
        assertTrue(state is SaveState.Error)
        assertTrue((state as SaveState.Error).message.contains("URL", ignoreCase = true))
    }

    @Test
    fun `saveExam with valid inputs emits Success state with examId`() = runTest {
        every { repository.isValidGoogleFormUrl(validUrl) } returns true
        coEvery { repository.saveExam(any<Exam>()) } returns 7L

        viewModel.saveExam(name = "Ujian Biologi", url = validUrl)
        advanceUntilIdle()

        val state = viewModel.saveState.value
        assertTrue(state is SaveState.Success)
        assertEquals(7L, (state as SaveState.Success).examId)
    }

    @Test
    fun `resetState sets state back to Idle`() = runTest {
        every { repository.isValidGoogleFormUrl(invalidUrl) } returns false
        viewModel.saveExam(name = "Test", url = invalidUrl)
        advanceUntilIdle()

        viewModel.resetState()

        assertTrue(viewModel.saveState.value is SaveState.Idle)
    }

    @Test
    fun `saveExam trims whitespace from name and url`() = runTest {
        every { repository.isValidGoogleFormUrl(validUrl) } returns true
        coEvery { repository.saveExam(any<Exam>()) } returns 1L

        viewModel.saveExam(name = "  Ujian PKN  ", url = "  $validUrl  ")
        advanceUntilIdle()

        assertTrue(viewModel.saveState.value is SaveState.Success)
    }
}
