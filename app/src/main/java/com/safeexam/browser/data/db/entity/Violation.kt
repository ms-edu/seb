package com.safeexam.browser.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class ViolationType {
    APP_SWITCH,
    SCREENSHOT_ATTEMPT,
    OVERLAY_DETECTED,
    CLIPBOARD_COPY,
    INCOMING_CALL,
    NOTIFICATION_RECEIVED,
    SPLIT_SCREEN,
    ROOT_DETECTED,
    DEV_MODE_DETECTED,
    VPN_DETECTED
}

@Entity(
    tableName = "violations",
    foreignKeys = [
        ForeignKey(
            entity = Exam::class,
            parentColumns = ["id"],
            childColumns = ["examId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("examId")]
)
data class Violation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val examId: Long,
    val type: ViolationType,
    val timestamp: Long = System.currentTimeMillis(),
    val detail: String = ""
)
