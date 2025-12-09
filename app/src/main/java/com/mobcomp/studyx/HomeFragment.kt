package com.mobcomp.studyx

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HomeFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var tvUserName: TextView
    private lateinit var tvQuote: TextView
    private lateinit var tvTasksCompleted: TextView
    private lateinit var tvStudyTime: TextView
    private lateinit var tvStreak: TextView
    private lateinit var btnLogout: Button

    private val quotes = listOf(
        "The future belongs to those who believe in the beauty of their dreams. - Eleanor Roosevelt",
        "Education is the most powerful weapon which you can use to change the world. - Nelson Mandela",
        "The beautiful thing about learning is that no one can take it away from you. - B.B. King",
        "Don't let what you cannot do interfere with what you can do. - John Wooden",
        "Start where you are. Use what you have. Do what you can. - Arthur Ashe",
        "The only way to learn mathematics is to do mathematics. - Paul Halmos",
        "Quality education is the foundation of a better future. - Unknown",
        "Study not to know more but to know better. - Seneca",
        "Learning is a treasure that will follow its owner everywhere. - Chinese Proverb",
        "Push yourself, because no one else is going to do it for you. - Unknown"
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val layoutId = requireActivity().resources.getIdentifier("fragment_home", "layout", requireActivity().packageName)
        return inflater.inflate(layoutId, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val tvUserNameId = requireActivity().resources.getIdentifier("tvUserName", "id", requireActivity().packageName)
        val tvQuoteId = requireActivity().resources.getIdentifier("tvQuote", "id", requireActivity().packageName)
        val tvTasksCompletedId = requireActivity().resources.getIdentifier("tvTasksCompleted", "id", requireActivity().packageName)
        val tvStudyTimeId = requireActivity().resources.getIdentifier("tvStudyTime", "id", requireActivity().packageName)
        val tvStreakId = requireActivity().resources.getIdentifier("tvStreak", "id", requireActivity().packageName)
        val btnLogoutId = requireActivity().resources.getIdentifier("btnLogout", "id", requireActivity().packageName)

        tvUserName = view.findViewById(tvUserNameId)
        tvQuote = view.findViewById(tvQuoteId)
        tvTasksCompleted = view.findViewById(tvTasksCompletedId)
        tvStudyTime = view.findViewById(tvStudyTimeId)
        tvStreak = view.findViewById(tvStreakId)
        btnLogout = view.findViewById(btnLogoutId)

        loadUserData()
        showRandomQuote()

        btnLogout.setOnClickListener {
            auth.signOut()
            Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finish()
        }
    }

    override fun onResume() {
        super.onResume()
        loadProgressStats()
    }

    private fun loadUserData() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (isAdded && document.exists()) {
                    val fullName = document.getString("fullName") ?: "Student"
                    tvUserName.text = fullName
                }
            }
    }

    private fun showRandomQuote() {
        if (isAdded) {
            val randomQuote = quotes.random()
            tvQuote.text = randomQuote
        }
    }

    private fun loadProgressStats() {
        val userId = auth.currentUser?.uid ?: return

        // Check if fragment is still attached
        if (!isAdded) return

        // Load all completed tasks
        db.collection("tasks")
            .whereEqualTo("userId", userId)
            .whereEqualTo("isCompleted", true)
            .get()
            .addOnSuccessListener { tasksSnapshot ->
                if (!isAdded) return@addOnSuccessListener

                val completedTasks = tasksSnapshot.documents.size
                tvTasksCompleted.text = "✅ Tasks Completed: $completedTasks"

                // Load all study sessions
                db.collection("study_sessions")
                    .whereEqualTo("userId", userId)
                    .get()
                    .addOnSuccessListener { sessionsSnapshot ->
                        if (!isAdded) return@addOnSuccessListener

                        var totalMinutes = 0L

                        sessionsSnapshot.documents.forEach { document ->
                            val duration = document.getLong("duration") ?: 0L
                            totalMinutes += duration
                        }

                        val hours = totalMinutes / 60
                        val minutes = totalMinutes % 60

                        if (hours > 0) {
                            tvStudyTime.text = "⏱️ Study Time: ${hours}h ${minutes}m"
                        } else {
                            tvStudyTime.text = "⏱️ Study Time: ${minutes}m"
                        }

                        // Calculate streak
                        calculateStudyStreak(userId)
                    }
                    .addOnFailureListener { e ->
                        if (isAdded) {
                            tvStudyTime.text = "⏱️ Study Time: 0m"
                            tvStreak.text = "🔥 Study Streak: 0 days"
                        }
                    }
            }
            .addOnFailureListener { e ->
                if (isAdded) {
                    tvTasksCompleted.text = "✅ Tasks Completed: 0"
                    tvStudyTime.text = "⏱️ Study Time: 0m"
                    tvStreak.text = "🔥 Study Streak: 0 days"
                }
            }
    }

    private fun calculateStudyStreak(userId: String) {
        if (!isAdded) return

        db.collection("study_sessions")
            .whereEqualTo("userId", userId)
            .whereEqualTo("sessionType", "focus")
            .get()
            .addOnSuccessListener { snapshot ->
                if (!isAdded) return@addOnSuccessListener

                if (snapshot.documents.isEmpty()) {
                    tvStreak.text = "🔥 Study Streak: 0 days"
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

                tvStreak.text = "🔥 Study Streak: $streak ${if (streak == 1) "day" else "days"}"
            }
            .addOnFailureListener { e ->
                if (isAdded) {
                    tvStreak.text = "🔥 Study Streak: 0 days"
                }
            }
    }
}