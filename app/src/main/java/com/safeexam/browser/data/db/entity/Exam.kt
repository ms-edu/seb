package com.safeexam.browser.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exams")
data class Exam(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val namaUjian: String,
    val url: String,
    val createdAt: Long = System.currentTimeMillis()
)
