package com.safeexam.browser.ui.exam

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safeexam.browser.data.db.entity.ViolationType
import com.safeexam.browser.data.repository.ExamRepository
import com.safeexam.browser.data.repository.ViolationRepository
import com.safeexam.browser.security.AntiCheatManager
import com.safeexam.browser.security.SecurityEvent
import com.safeexam.browser.security.SecurityEventType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExamViewModel @Inject constructor(
    private val examRepository: ExamRepository,
    private val violationRepository: ViolationRepository,
    val antiCheatManager: AntiCheatManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExamUiState())
    val uiState: StateFlow<ExamUiState> = _uiState.asStateFlow()

    private val _examEvent = MutableSharedFlow<ExamEvent>()
    val examEvent: SharedFlow<ExamEvent> = _examEvent.asSharedFlow()

    fun initSession(examId: Long) {
        antiCheatManager.startSession(examId)

        viewModelScope.launch {
            val exam = examRepository.getExamById(examId)
            _uiState.value = _uiState.value.copy(
                examId   = examId,
                examName = exam?.namaUjian ?: "",
                examUrl  = exam?.url ?: ""
            )

            // Observe security events
            launch {
                antiCheatManager.securityEvent.collect { event ->
                    handleSecurityEvent(event, examId)
                }
            }

            // Observe violation count
            launch {
                antiCheatManager.violationCount.collect { count ->
                    _uiState.value = _uiState.value.copy(violationCount = count)
                }
            }
        }
    }

    private suspend fun handleSecurityEvent(event: SecurityEvent, examId: Long) {
        val violationType = event.type.toViolationType()
        violationType?.let {
            violationRepository.recordViolation(examId, it, event.detail)
        }

        when (event.type) {
            SecurityEventType.VIOLATION_LIMIT_REACHED -> {
                _examEvent.emit(ExamEvent.ViolationLimitReached)
            }
            SecurityEventType.APP_SWITCH -> {
                _examEvent.emit(ExamEvent.ShowWarning("Peringatan! Anda meninggalkan aplikasi ujian."))
            }
            SecurityEventType.CLIPBOARD_COPY -> {
                _examEvent.emit(ExamEvent.ShowWarning("Peringatan! Aktivitas clipboard terdeteksi."))
            }
            SecurityEventType.OVERLAY_DETECTED -> {
                _examEvent.emit(ExamEvent.ShowWarning("Peringatan! Aplikasi overlay terdeteksi."))
            }
            SecurityEventType.SPLIT_SCREEN_DETECTED -> {
                _examEvent.emit(ExamEvent.ShowWarning("Peringatan! Mode split-screen terdeteksi."))
            }
            SecurityEventType.CALL_INCOMING -> {
                _examEvent.emit(ExamEvent.ShowWarning("Peringatan! Panggilan masuk terdeteksi."))
            }
        }
    }

    fun onAppResumed() {
        if (_uiState.value.examId != -1L && _uiState.value.examStarted) {
            antiCheatManager.onAppSwitchDetected()
        }
    }

    fun onExamStarted() {
        _uiState.value = _uiState.value.copy(examStarted = true)
        antiCheatManager.startClipboardMonitoring()
    }

    fun endExam() {
        antiCheatManager.endSession()
        viewModelScope.launch { _examEvent.emit(ExamEvent.ExamEnded) }
    }

    override fun onCleared() {
        super.onCleared()
        antiCheatManager.endSession()
    }
}

// ──────────────────────────────────────────────
// UI State & Events
// ──────────────────────────────────────────────

data class ExamUiState(
    val examId: Long = -1L,
    val examName: String = "",
    val examUrl: String = "",
    val violationCount: Int = 0,
    val examStarted: Boolean = false
)

sealed class ExamEvent {
    data class ShowWarning(val message: String) : ExamEvent()
    object ViolationLimitReached : ExamEvent()
    object ExamEnded : ExamEvent()
}

// ──────────────────────────────────────────────
// Extension: map SecurityEventType → ViolationType
// ──────────────────────────────────────────────

private fun SecurityEventType.toViolationType(): ViolationType? = when (this) {
    SecurityEventType.APP_SWITCH             -> ViolationType.APP_SWITCH
    SecurityEventType.CLIPBOARD_COPY         -> ViolationType.CLIPBOARD_COPY
    SecurityEventType.OVERLAY_DETECTED       -> ViolationType.OVERLAY_DETECTED
    SecurityEventType.SPLIT_SCREEN_DETECTED  -> ViolationType.SPLIT_SCREEN
    SecurityEventType.CALL_INCOMING          -> ViolationType.INCOMING_CALL
    SecurityEventType.VIOLATION_LIMIT_REACHED -> null
}
