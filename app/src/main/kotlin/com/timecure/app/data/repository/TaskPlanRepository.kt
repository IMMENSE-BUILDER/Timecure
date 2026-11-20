package com.timecure.app.data.repository

import com.timecure.app.data.local.TimecureDatabaseHelper
import com.timecure.app.model.AiTaskItem
import kotlinx.coroutines.flow.Flow

class TaskPlanRepository(private val db: TimecureDatabaseHelper) {

    val tasksFlow: Flow<List<AiTaskItem>> = db.tasksFlow

    suspend fun refresh() = db.refreshTasks()
    suspend fun getAll(): List<AiTaskItem> = db.getAllTasks()
    suspend fun replacePlan(items: List<AiTaskItem>) = db.replaceTaskPlan(items)
    suspend fun toggleCompleted(id: Long, completed: Boolean) = db.setTaskCompleted(id, completed)
    suspend fun clearAll() = db.clearTaskPlan()
}
