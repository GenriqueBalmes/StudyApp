package com.mobcomp.studyx

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SimpleTaskAdapter(
    private var tasks: List<Task> = emptyList(),
    private val onTaskClick: (Task) -> Unit,
    private val onTaskDelete: (Task) -> Unit,
    private val onTaskToggle: (Task, Boolean) -> Unit
) : RecyclerView.Adapter<SimpleTaskAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Use regular findViewById since you have R imports
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        val cbCompleted: CheckBox = itemView.findViewById(R.id.cbCompleted)
        val ivDelete: ImageView = itemView.findViewById(R.id.ivDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task_simple, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val task = tasks[position]

        holder.tvTitle.text = task.title
        holder.tvDescription.text = task.description

        // Remove listener before setting to avoid recycling issues
        holder.cbCompleted.setOnCheckedChangeListener(null)
        holder.cbCompleted.isChecked = task.isCompleted

        // Update UI based on completion
        if (task.isCompleted) {
            holder.tvTitle.paintFlags = holder.tvTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            holder.tvTitle.setTextColor(0xFF888888.toInt())
            holder.tvDescription.setTextColor(0xFFAAAAAA.toInt())
            holder.tvDescription.visibility = if (task.description.isEmpty()) View.GONE else View.VISIBLE
        } else {
            holder.tvTitle.paintFlags = holder.tvTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            holder.tvTitle.setTextColor(0xFF333333.toInt())
            holder.tvDescription.setTextColor(0xFF666666.toInt())
            holder.tvDescription.visibility = if (task.description.isEmpty()) View.GONE else View.VISIBLE
        }

        // Set listeners
        holder.cbCompleted.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked != task.isCompleted) {
                onTaskToggle(task, isChecked)
            }
        }

        holder.ivDelete.setOnClickListener {
            onTaskDelete(task)
        }

        holder.itemView.setOnClickListener {
            onTaskClick(task)
        }
    }

    override fun getItemCount(): Int = tasks.size

    fun updateTasks(newTasks: List<Task>) {
        tasks = newTasks
        notifyDataSetChanged()
    }
}