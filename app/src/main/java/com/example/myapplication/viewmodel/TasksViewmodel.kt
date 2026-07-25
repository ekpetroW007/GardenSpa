package com.example.myapplication.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.database.entity.TaskEntity
import com.example.myapplication.data.repository.BookeeperRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TasksViewmodel(
    private val repository: BookeeperRepository
) : ViewModel() {

    val tasks = repository.allTasks
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    fun addTask(name: String) {
        viewModelScope.launch {
            try {
                val newTask = TaskEntity(
                    name = name
                )
                repository.insertTask(newTask)
            } catch (e: Exception) {
                Log.d("addTask", e.toString())
            }
        }
    }

    fun deleteTask(id: Int) {
        viewModelScope.launch {
            try {
                repository.deleteTask(id)
            } catch (e: Exception) {
                Log.d("deleteTask", e.toString())
            }
        }
    }

}