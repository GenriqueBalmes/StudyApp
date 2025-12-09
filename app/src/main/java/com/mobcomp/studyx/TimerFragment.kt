package com.mobcomp.studyx

import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class TimerFragment : Fragment() {

    // Views
    private lateinit var tvTimer: TextView
    private lateinit var tvTimerStatus: TextView
    private lateinit var progressTimer: CircularProgressIndicator
    private lateinit var btnFocus: Button
    private lateinit var btnBreak: Button
    private lateinit var btnStart: Button
    private lateinit var btnPause: Button
    private lateinit var btnReset: Button
    private lateinit var tvFocusSessions: TextView
    private lateinit var tvTotalTime: TextView
    private lateinit var tvStreak: TextView

    // Timer variables
    private var countDownTimer: CountDownTimer? = null
    private var timeLeftInMillis: Long = 25 * 60 * 1000 // 25 minutes
    private var timerRunning = false
    private var isFocusSession = true
    private var sessionStartTime: Long = 0

    // Constants
    private val FOCUS_TIME = 25 * 60 * 1000L // 25 minutes in milliseconds
    private val BREAK_TIME = 5 * 60 * 1000L  // 5 minutes in milliseconds

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val layoutId = requireActivity().resources.getIdentifier("fragment_timer", "layout", requireActivity().packageName)
        return inflater.inflate(layoutId, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get view IDs
        tvTimer = view.findViewById(getViewId("tvTimer"))
        tvTimerStatus = view.findViewById(getViewId("tvTimerStatus"))
        progressTimer = view.findViewById(getViewId("progressTimer"))
        btnFocus = view.findViewById(getViewId("btnFocus"))
        btnBreak = view.findViewById(getViewId("btnBreak"))
        btnStart = view.findViewById(getViewId("btnStart"))
        btnPause = view.findViewById(getViewId("btnPause"))
        btnReset = view.findViewById(getViewId("btnReset"))
        tvFocusSessions = view.findViewById(getViewId("tvFocusSessions"))
        tvTotalTime = view.findViewById(getViewId("tvTotalTime"))
        tvStreak = view.findViewById(getViewId("tvStreak"))

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Setup progress bar
        progressTimer.max = FOCUS_TIME.toInt()
        progressTimer.progress = FOCUS_TIME.toInt()

        // Update timer display
        updateTimerDisplay()

        // Setup button listeners
        setupButtonListeners()

        // Load stats
        loadStats()
    }

    private fun setupButtonListeners() {
        btnFocus.setOnClickListener {
            if (!timerRunning) {
                switchToFocus()
            }
        }

        btnBreak.setOnClickListener {
            if (!timerRunning) {
                switchToBreak()
            }
        }

        btnStart.setOnClickListener {
            startTimer()
        }

        btnPause.setOnClickListener {
            pauseTimer()
        }

        btnReset.setOnClickListener {
            resetTimer()
        }
    }

    private fun switchToFocus() {
        isFocusSession = true
        timeLeftInMillis = FOCUS_TIME
        progressTimer.max = FOCUS_TIME.toInt()
        progressTimer.progress = FOCUS_TIME.toInt()
        tvTimerStatus.text = "Focus Session"
        updateTimerDisplay()
        updateButtonColors()
    }

    private fun switchToBreak() {
        isFocusSession = false
        timeLeftInMillis = BREAK_TIME
        progressTimer.max = BREAK_TIME.toInt()
        progressTimer.progress = BREAK_TIME.toInt()
        tvTimerStatus.text = "Break Time"
        updateTimerDisplay()
        updateButtonColors()
    }

    private fun updateButtonColors() {
        val blueDark = 0xFF1976D2.toInt()
        val white = 0xFFFFFFFF.toInt()
        val lightBlue = 0xFFE3F2FD.toInt()

        if (isFocusSession) {
            btnFocus.setBackgroundColor(blueDark)
            btnFocus.setTextColor(white)
            btnBreak.setBackgroundColor(lightBlue)
            btnBreak.setTextColor(blueDark)
        } else {
            btnBreak.setBackgroundColor(blueDark)
            btnBreak.setTextColor(white)
            btnFocus.setBackgroundColor(lightBlue)
            btnFocus.setTextColor(blueDark)
        }
    }

    private fun startTimer() {
        if (timerRunning) return

        sessionStartTime = System.currentTimeMillis()

        countDownTimer = object : CountDownTimer(timeLeftInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftInMillis = millisUntilFinished
                updateTimerDisplay()
            }

            override fun onFinish() {
                timerRunning = false
                timeLeftInMillis = 0
                updateTimerDisplay()
                onTimerComplete()
            }
        }.start()

        timerRunning = true
        updateControlButtons()
    }

    private fun pauseTimer() {
        countDownTimer?.cancel()
        timerRunning = false

        // Save partial session if it was a focus session
        if (isFocusSession && sessionStartTime > 0) {
            val elapsedMillis = System.currentTimeMillis() - sessionStartTime
            val minutesStudied = elapsedMillis / 60000 // Convert to minutes

            if (minutesStudied >= 1) {
                saveStudySession(minutesStudied, true)
            }
        }

        updateControlButtons()
    }

    private fun resetTimer() {
        countDownTimer?.cancel()
        timerRunning = false

        // Save partial session if it was a focus session
        if (isFocusSession && sessionStartTime > 0) {
            val elapsedMillis = System.currentTimeMillis() - sessionStartTime
            val minutesStudied = elapsedMillis / 60000

            if (minutesStudied >= 1) {
                saveStudySession(minutesStudied, true)
            }
        }

        if (isFocusSession) {
            timeLeftInMillis = FOCUS_TIME
        } else {
            timeLeftInMillis = BREAK_TIME
        }

        updateTimerDisplay()
        updateControlButtons()
    }

    private fun updateTimerDisplay() {
        val minutes = (timeLeftInMillis / 1000) / 60
        val seconds = (timeLeftInMillis / 1000) % 60

        tvTimer.text = String.format("%02d:%02d", minutes, seconds)
        progressTimer.progress = timeLeftInMillis.toInt()
    }

    private fun updateControlButtons() {
        btnStart.isEnabled = !timerRunning
        btnPause.isEnabled = timerRunning
        btnReset.isEnabled = !timerRunning

        btnStart.alpha = if (timerRunning) 0.5f else 1.0f
        btnPause.alpha = if (!timerRunning) 0.5f else 1.0f
        btnReset.alpha = if (timerRunning) 0.5f else 1.0f
    }

    private fun onTimerComplete() {
        // Calculate actual minutes completed
        val totalMinutes = if (isFocusSession) {
            // For completed focus session, use the full 25 minutes
            25L
        } else {
            // For breaks, use 5 minutes
            5L
        }

        saveStudySession(totalMinutes, false)

        if (isFocusSession) {
            safeShowToast("🎉 Focus session completed! ${totalMinutes}m saved")
            switchToBreak()
        } else {
            safeShowToast("⏰ Break completed! ${totalMinutes}m saved")
            switchToFocus()
        }

        loadStats()
    }

    private fun saveStudySession(minutes: Long, isPartial: Boolean) {
        val userId = auth.currentUser?.uid ?: return

        if (minutes <= 0) return

        val sessionData = hashMapOf(
            "userId" to userId,
            "duration" to minutes,
            "sessionType" to if (isFocusSession) "focus" else "break",
            "createdAt" to System.currentTimeMillis()
        )

        db.collection("study_sessions")
            .add(sessionData)
            .addOnSuccessListener {
                // Don't show toast for partial saves to reduce spam
                if (!isPartial) {
                    safeShowToast("✅ Saved $minutes minutes studied")
                }
                // Always reload stats after saving
                loadStats()
            }
            .addOnFailureListener { e ->
                // Don't show error toast if fragment is detached
                if (isAdded) {
                    safeShowToast("Failed to save session")
                }
            }
    }

    private fun loadStats() {
        val userId = auth.currentUser?.uid ?: return

        // Only load stats if fragment is still attached
        if (!isAdded) return

        db.collection("study_sessions")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                // Check if fragment is still attached before updating UI
                if (!isAdded) return@addOnSuccessListener

                var focusCount = 0
                var totalMinutes = 0L

                snapshot.documents.forEach { document ->
                    val sessionType = document.getString("sessionType") ?: "focus"
                    val duration = document.getLong("duration") ?: 0L

                    if (sessionType == "focus") {
                        focusCount++
                        totalMinutes += duration
                    }
                }

                tvFocusSessions.text = focusCount.toString()

                val hours = totalMinutes / 60
                val minutes = totalMinutes % 60

                if (hours > 0) {
                    tvTotalTime.text = "${hours}h ${minutes}m"
                } else {
                    tvTotalTime.text = "${minutes}m"
                }

                // Calculate streak
                calculateStreak(userId)
            }
            .addOnFailureListener { e ->
                if (isAdded) {
                    tvFocusSessions.text = "0"
                    tvTotalTime.text = "0m"
                    tvStreak.text = "0"
                }
            }
    }

    private fun calculateStreak(userId: String) {
        // Only calculate if fragment is attached
        if (!isAdded) return

        db.collection("study_sessions")
            .whereEqualTo("userId", userId)
            .whereEqualTo("sessionType", "focus")
            .get()
            .addOnSuccessListener { snapshot ->
                if (!isAdded) return@addOnSuccessListener

                if (snapshot.documents.isEmpty()) {
                    tvStreak.text = "0"
                    return@addOnSuccessListener
                }

                val dateFormat = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault())
                val studyDates = mutableSetOf<String>()

                snapshot.documents.forEach { document ->
                    val createdAt = document.getLong("createdAt") ?: 0L
                    val dateStr = dateFormat.format(java.util.Date(createdAt))
                    studyDates.add(dateStr)
                }

                var streak = 0
                val calendar = java.util.Calendar.getInstance()
                calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
                calendar.set(java.util.Calendar.MINUTE, 0)
                calendar.set(java.util.Calendar.SECOND, 0)
                calendar.set(java.util.Calendar.MILLISECOND, 0)

                while (true) {
                    val dateStr = dateFormat.format(calendar.time)
                    if (studyDates.contains(dateStr)) {
                        streak++
                        calendar.add(java.util.Calendar.DAY_OF_YEAR, -1)
                    } else {
                        break
                    }
                }

                tvStreak.text = streak.toString()
            }
            .addOnFailureListener { e ->
                if (isAdded) {
                    tvStreak.text = "0"
                }
            }
    }

    private fun safeShowToast(message: String) {
        // Only show toast if fragment is attached to an activity
        if (isAdded && context != null) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun getViewId(name: String): Int {
        return requireActivity().resources.getIdentifier(name, "id", requireActivity().packageName)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        countDownTimer?.cancel()

        // Save any partial session when leaving the fragment
        if (isFocusSession && sessionStartTime > 0 && !timerRunning) {
            val elapsedMillis = System.currentTimeMillis() - sessionStartTime
            val minutesStudied = elapsedMillis / 60000

            if (minutesStudied >= 1) {
                saveStudySession(minutesStudied, true)
            }
        }
    }
}