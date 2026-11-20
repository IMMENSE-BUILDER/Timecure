package com.timecure.app.data.repository

import com.timecure.app.data.local.TimecureDatabaseHelper
import com.timecure.app.model.SavedPlace
import com.timecure.app.model.SmartTask
import kotlinx.coroutines.flow.Flow

class SmartTaskRepository(private val db: TimecureDatabaseHelper) {
    val placesFlow: Flow<List<SavedPlace>> = db.placesFlow
    val tasksFlow: Flow<List<SmartTask>> = db.smartTasksFlow

    suspend fun refreshAll() {
        db.refreshPlaces()
        db.refreshSmartTasks()
    }

    suspend fun getAllPlaces(): List<SavedPlace> = db.getAllPlaces()
    suspend fun getPlaceById(id: Long): SavedPlace? = db.getPlaceById(id)
    suspend fun insertPlace(place: SavedPlace): Long = db.insertPlace(place)
    suspend fun updatePlace(place: SavedPlace) = db.updatePlace(place)
    suspend fun deletePlace(id: Long) = db.deletePlace(id)

    suspend fun getAllTasks(): List<SmartTask> = db.getAllSmartTasks()
    suspend fun getTaskById(id: Long): SmartTask? = db.getSmartTaskById(id)
    suspend fun insertTask(task: SmartTask): Long = db.insertSmartTask(task)
    suspend fun updateTask(task: SmartTask) = db.updateSmartTask(task)
    suspend fun deleteTask(id: Long) = db.deleteSmartTask(id)
    suspend fun setTaskEnabled(id: Long, enabled: Boolean) = db.setSmartTaskEnabled(id, enabled)
    suspend fun setTaskCompleted(id: Long, completed: Boolean) = db.setSmartTaskCompleted(id, completed)
    suspend fun updateTaskGeoState(id: Long, isInside: Boolean, lastTriggeredMillis: Long) =
        db.updateSmartTaskGeoState(id, isInside, lastTriggeredMillis)
}
