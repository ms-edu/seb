package com.safeexam.browser.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safeexam.browser.data.db.entity.Exam
import com.safeexam.browser.data.repository.ExamRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val examRepository: ExamRepository
) : ViewModel() {

    val exams: StateFlow<List<Exam>> = examRepository
        .getAllExams()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun deleteExam(exam: Exam) {
        viewModelScope.launch { examRepository.deleteExam(exam) }
    }
}
