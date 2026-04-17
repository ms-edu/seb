package com.safeexam.browser.ui.addexam

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safeexam.browser.data.db.entity.Exam
import com.safeexam.browser.data.repository.ExamRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SaveState {
    object Idle : SaveState()
    object Saving : SaveState()
    data class Success(val examId: Long) : SaveState()
    data class Error(val message: String) : SaveState()
}

@HiltViewModel
class AddExamViewModel @Inject constructor(
    private val examRepository: ExamRepository
) : ViewModel() {

    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState.asStateFlow()

    fun saveExam(name: String, url: String) {
        val trimmedName = name.trim()
        val trimmedUrl  = url.trim()

        when {
            trimmedName.isEmpty() -> {
                _saveState.value = SaveState.Error("Nama ujian tidak boleh kosong")
                return
            }
            !examRepository.isValidGoogleFormUrl(trimmedUrl) -> {
                _saveState.value = SaveState.Error(
                    "URL tidak valid. Harus menggunakan URL Google Form (docs.google.com/forms)"
                )
                return
            }
        }

        viewModelScope.launch {
            _saveState.value = SaveState.Saving
            val id = examRepository.saveExam(Exam(namaUjian = trimmedName, url = trimmedUrl))
            _saveState.value = SaveState.Success(id)
        }
    }

    fun resetState() {
        _saveState.value = SaveState.Idle
    }
}
