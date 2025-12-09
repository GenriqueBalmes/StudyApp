package com.mobcomp.studyx

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class TasksFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var rvTasks: RecyclerView
    private lateinit var llEmptyState: LinearLayout
    private lateinit var tvTotalTasks: TextView
    private lateinit var tvCompletedTasks: TextView
    private lateinit var tvPendingTasks: TextView
    private lateinit var fabAddTask: FloatingActionButton

    private lateinit var taskAdapter: SimpleTaskAdapter
    private var tasksListener: ListenerRegistration? = null
    private val tasks = mutableListOf<Task>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Use the regular inflate since we're using R imports
        return inflater.inflate(R.layout.fragment_tasks, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Initialize views
        rvTasks = view.findViewById(R.id.rvTasks)
        llEmptyState = view.findViewById(R.id.llEmptyState)
        tvTotalTasks = view.findViewById(R.id.tvTotalTasks)
        tvCompletedTasks = view.findViewById(R.id.tvCompletedTasks)
        tvPendingTasks = view.findViewById(R.id.tvPendingTasks)
        fabAddTask = view.findViewById(R.id.fabAddTask)

        // Setup RecyclerView
        setupRecyclerView()

        // Load tasks
        loadTasks()

        // FAB click listener
        fabAddTask.setOnClickListener {
            showAddTaskDialog()
        }


    }

    private fun setupRecyclerView() {
        taskAdapter = SimpleTaskAdapter(
            tasks = tasks,
            onTaskClick = { task ->
                showEditTaskDialog(task)
            },
            onTaskDelete = { task ->
                showDeleteDialog(task)
            },
            onTaskToggle = { task, isChecked ->
                updateTaskCompletion(task, isChecked)
            }
        )

        rvTasks.layoutManager = LinearLayoutManager(requireContext())
        rvTasks.adapter = taskAdapter
    }

    private fun loadTasks() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            showToast("Please login again")
            return
        }

        println("✅ DEBUG: Loading tasks for user: $userId")

        tasksListener = db.collection("tasks")
            .whereEqualTo("userId", userId)  // CRITICAL: Only get tasks for current user
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    println("❌ ERROR: ${error.message}")
                    showToast("Error loading tasks")
                    return@addSnapshotListener
                }

                tasks.clear()
                snapshot?.documents?.forEach { document ->
                    try {
                        val task = Task(
                            id = document.id,
                            title = document.getString("title") ?: "",
                            description = document.getString("description") ?: "",
                            isCompleted = document.getBoolean("isCompleted") ?: false,
                            dueDate = document.getLong("dueDate") ?: 0L,
                            priority = (document.getLong("priority") ?: 0L).toInt(),
                            createdAt = document.getLong("createdAt") ?: System.currentTimeMillis(),
                            userId = document.getString("userId") ?: userId
                        )
                        tasks.add(task)
                        println("✅ Loaded task: ${task.title} (User: ${task.userId})")
                    } catch (e: Exception) {
                        println("❌ Error parsing task: ${e.message}")
                    }
                }

                println("✅ Total tasks loaded: ${tasks.size}")
                taskAdapter.updateTasks(tasks)
                updateStats()
                updateEmptyState()
            }
    }

    private fun showAddTaskDialog() {
        showTaskDialog(null)
    }

    private fun showEditTaskDialog(task: Task) {
        showTaskDialog(task)
    }

    private fun showTaskDialog(task: Task? = null) {
        val context = requireContext()

        val dialogView = LinearLayout(context)
        dialogView.orientation = LinearLayout.VERTICAL
        dialogView.setPadding(50, 30, 50, 30)

        val titleInput = EditText(context)
        titleInput.hint = "Task Title *"
        titleInput.setText(task?.title ?: "")

        val descInput = EditText(context)
        descInput.hint = "Description (Optional)"
        descInput.setText(task?.description ?: "")

        dialogView.addView(titleInput)
        dialogView.addView(descInput)

        AlertDialog.Builder(context)
            .setTitle(if (task == null) "Add New Task" else "Edit Task")
            .setView(dialogView)
            .setPositiveButton("Save") { dialog, _ ->
                val title = titleInput.text.toString().trim()
                val description = descInput.text.toString().trim()

                if (title.isEmpty()) {
                    titleInput.error = "Title is required"
                    return@setPositiveButton
                }

                val userId = auth.currentUser?.uid
                if (userId == null) {
                    showToast("Please login again")
                    return@setPositiveButton
                }

                val taskToSave = if (task == null) {
                    // NEW TASK - includes userId
                    Task(
                        title = title,
                        description = description,
                        userId = userId,  // CRITICAL: Add user ID
                        createdAt = System.currentTimeMillis()
                    )
                } else {
                    // UPDATE EXISTING TASK
                    task.copy(
                        title = title,
                        description = description
                    )
                }

                saveTask(taskToSave)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveTask(task: Task) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            showToast("Please login again")
            return
        }

        println("💾 DEBUG: Saving task for user: $userId")
        println("💾 Task title: ${task.title}")
        println("💾 Task has ID: ${task.id.isNotEmpty()}")

        // Prepare Firestore data
        val taskData = hashMapOf(
            "title" to task.title,
            "description" to task.description,
            "isCompleted" to task.isCompleted,
            "dueDate" to task.dueDate,
            "priority" to task.priority,
            "createdAt" to task.createdAt,
            "userId" to userId  // CRITICAL: Store user ID
        )

        if (task.id.isEmpty()) {
            // NEW TASK
            db.collection("tasks")
                .add(taskData)
                .addOnSuccessListener { documentReference ->
                    println("✅ Task saved! ID: ${documentReference.id}")
                    showToast("Task added!")
                }
                .addOnFailureListener { e ->
                    println("❌ Error saving task: ${e.message}")
                    showToast("Failed to save: ${e.message}")
                }
        } else {
            // UPDATE TASK
            db.collection("tasks")
                .document(task.id)
                .set(taskData)
                .addOnSuccessListener {
                    println("✅ Task updated: ${task.id}")
                    showToast("Task updated!")
                }
                .addOnFailureListener { e ->
                    println("❌ Error updating task: ${e.message}")
                    showToast("Failed to update: ${e.message}")
                }
        }
    }

    private fun showDeleteDialog(task: Task) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Task")
            .setMessage("Delete '${task.title}'?")
            .setPositiveButton("Delete") { _, _ ->
                deleteTask(task)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteTask(task: Task) {
        val userId = auth.currentUser?.uid
        if (userId == null || task.userId != userId) {
            showToast("Cannot delete task: Unauthorized")
            return
        }

        println("🗑️ DEBUG: Attempting to delete task: ${task.id}")
        println("🗑️ Task belongs to user: ${task.userId}")
        println("🗑️ Current user: $userId")

        db.collection("tasks")
            .document(task.id)
            .delete()
            .addOnSuccessListener {
                println("✅ Task successfully deleted: ${task.id}")
                showToast("Task deleted")

                // Remove from local list
                val index = tasks.indexOfFirst { it.id == task.id }
                if (index != -1) {
                    tasks.removeAt(index)
                    taskAdapter.updateTasks(tasks)
                    updateStats()
                    updateEmptyState()
                }
            }
            .addOnFailureListener { e ->
                println("❌ Error deleting task: ${e.message}")
                println("❌ Error details: ${e.toString()}")

                // Show more detailed error message
                val errorMsg = when {
                    e.message?.contains("permission") == true ->
                        "Permission denied. Check Firebase rules."
                    e.message?.contains("not found") == true ->
                        "Task not found or already deleted"
                    else -> "Error: ${e.message}"
                }
                showToast(errorMsg)

                // Test if we can read the task first
                testTaskAccess(task)
            }
    }

    // Add this debug function
    private fun testTaskAccess(task: Task) {
        val userId = auth.currentUser?.uid ?: return

        println("🔍 DEBUG: Testing access to task ${task.id}")

        db.collection("tasks")
            .document(task.id)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val taskUserId = document.getString("userId") ?: "none"
                    println("📄 Task exists! User ID in DB: $taskUserId")
                    println("📄 Current user ID: $userId")
                    println("📄 Can delete? ${taskUserId == userId}")

                    if (taskUserId != userId) {
                        showToast("Cannot delete other user's task")
                    }
                } else {
                    println("📄 Task doesn't exist in DB")
                }
            }
            .addOnFailureListener { e ->
                println("❌ Cannot read task: ${e.message}")
            }
    }

    private fun updateTaskCompletion(task: Task, isChecked: Boolean) {
        val updatedTask = task.copy(isCompleted = isChecked)

        val taskData = hashMapOf(
            "title" to updatedTask.title,
            "description" to updatedTask.description,
            "isCompleted" to updatedTask.isCompleted,
            "dueDate" to updatedTask.dueDate,
            "priority" to updatedTask.priority,
            "createdAt" to updatedTask.createdAt,
            "userId" to updatedTask.userId
        )

        db.collection("tasks")
            .document(task.id)
            .set(taskData)
            .addOnSuccessListener {
                val index = tasks.indexOfFirst { it.id == task.id }
                if (index != -1) {
                    tasks[index] = updatedTask
                    taskAdapter.updateTasks(tasks)
                    updateStats()
                }
                println("✅ Task completion updated: ${task.id} -> $isChecked")
            }
            .addOnFailureListener { e ->
                println("❌ Error updating completion: ${e.message}")
                showToast("Error: ${e.message}")
            }
    }

    private fun updateStats() {
        val total = tasks.size
        val completed = tasks.count { it.isCompleted }
        val pending = total - completed

        tvTotalTasks.text = total.toString()
        tvCompletedTasks.text = completed.toString()
        tvPendingTasks.text = pending.toString()
    }

    private fun updateEmptyState() {
        val isEmpty = tasks.isEmpty()
        llEmptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
        rvTasks.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    // DEBUG FUNCTION - TEMPORARY
    private fun addDebugButton(view: View) {
        val debugButton = Button(requireContext())
        debugButton.text = "DEBUG: Test Firebase"
        debugButton.setOnClickListener {
            testFirebase()
        }
        (view as? ViewGroup)?.addView(debugButton)
    }

    private fun testFirebase() {
        val userId = auth.currentUser?.uid ?: return

        println("=== 🔍 FIREBASE DEBUG TEST ===")
        println("Current user ID: $userId")

        // Test if we can write
        val testTask = hashMapOf(
            "title" to "Test Task",
            "description" to "This is a test",
            "isCompleted" to false,
            "userId" to userId,
            "createdAt" to System.currentTimeMillis()
        )

        db.collection("tasks")
            .add(testTask)
            .addOnSuccessListener {
                println("✅ WRITE TEST: Success! Task added.")
                showToast("Write test: Success!")
            }
            .addOnFailureListener { e ->
                println("❌ WRITE TEST: Failed - ${e.message}")
                showToast("Write failed: ${e.message}")
            }

        // Test if we can read
        db.collection("tasks")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                println("✅ READ TEST: Found ${snapshot.documents.size} tasks")
                showToast("Found ${snapshot.documents.size} tasks")
            }
            .addOnFailureListener { e ->
                println("❌ READ TEST: Failed - ${e.message}")
                showToast("Read failed: ${e.message}")
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        tasksListener?.remove()
    }
}