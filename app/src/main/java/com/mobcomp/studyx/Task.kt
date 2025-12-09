package com.mobcomp.studyx

data class Task(
    var id: String = "",
    var title: String = "",
    var description: String = "",
    var isCompleted: Boolean = false,
    var dueDate: Long = 0L,
    var priority: Int = 0,
    var createdAt: Long = System.currentTimeMillis(),
    var userId: String = ""  // This is CRITICAL for user separation
)