package com.mobcomp.studyx

import com.google.firebase.firestore.PropertyName
import java.util.*

data class StudySession(
    @get:PropertyName("id") @set:PropertyName("id")
    var id: String = "",

    @get:PropertyName("userId") @set:PropertyName("userId")
    var userId: String = "",

    @get:PropertyName("startTime") @set:PropertyName("startTime")
    var startTime: Long = 0,

    @get:PropertyName("endTime") @set:PropertyName("endTime")
    var endTime: Long = 0,

    @get:PropertyName("duration") @set:PropertyName("duration")
    var duration: Long = 0, // in minutes

    @get:PropertyName("sessionType") @set:PropertyName("sessionType")
    var sessionType: String = "focus", // "focus" or "break"

    @get:PropertyName("createdAt") @set:PropertyName("createdAt")
    var createdAt: Long = System.currentTimeMillis()
) {
    // Helper to get formatted duration
    fun getFormattedDuration(): String {
        val hours = duration / 60
        val minutes = duration % 60

        return if (hours > 0) {
            "${hours}h ${minutes}m"
        } else {
            "${minutes}m"
        }
    }

    // Helper to get date string
    fun getDateString(): String {
        val date = Date(createdAt)
        val format = java.text.SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return format.format(date)
    }
}