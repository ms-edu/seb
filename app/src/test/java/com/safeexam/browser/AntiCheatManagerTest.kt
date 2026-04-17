package com.safeexam.browser

import android.content.Context
import com.safeexam.browser.security.AntiCheatManager
import com.safeexam.browser.security.DevModeDetector
import com.safeexam.browser.security.RootDetector
import com.safeexam.browser.security.SecurityEventType
import com.safeexam.browser.security.VpnDetector
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AntiCheatManagerTest {

    private lateinit var manager: AntiCheatManager
    private lateinit var rootDetector: RootDetector
    private lateinit var devModeDetector: DevModeDetector
    private lateinit var vpnDetector: VpnDetector
    private lateinit var context: Context

    @Before
    fun setUp() {
        context          = mockk(relaxed = true)
        rootDetector     = mockk()
        devModeDetector  = mockk()
        vpnDetector      = mockk()
        manager          = AntiCheatManager(context, rootDetector, devModeDetector, vpnDetector)
    }

    // ── Violation counting ────────────────────────────────────────────────────

    @Test
    fun `violation count starts at 0 after session start`() = runTest {
        manager.startSession(1L)
        assertEquals(0, manager.violationCount.first())
    }

    @Test
    fun `recordViolation increments count`() = runTest {
        manager.startSession(1L)

        manager.recordViolation(SecurityEventType.APP_SWITCH)
        manager.recordViolation(SecurityEventType.CLIPBOARD_COPY)

        assertEquals(2, manager.violationCount.value)
    }

    @Test
    fun `isViolationLimitReached returns false below limit`() = runTest {
        manager.startSession(1L)
        repeat(AntiCheatManager.MAX_VIOLATIONS - 1) {
            manager.recordViolation(SecurityEventType.APP_SWITCH)
        }
        assertFalse(manager.isViolationLimitReached())
    }

    @Test
    fun `isViolationLimitReached returns true at limit`() = runTest {
        manager.startSession(1L)
        repeat(AntiCheatManager.MAX_VIOLATIONS) {
            manager.recordViolation(SecurityEventType.APP_SWITCH)
        }
        assertTrue(manager.isViolationLimitReached())
    }

    @Test
    fun `startSession resets violation count`() = runTest {
        manager.startSession(1L)
        repeat(2) { manager.recordViolation(SecurityEventType.APP_SWITCH) }

        manager.startSession(2L)
        assertEquals(0, manager.violationCount.value)
    }

    // ── Pre-exam checks ───────────────────────────────────────────────────────

    @Test
    fun `performPreExamChecks isClean when no issues`() {
        every { rootDetector.isRooted() }                 returns false
        every { devModeDetector.isDeveloperModeEnabled() } returns false
        every { devModeDetector.isUsbDebuggingEnabled() }  returns false
        every { vpnDetector.isVpnConnected() }            returns false

        val result = manager.performPreExamChecks()

        assertTrue(result.isClean)
        assertFalse(result.hasHighRiskIssue)
        assertFalse(result.hasWarning)
    }

    @Test
    fun `performPreExamChecks hasHighRiskIssue when rooted`() {
        every { rootDetector.isRooted() }                 returns true
        every { devModeDetector.isDeveloperModeEnabled() } returns false
        every { devModeDetector.isUsbDebuggingEnabled() }  returns false
        every { vpnDetector.isVpnConnected() }            returns false

        val result = manager.performPreExamChecks()

        assertTrue(result.hasHighRiskIssue)
        assertFalse(result.isClean)
    }

    @Test
    fun `performPreExamChecks hasWarning when devMode enabled`() {
        every { rootDetector.isRooted() }                 returns false
        every { devModeDetector.isDeveloperModeEnabled() } returns true
        every { devModeDetector.isUsbDebuggingEnabled() }  returns false
        every { vpnDetector.isVpnConnected() }            returns false

        val result = manager.performPreExamChecks()

        assertFalse(result.hasHighRiskIssue)
        assertTrue(result.hasWarning)
        assertFalse(result.isClean)
    }

    @Test
    fun `performPreExamChecks hasWarning when VPN connected`() {
        every { rootDetector.isRooted() }                 returns false
        every { devModeDetector.isDeveloperModeEnabled() } returns false
        every { devModeDetector.isUsbDebuggingEnabled() }  returns false
        every { vpnDetector.isVpnConnected() }            returns true

        val result = manager.performPreExamChecks()

        assertTrue(result.hasWarning)
    }

    // ── Security events ───────────────────────────────────────────────────────

    @Test
    fun `recordViolation emits VIOLATION_LIMIT_REACHED event at max violations`() = runTest {
        manager.startSession(1L)

        val events = mutableListOf<SecurityEventType>()
        val job = kotlinx.coroutines.launch {
            manager.securityEvent.collect { events.add(it.type) }
        }

        repeat(AntiCheatManager.MAX_VIOLATIONS) {
            manager.recordViolation(SecurityEventType.APP_SWITCH)
        }

        kotlinx.coroutines.delay(100)
        job.cancel()

        assertTrue(events.contains(SecurityEventType.VIOLATION_LIMIT_REACHED))
    }
}
