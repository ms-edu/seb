package com.safeexam.browser.security

import android.content.Context
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DevModeDetector @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * Returns true if Developer Options are enabled.
     */
    fun isDeveloperModeEnabled(): Boolean {
        return try {
            Settings.Global.getInt(
                context.contentResolver,
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
                0
            ) != 0
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Returns true if USB Debugging is enabled.
     */
    fun isUsbDebuggingEnabled(): Boolean {
        return try {
            Settings.Global.getInt(
                context.contentResolver,
                Settings.Global.ADB_ENABLED,
                0
            ) != 0
        } catch (_: Exception) {
            false
        }
    }
}
