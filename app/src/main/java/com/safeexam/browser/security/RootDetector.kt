package com.safeexam.browser.security

import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RootDetector @Inject constructor() {

    private val rootBinaries = listOf(
        "su", "busybox", "supersu", "magisk", "resetprop"
    )

    private val rootPaths = listOf(
        "/system/app/Superuser.apk",
        "/system/app/SuperSU.apk",
        "/system/xbin/su",
        "/system/bin/su",
        "/system/sbin/su",
        "/vendor/bin/su",
        "/sbin/su",
        "/su/bin/su",
        "/data/local/xbin/su",
        "/data/local/bin/su",
        "/data/local/su",
        "/system/sd/xbin/su",
        "/system/bin/.ext/.su",
        "/cache/su",
        "/data/su"
    )

    private val rootPackages = listOf(
        "com.koushikdutta.superuser",
        "com.noshufou.android.su",
        "eu.chainfire.supersu",
        "com.topjohnwu.magisk"
    )

    /**
     * Returns true if the device appears to be rooted.
     */
    fun isRooted(): Boolean =
        checkSuBinary() || checkRootPaths() || checkMagiskMount()

    private fun checkSuBinary(): Boolean {
        val paths = System.getenv("PATH")?.split(":") ?: emptyList()
        return rootBinaries.any { binary ->
            paths.any { path -> File("$path/$binary").exists() }
        }
    }

    private fun checkRootPaths(): Boolean =
        rootPaths.any { File(it).exists() }

    private fun checkMagiskMount(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("mount"))
            val output = process.inputStream.bufferedReader().readText()
            output.contains("magisk") || output.contains("/sbin/.core")
        } catch (_: Exception) {
            false
        }
    }
}
