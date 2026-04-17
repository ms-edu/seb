package com.safeexam.browser.security

import android.app.Activity
import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.view.WindowManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

data class SecurityEvent(val type: SecurityEventType, val detail: String = "")

enum class SecurityEventType {
    APP_SWITCH,
    CLIPBOARD_COPY,
    OVERLAY_DETECTED,
    SPLIT_SCREEN_DETECTED,
    CALL_INCOMING,
    VIOLATION_LIMIT_REACHED
}

@Singleton
class AntiCheatManager @Inject constructor(
    @ApplicationContext private val context: Context,
    val rootDetector: RootDetector,
    val devModeDetector: DevModeDetector,
    val vpnDetector: VpnDetector
) {
    companion object {
        const val MAX_VIOLATIONS = 3
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _violationCount = MutableStateFlow(0)
    val violationCount: StateFlow<Int> = _violationCount.asStateFlow()

    private val _securityEvent = MutableSharedFlow<SecurityEvent>()
    val securityEvent: SharedFlow<SecurityEvent> = _securityEvent.asSharedFlow()

    private var clipboardListener: ClipboardManager.OnPrimaryClipChangedListener? = null
    private var examId: Long = -1

    // ──────────────────────────────────────────────
    // Initialise for a specific exam session
    // ──────────────────────────────────────────────

    fun startSession(id: Long) {
        examId = id
        _violationCount.value = 0
    }

    fun endSession() {
        stopClipboardMonitoring()
        examId = -1
    }

    // ──────────────────────────────────────────────
    // FLAG_SECURE (prevent screenshots / recordings)
    // ──────────────────────────────────────────────

    fun applyScreenSecurity(activity: Activity) {
        activity.window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
    }

    // ──────────────────────────────────────────────
    // Kiosk / Lock-Task Mode
    // ──────────────────────────────────────────────

    fun enterKioskMode(activity: Activity) {
        val am = activity.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val dpm = activity.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminComponent = ComponentName(context, ExamDeviceAdminReceiver::class.java)

        if (dpm.isDeviceOwnerApp(context.packageName)) {
            // Full device-owner lock task
            dpm.setLockTaskPackages(adminComponent, arrayOf(context.packageName))
            activity.startLockTask()
        } else if (am.isInLockTaskMode) {
            // Already in lock-task (pinned by user)
        } else {
            // Fallback: start screen-pinning (requires LOCK_TASK_MODE_PINNED)
            activity.startLockTask()
        }
    }

    fun exitKioskMode(activity: Activity) {
        try {
            activity.stopLockTask()
        } catch (_: Exception) { /* ignore if not in kiosk */ }
    }

    // ──────────────────────────────────────────────
    // App-switch / lifecycle violation
    // ──────────────────────────────────────────────

    fun onAppSwitchDetected() {
        recordViolation(SecurityEventType.APP_SWITCH, "User switched app")
    }

    // ──────────────────────────────────────────────
    // Clipboard monitoring
    // ──────────────────────────────────────────────

    fun startClipboardMonitoring() {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardListener = ClipboardManager.OnPrimaryClipChangedListener {
            recordViolation(SecurityEventType.CLIPBOARD_COPY, "Clipboard change detected")
        }
        cm.addPrimaryClipChangedListener(clipboardListener!!)
    }

    fun stopClipboardMonitoring() {
        clipboardListener?.let {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.removePrimaryClipChangedListener(it)
            clipboardListener = null
        }
    }

    // ──────────────────────────────────────────────
    // Split-screen / multi-window detection
    // ──────────────────────────────────────────────

    fun checkMultiWindow(activity: Activity): Boolean {
        val inMultiWindow = activity.isInMultiWindowMode
        if (inMultiWindow) {
            recordViolation(SecurityEventType.SPLIT_SCREEN_DETECTED, "Split-screen mode detected")
        }
        return inMultiWindow
    }

    // ──────────────────────────────────────────────
    // Violation management
    // ──────────────────────────────────────────────

    fun recordViolation(type: SecurityEventType, detail: String = "") {
        val newCount = _violationCount.value + 1
        _violationCount.value = newCount
        scope.launch {
            _securityEvent.emit(SecurityEvent(type, detail))
            if (newCount >= MAX_VIOLATIONS) {
                _securityEvent.emit(SecurityEvent(SecurityEventType.VIOLATION_LIMIT_REACHED))
            }
        }
    }

    fun isViolationLimitReached(): Boolean =
        _violationCount.value >= MAX_VIOLATIONS

    // ──────────────────────────────────────────────
    // Pre-exam security checks
    // ──────────────────────────────────────────────

    data class PreExamCheckResult(
        val isRooted: Boolean,
        val isDevMode: Boolean,
        val isUsbDebug: Boolean,
        val isVpn: Boolean
    ) {
        val hasHighRiskIssue: Boolean get() = isRooted
        val hasWarning: Boolean get() = isDevMode || isUsbDebug || isVpn
        val isClean: Boolean get() = !hasHighRiskIssue && !hasWarning
    }

    fun performPreExamChecks(): PreExamCheckResult = PreExamCheckResult(
        isRooted   = rootDetector.isRooted(),
        isDevMode  = devModeDetector.isDeveloperModeEnabled(),
        isUsbDebug = devModeDetector.isUsbDebuggingEnabled(),
        isVpn      = vpnDetector.isVpnConnected()
    )
}
